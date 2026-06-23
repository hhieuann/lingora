# Lingora Backend — dịch & tóm tắt bài giảng

Backend **Spring Boot 3 (Java 21)** cho [Lingora Study](https://hhieuann.github.io/lingora/).
Tách phần **dịch** và **tóm tắt** ra server: giữ API key an toàn, dùng **LLM thật** cho tóm tắt,
né giới hạn của MyMemory phía client. Frontend (`index.html`) chỉ gọi REST.

## Chức năng

- **Dịch** (`/api/translate`) — MyMemory (free, không cần key).
- **Tóm tắt** (`/api/summarize`) — Claude API trả JSON; không key → fallback extractive.
- **Tạo quiz ôn tập** (`/api/quiz`) — Claude sinh câu trắc nghiệm; không key → fallback từ từ vựng.
- **Lưu / mở lại buổi học** (`/api/sessions`) — CRUD, lưu vào H2 (giữ giữa các lần chạy).
- **Tiện ích** (`/api/health`, `/api/config`) — kiểm tra backend & xem LLM có bật không.

## Công nghệ

- Spring Boot 3.4, **Java 21**, Maven.
- `spring-boot-starter-web` + `-validation` + `-data-jpa`, **H2** (file DB), `RestClient` gọi provider ngoài.
- **LLM:** Anthropic Claude API (`claude-sonnet-4-6` mặc định) qua `AnthropicClient` dùng chung.
  Nếu **không có API key** → tự động fallback để vẫn chạy được.

## Cấu trúc

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/lingora/
    │   ├── LingoraApplication.java
    │   ├── config/CorsConfig.java
    │   ├── controller/  Translate · Summarize · Quiz · Session · System Controller
    │   ├── dto/         Translate · Summarize · Quiz · SaveSession · SessionListItem · StudySessionResponse
    │   ├── model/StudySession.java        (entity JPA)
    │   ├── repo/StudySessionRepository.java
    │   └── service/  Translation · Summary · Quiz · Session Service · AnthropicClient
    └── resources/application.properties
```

## Chạy

```bash
cd backend
mvn spring-boot:run           # mặc định http://localhost:8080
```

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `ANTHROPIC_API_KEY` | *(trống)* | Key Claude API. Trống → dùng extractive fallback. **KHÔNG hardcode.** |
| `LINGORA_MODEL` | `claude-sonnet-4-6` | Model tóm tắt (đổi `claude-opus-4-8` nếu muốn mạnh hơn). |

```bash
# Windows PowerShell
$env:ANTHROPIC_API_KEY = "sk-ant-..."
mvn spring-boot:run

# bash
ANTHROPIC_API_KEY=sk-ant-... mvn spring-boot:run
```

## API

### POST `/api/translate`
```jsonc
// request
{ "text": "A function is a reusable block of code.", "source": "en", "target": "vi" }
// response
{ "translatedText": "Hàm là một khối lệnh có thể tái sử dụng." }
```

### POST `/api/summarize`
```jsonc
// request
{ "targetLang": "vi",
  "segments": [ { "speaker": "Teacher", "src": "Welcome back...", "tgt": "Chào mừng..." } ] }
// response (khớp renderSummary ở frontend)
{ "points":  ["Điểm chính 1", "Điểm chính 2"],
  "vocab":   [ { "term": "function", "lang": "en-US", "mean": "hàm" } ],
  "homework":[ { "who": "", "text": "Viết 3 hàm và kiểm thử." } ] }
```

### POST `/api/quiz`
```jsonc
// request  ({"count" tùy chọn, mặc định 5})
{ "targetLang": "vi", "count": 3,
  "segments": [ { "speaker": "Teacher", "src": "A function is...", "tgt": "Hàm là..." } ] }
// response
{ "questions": [
    { "question": "...", "options": ["A","B","C","D"], "answerIndex": 2, "explanation": "..." } ] }
```

### `/api/sessions` — lưu/mở lại buổi học (H2)
```jsonc
POST   /api/sessions      // body: {title, srcLang, tgtLang, durationSeconds, segments[], summary} → 201 {id,...}
GET    /api/sessions      // danh sách (metadata, mới nhất trước)
GET    /api/sessions/{id} // đầy đủ (kèm segments + summary)  | 404 nếu không có
DELETE /api/sessions/{id} // 204 | 404
```

### Tiện ích
```jsonc
GET /api/health  // {"status":"ok"}
GET /api/config  // {"llmEnabled":true|false,"model":"claude-sonnet-4-6","version":"0.1.0"}
```

Test nhanh (có sẵn `test-summarize.json`, `test-session.json`, `test-quiz.json`):
```bash
curl -X POST http://localhost:8080/api/translate -H "Content-Type: application/json" \
  -d '{"text":"A function is a reusable block of code.","source":"en","target":"vi"}'

curl -X POST http://localhost:8080/api/summarize -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @test-summarize.json

curl -X POST http://localhost:8080/api/quiz -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @test-quiz.json

curl -X POST http://localhost:8080/api/sessions -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @test-session.json
curl http://localhost:8080/api/sessions
```

## CORS

`CorsConfig` cho phép origin trong `lingora.cors.allowed-origins` (application.properties):
`https://hhieuann.github.io`, `http://localhost:5500`, `http://127.0.0.1:5500`.

## Nối với frontend

Trong `index.html`, đặt hằng số đầu `<script>`:
```js
const API_BASE = "http://localhost:8080";        // khi test local
// const API_BASE = "https://<backend-da-deploy>"; // khi deploy
```
- `API_BASE` rỗng → frontend dùng MyMemory + extractive như cũ (live site không vỡ).
- `API_BASE` có URL → `translate()` và `buildSummary()` gọi backend; tự fallback nếu backend lỗi.

## Deploy (free)

Render / Railway / Fly.io. Build jar: `mvn clean package` → `target/lingora-backend-0.1.0.jar`,
chạy `java -jar ...`. Đặt `ANTHROPIC_API_KEY` ở dashboard. Có URL rồi → cập nhật `API_BASE` trong
`index.html` → push lại GitHub Pages.
