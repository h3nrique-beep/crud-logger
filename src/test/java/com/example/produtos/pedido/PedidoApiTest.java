package com.example.produtos.pedido;

import com.example.produtos.ConsultaCep;
import com.example.produtos.Endereco;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fluxo HTTP completo: CRUD de produto + pedido passando pelo CepProxy real. Só o Adapter externo é simulado (sem rede). */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoApiTest {
    @Autowired MockMvc mvc;

    @MockBean(name = "provedorCep")
    ConsultaCep adapter;

    @Test
    void pedidoDoCadastroAteEntrega() throws Exception {
        when(adapter.buscar("85810000")).thenReturn(new Endereco("85810000", "Rua A", "Centro", "Cascavel", "PR"));

        String produto = mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Caneta\",\"preco\":10.0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer produtoId = JsonPath.read(produto, "$.id");

        String pedido = mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":" + produtoId + ",\"quantidade\":3,\"cep\":\"85810000\",\"tipoFrete\":\"PADRAO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(30.0))
                .andExpect(jsonPath("$.frete").value(15.0))
                .andExpect(jsonPath("$.total").value(45.0))
                .andExpect(jsonPath("$.status").value("CRIADO"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(pedido, "$.id");

        mvc.perform(post("/pedidos/" + id + "/enviar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + id + "/pagar")).andExpect(jsonPath("$.status").value("PAGO"));
        mvc.perform(post("/pedidos/" + id + "/enviar")).andExpect(jsonPath("$.status").value("ENVIADO"));
        mvc.perform(post("/pedidos/" + id + "/cancelar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + id + "/entregar")).andExpect(jsonPath("$.status").value("ENTREGUE"));

        // segundo pedido com o mesmo CEP: o Proxy responde do cache, o Adapter foi chamado uma vez só
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":" + produtoId + ",\"quantidade\":1,\"cep\":\"85810000\",\"tipoFrete\":\"RETIRADA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frete").value(0.0));
        verify(adapter, times(1)).buscar("85810000");

        // produto pode ser removido no CRUD mesmo com pedidos (o pedido guarda cópia)
        mvc.perform(delete("/produtos/" + produtoId)).andExpect(status().isNoContent());
        mvc.perform(get("/pedidos/" + id)).andExpect(jsonPath("$.produtoNome").value("Caneta"));
    }

    @Test
    void recusaDadosInvalidos() throws Exception {
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":1,\"quantidade\":0,\"cep\":\"85810000\",\"tipoFrete\":\"PADRAO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":1,\"quantidade\":1,\"cep\":\"85810000\",\"tipoFrete\":\"DRONE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":999,\"quantidade\":1,\"cep\":\"85810000\",\"tipoFrete\":\"PADRAO\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pedidos/999")).andExpect(status().isNotFound());
    }
}
