package com.skillgap.analyzer.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.skillgap.analyzer.exception.AiEngineBadResponseException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the HTTP boundary of {@link AiEngineClient} using Spring's
 * {@link MockRestServiceServer} - no Spring context, no database and no live Python process.
 */
class AiEngineClientTest {

    private static final String ENGINE_URL = "http://engine.test/api/v1/skills/extract";

    private MockRestServiceServer server;

    private AiEngineClient client() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new AiEngineClient(builder.baseUrl("http://engine.test").build());
    }

    @Test
    void returnsSkillsWhenEngineRespondsSuccessfully() {
        AiEngineClient client = client();
        server.expect(requestTo(ENGINE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"skills\":[\"Java\",\"Python\",\"Spring Boot\",\"AWS\"]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.extractSkills("resume text"))
                .containsExactly("Java", "Python", "Spring Boot", "AWS");

        server.verify();
    }

    @Test
    void mapsEngineHttpErrorToControlledException() {
        AiEngineClient client = client();
        server.expect(requestTo(ENGINE_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.extractSkills("resume text"))
                .isInstanceOf(AiEngineBadResponseException.class)
                .hasMessage("AI skill extraction service returned an error response");
    }

    @Test
    void mapsUnexpectedPayloadToControlledException() {
        AiEngineClient client = client();
        server.expect(requestTo(ENGINE_URL))
                .andRespond(withSuccess("{\"unexpected\":true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.extractSkills("resume text"))
                .isInstanceOf(AiEngineBadResponseException.class)
                .hasMessage("AI skill extraction service returned an invalid response");
    }
}
