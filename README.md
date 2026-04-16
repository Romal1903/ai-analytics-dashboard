# AI Analytics Dashboard

A full-stack AI-powered analytics dashboard — upload a CSV, ask questions in plain English, and get instant charts and insights.

**Stack:** Spring Boot · React · Tailwind CSS · Groq (Llama 3.3 70B) · Recharts

---

## Features

- **CSV Upload** — Drag-and-drop or browse; auto-detects column types
- **Natural Language Queries** — Ask anything about your data in plain English
- **AI Query Planning** — Groq Llama 3.3 70B converts questions into structured query plans
- **Charts** — Bar, Line, Pie, and Table views via Recharts
- **AI Insights** — Auto-generated bullet-point insights on upload
- **Export** — Download results as Excel (`.xlsx`) or CSV
- **Operations** — Aggregations, GROUP BY, TOP N, Distribution, Filters

---

## Project Structure

```
ai-analytics-dashboard/
├── backend/
│   └── src/main/java/com/example/ai_analytics_dashboard/
│       ├── config/
│       │   └── CorsConfig.java
│       ├── controller/
│       │   ├── ExportController.java
│       │   ├── FileController.java
│       │   └── QueryController.java
│       ├── service/
│       │   ├── AIService.java
│       │   ├── CsvService.java
│       │   ├── ExportService.java
│       │   └── SummaryService.java
│       └── Application.java
├── frontend/
│   └── src/
│       ├── components/
│       │   ├── Chat.jsx
│       │   ├── ChartDisplay.jsx
│       │   ├── Dashboard.jsx
│       │   ├── DataSummary.jsx
│       │   └── Upload.jsx
│       ├── utils/
│       │   └── api.js
│       ├── App.jsx
│       └── index.jsx
└── README.md
```

---

## Prerequisites

- Java 17+
- Node.js 16+
- Maven 3.6+
- [Groq API Key](https://console.groq.com/)

---

## Setup

### Backend

```bash
cd backend
```

Set your Groq API key in `src/main/resources/application.properties`:

```properties
groq.api.key=YOUR_GROQ_API_KEY_HERE
groq.model.name=llama-3.3-70b-versatile
```

```bash
mvn clean install
mvn spring-boot:run
```

Runs at `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm start
```

Runs at `http://localhost:3000`

---

## Sample Data & Test Questions

### `sample_students.csv`

**Try these questions:**
- What is the average marks?
- What is the highest marks?
- How many students scored above 90?
- Show average marks by subject
- Count students per grade
- Which student has the highest marks?
- Who got grade A in Math?
- Average marks for Science students
- Students with grade B
- Marks distribution by subject
- Which student has highest marks in Math subject?
- Average marks for grade A students in Science

---

### `sample_employees.csv`

**Try these questions:**
- What is the average salary?
- Total salary budget?
- Average salary by department
- Employee count per department
- Who has the highest salary?
- Top 3 departments by avg salary
- Most experienced employee
- Salary > 70000 in Engineering
- HR department average salary
- Department distribution

---

### `sample_sales.csv`

**Try these questions:**
- Total sales across all regions?
- Average quantity per transaction?
- Highest single sale amount?
- Total sales by region
- Sales by product
- Quantity sold per region
- Best selling product?
- Top 3 products by quantity
- Laptop sales in North region
- Phone quantity in East
- Sales > 40000 for any product
- Product mix breakdown
- Sales on 2024-01-15
- Show total sales of Laptop in North and South regions
- Top 3 products by sales where quantity > 20

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/upload` | Upload CSV file |
| GET | `/api/data/info` | Dataset info |
| GET | `/api/data/summary` | Data summary |
| POST | `/api/query` | Natural language query |
| POST | `/api/export/excel` | Export as Excel |
| POST | `/api/export/csv` | Export as CSV |

---

## How It Works

1. **Upload CSV** → Parsed by Apache Commons CSV, stored in memory, AI generates insights
2. **Ask a question** → Sent to Groq Llama 3.3 70B, which returns a structured JSON query plan
3. **Backend executes** → Applies filters, aggregations, grouping on the in-memory data
4. **Frontend renders** → Displays the correct chart type with export options

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Java 17 |
| AI | Groq API — Llama 3.3 70B Versatile |
| CSV Parsing | Apache Commons CSV |
| Excel Export | Apache POI |
| Frontend | React 18, Tailwind CSS |
| Charts | Recharts |
| HTTP Client | Axios |

---

## License

MIT
