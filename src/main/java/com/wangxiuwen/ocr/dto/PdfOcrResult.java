package com.wangxiuwen.ocr.dto;

import com.benjaminwan.ocrlibrary.OcrResult;
import lombok.Data;

import java.util.List;

@Data
public class PdfOcrResult {
    private int totalPages;
    private List<PageResult> pages;

    @Data
    public static class PageResult {
        private int pageNumber;
        private OcrResult ocrResult;
    }
}