package school.sptech.model;

public class Alerta
{
    private Integer idAlerta;

    private Integer fkEquipamento;
    private Integer fkComponente;
    private Integer fkEmpresa;

    private String jiraKey;

    // vindo dos joins
    private String empresa;
    private String equipamento;
    private String componente;

    private String tipoAlerta;
    private Double valorMedido;
    private Double limiteViolado;
    private String statusAlerta;

    private String criadoEm;
    private String resolvidoEm;

    private Integer slaResolucao; // entidade empresa

    public Alerta() {
    }

    public Alerta(Integer idAlerta, Integer fkEquipamento, Integer fkComponente, Integer fkEmpresa, String jiraKey, String empresa, String equipamento, String componente, String tipoAlerta, Double valorMedido, Double limiteViolado, String statusAlerta, String criadoEm, String resolvidoEm, Integer slaResolucao) {
        this.idAlerta = idAlerta;
        this.fkEquipamento = fkEquipamento;
        this.fkComponente = fkComponente;
        this.fkEmpresa = fkEmpresa;
        this.jiraKey = jiraKey;
        this.empresa = empresa;
        this.equipamento = equipamento;
        this.componente = componente;
        this.tipoAlerta = tipoAlerta;
        this.valorMedido = valorMedido;
        this.limiteViolado = limiteViolado;
        this.statusAlerta = statusAlerta;
        this.criadoEm = criadoEm;
        this.resolvidoEm = resolvidoEm;
        this.slaResolucao = slaResolucao;
    }

    public Integer getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(Integer idAlerta) {
        this.idAlerta = idAlerta;
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

    public Integer getFkEmpresa() {
        return fkEmpresa;
    }

    public void setFkEmpresa(Integer fkEmpresa) {
        this.fkEmpresa = fkEmpresa;
    }

    public String getJiraKey() {
        return jiraKey;
    }

    public void setJiraKey(String jiraKey) {
        this.jiraKey = jiraKey;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getEquipamento() {
        return equipamento;
    }

    public void setEquipamento(String equipamento) {
        this.equipamento = equipamento;
    }

    public String getComponente() {
        return componente;
    }

    public void setComponente(String componente) {
        this.componente = componente;
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

    public String getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(String criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getResolvidoEm() {
        return resolvidoEm;
    }

    public void setResolvidoEm(String resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
    }

    public Integer getSlaResolucao() {
        return slaResolucao;
    }

    public void setSlaResolucao(Integer slaResolucao) {
        this.slaResolucao = slaResolucao;
    }

    @Override
    public String toString() {
        return "Alerta{" +
                "idAlerta=" + idAlerta +
                ", fkEquipamento=" + fkEquipamento +
                ", fkComponente=" + fkComponente +
                ", fkEmpresa=" + fkEmpresa +
                ", jiraKey='" + jiraKey + '\'' +
                ", empresa='" + empresa + '\'' +
                ", equipamento='" + equipamento + '\'' +
                ", componente='" + componente + '\'' +
                ", tipoAlerta='" + tipoAlerta + '\'' +
                ", valorMedido=" + valorMedido +
                ", limiteViolado=" + limiteViolado +
                ", statusAlerta='" + statusAlerta + '\'' +
                ", criadoEm='" + criadoEm + '\'' +
                ", resolvidoEm='" + resolvidoEm + '\'' +
                ", slaResolucao=" + slaResolucao +
                '}';
    }
}
