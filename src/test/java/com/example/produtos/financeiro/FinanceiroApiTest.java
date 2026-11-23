package com.example.produtos.financeiro;

import com.example.produtos.ConsultaCep;
import com.example.produtos.Endereco;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fluxo HTTP completo do financeiro, mais a prova de que o que já existia continua
 * funcionando: CRUD de produtos e consulta de CEP pelo Proxy real (só o Adapter
 * externo é simulado, sem rede).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FinanceiroApiTest {
    @Autowired MockMvc mvc;

    @MockBean(name = "provedorCep")
    ConsultaCep adapter;

    private final LocalDate hoje = LocalDate.now();

    private Integer criar(String json) throws Exception {
        String r = mvc.perform(post("/lancamentos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.id");
    }

    @Test
    void despesaPagaComAtrasoEstornadaECancelada() throws Exception {
        Integer id = criar("{\"tipo\":\"DESPESA\",\"descricao\":\"Aluguel\",\"categoria\":\"Fixas\",\"valor\":1000,"
                + "\"vencimento\":\"" + hoje.minusDays(10) + "\",\"regraPagamento\":\"MULTA_JUROS\"}");

        mvc.perform(get("/lancamentos/" + id)).andExpect(jsonPath("$.vencido").value(true));

        mvc.perform(post("/lancamentos/" + id + "/baixar").param("data", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.ajuste").value(23.30))
                .andExpect(jsonPath("$.valorPago").value(1023.30));

        mvc.perform(post("/lancamentos/" + id + "/baixar")).andExpect(status().isConflict());
        mvc.perform(post("/lancamentos/" + id + "/cancelar")).andExpect(status().isConflict());
        mvc.perform(post("/lancamentos/" + id + "/estornar")).andExpect(jsonPath("$.status").value("PENDENTE"));
        mvc.perform(post("/lancamentos/" + id + "/cancelar")).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(post("/lancamentos/" + id + "/baixar")).andExpect(status().isConflict());
    }

    @Test
    void resumoSomaPendentesERealizados() throws Exception {
        Integer receita = criar("{\"tipo\":\"RECEITA\",\"descricao\":\"Venda balcão\",\"valor\":500,"
                + "\"vencimento\":\"" + hoje + "\",\"regraPagamento\":\"DESCONTO_PONTUALIDADE\"}");
        criar("{\"tipo\":\"RECEITA\",\"descricao\":\"Venda a prazo\",\"valor\":300,\"vencimento\":\"" + hoje.plusDays(30) + "\"}");
        criar("{\"tipo\":\"DESPESA\",\"descricao\":\"Fornecedor\",\"valor\":200,\"vencimento\":\"" + hoje.minusDays(1) + "\"}");

        mvc.perform(post("/lancamentos/" + receita + "/baixar"))
                .andExpect(jsonPath("$.valorPago").value(475.00));

        mvc.perform(get("/lancamentos/resumo"))
                .andExpect(jsonPath("$.aReceber").value(300))
                .andExpect(jsonPath("$.aPagar").value(200))
                .andExpect(jsonPath("$.recebido").value(475.00))
                .andExpect(jsonPath("$.saldo").value(475.00))
                .andExpect(jsonPath("$.vencidos").value(1));

        mvc.perform(get("/lancamentos").param("status", "PAGO")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void recusaDadosInvalidos() throws Exception {
        String base = "\"descricao\":\"x\",\"vencimento\":\"" + hoje + "\"";
        mvc.perform(post("/lancamentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DESPESA\"," + base + ",\"valor\":0}")).andExpect(status().isBadRequest());
        mvc.perform(post("/lancamentos").contentType(MediaType.APPLICATION_JSON)
                .content("{" + base + ",\"valor\":10}")).andExpect(status().isBadRequest());
        mvc.perform(post("/lancamentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DESPESA\"," + base + ",\"valor\":10,\"regraPagamento\":\"PIX_TURBO\"}")).andExpect(status().isBadRequest());
        Integer id = criar("{\"tipo\":\"DESPESA\"," + base + ",\"valor\":10}");
        mvc.perform(post("/lancamentos/" + id + "/baixar").param("data", hoje.plusDays(1).toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/lancamentos/999")).andExpect(status().isNotFound());
    }

    @Test
    void crudEConsultaDeCepContinuamFuncionando() throws Exception {
        String p = mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Caneta\",\"preco\":3.5}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer produto = JsonPath.read(p, "$.id");
        mvc.perform(get("/produtos/" + produto)).andExpect(jsonPath("$.nome").value("Caneta"));
        mvc.perform(delete("/produtos/" + produto)).andExpect(status().isNoContent());

        when(adapter.buscar("85810000")).thenReturn(new Endereco("85810000", "Rua A", "Centro", "Cascavel", "PR"));
        mvc.perform(get("/cep/85810000")).andExpect(jsonPath("$.uf").value("PR"));
        mvc.perform(get("/cep/85810000")).andExpect(jsonPath("$.cidade").value("Cascavel"));
        verify(adapter, times(1)).buscar("85810000"); // segunda vez veio do cache do Proxy
        mvc.perform(get("/cep/123")).andExpect(status().isBadRequest());
    }
}
