package school.sptech.model;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

// Fornece o acesso ao banco de dados
public class DBConnectionProvider {

    private final DataSource dataSource;

    public DBConnectionProvider() {

        // Configura os dados de conexão com o MySQL
        BasicDataSource basicDataSource = new BasicDataSource();

        // Define endereço, usuário e senha do banco
        basicDataSource.setUrl("jdbc:mysql://localhost:3306/InfraWatch");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("Longinus19#");

        this.dataSource = basicDataSource;
    }

    // Cria e retorna um JdbcTemplate para executar consultas SQL
    public JdbcTemplate getConnection() {
        return new JdbcTemplate(dataSource);
    }
}
