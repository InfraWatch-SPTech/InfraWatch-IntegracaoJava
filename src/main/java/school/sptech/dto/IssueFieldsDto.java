package school.sptech.dto;

import jdk.jfr.Description;

// DTO que modela os campos reais enviados para o Jira na criação da issue.
public class IssueFieldsDto {

    private ProjectDto project;
    private String summary;
    private DescriptionDto description;
    private IssueTypeDto issuetype;

    public IssueFieldsDto() {
    }

    public IssueFieldsDto(ProjectDto project, String summary, DescriptionDto description, IssueTypeDto issuetype) {
        this.project = project;
        this.summary = summary;
        this.description = description;
        this.issuetype = issuetype;
    }

    public ProjectDto getProject() {
        return project;
    }

    public void setProject(ProjectDto project) {
        this.project = project;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public DescriptionDto getDescription() {
        return description;
    }

    public void setDescription(DescriptionDto description) {
        this.description = description;
    }

    public IssueTypeDto getIssuetype() {
        return issuetype;
    }

    public void setIssuetype(IssueTypeDto issuetype) {
        this.issuetype = issuetype;
    }
}
