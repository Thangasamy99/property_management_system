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

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LandServiceIntegrationTest {

    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Set<String> OFFICER_PERMS = Set.of(
            "LAND_VIEW", "LAND_CREATE", "LAND_UPDATE", "LAND_DELETE",
            "LAND_SURVEY_VIEW", "LAND_SURVEY_CREATE", "LAND_SURVEY_UPDATE",
            "LAND_OWNERSHIP_VIEW", "LAND_OWNERSHIP_CREATE",
            "LAND_ACQUISITION_CREATE", "LAND_SALE_REQUEST", "LAND_REPORT_VIEW");
    private static final Set<String> MANAGER_PERMS = Set.of(
            "LAND_VIEW", "LAND_SALE_APPROVE", "LAND_ACQUISITION_APPROVE",
            "LAND_SURVEY_VIEW", "LAND_OWNERSHIP_VIEW", "LAND_REPORT_VIEW");

    private String officer;
    private String manager;
    private String noPerms;

    private static long parcelId;
    private static long saleId;
    private static long transferId;

    @Autowired
    void setup(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        officer = "Bearer " + JwtTestHelper.token(Set.of("LAND_OFFICER"), OFFICER_PERMS);
        manager = "Bearer " + JwtTestHelper.token(Set.of("MANAGER"), MANAGER_PERMS);
        noPerms = "Bearer " + JwtTestHelper.token(Set.of("ACCOUNTANT"), Set.of());
    }

    private long id(MvcResult res) throws Exception {
        JsonNode node = mapper.readTree(res.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    @Test
    @Order(1)
    void rejectsUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/land-parcels")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void rejectsWrongPermission() throws Exception {
        mvc.perform(get("/api/v1/land-parcels").header("Authorization", noPerms))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/land-parcels").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void registersParcelAndConvertsUnits() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/land-parcels").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Buea Farm Estate","description":"Agricultural land near Buea",
                         "classification":"AGRICULTURAL","areaValue":2.5,"unit":"HECTARE",
                         "region":"South West","division":"Fako","subdivision":"Buea",
                         "locality":"Bokwango","address":"Mile 17"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parcelReference").value("LND-0001"))
                .andExpect(jsonPath("$.areaSqm").value(25000.0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn();
        parcelId = id(res);
    }

    @Test
    @Order(4)
    void rejectsInvalidParcelInput() throws Exception {
        mvc.perform(post("/api/v1/land-parcels").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"X","classification":"RESIDENTIAL","areaValue":-5,"unit":"ACRE","region":"West"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void validatesCoordinates() throws Exception {
        mvc.perform(put("/api/v1/land-coordinates").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"points":[
                          {"sequence":1,"latitude":4.1500,"longitude":9.2300,"label":"NW corner"},
                          {"sequence":2,"latitude":4.1480,"longitude":9.2320,"label":"NE corner"},
                          {"sequence":3,"latitude":4.1460,"longitude":9.2280,"label":"SE corner"}
                        ]}
                        """.formatted(parcelId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mvc.perform(put("/api/v1/land-coordinates").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"points":[{"sequence":1,"latitude":95,"longitude":9.2}]}
                        """.formatted(parcelId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void recordsSurvey() throws Exception {
        mvc.perform(post("/api/v1/land-surveys").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"surveyDate":"2025-06-01","surveyorName":"J. Manga",
                         "surveyorOrganization":"CamSurv Ltd","surveyedArea":25000.5,
                         "boundaryNorth":"Molyko road","boundarySouth":"River Muea",
                         "boundaryEast":"State land","boundaryWest":"Village land"}
                        """.formatted(parcelId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.surveyReference").value("SRV-0001"));
    }

    @Test
    @Order(7)
    void registersCompanyOwnership() throws Exception {
        mvc.perform(post("/api/v1/land-ownership").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"ownerName":"PCC Cameroon","titleReference":"TC-4421/BUEA",
                         "effectiveDate":"2025-06-15","acquisitionDate":"2025-06-10"}
                        """.formatted(parcelId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownershipReference").value("OWN-0001"))
                .andExpect(jsonPath("$.status").value("CURRENT"));
    }

    @Test
    @Order(8)
    void acquisitionLifecycle() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/land-acquisitions").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"sellerName":"Bokwango Family Trust","sellerContact":"+237 600 000 001",
                         "acquisitionDate":"2025-06-10","purchasePrice":45000000}
                        """.formatted(parcelId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acquisitionReference").value("ACQ-0001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long acqId = id(res);

        mvc.perform(post("/api/v1/land-acquisitions/" + acqId + "/approve")
                        .header("Authorization", officer))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/land-acquisitions/" + acqId + "/approve")
                        .header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Verified\",\"approvalReference\":\"BRD-2025-19\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/land-acquisitions/" + acqId + "/complete")
                        .header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(9)
    void preventsConflictingSales() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/land-sales").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"buyerName":"Douala Estates SARL","buyerContact":"+237 699 000 002",
                         "proposedPrice":60000000}
                        """.formatted(parcelId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleReference").value("SAL-0001"))
                .andReturn();
        saleId = id(res);

        mvc.perform(post("/api/v1/land-sales").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"buyerName":"Second Buyer","proposedPrice":1}
                        """.formatted(parcelId)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(10)
    void saleWorkflowDrivesParcelStatus() throws Exception {
        mvc.perform(post("/api/v1/land-sales/" + saleId + "/submit").header("Authorization", officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(get("/api/v1/land-parcels/" + parcelId).header("Authorization", officer))
                .andExpect(jsonPath("$.status").value("UNDER_SALE"));

        mvc.perform(post("/api/v1/land-sales/" + saleId + "/approve").header("Authorization", officer))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/land-sales/" + saleId + "/approve").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreedPrice\":58000000,\"note\":\"Board approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.agreedPrice").value(58000000));

        mvc.perform(post("/api/v1/land-sales/" + saleId + "/progress").header("Authorization", manager))
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"));
        mvc.perform(post("/api/v1/land-sales/" + saleId + "/progress").header("Authorization", manager))
                .andExpect(jsonPath("$.status").value("LEGAL_PROCESSING"));
        mvc.perform(post("/api/v1/land-sales/" + saleId + "/progress").header("Authorization", manager))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/v1/land-parcels/" + parcelId).header("Authorization", officer))
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    @Order(11)
    void transferCompletionPreservesOwnershipHistory() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/land-transfers").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"parcelId":%d,"newOwnerName":"Douala Estates SARL","saleId":%d,
                         "titleReference":"TC-9988/DLA"}
                        """.formatted(parcelId, saleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.previousOwnerName").value("PCC Cameroon"))
                .andReturn();
        transferId = id(res);

        mvc.perform(post("/api/v1/land-transfers/" + transferId + "/submit").header("Authorization", officer))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(post("/api/v1/land-transfers/" + transferId + "/approve").header("Authorization", manager))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/land-transfers/" + transferId + "/complete")
                        .header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legalCompletionDate\":\"2025-09-30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/v1/land-ownership/by-parcel/" + parcelId).header("Authorization", officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status", hasItems("CURRENT", "ENDED")))
                .andExpect(jsonPath("$[*].ownerName", hasItems("PCC Cameroon", "Douala Estates SARL")));

        mvc.perform(get("/api/v1/land-ownership/history/" + parcelId).header("Authorization", officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType",
                        hasItems("REGISTERED", "TRANSFERRED_OUT", "TRANSFERRED_IN")));
    }

    @Test
    @Order(12)
    void ledgerAndSummaryPopulated() throws Exception {
        mvc.perform(get("/api/v1/land-transactions?parcelId=" + parcelId).header("Authorization", officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(10)));
        mvc.perform(get("/api/v1/land-parcels/summary").header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParcels").value(1))
                .andExpect(jsonPath("$.parcelsByStatus.SOLD").value(1));
        mvc.perform(get("/api/v1/land-parcels/" + parcelId + "/status-history")
                        .header("Authorization", officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].newStatus",
                        hasItems("AVAILABLE", "UNDER_SALE", "SOLD", "UNDER_TRANSFER")));
    }
}
