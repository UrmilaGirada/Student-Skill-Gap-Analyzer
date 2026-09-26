package com.skillgap.analyzer.resume;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skillgap.analyzer.exception.MissingResumeFileException;
import com.skillgap.analyzer.exception.NoExtractableTextException;
import com.skillgap.analyzer.exception.ResumeFileTooLargeException;
import com.skillgap.analyzer.exception.UnsupportedResumeFileTypeException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extraction tests with the real Apache Tika parsers.
 *
 * <p>No mocks, no Spring context and no database: the service under test has no dependencies other
 * than Tika, so it can be exercised on its own.</p>
 */
class ResumeTextExtractionServiceTest {

    private static final String PDF = "application/pdf";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final ResumeTextExtractionService service = new ResumeTextExtractionService();

    @Test
    void extractsTextFromPdf() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "resume.pdf", PDF,
                ResumeTestDocuments.pdfWithText(ResumeTestDocuments.SAMPLE_TEXT));

        String extractedText = service.extractText(file);

        assertThat(extractedText).contains("TEST CANDIDATE");
        assertThat(extractedText).contains("Java, Spring Boot, MySQL");
    }

    @Test
    void extractsTextFromDocx() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "resume.docx", DOCX,
                ResumeTestDocuments.docxWithText(ResumeTestDocuments.SAMPLE_TEXT));

        String extractedText = service.extractText(file);

        assertThat(extractedText).contains("TEST CANDIDATE");
        assertThat(extractedText).contains("B.Tech Information Technology");
    }

    @Test
    void rejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "empty.pdf", PDF, new byte[0]);

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(MissingResumeFileException.class)
                .hasMessage("Resume file is required");
    }

    @Test
    void rejectsUnsupportedContentType() {
        MultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "plain text".getBytes(UTF_8));

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(UnsupportedResumeFileTypeException.class)
                .hasMessage("Unsupported resume file type");
    }

    @Test
    void rejectsFileWhoseBytesDoNotMatchTheDeclaredType() {
        // declared as a PDF but the bytes are plain text (a renamed or forged file)
        MultipartFile file = new MockMultipartFile("file", "fake.pdf", PDF, "not a pdf at all".getBytes(UTF_8));

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(UnsupportedResumeFileTypeException.class)
                .hasMessage("Unsupported resume file type");
    }

    @Test
    void rejectsFileLargerThanTheLimit() {
        byte[] tooBig = new byte[(int) ResumeTextExtractionService.MAX_FILE_SIZE_BYTES + 1];
        MultipartFile file = new MockMultipartFile("file", "big.pdf", PDF, tooBig);

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(ResumeFileTooLargeException.class)
                .hasMessage("Resume file exceeds the 10 MB limit");
    }

    @Test
    void rejectsDocumentWithoutExtractableText() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "scanned.pdf", PDF, ResumeTestDocuments.blankPdf());

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(NoExtractableTextException.class)
                .hasMessage("No extractable text found in resume");
    }

    @Test
    void normalizesLineEndingsAndRepeatedBlankLines() {
        assertThat(ResumeTextExtractionService.normalizeText("  A\r\n\r\n\r\n\r\nB  ")).isEqualTo("A\n\nB");
        assertThat(ResumeTextExtractionService.normalizeText(null)).isEmpty();
    }
}
