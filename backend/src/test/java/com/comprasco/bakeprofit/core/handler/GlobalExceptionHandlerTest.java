package com.comprasco.bakeprofit.core.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.io.IOException;
import java.net.SocketException;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/products/search";
    private static final String CLIENT_ABORT_MESSAGE =
            "Se ha anulado una conexión establecida por el software en su equipo host";
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ListAppender<ILoggingEvent> capturedLogs = new ListAppender<>();
    private Level previousLevel;
    private MockHttpServletRequest request;

    @BeforeEach
    void captureHandlerLogs() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        capturedLogs.start();
        logger.addAppender(capturedLogs);
        request = new MockHttpServletRequest("GET", REQUEST_PATH);
    }

    @AfterEach
    void restoreHandlerLogs() {
        logger.detachAppender(capturedLogs);
        capturedLogs.stop();
        logger.setLevel(previousLevel);
    }

    @Test
    void desconexionAsyncSeRegistraEnDebugSinRespuestaNiStackTrace() {
        var exception = new AsyncRequestNotUsableException("ServletResponse failed to flushBuffer",
                new ClientAbortException(new IOException(CLIENT_ABORT_MESSAGE)));

        assertThat(handler.handleGeneric(exception, request)).isNull();
        assertThat(capturedLogs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).contains("Cliente desconectado", REQUEST_PATH);
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    void reconoceClientAbortAnidadoSinDependerDelIdiomaDelMensaje() {
        var exception = new ServletException("Fallo al escribir",
                new ClientAbortException(new IOException(CLIENT_ABORT_MESSAGE)));

        assertThat(handler.handleGeneric(exception, request)).isNull();
        assertThat(capturedLogs.list).allSatisfy(event ->
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
    }

    @Test
    void reconoceBrokenPipeDeLaConexionDelCliente() {
        assertThat(handler.handleGeneric(new IOException("Broken pipe"), request)).isNull();
        assertThat(capturedLogs.list).singleElement().satisfies(event ->
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
    }

    @Test
    void errorDeAplicacionConserva500YRegistroDeError() {
        var response = handler.handleGeneric(new IllegalStateException("Fallo de aplicación"), request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Error interno del servidor");
        assertThat(response.getBody().getPath()).isEqualTo(REQUEST_PATH);
        assertThat(capturedLogs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getThrowableProxy()).isNotNull();
        });
    }

    @Test
    void errorDeLecturaDeArchivoNoSeConfundeConDesconexion() {
        var response = handler.handleGeneric(new IOException("Error leyendo archivo de configuración"), request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(capturedLogs.list).singleElement().satisfies(event ->
                assertThat(event.getLevel()).isEqualTo(Level.ERROR));
    }

    @Test
    void conexionCaidaDeLaBaseNoSeOcultaComoCancelacionDelNavegador() {
        var exception = new DataAccessResourceFailureException("Base de datos inaccesible",
                new SocketException("Broken pipe"));

        var response = handler.handleGeneric(exception, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(capturedLogs.list).singleElement().satisfies(event ->
                assertThat(event.getLevel()).isEqualTo(Level.ERROR));
    }

    @Test
    void conexionCaidaDeUnaApiExternaConservaErrorDelServidor() {
        var exception = new ResourceAccessException("API externa inaccesible",
                new IOException("Connection reset by peer"));

        var response = handler.handleGeneric(exception, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(capturedLogs.list).singleElement().satisfies(event ->
                assertThat(event.getLevel()).isEqualTo(Level.ERROR));
    }

    @Test
    void springMvcNoIntentaGenerarUnJsonParaLaConexionCerrada() throws Exception {
        var mockMvc = MockMvcBuilders.standaloneSetup(new DisconnectedController())
                .setControllerAdvice(handler).build();

        mockMvc.perform(get(REQUEST_PATH))
                .andExpect(content().string(""))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(AsyncRequestNotUsableException.class));
        assertThat(capturedLogs.list).noneSatisfy(event ->
                assertThat(event.getLevel()).isEqualTo(Level.ERROR));
    }

    @RestController
    static class DisconnectedController {
        @GetMapping(REQUEST_PATH)
        String search() throws AsyncRequestNotUsableException {
            throw new AsyncRequestNotUsableException("La respuesta ya no puede usarse");
        }
    }
}
