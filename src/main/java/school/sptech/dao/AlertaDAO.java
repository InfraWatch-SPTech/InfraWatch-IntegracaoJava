package school.sptech.dao;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import school.sptech.model.Alerta;
import school.sptech.config.DBConnectionProvider;

import java.util.List;
import java.util.Map;

// Responsável por toda a persistência ligada aos alertas e pelas consultas auxiliares usadas no fluxo.
public class AlertaDAO
{
    private JdbcTemplate connection;

    public AlertaDAO()
    {
        // Cada instância do DAO recebe um JdbcTemplate já configurado para concentrar o acesso ao banco.
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        this.connection = dbConnectionProvider.getConnection();
    }

    public String buscarEquipamento(String id)
    {
        // Resolve o nome do equipamento a partir do identificador, garantindo que a mensagem enviada
        // ao Jira use um texto legível em vez do código bruto vindo do JSON.
        String sql = "SELECT nome FROM equipamento WHERE idEquipamento = ?";

        return connection.queryForObject(sql, String.class, id);
    }

    public Integer buscarIdEquipamentoPorId(Integer id)
    {
        // Primeiro tenta validar o código recebido como identificador numérico direto.
        String sql = "SELECT idEquipamento FROM equipamento WHERE idEquipamento = ?";

        return connection.queryForObject(sql, Integer.class, id);
    }

    public Integer buscarIdEquipamentoPorNome(String nome)
    {
        // Se não for número, o fluxo cai para a busca por nome, que cobre a forma textual do equipamento.
        String sql = "SELECT idEquipamento FROM equipamento WHERE nome = ?";

        return connection.queryForObject(sql, Integer.class, nome);
    }

    // Busca no banco todos os componentes e limites ativos para este equipamento
    public List<Map<String, Object>> buscarParametrosAtivos(Integer idEquipamento)
    {
        // Essa consulta devolve tanto o componente quanto os limites configurados, permitindo que o
        // service compare os valores medidos com as regras já cadastradas no banco.
        String sql = """
            SELECT p.fkComponente, p.limite_atencao, p.limite_critico, c.nome AS nome_componente, c.tipo AS tipo_componente
            FROM parametro_alerta p
            JOIN componente c ON p.fkComponente = c.idComponente
            WHERE p.fkEquipamento = ? AND p.ativo = 1;
        """;

        return connection.queryForList(sql, idEquipamento);
    }

    public List<Map<String, Object>> buscarEquipamentosDaEmpresa(String nomeEmpresa)
    {
        // A tarefa do S3 precisa percorrer todos os equipamentos da empresa, então essa consulta
        // devolve os ids e nomes associados ao mesmo dono.
        String sql = """
            SELECT eq.idEquipamento, eq.nome
            FROM equipamento eq
            JOIN empresa emp ON eq.fkEmpresa = emp.idEmpresa
            WHERE emp.nome = ?
            ORDER BY eq.idEquipamento
        """;

        return connection.queryForList(sql, nomeEmpresa);
    }

    public void salvar(Alerta alerta)
    {
        // Persiste apenas os campos que fazem parte da tabela alerta, deixando o restante dos dados
        // para consultas com JOIN quando necessário.
        String sql = """
                INSERT INTO alerta (
                jira_key,
                fkEquipamento,
                fkComponente,
                tipo_alerta,
                valor_medido,
                limite_violado,
                arquivo_lido)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        connection.update(
                sql,
                alerta.getJiraKey(),
                alerta.getFkEquipamento(),
                alerta.getFkComponente(),
                alerta.getTipoAlerta(),
                alerta.getValorMedido(),
                alerta.getLimiteViolado(),
                alerta.getArquivoLido()
        );
        // criado_em e status entra sozinho porque tem default
    }

    public List<Alerta> buscarAlertasNaoResolvidos()
    {
        // A sincronização precisa revisar tanto chamados ainda não concluídos quanto chamados que
        // já estão concluídos, mas ficaram sem a data de resolução preenchida.
        String sql = """
            SELECT jira_key AS jiraKey, status_alerta AS statusAlerta, resolvido_em AS resolvidoEm
            FROM alerta
            WHERE status_alerta <> 'Concluído'
               OR resolvido_em IS NULL
        """;

        return connection.query(sql, new BeanPropertyRowMapper<>(Alerta.class));
    }

    public void atualizarStatus(String jiraKey, String statusAlerta)
    {
        // Usa a jira_key para achar o alerta, já que ela é o vínculo entre o banco e o chamado no Jira.
        String sql = "UPDATE alerta SET status_alerta = ? WHERE jira_key = ?";

        connection.update(sql, statusAlerta, jiraKey);
    }

    public void atualizarStatusResolvido(String jiraKey, String statusAlerta, String resolvidoEm)
    {
        // Quando o chamado muda para resolvido, o próprio sistema registra a data da confirmação
        // feita pela task, sem depender de campos opcionais vindos do Jira.
        String sql = "UPDATE alerta SET status_alerta = ?, resolvido_em = ? WHERE jira_key = ?";

        connection.update(sql, statusAlerta, resolvidoEm, jiraKey);
    }

    public boolean arquivoJaLido(String arquivoLido)
    {
        // Se já existe algum alerta gravado com esse arquivo, ele já foi lido e os chamados dele já
        // foram abertos, então não precisa ser lido de novo.
        String sql = "SELECT COUNT(*) FROM alerta WHERE arquivo_lido = ?";

        Integer quantidade = connection.queryForObject(sql, Integer.class, arquivoLido);

        return quantidade > 0;
    }
}
