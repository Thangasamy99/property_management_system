package com.pcc.reporting.report;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Static registry of report types. Each entry maps a report key to the owning
 * service endpoint plus the columns to project. Seeded into
 * report_definitions at startup so DB metadata stays in sync with code.
 */
public final class ReportRegistry {

    public record Col(String key, String label, String type) {
        static Col t(String k, String l) { return new Col(k, l, "text"); }
        static Col m(String k, String l) { return new Col(k, l, "money"); }
        static Col n(String k, String l) { return new Col(k, l, "int"); }
        static Col d(String k, String l) { return new Col(k, l, "date"); }
    }

    public record Def(String key, String name, String domain, String service,
                      String path, String permission, List<Col> columns,
                      boolean exportable, boolean paged) {}

    private ReportRegistry() {}

    private static Def d(String key, String name, String domain, String service,
                         String path, String perm, List<Col> cols) {
        return new Def(key, name, domain, service, path, perm, cols, true, true);
    }

    public static final List<Def> DEFS = List.of(
        // ---------- PROPERTY ----------
        d("property-register", "Property Register", "PROPERTY", "PROPERTY", "/api/v1/properties",
          "PROPERTY_REPORT_VIEW", List.of(Col.t("code","Code"), Col.t("name","Name"),
          Col.t("propertyType","Type"), Col.t("status","Status"), Col.m("acquisitionCost","Acquisition Cost"),
          Col.d("acquisitionDate","Acquired"), Col.n("buildingCount","Buildings"))),
        d("building-register", "Building Register", "PROPERTY", "PROPERTY", "/api/v1/buildings",
          "PROPERTY_REPORT_VIEW", List.of(Col.t("code","Code"), Col.t("name","Name"),
          Col.t("propertyName","Property"), Col.n("numberOfFloors","Floors"), Col.n("apartmentCount","Apartments"),
          Col.t("status","Status"))),
        d("apartment-register", "Apartment Register", "PROPERTY", "PROPERTY", "/api/v1/apartments",
          "PROPERTY_REPORT_VIEW", List.of(Col.t("apartmentNumber","No."), Col.t("buildingName","Building"),
          Col.t("propertyName","Property"), Col.n("bedrooms","Beds"), Col.m("monthlyRent","Monthly Rent"),
          Col.t("status","Status"))),
        d("tenant-register", "Tenant Register", "PROPERTY", "PROPERTY", "/api/v1/tenants",
          "PROPERTY_REPORT_VIEW", List.of(Col.t("code","Code"), Col.t("name","Name"),
          Col.t("tenantType","Type"), Col.t("email","Email"), Col.t("phone","Phone"), Col.t("status","Status"))),
        d("rental-agreements", "Rental Agreements", "PROPERTY", "PROPERTY", "/api/v1/rental-agreements",
          "PROPERTY_REPORT_VIEW", List.of(Col.t("agreementNumber","Agreement"), Col.t("tenantName","Tenant"),
          Col.t("propertyName","Property"), Col.m("monthlyRent","Rent"), Col.d("startDate","Start"),
          Col.d("endDate","End"), Col.t("status","Status"))),
        d("maintenance-report", "Maintenance Report", "PROPERTY", "PROPERTY", "/api/v1/maintenance",
          "PROPERTY_REPORT_VIEW", List.of(Col.t("requestNumber","Request"), Col.t("propertyName","Property"),
          Col.t("apartmentNumber","Apt"), Col.t("category","Category"), Col.t("priority","Priority"),
          Col.t("status","Status"), Col.d("createdAt","Created"))),
        // ---------- LAND ----------
        d("land-parcel-register", "Land Parcel Register", "LAND", "LAND", "/api/v1/land-parcels",
          "LAND_REPORT_VIEW", List.of(Col.t("parcelReference","Reference"), Col.t("name","Name"),
          Col.t("classification","Classification"), Col.t("status","Status"), Col.m("areaSqm","Area (sqm)"),
          Col.t("region","Region"), Col.t("division","Division"), Col.t("locality","Locality"))),
        d("land-acquisitions", "Land Acquisitions", "LAND", "LAND", "/api/v1/land-acquisitions",
          "LAND_REPORT_VIEW", List.of(Col.t("acquisitionReference","Reference"), Col.t("parcelReference","Parcel"),
          Col.t("sellerName","Seller"), Col.m("purchasePrice","Price"), Col.d("acquisitionDate","Date"),
          Col.t("status","Status"))),
        d("land-sales", "Land Sales", "LAND", "LAND", "/api/v1/land-sales",
          "LAND_REPORT_VIEW", List.of(Col.t("saleReference","Reference"), Col.t("parcelReference","Parcel"),
          Col.t("buyerName","Buyer"), Col.m("agreedPrice","Price"), Col.d("agreementDate","Date"),
          Col.t("status","Status"))),
        d("land-transfers", "Ownership Transfers", "LAND", "LAND", "/api/v1/land-transfers",
          "LAND_REPORT_VIEW", List.of(Col.t("transferReference","Reference"), Col.t("parcelReference","Parcel"),
          Col.t("status","Status"), Col.d("createdAt","Created"))),
        d("land-surveys", "Survey Register", "LAND", "LAND", "/api/v1/land-surveys",
          "LAND_REPORT_VIEW", List.of(Col.t("surveyReference","Reference"), Col.t("parcelReference","Parcel"),
          Col.t("surveyorName","Surveyor"), Col.d("surveyDate","Date"), Col.t("status","Status"))),
        // ---------- FINANCE ----------
        d("payments-register", "Rent Collection Register", "FINANCE", "FINANCE", "/api/v1/rent-collections/payments",
          "FINANCIAL_REPORT_VIEW", List.of(Col.t("paymentRef","Reference"), Col.m("amount","Amount"),
          Col.t("paymentMethod","Method"), Col.t("status","Status"), Col.d("paymentDate","Date"),
          Col.t("recordedByName","Recorded By"))),
        d("expense-register", "Expense Report", "FINANCE", "FINANCE", "/api/v1/expenses",
          "FINANCIAL_REPORT_VIEW", List.of(Col.t("expenseRef","Reference"), Col.t("category","Category"),
          Col.t("description","Description"), Col.m("amount","Amount"), Col.t("status","Status"),
          Col.d("expenseDate","Date"))),
        d("refund-register", "Refund Report", "FINANCE", "FINANCE", "/api/v1/refunds",
          "FINANCIAL_REPORT_VIEW", List.of(Col.t("refundRef","Reference"), Col.m("amount","Amount"),
          Col.t("reason","Reason"), Col.t("status","Status"), Col.d("createdAt","Requested"))),
        d("income-register", "Other Income Report", "FINANCE", "FINANCE", "/api/v1/other-income",
          "FINANCIAL_REPORT_VIEW", List.of(Col.t("incomeRef","Reference"), Col.t("category","Category"),
          Col.m("amount","Amount"), Col.t("status","Status"), Col.d("incomeDate","Date"))),
        d("ledger-report", "Financial Ledger", "FINANCE", "FINANCE", "/api/v1/ledger",
          "FINANCIAL_REPORT_VIEW", List.of(Col.t("entryRef","Entry"), Col.t("entryType","Type"),
          Col.m("amount","Amount"), Col.t("description","Description"), Col.d("createdAt","Posted"))),
        d("finance-collections", "Collection Report", "FINANCE", "FINANCE", "/api/v1/reports/collections",
          "FINANCIAL_REPORT_VIEW", List.of(Col.t("date","Date"), Col.m("total","Total"),
          Col.n("count","Count"))),
        d("rent-arrears", "Rent Arrears", "FINANCE", "FINANCE", "/api/v1/reports/rent-arrears",
          "FINANCIAL_REPORT_VIEW", List.of(Col.t("agreementNumber","Agreement"), Col.t("tenantName","Tenant"),
          Col.m("outstanding","Outstanding"), Col.t("overdueCount","Overdue Schedules"))),
        d("payment-method-summary", "Payment Method Summary", "FINANCE", "FINANCE",
          "/api/v1/reports/payment-method-summary", "FINANCIAL_REPORT_VIEW",
          List.of(Col.t("method","Method"), Col.m("total","Total"), Col.n("count","Count"))),
        // ---------- WORKFLOW ----------
        d("workflow-requests", "Workflow Requests", "WORKFLOW", "WORKFLOW", "/api/v1/workflows",
          "WORKFLOW_REPORT_VIEW", List.of(Col.t("workflowRef","Reference"), Col.t("definitionName","Workflow"),
          Col.t("module","Module"), Col.t("status","Status"), Col.t("submittedByName","Submitted By"),
          Col.d("submittedAt","Submitted"), Col.n("currentLevel","Level"))),
        d("workflow-delegations", "Delegated Approvals", "WORKFLOW", "WORKFLOW", "/api/v1/delegations/mine",
          "WORKFLOW_REPORT_VIEW", List.of(Col.t("delegatorId","Delegator"), Col.t("delegateName","Delegate"),
          Col.t("module","Scope"), Col.d("startDate","From"), Col.d("endDate","To"), Col.t("status","Status"))),
        // ---------- DOCUMENT ----------
        d("document-register", "Document Register", "DOCUMENT", "DOCUMENT", "/api/v1/documents",
          "DOCUMENT_REPORT_VIEW", List.of(Col.t("documentRef","Reference"), Col.t("originalFilename","Filename"),
          Col.t("categoryName","Category"), Col.m("fileSize","Size"), Col.t("status","Status"),
          Col.d("createdAt","Uploaded"), Col.t("uploadedByName","By"))),
        d("document-expiring", "Expiring Documents", "DOCUMENT", "DOCUMENT", "/api/v1/documents/expiry?bucket=30",
          "DOCUMENT_REPORT_VIEW", List.of(Col.t("documentRef","Reference"), Col.t("originalFilename","Filename"),
          Col.t("categoryName","Category"), Col.d("expiryDate","Expiry"))),
        // ---------- EMPLOYEE / AUDIT ----------
        d("employee-register", "Employee Register", "EMPLOYEE", "IDENTITY", "/api/v1/users",
          "EMPLOYEE_REPORT_VIEW", List.of(Col.t("employeeCode","Code"), Col.t("username","Username"),
          Col.t("firstName","First Name"), Col.t("lastName","Last Name"), Col.t("email","Email"))),
        d("login-activity", "Login Activity", "AUDIT", "IDENTITY", "/api/v1/login-history",
          "AUDIT_REPORT_VIEW", List.of(Col.t("username","User"), Col.t("status","Status"),
          Col.t("ipAddress","IP"), Col.d("loginAt","Login At"))),
        d("audit-logs", "Audit Logs", "AUDIT", "IDENTITY", "/api/v1/audit-logs",
          "AUDIT_REPORT_VIEW", List.of(Col.t("username","User"), Col.t("action","Action"),
          Col.t("entityType","Entity"), Col.t("detail","Detail"), Col.d("createdAt","At")))
    );

    public static Optional<Def> find(String key) {
        return DEFS.stream().filter(x -> x.key().equals(key)).findFirst();
    }

    public static List<Def> byDomain(String domain) {
        return DEFS.stream().filter(x -> x.domain().equalsIgnoreCase(domain)).toList();
    }
}
