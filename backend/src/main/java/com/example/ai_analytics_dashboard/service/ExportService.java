package com.example.ai_analytics_dashboard.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class ExportService {

    public byte[] exportToExcel(List<Map<String, Object>> chartData, String queryQuestion) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Results");

            if (chartData == null || chartData.isEmpty()) {
                Row row = sheet.createRow(0);
                row.createCell(0).setCellValue("No data to export");
            } else {
                Set<String> allKeys = new HashSet<>();
                for (Map<String, Object> item : chartData) {
                    if (item != null) {
                        allKeys.addAll(item.keySet());
                    }
                }

                java.util.List<String> headers = new java.util.ArrayList<>();
                for (String key : allKeys) {
                    if (!"value".equals(key)) {
                        headers.add(key);
                    }
                }
                if (allKeys.contains("value")) {
                    headers.add("value");
                }

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                }

                int rowNum = 1;
                for (Map<String, Object> data : chartData) {
                    if (data == null) continue;
                    Row row = sheet.createRow(rowNum++);
                    
                    for (int i = 0; i < headers.size(); i++) {
                        String key = headers.get(i);
                        Object value = data.get(key);
                        Cell cell = row.createCell(i);
                        
                        if (value == null) {
                            cell.setCellValue("");
                        } else if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }

                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            workbook.write(outputStream);
            System.out.println("Excel file generated successfully");
            return outputStream.toByteArray();

        } catch (Exception e) {
            System.err.println("Excel export error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    public String exportToCSV(List<Map<String, Object>> chartData) {
        StringBuilder csv = new StringBuilder();

        if (chartData == null || chartData.isEmpty()) {
            csv.append("Category,Value\n");
            csv.append("No data,0\n");
            return csv.toString();
        }

        Set<String> allKeys = new HashSet<>();
        for (Map<String, Object> item : chartData) {
            if (item != null) {
                allKeys.addAll(item.keySet());
            }
        }

        java.util.List<String> headers = new java.util.ArrayList<>();
        for (String key : allKeys) {
            if (!"value".equals(key)) {
                headers.add(key);
            }
        }
        if (allKeys.contains("value")) {
            headers.add("value");
        }

        for (int i = 0; i < headers.size(); i++) {
            csv.append(escapeCSV(headers.get(i)));
            if (i < headers.size() - 1) csv.append(",");
        }
        csv.append("\n");

        for (Map<String, Object> data : chartData) {
            if (data == null) continue;
            
            for (int i = 0; i < headers.size(); i++) {
                String key = headers.get(i);
                Object value = data.get(key);
                csv.append(escapeCSV(value != null ? value.toString() : ""));
                if (i < headers.size() - 1) csv.append(",");
            }
            csv.append("\n");
        }

        System.out.println("CSV generated successfully");
        return csv.toString();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
