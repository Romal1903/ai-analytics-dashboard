package com.example.ai_analytics_dashboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model.name}")
    private String modelName;

    private final RestTemplate restTemplate = createRestTemplate();

    public Map<String, Object> generateDataSummary(List<Map<String, String>> data) {
        try {
            if (data == null || data.isEmpty()) {
                return Map.of("error", "No data available");
            }

            System.out.println("Generating summary for " + data.size() + " rows");

            Map<String, Object> summary = new HashMap<>();
            summary.put("total_rows", data.size());
            summary.put("total_columns", data.get(0).keySet().size());
            summary.put("columns", new ArrayList<>(data.get(0).keySet()));

            Map<String, Map<String, Object>> columnStats = analyzeColumns(data);
            summary.put("column_statistics", columnStats);

            String aiInsights = generateAIInsights(data, columnStats);
            summary.put("ai_insights", aiInsights);

            return summary;

        } catch (Exception e) {
            System.err.println("Summary generation error: " + e.getMessage());
            Map<String, Object> summary = new HashMap<>();
            summary.put("total_rows", data != null ? data.size() : 0);
            summary.put("total_columns", data != null && !data.isEmpty() ? data.get(0).keySet().size() : 0);
            summary.put("columns", data != null && !data.isEmpty() ? new ArrayList<>(data.get(0).keySet()) : new ArrayList<>());
            summary.put("ai_insights", "");
            return summary;
        }
    }

    private Map<String, Map<String, Object>> analyzeColumns(List<Map<String, String>> data) {
        Map<String, Map<String, Object>> columnStats = new HashMap<>();

        for (String column : data.get(0).keySet()) {
            Map<String, Object> stats = new HashMap<>();
            boolean isNumeric = isNumericColumn(data, column);
            stats.put("type", isNumeric ? "numeric" : "text");

            if (isNumeric) {
                List<Double> values = data.stream()
                        .map(row -> parseDouble(row.get(column)))
                        .filter(v -> v != 0)
                        .collect(Collectors.toList());

                if (!values.isEmpty()) {
                    stats.put("min", values.stream().min(Double::compareTo).orElse(0.0));
                    stats.put("max", values.stream().max(Double::compareTo).orElse(0.0));
                    stats.put("avg", values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                }
            } else {
                Set<String> uniqueValues = data.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                stats.put("unique_values", uniqueValues.size());
                stats.put("sample_values", uniqueValues.stream().limit(5).collect(Collectors.toList()));
            }

            columnStats.put(column, stats);
        }

        return columnStats;
    }

    private String generateAIInsights(List<Map<String, String>> data, 
                                     Map<String, Map<String, Object>> columnStats) {
        try {
            if (data == null || data.isEmpty()) {
                return "";
            }
            
            String dataContext = buildDataContext(data, columnStats);

            String prompt = String.format("""
                Analyze this dataset and provide 3-5 KEY insights in bullet points.
                
                %s
                
                Requirements:
                - Be specific and actionable
                - Focus on interesting patterns or notable values
                - Keep each insight to one concise sentence
                - Use bullet points (-)
                
                Return ONLY the bullet points, nothing else.
                """, dataContext);

            String result = callGroqAPI(prompt);
            
            return (result != null && !result.trim().isEmpty()) ? result.trim() : "";

        } catch (Exception e) {
            System.err.println("Could not generate AI insights: " + e.getMessage());
            return "";
        }
    }

    private String callGroqAPI(String prompt) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 800);
        

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        
        if (response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String result = (String) message.get("content");
                if (result != null && !result.trim().isEmpty()) {
                    System.out.println("Groq insights generated");
                    return result;
                }
            }
        }
        
        throw new Exception("Invalid response from Groq API");
    }
    private String buildDataContext(List<Map<String, String>> data, 
                                    Map<String, Map<String, Object>> columnStats) {
        StringBuilder context = new StringBuilder();
        context.append("Dataset Summary:\n");
        context.append("- Total Records: ").append(data.size()).append("\n");
        
        int count = 0;
        for (Map.Entry<String, Map<String, Object>> entry : columnStats.entrySet()) {
            if (count++ >= 5) break;
            
            String column = entry.getKey();
            Map<String, Object> stats = entry.getValue();
            
            context.append("- ").append(column).append(" (").append(stats.get("type")).append("): ");
            
            if ("numeric".equals(stats.get("type"))) {
                context.append(String.format("min=%.1f, max=%.1f, avg=%.1f", 
                    ((Number) stats.getOrDefault("min", 0)).doubleValue(),
                    ((Number) stats.getOrDefault("max", 0)).doubleValue(),
                    ((Number) stats.getOrDefault("avg", 0)).doubleValue()));
            } else {
                context.append(stats.get("unique_values")).append(" unique values");
            }
            context.append("\n");
        }

        return context.toString();
    }

    private boolean isNumericColumn(List<Map<String, String>> data, String column) {
        int numericCount = 0;
        int sampleSize = Math.min(50, data.size());

        for (int i = 0; i < sampleSize; i++) {
            try {
                String value = data.get(i).get(column);
                if (value != null && !value.trim().isEmpty()) {
                    Double.parseDouble(value);
                    numericCount++;
                }
            } catch (Exception ignored) {}
        }

        return numericCount > sampleSize * 0.8;
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        return new RestTemplate(factory);
    }

    private double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value.trim()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
