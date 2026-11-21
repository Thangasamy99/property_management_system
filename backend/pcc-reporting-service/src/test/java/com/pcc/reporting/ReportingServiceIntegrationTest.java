package com.pcc.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.reporting.integration.SourceGateway;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReportingServiceIntegrationTest {

    private static final String ADMIN = JwtTestHelper.token(1, "admin", Set.of("ADMIN"), Set.of(
            "REPORTING_VIEW","DASHBOARD_VIEW","PROPERTY_REPORT_VIEW","LAND_REPORT_VIEW",
            "FINANCIAL_REPORT_VIEW","WORKFLOW_REPORT_VIEW","DOCUMENT_REPORT_VIEW",
            "EMPLOYEE_REPORT_VIEW","AUDIT_REPORT_VIEW","REPORT_EXPORT","REPORT_REFRESH",
            "REPORT_CONFIGURE"));
    private static final String ACCOUNTANT = JwtTestHelper.token(4, "acc1", Set.of("ACCOUNTANT"), Set.of(
            "REPORTING_VIEW","FINANCIAL_REPORT_VIEW","WORKFLOW_REPORT_VIEW",
            "DOCUMENT_REPORT_VIEW","REPORT_EXPORT"));
    private static final String OFFICER = JwtTestHelper.token(2, "po1", Set.of("PROPERTY_OFFICER"), Set.of(
            "REPORTING_VIEW","PROPERTY_REPORT_VIEW","DOCUMENT_REPORT_VIEW"));

    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    @MockitoBean
    SourceGateway gateway;

    @Autowired
    void setup(WebApplicationContext ctx) {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        lenient().when(gateway.get(anyString(), anyString()))
                .thenAnswer(inv -> fakeResult(inv.getArgument(1)));
        lenient().when(gateway.getAllRows(anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(inv -> fakeRows(inv.getArgument(1)));
        lenient().when(gateway.getAll(anyList())).thenAnswer(inv ->
                java.util.List.<SourceGateway.SourceResult>of());
    }

    private SourceGateway.SourceResult fakeResult(String path) {
        return new SourceGateway.SourceResult("X", true, om.createObjectNode().put("totalElements", 7), null, Instant.now(), false);
    }

    private SourceGateway.SourceResult fakeRows(String path) {
        var arr = om.createArrayNode();
        arr.add(om.createObjectNode().put("code", "P-1").put("name", "Plaza")
                .put("status", "ACTIVE").put("acquisitionCost", 5000)
                .put("propertyType", "COMMERCIAL"));
        arr.add(om.createObjectNode().put("code", "P-2").put("name", "Hall")
                .put("status", "ACTIVE").put("acquisitionCost", 7000)
                .put("propertyType", "RESIDENTIAL"));
        return new SourceGateway.SourceResult("PROPERTY", true, arr, null, Instant.now(), false);
    }

    private JsonNode body(MvcResult r) throws Exception {
        return om.readTree(r.getResponse().getContentAsString());
    }

    @Test @Order(1)
    void definitionsSeededAndFiltered() throws Exception {
        JsonNode defs = body(mvc.perform(get("/api/v1/reports/definitions")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(defs.size()).isGreaterThanOrEqualTo(20);
        // accountant sees only finance/workflow/document reports
        JsonNode accDefs = body(mvc.perform(get("/api/v1/reports/definitions")
                .header("Authorization", "Bearer " + ACCOUNTANT)).andExpect(status().isOk()).andReturn());
        for (JsonNode d : accDefs)
            assertThat(d.get("domain").asText()).isIn("FINANCE", "WORKFLOW", "DOCUMENT");
    }

    @Test @Order(2)
    void reportRunsWithRowsAndTotals() throws Exception {
        JsonNode r = body(mvc.perform(get("/api/v1/reports/property/property-register")
                .header("Authorization", "Bearer " + OFFICER)).andExpect(status().isOk()).andReturn());
        assertThat(r.get("rows").size()).isEqualTo(2);
        assertThat(r.get("totals").get("acquisitionCost").asDouble()).isEqualTo(12000.0);
        assertThat(r.get("sources").get(0).get("status").asText()).isEqualTo("OK");
    }

    @Test @Order(3)
    void leastPrivilegeEnforced() throws Exception {
        // officer has no FINANCIAL_REPORT_VIEW
        mvc.perform(get("/api/v1/reports/finance/expense-register")
                        .header("Authorization", "Bearer " + OFFICER))
                .andExpect(status().isForbidden());
        // accountant has no PROPERTY_REPORT_VIEW
        mvc.perform(get("/api/v1/reports/property/property-register")
                        .header("Authorization", "Bearer " + ACCOUNTANT))
                .andExpect(status().isForbidden());
        // no REPORTING_VIEW at all
        String nobody = JwtTestHelper.token(9, "x", Set.of("ADMIN"), Set.of());
        mvc.perform(get("/api/v1/reports/definitions")
                        .header("Authorization", "Bearer " + nobody))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reports/property/property-register"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(4)
    void downSourceProducesDownHealth() throws Exception {
        Mockito.reset(gateway);
        lenient().when(gateway.getAllRows(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new SourceGateway.SourceResult("PROPERTY", false, null,
                        "connect timed out", Instant.now(), false));
        JsonNode r = body(mvc.perform(get("/api/v1/reports/property/property-register")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(r.get("sources").get(0).get("status").asText()).isEqualTo("DOWN");
        assertThat(r.get("rows").size()).isEqualTo(0);
    }

    @Test @Order(5)
    void exportProducesFiles() throws Exception {
        Mockito.reset(gateway);
        lenient().when(gateway.getAllRows(anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(inv -> fakeRows(inv.getArgument(1)));

        MvcResult csv = mvc.perform(get("/api/v1/reports/property-register/export?format=CSV")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk()).andReturn();
        String text = csv.getResponse().getContentAsString();
        assertThat(text).contains("PCC — Property Register").contains("XAF 12000");

        MvcResult xlsx = mvc.perform(get("/api/v1/reports/property-register/export?format=XLSX")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk()).andReturn();
        assertThat(xlsx.getResponse().getContentAsByteArray()).startsWith(new byte[]{0x50, 0x4B});

        MvcResult pdf = mvc.perform(get("/api/v1/reports/property-register/export?format=PDF")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk()).andReturn();
        assertThat(pdf.getResponse().getContentAsByteArray()).startsWith("%PDF".getBytes());

        // export permission required
        mvc.perform(get("/api/v1/reports/property-register/export?format=CSV")
                        .header("Authorization", "Bearer " + OFFICER))
                .andExpect(status().isForbidden());
    }

    @Test @Order(6)
    void refreshRequiresPermission() throws Exception {
        mvc.perform(post("/api/v1/reports/refresh")
                        .header("Authorization", "Bearer " + OFFICER))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reports/refresh")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk());
    }

    @Test @Order(7)
    void executionHistoryLogged() throws Exception {
        JsonNode page = body(mvc.perform(get("/api/v1/reports/execution-history")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
    }
}
