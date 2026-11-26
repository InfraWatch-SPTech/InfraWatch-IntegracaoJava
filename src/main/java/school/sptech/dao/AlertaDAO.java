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
                limite_violado)
                VALUES (?, ?, ?, ?, ?, ?)
            """;

        connection.update(
                sql,
                alerta.getJiraKey(),
                alerta.getFkEquipamento(),
                alerta.getFkComponente(),
                alerta.getTipoAlerta(),
                alerta.getValorMedido(),
                alerta.getLimiteViolado()
        );

        // criado_em e status entra sozinho porque tem default
    }
}
