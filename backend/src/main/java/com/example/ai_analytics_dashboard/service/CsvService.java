package com.example.ai_analytics_dashboard.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

@Service
public class CsvService {
    
    private List<Map<String, String>> data = new ArrayList<>();
    private String currentFileName = null;
    private List<String> columnNames = new ArrayList<>();
    public void processCsv(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim()
                    .parse(reader);

            data.clear();
            columnNames.clear();

            columnNames = new ArrayList<>(parser.getHeaderNames());
            currentFileName = file.getOriginalFilename();

            for (CSVRecord record : parser) {
                Map<String, String> row = new HashMap<>();
                for (String header : parser.getHeaderNames()) {
                    row.put(header, record.get(header));
                }
                data.add(row);
            }

            System.out.println("Loaded " + data.size() + " rows from " + currentFileName);
            System.out.println("Columns: " + columnNames);

        } catch (Exception e) {
            System.err.println("CSV Processing Error: " + e.getMessage());
            throw new RuntimeException("Failed to process CSV: " + e.getMessage(), e);
        }
    }

    public List<Map<String, String>> getData() {
        return new ArrayList<>(data);
    }

    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public Map<String, Object> getDatasetInfo() {
        Map<String, Object> info = new HashMap<>();
        
        if (data.isEmpty()) {
            info.put("uploaded", false);
            info.put("message", "No data uploaded");
            return info;
        }

        info.put("uploaded", true);
        info.put("fileName", currentFileName);
        info.put("rows", data.size());
        info.put("columns", columnNames);
        info.put("sample", data.stream().limit(5).toList());

        return info;
    }

    public boolean hasData() {
        return !data.isEmpty();
    }

    public void clearData() {
        data.clear();
        columnNames.clear();
        currentFileName = null;
        System.out.println("Data cleared");
    }
}
