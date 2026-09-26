package com.skillgap.analyzer.resume;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Builds tiny PDF/DOCX documents in memory for tests.
 *
 * <p>No binary fixtures are committed to the repository and no personal data is used - the content is
 * obviously fake placeholder text. Uses the PDFBox/POI libraries that ship with Apache Tika.</p>
 */
final class ResumeTestDocuments {

    /** Deliberately fake, non-personal placeholder text. */
    static final String SAMPLE_TEXT = "TEST CANDIDATE\nB.Tech Information Technology\nJava, Spring Boot, MySQL";

    private ResumeTestDocuments() {
    }

    /** A small PDF whose page content contains {@code text}. */
    static byte[] pdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(60, 720);
                for (String line : text.split("\n", -1)) {
                    content.showText(line);
                    content.newLineAtOffset(0, -18);
                }
                content.endText();
            }

            return toBytes(document);
        }
    }

    /** A PDF with one page but no text at all - stands in for a scanned/image-only document. */
    static byte[] blankPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            return toBytes(document);
        }
    }

    /** Bytes that start like a PDF but are otherwise unusable - a corrupted document. */
    static byte[] corruptedPdf() {
        return "%PDF-1.4\nthis is not a real pdf body".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** A small DOCX whose paragraphs contain {@code text}. */
    static byte[] docxWithText(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            for (String line : text.split("\n", -1)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    private static byte[] toBytes(PDDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        return out.toByteArray();
    }
}
