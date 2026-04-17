package com.example.ai_analytics_dashboard.controller;

import com.example.ai_analytics_dashboard.service.CsvService;
import com.example.ai_analytics_dashboard.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*")
public class FileController {

    @Autowired
    private CsvService csvService;

    @Autowired
    private SummaryService summaryService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("Upload request received: " + file.getOriginalFilename());

            if (file.isEmpty()) {
                System.out.println("File is empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                System.out.println("Not a CSV file");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only CSV files are supported"));
            }

            csvService.processCsv(file);

            List<Map<String, String>> data = csvService.getData();
            Map<String, Object> summary = summaryService.generateDataSummary(data);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded successfully!");
            response.put("filename", file.getOriginalFilename());
            response.put("summary", summary);

            System.out.println("Upload successful");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process: " + e.getMessage()));
        }
    }

    @GetMapping("/data/info")
    public ResponseEntity<Map<String, Object>> getDataInfo() {
        Map<String, Object> info = csvService.getDatasetInfo();
        return ResponseEntity.ok(info);
    }

    @GetMapping("/data/summary")
    public ResponseEntity<Map<String, Object>> getDataSummary() {
        try {
            List<Map<String, String>> data = csvService.getData();
            if (data.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "No data uploaded"));
            }

            Map<String, Object> summary = summaryService.generateDataSummary(data);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
