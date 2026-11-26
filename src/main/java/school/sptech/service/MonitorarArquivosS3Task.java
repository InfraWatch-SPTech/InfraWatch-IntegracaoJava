package school.sptech.service;

import school.sptech.dao.AlertaDAO;
import school.sptech.model.DadosHardwareModelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

// Tarefa agendada responsável por varrer os equipamentos da empresa, ler o último JSON de cada
// um no S3 e acionar o fluxo de alertas quando existir dado novo para processar.
public class MonitorarArquivosS3Task implements Runnable
{
    private static final DateTimeFormatter FORMATO_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlertaDAO alertaDAO;
    private final S3Service s3Service;
    private final AlertaService alertaService;
    private final String bucketName;
    private final String empresa;

    private static String montarLog(String nivel, String bloco, String mensagem) {
        return "[" + LocalDateTime.now().format(FORMATO_LOG) + "] [" + nivel + "] [" + bloco + "] " + mensagem;
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

    public MonitorarArquivosS3Task(AlertaDAO alertaDAO, S3Service s3Service, AlertaService alertaService, String bucketName, String empresa)
    {
        this.alertaDAO = alertaDAO;
        this.s3Service = s3Service;
        this.alertaService = alertaService;
        this.bucketName = bucketName;
        this.empresa = empresa;
    }

    @Override
    public void run()
    {
        try
        {
            section("S3-TASK", "Iniciando varredura dos equipamentos da empresa " + empresa);

            // Busca todos os equipamentos pertencentes à empresa para percorrer o S3 por id,
            // seguindo a organização já usada no bucket.
            List<Map<String, Object>> equipamentos = alertaDAO.buscarEquipamentosDaEmpresa(empresa);

            if (equipamentos.isEmpty())
            {
                warn("S3-TASK", "Nenhum equipamento encontrado para a empresa " + empresa + ".");
                return;
            }

            // Cada equipamento é processado separadamente para manter o fluxo simples e permitir
            // que um erro em um id não interrompa os demais.
            for (Map<String, Object> equipamento : equipamentos)
            {
                try
                {
                    Integer idEquipamento = ((Number) equipamento.get("idEquipamento")).intValue();
                    String nomeEquipamento = (equipamento.get("nome") != null) ? equipamento.get("nome").toString() : String.valueOf(idEquipamento);

                    section("S3-TASK", "Processando equipamento " + nomeEquipamento + " (id " + idEquipamento + ")");

                    // O prefixo do S3 segue o id do equipamento, então o mesmo id do banco é usado
                    // para localizar o último JSON daquele servidor.
                    DadosHardwareModelo dados = s3Service.lerUltimoArquivoJson(bucketName, empresa, String.valueOf(idEquipamento));

                    if (dados == null)
                    {
                        warn("S3-TASK", "Sem dados novos para o equipamento " + nomeEquipamento + " (id " + idEquipamento + ").");
                        continue;
                    }

                    // O modelo do JSON já chega com a origem marcada, então o service consegue
                    // criar o alerta e salvar o arquivo lido no banco.
                    alertaService.verificarEEnviarAlertas(dados);
                }

                catch (Exception e)
                {
                    error("S3-TASK", "Erro ao processar equipamento da empresa " + empresa + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        catch (Exception e)
        {
            error("S3-TASK", "Erro geral ao varrer a empresa " + empresa + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
