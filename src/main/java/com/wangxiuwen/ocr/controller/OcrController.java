package com.wangxiuwen.ocr.controller;

import com.benjaminwan.ocrlibrary.OcrResult;
import com.wangxiuwen.ocr.dto.PdfOcrResult;
import com.wangxiuwen.ocr.service.OcrService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Api(tags = "OCR识别接口", description = "提供OCR图片文字识别功能")
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @ApiOperation(value = "OCR图片识别", notes = "上传图片文件，返回识别到的文字内容和位置信息")
    @PostMapping("/image")
    public OcrResult ocr(
            @ApiParam(value = "要识别的图片文件，支持jpg、jpeg、png、bmp格式，文件大小不超过10MB", name = "image", required = true, example = "test.jpg") @RequestPart("image") MultipartFile imageFile)
            throws IOException {
        return ocrService.recognizeImage(imageFile);
    }

    @ApiOperation(value = "OCR PDF识别", notes = "上传PDF文件，返回每一页识别到的文字内容和位置信息")
    @PostMapping("/pdf")
    public PdfOcrResult ocrPdf(
            @ApiParam(value = "要识别的PDF文件", name = "pdf", required = true, example = "test.pdf") @RequestPart("pdf") MultipartFile pdfFile)
            throws IOException {
        return ocrService.recognizePdf(pdfFile);
    }
}
