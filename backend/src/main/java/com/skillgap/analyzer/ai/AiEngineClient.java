package com.skillgap.analyzer.ai;

import java.time.Duration;
import java.util.List;

import com.skillgap.analyzer.ai.dto.SkillExtractionRequest;
import com.skillgap.analyzer.ai.dto.SkillExtractionResponse;
import com.skillgap.analyzer.exception.AiEngineBadResponseException;
import com.skillgap.analyzer.exception.AiEngineUnavailableException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP client for the Python AI engine ({@code POST /api/v1/skills/extract}).
 *
 * <p>This is the only class that talks to the Python process. It translates low-level HTTP
 * failures into the project's controlled exceptions so no raw stack trace or Python error ever
 * reaches the API client:</p>
 *
 * <ul>
 *   <li>engine unreachable / timeout -&gt; {@link AiEngineUnavailableException} (HTTP 503)</li>
 *   <li>engine answered with an error or invalid payload -&gt; {@link AiEngineBadResponseException} (HTTP 502)</li>
 * </ul>
 */
@Service
public class AiEngineClient {

    private final RestClient restClient;

    @Autowired
    public AiEngineClient(
            @Value("${ai.engine.base-url}") String baseUrl,
            @Value("${ai.engine.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${ai.engine.read-timeout-ms}") int readTimeoutMs) {
        this(buildRestClient(baseUrl, connectTimeoutMs, readTimeoutMs));
    }

    /** Allows tests to bind Spring's {@code MockRestServiceServer} to the underlying client. */
    AiEngineClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sends the stored resume text to the Python engine and returns its canonical skills.
     *
     * @throws AiEngineUnavailableException if the engine cannot be reached in time
     * @throws AiEngineBadResponseException if the engine answers with an error or unexpected data
     */
    public List<String> extractSkills(String text) {
        try {
            SkillExtractionResponse response = restClient.post()
                    .uri("/api/v1/skills/extract")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SkillExtractionRequest(text))
                    .retrieve()
                    .body(SkillExtractionResponse.class);

            if (response == null || response.skills() == null) {
                throw new AiEngineBadResponseException(
                        "AI skill extraction service returned an invalid response");
            }
            return response.skills();
        } catch (ResourceAccessException exception) {
            // connection refused / read timeout: the Python process is not reachable
            throw new AiEngineUnavailableException("AI skill extraction service is unavailable", exception);
        } catch (RestClientResponseException exception) {
            // the engine answered, but with a 4xx/5xx status - never forwarded to the client
            throw new AiEngineBadResponseException(
                    "AI skill extraction service returned an error response", exception);
        } catch (RestClientException | HttpMessageNotReadableException exception) {
            // malformed or unexpected payload
            throw new AiEngineBadResponseException(
                    "AI skill extraction service returned an invalid response", exception);
        }
    }

    private static RestClient buildRestClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
