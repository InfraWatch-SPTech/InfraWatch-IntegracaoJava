package school.sptech.dao;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import school.sptech.model.Alerta;
import school.sptech.config.DBConnectionProvider;

import java.util.List;
import java.util.Map;

// Executa SQL e recupera os alertas
public class AlertaDAO
{
    private JdbcTemplate connection;

    public AlertaDAO()
    {
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        this.connection = dbConnectionProvider.getConnection();
    }

    /*public List<Alerta> buscarTodos()
    {
        String sql = """
            SELECT
                a.id_alerta AS idAlerta,
                a.jira_key AS jiraKey,
                a.fkEquipamento,
                a.fkComponente,
                eq.fkEmpresa,
                emp.nome AS empresa,
                eq.nome AS equipamento,
                c.nome AS componente,
                a.tipo_alerta AS tipoAlerta,
                a.valor_medido AS valorMedido,
                a.limite_violado AS limiteViolado,
                a.status_alerta AS statusAlerta,
                a.criado_em AS criadoEm,
                a.resolvido_em AS resolvidoEm,
                emp.sla_resolucao AS slaResolucao
            FROM alerta a
            JOIN equipamento eq ON a.fkEquipamento = eq.idEquipamento
            JOIN empresa emp ON eq.fkEmpresa = emp.idEmpresa
            JOIN componente c ON a.fkComponente = c.idComponente
        """;

        List<Alerta> alertas = connection.query(sql, new BeanPropertyRowMapper<>(Alerta.class));

        return alertas;
    }*/

    public String buscarEquipamento(String id)
    {
        String sql = "SELECT nome FROM equipamento WHERE idEquipamento = ?";

        return connection.queryForObject(sql, String.class, id);
    }

    public Integer buscarIdEquipamentoPorId(Integer id)
    {
        String sql = "SELECT idEquipamento FROM equipamento WHERE idEquipamento = ?";

        return connection.queryForObject(sql, Integer.class, id);
    }

    public Integer buscarIdEquipamentoPorNome(String nome)
    {
        String sql = "SELECT idEquipamento FROM equipamento WHERE nome = ?";

        return connection.queryForObject(sql, Integer.class, nome);
    }

    // Busca no banco todos os componentes e limites ativos para este equipamento
    public List<Map<String, Object>> buscarParametrosAtivos(Integer idEquipamento)
    {
        String sql = """
            SELECT p.fkComponente, p.limite_atencao, p.limite_critico, c.nome AS nome_componente, c.tipo AS tipo_componente
            FROM parametro_alerta p
            JOIN componente c ON p.fkComponente = c.idComponente
            WHERE p.fkEquipamento = ? AND p.ativo = 1;
        """;

        return connection.queryForList(sql, idEquipamento);
    }

    public void salvar(Alerta alerta)
    {
        String sql = """
                INSERT INTO alerta (
                jira_key,
                fkEquipamento,
                fkComponente,
                tipo_alerta,
                valor_medido,
                limite_violado,
                status_alerta)
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
                alerta.getStatusAlerta()
        );

        // criado_em entra sozinho porque tem default
    }

}
