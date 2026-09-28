# Prumo

Base inicial de três serviços independentes para controle financeiro pessoal. Cada diretório é um projeto Maven próprio; não há módulo compartilhado nem acesso cruzado a bancos.

| Serviço | Responsabilidade | Porta | Banco lógico |
| --- | --- | ---: | --- |
| `auth-service` | Cadastro, login e validação de sessões | 8081 | `prumo_auth` |
| `account-service` | Contas financeiras do usuário autenticado | 8082 | `prumo_account` |
| `transaction-service` | Lançamentos de contas acessíveis ao usuário | 8083 | `prumo_transaction` |

Os três serviços expõem `GET /actuator/health`. Há um fluxo inicial de cadastro, login, criação e listagem de contas e lançamentos, além de consulta de saldo por conta. Não há atualização, exclusão ou notificações.

## Bancos locais com Docker Compose

O `compose.yaml` inicia **somente os três bancos PostgreSQL**. Cada banco tem seu próprio contêiner, volume persistente e porta acessível apenas em `127.0.0.1`.

1. Copie `.env.example` para `.env` (`Copy-Item .env.example .env` no PowerShell), preencha as três senhas com valores diferentes e gere uma `INTERNAL_SERVICE_KEY` aleatória de pelo menos 32 bytes. O arquivo `.env` é ignorado pelo Git.
2. Execute `docker compose up -d` na raiz do projeto.
3. Confira a inicialização com `docker compose ps`. Para parar sem apagar os dados, use `docker compose down`.

| Aplicação executada no host | Banco | `DB_PORT` | `DB_USERNAME` | `DB_PASSWORD` |
| --- | --- | ---: | --- | --- |
| `auth-service` | `prumo_auth` | 5433 | `prumo_auth` | valor de `AUTH_DB_PASSWORD` no `.env` |
| `account-service` | `prumo_account` | 5434 | `prumo_account` | valor de `ACCOUNT_DB_PASSWORD` no `.env` |
| `transaction-service` | `prumo_transaction` | 5435 | `prumo_transaction` | valor de `TRANSACTION_DB_PASSWORD` no `.env` |

As aplicações continuam fora do Compose. Para iniciá-las localmente, use Java 21 e Maven 3.6.3+, configure as variáveis da tabela no processo de cada serviço e execute `mvn spring-boot:run` em seu diretório. `DB_HOST` permanece `localhost` e `DB_NAME` já tem o valor correspondente como padrão no `application.yml`. Configure o mesmo `INTERNAL_SERVICE_KEY` aleatório de pelo menos 32 bytes no `auth-service` e no `account-service`. A porta HTTP de cada aplicação pode ser alterada com `SERVER_PORT`.

Para usar um PostgreSQL instalado fora do Docker, o script `database/bootstrap.psql` continua disponível como alternativa para criar os bancos e usuários manualmente.

O endpoint `/actuator/health` verifica também a conexão com o banco. Cada serviço aplica suas próprias migrações Flyway na inicialização e exige conexão válida. O `account-service` consulta o `auth-service` pelo endereço `AUTH_BASE_URL` (padrão `http://localhost:8081`). O `transaction-service` consulta o `account-service` por `ACCOUNT_BASE_URL` (padrão `http://localhost:8082`).

Com os três serviços ativos e saudáveis, execute `pwsh ./scripts/smoke-test.ps1` na raiz do projeto. O script cria dois usuários de teste, duas contas e dois lançamentos, percorre o fluxo HTTP completo, verifica a paginação e confirma que o segundo usuário não consegue acessar a conta do primeiro. Ele cria dados reais nesses bancos; use bancos de desenvolvimento. As URLs podem ser alteradas pelos parâmetros `-AuthBaseUrl`, `-AccountBaseUrl` e `-TransactionBaseUrl`.

## Fluxo HTTP inicial

1. `POST http://localhost:8081/users` com `{"email":"ana@example.com","password":"uma senha com pelo menos 12 caracteres"}` cria o usuário e retorna ID e e-mail (`201`). E-mail duplicado retorna `409`.
2. `POST http://localhost:8081/sessions` com o mesmo e-mail e senha retorna `accessToken` e `expiresAt`. Credenciais incorretas retornam `401`.
3. `POST http://localhost:8082/accounts` com `Authorization: Bearer <accessToken>` e `{"name":"Conta corrente","currency":"BRL"}` cria uma conta (`201`). `GET /accounts/{id}` retorna apenas uma conta pertencente ao mesmo usuário. `GET /accounts` lista suas contas.
4. `POST http://localhost:8083/transactions` com o mesmo cabeçalho e `{"accountId":"<id da conta>","type":"EXPENSE","amount":25.50,"description":"Mercado","occurredAt":"2024-05-03T12:30:00Z"}` registra um lançamento (`201`). `occurredAt` é opcional e usa o instante atual quando omitido. `INCOME` é o outro tipo aceito. `GET /transactions?accountId=<id da conta>` lista os lançamentos dessa conta após confirmar o acesso do usuário. `GET /transactions/balance?accountId=<id da conta>` retorna `{"accountId":"<id da conta>","balance":0.00}`, calculando receitas menos despesas sobre todos os lançamentos da conta; uma conta vazia tem saldo zero.

As duas listagens aceitam `page` (a partir de 0) e `size` (de 1 a 100), com padrão `page=0&size=20`. Contas são ordenadas por data de criação; lançamentos, do mais recente para o mais antigo. As respostas são listas JSON, sem total de páginas nesta etapa.

Tokens são aleatórios, expiram em uma hora e são armazenados apenas como hash no banco de autenticação. A validação síncrona usa `POST /internal/sessions/introspect` com `X-Service-Key`; o cliente de contas falha fechado se a autenticação estiver indisponível. O serviço de lançamentos consulta a conta usando o token recebido. As chamadas entre serviços têm timeout de dois segundos. Em um ambiente fora da máquina local, use HTTPS e mantenha o endpoint interno acessível apenas na rede privada.

## Limites entre serviços

Cada serviço controla seu próprio banco e migrações. O `transaction-service` guarda o ID da conta e do proprietário confirmado pelo `account-service`, sem chave estrangeira entre bancos ou acesso direto ao banco de contas. A comunicação síncrona é usada apenas para autenticar a requisição e confirmar o acesso à conta. Eventos via RabbitMQ ficam para necessidades assíncronas futuras, como notificações, sem introduzir mensageria agora.

Os pacotes continuam pequenos e organizados por responsabilidade dentro de cada serviço. Regras ficam nos serviços, SQL nos repositórios e HTTP nos controllers. A configuração inclui Web, JDBC, PostgreSQL, Flyway, Actuator e testes, sem ORM. A senha é armazenada como hash BCrypt. Este fluxo ainda não inclui logout, renovação de sessão, limitação de tentativas de login ou testes de integração com PostgreSQL; essas melhorias devem acompanhar uma próxima evolução da autenticação.

Em cada serviço, `api` contém controllers e dados de entrada HTTP, `application` concentra os casos de uso, `domain` guarda os tipos do domínio, e `persistence` contém o acesso ao banco próprio. `integration` existe apenas em `account-service` e `transaction-service`, para as chamadas HTTP a outros serviços. Os testes de caso de uso ficam no pacote `application` correspondente.
