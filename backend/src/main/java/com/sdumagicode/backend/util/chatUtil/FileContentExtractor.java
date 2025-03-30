package com.sdumagicode.backend.util.chatUtil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;

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
}
