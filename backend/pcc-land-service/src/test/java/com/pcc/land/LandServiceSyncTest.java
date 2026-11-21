package com.pcc.land;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Allocation-level finance payment + reversal sync contract used by Finance
 * Service for land acquisitions and sales.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LandServiceSyncTest {

    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private String officer, manager, sync;
    private static long parcelId, acqId, saleId;

    @Autowired
    void setup(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        officer = "Bearer " + JwtTestHelper.token(Set.of("LAND_OFFICER"), Set.of(
                "LAND_VIEW", "LAND_CREATE", "LAND_ACQUISITION_CREATE", "LAND_SALE_REQUEST"));
        manager = "Bearer " + JwtTestHelper.token(Set.of("MANAGER"), Set.of(
                "LAND_VIEW", "LAND_ACQUISITION_APPROVE", "LAND_SALE_APPROVE"));
        sync = "Bearer " + JwtTestHelper.token(Set.of("FINANCE_SERVICE"), Set.of("LAND_PAYMENT_SYNC"));
    }

    private JsonNode read(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    private MvcResult postJson(String path, String token, Object body) throws Exception {
        return mvc.perform(post(path).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body instanceof String s ? s : mapper.writeValueAsString(body))).andReturn();
    }

    @Test
    @Order(1)
    void setup_acquisitionAndSale() throws Exception {
        parcelId = read(postJson("/api/v1/land-parcels", officer,
                "{\"name\":\"Sync Test Parcel\",\"classification\":\"RESIDENTIAL\"," +
                "\"areaValue\":2000,\"unit\":\"SQUARE_METRE\",\"region\":\"Centre\"}"))
                .get("id").asLong();

        JsonNode acqBody = read(postJson("/api/v1/land-acquisitions", officer,
                ("{\"parcelId\":%d,\"sellerName\":\"Sync Seller\",\"sellerContact\":\"+237 6 00 00\"," +
                 "\"acquisitionDate\":\"%s\",\"purchasePrice\":1000000}")
                        .formatted(parcelId, LocalDate.now())));
        assertThat(acqBody.has("id")).as("acquisition create failed: " + acqBody).isTrue();
        acqId = acqBody.get("id").asLong();
        postJson("/api/v1/land-acquisitions/" + acqId + "/approve", manager, "{}");

        JsonNode saleBody = read(postJson("/api/v1/land-sales", officer,
                ("{\"parcelId\":%d,\"buyerName\":\"Sync Buyer\",\"buyerContact\":\"+237 6 99 99\"," +
                "\"proposedPrice\":2000000}").formatted(parcelId)));
        assertThat(saleBody.has("id")).as("sale create failed: " + saleBody).isTrue();
        saleId = saleBody.get("id").asLong();
        postJson("/api/v1/land-sales/" + saleId + "/submit", officer, "{}");
        postJson("/api/v1/land-sales/" + saleId + "/approve", manager,
                "{\"agreedPrice\":2000000}");
        assertThat(acqId).isPositive();
        assertThat(saleId).isPositive();
    }

    @Test
    @Order(2)
    void syncEndpoints_secured() throws Exception {
        mvc.perform(post("/api/v1/land-acquisitions/" + acqId + "/payment-reversals")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/land-acquisitions/" + acqId + "/payment-reversals")
                        .header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentRef\":\"P\",\"allocationRef\":\"A\",\"reversalRef\":\"R\",\"amount\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void acquisition_paymentAndPartialReversal() throws Exception {
        JsonNode r = read(postJson("/api/v1/land-acquisitions/" + acqId + "/finance-payments", sync,
                java.util.Map.of("paymentRef", "PAY-8001", "allocationRef", "PAY-8001-A1", "amount", 1000000)));
        assertThat(r.has("financeStatus")).as("finance-payments failed: " + r).isTrue();
        assertThat(r.get("financeStatus").asText()).isEqualTo("PAID");

        r = read(postJson("/api/v1/land-acquisitions/" + acqId + "/payment-reversals", sync,
                java.util.Map.of("paymentRef", "PAY-8001", "allocationRef", "PAY-8001-A1",
                        "reversalRef", "RFD-1001-1", "amount", 250000,
                        "reversalDate", LocalDate.now().toString(), "reason", "Partial refund")));
        assertThat(r.get("paidAmount").asLong()).isEqualTo(750000L);
        assertThat(r.get("financeStatus").asText()).isEqualTo("PARTIALLY_PAID");

        // duplicate reversalRef -> no-op
        r = read(postJson("/api/v1/land-acquisitions/" + acqId + "/payment-reversals", sync,
                java.util.Map.of("paymentRef", "PAY-8001", "allocationRef", "PAY-8001-A1",
                        "reversalRef", "RFD-1001-1", "amount", 250000,
                        "reversalDate", LocalDate.now().toString())));
        assertThat(r.get("paidAmount").asLong()).isEqualTo(750000L);

        // over-refund rejected
        mvc.perform(post("/api/v1/land-acquisitions/" + acqId + "/payment-reversals")
                        .header("Authorization", sync)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentRef\":\"PAY-8001\",\"allocationRef\":\"PAY-8001-A1\"," +
                                "\"reversalRef\":\"RFD-1999-1\",\"amount\":800000}"))
                .andExpect(status().isUnprocessableEntity());

        // full remainder reversal -> UNPAID
        r = read(postJson("/api/v1/land-acquisitions/" + acqId + "/payment-reversals", sync,
                java.util.Map.of("paymentRef", "PAY-8001", "allocationRef", "PAY-8001-A1",
                        "reversalRef", "REV-PAY-8001-1", "amount", 750000,
                        "reversalDate", LocalDate.now().toString())));
        assertThat(r.get("paidAmount").asLong()).isZero();
        assertThat(r.get("financeStatus").asText()).isEqualTo("UNPAID");
        // parcel ownership/workflow status untouched by financial reversal
        assertThat(r.get("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    @Order(4)
    void sale_paymentAndFullReversal() throws Exception {
        JsonNode r = read(postJson("/api/v1/land-sales/" + saleId + "/finance-payments", sync,
                java.util.Map.of("paymentRef", "PAY-8002", "allocationRef", "PAY-8002-A1", "amount", 2000000)));
        assertThat(r.has("financeStatus")).as("finance-payments failed: " + r).isTrue();
        assertThat(r.get("financeStatus").asText()).isEqualTo("PAID");

        r = read(postJson("/api/v1/land-sales/" + saleId + "/payment-reversals", sync,
                java.util.Map.of("paymentRef", "PAY-8002", "allocationRef", "PAY-8002-A1",
                        "reversalRef", "REV-PAY-8002-1", "amount", 2000000,
                        "reversalDate", LocalDate.now().toString(), "reason", "Payment reversed")));
        assertThat(r.get("paidAmount").asLong()).isZero();
        assertThat(r.get("financeStatus").asText()).isEqualTo("UNPAID");
        // legal/sale workflow status unchanged
        assertThat(r.get("status").asText()).isEqualTo("APPROVED");
    }
}
