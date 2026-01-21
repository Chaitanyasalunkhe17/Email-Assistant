package com.email.writer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String GeminiApiUrl;

    @Value("${gemini.api.key}")
    private String GeminiApikey;

    public EmailGeneratorService() {
        this.webClient = WebClient.create();
    }

    public String generateEmail(EmailRequest emailRequest)
    {
        //Build a prompt
        String prompt = buildprompt(emailRequest);

        //Craft request
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", prompt)
                                }
                        )
                }
        );

        //do request and get response
        String response = webClient.post().uri(GeminiApiUrl + GeminiApikey).header("Content-Type","application/json").bodyValue(requestBody).retrieve().bodyToMono(String.class).block();

        //Extract the response and return
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootnode = mapper.readTree(response);
            return  rootnode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    private String buildprompt(EmailRequest emailRequest) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Genrate aproffesional email reply for the folowing email content . please do not generate subject line");
        if(emailRequest.getTone()!= null && !emailRequest.getTone().isEmpty() )
        {
            prompt.append("Use a").append(emailRequest.getTone()).append(" tone");
        }
        prompt.append("\n Original Email : \n").append(emailRequest.getEmailcontent());
        return prompt.toString();
    }
}
