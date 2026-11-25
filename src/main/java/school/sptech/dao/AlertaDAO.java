package school.sptech.dao;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import school.sptech.model.Alerta;
import school.sptech.config.DBConnectionProvider;

import java.util.List;

// Executa SQL e recupera os alertas
public class AlertaDAO
{
    private JdbcTemplate connection;

    public AlertaDAO()
    {
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        this.connection = dbConnectionProvider.getConnection();
    }

    public List<Alerta> buscarTodos()
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
    }

    public String buscarEquipamento(String id)
    {
        String sql = "SELECT nome FROM equipamento WHERE idEquipamento = ?";

        return connection.queryForObject(sql, String.class, id);
    }

}
