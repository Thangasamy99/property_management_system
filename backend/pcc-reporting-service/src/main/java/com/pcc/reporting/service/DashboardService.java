package com.pcc.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcc.reporting.integration.SourceGateway;
import com.pcc.reporting.integration.SourceGateway.SourceResult;
import com.pcc.reporting.integration.SourceGateway.Spec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Builds consolidated dashboards from owning services' real endpoints.
 * Every dashboard carries a `sources` array — a section built from a down
 * service is omitted, never zero-filled.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SourceGateway gateway;
    private final CacheService cache;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final int CAP = 1000;

    public record SourceHealth(String service, String status, Instant fetchedAt,
                               String error, boolean cached) {}

    public ObjectNode executive(String period, String from, String to) {
        var results = gateway.getAll(List.of(
                new Spec("PROPERTY", "/api/v1/properties?size=1"),
                new Spec("PROPERTY", "/api/v1/buildings?size=1"),
                new Spec("PROPERTY", "/api/v1/apartments?size=1"),
                new Spec("PROPERTY", "/api/v1/rental-agreements?size=1"),
                new Spec("LAND", "/api/v1/land-parcels/summary"),
                new Spec("FINANCE", "/api/v1/dashboard/summary"),
                new Spec("FINANCE", collectionsPath(period, from, to)),
                new Spec("WORKFLOW", "/api/v1/workflows/dashboard"),
                new Spec("DOCUMENT", "/api/v1/dashboard"),
                new Spec("DOCUMENT", "/api/v1/documents/expiry/summary")));

        ObjectNode root = mapper.createObjectNode();
        root.put("generatedAt", Instant.now().toString());
        root.put("period", period);
        root.set("sources", sourcesNode(results));

        // --- property portfolio ---
        ObjectNode prop = root.putObject("property");
        putTotal(prop, "totalProperties", results.get(0));
        putTotal(prop, "totalBuildings", results.get(1));
        var aptsRes = gateway.getAllRows("PROPERTY", "/api/v1/apartments", 100, CAP);
        var agrsRes = gateway.getAllRows("PROPERTY", "/api/v1/rental-agreements", 100, CAP);
        if (aptsRes.ok()) {
            var apts = rows(aptsRes);
            long occupied = 0, vacant = 0;
            for (JsonNode a : apts) {
                String s = a.path("status").asText("");
                if ("OCCUPIED".equals(s)) occupied++;
                else if ("VACANT".equals(s) || "AVAILABLE".equals(s)) vacant++;
            }
            prop.put("totalApartments", apts.size());
            prop.put("occupiedApartments", occupied);
            prop.put("vacantApartments", vacant);
            prop.put("occupancyPercent",
                    apts.size() == 0 ? 0 : Math.round(occupied * 1000.0 / apts.size()) / 10.0);
        }
        if (agrsRes.ok()) {
            var agrs = rows(agrsRes);
            long active = 0, expiring = 0;
            LocalDate horizon = LocalDate.now().plusDays(60);
            for (JsonNode g : agrs) {
                if ("ACTIVE".equals(g.path("status").asText(""))) {
                    active++;
                    String end = g.path("endDate").asText(null);
                    if (end != null && !LocalDate.parse(end).isAfter(horizon)) expiring++;
                }
            }
            prop.put("totalAgreements", agrs.size());
            prop.put("activeAgreements", active);
            prop.put("expiringAgreements", expiring);
        }

        // --- land portfolio ---
        if (results.get(4).ok()) {
            JsonNode s = results.get(4).data();
            ObjectNode land = root.putObject("land");
            land.put("totalParcels", s.path("totalParcels").asLong());
            land.put("totalAreaSqm", s.path("totalAreaSqm").decimalValue());
            land.set("parcelsByStatus", s.path("parcelsByStatus"));
            land.put("totalSales", s.path("totalSales").asLong());
            land.put("pendingTransfers", s.path("pendingTransfers").asLong());
        }

        // --- finance ---
        if (results.get(5).ok()) root.set("finance", results.get(5).data());
        if (results.get(6).ok()) {
            ObjectNode fin = root.with("finance");
            fin.set("periodCollections", results.get(6).data());
        }

        // --- workflow / documents ---
        if (results.get(7).ok()) root.set("workflow", results.get(7).data());
        if (results.get(8).ok()) root.set("documents", results.get(8).data());
        if (results.get(9).ok()) {
            root.with("documents").set("expiry", results.get(9).data());
        }
        return root;
    }

    public ObjectNode domain(String domain, String period, String from, String to) {
        return switch (domain.toUpperCase()) {
            case "PROPERTY" -> propertyDash();
            case "LAND" -> landDash();
            case "FINANCE" -> financeDash(period, from, to);
            case "WORKFLOW" -> singleSource("WORKFLOW", "/api/v1/workflows/dashboard", "workflow");
            case "DOCUMENTS", "DOCUMENT" -> documentDash();
            default -> executive(period, from, to);
        };
    }

    private ObjectNode propertyDash() {
        var r = gateway.getAll(List.of(
                new Spec("PROPERTY", "/api/v1/properties?size=1"),
                new Spec("PROPERTY", "/api/v1/buildings?size=1"),
                new Spec("PROPERTY", "/api/v1/apartments?size=1"),
                new Spec("PROPERTY", "/api/v1/rental-agreements?size=1"),
                new Spec("PROPERTY", "/api/v1/maintenance?size=1")));
        ObjectNode root = base("PROPERTY", r);
        ObjectNode sec = root.putObject("summary");
        var propsRes = gateway.getAllRows("PROPERTY", "/api/v1/properties", 100, CAP);
        var aptsRes2 = gateway.getAllRows("PROPERTY", "/api/v1/apartments", 100, CAP);
        var agrsRes2 = gateway.getAllRows("PROPERTY", "/api/v1/rental-agreements", 100, CAP);
        if (propsRes.ok()) {
            var props = rows(propsRes);
            sec.put("totalProperties", props.size());
            var byStatus = sec.putObject("propertiesByStatus");
            props.forEach(p -> byStatus.put(p.path("status").asText("?"),
                    byStatus.path(p.path("status").asText("?")).asInt(0) + 1));
        }
        putTotal(sec, "totalBuildings", r.get(1));
        if (aptsRes2.ok()) {
            var apts = rows(aptsRes2);
            var byStatus = sec.putObject("apartmentsByStatus");
            apts.forEach(a -> byStatus.put(a.path("status").asText("?"),
                    byStatus.path(a.path("status").asText("?")).asInt(0) + 1));
            sec.put("totalApartments", apts.size());
        }
        if (agrsRes2.ok()) {
            var agrs = rows(agrsRes2);
            var byStatus = sec.putObject("agreementsByStatus");
            agrs.forEach(g -> byStatus.put(g.path("status").asText("?"),
                    byStatus.path(g.path("status").asText("?")).asInt(0) + 1));
            sec.put("totalAgreements", agrs.size());
        }
        putTotal(sec, "openMaintenance", r.get(4));
        return root;
    }

    private ObjectNode landDash() {
        var r = gateway.getAll(List.of(
                new Spec("LAND", "/api/v1/land-parcels/summary"),
                new Spec("LAND", "/api/v1/land-parcels?size=1"),
                new Spec("LAND", "/api/v1/land-acquisitions?size=1"),
                new Spec("LAND", "/api/v1/land-sales?size=1"),
                new Spec("LAND", "/api/v1/land-transfers?size=1")));
        ObjectNode root = base("LAND", r);
        if (r.get(0).ok()) root.set("summary", r.get(0).data());
        var parcelsRes = gateway.getAllRows("LAND", "/api/v1/land-parcels", 100, CAP);
        if (parcelsRes.ok()) {
            var parcels = rows(parcelsRes);
            var byRegion = root.putObject("areaByRegion");
            var byClass = root.putObject("areaByClassification");
            parcels.forEach(p -> {
                String reg = p.path("region").asText("Unknown");
                String cls = p.path("classification").asText("Unknown");
                byRegion.put(reg, byRegion.path(reg).decimalValue()
                        .add(p.path("areaSqm").decimalValue()));
                byClass.put(cls, byClass.path(cls).decimalValue()
                        .add(p.path("areaSqm").decimalValue()));
            });
        }
        putTotal(root, "totalAcquisitions", r.get(2));
        putTotal(root, "totalSalesCount", r.get(3));
        putTotal(root, "totalTransfers", r.get(4));
        return root;
    }

    private ObjectNode financeDash(String period, String from, String to) {
        var r = gateway.getAll(List.of(
                new Spec("FINANCE", "/api/v1/dashboard/summary"),
                new Spec("FINANCE", collectionsPath(period, from, to)),
                new Spec("FINANCE", "/api/v1/reports/income-vs-expense"
                        + rangeQuery(period, from, to)),
                new Spec("FINANCE", "/api/v1/reports/rent-arrears"),
                new Spec("FINANCE", "/api/v1/reports/payment-method-summary"
                        + rangeQuery(period, from, to))));
        ObjectNode root = base("FINANCE", r);
        if (r.get(0).ok()) root.set("summary", r.get(0).data());
        if (r.get(1).ok()) root.set("periodCollections", r.get(1).data());
        if (r.get(2).ok()) root.set("incomeVsExpense", r.get(2).data());
        if (r.get(3).ok()) root.set("rentArrears", r.get(3).data());
        if (r.get(4).ok()) root.set("paymentMethods", r.get(4).data());
        return root;
    }

    private ObjectNode documentDash() {
        var r = gateway.getAll(List.of(
                new Spec("DOCUMENT", "/api/v1/dashboard"),
                new Spec("DOCUMENT", "/api/v1/documents/expiry/summary"),
                new Spec("DOCUMENT", "/api/v1/categories")));
        ObjectNode root = base("DOCUMENT", r);
        if (r.get(0).ok()) root.set("summary", r.get(0).data());
        if (r.get(1).ok()) root.set("expiry", r.get(1).data());
        if (r.get(2).ok()) {
            var byDomain = root.putObject("categoriesByDomain");
            r.get(2).data().forEach(c -> {
                String dom = c.path("domain").asText("OTHER");
                byDomain.put(dom, byDomain.path(dom).asInt(0) + 1);
            });
        }
        return root;
    }

    private ObjectNode singleSource(String service, String path, String key) {
        var r = List.of(gateway.get(service, path));
        ObjectNode root = base(service, r);
        if (r.get(0).ok()) root.set(key, r.get(0).data());
        return root;
    }

    private String collectionsPath(String period, String from, String to) {
        return "/api/v1/reports/daily-collection" + rangeQuery(period, from, to);
    }

    private String rangeQuery(String period, String from, String to) {
        StringBuilder q = new StringBuilder("?");
        if (from != null && !from.isBlank()) q.append("from=").append(from).append("&");
        if (to != null && !to.isBlank()) q.append("to=").append(to).append("&");
        if (period != null && !period.isBlank()) q.append("period=").append(period);
        return q.toString().replaceAll("[&?]$", "");
    }

    private ObjectNode base(String domain, List<SourceResult> r) {
        ObjectNode root = mapper.createObjectNode();
        root.put("domain", domain);
        root.put("generatedAt", Instant.now().toString());
        root.set("sources", sourcesNode(r));
        return root;
    }

    private JsonNode sourcesNode(List<SourceResult> results) {
        var arr = mapper.createArrayNode();
        Map<String, SourceResult> latest = new LinkedHashMap<>();
        results.forEach(r -> latest.merge(r.service(), r, (a, b) -> a.ok() ? a : b));
        latest.values().forEach(r -> {
            ObjectNode n = arr.addObject();
            n.put("service", r.service());
            n.put("status", r.ok() ? "OK" : "DOWN");
            n.put("fetchedAt", r.fetchedAt().toString());
            if (r.error() != null) n.put("error", r.error());
            n.put("cached", r.cached());
        });
        return arr;
    }

    private JsonNode rows(SourceResult r) {
        JsonNode d = r.data();
        return d != null && d.isArray() ? d : (d == null ? mapper.createArrayNode() : d.path("content"));
    }

    private void putTotal(ObjectNode node, String field, SourceResult r) {
        if (r.ok()) node.put(field, r.data().path("totalElements").asLong(
                r.data().isArray() ? r.data().size() : 0));
    }
}
