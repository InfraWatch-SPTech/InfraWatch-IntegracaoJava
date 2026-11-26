package school.sptech.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Tarefa agendada que dispara a sincronização dos chamados com o Jira em intervalos fixos.
public class SincronizacaoChamadosTask implements Runnable
{
    private static final DateTimeFormatter FORMATO_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlertaService alertaService;

    private static String montarLog(String nivel, String bloco, String mensagem) {
        return "[" + LocalDateTime.now().format(FORMATO_LOG) + "] [" + nivel + "] [" + bloco + "] " + mensagem;
    }

    private static void section(String bloco, String titulo) {
        System.out.println();
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
        System.out.println(montarLog("BLOCO", bloco, titulo));
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
    }

    // Recebe o service já pronto porque a tarefa apenas agenda a execução da regra central.
    public SincronizacaoChamadosTask(AlertaService alertaService)
    {
        this.alertaService = alertaService;
    }

    // Método chamado automaticamente pelo agendador a cada intervalo definido no Main.
    @Override
    public void run()
    {
        // A task só dispara a rotina de sincronização; a responsabilidade real continua no service.
        section("SINCRONIZACAO", "Verificando status dos chamados no Jira");
        alertaService.sincronizarStatusChamados();
    }
}
