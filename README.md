# Prumo

Base de serviços independentes para controle financeiro pessoal e um gateway HTTP. Cada diretório Java é um projeto Maven próprio; não há módulo compartilhado nem acesso cruzado a bancos.

| Serviço | Responsabilidade | Porta | Banco lógico |
| --- | --- | ---: | --- |
| `auth-service` | Cadastro, login e validação de sessões | 8081 | `prumo_auth` |
| `account-service` | Contas financeiras do usuário autenticado | 8082 | `prumo_account` |
| `transaction-service` | Lançamentos e categorias do usuário | 8083 | `prumo_transaction` |
| `api-gateway` | Entrada HTTP para os três serviços | 8080 | nenhum |
| `notification-service` | Consumo e registro de eventos de transação | sem HTTP | nenhum |

Os serviços HTTP expõem `GET /actuator/health`. O consumidor de notificações registra eventos no log; ele ainda não envia mensagens ao usuário.

## Sistema completo com Docker Compose

O `compose.yaml` constrói e inicia o gateway, os quatro serviços, os três bancos PostgreSQL e o RabbitMQ. Cada banco e o RabbitMQ têm volume persistente. As portas publicadas ficam acessíveis apenas em `127.0.0.1`.

1. Copie `.env.example` para `.env` (`Copy-Item .env.example .env` no PowerShell), preencha as três senhas de banco e `RABBITMQ_PASSWORD` com valores diferentes e gere uma `INTERNAL_SERVICE_KEY` aleatória de pelo menos 32 bytes. Se já possui `.env`, adicione `RABBITMQ_PASSWORD`. O arquivo `.env` é ignorado pelo Git.
2. Execute `docker compose up --build -d` na raiz do projeto. Docker constrói as aplicações com Maven e Java 21; não é necessário instalar Maven no host.
3. Confira a inicialização com `docker compose ps`: os serviços HTTP, bancos e RabbitMQ devem aparecer como `healthy`; `notification-service` deve aparecer como `running`. Para parar sem apagar os dados, use `docker compose down`.
4. Execute `pwsh ./scripts/smoke-test.ps1` para validar cadastro, autenticação, contas e lançamentos por HTTP.

O gateway atende em `http://localhost:8080` com os caminhos originais: `/users` e `/sessions` vão ao `auth-service`; `/accounts` e `/accounts/{id}` ao `account-service`; `/transactions`, `/transactions/balance`, `/transactions/{id}`, `/categories` e `/categories/{id}` ao `transaction-service`. O endpoint interno `/internal/sessions/introspect` não é roteado. O cabeçalho `Authorization` é repassado sem alteração. O gateway aceita um `X-Correlation-Id` existente ou gera um UUID, repassa o valor ao serviço e o devolve na resposta.

Para testar pelo gateway, rode `pwsh ./scripts/smoke-test.ps1 -AuthBaseUrl http://localhost:8080 -AccountBaseUrl http://localhost:8080 -TransactionBaseUrl http://localhost:8080`. Verifique a saúde em `http://localhost:8080/actuator/health`. CORS permite por padrão `http://localhost:3000`; configure `CORS_ALLOWED_ORIGINS` no `.env` para outra origem do frontend. As URLs dos destinos são configuradas por `AUTH_BASE_URL`, `ACCOUNT_BASE_URL` e `TRANSACTION_BASE_URL` no processo do gateway; o Compose usa os nomes dos serviços na rede interna.

| Aplicação executada no host | Banco | `DB_PORT` | `DB_USERNAME` | `DB_PASSWORD` |
| --- | --- | ---: | --- | --- |
| `auth-service` | `prumo_auth` | 5433 | `prumo_auth` | valor de `AUTH_DB_PASSWORD` no `.env` |
| `account-service` | `prumo_account` | 5434 | `prumo_account` | valor de `ACCOUNT_DB_PASSWORD` no `.env` |
| `transaction-service` | `prumo_transaction` | 5435 | `prumo_transaction` | valor de `TRANSACTION_DB_PASSWORD` no `.env` |

Para iniciar uma aplicação fora do Compose, use Java 21 e Maven 3.6.3+, configure as variáveis da tabela no processo do serviço e execute `mvn spring-boot:run` em seu diretório. `DB_HOST` permanece `localhost` e `DB_NAME` já tem o valor correspondente como padrão no `application.yml`. Configure o mesmo `INTERNAL_SERVICE_KEY` aleatório de pelo menos 32 bytes nos três serviços. A porta HTTP de cada aplicação pode ser alterada com `SERVER_PORT`. Dentro do Compose, os serviços usam os nomes dos contêineres como endereços e a porta interna `5432`.

Para usar um PostgreSQL instalado fora do Docker, o script `database/bootstrap.psql` continua disponível como alternativa para criar os bancos e usuários manualmente.

O endpoint `/actuator/health` verifica também a conexão com o banco. Cada serviço aplica suas próprias migrações Flyway na inicialização e exige conexão válida. O `account-service` consulta o `auth-service` pelo endereço `AUTH_BASE_URL` (padrão `http://localhost:8081`). O `transaction-service` consulta o `account-service` por `ACCOUNT_BASE_URL` (padrão `http://localhost:8082`) e valida tokens para categorias diretamente no `auth-service` por `AUTH_BASE_URL`. Configure o mesmo `INTERNAL_SERVICE_KEY` também no `transaction-service`.

O script de smoke test cria dois usuários de teste, duas contas e dois lançamentos, percorre o fluxo HTTP completo, verifica a paginação e confirma que o segundo usuário não consegue acessar a conta do primeiro. Ele cria dados reais nesses bancos; use bancos de desenvolvimento. As URLs podem ser alteradas pelos parâmetros `-AuthBaseUrl`, `-AccountBaseUrl` e `-TransactionBaseUrl`.

## Fluxo HTTP inicial

1. `POST http://localhost:8081/users` com `{"email":"ana@example.com","password":"uma senha com pelo menos 12 caracteres"}` cria o usuário e retorna ID e e-mail (`201`). E-mail duplicado retorna `409`.
2. `POST http://localhost:8081/sessions` com o mesmo e-mail e senha retorna `accessToken` e `expiresAt`. Credenciais incorretas retornam `401`.
3. `POST http://localhost:8082/accounts` com `Authorization: Bearer <accessToken>` e `{"name":"Conta corrente","currency":"BRL"}` cria uma conta (`201`). `GET /accounts/{id}` retorna apenas uma conta pertencente ao mesmo usuário. `GET /accounts` lista suas contas.
4. `POST http://localhost:8083/transactions` com o mesmo cabeçalho e `{"accountId":"<id da conta>","type":"EXPENSE","amount":25.50,"description":"Mercado","occurredAt":"2024-05-03T12:30:00Z"}` registra um lançamento (`201`). `occurredAt` é opcional e usa o instante atual quando omitido. `INCOME` é o outro tipo aceito. `GET /transactions?accountId=<id da conta>` lista os lançamentos dessa conta após confirmar o acesso do usuário. `GET /transactions/balance?accountId=<id da conta>` retorna `{"accountId":"<id da conta>","balance":0.00}`, calculando receitas menos despesas sobre todos os lançamentos da conta; uma conta vazia tem saldo zero.

As duas listagens aceitam `page` (a partir de 0) e `size` (de 1 a 100), com padrão `page=0&size=20`. Contas são ordenadas por data de criação; lançamentos, do mais recente para o mais antigo. As respostas são listas JSON, sem total de páginas nesta etapa.

Categorias ficam no `transaction-service`: `POST /categories` recebe `{"name":"Alimentação","type":"EXPENSE"}`, `GET /categories` lista as categorias do usuário, `PUT /categories/{id}` altera nome e tipo, e `DELETE /categories/{id}` exclui uma categoria sem lançamentos (`409` se estiver em uso). O tipo pode ser `INCOME`, `EXPENSE` ou `BOTH`; quando omitido, usa `BOTH`. Na criação de transação, `categoryId` é opcional e só aceita uma categoria do próprio usuário compatível com o tipo do lançamento.

`PUT /transactions/{id}` substitui `type`, `amount`, `description`, `occurredAt` e `categoryId` (opcional), sem permitir trocar a conta. `createdAt` é definido na criação e permanece fixo; para lançamentos anteriores à migration, ele recebe o valor histórico de `occurredAt`. `DELETE /transactions/{id}` remove o lançamento. Ambos exigem o token do proprietário e retornam `404` para lançamentos inexistentes ou de outro usuário. O saldo é calculado a partir dos lançamentos atuais, refletindo edições e exclusões.

Tokens são aleatórios, expiram em uma hora e são armazenados apenas como hash no banco de autenticação. A validação síncrona usa `POST /internal/sessions/introspect` com `X-Service-Key`; o cliente de contas falha fechado se a autenticação estiver indisponível. O serviço de lançamentos consulta a conta usando o token recebido. As chamadas entre serviços têm timeout de dois segundos. Em um ambiente fora da máquina local, use HTTPS e mantenha o endpoint interno acessível apenas na rede privada.

## Eventos de transação

O `transaction-service` publica mensagens JSON no exchange durável `prumo.transactions` com as routing keys `TransactionCreated`, `TransactionUpdated` e `TransactionDeleted`. A fila durável `prumo.notifications.transactions` recebe os três tipos. Cada mensagem inclui `eventId`, `eventType`, `occurredAt`, `ownerId`, `transactionId`, `accountId` e o estado da transação. Na exclusão, o estado é o último registro antes de removê-lo.

O `notification-service` consome a fila, valida IDs, tipo, horário e correspondência dos IDs com os dados da transação, e registra no log o tipo e os identificadores. Mensagens inválidas são rejeitadas sem reentrega. Para acompanhar o fluxo após criar, editar ou excluir uma transação, execute `docker compose logs -f notification-service`. O painel local do RabbitMQ fica em `http://localhost:15672` com usuário `prumo` e senha `RABBITMQ_PASSWORD`.

Esta primeira implementação publica depois da gravação no banco. Banco e RabbitMQ não participam de uma transação atômica: uma falha de publicação após a gravação pode deixar a alteração persistida sem evento. Um outbox transacional é o próximo passo se for necessária garantia de entrega.

## Limites entre serviços

Cada serviço controla seu próprio banco e migrações. O `transaction-service` guarda o ID da conta e do proprietário confirmado pelo `account-service`, sem chave estrangeira entre bancos ou acesso direto ao banco de contas. A comunicação síncrona é usada apenas para autenticar a requisição e confirmar o acesso à conta. Eventos de transação seguem pelo RabbitMQ para o `notification-service`, sem acesso ao banco de lançamentos pelo consumidor.

### Transferências entre contas

`POST /transfers` recebe `{"sourceAccountId":"<uuid>","destinationAccountId":"<uuid>","amount":300.00,"description":"Reserva","occurredAt":"2024-05-03T12:30:00Z"}`. A data é opcional na criação. As duas contas devem pertencer ao usuário, ser diferentes e usar a mesma moeda; o valor precisa ser positivo. `GET /transfers` lista com `page` e `size`, `GET /transfers/{id}` consulta, `PUT /transfers/{id}` altera todos os campos (incluindo `occurredAt`) e `DELETE /transfers/{id}` exclui a transferência.

Cada transferência cria uma despesa na origem e uma receita no destino, ambas com `transferId` e sem categoria. As duas movimentações e a transferência são gravadas na mesma transação de banco; uma restrição diferida do PostgreSQL verifica que ambas correspondem à operação. O saldo das contas inclui essas movimentações. `PUT` e `DELETE` de uma perna por `/transactions/{id}` retornam `409`; use `/transfers/{id}` para manter os dois lados sincronizados. Ao calcular receitas e despesas reais em relatórios, filtre `transfer_id IS NULL`.

Os pacotes continuam pequenos e organizados por responsabilidade dentro de cada serviço. Regras ficam nos serviços, SQL nos repositórios e HTTP nos controllers. A configuração inclui Web, JDBC, PostgreSQL, Flyway, Actuator e testes, sem ORM. A senha é armazenada como hash BCrypt. Este fluxo ainda não inclui logout, renovação de sessão ou limitação de tentativas de login.

## Testes com PostgreSQL real

Com Java 21, Maven e Docker em execução, rode `mvn test` dentro de cada diretório de serviço. Os testes de integração usam Testcontainers para iniciar um PostgreSQL temporário. O Spring aplica as migrations Flyway na inicialização do contexto; os testes exercitam repositórios e, no `transaction-service`, os fluxos de criação, listagem, edição, exclusão e saldo. As chamadas HTTP a outros serviços são simuladas nesses testes. Não é necessário iniciar o Compose nem configurar `.env` para executá-los.

Em cada serviço, `api` contém controllers e dados de entrada HTTP, `application` concentra os casos de uso, `domain` guarda os tipos do domínio, e `persistence` contém o acesso ao banco próprio. `integration` existe apenas em `account-service` e `transaction-service`, para as chamadas HTTP a outros serviços. Os testes de caso de uso ficam no pacote `application` correspondente.
