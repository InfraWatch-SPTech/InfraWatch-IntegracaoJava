package school.sptech.dto;

// DTO pequeno para serializar o projeto do Jira na estrutura esperada pela API.
public class ProjectDto {

    private String key;

    public ProjectDto() {
    }

    public ProjectDto(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
