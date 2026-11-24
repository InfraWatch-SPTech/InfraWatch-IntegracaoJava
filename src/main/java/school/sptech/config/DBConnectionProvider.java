package school.sptech.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import io.github.cdimascio.dotenv.Dotenv;

// Fornece o acesso ao banco de dados
public class DBConnectionProvider {

    private final DataSource dataSource; // guarda a referência ao pool criado

    public DBConnectionProvider() {

        Dotenv dotenv = Dotenv.load();

        // Configura os dados de conexão com o MySQL
        BasicDataSource basicDataSource = new BasicDataSource(); // cria e configura o pool de conexões

        // Define endereço, usuário e senha do banco
        basicDataSource.setUrl(dotenv.get("DB_URL"));
        basicDataSource.setUsername(dotenv.get("DB_USUARIO"));
        basicDataSource.setPassword(dotenv.get("DB_SENHA"));

        this.dataSource = basicDataSource;
    }

    // Cria e retorna um JdbcTemplate para executar consultas SQL
    // utiliza esse DataSource para executar consultas SQL
    public JdbcTemplate getConnection() {
        return new JdbcTemplate(dataSource);
    }
}
