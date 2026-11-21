package com.pcc.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Allocation-level payment + reversal sync contract used by Finance Service:
 * idempotent payment records, validated reversals, status recomputation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RentScheduleSyncTest {

    private static final Set<String> SETUP_PERMS = Set.of(
            "PROPERTY_CREATE", "BUILDING_CREATE", "APARTMENT_CREATE",
            "TENANT_MANAGE", "RENTAL_MANAGE", "RENTAL_VIEW");

    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private static String officer, sync;
    private static Long scheduleId;

    @BeforeAll
    static void tokens() {
        officer = JwtTestHelper.token(Set.of("PROPERTY_OFFICER"), SETUP_PERMS);
        sync = JwtTestHelper.token(Set.of("FINANCE_SERVICE"), Set.of("RENT_SCHEDULE_PAYMENT_SYNC"));
    }

    private JsonNode read(ResultActions r) throws Exception {
        return mapper.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private ResultActions postJson(String path, String token, Object body) throws Exception {
        return mvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private JsonNode postOk(String path, String token, Object body) throws Exception {
        return read(postJson(path, token, body).andExpect(status().isOk()));
    }

    private JsonNode postCreated(String path, String token, Object body) throws Exception {
        return read(postJson(path, token, body).andExpect(status().isCreated()));
    }

    private Map<String, Object> alloc(String pay, String alloc, long amount) {
        return Map.of("paymentRef", pay, "allocationRef", alloc, "amount", amount);
    }

    private Map<String, Object> reversal(String pay, String alloc, String rev, long amount) {
        return Map.of("paymentRef", pay, "allocationRef", alloc, "reversalRef", rev,
                "amount", amount, "reversalDate", LocalDate.now().toString(),
                "reason", "test", "actor", "accountant");
    }

    @Test
    @Order(1)
    void setup_scheduleChain() throws Exception {
        Long propertyId = postCreated("/api/v1/properties", officer, Map.of(
                "name", "Sync Test Block", "propertyType", "COMMERCIAL", "city", "Douala"))
                .get("id").asLong();
        Long buildingId = postCreated("/api/v1/buildings", officer, Map.of(
                "propertyId", propertyId, "name", "S1")).get("id").asLong();
        Long apartmentId = postCreated("/api/v1/apartments", officer, Map.of(
                "buildingId", buildingId, "apartmentNumber", "S-1", "monthlyRent", 100000))
                .get("id").asLong();
        Long tenantId = postCreated("/api/v1/tenants", officer, Map.of(
                "tenantType", "INDIVIDUAL", "name", "Sync Tenant")).get("id").asLong();
        Long agreementId = postCreated("/api/v1/rental-agreements", officer, Map.of(
                "apartmentId", apartmentId, "tenantId", tenantId,
                "startDate", LocalDate.now().plusYears(1).toString(),
                "endDate", LocalDate.now().plusYears(1).plusMonths(2).toString(),
                "monthlyRent", 100000, "paymentFrequency", "MONTHLY", "rentDueDay", 5))
                .get("id").asLong();
        JsonNode schedules = read(mvc.perform(get("/api/v1/rental-agreements/" + agreementId + "/schedules")
                        .header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk()));
        scheduleId = schedules.get(0).get("id").asLong();
        assertThat(scheduleId).isNotNull();
    }

    @Test
    @Order(2)
    void paymentSync_endpointsSecured() throws Exception {
        mvc.perform(post("/api/v1/rent-schedules/" + scheduleId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/rent-schedules/" + scheduleId + "/payments")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentRef\":\"P\",\"allocationRef\":\"A\",\"amount\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void recordAllocation_paysSchedule() throws Exception {
        JsonNode r = postOk("/api/v1/rent-schedules/" + scheduleId + "/payments", sync,
                alloc("PAY-9001", "PAY-9001-A1", 100000));
        assertThat(r.get("paidAmount").asLong()).isEqualTo(100000L);
        assertThat(r.get("status").asText()).isEqualTo("PAID");
    }

    @Test
    @Order(4)
    void allocationReplay_isIdempotent() throws Exception {
        JsonNode r = postOk("/api/v1/rent-schedules/" + scheduleId + "/payments", sync,
                alloc("PAY-9001", "PAY-9001-A1", 100000));
        assertThat(r.get("paidAmount").asLong()).isEqualTo(100000L);
    }

    @Test
    @Order(5)
    void partialReversal_reducesPaidAndStatus() throws Exception {
        JsonNode r = postOk("/api/v1/rent-schedules/" + scheduleId + "/payment-reversals", sync,
                reversal("PAY-9001", "PAY-9001-A1", "RFD-0001-1", 40000));
        assertThat(r.get("paidAmount").asLong()).isEqualTo(60000L);
        assertThat(r.get("status").asText()).isEqualTo("PARTIAL");
    }

    @Test
    @Order(6)
    void duplicateReversal_isIdempotent() throws Exception {
        JsonNode r = postOk("/api/v1/rent-schedules/" + scheduleId + "/payment-reversals", sync,
                reversal("PAY-9001", "PAY-9001-A1", "RFD-0001-1", 40000));
        assertThat(r.get("paidAmount").asLong()).isEqualTo(60000L);
    }

    @Test
    @Order(7)
    void overReversal_isRejected() throws Exception {
        postJson("/api/v1/rent-schedules/" + scheduleId + "/payment-reversals", sync,
                reversal("PAY-9001", "PAY-9001-A1", "RFD-9999-1", 70000))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(8)
    void wrongPaymentRef_isRejected() throws Exception {
        postJson("/api/v1/rent-schedules/" + scheduleId + "/payment-reversals", sync,
                reversal("PAY-WRONG", "PAY-9001-A1", "RFD-9998-1", 1000))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(9)
    void unknownAllocation_is404() throws Exception {
        postJson("/api/v1/rent-schedules/" + scheduleId + "/payment-reversals", sync,
                reversal("PAY-9001", "PAY-GHOST", "RFD-9997-1", 1000))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    void fullReversal_restoresPending() throws Exception {
        JsonNode r = postOk("/api/v1/rent-schedules/" + scheduleId + "/payment-reversals", sync,
                reversal("PAY-9001", "PAY-9001-A1", "REV-PAY-9001-1", 60000));
        assertThat(r.get("paidAmount").asLong()).isZero();
        assertThat(r.get("status").asText()).isEqualTo("PENDING");
    }
}
