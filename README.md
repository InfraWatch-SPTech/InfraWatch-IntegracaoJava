# InfraWatch-IntegracaoJava

Projeto Java que integra tres pontos do fluxo de monitoramento:

1. leitura do ultimo JSON gerado no S3;
2. comparacao das metricas com os limites cadastrados no MySQL;
3. criacao de chamado no Jira quando uma regra de alerta e violada.

O projeto esta em uma etapa de integracao entre fontes de dados e sistemas externos. Ele nao usa Spring Boot. O que existe hoje e uma aplicacao Java pura, com `JdbcTemplate` para o banco, AWS SDK para o S3, Jackson para leitura de JSON e `HttpClient` para a comunicacao com o Jira.

## Objetivo atual

O objetivo do sistema e identificar quando um equipamento ultrapassa os limites configurados para seus componentes, abrir uma issue no Jira com o contexto do alerta e persistir esse chamado no banco para manter historico e rastreabilidade.

## Como o projeto esta organizado

### `Main`
Ponto de entrada da aplicacao. Ele monta as dependencias, busca o ultimo arquivo do S3 e dispara o processamento principal do alerta.

### `config`
- `S3Provider`: cria o cliente AWS S3 a partir das variaveis de ambiente.
- `DBConnectionProvider`: monta o `JdbcTemplate` usado pelos DAO para falar com o MySQL.

### `service`
- `S3Service`: encontra o JSON mais recente no bucket e converte o conteudo para `DadosHardwareModelo`.
- `AlertaService`: concentra a regra de negocio do alerta. Ele cruza o JSON com os parametros ativos do banco, decide se o alerta e critico ou de atencao, cria a issue no Jira e pede a persistencia do chamado.

### `controller`
- `JiraController`: faz a comunicacao HTTP com a API do Jira para criar issues.
- `AlertaController`: existe como classe de apoio, mas nao faz parte do fluxo principal atual.

### `dao`
- `AlertaDAO`: executa as consultas SQL relacionadas aos alertas e tambem busca informacoes auxiliares de equipamento e parametros de alerta.

### `model`
- `DadosHardwareModelo`: representa o JSON lido do S3.
- `Alerta`: representa o alerta consolidado que sera persistido no banco.

### `dto`
Esses objetos montam o payload enviado ao Jira:
- `ProjectDto`
- `IssueTypeDto`
- `IssueRequestDto`
- `IssueFieldsDto`
- `DescriptionDto`

## Fluxo completo de execucao

### 1. Inicio em `Main`
`Main.main()` carrega as variaveis do `.env`, cria o cliente do S3 e instancia o `S3Service`.

Em seguida, ele chama:

```java
readerService.lerUltimoArquivoJson("gold-api-java", "NovaTech Solucoes", "8")
```

Esse valor mostra que o fluxo atual trabalha com um bucket, uma empresa e um equipamento especificos.

### 2. Leitura do arquivo no S3
`S3Service.lerUltimoArquivoJson(...)` monta o prefixo:

```text
gold/{empresa}/{equipamento}/
```

Depois ele:
- lista os objetos nesse caminho;
- filtra os arquivos `.json`;
- escolhe o mais recente pela data de ultima modificacao;
- baixa o conteudo;
- converte o JSON para `DadosHardwareModelo`.

Se nenhum JSON existir ou se houver erro na conversao, o metodo retorna `null` e o fluxo principal nao avanca.

### 3. Cria as dependencias do alerta
Se o JSON foi lido com sucesso, `Main` cria:

- `JiraController`
- `AlertaService`

O `JiraController` recebe as credenciais do Jira vindas do `.env`. O `AlertaService` recebe essa instancia do Jira para poder abrir chamados quando necessario.

### 4. Inicio da regra de negocio
`AlertaService.verificarEEnviarAlertas(dados)` recebe o modelo vindo do S3 e inicia o processamento.

A primeira responsabilidade desse metodo e descobrir qual equipamento do banco corresponde ao valor recebido no JSON. Para isso, ele chama `buscarIdEquipamento(...)`, que tenta primeiro resolver por ID numerico e, se necessario, por nome.

### 5. Busca dos parametros ativos no banco
Com o equipamento identificado, o service chama `AlertaDAO.buscarParametrosAtivos(idEquipamento)`.

Essa consulta traz os componentes monitorados daquele equipamento, seus limites de atencao e critico, o nome do componente e o tipo cadastrado no banco.

### 6. Relacionamento entre componente e metrica do JSON
Para cada componente retornado do banco, `AlertaService` chama `extrairValorMetrica(...)`.

Esse metodo usa o tipo e o nome do componente para descobrir qual campo do JSON corresponde a ele. O modelo `DadosHardwareModelo` possui suporte a nomes alternativos para as metricas, entao o fluxo aceita variações como CPU, Processador, RAM, Memoria e similares.

Se a metrica nao existir no JSON, o componente e apenas logado e o fluxo segue para o proximo.

### 7. Avaliacao da severidade
Quando existe valor medido, `AlertaService.avaliarEEnviarAlerta(...)` compara o valor com os limites:

- se o valor for maior ou igual ao limite critico, o alerta e tratado como `CRITICO`;
- se for maior ou igual ao limite de atencao, o alerta e tratado como `ALERTA`;
- se estiver abaixo dos limites, nada e enviado para o Jira.

### 8. Montagem do chamado no Jira
Quando ha alerta, o service monta duas informacoes:

- `summary`: titulo curto da issue;
- `descriptionText`: descricao detalhada do alerta.

O `JiraController.createIssue(...)` recebe esses dados, monta o payload em ADF para o campo `description` e envia a requisicao para o endpoint de criacao de issues do Jira.

### 9. Persistencia do chamado no banco
A resposta do Jira retorna a `key` da issue criada.

O service extrai essa key, monta um objeto `Alerta` com:

- `jiraKey`
- `fkEquipamento`
- `fkComponente`
- `tipoAlerta`
- `valorMedido`
- `limiteViolado`

Depois ele chama `alertaDAO.salvar(alerta)` para persistir o chamado no banco.

### 10. Resultado final
Ao final do processo, o sistema:

- le o JSON mais recente do S3;
- identifica os componentes relevantes;
- verifica quais passaram dos limites;
- cria a issue no Jira;
- salva o registro do alerta no banco.

## Responsabilidade de cada classe principal

### `school.sptech.Main`
Coordena a inicializacao da aplicacao e chama o fluxo principal.

### `school.sptech.service.S3Service`
Localiza o arquivo mais recente no S3 e converte o JSON para objeto Java.

### `school.sptech.model.DadosHardwareModelo`
Modela os dados lidos do JSON e faz o mapeamento de metricas com nomes alternativos.

### `school.sptech.service.AlertaService`
Aplica as regras de negocio do alerta e integra S3, MySQL e Jira.

### `school.sptech.dao.AlertaDAO`
Executa as consultas SQL usadas pelo fluxo de alerta.

### `school.sptech.controller.JiraController`
Monta e envia a requisicao HTTP para o Jira.

### `school.sptech.model.Alerta`
Transporta os dados do chamado consolidado para persistencia.

### `school.sptech.dto.*`
Estruturam o payload esperado pelo Jira.

## Variaveis de ambiente esperadas

O projeto usa `dotenv-java` para carregar configuracoes do arquivo `.env`.

Variaveis usadas no codigo atual:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`
- `AWS_REGION`
- `AWS_BUCKET_NAME`
- `DB_URL`
- `DB_USUARIO`
- `DB_SENHA`
- `JIRA_BASE_URL`
- `JIRA_EMAIL`
- `JIRA_API_TOKEN`

## Observacoes importantes

- O fluxo principal esta concentrado em `Main`, nao em Spring Boot.
- O caminho do S3, a empresa e o equipamento usados no processamento estao fixos na chamada atual do `Main`.
- O projeto foi desenhado em camadas, mas ainda com uma estrutura simples, sem container de injeccao de dependencia.

## Tecnologias usadas

- Java 21
- Maven
- Spring JDBC
- Apache Commons DBCP2
- AWS SDK for Java v2
- Jackson
- dotenv-java
- MySQL
- Jira REST API
