package com.wangxiuwen.ocr.service;

import com.benjaminwan.ocrlibrary.OcrResult;
import com.wangxiuwen.ocr.dto.PdfOcrResult;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.ocr.config.ParamConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ai.onnxruntime.OrtException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    private static final String[] SUPPORTED_IMAGE_FORMATS = { "jpg", "jpeg", "png", "bmp" };
    private static final String PDF_FORMAT = "pdf";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public OcrResult recognizeImage(MultipartFile imageFile) throws IOException {
        String extension = getFileExtension(imageFile);
        if (PDF_FORMAT.equals(extension)) {
            throw new IllegalArgumentException("请使用PDF专用接口处理PDF文件");
        }
        return processImage(imageFile);
    }

    public PdfOcrResult recognizePdf(MultipartFile pdfFile) throws IOException {
        String extension = getFileExtension(pdfFile);
        if (!PDF_FORMAT.equals(extension)) {
            throw new IllegalArgumentException("不支持的文件格式：" + extension + "，仅支持PDF格式");
        }

        if (pdfFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持50MB");
        }

        File tempPdfFile = null;
        try {
            tempPdfFile = Files.createTempFile("ocr", ".pdf").toFile();
            pdfFile.transferTo(tempPdfFile);
            tempPdfFile.deleteOnExit();

            PDDocument document = PDDocument.load(tempPdfFile);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            PdfOcrResult result = new PdfOcrResult();
            result.setTotalPages(pageCount);
            List<PdfOcrResult.PageResult> pages = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 300);
                File tempImageFile = Files.createTempFile("page_" + (pageIndex + 1), ".png").toFile();
                tempImageFile.deleteOnExit();
                ImageIO.write(image, "PNG", tempImageFile);

                ParamConfig paramConfig = ParamConfig.getDefaultConfig();
                paramConfig.setDoAngle(true);
                paramConfig.setMostAngle(true);

                InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
                OcrResult ocrResult = engine.runOcr(tempImageFile.getPath(), paramConfig);

                PdfOcrResult.PageResult pageResult = new PdfOcrResult.PageResult();
                pageResult.setPageNumber(pageIndex + 1);
                pageResult.setOcrResult(ocrResult);
                pages.add(pageResult);

                tempImageFile.delete();
            }

            result.setPages(pages);
            document.close();
            return result;
        } catch (Throwable e) {
            if (e instanceof OrtException) {
                throw new IllegalArgumentException("PDF页面格式不符合OCR模型要求，请检查PDF文件是否正确", e);
            }
            throw new RuntimeException("PDF OCR识别失败：" + e.getMessage(), e);
        } finally {
            if (tempPdfFile != null && tempPdfFile.exists()) {
                tempPdfFile.delete();
            }
        }
    }

    private OcrResult processImage(MultipartFile imageFile) throws IOException {
        validateImage(imageFile);

        ParamConfig paramConfig = ParamConfig.getDefaultConfig();
        paramConfig.setDoAngle(true);
        paramConfig.setMostAngle(true);

        File tempFile = null;
        try {
            tempFile = Files.createTempFile("ocr", ".png").toFile();
            imageFile.transferTo(tempFile);
            tempFile.deleteOnExit();

            InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);

            OcrResult ocrResult = engine.runOcr(tempFile.getPath(), paramConfig);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            return ocrResult;
        } catch (Throwable e) {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            if (e instanceof OrtException) {
                throw new IllegalArgumentException("图片格式不符合OCR模型要求，请检查图片通道数是否正确", e);
            }
            throw new RuntimeException("OCR识别失败：" + e.getMessage(), e);
        }
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void validateImage(MultipartFile file) throws IOException {
        String extension = getFileExtension(file);
        boolean isSupported = false;
        for (String format : SUPPORTED_IMAGE_FORMATS) {
            if (format.equals(extension)) {
                isSupported = true;
                break;
            }
        }
        if (!isSupported) {
            throw new IllegalArgumentException(
                    "不支持的图片格式：" + extension + "，仅支持：" + String.join(", ", SUPPORTED_IMAGE_FORMATS));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持50MB");
        }

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("无效的图片文件");
        }
    }
}