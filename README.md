# conexao-banco-de-dados

Projeto Java para testar conexao com banco de dados usando `JDBC`, `Spring JDBC` e `JdbcTemplate`.

## Estado atual

- Conexao com o banco funcionando
- Classe `AlertaDAO` com consulta `buscarTodos`
- Classes base de dominio para `Alerta` e `Empresa`
- Estrutura inicial para evoluir o projeto sem colocar tudo na `Main`

## Estrutura principal

- `Main` - ponto de entrada da aplicacao
- `banco/Alerta` - entidade de alerta
- `banco/Empresa` - entidade base de empresa
- `banco/AlertaDAO` - acesso ao banco e consultas SQL
- `banco/DBConnectionProvider` - provider da conexao com o banco

## Como executar

1. Abrir o projeto em uma IDE Java
2. Ajustar os dados de conexao no `DBConnectionProvider`
3. Executar a classe `Main`

## Visao futura

O proximo passo e evoluir o projeto para uma organizacao melhor por camadas, com separacao entre:

- modelo
- acesso ao banco
- regras de negocio
- integracao com Jira

Tambem e esperado adicionar as operacoes de:

- `INSERT`
- `UPDATE`
- `DELETE`
- consultas filtradas por id, empresa e status

## Tecnologias

- Java 21
- Spring JDBC
- JDBC
- MySQL
- Maven
