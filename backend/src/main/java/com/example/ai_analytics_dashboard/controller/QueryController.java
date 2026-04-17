package com.example.ai_analytics_dashboard.controller;

import com.example.ai_analytics_dashboard.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "https://ai-analytics-dashboard-sigma.vercel.app"
})
public class QueryController {

    @Autowired
    private AIService aiService;

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");

            System.out.println("Query received: " + question);

            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Question cannot be empty"));
            }

            Map<String, Object> response = aiService.processQuery(question);

            System.out.println("Query processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Query failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Query failed: " + e.getMessage()));
        }
    }
}
