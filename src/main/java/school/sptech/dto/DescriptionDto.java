package school.sptech.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DescriptionDto
{
    private String type = "doc";
    private Integer version = 1;
    private List<Object> content = new ArrayList<>();

    public DescriptionDto() {
    }

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
