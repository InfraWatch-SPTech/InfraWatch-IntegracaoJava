package school.sptech;

import io.github.cdimascio.dotenv.Dotenv;
import school.sptech.config.S3Provider;
import school.sptech.controller.JiraController;
import school.sptech.dao.AlertaDAO;
import school.sptech.service.AlertaService;
import school.sptech.service.MonitorarArquivosS3Task;
import school.sptech.service.S3Service;
import school.sptech.service.SincronizacaoChamadosTask;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main
{
    private static final DateTimeFormatter FORMATO_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String montarLog(String nivel, String bloco, String mensagem) {
        return "[" + LocalDateTime.now().format(FORMATO_LOG) + "] [" + nivel + "] [" + bloco + "] " + mensagem;
    }

    private static void section(String bloco, String titulo) {
        System.out.println();
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
        System.out.println(montarLog("BLOCO", bloco, titulo));
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
    }

    private static void info(String bloco, String mensagem) {
        System.out.println(montarLog("INFO", bloco, mensagem));
    }

    public static void main(String[] args) {
        section("BOOT", "Inicializando InfraWatch");

        // As configurações externas precisam estar disponíveis antes de criar os clientes e as
        // tarefas, porque o restante do fluxo depende das credenciais e do endereço do Jira.
        Dotenv dotenv = Dotenv.load();

        // O bucket, a empresa e os ids dos equipamentos seguem a organização já usada no projeto:
        // o S3 guarda os JSONs separados por id de equipamento dentro da pasta da empresa.
        String bucketName = "gold-api-java";
        String empresa = "NovaTech Solucoes";

        S3Client s3Client = new S3Provider().criarClienteS3();
        S3Service readerService = new S3Service(s3Client);
        JiraController jira = new JiraController(dotenv.get("JIRA_BASE_URL"), dotenv.get("JIRA_EMAIL"), dotenv.get("JIRA_API_TOKEN"));
        AlertaService alertaService = new AlertaService(jira);
        AlertaDAO alertaDAO = new AlertaDAO();

        // Um pool com duas threads permite executar a varredura do S3 e a sincronização de status
        // ao mesmo tempo, sem misturar as rotinas em uma única fila de execução.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        section("BOOT", "Agendando a varredura do S3");

        // Task que percorre a empresa inteira, lê o último JSON de cada equipamento e dispara os
        // alertas quando existirem métricas acima do limite.
        MonitorarArquivosS3Task tarefaS3 = new MonitorarArquivosS3Task(alertaDAO, readerService, alertaService, bucketName, empresa);

        section("BOOT", "Agendando a sincronização com o Jira");

        // Task que verifica periodicamente se os chamados criados no Jira tiveram mudança de status
        // e atualiza o banco para manter o histórico sincronizado.
        SincronizacaoChamadosTask tarefaSincronizacao = new SincronizacaoChamadosTask(alertaService);

        // A primeira execução acontece imediatamente para não depender de um novo arquivo aparecer
        // antes de iniciar o processamento.
        scheduler.scheduleAtFixedRate(tarefaS3, 0, 20, TimeUnit.SECONDS);

        // Em paralelo à leitura do S3, a aplicação consulta o Jira para manter os status dos
        // chamados atualizados no banco.
        scheduler.scheduleAtFixedRate(tarefaSincronizacao, 0, 20, TimeUnit.SECONDS);

        // Garante encerramento ordenado do agendador quando a aplicação for finalizada manualmente.
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));

        info("BOOT", "Aplicação em execução com tarefas agendadas ativas.");

        // Agenda a sincronização dos chamados: a cada 20 segundos a tarefa consulta o Jira e atualiza
        // no banco o status dos chamados que ficaram desatualizados.
        // O 0 é o tempo de espera antes da primeira execução, e o 20 é o intervalo em segundos.
    }
}
