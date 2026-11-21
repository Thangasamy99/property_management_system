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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertyServiceIntegrationTest {

    private static final Set<String> OFFICER_PERMS = Set.of(
            "PROPERTY_VIEW", "PROPERTY_CREATE", "PROPERTY_UPDATE", "PROPERTY_DELETE",
            "BUILDING_VIEW", "BUILDING_CREATE", "BUILDING_UPDATE",
            "APARTMENT_VIEW", "APARTMENT_CREATE", "APARTMENT_UPDATE",
            "TENANT_VIEW", "TENANT_MANAGE", "RENTAL_VIEW", "RENTAL_MANAGE",
            "MAINTENANCE_VIEW", "MAINTENANCE_MANAGE");

    @Autowired
    MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    private static String officer;
    private static Long propertyId;
    private static Long buildingId;
    private static Long apartmentId;
    private static Long tenantId;
    private static Long agreementId;
    private static Long maintenanceId;

    @BeforeAll
    static void tokens() {
        officer = JwtTestHelper.token(Set.of("PROPERTY_OFFICER"), OFFICER_PERMS);
    }

    private JsonNode readJson(org.springframework.test.web.servlet.MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    // ---------- security ----------

    @Test
    @Order(1)
    void unauthenticated_isRejected() throws Exception {
        mvc.perform(get("/api/v1/properties")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void insufficientPermission_isForbidden() throws Exception {
        String landOfficer = JwtTestHelper.token(Set.of("LAND_OFFICER"), Set.of("LAND_VIEW"));
        mvc.perform(post("/api/v1/properties")
                        .header("Authorization", "Bearer " + landOfficer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"propertyType\":\"COMMERCIAL\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- properties ----------

    @Test
    @Order(3)
    void createProperty_generatesCode() throws Exception {
        var res = mvc.perform(post("/api/v1/properties")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bonanjo Office Block","propertyType":"COMMERCIAL",
                                 "city":"Douala","region":"Littoral","addressLine1":"Avenue de Gaulle",
                                 "acquisitionCost":250000000}"""))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = readJson(res);
        assertThat(body.get("code").asText()).isEqualTo("PROP-0001");
        assertThat(body.get("status").asText()).isEqualTo("ACTIVE");
        propertyId = body.get("id").asLong();
    }

    @Test
    @Order(4)
    void createProperty_validationFails_onMissingName() throws Exception {
        var res = mvc.perform(post("/api/v1/properties")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"propertyType\":\"COMMERCIAL\"}"))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(readJson(res).get("fieldErrors")).isNotNull();
    }

    @Test
    @Order(5)
    void listProperties_paginatedAndSearchable() throws Exception {
        var res = mvc.perform(get("/api/v1/properties?search=bonanjo")
                        .header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(res).get("content")).hasSize(1);
    }

    // ---------- buildings ----------

    @Test
    @Order(6)
    void createBuilding_generatesCode() throws Exception {
        var res = mvc.perform(post("/api/v1/buildings")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "propertyId", propertyId, "name", "Tower A",
                                "numberOfFloors", 6, "constructionYear", 2015))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = readJson(res);
        assertThat(body.get("code").asText()).isEqualTo("BLD-0001");
        buildingId = body.get("id").asLong();
    }

    @Test
    @Order(7)
    void createBuilding_unknownProperty_404() throws Exception {
        mvc.perform(post("/api/v1/buildings")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"propertyId\":9999,\"name\":\"Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- apartments ----------

    @Test
    @Order(8)
    void createApartment_thenDuplicateNumber_conflicts() throws Exception {
        String payload = mapper.writeValueAsString(Map.of(
                "buildingId", buildingId, "apartmentNumber", "A-01",
                "floorNumber", 1, "bedrooms", 2, "bathrooms", 1,
                "areaSqm", 65.5, "monthlyRent", 150000));
        var res = mvc.perform(post("/api/v1/apartments")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn();
        apartmentId = readJson(res).get("id").asLong();

        mvc.perform(post("/api/v1/apartments")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }

    // ---------- tenants ----------

    @Test
    @Order(9)
    void createTenant_generatesCode() throws Exception {
        var res = mvc.perform(post("/api/v1/tenants")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantType":"INDIVIDUAL","name":"Marie Ngono",
                                 "email":"marie@example.cm","phone":"+237699000111",
                                 "identificationType":"NATIONAL_ID","identificationNumber":"ID-778899"}"""))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = readJson(res);
        assertThat(body.get("code").asText()).isEqualTo("TNT-0001");
        tenantId = body.get("id").asLong();
    }

    // ---------- rental agreements ----------

    @Test
    @Order(10)
    void createAgreement_generatesSchedulesAndOccupiesApartment() throws Exception {
        var res = mvc.perform(post("/api/v1/rental-agreements")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "apartmentId", apartmentId, "tenantId", tenantId,
                                "startDate", LocalDate.now().toString(),
                                "endDate", LocalDate.now().plusYears(1).minusDays(1).toString(),
                                "monthlyRent", 150000, "deposit", 300000,
                                "paymentFrequency", "MONTHLY", "rentDueDay", 5,
                                "terms", List.of("Quiet hours 22h-6h", "No subletting")))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = readJson(res);
        assertThat(body.get("agreementNumber").asText()).isEqualTo("AGR-0001");
        assertThat(body.get("status").asText()).isEqualTo("ACTIVE");
        agreementId = body.get("id").asLong();

        var sched = mvc.perform(get("/api/v1/rental-agreements/" + agreementId + "/schedules")
                        .header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(sched).size()).isEqualTo(12);

        var apt = mvc.perform(get("/api/v1/apartments/" + apartmentId)
                        .header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(apt).get("status").asText()).isEqualTo("OCCUPIED");
    }

    @Test
    @Order(11)
    void overlappingAgreement_isRejected() throws Exception {
        mvc.perform(post("/api/v1/rental-agreements")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "apartmentId", apartmentId, "tenantId", tenantId,
                                "startDate", LocalDate.now().plusMonths(3).toString(),
                                "endDate", LocalDate.now().plusMonths(15).toString(),
                                "monthlyRent", 150000,
                                "paymentFrequency", "MONTHLY", "rentDueDay", 5))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(12)
    void terminateAgreement_freesApartmentAndRecordsHistory() throws Exception {
        var res = mvc.perform(post("/api/v1/rental-agreements/" + agreementId + "/terminate")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Tenant relocation\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(res).get("status").asText()).isEqualTo("TERMINATED");

        var apt = mvc.perform(get("/api/v1/apartments/" + apartmentId)
                        .header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(apt).get("status").asText()).isEqualTo("AVAILABLE");

        var hist = mvc.perform(get("/api/v1/rental-agreements/" + agreementId + "/history")
                        .header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(hist).size()).isGreaterThanOrEqualTo(2);
    }

    // ---------- maintenance ----------

    @Test
    @Order(13)
    void maintenance_lifecycle() throws Exception {
        var res = mvc.perform(post("/api/v1/maintenance")
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "apartmentId", apartmentId, "category", "PLUMBING",
                                "description", "Leaking kitchen tap", "priority", "HIGH"))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = readJson(res);
        assertThat(body.get("requestNumber").asText()).isEqualTo("MNT-0001");
        maintenanceId = body.get("id").asLong();

        var upd = mvc.perform(patch("/api/v1/maintenance/" + maintenanceId)
                        .header("Authorization", "Bearer " + officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"note\":\"Fixed\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode updated = readJson(upd);
        assertThat(updated.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(updated.get("completedAt").isNull()).isFalse();

        var hist = mvc.perform(get("/api/v1/maintenance/" + maintenanceId + "/history")
                        .header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(hist).size()).isGreaterThanOrEqualTo(2);
    }
}
