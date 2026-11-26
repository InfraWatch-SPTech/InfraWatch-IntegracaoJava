package school.sptech.dto;

// DTO que informa ao Jira qual tipo de issue deve ser criada no projeto.
public class IssueTypeDto {

    private String name;

    public IssueTypeDto() {
    }

    public IssueTypeDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
