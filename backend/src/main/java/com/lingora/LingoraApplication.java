package com.lingora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động backend Lingora Study.
 * Cung cấp REST API dịch ({@code /api/translate}) và tóm tắt ({@code /api/summarize})
 * cho frontend (index.html). Xem README.md để biết cách chạy + biến môi trường.
 */
@SpringBootApplication
public class LingoraApplication {
    public static void main(String[] args) {
        SpringApplication.run(LingoraApplication.class, args);
    }
}
