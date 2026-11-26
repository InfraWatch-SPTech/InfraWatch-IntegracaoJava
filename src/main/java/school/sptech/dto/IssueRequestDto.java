package school.sptech.dto;

// DTO raiz da requisição do Jira, encapsulando todos os campos que vão dentro de "fields".
public class IssueRequestDto {

    private IssueFieldsDto fields;

    public IssueRequestDto() {
    }

    public IssueRequestDto(IssueFieldsDto fields) {
        this.fields = fields;
    }

    public IssueFieldsDto getFields() {
        return fields;
    }

    public void setFields(IssueFieldsDto fields) {
        this.fields = fields;
    }
}
