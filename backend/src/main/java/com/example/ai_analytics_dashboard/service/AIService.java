package com.example.ai_analytics_dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AIService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model.name}")
    private String modelName;

    @Autowired
    private CsvService csvService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = createRestTemplate();

    public Map<String, Object> processQuery(String question) {
        try {
            List<Map<String, String>> data = csvService.getData();

            if (data == null || data.isEmpty()) {
                return createErrorResponse("No data uploaded. Please upload a CSV file first.");
            }

            System.out.println("Processing query: " + question);

            String normalizedQuestion = normalizeQuestion(question);
            System.out.println("Normalized question: " + normalizedQuestion);

            Map<String, String> schema = detectDataTypes(data);
            List<String> orderedColumns = csvService.getColumnNames();
            Set<String> columns = new LinkedHashSet<>(orderedColumns);

            String prompt = buildPrompt(normalizedQuestion, columns, schema, data, orderedColumns);
            String aiResponse = callGroqAPI(prompt);

            System.out.println("Raw AI Response: " + aiResponse.substring(0, Math.min(300, aiResponse.length())));

            Map<String, Object> queryPlan = parseAIResponse(aiResponse);
            System.out.println("Parsed Query Plan: " + queryPlan);

            return executeQueryPlan(queryPlan, data, normalizedQuestion, columns, schema);

        } catch (Exception e) {
            System.err.println("Query processing error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Error: " + e.getMessage());
        }
    }

    private String normalizeQuestion(String question) {
        if (question == null) return "";
        return question
                .replaceAll("[\r\n\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String buildPrompt(String question,
                               Set<String> columns,
                               Map<String, String> schema,
                               List<Map<String, String>> data,
                               List<String> orderedColumns) {

        StringBuilder datasetDesc = new StringBuilder();
        datasetDesc.append("COLUMNS: ").append(String.join(", ", orderedColumns)).append("\n");
        datasetDesc.append("TYPES: ");
        orderedColumns.forEach(col ->
                datasetDesc.append(col).append("=").append(schema.getOrDefault(col, "text")).append(" | "));
        datasetDesc.append("\nROW COUNT: ").append(data.size()).append("\n");

        datasetDesc.append("CATEGORICAL VALUES:\n");
        int sampleSize = Math.min(200, data.size());
        for (String col : orderedColumns) {
            if (!"numeric".equals(schema.getOrDefault(col, "text"))) {
                Map<String, Long> freq = new LinkedHashMap<>();
                for (int i = 0; i < sampleSize; i++) {
                    String v = data.get(i).get(col);
                    if (v != null && !v.trim().isEmpty()) {
                        freq.merge(v.trim(), 1L, Long::sum);
                    }
                }
                List<String> topVals = freq.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(8)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                if (!topVals.isEmpty()) {
                    datasetDesc.append("  ").append(col).append(": ").append(topVals).append("\n");
                }
            }
        }

        datasetDesc.append("NUMERIC RANGES:\n");
        for (String col : orderedColumns) {
            if ("numeric".equals(schema.getOrDefault(col, "text"))) {
                OptionalDouble min = data.stream()
                        .mapToDouble(r -> parseDouble(r.get(col))).filter(v -> v != 0).min();
                OptionalDouble max = data.stream()
                        .mapToDouble(r -> parseDouble(r.get(col))).max();
                if (min.isPresent()) {
                    datasetDesc.append("  ").append(col)
                            .append(": min=").append(String.format("%.1f", min.getAsDouble()))
                            .append(", max=").append(String.format("%.1f", max.getAsDouble()))
                            .append("\n");
                }
            }
        }

        datasetDesc.append("SAMPLE ROWS (first 3):\n");
        for (int i = 0; i < Math.min(3, data.size()); i++) {
            datasetDesc.append("  ").append(data.get(i)).append("\n");
        }

        return String.format("""
                You are a data query planner. Convert the user question into a structured JSON query plan.
                Output ONLY valid JSON — no markdown, no prose, no explanation.
                Start with '{' and end with '}'. Use double quotes. No trailing commas.

                DATASET INFO:
                %s

                USER QUESTION: "%s"

                DECISION RULES (read carefully):

                1. IDENTIFY THE INTENT:
                - "which/who has min/max/highest/lowest X" → TOP_N (group_by=entity_col, sort=asc/desc, limit=1)
                - "show X by Y" / "X per Y" / "X for each Y" → GROUP_BY (group_by=Y, aggregation on X)
                - "what is total/avg/sum/count/min/max of X" → AGGREGATE (single number, no group_by)
                - "distribution/breakdown/share of X" → DISTRIBUTION (pie chart)
                - If multiple sub-questions in one query → pick the DOMINANT one (first/main intent)

                2. FILTERS — extract ALL mentioned conditions:
                - "in North region" → filter region=North
                - "for Electronics category" → filter category=Electronics
                - "where sales > 1000" → filter sales gt 1000
                - Match filter values EXACTLY as they appear in CATEGORICAL VALUES above
                - Multiple conditions like "for Electronics in North" → include BOTH as filters

                3. COLUMN RESOLUTION:
                - Match question words to actual column names from COLUMNS above (case-insensitive, partial match ok)
                - Use the EXACT column names from the dataset, not synonyms
                - If column not found, return clarify_question

                4. MULTI-LINE / COMPOUND QUERIES:
                - Treat newlines as spaces; read the full question as one intent
                - If truly two separate questions, answer the FIRST/MAIN question
                - Extract any filter conditions mentioned anywhere in the query

                5. AGGREGATION DEFAULTS:
                - "total/sum" → sum | "average/avg/mean" → avg | "count/how many" → count
                - "min/minimum/lowest/least" → min | "max/maximum/highest/most" → max
                - No keyword → sum (for numeric), count (for text group-by)


                GENERIC EXAMPLES (adapt to ANY dataset):

                Q: "which item has minimum value in price"
                A: {"operation":"TOP_N","target_column":"price","aggregation":"min","group_by":"name","filters":[],"sort":"asc","limit":1,"chart_type":"table","clarify_question":null}

                Q: "show me revenue by region for Electronics"
                A: {"operation":"GROUP_BY","target_column":"revenue","aggregation":"sum","group_by":"region","filters":[{"column":"category","operator":"eq","value":"Electronics"}],"sort":"desc","limit":10,"chart_type":"bar","clarify_question":null}

                Q: "what is the average salary of employees in HR department"
                A: {"operation":"AGGREGATE","target_column":"salary","aggregation":"avg","group_by":null,"filters":[{"column":"department","operator":"eq","value":"HR"}],"sort":null,"limit":10,"chart_type":"table","clarify_question":null}

                Q: "top 5 products by sales in North region"
                A: {"operation":"TOP_N","target_column":"sales","aggregation":"sum","group_by":"product","filters":[{"column":"region","operator":"eq","value":"North"}],"sort":"desc","limit":5,"chart_type":"bar","clarify_question":null}

                Q: "distribution of orders across categories"
                A: {"operation":"DISTRIBUTION","target_column":"category","aggregation":"count","group_by":null,"filters":[],"sort":"desc","limit":10,"chart_type":"pie","clarify_question":null}

                Q: "total sales by region for Q4"
                A: {"operation":"GROUP_BY","target_column":"sales","aggregation":"sum","group_by":"region","filters":[{"column":"quarter","operator":"eq","value":"Q4"}],"sort":"desc","limit":10,"chart_type":"bar","clarify_question":null}


                OUTPUT SCHEMA (all keys required, use null if not applicable):
                {
                "operation": "AGGREGATE|GROUP_BY|TOP_N|DISTRIBUTION|COMPARISON|CLARIFY",
                "target_column": "exact_column_name_or_null",
                "aggregation": "sum|avg|count|min|max|null",
                "group_by": "exact_column_name_or_null",
                "filters": [{"column":"col","operator":"eq|neq|contains|in|gt|gte|lt|lte","value":"val"}],
                "sort": "asc|desc|null",
                "limit": 10,
                "chart_type": "bar|line|pie|table|null",
                "clarify_question": "string_or_null"
                }

                Return ONLY the JSON object. Nothing else.
                """, datasetDesc.toString(), question);
    }

    private String callGroqAPI(String prompt) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";
        System.out.println("🔗 Calling Groq API: " + modelName);

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
        requestBody.put("temperature", 0.0);
        requestBody.put("max_tokens", 800);

        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        requestBody.put("response_format", responseFormat);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String result = (String) message.get("content");
                if (result != null && !result.trim().isEmpty()) {
                    System.out.println("Groq response received (" + result.length() + " chars)");
                    return result;
                }
            }
        }
        throw new Exception("Invalid response from Groq API: " + response.getBody());
    }

    private Map<String, Object> parseAIResponse(String aiResponse) throws Exception {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new Exception("Empty AI response");
        }

        String cleaned = aiResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        cleaned = extractFirstJsonObject(cleaned);
        cleaned = attemptBalanceJson(cleaned);
        cleaned = cleaned.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
        cleaned = sanitizeJsonStringLiterals(cleaned);

        try {
            return mapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            System.out.println("JSON parse failed, attempting repair...");
            String repairPrompt = buildJsonRepairPrompt(aiResponse);
            String repaired = callGroqAPI(repairPrompt);
            String repairedCleaned = extractFirstJsonObject(
                    repaired.replaceAll("```[a-z]*\\s*", "").trim());
            repairedCleaned = attemptBalanceJson(repairedCleaned);
            repairedCleaned = repairedCleaned.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
            repairedCleaned = sanitizeJsonStringLiterals(repairedCleaned);
            try {
                return mapper.readValue(repairedCleaned, Map.class);
            } catch (Exception e2) {
                throw new Exception("Failed to parse AI response as JSON: " + e2.getMessage());
            }
        }
    }

    private String extractFirstJsonObject(String text) {
        if (text == null) return "";
        int start = text.indexOf('{');
        if (start < 0) return text.trim();

        boolean inString = false;
        boolean escape = false;
        int depth = 0;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\' && inString) { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        return text.substring(start);
    }

    private String sanitizeJsonStringLiterals(String json) {
        if (json == null || json.isEmpty()) return json;
        StringBuilder out = new StringBuilder(json.length() + 16);
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) { out.append(c); escape = false; continue; }
            if (c == '\\' && inString) { out.append(c); escape = true; continue; }
            if (c == '"') { out.append(c); inString = !inString; continue; }
            if (inString) {
                if (c == '\n') { out.append("\\n"); continue; }
                if (c == '\r') { out.append("\\r"); continue; }
                if (c == '\t') { out.append("\\t"); continue; }
                if (c < 0x20) { out.append(String.format("\\u%04x", (int) c)); continue; }
            }
            out.append(c);
        }
        return out.toString();
    }

    private String attemptBalanceJson(String jsonLike) {
        if (jsonLike == null) return "";
        int openCurly = 0, closeCurly = 0, openSquare = 0, closeSquare = 0;
        for (char c : jsonLike.toCharArray()) {
            if (c == '{') openCurly++;
            if (c == '}') closeCurly++;
            if (c == '[') openSquare++;
            if (c == ']') closeSquare++;
        }
        StringBuilder sb = new StringBuilder(jsonLike);
        for (int i = 0; i < Math.max(0, openSquare - closeSquare); i++) sb.append(']');
        for (int i = 0; i < Math.max(0, openCurly - closeCurly); i++) sb.append('}');
        return sb.toString();
    }

    private String buildJsonRepairPrompt(String rawModelText) {
        return String.format("""
                Fix this invalid JSON to match the schema below. Return ONLY valid JSON, nothing else.

                REQUIRED SCHEMA (all keys required, use null if not applicable):
                {
                  "operation": "AGGREGATE|GROUP_BY|TOP_N|DISTRIBUTION|COMPARISON|CLARIFY",
                  "target_column": "string_or_null",
                  "aggregation": "sum|avg|count|min|max|null",
                  "group_by": "string_or_null",
                  "filters": [{"column":"string","operator":"eq|neq|contains|in|gt|gte|lt|lte","value":"string"}],
                  "sort": "asc|desc|null",
                  "limit": 10,
                  "chart_type": "bar|line|pie|table|null",
                  "clarify_question": "string_or_null"
                }

                INVALID JSON TO FIX:
                %s

                Return ONLY the fixed JSON. Start with '{'. End with '}'.
                """, rawModelText);
    }

    private Map<String, Object> executeQueryPlan(Map<String, Object> plan,
                                                  List<Map<String, String>> data,
                                                  String question,
                                                  Set<String> columns,
                                                  Map<String, String> schema) {
        try {
            String operation = plan != null ? (String) plan.get("operation") : null;
            if (operation == null) {
                return createErrorResponse("AI did not return an operation. Please rephrase your question.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("question", question);
            response.put("operation", operation);

            List<String> warnings = new ArrayList<>();
            canonicalizePlan(plan, columns, warnings);

            List<Map<String, String>> filteredData = applyFilters(data, plan, columns, schema);
            response.put("rows_before", data.size());
            response.put("rows_after", filteredData.size());
            response.put("warnings", warnings);
            response.put("filters", plan.getOrDefault("filters", Collections.emptyList()));

            if ("CLARIFY".equalsIgnoreCase(operation)) {
                response.put("needs_clarification", true);
                response.put("summary", plan.getOrDefault("clarify_question", "Please clarify your question."));
                response.put("chart_type", "table");
                response.put("chart_data", Collections.emptyList());
                return response;
            }

            if (filteredData.isEmpty()) {
                response.put("summary", "No rows matched the filters from your question. " +
                        "Try rephrasing or check the filter values.");
                response.put("chart_type", "table");
                response.put("chart_data", Collections.emptyList());
                return response;
            }

            if ("AGGREGATE".equalsIgnoreCase(operation)) {
                String q = question.toLowerCase(Locale.ROOT);
                if (q.contains("which") || q.contains("who")) {
                    String groupByColumn = findBestGroupColumn(new ArrayList<>(columns), q);
                    if (groupByColumn != null) {
                        plan.put("operation", "TOP_N");
                        plan.put("group_by", groupByColumn);
                        String agg = (String) plan.get("aggregation");
                        plan.put("sort", "min".equals(agg) ? "asc" : "desc");
                        plan.put("limit", 1);
                        plan.put("chart_type", "table");
                        operation = "TOP_N";
                        System.out.println("Auto-corrected to TOP_N with group_by=" + groupByColumn);
                    }
                }
            }

            String targetColumn = (String) plan.get("target_column");
            String groupBy = (String) plan.get("group_by");
            String aggregation = (String) plan.get("aggregation");

            if ("AGGREGATE".equalsIgnoreCase(operation)) {
                if (aggregation == null) {
                    return createNeedsClarification(response, "Which aggregation do you want (sum, avg, count, min, max)?");
                }
                if (!"count".equalsIgnoreCase(aggregation) && (targetColumn == null || targetColumn.trim().isEmpty())) {
                    return createNeedsClarification(response, "Which numeric column should be aggregated?");
                }
            }

            if ("GROUP_BY".equalsIgnoreCase(operation) || "TOP_N".equalsIgnoreCase(operation)
                    || "COMPARISON".equalsIgnoreCase(operation)) {
                if (groupBy == null || groupBy.trim().isEmpty()) {
                    return createNeedsClarification(response, "Which column should I group by?");
                }
                if (aggregation != null && !"count".equalsIgnoreCase(aggregation)
                        && (targetColumn == null || targetColumn.trim().isEmpty())) {
                    return createNeedsClarification(response, "Which numeric column should be aggregated?");
                }
            }

            if ("DISTRIBUTION".equalsIgnoreCase(operation)
                    && (targetColumn == null || targetColumn.trim().isEmpty())
                    && (groupBy == null || groupBy.trim().isEmpty())) {
                return createNeedsClarification(response, "Which column's distribution do you want?");
            }

            switch (operation.toUpperCase(Locale.ROOT)) {
                case "AGGREGATE": return executeAggregate(plan, filteredData, response);
                case "GROUP_BY": return executeGroupBy(plan, filteredData, response);
                case "TOP_N": return executeTopN(plan, filteredData, response);
                case "COMPARISON": return executeGroupBy(plan, filteredData, response);
                case "DISTRIBUTION": return executeDistribution(plan, filteredData, response);
                default: return createErrorResponse("Unsupported operation: " + operation);
            }

        } catch (Exception e) {
            System.err.println("Execution error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Execution failed: " + e.getMessage());
        }
    }

    private void canonicalizePlan(Map<String, Object> plan, Set<String> columns, List<String> warnings) {
        if (plan == null) return;

        plan.putIfAbsent("filters", new ArrayList<>());
        plan.putIfAbsent("limit", 10);
        plan.putIfAbsent("sort", null);
        plan.putIfAbsent("chart_type", null);
        plan.putIfAbsent("clarify_question", null);
        plan.putIfAbsent("aggregation", null);
        plan.putIfAbsent("target_column", null);
        plan.putIfAbsent("group_by", null);

        if (plan.get("filters") == null) plan.put("filters", new ArrayList<>());

        plan.put("target_column", resolveColumnName((String) plan.get("target_column"), columns));
        plan.put("group_by", resolveColumnName((String) plan.get("group_by"), columns));

        Object sortObj = plan.get("sort");
        if (sortObj instanceof String) {
            String s = ((String) sortObj).trim().toLowerCase(Locale.ROOT);
            if (!s.equals("asc") && !s.equals("desc")) plan.put("sort", null);
        }

        Object filtersObj = plan.get("filters");
        if (filtersObj instanceof List) {
            for (Object f : (List<?>) filtersObj) {
                if (f instanceof Map) {
                    Map fm = (Map) f;
                    Object c = fm.get("column");
                    if (c instanceof String) {
                        String resolved = resolveColumnName((String) c, columns);
                        if (resolved == null) {
                            warnings.add("Unknown filter column: " + c);
                        } else {
                            fm.put("column", resolved);
                        }
                    }
                }
            }
        } else {
            plan.put("filters", new ArrayList<>());
        }

        String op = (String) plan.get("operation");
        if (plan.get("chart_type") == null && op != null) {
            switch (op.toUpperCase(Locale.ROOT)) {
                case "GROUP_BY": case "TOP_N": case "COMPARISON": plan.put("chart_type", "bar"); break;
                case "DISTRIBUTION": plan.put("chart_type", "pie"); break;
                case "AGGREGATE": plan.put("chart_type", "table"); break;
            }
        }

        Object ct = plan.get("chart_type");
        if (ct instanceof String) {
            String s = ((String) ct).trim().toLowerCase(Locale.ROOT);
            if (s.contains("bar")) plan.put("chart_type", "bar");
            else if (s.contains("line")) plan.put("chart_type", "line");
            else if (s.contains("pie")) plan.put("chart_type", "pie");
            else if (s.contains("table")) plan.put("chart_type", "table");
        }

        if ("AGGREGATE".equalsIgnoreCase(op)) plan.put("chart_type", "table");
    }

    private String resolveColumnName(String requested, Set<String> columns) {
        if (requested == null) return null;
        if (columns.contains(requested)) return requested;
        String norm = normalizeCol(requested);
        for (String c : columns) {
            if (c.equalsIgnoreCase(requested)) return c;
            if (normalizeCol(c).equals(norm)) return c;
        }
        for (String c : columns) {
            if (normalizeCol(c).contains(norm) || norm.contains(normalizeCol(c))) return c;
        }
        return null;
    }

    private String normalizeCol(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String findBestGroupColumn(List<String> columns, String question) {
        String[] priorities = {"name", "student", "product", "employee", "id", "category", "item"};
        for (String priority : priorities) {
            for (String col : columns) {
                if (col.toLowerCase(Locale.ROOT).contains(priority)) return col;
            }
        }
        return columns.isEmpty() ? null : columns.get(0);
    }

    private List<Map<String, String>> applyFilters(List<Map<String, String>> data,
                                                    Map<String, Object> plan,
                                                    Set<String> columns,
                                                    Map<String, String> schema) {
        Object filtersObj = plan.get("filters");
        if (!(filtersObj instanceof List)) return data;
        List<Map<String, Object>> filters = (List<Map<String, Object>>) filtersObj;
        if (filters.isEmpty()) return data;

        return data.stream().filter(row -> {
            for (Map<String, Object> f : filters) {
                if (f == null) continue;
                String col = (String) f.get("column");
                String op = (String) f.get("operator");
                Object value = f.get("value");
                if (col == null || op == null || !columns.contains(col)) continue;
                String cell = row.get(col);
                if (!matchesFilter(cell, schema.getOrDefault(col, "text"), op, value)) return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private boolean matchesFilter(String cell, String type, String operator, Object value) {
        String op = operator == null ? "" : operator.trim().toLowerCase(Locale.ROOT);
        if (cell == null) cell = "";

        if ("numeric".equalsIgnoreCase(type) &&
                (op.equals("gt") || op.equals("gte") || op.equals("lt") || op.equals("lte"))) {
            double cellNum = parseDouble(cell);
            double v = value instanceof Number
                    ? ((Number) value).doubleValue()
                    : parseDouble(String.valueOf(value));
            switch (op) {
                case "gt": return cellNum > v;
                case "gte": return cellNum >= v;
                case "lt": return cellNum < v;
                case "lte": return cellNum <= v;
                default: return true;
            }
        }

        String cellNorm = cell.trim().toLowerCase(Locale.ROOT);
        String valStr = value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);

        switch (op) {
            case "eq": return cellNorm.equals(valStr);
            case "neq": return !cellNorm.equals(valStr);
            case "contains": return cellNorm.contains(valStr);
            case "in":
                if (value instanceof List) {
                    return ((List<?>) value).stream()
                            .anyMatch(v -> cellNorm.equals(String.valueOf(v).trim().toLowerCase(Locale.ROOT)));
                }
                return Arrays.stream(valStr.split(","))
                        .anyMatch(v -> cellNorm.equals(v.trim()));
            default: return true;
        }
    }

    private Map<String, Object> executeAggregate(Map<String, Object> plan,
                                                  List<Map<String, String>> data,
                                                  Map<String, Object> response) {
        String column = (String) plan.get("target_column");
        String aggregation = (String) plan.get("aggregation");

        if ("count".equalsIgnoreCase(aggregation) && (column == null || column.trim().isEmpty())) {
            double result = data.size();
            response.put("result", result);
            response.put("summary", "Count: " + (long) result);
            response.put("chart_type", "table");
            response.put("chart_data", List.of(Map.of("label", "count", "value", result)));
            return response;
        }

        Object result = calculateAggregation(data, column, aggregation);
        String displayVal = result instanceof Double
                ? String.format("%.2f", (Double) result)
                : String.valueOf(result);

        response.put("result", result);
        response.put("summary", String.format("The %s of %s is: %s", aggregation, column, displayVal));
        response.put("chart_type", "table");
        response.put("chart_data", List.of(Map.of("label", aggregation + " of " + column, "value", result)));
        System.out.println("AGGREGATE executed: " + result);
        return response;
    }


    private Map<String, Object> executeGroupBy(Map<String, Object> plan,
                                                List<Map<String, String>> data,
                                                Map<String, Object> response) {
        String groupByColumn = (String) plan.get("group_by");
        String targetColumn = (String) plan.get("target_column");
        String aggregation = (String) plan.getOrDefault("aggregation", "count");
        if (aggregation == null) aggregation = "count";

        final String agg = aggregation;
        Map<String, List<Map<String, String>>> grouped = data.stream()
                .collect(Collectors.groupingBy(row -> row.getOrDefault(groupByColumn, "N/A")));

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : grouped.entrySet()) {
            String label = entry.getKey() != null ? entry.getKey() : "N/A";
            Object value = targetColumn != null
                    ? calculateAggregation(entry.getValue(), targetColumn, agg)
                    : (double) entry.getValue().size();
            
            Map<String, Object> row = new HashMap<>();
            
            if (!entry.getValue().isEmpty()) {
                Map<String, String> originalRow = entry.getValue().get(0);
                for (Map.Entry<String, String> col : originalRow.entrySet()) {
                    row.put(col.getKey(), col.getValue());
                }
            }
            
            row.put("label", label);
            row.put("value", value);
            row.put(groupByColumn, label);
            
            chartData.add(row);
        }

        sortAndLimit(chartData, plan);

        response.put("chart_data", chartData);
        response.put("chart_type", plan.getOrDefault("chart_type", "bar"));
        if (!chartData.isEmpty()) {
            response.put("summary", String.format("'%s' leads with %.2f",
                    chartData.get(0).get("label"),
                    ((Number) chartData.get(0).get("value")).doubleValue()));
        }
        System.out.println("GROUP_BY executed: " + chartData.size() + " groups");
        return response;
    }

    private Map<String, Object> executeTopN(Map<String, Object> plan,
                                            List<Map<String, String>> data,
                                            Map<String, Object> response) {
        String groupByColumn = (String) plan.get("group_by");
        String targetColumn = (String) plan.get("target_column");
        String aggregation = (String) plan.getOrDefault("aggregation", "count");
        if (aggregation == null) aggregation = "count";

        final String agg = aggregation;
        Map<String, List<Map<String, String>>> grouped = data.stream()
                .collect(Collectors.groupingBy(row -> row.getOrDefault(groupByColumn, "N/A")));

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : grouped.entrySet()) {
            String label = entry.getKey() != null ? entry.getKey() : "N/A";
            Object value = targetColumn != null
                    ? calculateAggregation(entry.getValue(), targetColumn, agg)
                    : (double) entry.getValue().size();
            
            Map<String, Object> row = new HashMap<>();
            
            if (!entry.getValue().isEmpty()) {
                Map<String, String> originalRow = entry.getValue().get(0);
                for (Map.Entry<String, String> col : originalRow.entrySet()) {
                    row.put(col.getKey(), col.getValue());
                }
            }
            
            row.put("label", label);
            row.put("value", value);
            row.put(groupByColumn, label);
            
            chartData.add(row);
        }

        sortAndLimit(chartData, plan);

        response.put("chart_data", chartData);
        response.put("chart_type", plan.getOrDefault("chart_type", "table"));
        if (!chartData.isEmpty()) {
            Map<String, Object> top = chartData.get(0);
            response.put("summary", String.format("Result: %s → %.2f",
                    top.get("label"),
                    ((Number) top.get("value")).doubleValue()));
        }
        System.out.println("TOP_N executed: " + chartData.size() + " items");
        return response;
    }

    private Map<String, Object> executeDistribution(Map<String, Object> plan,
                                                     List<Map<String, String>> data,
                                                     Map<String, Object> response) {
        String column = (String) plan.get("target_column");
        if (column == null || column.trim().isEmpty()) {
            column = (String) plan.get("group_by");
        }
        if (column == null) throw new RuntimeException("Missing target_column for distribution");

        final String col = column;
        Map<String, Long> distribution = data.stream()
                .collect(Collectors.groupingBy(
                        row -> row.getOrDefault(col, "N/A"),
                        Collectors.counting()));

        List<Map<String, Object>> chartData = distribution.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("label", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Long.compare(
                        ((Number) b.get("value")).longValue(),
                        ((Number) a.get("value")).longValue()))
                .collect(Collectors.toList());

        response.put("chart_data", chartData);
        response.put("chart_type", "pie");
        response.put("summary", String.format("Distribution of %s across %d categories",
                col, distribution.size()));
        System.out.println("DISTRIBUTION executed: " + chartData.size() + " categories");
        return response;
    }

    private void sortAndLimit(List<Map<String, Object>> chartData, Map<String, Object> plan) {
        String sort = (String) plan.get("sort");
        final String dir = (sort == null || sort.trim().isEmpty()) ? "desc" : sort.trim().toLowerCase(Locale.ROOT);
        chartData.sort((a, b) -> {
            double v1 = ((Number) a.get("value")).doubleValue();
            double v2 = ((Number) b.get("value")).doubleValue();
            return dir.equals("asc") ? Double.compare(v1, v2) : Double.compare(v2, v1);
        });
        Object limitObj = plan.get("limit");
        int limit = limitObj instanceof Number ? ((Number) limitObj).intValue() : 10;
        if (limit > 0 && chartData.size() > limit) {
            chartData.subList(limit, chartData.size()).clear();
        }
    }

    private Object calculateAggregation(List<Map<String, String>> data, String column, String agg) {
        if (column == null || agg == null) return 0.0;
        switch (agg.toLowerCase(Locale.ROOT)) {
            case "sum": return data.stream().mapToDouble(r -> parseDouble(r.get(column))).sum();
            case "avg": return data.stream().mapToDouble(r -> parseDouble(r.get(column))).average().orElse(0);
            case "count": return (double) data.size();
            case "min": return data.stream().mapToDouble(r -> parseDouble(r.get(column))).min().orElse(0);
            case "max": return data.stream().mapToDouble(r -> parseDouble(r.get(column))).max().orElse(0);
            default: return (double) data.size();
        }
    }

    private Map<String, String> detectDataTypes(List<Map<String, String>> data) {
        Map<String, String> types = new HashMap<>();
        if (data.isEmpty()) return types;
        for (String column : data.get(0).keySet()) {
            int numericCount = 0, total = 0;
            for (Map<String, String> row : data.subList(0, Math.min(100, data.size()))) {
                String value = row.get(column);
                if (value != null && !value.trim().isEmpty()) {
                    total++;
                    try { Double.parseDouble(value); numericCount++; }
                    catch (NumberFormatException ignored) {}
                }
            }
            types.put(column, (total > 0 && numericCount > total * 0.8) ? "numeric" : "text");
        }
        return types;
    }

    private double parseDouble(String value) {
        if (value == null) return 0;
        try { return Double.parseDouble(value.trim()); }
        catch (Exception e) { return 0; }
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        return new RestTemplate(factory);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> r = new HashMap<>();
        r.put("error", true);
        r.put("message", message);
        return r;
    }

    private Map<String, Object> createNeedsClarification(Map<String, Object> response, String msg) {
        response.put("needs_clarification", true);
        response.put("summary", msg);
        response.put("chart_type", "table");
        response.put("chart_data", Collections.emptyList());
        return response;
    }
}
