package school.sptech.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Estrutura mínima do ADF exigido pelo Jira para preencher o campo description via REST API v3.
public class DescriptionDto
{
    // O Jira espera um documento com tipo e versão, mesmo quando a descrição contém apenas um texto.
    private String type = "doc";
    private Integer version = 1;
    private List<Object> content = new ArrayList<>();

    public DescriptionDto() {
    }

    // Constrói um documento ADF com um único parágrafo de texto plano.
    public DescriptionDto(String text)
    {
        this.content.add(Map.of(
                "type", "paragraph",
                "content", List.of(
                        Map.of(
                                "type", "text",
                                "text", text
                        )
                )
        ));
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<Object> getContent() {
        return content;
    }

    public void setContent(List<Object> content) {
        this.content = content;
    }
}
