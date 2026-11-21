package com.pcc.reporting.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pcc.reporting.entity.ReportExportLog;
import com.pcc.reporting.repository.ReportExportLogRepository;
import com.pcc.reporting.security.UserPrincipal;
import com.pcc.reporting.service.ReportService.ReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** CSV / XLSX / PDF exporters with PCC branding headers. Never writes
 *  service URLs, secrets, or storage paths into output. */
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ReportExportLogRepository exportLogs;

    public record Exported(byte[] bytes, String filename, String contentType) {}

    private static final DateTimeFormatter FN =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    public Exported export(ReportResult r, String format, Map<String, String> filters,
                           UserPrincipal actor) {
        String safe = r.reportKey().replaceAll("[^a-zA-Z0-9-]", "");
        Exported out = switch (format.toUpperCase()) {
            case "XLSX" -> new Exported(xlsx(r), "pcc-" + safe + "-" + FN.format(LocalDateTime.now()) + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "PDF" -> new Exported(pdf(r, filters, actor), "pcc-" + safe + "-" + FN.format(LocalDateTime.now()) + ".pdf",
                    "application/pdf");
            default -> new Exported(csv(r), "pcc-" + safe + "-" + FN.format(LocalDateTime.now()) + ".csv",
                    "text/csv;charset=UTF-8");
        };
        logExport(r.reportKey(), format.toUpperCase(), out.filename(), r.rows().size(), actor);
        return out;
    }

    private byte[] csv(ReportResult r) {
        var baos = new ByteArrayOutputStream();
        var w = new PrintWriter(baos, true, StandardCharsets.UTF_8);
        w.println("PCC — " + r.name());
        w.println("Generated: " + r.generatedAt());
        w.println(r.columns().stream().map(c -> esc(c.label())).reduce((a, b) -> a + "," + b).orElse(""));
        for (var row : r.rows()) {
            w.println(r.columns().stream()
                    .map(c -> esc(fmt(row.get(c.key()), c.type())))
                    .reduce((a, b) -> a + "," + b).orElse(""));
        }
        if (!r.totals().isEmpty()) {
            w.println(r.columns().stream()
                    .map(c -> r.totals().containsKey(c.key()) ? esc(xaf(r.totals().get(c.key()))) : "")
                    .reduce((a, b) -> a + "," + b).orElse(""));
        }
        w.flush();
        return baos.toByteArray();
    }

    private byte[] xlsx(ReportResult r) {
        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var baos = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("Report");
            var titleStyle = wb.createCellStyle();
            var bold = wb.createFont(); bold.setBold(true); bold.setFontHeightInPoints((short) 13);
            titleStyle.setFont(bold);
            var headStyle = wb.createCellStyle();
            var hFont = wb.createFont(); hFont.setBold(true); headStyle.setFont(hFont);
            headStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LAVENDER.getIndex());
            headStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            int rowIdx = 0;
            var title = sheet.createRow(rowIdx++).createCell(0);
            title.setCellValue("PCC — " + r.name()); title.setCellStyle(titleStyle);
            sheet.createRow(rowIdx++).createCell(0)
                    .setCellValue("Generated: " + r.generatedAt() + (r.truncated() ? "  (truncated)" : ""));
            rowIdx++;
            var head = sheet.createRow(rowIdx++);
            for (int i = 0; i < r.columns().size(); i++) {
                var c = head.createCell(i); c.setCellValue(r.columns().get(i).label()); c.setCellStyle(headStyle);
            }
            for (var row : r.rows()) {
                var xr = sheet.createRow(rowIdx++);
                for (int i = 0; i < r.columns().size(); i++) {
                    Object v = row.get(r.columns().get(i).key());
                    var cell = xr.createCell(i);
                    if (v instanceof Number num) cell.setCellValue(num.doubleValue());
                    else cell.setCellValue(v == null ? "" : String.valueOf(v));
                }
            }
            if (!r.totals().isEmpty()) {
                var tr = sheet.createRow(rowIdx++);
                for (int i = 0; i < r.columns().size(); i++) {
                    var col = r.columns().get(i);
                    var cell = tr.createCell(i); cell.setCellStyle(headStyle);
                    if (i == 0) cell.setCellValue("TOTAL");
                    else if (r.totals().containsKey(col.key()))
                        cell.setCellValue(r.totals().get(col.key()).doubleValue());
                }
            }
            for (int i = 0; i < r.columns().size(); i++) sheet.autoSizeColumn(i);
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("XLSX generation failed", e);
        }
    }

    private byte[] pdf(ReportResult r, Map<String, String> filters, UserPrincipal actor) {
        var baos = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4.rotate(), 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            var h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            var h2 = FontFactory.getFont(FontFactory.HELVETICA, 9);
            var head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            var cell = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
            doc.add(new Paragraph("Presbyterian Church in Cameroon", h1));
            doc.add(new Paragraph(r.name(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            doc.add(new Paragraph("Generated: " + r.generatedAt() + "  by " + actor.getUsername(), h2));
            if (filters != null && !filters.isEmpty())
                doc.add(new Paragraph("Filters: " + filters, h2));
            if (r.truncated())
                doc.add(new Paragraph("Note: result truncated at " + r.rows().size() + " rows.", h2));
            var downSources = r.sources().stream().filter(s -> !"OK".equals(s.status())).toList();
            if (!downSources.isEmpty())
                doc.add(new Paragraph("Warning: partial data — unavailable sources: "
                        + downSources.stream().map(ReportService.SourceHealth::service).toList(), h2));
            doc.add(Chunk.NEWLINE);

            var table = new PdfPTable(r.columns().size());
            table.setWidthPercentage(100);
            r.columns().forEach(c -> {
                var p = new PdfPCell(new Phrase(c.label(), head));
                p.setBackgroundColor(new java.awt.Color(0xE9, 0xE3, 0xF5));
                table.addCell(p);
            });
            for (var row : r.rows())
                r.columns().forEach(c ->
                        table.addCell(new PdfPCell(new Phrase(fmt(row.get(c.key()), c.type()), cell))));
            if (!r.totals().isEmpty()) {
                r.columns().forEach(c -> {
                    var p = new PdfPCell(new Phrase(
                            r.totals().containsKey(c.key()) ? xaf(r.totals().get(c.key())) : "", head));
                    table.addCell(p);
                });
            }
            doc.add(table);
            doc.add(new Paragraph("Rows: " + r.rows().size(), h2));
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF generation failed", e);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    private void logExport(String key, String format, String filename, int rows, UserPrincipal actor) {
        try {
            ReportExportLog l = new ReportExportLog();
            l.setReportKey(key); l.setFormat(format); l.setFilename(filename);
            l.setExportedById(actor.getId()); l.setExportedBy(actor.getUsername());
            l.setRowCount(rows);
            exportLogs.save(l);
        } catch (Exception ignored) { }
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.contains(",") || v.contains("\"") || v.contains("\n")
                ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    private String fmt(Object v, String type) {
        if (v == null) return "";
        if ("money".equals(type) && v instanceof Number num)
            return xaf(new BigDecimal(num.toString()));
        return String.valueOf(v);
    }

    private String xaf(BigDecimal v) {
        return "XAF " + v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
