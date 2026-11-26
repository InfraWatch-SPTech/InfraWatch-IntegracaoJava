package school.sptech.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import school.sptech.config.DBConnectionProvider;
import school.sptech.controller.JiraController;
import school.sptech.dao.AlertaDAO;
import school.sptech.model.Alerta;
import school.sptech.model.DadosHardwareModelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AlertaService
{
    private static final DateTimeFormatter FORMATO_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlertaDAO alertaDAO;
    private final JiraController jira;

    private static String montarLog(String nivel, String bloco, String mensagem) {
        return "[" + LocalDateTime.now().format(FORMATO_LOG) + "] [" + nivel + "] [" + bloco + "] " + mensagem;
    }

    private static void info(String bloco, String mensagem) {
        System.out.println(montarLog("INFO", bloco, mensagem));
    }

    private static void warn(String bloco, String mensagem) {
        System.out.println(montarLog("WARN", bloco, mensagem));
    }

    private static void error(String bloco, String mensagem) {
        System.err.println(montarLog("ERRO", bloco, mensagem));
    }

    private static void section(String bloco, String titulo) {
        System.out.println();
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
        System.out.println(montarLog("BLOCO", bloco, titulo));
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
    }

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
            error("ALERTA", "Código ou ID do equipamento veio nulo/vazio do S3.");
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
            error("ALERTA", "Erro SQL ao buscar equipamento '" + codigoEquipamento + "' no MySQL: " + e.getMessage());
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
            error("ALERTA", "Objeto DadosHardwareModelo está nulo.");
            return;
        }

        section("ALERTA", "Validando leituras do equipamento " + dados.getEquipamento());

        Integer idEquipamento = buscarIdEquipamento(dados.getEquipamento());

        if (idEquipamento == null)
        {
            error("ALERTA", "Processamento interrompido: equipamento não encontrado no banco MySQL.");
            return;
        }

        info("ALERTA", "Equipamento encontrado no MySQL com ID: " + idEquipamento);

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
            warn("ALERTA", "Nenhum parâmetro de alerta ativo cadastrado para o equipamento ID: " + idEquipamento);
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
                warn("ALERTA", "Métrica não encontrada no JSON para o componente: " + nomeComp);
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
                tipoAlerta = "ATENCAO";
                limiteUsado = limiteAtencao;

                /*System.out.println("Disparando Alerta de Atenção no Jira para " + nomeComponente);
                String response = jira.createIssue("KAN", mensagemAtencao, "Task");
                System.out.println("Issue criada no Jira: " + response);*/

            }

            else
            {
                info("ALERTA", "Componente " + nomeComponente + " (Média: " + valorMedio + "%) dentro dos limites.");
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

            section("ALERTA", "Criando chamado no Jira para o componente " + nomeComponente);

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

            // Guarda o arquivo do S3 que gerou o alerta para que ele não seja processado de novo
            alerta.setArquivoLido(dados.getArquivoOrigem());

            alertaDAO.salvar(alerta);

            info("ALERTA", "Issue criada no Jira: " + response);
        }

        catch (IOException | InterruptedException e)
        {
            error("ALERTA", "Erro ao comunicar com a API do Jira: " + e.getMessage());
            e.printStackTrace();
        }

        catch (Exception e)
        {
            error("ALERTA", "Erro ao processar alerta para " + nomeComponente + ": " + e.getMessage());
        }
    }

    /**
     * Compara o status dos chamados no banco com o status no Jira e atualiza o banco quando estiver desatualizado.
     */
    public void sincronizarStatusChamados()
    {
        section("SINCRONIZACAO", "Iniciando a sincronização dos chamados com o Jira");

        // Busca no banco apenas os alertas que ainda não estão resolvidos
        List<Alerta> alertasEmAberto = alertaDAO.buscarAlertasNaoResolvidos();

        if (alertasEmAberto.isEmpty())
        {
            info("SINCRONIZACAO", "Nenhum chamado pendente para sincronizar.");
            return;
        }

        // Cada alerta é consultado individualmente no Jira usando a jira_key salva no banco.
        for (Alerta alerta : alertasEmAberto)
        {
            // O try fica dentro do for para que um chamado com erro não impeça a verificação dos outros
            // e não derrube a tarefa agendada.
            try
            {
                String response = jira.consultarChamado(alerta.getJiraKey());

                JsonNode fields = new ObjectMapper().readTree(response).path("fields");

                // Usa a categoria do status (new, indeterminate, done) e não o nome, porque o nome muda
                // conforme o idioma e a configuração do quadro no Jira.
                String categoria = fields.path("status").path("statusCategory").path("key").asText();
                String statusJira = converterStatusJira(categoria);

                // Se o status do banco já é igual ao do Jira, a única exceção é o caso do chamado
                // já estar concluído sem ter a data preenchida; aí a própria task completa a pendência.
                if (statusJira.equals(alerta.getStatusAlerta()))
                {
                    if (statusJira.equals("Concluído") && (alerta.getResolvidoEm() == null || alerta.getResolvidoEm().trim().isEmpty()))
                    {
                        String resolvidoEm = java.time.LocalDateTime.now().toString().substring(0, 19).replace("T", " ");
                        alertaDAO.atualizarStatusResolvido(alerta.getJiraKey(), statusJira, resolvidoEm);
                        info("SINCRONIZACAO", "Chamado " + alerta.getJiraKey() + " já estava concluído, então a data de resolução foi preenchida agora.");
                    }

                    info("SINCRONIZACAO", "Chamado " + alerta.getJiraKey() + " já está atualizado (" + statusJira + ").");
                    continue;
                }

                if (statusJira.equalsIgnoreCase("CONCLUÍDO"))
                {
                    // Quando a task identifica a transição para resolvido, a própria aplicação registra
                    // o instante da confirmação, evitando depender de campos opcionais do Jira.
                    String resolvidoEm = java.time.LocalDateTime.now().toString().substring(0, 19).replace("T", " ");

                    alertaDAO.atualizarStatusResolvido(alerta.getJiraKey(), statusJira, resolvidoEm);
                }

                else
                {
                    alertaDAO.atualizarStatus(alerta.getJiraKey(), statusJira);
                }

                info("SINCRONIZACAO", "Chamado " + alerta.getJiraKey() + " atualizado de " + alerta.getStatusAlerta() + " para " + statusJira);
            }

            catch (IOException | InterruptedException e)
            {
                error("SINCRONIZACAO", "Erro ao comunicar com a API do Jira para o chamado " + alerta.getJiraKey() + ": " + e.getMessage());
            }

            catch (Exception e)
            {
                error("SINCRONIZACAO", "Erro ao sincronizar o chamado " + alerta.getJiraKey() + ": " + e.getMessage());
            }
        }
    }

    /**
             * Converte a categoria de status do Jira para o status usado na tabela alerta
     */
    private String converterStatusJira(String categoriaJira)
    {
        if (categoriaJira.equals("done"))
        {
            return "Concluído";
        }

        else if (categoriaJira.equals("indeterminate"))
        {
            return "Em andamento";
        }

        return "A fazer";
    }
}
