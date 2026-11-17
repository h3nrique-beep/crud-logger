# CRUD Produtos + Logger Singleton + Pedidos com entrega

API Spring Boot com:

- CRUD de produtos (`/produtos`);
- `AppLogger` Singleton registrando as operações no console e em `app.log`;
- consulta de CEP (`/cep/{cep}`) com **Adapter** (ViaCEP ou BrasilAPI) e **Proxy** (validação, cache e log);
- **nova funcionalidade: pedidos com entrega** (`/pedidos`), usando os padrões comportamentais **State** e **Strategy**.

## Como executar

Pré-requisitos: JDK 17+ e Maven 3.9+.

```bash
mvn spring-boot:run     # sobe em http://localhost:8080 (H2 em memória)
mvn test                # testes de estado, frete e o fluxo HTTP completo
```

O provedor de CEP é escolhido em `application.properties` (`cep.provedor=viacep` ou `brasilapi`). A consulta real precisa de internet; os testes simulam só o provedor externo.

## Nova funcionalidade: pedidos com entrega

**Problema que resolve:** o CRUD só cadastrava produtos. Agora é possível vender um produto, calcular o frete até o CEP do cliente e acompanhar o pedido até a entrega, sem permitir operações fora de ordem (enviar sem pagar, cancelar depois de enviado...).

### Regras de negócio

- Quantidade maior que zero, produto existente e tipo de frete conhecido (senão 400/404).
- O CEP passa pelo `CepProxy` (8 dígitos, cache) e pelo Adapter configurado; a UF do endereço define o frete.
- O pedido guarda nome e preço do produto no momento da compra: alterar o preço ou remover o produto no CRUD não muda pedidos antigos.
- `total = subtotal + frete`, em reais com 2 casas.
- Ciclo de vida:

```text
CRIADO --pagar--> PAGO --enviar--> ENVIADO --entregar--> ENTREGUE
   |                |
   +--cancelar------+--cancelar--> CANCELADO
```

  Qualquer outra operação responde **409 Conflict** com o motivo (ex.: `não é possível enviar o pedido 1 no estado CRIADO`).

- Fretes (loja no PR):

| Tipo | Regra |
| --- | --- |
| `PADRAO` | grátis a partir de R$ 200; senão R$ 15 no PR e R$ 30 fora |
| `EXPRESSO` | R$ 30 no PR e R$ 60 fora, + 5% do subtotal (seguro); nunca grátis |
| `RETIRADA` | R$ 0 (cliente busca na loja) |

### Endpoints

| Método | Rota | O que faz |
| --- | --- | --- |
| POST | `/pedidos` | cria: `{"produtoId":1,"quantidade":3,"cep":"85810000","tipoFrete":"PADRAO"}` |
| GET | `/pedidos` | lista |
| GET | `/pedidos/{id}` | detalha |
| POST | `/pedidos/{id}/pagar` | CRIADO → PAGO |
| POST | `/pedidos/{id}/enviar` | PAGO → ENVIADO |
| POST | `/pedidos/{id}/entregar` | ENVIADO → ENTREGUE |
| POST | `/pedidos/{id}/cancelar` | CRIADO ou PAGO → CANCELADO |

### Roteiro de demonstração

```bash
curl -X POST localhost:8080/produtos -H "Content-Type: application/json" -d "{\"nome\":\"Caneta\",\"preco\":10}"
curl -X POST localhost:8080/pedidos -H "Content-Type: application/json" -d "{\"produtoId\":1,\"quantidade\":3,\"cep\":\"85810000\",\"tipoFrete\":\"PADRAO\"}"
curl -X POST localhost:8080/pedidos/1/enviar     # 409: ainda não foi pago
curl -X POST localhost:8080/pedidos/1/pagar
curl -X POST localhost:8080/pedidos/1/enviar
curl -X POST localhost:8080/pedidos/1/cancelar   # 409: já foi enviado
curl -X POST localhost:8080/pedidos/1/entregar
```

Cada passo aparece no `app.log`, por exemplo:

```text
[INFO] CEP 85810000 veio do PROVEDOR ViaCepAdapter
[INFO] frete PADRAO para PR sobre 30.0 = 15.0
[INFO] pedido criado id=1 total=45.0 status=CRIADO
[ERRO] não é possível enviar o pedido 1 no estado CRIADO
[INFO] pedido 1: CRIADO -> PAGO
```

## Padrões comportamentais

Código em `src/main/java/com/example/produtos/pedido/`.

### State — ciclo de vida do pedido

**Por quê:** sem o padrão, cada operação teria um `if/switch` sobre o status (`if (status == PAGO || status == CRIADO) ...`) espalhado pelo service, e cada status novo obrigaria a mexer em todos eles.

**Como:**

- `EstadoPedido` (interface): `pagar`, `enviar`, `entregar`, `cancelar`. Por padrão todas lançam `TransicaoInvalidaException` (HTTP 409).
- `EstadoCriado`, `EstadoPago`, `EstadoEnviado`: sobrescrevem só as operações que aceitam e dizem qual o próximo status.
- `EstadoFinal`: entregue ou cancelado; não aceita nada.
- `StatusPedido` (enum gravado no banco) liga cada status ao seu objeto de estado.
- `Pedido.pagar()` apenas delega: `status.estado().pagar(this)`. Só os estados (mesmo pacote) chamam `mudarPara`, então ninguém de fora pula etapas.

### Strategy — cálculo do frete

**Por quê:** o frete muda conforme o tipo de entrega. Com `if (tipo == ...)` no service, cada nova modalidade exigiria alterar uma classe que já funciona.

**Como:**

- `CalculoFrete` (interface): `tipo()` e `calcular(subtotal, destino)`.
- `FretePadrao`, `FreteExpresso`, `RetiradaNaLoja`: uma regra por classe, cada uma um `@Component`.
- O `PedidoService` recebe a `List<CalculoFrete>` do Spring e monta um mapa por `tipo()`. Para criar, por exemplo, "MOTOBOY", basta uma classe nova: o service não muda.

### Integração com o que já existia

```text
PedidoController ──> PedidoService ──> ConsultaCep (CepProxy, @Primary)
     (HTTP)         (regras, log)          └─> ViaCepAdapter / BrasilApiAdapter
                        │
                        ├─> CalculoFrete (Strategy escolhida pelo tipo)
                        ├─> Pedido ──> EstadoPedido (State)
                        └─> PedidoRepository / ProdutoRepository (JPA)
```

- **Adapter e Proxy:** o `PedidoService` depende de `ConsultaCep`; recebe o `CepProxy`, que valida, cacheia e delega ao Adapter. O teste confirma que dois pedidos com o mesmo CEP chamam o provedor uma vez só.
- **Singleton:** service e controller usam `AppLogger.getInstance()` para criação, frete calculado, transições e recusas.
- **CRUD:** `/produtos` continua igual; o pedido só lê o produto.

## SOLID aplicado

- **S (Responsabilidade única):** controller só traduz HTTP; service orquestra; cada estado e cada frete tem uma regra só.
- **O (Aberto/fechado):** novo frete ou novo estado = nova classe, sem alterar `PedidoService`.
- **L (Substituição de Liskov):** qualquer `CalculoFrete` ou `EstadoPedido` pode ser usado no lugar de outro; qualquer `ConsultaCep` (Proxy ou Adapter) também.
- **I (Segregação de interfaces):** `CalculoFrete` tem só o necessário para calcular; `ConsultaCep` só busca.
- **D (Inversão de dependência):** o service depende das interfaces `ConsultaCep` e `CalculoFrete`, injetadas pelo Spring, não de `ViaCepAdapter` ou `FretePadrao`.

## Melhorias obtidas

- Status inválido vira erro claro (409 com motivo) em vez de dado inconsistente no banco.
- Regras de frete testáveis isoladamente, sem subir o Spring (`CalculoFreteTest`, `PedidoEstadoTest`).
- O motivo dos erros aparece no corpo da resposta (`server.error.include-message=always`), inclusive os do Proxy de CEP.

## Dificuldades encontradas

- O `CepProxy` não compilava: a regex `"\d{8}"` é um escape inválido em Java. Trocada por `"[0-9]{8}"`.
- Testar sem internet: o teste HTTP substitui só o bean `provedorCep` (Adapter) por um mock, mantendo o Proxy real.
- Decidir onde mudar o status: ficou restrito ao pacote `pedido` (`mudarPara` não é público) para que só os estados façam a transição.
- Preço do produto pode mudar depois da venda: o pedido guarda uma cópia do nome e do preço.

## Respostas da atividade do Singleton

**Quem garante a instância única em cada versão?**
- `AppLogger` (clássico): o próprio código — construtor privado + `getInstance()` com `volatile` e dupla checagem em `synchronized`.
- `SpringAppLogger` (`@Component`): o container Spring, que cria um único bean por contexto (escopo `singleton`). Nada impede um `new` manual.

**Quando o Singleton clássico ainda faz sentido?**
- Código fora do container Spring (utilitários estáticos, `main`, bibliotecas sem Spring) ou quando a instância única precisa valer para toda a JVM, não só para um contexto.

**Versão final: `AppLogger` (clássico).** Service e Controller usam `AppLogger.getInstance()`. Justificativa: é o foco do trabalho, não depende do Spring, não exige injeção em cada classe e garante instância única na JVM. Contra: mais difícil de mockar em testes; em projeto real, o bean Spring seria preferível por isso.
