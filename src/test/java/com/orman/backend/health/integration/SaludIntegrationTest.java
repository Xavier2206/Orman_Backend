package com.orman.backend.health.integration;

import com.orman.backend.health.service.SaludServiceImpl;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

// HTTP real: el contenedor servlet elimina el cuerpo de HEAD, algo que MockMvc no reproduce.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SaludIntegrationTest {

    @Value("${local.server.port}") private int port;
    @MockitoSpyBean private SaludServiceImpl saludService;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void respondePublicamenteCuandoPostgresEjecutaLaConsultaDeSalud() throws Exception {
        HttpResponse<String> response = consultar("GET", "/api/v1/salud");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"estado\":\"UP\",\"baseDatos\":\"UP\"}");
        verify(saludService).comprobarBaseDatos();
    }

    @Test
    void headRespondePublicamenteSinCuerpoCuandoPostgresEjecutaLaConsultaDeSalud() throws Exception {
        HttpResponse<String> response = consultar("HEAD", "/api/v1/salud");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
        verify(saludService).comprobarBaseDatos();
    }

    @Test
    void getYHeadResponden503CuandoFallaLaComprobacionDeBaseDeDatos() throws Exception {
        doReturn(false).when(saludService).comprobarBaseDatos();

        HttpResponse<String> get = consultar("GET", "/api/v1/salud");
        assertThat(get.statusCode()).isEqualTo(503);
        assertThat(get.body()).isEqualTo("{\"estado\":\"DOWN\",\"baseDatos\":\"DOWN\"}");

        HttpResponse<String> head = consultar("HEAD", "/api/v1/salud");
        assertThat(head.statusCode()).isEqualTo(503);
        assertThat(head.body()).isEmpty();
    }

    @Test
    void recursoProtegidoRechazaGetYHeadSinAutenticacion() throws Exception {
        assertThat(consultar("GET", "/api/v1/auth/sessions").statusCode()).isEqualTo(401);
        assertThat(consultar("HEAD", "/api/v1/auth/sessions").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> consultar(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
