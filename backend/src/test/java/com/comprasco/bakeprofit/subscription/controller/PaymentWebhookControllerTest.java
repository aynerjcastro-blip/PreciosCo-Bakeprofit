package com.comprasco.bakeprofit.subscription.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.comprasco.bakeprofit.subscription.exception.InvalidWebhookSignatureException;
import com.comprasco.bakeprofit.subscription.service.OrderWebhookService;

import tools.jackson.databind.json.JsonMapper;

class PaymentWebhookControllerTest {

    private static final String ORDER_ID = "ORDTST01EXAMPLEORDER";
    private static final String SECRET = "local-only-test-webhook-signing-secret";
    private static final String REQUEST_ID = "f47a082b-87c0-42a5-9ea5-f474242095cb";
    private final OrderWebhookService service = mock(OrderWebhookService.class);
    private final PaymentWebhookController controller = new PaymentWebhookController(service, new JsonMapper(), SECRET);

    @Test
    void rejectsAnInvalidSignatureBeforeReadingTheBody() throws Exception {
        var request = signedRequest(ORDER_ID);
        request.removeHeader("x-signature");
        request.addHeader("x-signature", "ts=1,v1=invalid");

        assertThatThrownBy(() -> controller.receive(request, "not even JSON"))
            .isInstanceOf(InvalidWebhookSignatureException.class);
        verifyNoInteractions(service);
    }

    @Test
    void ignoresOtherTopicsAfterVerifyingTheSignature() throws Exception {
        assertThat(controller.receive(signedRequest(ORDER_ID), notification("payment", ORDER_ID))
            .getStatusCode().value()).isEqualTo(200);
        verifyNoInteractions(service);
    }

    @Test
    void refusesAnOrderIdInTheBodyThatDoesNotMatchTheSignature() throws Exception {
        assertThatThrownBy(() -> controller.receive(signedRequest(ORDER_ID), notification("order", "ORDOTHER")))
            .isInstanceOf(InvalidWebhookSignatureException.class);
        verifyNoInteractions(service);
    }

    @Test
    void dispatchesASignedOrderUsingItsSignedId() throws Exception {
        assertThat(controller.receive(signedRequest(ORDER_ID), notification("order", ORDER_ID))
            .getStatusCode().value()).isEqualTo(200);
        verify(service).process(ORDER_ID);
    }

    private String notification(String type, String id) {
        return """
            {"type":"%s","data":{"id":"%s"}}
            """.formatted(type, id);
    }

    private MockHttpServletRequest signedRequest(String id) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String manifest = "id:%s;request-id:%s;ts:%s;".formatted(id.toLowerCase(Locale.ROOT), REQUEST_ID, timestamp);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

        var request = new MockHttpServletRequest("POST", "/api/payments/webhook/mercadopago");
        request.setQueryString("data.id=" + id);
        request.addHeader("x-request-id", REQUEST_ID);
        request.addHeader("x-signature", "ts=%s,v1=%s".formatted(timestamp, signature));
        return request;
    }
}
