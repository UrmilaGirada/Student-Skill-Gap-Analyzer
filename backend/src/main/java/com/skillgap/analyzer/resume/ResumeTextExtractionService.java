package com.skillgap.analyzer.resume;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import com.skillgap.analyzer.exception.MissingResumeFileException;
import com.skillgap.analyzer.exception.NoExtractableTextException;
import com.skillgap.analyzer.exception.ResumeExtractionException;
import com.skillgap.analyzer.exception.ResumeFileTooLargeException;
import com.skillgap.analyzer.exception.UnsupportedResumeFileTypeException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

/**
 * Turns an uploaded resume document into raw plain text.
 *
 * <p>This service is deliberately free of persistence: it validates the upload, extracts the text
 * with Apache Tika and normalizes it. Saving the result is the job of {@link ResumeService}.</p>
 *
 * <p>Supported documents are PDF and DOCX. The uploaded bytes are processed as a stream and never
 * written to the filesystem, and the client-supplied file name is never used to build a path.</p>
 */
@Service
public class ResumeTextExtractionService {

    /** Maximum accepted upload size in bytes - mirrors {@code spring.servlet.multipart.max-file-size}. */
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(PDF_CONTENT_TYPE, DOCX_CONTENT_TYPE);

    /** PDF files start with "%PDF-", DOCX files are ZIP containers starting with "PK". */
    private static final byte[] PDF_MAGIC_BYTES = { '%', 'P', 'D', 'F', '-' };
    private static final byte[] ZIP_MAGIC_BYTES = { 'P', 'K' };

    /** Tika parsers are thread-safe and expensive to build, so a single instance is shared. */
    private final AutoDetectParser parser = new AutoDetectParser();

    /**
     * Validates the upload, extracts the raw text and normalizes it.
     *
     * @throws MissingResumeFileException         no file was sent, or it is empty (400)
     * @throws ResumeFileTooLargeException        the file exceeds {@link #MAX_FILE_SIZE_BYTES} (413)
     * @throws UnsupportedResumeFileTypeException not a PDF/DOCX, or the bytes do not match the declared type (415)
     * @throws NoExtractableTextException         the document holds no readable text (422)
     * @throws ResumeExtractionException          the document cannot be parsed (422)
     */
    public String extractText(MultipartFile file) {
        validateFileIsPresent(file);
        validateFileSize(file);
        String contentType = validateContentType(file);
        validateContentMatchesContentType(file, contentType);

        String normalizedText = normalizeText(parseToText(file));

        if (normalizedText.isBlank()) {
            throw new NoExtractableTextException("No extractable text found in resume");
        }
        return normalizedText;
    }

    private void validateFileIsPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MissingResumeFileException("Resume file is required");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResumeFileTooLargeException("Resume file exceeds the 10 MB limit");
        }
    }

    private String validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        String normalized = (contentType == null) ? "" : contentType.toLowerCase(Locale.ROOT).trim();

        if (!SUPPORTED_CONTENT_TYPES.contains(normalized)) {
            throw new UnsupportedResumeFileTypeException("Unsupported resume file type");
        }
        return normalized;
    }

    /** The declared content type must also match the real bytes, so renamed images/executables are rejected. */
    private void validateContentMatchesContentType(MultipartFile file, String contentType) {
        byte[] expectedMagicBytes = PDF_CONTENT_TYPE.equals(contentType) ? PDF_MAGIC_BYTES : ZIP_MAGIC_BYTES;

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(expectedMagicBytes.length);
            if (!Arrays.equals(header, expectedMagicBytes)) {
                throw new UnsupportedResumeFileTypeException("Unsupported resume file type");
            }
        } catch (IOException exception) {
            throw new ResumeExtractionException("Could not read the uploaded resume file", exception);
        }
    }

    private String parseToText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            // -1 disables Tika's default 100k character write limit so long resumes stay complete.
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(inputStream, handler, new Metadata(), new ParseContext());
            return handler.toString();
        } catch (IOException | TikaException | SAXException exception) {
            throw new ResumeExtractionException("Could not extract text from the resume file", exception);
        }
    }

    /**
     * Small deterministic clean-up of the extracted text: uniform line endings, no runs of blank
     * lines, and no leading/trailing whitespace. Resume wording itself is left untouched, because
     * Phase 5 consumes this text.
     */
    static String normalizeText(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
