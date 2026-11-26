package school.sptech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import school.sptech.dto.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class JiraController
{
    private final String baseUrl;
    private final String authHeader;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Centraliza a integração HTTP com o Jira, mantendo autenticação, serialização e envio de
    // payload em um único ponto reutilizável pelo serviço de alerta.
    public JiraController(String baseUrl, String email, String apiToken) {
        this.baseUrl = baseUrl;

        String auth = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public String createIssue(String projectKey, String summary, String descriptionText, String issueType) throws IOException, InterruptedException
    {
        // Monta o corpo da requisição usando os DTOs
        ProjectDto project = new ProjectDto(projectKey);
        IssueTypeDto type = new IssueTypeDto(issueType);
        DescriptionDto description = new DescriptionDto(descriptionText);
        IssueFieldsDto fields = new IssueFieldsDto(project, summary, description, type);
        IssueRequestDto issueRequest = new IssueRequestDto(fields);

        // Transforma o objeto Java em JSON
        String json = objectMapper.writeValueAsString(issueRequest);

        // Cria a requisição POST para o endpoint de criação de issues
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/api/3/issue"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // Envia a requisição
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return response.body();
        }

        throw new RuntimeException("Jira request failed: " + status + " - " + response.body());
    }


    public String consultarChamado(String issueKey) throws IOException, InterruptedException
    {
        // Pede só o campo de status, porque a data de resolução passou a ser registrada pela própria
        // aplicação no momento em que ela detecta a mudança para resolvido.
        String url = baseUrl + "/rest/api/3/issue/" + issueKey + "?fields=status";

        // Cria a requisição GET para consultar a issue que já foi criada
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        // Envia a requisição
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status >= 200 && status < 300)
        {
            return response.body();
        }

        throw new RuntimeException("Jira request failed: " + status + " - " + response.body());
    }
}
