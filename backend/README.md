# Lingora Backend — dịch & tóm tắt bài giảng

Backend **Spring Boot 3 (Java 21)** cho [Lingora Study](https://hhieuann.github.io/lingora/).
Tách phần **dịch** và **tóm tắt** ra server: giữ API key an toàn, dùng **LLM thật** cho tóm tắt,
né giới hạn của MyMemory phía client. Frontend (`index.html`) chỉ gọi REST.

## Công nghệ

- Spring Boot 3.4, **Java 21**, Maven.
- `spring-boot-starter-web` + `spring-boot-starter-validation`, dùng `RestClient` gọi provider ngoài.
- **Dịch:** MyMemory (free, không cần key).
- **Tóm tắt:** Anthropic Claude API (`claude-sonnet-4-6` mặc định) → trả JSON có cấu trúc.
  Nếu **không có API key** → tự động fallback **extractive** (tần suất từ + cue) để vẫn chạy được.

## Cấu trúc

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/lingora/
    │   ├── LingoraApplication.java
    │   ├── config/CorsConfig.java
    │   ├── controller/TranslateController.java   SummarizeController.java
    │   ├── dto/TranslateRequest/Response.java     SummarizeRequest/Response.java
    │   └── service/TranslationService.java         SummaryService.java
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

Test nhanh (có sẵn `test-summarize.json`):
```bash
curl -X POST http://localhost:8080/api/translate -H "Content-Type: application/json" \
  -d '{"text":"A function is a reusable block of code.","source":"en","target":"vi"}'

curl -X POST http://localhost:8080/api/summarize -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @test-summarize.json
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
