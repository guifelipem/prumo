# Prumo

Base inicial de três serviços independentes para controle financeiro pessoal. Cada diretório é um projeto Maven próprio; não há módulo compartilhado nem acesso cruzado a bancos.

| Serviço | Responsabilidade | Porta | Banco lógico |
| --- | --- | ---: | --- |
| `auth-service` | Identidade, credenciais e emissão/validação de identidade no futuro | 8081 | `prumo_auth` |
| `account-service` | Contas financeiras e seus dados próprios | 8082 | `prumo_account` |
| `transaction-service` | Lançamentos financeiros, associados a contas por identificador | 8083 | `prumo_transaction` |

Neste estágio, os serviços expõem somente `GET /actuator/health`. Não há endpoints de negócio, entidades ou tabelas.

## Executar localmente

Pré-requisitos: Java 21, Maven 3.6.3+ e PostgreSQL. Crie os três bancos acima e usuários próprios, cada um com acesso somente ao seu banco. Configure, por serviço, as variáveis `DB_HOST` (padrão `localhost`), `DB_PORT` (padrão `5432`), `DB_NAME`, `DB_USERNAME` e `DB_PASSWORD`. Os nomes de banco têm valores padrão distintos no `application.yml`; defina credenciais reais no ambiente. Inicie cada serviço a partir de seu diretório com `mvn spring-boot:run`. A porta pode ser alterada com `SERVER_PORT`.

O endpoint `/actuator/health` verifica também a conexão com o banco. Sem PostgreSQL configurado, a aplicação pode iniciar, mas o health check sinaliza indisponibilidade.

## Limites entre serviços

Cada serviço controla seu próprio banco e suas futuras migrações. O `transaction-service` poderá guardar o ID da conta, mas não terá chave estrangeira entre bancos nem acesso direto ao banco do `account-service`. Para uma operação que exija dados atuais de outro serviço, será usada uma API HTTP explícita com tratamento de falhas. Eventos via RabbitMQ ficam para necessidades assíncronas futuras, como notificações, sem introduzir mensageria agora.

Os pacotes começam planos porque ainda não existe lógica a distribuir. Conforme casos de uso surgirem, cada serviço poderá separar apresentação, aplicação, domínio e infraestrutura onde isso trouxer clareza. A configuração atual inclui Web, JDBC, PostgreSQL, Actuator e suporte a testes, sem ORM ou configuração de segurança prematura.
