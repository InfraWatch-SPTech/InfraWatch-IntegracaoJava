package school.sptech.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)

// Representa o JSON lido do S3, com campos principais do equipamento e métricas agregadas.
public class DadosHardwareModelo
{
    // Identificam a origem do arquivo e permitem relacionar o conteúdo ao equipamento monitorado.
    private String empresa;
    private String equipamento;

    // Key do arquivo do S3 de onde esses dados foram lidos. Não vem no JSON, é preenchida pelo S3Service
    private String arquivoOrigem;

    @JsonProperty("cpu_media")
    @JsonAlias({"Processador_media", "CPU_media", "cpu_media", "processador_media", "Uso_do_processador_media"})
    private Double cpuMedia;

    @JsonProperty("ram_media")
    @JsonAlias({"Memoria RAM_media", "Memoria_RAM_media", "RAM_media", "ram_media", "memoria_media", "Uso_da_memoria_principal_media"})
    private Double ramMedia;

    @JsonProperty("disco_media")
    @JsonAlias({"Disco_media", "disco_media", "ARMAZENAMENTO_media", "armazenamento_media", "Uso_do_armazenamento_media"})
    private Double discoMedia;

    @JsonProperty("temperatura_media")
    @JsonAlias({"Temperatura_media", "temperatura_media", "Temperatura_interna_do_equipamento_media"})
    private Double temperaturaMedia;

    @JsonProperty("rede_media")
    @JsonAlias({"Rede_media", "rede_media", "Uso_da_rede_do_equipamento_media"})
    private Double redeMedia;

    // Captura métricas extras sem quebrar o parse quando o JSON trouxer chaves adicionais.
    private Map<String, Object> metricasExtras = new HashMap<>();

    // Faz uma varredura no mapa de métricas extras para recuperar valores que não vieram mapeados
    // de forma explícita, mantendo o modelo tolerante a variações do arquivo do S3.
    private Double buscarValorNoMapa(String... palavrasChave) {
        for (Map.Entry<String, Object> entry : metricasExtras.entrySet()) {
            String key = entry.getKey().toLowerCase();
            for (String palavra : palavrasChave) {
                if (key.contains(palavra.toLowerCase())) {
                    Object val = entry.getValue();
                    if (val instanceof Number) {
                        return ((Number) val).doubleValue();
                    } else if (val instanceof String) {
                        try {
                            return Double.parseDouble((String) val);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return null;
    }

    @JsonAnySetter
    public void setMetricaExtra(String key, Object value) {
        this.metricasExtras.put(key, value);
    }

    public Map<String, Object> getMetricasExtras() {
        return metricasExtras;
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

    public String getArquivoOrigem() {
        return arquivoOrigem;
    }

    public void setArquivoOrigem(String arquivoOrigem) {
        this.arquivoOrigem = arquivoOrigem;
    }

    public Double getCpuMedia() {
        if (cpuMedia != null) return cpuMedia;
        return buscarValorNoMapa("processador", "cpu");
    }

    public void setCpuMedia(Double cpuMedia) {
        this.cpuMedia = cpuMedia;
    }

    public Double getRamMedia() {
        if (ramMedia != null) return ramMedia;
        return buscarValorNoMapa("memoria", "ram");
    }

    public void setRamMedia(Double ramMedia) {
        this.ramMedia = ramMedia;
    }

    public Double getDiscoMedia() {
        if (discoMedia != null) return discoMedia;
        return buscarValorNoMapa("disco", "armazenamento");
    }

    public void setDiscoMedia(Double discoMedia) {
        this.discoMedia = discoMedia;
    }

    public Double getTemperaturaMedia() {
        if (temperaturaMedia != null) return temperaturaMedia;
        return buscarValorNoMapa("temperatura");
    }

    public void setTemperaturaMedia(Double temperaturaMedia) {
        this.temperaturaMedia = temperaturaMedia;
    }

    public Double getRedeMedia() {
        if (redeMedia != null) return redeMedia;
        return buscarValorNoMapa("rede");
    }

    public void setRedeMedia(Double redeMedia) {
        this.redeMedia = redeMedia;
    }
}
