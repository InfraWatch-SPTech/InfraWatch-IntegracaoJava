package school.sptech.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import school.sptech.config.DBConnectionProvider;
import school.sptech.controller.JiraController;
import school.sptech.dao.AlertaDAO;
import school.sptech.model.Alerta;
import school.sptech.model.DadosHardwareModelo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AlertaService
{
    private final AlertaDAO alertaDAO;
    private final JiraController jira;

    // O serviço concentra a orquestração do fluxo: identifica o equipamento, lê os parâmetros
    // ativos, compara as métricas e decide se um chamado precisa ser gerado.
    public AlertaService(JiraController jira)
    {
        this.alertaDAO = new AlertaDAO();
        this.jira = jira;
    }

    /**
     * Busca o ID do equipamento no MySQL usando o ID direto ou o nome do equipamento.
     */
    public Integer buscarIdEquipamento(String codigoEquipamento)
    {
        if (codigoEquipamento == null || codigoEquipamento.trim().isEmpty())
        {
            System.err.println("Código ou ID do equipamento veio nulo/vazio do S3.");
            return null;
        }

        try
        {
            // Primeiro tenta interpretar o código como identificador numérico, porque o JSON pode
            // carregar apenas o número do equipamento em vez do nome completo.
            // 1. Tenta buscar pelo ID numérico diretamente (ex: "1" -> 1)
            try
            {
                Integer id = Integer.parseInt(codigoEquipamento.trim());
                //String sql = "SELECT idEquipamento FROM equipamento WHERE idEquipamento = ?;";
                //return template.queryForObject(sql, Integer.class, id);
                return alertaDAO.buscarIdEquipamentoPorId(id);
            }

            catch (NumberFormatException e)
            {
                // Se não for número, o fluxo cai para a busca pelo nome, cobrindo a forma textual
                // que também pode chegar no arquivo do S3.
                // 2. Se não for número, busca pelo campo 'nome' na tabela 'equipamento'
                //String sql = "SELECT idEquipamento FROM equipamento WHERE nome = ?;";
                //return template.queryForObject(sql, Integer.class, codigoEquipamento.trim());
                return alertaDAO.buscarIdEquipamentoPorNome(codigoEquipamento.trim());
            }
        }

        catch (Exception e)
        {
            System.err.println("Erro SQL ao buscar equipamento '" + codigoEquipamento + "' no MySQL: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifica os componentes configurados e ativos para o equipamento no banco.
     */
    public void verificarEEnviarAlertas(DadosHardwareModelo dados)
    {
        if (dados == null)
        {
            System.err.println("Objeto DadosHardwareModelo está nulo.");
            return;
        }

        Integer idEquipamento = buscarIdEquipamento(dados.getEquipamento());

        if (idEquipamento == null)
        {
            System.err.println("Processamento interrompido: equipamento não encontrado no banco MySQL.");
            return;
        }

        System.out.println("Equipamento encontrado no MySQL com ID: " + idEquipamento);

        // Busca no banco todos os componentes e limites ativos para este equipamento
        /*String sql = """
            SELECT p.limite_atencao, p.limite_critico, c.nome AS nome_componente, c.tipo AS tipo_componente
            FROM parametro_alerta p
            JOIN componente c ON p.fkComponente = c.idComponente
            WHERE p.fkEquipamento = ? AND p.ativo = 1;
        """;*/

        //List<Map<String, Object>> componentesMonitorados = template.queryForList(sql, idEquipamento);
        List<Map<String, Object>> componentesMonitorados = alertaDAO.buscarParametrosAtivos(idEquipamento);

        if (componentesMonitorados.isEmpty()) {
            System.out.println("Nenhum parâmetro de alerta ativo cadastrado para o equipamento ID: " + idEquipamento);
            return;
        }

        // Cada linha retornada representa um componente monitorado com seus limites ativos.
        for (Map<String, Object> comp : componentesMonitorados)
        {
            String nomeComp = (String) comp.get("nome_componente");
            String tipoComp = (comp.get("tipo_componente") != null) ? comp.get("tipo_componente").toString().toUpperCase() : "";
            Double limiteAtencao = ((Number) comp.get("limite_atencao")).doubleValue();
            Double limiteCritico = ((Number) comp.get("limite_critico")).doubleValue();
            Integer fkComponente = ((Number) comp.get("fkComponente")).intValue();

            // A métrica correta é escolhida a partir do tipo/nome do componente, pois o JSON pode
            // variar a nomenclatura entre "Processador", "CPU", "Memoria RAM" e outros sinônimos.
            Double valorMedio = extrairValorMetrica(dados, tipoComp, nomeComp);

            if (valorMedio != null)
            {
                avaliarEEnviarAlerta(dados, idEquipamento, fkComponente, nomeComp, valorMedio, limiteAtencao, limiteCritico);
            }

            else
            {
                System.out.println("Métrica não encontrada no JSON para o componente: " + nomeComp);
            }
        }
    }

    /**
     * Mapeia o tipo/nome do componente vindo do banco para a métrica do JSON
     */
    private Double extrairValorMetrica(DadosHardwareModelo dados, String tipoComp, String nomeComp)
    {
        // O identificador é normalizado em maiúsculas para simplificar a comparação com os nomes
        // cadastrados no banco e reduzir a sensibilidade a variações de escrita.
        String identificador = (tipoComp + " " + nomeComp).toUpperCase();

        if (identificador.contains("CPU") || identificador.contains("PROCESSADOR"))
        {
            return dados.getCpuMedia();
        }

        else if (identificador.contains("RAM") || identificador.contains("MEMORIA"))
        {
            return dados.getRamMedia();
        }

        else if (identificador.contains("DISCO") || identificador.contains("ARMAZENAMENTO"))
        {
            return dados.getDiscoMedia();
        }

        else if (identificador.contains("TEMPERATURA"))
        {
            return dados.getTemperaturaMedia();
        }

        else if (identificador.contains("REDE"))
        {
            return dados.getRedeMedia();
        }

        return null;
    }

    /**
     * Compara o valor lido com os limites e dispara a issue no Jira
     */
    private void avaliarEEnviarAlerta(DadosHardwareModelo dados, Integer fkEquipamento, Integer fkComponente, String nomeComponente, Double valorMedio, Double limiteAtencao, Double limiteCritico)
    {
        String tipoAlerta;
        Double limiteUsado;

        try
        {
            /*String mensagemCritica = String.format("ALERTA CRÍTICO: O componente %s alcançou %.2f%%!", nomeComponente, valorMedio);
            String mensagemAtencao = String.format("ALERTA DE ATENÇÃO: O componente %s alcançou %.2f%%!", nomeComponente, valorMedio);*/

            // A severidade crítica é verificada primeiro para garantir prioridade quando o valor
            // ultrapassa mais de um limite ao mesmo tempo.
            if (valorMedio >= limiteCritico)
            {
                tipoAlerta = "CRITICO";
                limiteUsado = limiteCritico;

                /*System.out.println("Disparando Alerta Crítico no Jira para " + nomeComponente);
                String response = jira.createIssue("KAN", mensagemCritica, "Task");
                System.out.println("Issue criada no Jira: " + response);*/

            }

            else if (valorMedio >= limiteAtencao)
            {
                tipoAlerta = "ALERTA";
                limiteUsado = limiteAtencao;

                /*System.out.println("Disparando Alerta de Atenção no Jira para " + nomeComponente);
                String response = jira.createIssue("KAN", mensagemAtencao, "Task");
                System.out.println("Issue criada no Jira: " + response);*/

            }

            else
            {
                System.out.println("Componente " + nomeComponente + " (Média: " + valorMedio + "%) dentro dos limites.");
                return; // impedir envio quando nao tiver alerta
            }

            // O nome legível do equipamento vem do banco para que a issue seja compreensível sem expor
            // o identificador bruto do JSON.
            String equipamentoNome = alertaDAO.buscarEquipamento(dados.getEquipamento());

            // O summary é o título curto da issue; a descrição carrega o contexto completo para leitura
            // humana e para facilitar a rastreabilidade do incidente.
            String summary = String.format("[%s] %s em %.1f%% - %s",
                    tipoAlerta,
                    nomeComponente,
                    valorMedio,
                    equipamentoNome);

            String descriptionText = String.format(
                    "Empresa: %s | Equipamento: %s | Componente: %s | Tipo: %s | Valor medido: %.1f | Limite definido: %.1f",
                    dados.getEmpresa(),
                    equipamentoNome,
                    nomeComponente,
                    tipoAlerta,
                    valorMedio,
                    limiteUsado
            );

            System.out.println("Disparando alerta no Jira para " + nomeComponente);

            String response = jira.createIssue("KAN", summary, descriptionText, "Task");

            // A resposta da API do Jira devolve a key da issue criada, que é o vínculo persistido no
            // banco para manter a correlação entre chamado e monitoramento.
            JsonNode node = new ObjectMapper().readTree(response);
            String jiraKey = node.get("key").asText();

            // O objeto persistido no banco guarda apenas o necessário para reconstruir o histórico do
            // alerta e relacioná-lo ao Jira e ao equipamento/componente de origem.
            Alerta alerta = new Alerta();

            alerta.setJiraKey(jiraKey);
            alerta.setFkEquipamento(fkEquipamento);
            alerta.setFkComponente(fkComponente);
            alerta.setTipoAlerta(tipoAlerta);
            alerta.setValorMedido(valorMedio);
            alerta.setLimiteViolado(limiteUsado);

            alertaDAO.salvar(alerta);

            System.out.println("Issue criada no Jira: " + response);
        }

        catch (IOException | InterruptedException e)
        {
            System.err.println("Erro ao comunicar com a API do Jira: " + e.getMessage());
            e.printStackTrace();
        }

        catch (Exception e)
        {
            System.err.println("Erro ao processar alerta para " + nomeComponente + ": " + e.getMessage());
        }
    }
}
