package com.pcc.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.finance.entity.SyncOutbox;
import com.pcc.finance.integration.ServiceGateway;
import com.pcc.finance.repository.SyncOutboxRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Persistent sync-outbox behavior: persistence, dispatch, retry, permissions. */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinanceSyncOutboxTest {

    private static final String ACCOUNTANT = JwtTestHelper.token(Set.of("ACCOUNTANT"), Set.of(
            "RENT_COLLECTION_VIEW", "RENT_COLLECTION_CREATE", "REFUND_VIEW", "REFUND_CREATE",
            "RECEIPT_VIEW", "PAYMENT_VIEW"));
    private static final String MANAGER = JwtTestHelper.token(Set.of("MANAGER"), Set.of(
            "REFUND_VIEW", "REFUND_APPROVE", "PAYMENT_VIEW"));

    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();
    @MockitoBean ServiceGateway gateway;
    @Autowired SyncOutboxRepository outboxRepository;

    private static Long paymentId;
    private static String paymentRef;
    private static Long refundId;
    private static String refundRef;

    @Autowired
    void setup(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @BeforeEach
    void stubGateway() throws Exception {
        lenient().when(gateway.hasRelayToken()).thenReturn(true);
        lenient().when(gateway.propertyGet("/api/v1/rental-agreements/6/schedules"))
                .thenReturn(Optional.of(om.readTree(
                        "[{\"id\":20,\"agreementId\":6,\"dueDate\":\"2026-01-05\",\"amount\":100000,\"status\":\"PENDING\"}]")));
        lenient().when(gateway.propertyGet("/api/v1/rental-agreements/6"))
                .thenReturn(Optional.of(om.readTree(
                        "{\"id\":6,\"agreementNumber\":\"AGR-0006\",\"tenantName\":\"Sync Tenant\",\"propertyName\":\"Sync Plaza\"}")));
        lenient().when(gateway.propertyPost(anyString(), anyMap()))
                .thenReturn(new ServiceGateway.SyncCallResult(true, false, null));
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String token, Object body) throws Exception {
        return mvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType("application/json").content(om.writeValueAsString(body)));
    }

    @Test @Order(1)
    void postedPayment_enqueuesSuccessfulPaymentSync() throws Exception {
        JsonNode body = om.readTree(postJson("/api/v1/rent-collections/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 20L;
            public final Long parentId = 6L;
            public final String payerName = "Tenant Ltd";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("50000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
            public final String paymentReference = "RCPT-SYNC-1";
            public final String idempotencyKey = "key-sync-1";
        }).andReturn().getResponse().getContentAsString());
        paymentId = body.get("id").asLong();
        paymentRef = body.get("paymentRef").asText();

        SyncOutbox row = outboxRepository.findBySyncRef(paymentRef + "-A1").orElseThrow();
        assertThat(row.getSyncKind()).isEqualTo(SyncOutbox.Kind.PAYMENT);
        assertThat(row.getStatus()).isEqualTo(SyncOutbox.Status.SUCCESS);
        assertThat(row.getExternalService()).isEqualTo("PROPERTY");
        assertThat(row.getTargetId()).isEqualTo(20L);
        assertThat(row.getPayload()).contains("\"allocationRef\":\"" + paymentRef + "-A1\"");
    }

    @Test @Order(2)
    void transientFailure_leavesSyncRetryable() throws Exception {
        // downstream goes down
        when(gateway.propertyPost(anyString(), anyMap()))
                .thenReturn(new ServiceGateway.SyncCallResult(false, false, "Property unavailable"));

        JsonNode refund = om.readTree(postJson("/api/v1/refunds", ACCOUNTANT, new Object() {
            public final Long paymentId = FinanceSyncOutboxTest.paymentId;
            public final java.math.BigDecimal amount = new java.math.BigDecimal("20000");
            public final String reason = "Partial refund";
        }).andReturn().getResponse().getContentAsString());
        refundId = refund.get("id").asLong();
        refundRef = refund.get("refundRef").asText();

        postJson("/api/v1/refunds/" + refundId + "/approve", MANAGER, null)
                .andExpect(status().isOk());

        SyncOutbox row = outboxRepository.findBySyncRef(refundRef + "-1").orElseThrow();
        assertThat(row.getSyncKind()).isEqualTo(SyncOutbox.Kind.REVERSAL);
        assertThat(row.getStatus()).isEqualTo(SyncOutbox.Status.FAILED);
        assertThat(row.isPermanent()).isFalse();
        assertThat(row.getPayload()).contains("\"reversalRef\":\"" + refundRef + "-1\"")
                .contains("\"amount\":20000");
        assertThat(outboxRepository.findRetryable(10)).extracting(SyncOutbox::getId).contains(row.getId());
    }

    @Test @Order(3)
    void manualRetry_requiresApprover_andSucceedsWhenServiceReturns() throws Exception {
        SyncOutbox row = outboxRepository.findBySyncRef(refundRef + "-1").orElseThrow();
        // downstream still down (BeforeEach re-stubs to success)
        when(gateway.propertyPost(anyString(), anyMap()))
                .thenReturn(new ServiceGateway.SyncCallResult(false, false, "Property unavailable"));
        // accountant lacks REFUND_APPROVE -> 403
        mvc.perform(post("/api/v1/sync-records/" + row.getId() + "/retry")
                        .header("Authorization", "Bearer " + ACCOUNTANT))
                .andExpect(status().isForbidden());
        // still failing -> stays FAILED
        postJson("/api/v1/sync-records/" + row.getId() + "/retry", MANAGER, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));
        // service back online -> manual retry confirms success
        when(gateway.propertyPost(anyString(), anyMap()))
                .thenReturn(new ServiceGateway.SyncCallResult(true, false, null));
        postJson("/api/v1/sync-records/" + row.getId() + "/retry", MANAGER, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        // finance state unchanged by retries — single allocation adjustment only
        mvc.perform(get("/api/v1/rent-collections/targets/20")
                        .header("Authorization", "Bearer " + ACCOUNTANT))
                .andExpect(jsonPath("$.paidAmount").value(30000));
    }

    @Test @Order(4)
    void permanentFailure_isNotAutoRetryable() throws Exception {
        when(gateway.propertyPost(anyString(), anyMap()))
                .thenReturn(new ServiceGateway.SyncCallResult(false, true, "HTTP 422 — rejected"));

        JsonNode refund = om.readTree(postJson("/api/v1/refunds", ACCOUNTANT, new Object() {
            public final Long paymentId = FinanceSyncOutboxTest.paymentId;
            public final java.math.BigDecimal amount = new java.math.BigDecimal("5000");
            public final String reason = "Second refund";
        }).andReturn().getResponse().getContentAsString());
        postJson("/api/v1/refunds/" + refund.get("id").asLong() + "/approve", MANAGER, null)
                .andExpect(status().isOk());

        SyncOutbox row = outboxRepository.findBySyncRef(refund.get("refundRef").asText() + "-1").orElseThrow();
        assertThat(row.getStatus()).isEqualTo(SyncOutbox.Status.FAILED);
        assertThat(row.isPermanent()).isTrue();
        assertThat(outboxRepository.findRetryable(10)).extracting(SyncOutbox::getId).doesNotContain(row.getId());
    }

    @Test @Order(5)
    void reversal_enqueuesPerAllocationReversal() throws Exception {
        when(gateway.propertyPost(anyString(), anyMap()))
                .thenReturn(new ServiceGateway.SyncCallResult(true, false, null));

        // second payment on the same schedule -> independent allocation
        JsonNode p2 = om.readTree(postJson("/api/v1/rent-collections/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 20L;
            public final Long parentId = 6L;
            public final String payerName = "Tenant Ltd";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("25000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
            public final String paymentReference = "RCPT-SYNC-2";
            public final String idempotencyKey = "key-sync-2";
        }).andReturn().getResponse().getContentAsString());
        String p2Ref = p2.get("paymentRef").asText();

        postJson("/api/v1/reversals", MANAGER, new Object() {
            public final Long paymentId = p2.get("id").asLong();
            public final String reason = "Erroneous posting";
        }).andExpect(status().isOk());

        SyncOutbox row = outboxRepository.findBySyncRef("REV-" + p2Ref + "-1").orElseThrow();
        assertThat(row.getSyncKind()).isEqualTo(SyncOutbox.Kind.REVERSAL);
        assertThat(row.getPayload()).contains("\"allocationRef\":\"" + p2Ref + "-A1\"")
                .contains("\"amount\":25000");
        // reversal only touches p2's allocation: first payment's 25k net remains
        mvc.perform(get("/api/v1/rent-collections/targets/20")
                        .header("Authorization", "Bearer " + ACCOUNTANT))
                .andExpect(jsonPath("$.paidAmount").value(25000));
    }
}
