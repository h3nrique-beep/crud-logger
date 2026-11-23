# CRUD Produtos + Logger Singleton + Financeiro

API Spring Boot com:

- CRUD de produtos (`/produtos`);
- `AppLogger` Singleton registrando as operações no console e em `app.log`;
- consulta de CEP (`/cep/{cep}`) com **Adapter** (ViaCEP ou BrasilAPI) e **Proxy** (validação, cache e log);
- **nova funcionalidade: financeiro** (`/lancamentos`), contas a pagar e a receber com baixa, estorno, cancelamento e resumo, usando os padrões comportamentais **State** e **Strategy**. É a mesma área que está sendo desenvolvida no Sputnik (gestão de oficinas).

## Como executar

Pré-requisitos: JDK 17+ e Maven 3.9+.

```bash
mvn spring-boot:run     # sobe em http://localhost:8080 (H2 em memória)
mvn test                # estados, regras de pagamento e o fluxo HTTP completo
```

O provedor de CEP é escolhido em `application.properties` (`cep.provedor=viacep` ou `brasilapi`). A consulta real precisa de internet; os testes simulam só o provedor externo.

## Nova funcionalidade: financeiro

**Problema que resolve:** o sistema cadastrava produtos, mas não controlava o dinheiro. Agora registra o que a empresa tem a pagar e a receber, calcula multa, juros ou desconto na hora da baixa e mostra o resumo do caixa, sem permitir operações incoerentes (baixar duas vezes, cancelar algo já pago).

### Regras de negócio

- Lançamento tem tipo (`RECEITA` = a receber, `DESPESA` = a pagar), descrição, valor maior que zero e vencimento. Sem categoria, vira "Geral".
- Cada lançamento escolhe uma **regra de pagamento**, aplicada na baixa:

| Regra | O que faz |
| --- | --- |
| `MULTA_JUROS` | pago com atraso: 2% de multa + 0,033% ao dia de juros (≈1% ao mês); em dia, nada |
| `DESCONTO_PONTUALIDADE` | pago até o vencimento: 5% de desconto; depois, valor cheio |
| `VALOR_FIXO` (padrão) | sempre o valor original |

- `valorPago = valor + ajuste` (ajuste negativo é desconto), com 2 casas.
- A data de pagamento não pode ser futura; sem data, vale hoje.
- **Vencido** = pendente com vencimento passado. É calculado, não gravado, então muda sozinho com a data.
- Ciclo de vida:

```text
PENDENTE --baixar--> PAGO
   ^                  |
   +-----estornar-----+
PENDENTE --cancelar--> CANCELADO (final)
```

  Qualquer outra operação responde **409 Conflict** com o motivo (ex.: `não é possível cancelar o lançamento 1 no estado PAGO`). Para cancelar algo pago, estorne antes.

- **Resumo:** a receber e a pagar (pendentes), recebido e pago (com ajustes), saldo realizado e quantidade de vencidos.

### Endpoints

| Método | Rota | O que faz |
| --- | --- | --- |
| POST | `/lancamentos` | cria: `{"tipo":"DESPESA","descricao":"Aluguel","categoria":"Fixas","valor":1000,"vencimento":"2026-11-10","regraPagamento":"MULTA_JUROS"}` |
| GET | `/lancamentos?status=PENDENTE` | lista (filtro opcional) |
| GET | `/lancamentos/{id}` | detalha |
| GET | `/lancamentos/resumo` | indicadores |
| POST | `/lancamentos/{id}/baixar?data=2026-11-20` | PENDENTE → PAGO, aplicando a regra |
| POST | `/lancamentos/{id}/estornar` | PAGO → PENDENTE |
| POST | `/lancamentos/{id}/cancelar` | PENDENTE → CANCELADO |

### Roteiro de demonstração

```bash
curl -X POST localhost:8080/lancamentos -H "Content-Type: application/json" -d "{\"tipo\":\"DESPESA\",\"descricao\":\"Aluguel\",\"valor\":1000,\"vencimento\":\"2026-11-10\",\"regraPagamento\":\"MULTA_JUROS\"}"
curl -X POST "localhost:8080/lancamentos/1/baixar?data=2026-11-20"   # 10 dias de atraso: valorPago 1023.30
curl -X POST localhost:8080/lancamentos/1/baixar                      # 409: já está pago
curl -X POST localhost:8080/lancamentos/1/cancelar                    # 409: estorne antes
curl -X POST localhost:8080/lancamentos/1/estornar
curl -X POST localhost:8080/lancamentos/1/cancelar
curl localhost:8080/lancamentos/resumo
```

Cada passo aparece no `app.log`, por exemplo:

```text
[INFO] lançamento criado id=1 DESPESA 1000.00 vence 2026-11-10 regra=MULTA_JUROS
[INFO] baixa MULTA_JUROS em 2026-11-20: 1000.00 ajuste 23.30 = 1023.30
[INFO] lançamento 1: PENDENTE -> PAGO
[ERRO] não é possível baixar o lançamento 1 no estado PAGO
```

## Padrões comportamentais

Código em `src/main/java/com/example/produtos/financeiro/`.

### State: ciclo de vida do lançamento

**Por quê:** sem o padrão, cada operação teria um `if (status == ...)` no service, e cada status novo obrigaria a revisar todos eles. No Sputnik, por exemplo, a baixa faz `if (status == PAGO) throw`; com estorno e cancelamento isso se multiplica.

**Como:**

- `EstadoLancamento` (interface): `baixar`, `estornar` e `cancelar`. Por padrão todas lançam `TransicaoInvalidaException` (HTTP 409).
- `EstadoPendente` aceita baixar e cancelar; `EstadoPago` aceita estornar; `EstadoCancelado` não aceita nada.
- `StatusLancamento` (enum gravado no banco) liga cada status ao seu objeto de estado.
- `LancamentoFinanceiro.baixar()` só delega: `status.estado().baixar(this, ...)`. Os métodos que mudam status e pagamento são visíveis só no pacote, então ninguém de fora pula etapas.

### Strategy: regra de pagamento

**Por quê:** multa e juros, desconto por pontualidade e valor fixo são regras diferentes para a mesma operação (a baixa). Com `if (regra == ...)` no service, cada regra nova alteraria uma classe que já funciona.

**Como:**

- `RegraPagamento` (interface): `tipo()` e `ajuste(valor, vencimento, pagamentoEm)`.
- `MultaEJuros`, `DescontoPontualidade` e `ValorFixo`: uma regra por classe, cada uma um `@Component`.
- O `FinanceiroService` recebe a `List<RegraPagamento>` do Spring e monta um mapa por `tipo()`. O lançamento guarda qual regra usa; na baixa, o service busca a regra e passa o ajuste para o estado. Para criar, por exemplo, "PARCELADO", basta uma classe nova: o service não muda.

### Integração com o que já existia

```text
FinanceiroController ──> FinanceiroService ──> RegraPagamento (Strategy escolhida pelo lançamento)
      (HTTP)            (regras, AppLogger)  ├─> LancamentoFinanceiro ──> EstadoLancamento (State)
                                             └─> LancamentoRepository (JPA)
```

- **Singleton:** service e controller usam `AppLogger.getInstance()` para criação, baixa com o ajuste calculado, transições, recusas e resumo.
- **CRUD, Adapter e Proxy:** `/produtos` e `/cep/{cep}` não mudaram. O teste `FinanceiroApiTest` prova os dois: cria, lê e remove um produto; consulta o mesmo CEP duas vezes e confirma que o Adapter foi chamado uma vez só (a segunda veio do cache do Proxy); CEP inválido é barrado pelo Proxy.
- **Camadas:** regras no service e no domínio (estados e regras de pagamento); o controller só traduz HTTP.

## SOLID aplicado

- **S (Responsabilidade única):** controller só HTTP; service orquestra; cada estado e cada regra de pagamento tem uma responsabilidade.
- **O (Aberto/fechado):** regra de pagamento ou estado novo = classe nova, sem alterar o `FinanceiroService`.
- **L (Substituição de Liskov):** qualquer `RegraPagamento` ou `EstadoLancamento` pode ser usado no lugar de outro.
- **I (Segregação de interfaces):** `RegraPagamento` só calcula o ajuste; `EstadoLancamento` só decide transições.
- **D (Inversão de dependência):** o service depende da interface `RegraPagamento`, injetada pelo Spring, e não de `MultaEJuros`.

## Melhorias obtidas

- Operação incoerente vira erro claro (409 com motivo) em vez de dado inconsistente no banco.
- Valores em `BigDecimal` com arredondamento explícito: sem erro de centavos de `double`.
- Regras de pagamento e estados testados sem subir o Spring (`RegraPagamentoTest`, `EstadoLancamentoTest`).
- O motivo dos erros aparece no corpo da resposta (`server.error.include-message=always`), inclusive os do Proxy de CEP.

## Dificuldades encontradas

- O `CepProxy` não compilava: a regex `"\d{8}"` é um escape inválido em Java. Trocada por `"[0-9]{8}"`.
- "Vencido" não é um estado do State: depende da data de hoje. Gravar isso exigiria um job atualizando o banco; ficou calculado (`isVencido()`).
- Onde aplicar a Strategy: a regra fica gravada no lançamento, mas o cálculo só acontece na baixa, porque depende da data real do pagamento.
- Testar sem internet: o teste HTTP substitui só o bean `provedorCep` (Adapter) por um mock, mantendo o Proxy real.

## Respostas da atividade do Singleton

**Quem garante a instância única em cada versão?**
- `AppLogger` (clássico): o próprio código — construtor privado + `getInstance()` com `volatile` e dupla checagem em `synchronized`.
- `SpringAppLogger` (`@Component`): o container Spring, que cria um único bean por contexto (escopo `singleton`). Nada impede um `new` manual.

**Quando o Singleton clássico ainda faz sentido?**
- Código fora do container Spring (utilitários estáticos, `main`, bibliotecas sem Spring) ou quando a instância única precisa valer para toda a JVM, não só para um contexto.

**Versão final: `AppLogger` (clássico).** Service e Controller usam `AppLogger.getInstance()`. Justificativa: é o foco do trabalho, não depende do Spring, não exige injeção em cada classe e garante instância única na JVM. Contra: mais difícil de mockar em testes; em projeto real, o bean Spring seria preferível por isso.
