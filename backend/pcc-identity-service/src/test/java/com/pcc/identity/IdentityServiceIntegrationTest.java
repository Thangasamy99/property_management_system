package com.pcc.identity;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests against the real security chain, services and a real MySQL
 * database ({@code pcc_identity_test_db}). Schema and seed data come from the
 * production Flyway migrations; the admin account is created by the production
 * {@code AdminBootstrapRunner}. Test-created rows are cleaned up once per class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityServiceIntegrationTest {

    private static final String ADMIN_PASSWORD = "Admin@Test123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeAll
    void cleanTestData() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM login_history");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE ur FROM user_roles ur JOIN users u ON ur.user_id = u.id WHERE u.username <> 'admin'");
        jdbcTemplate.update("DELETE FROM users WHERE username <> 'admin'");
    }

    // ---------- helpers ----------

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String adminToken() throws Exception {
        return login("admin", ADMIN_PASSWORD);
    }

    private Long createUser(String adminToken, String username, String email,
                            String password, String roleCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeCode":"%s","username":"%s","firstName":"Test","lastName":"User",
                                 "email":"%s","password":"%s","roleCodes":["%s"]}
                                """.formatted(username.toUpperCase(), username, email, password, roleCode)))
                .andExpect(status().isCreated())
                .andReturn();
        Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.user.id");
        return id.longValue();
    }

    // ---------- authentication ----------

    @Test
    @Order(1)
    void adminBootstrapAndLoginWorks() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.roles[*].code", hasItem("ADMIN")))
                .andExpect(jsonPath("$.user.permissions", hasItem("USER_CREATE")))
                .andExpect(cookie().exists("pcc_refresh_token"));
    }

    @Test
    @Order(2)
    void loginWithEmailWorks() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin@test.local\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    @Order(3)
    void loginFailsWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @Order(4)
    void loginFailsWithUnknownUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"noone\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void loginValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    // ---------- authorization ----------

    @Test
    @Order(10)
    void meReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.permissions", hasItem("ROLE_VIEW")));
    }

    @Test
    @Order(11)
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    void protectedEndpointRejectsGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    void forbiddenWithoutRequiredPermission() throws Exception {
        String token = adminToken();
        createUser(token, "officer1", "officer1@pcc.cm", "IdentityTest_Officer#2026", "PROPERTY_OFFICER");
        String officerToken = login("officer1", "IdentityTest_Officer#2026");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
    }

    // ---------- user management ----------

    @Test
    @Order(20)
    void createAndFetchUser() throws Exception {
        String token = adminToken();
        Long id = createUser(token, "manager1", "manager1@pcc.cm", "Manager!123", "MANAGER");

        mockMvc.perform(get("/api/v1/users/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manager1"))
                .andExpect(jsonPath("$.roles[*].code", hasItem("MANAGER")));

        mockMvc.perform(get("/api/v1/users?search=manager1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @Order(21)
    void duplicateUsernameRejected() throws Exception {
        String token = adminToken();
        createUser(token, "dupuser", "dup1@pcc.cm", "Pass!1234", "ACCOUNTANT");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeCode":"DUP-2","username":"dupuser","firstName":"Dup","lastName":"Two",
                                 "email":"dup2@pcc.cm","password":"Pass!1234"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Username")));
    }

    @Test
    @Order(22)
    void deactivateStopsLogin() throws Exception {
        String token = adminToken();
        Long id = createUser(token, "todeactivate", "deact@pcc.cm", "Pass!1234", "ACCOUNTANT");

        mockMvc.perform(patch("/api/v1/users/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("INACTIVE"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"todeactivate\",\"password\":\"Pass!1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("deactivated")));
    }

    @Test
    @Order(23)
    void accountLocksAfterFailedAttempts() throws Exception {
        String token = adminToken();
        createUser(token, "lockme", "lockme@pcc.cm", "Pass!1234", "ACCOUNTANT");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"lockme\",\"password\":\"bad\"}"));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lockme\",\"password\":\"Pass!1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("locked")));
    }

    @Test
    @Order(24)
    void roleAssignmentWorks() throws Exception {
        String token = adminToken();
        Long id = createUser(token, "roleuser", "roleuser@pcc.cm", "Pass!1234", "ACCOUNTANT");

        mockMvc.perform(put("/api/v1/users/" + id + "/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\":[\"MANAGER\",\"LAND_OFFICER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[*].code",
                        containsInAnyOrder("MANAGER", "LAND_OFFICER")));
    }

    // ---------- token lifecycle ----------

    @Test
    @Order(30)
    void refreshRotatesTokens() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String oldRefresh = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        String newRefresh = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.refreshToken");

        // Old token is revoked and cannot be replayed.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(newRefresh).isNotEqualTo(oldRefresh);
    }

    @Test
    @Order(31)
    void refreshWorksViaHttpOnlyCookie() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = login.getResponse().getCookie("pcc_refresh_token");
        org.assertj.core.api.Assertions.assertThat(cookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @Order(32)
    void logoutRevokesRefreshToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String refresh = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");
        String access = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- catalog endpoints ----------

    @Test
    @Order(40)
    void rolesAndPermissionsCatalogsWork() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code",
                        hasItems("ADMIN", "MANAGER", "ACCOUNTANT", "PROPERTY_OFFICER", "LAND_OFFICER")));

        mockMvc.perform(get("/api/v1/permissions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(30))));

        mockMvc.perform(get("/api/v1/permissions/modules").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.LAND").isArray())
                .andExpect(jsonPath("$.FINANCE").isArray());
    }

    @Test
    @Order(41)
    void loginHistoryIsRecorded() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/v1/auth/login-history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].eventType").isString());
    }

    @Test
    @Order(42)
    void swaggerDocsAreExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/login")));
    }

    @Test
    @Order(50)
    void dashboardStatsAreReturned() throws Exception {
        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeEmployees", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalRoles", is(5)))
                .andExpect(jsonPath("$.successfulLogins", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.recentRegistrations").isArray());
    }

    @Test
    @Order(51)
    void adminLoginHistoryListsAllUsers() throws Exception {
        mockMvc.perform(get("/api/v1/login-history?username=admin")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].username").isString());

        mockMvc.perform(get("/api/v1/login-history?eventType=LOGIN_SUCCESS")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(52)
    void auditLogsAreRecordedAndPaginated() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs?size=50")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].action").isString())
                .andExpect(jsonPath("$.content[0].actorUsername").isString());

        mockMvc.perform(get("/api/v1/audit-logs?action=USER_CREATE&entityType=USER")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(53)
    void adminEndpointsForbiddenForOfficer() throws Exception {
        createUser(adminToken(), "officer2", "officer2@test.pcc", "IdentityTest_Officer#2026", "PROPERTY_OFFICER");
        String officerToken = login("officer2", "IdentityTest_Officer#2026");

        mockMvc.perform(get("/api/v1/stats/dashboard").header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit-logs").header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/login-history").header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(54)
    void usersByRoleIsReturned() throws Exception {
        String token = adminToken();
        Long adminRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE code = 'ADMIN'", Long.class);

        mockMvc.perform(get("/api/v1/roles/" + adminRoleId + "/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].username").isString());
    }
}
