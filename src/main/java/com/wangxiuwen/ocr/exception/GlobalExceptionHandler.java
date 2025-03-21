package com.wangxiuwen.ocr.exception;

import ai.onnxruntime.OrtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrtException.class)
    public ResponseEntity<Map<String, String>> handleOrtException(OrtException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "OCR识别失败：图片格式不符合要求");
        response.put("details", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "处理失败：" + e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}