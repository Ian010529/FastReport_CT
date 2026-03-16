package com.ct.fastreport.service;

import com.ct.fastreport.dto.ReportResponse;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);

    public ResponseEntity<byte[]> download(ReportResponse report, String format) {
        return switch (format.toLowerCase()) {
            case "pdf" -> buildPdfResponse(report);
            case "csv" -> buildCsvResponse(report);
            default -> buildTxtResponse(report);
        };
    }

    private ResponseEntity<byte[]> buildTxtResponse(ReportResponse report) {
        String txt = buildPlainText(report);
        byte[] bytes = txt.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + report.id + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    private ResponseEntity<byte[]> buildCsvResponse(ReportResponse report) {
        StringBuilder csv = new StringBuilder();
        csv.append("字段,值\n");
        csv.append("报告ID,").append(report.id).append("\n");
        csv.append("客户编号,").append(report.customerId).append("\n");
        csv.append("客户姓名,").append(report.customerName).append("\n");
        csv.append("身份证号,").append(report.nationalId).append("\n");
        csv.append("客户经理,").append(report.managerName).append("\n");
        csv.append("经理工号,").append(report.managerId).append("\n");
        csv.append("业务编码,").append(report.serviceCode).append("\n");
        csv.append("当前套餐,").append(report.currentPlan).append("\n");
        csv.append("附加服务,").append(report.additionalServices != null ? report.additionalServices : "").append("\n");
        csv.append("近6月消费,").append(report.spendingLast6 != null ? report.spendingLast6 : "").append("\n");
        csv.append("状态,").append(report.status).append("\n");
        csv.append("创建时间,").append(report.createdAt).append("\n");
        String content = report.reportContent != null ? report.reportContent.replace("\"", "\"\"") : "";
        csv.append("报告内容,\"").append(content).append("\"\n");

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + report.id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    private ResponseEntity<byte[]> buildPdfResponse(ReportResponse report) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            PdfFont font;
            try {
                String[] fontPaths = {
                        "/System/Library/Fonts/STHeiti Light.ttc,0",
                        "/System/Library/Fonts/PingFang.ttc,0",
                        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                        "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"
                };
                PdfFont loaded = null;
                for (String path : fontPaths) {
                    try {
                        String clean = path.contains(",") ? path.split(",")[0] : path;
                        if (new java.io.File(clean).exists()) {
                            loaded = PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H);
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
                font = loaded != null ? loaded : PdfFontFactory.createFont();
            } catch (Exception e) {
                font = PdfFontFactory.createFont();
            }

            doc.setFont(font).setFontSize(11);
            doc.add(new Paragraph("客户服务优化报告 #" + report.id).setFontSize(16).setBold());
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("客户: " + report.customerName + " (" + report.customerId + ")"));
            doc.add(new Paragraph("客户经理: " + report.managerName + " (" + report.managerId + ")"));
            doc.add(new Paragraph("业务编码: " + report.serviceCode));
            doc.add(new Paragraph("当前套餐: " + report.currentPlan));
            doc.add(new Paragraph("创建时间: " + report.createdAt));
            doc.add(new Paragraph(" "));

            if (report.reportContent != null) {
                for (String line : report.reportContent.split("\n")) {
                    Paragraph p = new Paragraph(line);
                    if (line.startsWith("#")) {
                        p.setBold().setFontSize(13);
                    }
                    doc.add(p);
                }
            }

            doc.close();
            byte[] bytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + report.id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            log.error("PDF generation failed for report {}", report.id, e);
            return buildTxtResponse(report);
        }
    }

    private String buildPlainText(ReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  客户服务优化报告 #").append(report.id).append("\n");
        sb.append("========================================\n\n");
        sb.append("客户编号: ").append(report.customerId).append("\n");
        sb.append("客户姓名: ").append(report.customerName).append("\n");
        sb.append("身份证号: ").append(report.nationalId).append("\n");
        sb.append("客户经理: ").append(report.managerName).append(" (").append(report.managerId).append(")\n");
        sb.append("业务编码: ").append(report.serviceCode).append("\n");
        sb.append("当前套餐: ").append(report.currentPlan).append("\n");
        sb.append("创建时间: ").append(report.createdAt).append("\n");
        sb.append("\n────────────────────────────────────────\n\n");
        sb.append(report.reportContent != null ? report.reportContent : "（无内容）");
        sb.append("\n");
        return sb.toString();
    }
}
