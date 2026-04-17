package com.example.ai_analytics_dashboard.controller;

import com.example.ai_analytics_dashboard.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "https://your-actual-app.vercel.app"
})
public class ExportController {

    @Autowired
    private ExportService exportService;

    @PostMapping("/excel")
    public ResponseEntity<Resource> exportToExcel(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("Excel export request");
            
            Object chartDataObj = request.get("chartData");
            if (chartDataObj == null) {
                chartDataObj = request.get("chart_data");
            }
            if (!(chartDataObj instanceof List)) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ByteArrayResource("{\"error\":\"chartData must be an array\"}".getBytes()));
            }
            List<Map<String, Object>> chartData = (List<Map<String, Object>>) chartDataObj;
            String question = (String) request.getOrDefault("question", "");

            if (chartData == null) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ByteArrayResource("{\"error\":\"Missing chartData\"}".getBytes()));
            }

            byte[] excelData = exportService.exportToExcel(chartData, question);

            ByteArrayResource resource = new ByteArrayResource(excelData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=results.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(excelData.length)
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Excel export failed: " + e.getMessage());
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error";
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ByteArrayResource(("{\"error\":\"Excel export failed\",\"message\":\"" + msg + "\"}").getBytes()));
        }
    }

    @PostMapping("/csv")
    public ResponseEntity<String> exportToCSV(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("CSV export request");
            
            Object chartDataObj = request.get("chartData");
            if (chartDataObj == null) {
                chartDataObj = request.get("chart_data");
            }
            if (!(chartDataObj instanceof List)) {
                return ResponseEntity.badRequest().body("chartData must be an array");
            }
            List<Map<String, Object>> chartData = (List<Map<String, Object>>) chartDataObj;

            if (chartData == null) {
                return ResponseEntity.badRequest().body("Missing chartData");
            }

            String csvData = exportService.exportToCSV(chartData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=results.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvData);

        } catch (Exception e) {
            System.err.println("CSV export failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("CSV export failed: " + e.getMessage());
        }
    }
}
