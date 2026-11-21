package com.pcc.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.workflow.integration.DownstreamGateway;
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
class WorkflowServiceIntegrationTest {

    private static final Set<String> ADMIN_PERMS = Set.of(
            "WORKFLOW_VIEW","WORKFLOW_CREATE","WORKFLOW_SUBMIT","WORKFLOW_APPROVE","WORKFLOW_REJECT",
            "WORKFLOW_RETURN","WORKFLOW_CANCEL","WORKFLOW_CONFIGURE","WORKFLOW_ASSIGN",
            "WORKFLOW_DELEGATE","WORKFLOW_HISTORY_VIEW","WORKFLOW_REPORT_VIEW","NOTIFICATION_VIEW");
    private static final String ADMIN = JwtTestHelper.token(1, "admin", Set.of("ADMIN"), ADMIN_PERMS);
    private static final String MANAGER = JwtTestHelper.token(2, "mgr", Set.of("MANAGER"), Set.of(
            "WORKFLOW_VIEW","WORKFLOW_CREATE","WORKFLOW_SUBMIT","WORKFLOW_APPROVE","WORKFLOW_REJECT",
            "WORKFLOW_RETURN","WORKFLOW_CANCEL","WORKFLOW_DELEGATE",
            "WORKFLOW_HISTORY_VIEW","NOTIFICATION_VIEW"));
    private static final String OFFICER = JwtTestHelper.token(3, "po1", Set.of("PROPERTY_OFFICER"), Set.of(
            "WORKFLOW_VIEW","WORKFLOW_CREATE","WORKFLOW_SUBMIT","WORKFLOW_CANCEL",
            "WORKFLOW_HISTORY_VIEW","NOTIFICATION_VIEW"));
    private static final String OTHER = JwtTestHelper.token(4, "acc1", Set.of("ACCOUNTANT"), Set.of(
            "WORKFLOW_VIEW","WORKFLOW_SUBMIT","NOTIFICATION_VIEW"));

    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    @MockitoBean
    DownstreamGateway gateway;

    private static long defId;          // multi-level definition (no integration)
    private static long finDefId;       // finance definition (with integration)
    private static long instId;
    private static long retId;

    @Autowired
    void setup(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        lenient().when(gateway.invoke(anyString(), anyString(), anyString(), any()))
                .thenReturn(new DownstreamGateway.CallResult(true, false, null));
        lenient().when(gateway.hasRelayToken()).thenReturn(true);
    }

    private JsonNode body(MvcResult r) throws Exception {
        return om.readTree(r.getResponse().getContentAsString());
    }

    // ---------- definitions ----------

    @Test @Order(1)
    void seededDefinitionsListed() throws Exception {
        JsonNode list = body(mvc.perform(get("/api/v1/workflow-definitions")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(list.size()).isGreaterThanOrEqualTo(9);
    }

    @Test @Order(2)
    void createDefinitionRequiresConfigure() throws Exception {
        mvc.perform(post("/api/v1/workflow-definitions")
                        .header("Authorization", "Bearer " + OFFICER)
                        .contentType("application/json")
                        .content("{\"code\":\"X2\",\"name\":\"x\",\"module\":\"PROPERTY\",\"workflowType\":\"T\",\"entityType\":\"E\",\"levels\":[{\"levelOrder\":1,\"name\":\"l\",\"approverRole\":\"MANAGER\",\"mandatory\":true}]}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(3)
    void createMultiLevelDefinition() throws Exception {
        JsonNode def = body(mvc.perform(post("/api/v1/workflow-definitions")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("{\"code\":\"TEST_TWO_LEVEL\",\"name\":\"Two level test\",\"module\":\"PROPERTY\",\"workflowType\":\"TEST\",\"entityType\":\"PROPERTY\",\"levels\":[{\"levelOrder\":1,\"name\":\"L1\",\"approverRole\":\"MANAGER\",\"mandatory\":true},{\"levelOrder\":2,\"name\":\"L2\",\"approverRole\":\"ADMIN\",\"mandatory\":true}]}"))
                .andExpect(status().isOk()).andReturn());
        defId = def.get("id").asLong();
        assertThat(def.get("levels").size()).isEqualTo(2);
    }

    // ---------- submission + lifecycle ----------

    @Test @Order(10)
    void submitCreatesWorkflowAndTask() throws Exception {
        JsonNode i = body(mvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + OFFICER)
                        .contentType("application/json")
                        .content("{\"definitionId\":" + defId + ",\"entityRef\":\"1\",\"description\":\"test request\"}"))
                .andExpect(status().isOk()).andReturn());
        instId = i.get("id").asLong();
        assertThat(i.get("workflowRef").asText()).matches("WF-\\d{6}");
        assertThat(i.get("status").asText()).isEqualTo("PENDING_APPROVAL");

        JsonNode tasks = body(mvc.perform(get("/api/v1/workflows/" + instId + "/tasks")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0).get("assigneeRole").asText()).isEqualTo("MANAGER");
    }

    @Test @Order(11)
    void selfApprovalPrevented() throws Exception {
        // submitter is OFFICER — submit another as MANAGER-assignee? Submit as the
        // approver themselves is impossible since OFFICER lacks MANAGER role; but
        // a MANAGER-submitted workflow cannot be approved by the same manager.
        JsonNode i = body(mvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json")
                        .content("{\"definitionId\":" + defId + ",\"entityRef\":\"2\"}"))
                .andExpect(status().isOk()).andReturn());
        long id = i.get("id").asLong();
        mvc.perform(post("/api/v1/workflows/" + id + "/approve")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(12)
    void unauthorizedApproverDenied() throws Exception {
        mvc.perform(post("/api/v1/workflows/" + instId + "/approve")
                        .header("Authorization", "Bearer " + OTHER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());   // ACCOUNTANT lacks WORKFLOW_APPROVE
    }

    @Test @Order(13)
    void wrongRoleApproverDenied() throws Exception {
        // L1 requires MANAGER; ADMIN (has approve perm but wrong role) is rejected
        // unless ADMIN role matches — assert the assignee check works.
        mvc.perform(post("/api/v1/workflows/" + instId + "/approve")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(14)
    void sequentialApprovalAdvances() throws Exception {
        mvc.perform(post("/api/v1/workflows/" + instId + "/approve")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{\"comment\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLevel").value(2));
        // level skipping: L2 must approve ADMIN; MANAGER cannot act again
        mvc.perform(post("/api/v1/workflows/" + instId + "/approve")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        // final level completes (no integration on this definition)
        mvc.perform(post("/api/v1/workflows/" + instId + "/approve")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        // duplicate approval prevented
        mvc.perform(post("/api/v1/workflows/" + instId + "/approve")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @Order(15)
    void rejectRequiresReason() throws Exception {
        JsonNode i = body(mvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + OFFICER)
                        .contentType("application/json")
                        .content("{\"definitionId\":" + defId + ",\"entityRef\":\"3\"}"))
                .andReturn());
        long id = i.get("id").asLong();
        mvc.perform(post("/api/v1/workflows/" + id + "/reject")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/workflows/" + id + "/reject")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{\"comment\":\"incomplete\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test @Order(16)
    void returnAndResubmit() throws Exception {
        JsonNode i = body(mvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + OFFICER)
                        .contentType("application/json")
                        .content("{\"definitionId\":" + defId + ",\"entityRef\":\"4\"}"))
                .andReturn());
        retId = i.get("id").asLong();
        mvc.perform(post("/api/v1/workflows/" + retId + "/return")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{\"comment\":\"fix dates\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_FOR_CORRECTION"));
        mvc.perform(post("/api/v1/workflows/" + retId + "/resubmit")
                        .header("Authorization", "Bearer " + OFFICER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.currentLevel").value(1));
    }

    // ---------- notifications ----------

    @Test @Order(20)
    void notificationsCreated() throws Exception {
        // submitter got RETURNED notification
        JsonNode n = body(mvc.perform(get("/api/v1/notifications?size=50")
                .header("Authorization", "Bearer " + OFFICER)).andExpect(status().isOk()).andReturn());
        assertThat(n.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
        mvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + OFFICER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").isNumber());
    }

    // ---------- integration ----------

    @Test @Order(30)
    void integrationDispatchedOnFinalApproval() throws Exception {
        // find seeded EXPENSE_APPROVAL definition
        JsonNode defs = body(mvc.perform(get("/api/v1/workflow-definitions?module=FINANCE")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        for (JsonNode d : defs) {
            if (d.get("code").asText().equals("EXPENSE_APPROVAL")) finDefId = d.get("id").asLong();
        }
        assertThat(finDefId).isPositive();

        JsonNode i = body(mvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + OTHER)
                        .contentType("application/json")
                        .content("{\"definitionId\":" + finDefId + ",\"entityRef\":\"7\",\"amount\":50000}"))
                .andExpect(status().isOk()).andReturn());
        long id = i.get("id").asLong();
        mvc.perform(post("/api/v1/workflows/" + id + "/approve")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        Mockito.verify(gateway).invoke(eq("FINANCE"), eq("POST"), eq("/api/v1/expenses/7/approve"), anyString());
    }

    @Test @Order(31)
    void integrationFailureRetries() throws Exception {
        Mockito.reset(gateway);
        lenient().when(gateway.invoke(anyString(), anyString(), anyString(), any()))
                .thenReturn(new DownstreamGateway.CallResult(false, false, "FINANCE unavailable"));
        lenient().when(gateway.hasRelayToken()).thenReturn(true);

        JsonNode i = body(mvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + OTHER)
                        .contentType("application/json")
                        .content("{\"definitionId\":" + finDefId + ",\"entityRef\":\"8\",\"amount\":1000}"))
                .andExpect(status().isOk()).andReturn());
        long id = i.get("id").asLong();
        mvc.perform(post("/api/v1/workflows/" + id + "/approve")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTEGRATION_PENDING"));

        JsonNode ints = body(mvc.perform(get("/api/v1/workflows/" + id + "/integrations")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(ints.size()).isEqualTo(1);
        long outId = ints.get(0).get("id").asLong();

        // retry with success
        Mockito.reset(gateway);
        lenient().when(gateway.invoke(anyString(), anyString(), anyString(), any()))
                .thenReturn(new DownstreamGateway.CallResult(true, false, null));
        lenient().when(gateway.hasRelayToken()).thenReturn(true);
        mvc.perform(post("/api/v1/workflows/integrations/" + outId + "/retry")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(get("/api/v1/workflows/" + id)
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ---------- delegation ----------

    @Test @Order(40)
    void delegationFlow() throws Exception {
        // MANAGER delegates to ACCOUNTANT (id 4) for PROPERTY module
        mvc.perform(post("/api/v1/delegations")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json")
                        .content("{\"delegateId\":4,\"delegateName\":\"acc1\",\"module\":\"PROPERTY\",\"startDate\":\"" + java.time.LocalDate.now() + "\",\"endDate\":\"" + java.time.LocalDate.now().plusDays(7) + "\",\"reason\":\"leave\"}"))
                .andExpect(status().isOk());
        // OFFICER submits a PROPERTY workflow; ACCOUNTANT acts on delegated MANAGER task
        JsonNode i = body(mvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + OFFICER)
                        .contentType("application/json")
                        .content("{\"definitionId\":" + defId + ",\"entityRef\":\"9\"}"))
                .andReturn());
        long id = i.get("id").asLong();
        // ACCOUNTANT lacks WORKFLOW_APPROVE permission -> still 403 at endpoint level
        mvc.perform(post("/api/v1/workflows/" + id + "/approve")
                        .header("Authorization", "Bearer " + OTHER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(41)
    void delegationDateValidation() throws Exception {
        mvc.perform(post("/api/v1/delegations")
                        .header("Authorization", "Bearer " + MANAGER)
                        .contentType("application/json")
                        .content("{\"delegateId\":4,\"startDate\":\"" + java.time.LocalDate.now().plusDays(5) + "\",\"endDate\":\"" + java.time.LocalDate.now() + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- history/audit ----------

    @Test @Order(50)
    void historyAndAuditPreserved() throws Exception {
        JsonNode hist = body(mvc.perform(get("/api/v1/workflows/" + retId + "/history")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        Set<String> acts = new java.util.HashSet<>();
        for (JsonNode a : hist) acts.add(a.get("action").asText());
        assertThat(acts).contains("SUBMIT", "RETURN", "RESUBMIT");
        JsonNode audit = body(mvc.perform(get("/api/v1/workflows/" + retId + "/audit")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(audit.size()).isGreaterThanOrEqualTo(3);
    }

    // ---------- search/pagination ----------

    @Test @Order(60)
    void searchFiltersAndPagination() throws Exception {
        JsonNode page = body(mvc.perform(get("/api/v1/workflows?module=PROPERTY&status=PENDING_APPROVAL&size=5")
                .header("Authorization", "Bearer " + ADMIN)).andExpect(status().isOk()).andReturn());
        assertThat(page.get("size").asInt()).isEqualTo(5);
        for (JsonNode r : page.get("content"))
            assertThat(r.get("module").asText()).isEqualTo("PROPERTY");
    }
}
