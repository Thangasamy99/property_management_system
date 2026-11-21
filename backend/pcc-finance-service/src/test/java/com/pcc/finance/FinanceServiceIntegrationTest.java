package com.pcc.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.finance.integration.ServiceGateway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
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
class FinanceServiceIntegrationTest {

    private static final String ACCOUNTANT = JwtTestHelper.token(Set.of("ACCOUNTANT"), Set.of(
            "FINANCE_DASHBOARD_VIEW", "RENT_COLLECTION_VIEW", "RENT_COLLECTION_CREATE", "RENT_COLLECTION_APPROVE",
            "LAND_PAYMENT_VIEW", "LAND_PAYMENT_CREATE", "LAND_PAYMENT_APPROVE",
            "INCOME_VIEW", "INCOME_CREATE", "EXPENSE_VIEW", "EXPENSE_CREATE", "EXPENSE_PAY",
            "RECEIPT_VIEW", "RECEIPT_PRINT", "REFUND_VIEW", "REFUND_CREATE",
            "LEDGER_VIEW", "CASH_ACCOUNT_VIEW", "FINANCIAL_REPORT_VIEW", "PAYMENT_VIEW"));
    private static final String MANAGER = JwtTestHelper.token(Set.of("MANAGER"), Set.of(
            "FINANCE_DASHBOARD_VIEW", "RENT_COLLECTION_VIEW", "RENT_COLLECTION_APPROVE",
            "LAND_PAYMENT_VIEW", "LAND_PAYMENT_APPROVE", "INCOME_VIEW", "INCOME_APPROVE",
            "EXPENSE_VIEW", "EXPENSE_APPROVE", "RECEIPT_VIEW", "RECEIPT_PRINT",
            "REFUND_VIEW", "REFUND_APPROVE", "LEDGER_VIEW", "LEDGER_ADJUST",
            "CASH_ACCOUNT_VIEW", "FINANCIAL_REPORT_VIEW", "PAYMENT_VIEW"));
    private static final String OFFICER = JwtTestHelper.token(Set.of("LAND_OFFICER"), Set.of("LAND_VIEW"));

    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();
    @MockitoBean ServiceGateway gateway;

    @Autowired
    void setup(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static Long cashPaymentId;
    private static String cashPaymentRef;
    private static Long recordedPaymentId;
    private static Long expenseId;
    private static Long incomeId;
    private static Long refundId;

    private JsonNode json(String s) throws Exception {
        return om.readTree(s);
    }

    @BeforeEach
    void stubGateway() throws Exception {
        // Property Service: agreement 5 with schedule 10 (100,000 XAF)
        lenient().when(gateway.propertyGet("/api/v1/rental-agreements/5/schedules"))
                .thenReturn(Optional.of(json(
                        "[{\"id\":10,\"agreementId\":5,\"periodStart\":\"2026-01-01\",\"periodEnd\":\"2026-01-31\",\"dueDate\":\"2026-01-05\",\"amount\":100000,\"status\":\"PENDING\"}]")));
        lenient().when(gateway.propertyGet("/api/v1/rental-agreements/5"))
                .thenReturn(Optional.of(json(
                        "{\"id\":5,\"agreementNumber\":\"AGR-0001\",\"tenantName\":\"Tenant Ltd\",\"propertyName\":\"Akwa Plaza\"}")));
        lenient().when(gateway.propertyGet("/api/v1/rental-agreements/99/schedules"))
                .thenReturn(Optional.empty());
        // Land Service: acquisition 7 approved (12M), acquisition 8 pending
        lenient().when(gateway.landGet("/api/v1/land-acquisitions/7"))
                .thenReturn(Optional.of(json(
                        "{\"id\":7,\"acquisitionReference\":\"ACQ-0007\",\"status\":\"APPROVED\",\"purchasePrice\":12000000,\"sellerName\":\"Limbe Council\",\"parcelReference\":\"LND-0002\",\"parcelName\":\"Limbe Plot\"}")));
        lenient().when(gateway.landGet("/api/v1/land-acquisitions/8"))
                .thenReturn(Optional.of(json(
                        "{\"id\":8,\"acquisitionReference\":\"ACQ-0008\",\"status\":\"PENDING\",\"purchasePrice\":5000000,\"sellerName\":\"X\",\"parcelReference\":\"LND-0003\",\"parcelName\":\"M\"}")));
        lenient().when(gateway.landGet("/api/v1/land-acquisitions/99"))
                .thenReturn(Optional.empty());
        // Land Service: sale 9 approved (25M)
        lenient().when(gateway.landGet("/api/v1/land-sales/9"))
                .thenReturn(Optional.of(json(
                        "{\"id\":9,\"saleReference\":\"SAL-0009\",\"status\":\"APPROVED\",\"proposedPrice\":25000000,\"agreedPrice\":25000000,\"buyerName\":\"Resorts SA\",\"parcelReference\":\"LND-0002\",\"parcelName\":\"Limbe Plot\"}")));
        lenient().when(gateway.landGet("/api/v1/land-sales/99"))
                .thenReturn(Optional.empty());
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String token, Object body) throws Exception {
        return mvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(om.writeValueAsString(body)));
    }

    private org.springframework.test.web.servlet.ResultActions getJson(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    // ---------- auth ----------

    @Test @Order(1)
    void unauthenticated_requests_return_401() throws Exception {
        mvc.perform(get("/api/v1/rent-collections/payments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/ledger")).andExpect(status().isUnauthorized());
    }

    @Test @Order(2)
    void insufficient_permission_returns_403() throws Exception {
        getJson("/api/v1/rent-collections/payments", OFFICER).andExpect(status().isForbidden());
        getJson("/api/v1/expenses", OFFICER).andExpect(status().isForbidden());
        getJson("/api/v1/ledger", OFFICER).andExpect(status().isForbidden());
        getJson("/api/v1/dashboard/summary", OFFICER).andExpect(status().isForbidden());
        // accountant cannot approve expenses or refunds
        postJson("/api/v1/expenses/1/approve", ACCOUNTANT, null).andExpect(status().isForbidden());
        postJson("/api/v1/reversals", ACCOUNTANT, new Object() {
            public final Long paymentId = 1L; public final String reason = "x";
        }).andExpect(status().isForbidden());
    }

    // ---------- rent collection ----------

    @Test @Order(3)
    void cash_rent_payment_posts_immediately_with_receipt() throws Exception {
        var res = postJson("/api/v1/rent-collections/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 10L;
            public final Long parentId = 5L;
            public final String payerName = "Tenant Ltd";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("50000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
            public final String paymentReference = "RCPT-X1";
            public final String idempotencyKey = "key-rent-1";
        });
        res.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.paymentRef").value("PAY-0001"))
                .andExpect(jsonPath("$.receiptNo").value("RCP-0001"));
        JsonNode body = om.readTree(res.andReturn().getResponse().getContentAsString());
        cashPaymentId = body.get("id").asLong();
        cashPaymentRef = body.get("paymentRef").asText();

        // outstanding now 50,000
        getJson("/api/v1/rent-collections/targets/10", ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(100000))
                .andExpect(jsonPath("$.paidAmount").value(50000))
                .andExpect(jsonPath("$.outstanding").value(50000))
                .andExpect(jsonPath("$.fullyPaid").value(false));

        // receipt contains amount in words
        getJson("/api/v1/receipts/by-payment/" + cashPaymentId, ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountInWords").value("Fifty thousand francs CFA"));
    }

    @Test @Order(4)
    void overpayment_is_rejected() throws Exception {
        postJson("/api/v1/rent-collections/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 10L;
            public final Long parentId = 5L;
            public final String payerName = "Tenant Ltd";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("60000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
        }).andExpect(status().isUnprocessableEntity());
    }

    @Test @Order(5)
    void non_cash_payment_requires_posting_then_receipt() throws Exception {
        var res = postJson("/api/v1/rent-collections/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 10L;
            public final Long parentId = 5L;
            public final String payerName = "Tenant Ltd";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("30000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "BANK_TRANSFER";
            public final String paymentReference = "TRF-8899";
        });
        res.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andExpect(jsonPath("$.receiptNo").doesNotExist());
        recordedPaymentId = om.readTree(res.andReturn().getResponse().getContentAsString()).get("id").asLong();

        // no receipt yet
        getJson("/api/v1/receipts/by-payment/" + recordedPaymentId, ACCOUNTANT)
                .andExpect(status().isNotFound());

        // officer cannot post (no RENT_COLLECTION_APPROVE)
        mvc.perform(post("/api/v1/rent-collections/payments/" + recordedPaymentId + "/post")
                        .header("Authorization", "Bearer " + OFFICER)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());

        // approver posts with verification evidence
        postJson("/api/v1/rent-collections/payments/" + recordedPaymentId + "/post", MANAGER,
                new Object() { public final String verificationDetail = "Bank stmt line 42"; })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.receiptNo").exists());
    }

    @Test @Order(6)
    void idempotent_replay_returns_original_payment() throws Exception {
        var res = postJson("/api/v1/rent-collections/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 10L;
            public final Long parentId = 5L;
            public final String payerName = "Tenant Ltd";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("50000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
            public final String idempotencyKey = "key-rent-1";   // same key as the first payment
        });
        res.andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentRef").value(cashPaymentRef))
                .andExpect(jsonPath("$.id").value(cashPaymentId));
    }

    @Test @Order(7)
    void invalid_external_reference_returns_404() throws Exception {
        postJson("/api/v1/rent-collections/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 10L;
            public final Long parentId = 99L;                    // unknown agreement
            public final String payerName = "X";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("1000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
        }).andExpect(status().isNotFound());

        postJson("/api/v1/land-acquisition-payments/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 99L;
            public final String payerName = "PCC";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("1000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
        }).andExpect(status().isNotFound());
    }

    // ---------- land payments ----------

    @Test @Order(8)
    void land_acquisition_payment_partial_then_outstanding() throws Exception {
        postJson("/api/v1/land-acquisition-payments/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 7L;
            public final String payerName = "PCC Cameroon";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("4000000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
        }).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("POSTED"));

        getJson("/api/v1/land-acquisition-payments/targets/7", ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAmount").value(4000000))
                .andExpect(jsonPath("$.outstanding").value(8000000));
    }

    @Test @Order(9)
    void pending_acquisition_cannot_be_paid() throws Exception {
        postJson("/api/v1/land-acquisition-payments/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 8L;
            public final String payerName = "PCC";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("1000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
        }).andExpect(status().isUnprocessableEntity());
    }

    @Test @Order(10)
    void land_sale_receipt_payment_works() throws Exception {
        var res = postJson("/api/v1/land-sale-receipts/payments", ACCOUNTANT, new Object() {
            public final Long externalId = 9L;
            public final String payerName = "Resorts SA";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("10000000");
            public final String paymentDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CHEQUE";
            public final String paymentReference = "CHQ-001";
        });
        res.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RECORDED"));
        
    }

    // ---------- expenses & income ----------

    @Test @Order(11)
    void expense_requires_approval_before_payment() throws Exception {
        var res = postJson("/api/v1/expenses", ACCOUNTANT, new Object() {
            public final String category = "UTILITIES";
            public final String description = "Office electricity Feb";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("250000");
            public final String expenseDate = java.time.LocalDate.now().toString();
            public final String department = "ADMIN";
            public final String vendorName = "ENEO";
        });
        res.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.expenseRef").value("EXP-0001"));
        expenseId = om.readTree(res.andReturn().getResponse().getContentAsString()).get("id").asLong();

        // pay before approval -> 422
        postJson("/api/v1/expenses/" + expenseId + "/pay", ACCOUNTANT, new Object() {
            public final String methodCode = "CASH";
            public final String paidDate = java.time.LocalDate.now().toString();
        }).andExpect(status().isUnprocessableEntity());

        // approve with manager
        postJson("/api/v1/expenses/" + expenseId + "/approve", MANAGER, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        // pay
        postJson("/api/v1/expenses/" + expenseId + "/pay", ACCOUNTANT, new Object() {
            public final String methodCode = "CASH";
            public final String paidDate = java.time.LocalDate.now().toString();
        }).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentRef").exists());
    }

    @Test @Order(12)
    void income_approval_posts_payment_and_receipt() throws Exception {
        var res = postJson("/api/v1/other-income", ACCOUNTANT, new Object() {
            public final String category = "DONATION";
            public final String description = "Church donation";
            public final java.math.BigDecimal amount = new java.math.BigDecimal("150000");
            public final String incomeDate = java.time.LocalDate.now().toString();
            public final String methodCode = "CASH";
            public final String payerName = "Anonymous";
        });
        res.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RECORDED"));
        incomeId = om.readTree(res.andReturn().getResponse().getContentAsString()).get("id").asLong();

        postJson("/api/v1/other-income/" + incomeId + "/approve", MANAGER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.paymentRef").exists());
    }

    // ---------- refunds & reversals ----------

    @Test @Order(13)
    void refund_reduces_refundable_amount() throws Exception {
        var res = postJson("/api/v1/refunds", ACCOUNTANT, new Object() {
            public final Long paymentId = cashPaymentId;
            public final java.math.BigDecimal amount = new java.math.BigDecimal("20000");
            public final String reason = "Tenant overpaid";
        });
        res.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REQUESTED"));
        refundId = om.readTree(res.andReturn().getResponse().getContentAsString()).get("id").asLong();

        postJson("/api/v1/refunds/" + refundId + "/approve", MANAGER, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("POSTED"));

        getJson("/api/v1/rent-collections/payments/" + cashPaymentId, ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.refundableAmount").value(30000));

        // over-refund rejected
        postJson("/api/v1/refunds", ACCOUNTANT, new Object() {
            public final Long paymentId = cashPaymentId;
            public final java.math.BigDecimal amount = new java.math.BigDecimal("99999");
            public final String reason = "too much";
        }).andExpect(status().isUnprocessableEntity());
    }

    @Test @Order(14)
    void reversal_voids_payment_and_receipt() throws Exception {
        postJson("/api/v1/reversals", MANAGER, new Object() {
            public final Long paymentId = recordedPaymentId;
            public final String reason = "Bank transfer bounced";
        }).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVERSED"));

        getJson("/api/v1/receipts/by-payment/" + recordedPaymentId, ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voided").value(true));
    }

    // ---------- ledger / dashboard / reports ----------

    @Test @Order(15)
    void ledger_contains_all_posting_types() throws Exception {
        String body = getJson("/api/v1/ledger?size=100", ACCOUNTANT)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"transactionType\":\"RECEIPT\"")
                .contains("\"transactionType\":\"EXPENSE_PAYOUT\"")
                .contains("\"transactionType\":\"REFUND\"")
                .contains("\"transactionType\":\"REVERSAL\"")
                .contains("\"direction\":\"DEBIT\"")
                .contains("\"direction\":\"CREDIT\"");
    }

    @Test @Order(16)
    void dashboard_and_reports_derive_from_records() throws Exception {
        getJson("/api/v1/dashboard/summary", ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionsThisMonth").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.expensesThisMonth").value(250000))
                .andExpect(jsonPath("$.landAcquisitionPaid").value(4000000))
                .andExpect(jsonPath("$.outstandingRent").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.monthlySeries").isArray())
                .andExpect(jsonPath("$.recentTransactions").isArray());

        getJson("/api/v1/reports/income-vs-expense?from=2000-01-01&to=2100-01-01", ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.expenses").value(250000));

        getJson("/api/v1/reports/rent-arrears", ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingTotal").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test @Order(17)
    void reconciliation_frees_outstanding_after_reversal() throws Exception {
        // recordedPayment (30k) was reversed → outstanding back to 50k - 20k paid net + 30k freed
        getJson("/api/v1/rent-collections/targets/10", ACCOUNTANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAmount").value(30000))   // 50k posted - 20k refunded
                .andExpect(jsonPath("$.outstanding").value(70000));
    }
}
