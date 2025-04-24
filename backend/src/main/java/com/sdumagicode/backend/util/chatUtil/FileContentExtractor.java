package com.sdumagicode.backend.util.chatUtil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 统一文件内容提取工具类
 * 支持PDF、DOC、DOCX格式文件的内容提取
 *
 * 使用方法：String content = FileContentExtractor.extractContent(".pdf", inputStream);
 */
public class FileContentExtractor {
    // 文件后缀常量
    public static final String SUFFIX_PDF = ".pdf";
    public static final String SUFFIX_DOC = ".doc";
    public static final String SUFFIX_DOCX = ".docx";

    public static final String SUFFIX_TXT = ".txt";

    /**
     * 根据文件后缀提取文件内容
     * @param fileSuffix 文件后缀(.pdf/.doc/.docx)
     * @param inputStream 文件输入流
     * @return 文件文本内容
     */
    public static String extractContent(String fileSuffix, InputStream inputStream) {
        if (fileSuffix == null || inputStream == null) {
            return null;
        }

        switch (fileSuffix.toLowerCase()) {
            case SUFFIX_PDF:
                return extractPdfContent(inputStream);
            case SUFFIX_DOC:
                return extractDocContent(inputStream);
            case SUFFIX_DOCX:
                return extractDocxContent(inputStream);
            case SUFFIX_TXT:  // 新增TXT文件处理分支
                return extractTxtContent(inputStream);
            default:
                return null;
        }
    }

    /**
     * 提取PDF文件内容
     * @param inputStream PDF文件输入流
     * @return PDF文本内容
     */
    private static String extractPdfContent(InputStream inputStream) {
        // 禁用字体缓存
        System.setProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", "true");
        // 重要：禁用字体加载
        System.setProperty("org.apache.pdfbox.font.load", "false");

        PDDocument document = null;
        StringBuilder content = new StringBuilder();

        try {
            document = PDDocument.load(inputStream);
            int pageSize = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int i = 0; i < pageSize; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                content.append(stripper.getText(document).trim()).append("\n");
            }
            return content.toString();
        } catch (InvalidPasswordException e) {
            // 处理加密PDF情况
            return null;
        } catch (IOException e) {
            // 记录具体异常信息
            e.printStackTrace();
            return null;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // 关闭异常可忽略
                }
            }
        }
    }


    /**
     * 提取DOC文件内容
     * @param inputStream DOC文件输入流
     * @return DOC文本内容
     */
    private static String extractDocContent(InputStream inputStream) {
        try {
            WordExtractor ex = new WordExtractor(inputStream);
            String content = ex.getText();
            ex.close();
            return content;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取DOCX文件内容
     * @param inputStream DOCX文件输入流
     * @return DOCX文本内容
     */
    private static String extractDocxContent(InputStream inputStream) {
        try {
            XWPFDocument xdoc = new XWPFDocument(inputStream);
            XWPFWordExtractor extractor = new XWPFWordExtractor(xdoc);
            String content = extractor.getText();
            extractor.close();
            return content;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取TXT文件内容
     * @param inputStream TXT文件输入流
     * @return TXT文本内容
     */
    private static String extractTxtContent(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString().trim();
        } catch (IOException e) {
            return null;
        }
    }
}
