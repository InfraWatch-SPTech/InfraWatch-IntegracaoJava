package school.sptech.model;

// Modelo enxuto usado para persistir e transportar o alerta já consolidado entre Jira e banco.
public class Alerta
{
    // Chaves estrangeiras que ligam o alerta ao equipamento e ao componente que o originaram.
    private Integer fkEquipamento;
    private Integer fkComponente;

    // Identificador retornado pelo Jira para permitir rastreamento entre os dois sistemas.
    private String jiraKey;

    // Classificação do alerta e números usados para auditoria e exibição no histórico.
    private String tipoAlerta;
    private Double valorMedido;
    private Double limiteViolado;
    private String statusAlerta;
    private String resolvidoEm;

    public Alerta() {
    }

    // Construtor completo para cenários em que o alerta já chega totalmente preenchido.
    public Alerta(Integer fkEquipamento, Integer fkComponente, String jiraKey, String tipoAlerta, Double valorMedido, Double limiteViolado, String statusAlerta, String resolvidoEm) {
        this.fkEquipamento = fkEquipamento;
        this.fkComponente = fkComponente;
        this.jiraKey = jiraKey;
        this.tipoAlerta = tipoAlerta;
        this.valorMedido = valorMedido;
        this.limiteViolado = limiteViolado;
        this.statusAlerta = statusAlerta;
        this.resolvidoEm = resolvidoEm;
    }

    public Integer getFkEquipamento() {
        return fkEquipamento;
    }

    public void setFkEquipamento(Integer fkEquipamento) {
        this.fkEquipamento = fkEquipamento;
    }

    public Integer getFkComponente() {
        return fkComponente;
    }

    public void setFkComponente(Integer fkComponente) {
        this.fkComponente = fkComponente;
    }

    public String getJiraKey() {
        return jiraKey;
    }

    public void setJiraKey(String jiraKey) {
        this.jiraKey = jiraKey;
    }

    public String getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(String tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }

    public Double getValorMedido() {
        return valorMedido;
    }

    public void setValorMedido(Double valorMedido) {
        this.valorMedido = valorMedido;
    }

    public Double getLimiteViolado() {
        return limiteViolado;
    }

    public void setLimiteViolado(Double limiteViolado) {
        this.limiteViolado = limiteViolado;
    }

    public String getStatusAlerta() {
        return statusAlerta;
    }

    public void setStatusAlerta(String statusAlerta) {
        this.statusAlerta = statusAlerta;
    }

    public String getResolvidoEm() {
        return resolvidoEm;
    }

    public void setResolvidoEm(String resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
    }

    @Override
    public String toString() {
        return "Alerta{" +
                "fkEquipamento=" + fkEquipamento +
                ", fkComponente=" + fkComponente +
                ", jiraKey='" + jiraKey + '\'' +
                ", tipoAlerta='" + tipoAlerta + '\'' +
                ", valorMedido=" + valorMedido +
                ", limiteViolado=" + limiteViolado +
                ", statusAlerta='" + statusAlerta + '\'' +
                ", resolvidoEm='" + resolvidoEm + '\'' +
                '}';
    }
}
