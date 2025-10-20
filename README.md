# F.L.E.E.T. - Sistema de Gerenciamento de Pátio Mottu

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![Azure](https://img.shields.io/badge/Azure-Service%20Bus%20%7C%20Blob%20Storage-blue)
![Twilio](https://img.shields.io/badge/Twilio-WhatsApp%20%7C%20SendGrid-red)

---

## Índice

1.  [Sobre o Projeto](#1-sobre-o-projeto)
2.  [Arquitetura da Solução](#2-arquitetura-da-solução)
    * [Visão Geral: Java (Admin) vs. C# (Operacional)](#visão-geral-java-admin-vs-c-operacional)
    * [Arquitetura Detalhada da API Java](#arquitetura-detalhada-da-api-java)
3.  [Tecnologias Utilizadas](#3-tecnologias-utilizadas)
4.  [Instalação e Execução (Ambiente de Desenvolvimento)](#4-instalação-e-execução-ambiente-de-desenvolvimento)
5.  [Acessando a Aplicação](#5-acessando-a-aplicação)
6.  [Testando a API via Swagger](#6-testando-a-api-via-swagger)
7.  [Estrutura do Projeto](#7-estrutura-do-projeto)
8.  [Database Migrations](#8-database-migrations)
9.  [Vídeo Demonstrativo](#9-video-explicativo)

---

### 1. Sobre o Projeto

O **F.L.E.E.T.** (Fleet Localization and Efficient Tracking) é a solução de backend desenvolvida em Java para a empresa Mottu. O sistema visa modernizar o processo de gerenciamento de motocicletas no pátio, substituindo o controle manual por um sistema de tracking automatizado, em tempo real e de alta visibilidade.

Esta API serve como o **cérebro administrativo** da plataforma, gerenciando o ciclo de vida de Pátios, Administradores e Funcionários, e servindo como o sistema de autenticação central.

---

### 2. Arquitetura da Solução

#### Visão Geral: Java (Admin) vs. C# (Operacional)

A plataforma F.L.E.E.T. é projetada como um ecossistema de microserviços.

* **API Java:** É o **"Cérebro Administrativo"** (o "RH" do sistema).
    * **Responsabilidade:** Gerenciar o ciclo de vida de Pátios, Admins e Funcionários.
    * **Usuários:** O Painel do Super Admin (web) e o App do Admin de Pátio (mobile).
    * **Autenticação:** É a **autoridade central de autenticação**. Ela gera os JWTs para o funcionário (via Magic Link) e para o admin (via login).

* **API C#:** É o **"Cérebro Operacional"** (o "Controlador de Tráfego").
    * **Responsabilidade:** Gerenciar tarefas, motos e a localização em tempo real (via IoT).
    * **Usuários:** O App do Funcionário (mobile).
    * **Autenticação:** Ela **valida** os JWTs emitidos pela API Java, mas não os gera.

#### Arquitetura Detalhada da API Java

Esta API é construída com:

1.  **API REST & Painel Web:**
    * Uma API RESTful (`/api/**`) para o **App do Admin de Pátio**, protegida por JWT (`Stateless`).
    * Um Painel Web (`/admin/**`) em Thymeleaf para o **Super Admin**, protegido por Sessão (`Stateful`).

2.  **Armazenamento de Arquivos (Ambientes Híbridos):**
    * O sistema é "sensível ao perfil" para gerenciar o upload de fotos e plantas.
    * **Perfil `dev` (Local):** Usa o **Azurite** (emulador do Azure Storage) via Docker Compose. Os contêineres são criados como **públicos** e as URLs são ajustadas para `localhost`, facilitando os testes.
    * **Perfil `prod` (Nuvem):** Conecta-se ao **Azure Blob Storage** real. Os contêineres são criados como **privados**, e a API gera **URLs SAS (Tokens de Acesso Temporário)** para servir os arquivos de forma segura.

3.  **Mensageria e Filas (Azure Service Bus):**
    * O sistema utiliza filas para processamento assíncrono e resiliência.
    * **`whatsapp-failures-queue` / `email-failures-queue`:** Usadas no fluxo de *fallback de notificação*.
    * **`funcionario-criado-queue`:** Fila de **integração inter-serviços**. Publica eventos (`FUNCIONARIO_CRIADO`, `FUNCIONARIO_ATUALIZADO`) que são consumidos pela API de C# para manter os bancos de dados sincronizados.

4.  **Notificação em Tempo Real (WebSocket):**
    * A API expõe um endpoint (`/ws/notifications`) para o App do Admin de Pátio.
    * Quando um envio de notificação falha (detectado pelos `listeners` das filas), o backend envia uma mensagem via WebSocket **diretamente para o admin do pátio afetado**, atualizando a UI em tempo real e, se necessário, fornecendo o Magic Link de fallback para ação manual.

---

### 3. Tecnologias Utilizadas

-   **Linguagem:** Java 17
-   **Framework Principal:** Spring Boot 3
-   **Persistência:** Spring Data JPA / Hibernate
-   **Banco de Dados:** MySQL 8
-   **Migrations:** Flyway
-   **Segurança:** Spring Security (JWT Stateless + Sessão Stateful)
-   **Templates (Painel Admin):** Thymeleaf
-   **Documentação:** Springdoc (Swagger/OpenAPI v3)
-   **Containerização:** Docker & Docker Compose
-   **Geoespacial:** JTS (Java Topology Suite)

-   **Serviços de Nuvem e Integração:**
    -   **Armazenamento:** Azure Blob Storage (com emulador **Azurite** para dev)
    -   **Filas:** Azure Service Bus (via `spring-cloud-azure-starter-servicebus-jms` e `spring-boot-starter-jms`)
    -   **Notificação 1 (WhatsApp):** Twilio API
    -   **Notificação 2 (E-mail):** SendGrid API
    -   **Tempo Real:** Spring Boot Starter WebSocket

---

### 4. Instalação e Execução (Ambiente de Desenvolvimento)

Para executar este projeto em seu ambiente de desenvolvimento, siga os passos abaixo.

#### Pré-requisitos

-   [Git](https://git-scm.com/)
-   [Java (JDK) 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
-   [Apache Maven](https://maven.apache.org/download.cgi)
-   [Docker Desktop](https://www.docker.com/products/docker-desktop/)
-   Uma conta da **Azure** (para o Service Bus)
-   Uma conta do **Twilio** (para o WhatsApp e SendGrid)

#### Instalação e Execução

1.  **Clone o repositório:**
    ```bash
    git clone <URL_DO_SEU_REPOSITORIO>
    cd <NOME_DA_PASTA_DO_PROJETO>
    ```

2.  **Configure o Ambiente (`.env`)**
    Crie o arquivo `.env` a partir do exemplo (`cp .env.example .env`) e preencha **todas** as variáveis.

    ```env
    # --- Perfil do Spring ---
    # Define como 'dev' para rodar o seeder e usar o Azurite
    SPRING_PROFILES_ACTIVE=dev

    # --- Banco de Dados Local (MySQL) ---
    DB_HOST=fleet-db-mysql
    DB_PORT=3306
    DB_USER=fleetuser
    DB_PASSWORD=fleetpass
    DB_NAME=fleetdb

    # --- Segurança (JWT) ---
    JWT_KEY=a2c8a2b5e0f7e4d3c1b9a8e7f6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7
    JWT_EXPIRATION=86400000

    # --- URLs (Para Testes Locais com Ngrok) ---
    # ATENÇÃO: Esta deve ser a URL 'https' fornecida pelo Ngrok
    APPLICATION_BASE_URL=https://<sua_url_ngrok>.ngrok-free.app
    LOCAL_IP=192.168.1.10 # O IP da sua máquina na rede (para o deep link)

    # --- Twilio (WhatsApp) ---
    TWILIO_ACCOUNT_SID=<SEU_TWILIO_SID>
    TWILIO_AUTH_TOKEN=<SEU_TWILIO_TOKEN>
    TWILIO_WHATSAPP_NUMBER=whatsapp:+14155238886 # (Número do Sandbox)

    # --- SendGrid (E-mail Fallback) ---
    SENDGRID_API_KEY=<SUA_CHAVE_API_SENDGRID>
    MAIL_FROM_ADDRESS=<SEU_EMAIL_VERIFICADO_NO_SENDGRID>

    # --- Azure Storage (Blob) ---
    # ATENÇÃO: Em 'dev', usamos a string de conexão padrão do AZURITE
    AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"

    # --- Azure Service Bus (Filas) ---
    # ATENÇÃO: Esta é a string de conexão REAL da sua fila na Azure
    AZURE_SERVICEBUS_CONNECTION_STRING=<SUA_CONNECTION_STRING_DO_SERVICE_BUS>
    AZURE_SERVICEBUS_IDLE_TIMEOUT=60000
    ```

3.  **Construa e Suba os Containers:**
    O Docker Compose irá:
    * Subir o `fleet-db-mysql` (Banco de Dados).
    * Subir o `azurite` (Emulador do Blob Storage).
    * Construir a imagem `fleet-app-java` e subi-la.
    * A aplicação Java se conectará ao MySQL, ao Azurite e ao Service Bus na nuvem.

    ```bash
    docker-compose up --build
    ```
    Para parar e remover os containers e os volumes, use `docker-compose down -v`.

---

### 5. Acessando a Aplicação

A aplicação expõe três interfaces principais em `dev`:

#### **Painel Super Admin (Thymeleaf)**
-   **URL:** `http://localhost:8080/login`
-   **Função:** Gerenciar Pátios, Admins, ver Funcionários e monitorar Filas.
-   **Usuário Seeder:** `super@fleet.com` / `superadmin123`

#### **Documentação da API (Swagger)**
-   **URL:** `http://localhost:8080/swagger-ui.html`
-   **Função:** Testar a API REST.
-   **Usuário Seeder:** `pateo.admin@mottu.com` / `mottu123`
-   *(Para testar webhooks, acesse o Swagger pela URL do Ngrok)*.

#### **Interface de Gerenciamento do Azurite**
-   Para "espiar" os arquivos de imagem e plantas, use o [Azure Storage Explorer](https://azure.microsoft.com/en-us/products/storage/explorer) e conecte-se ao emulador local (Azurite).

---

### 6. Testando a API via Swagger

Para testar os endpoints protegidos (como o CRUD de funcionários), siga este fluxo:

1.  **Obtenha um Token:** Vá para o endpoint `POST /api/auth/login`, clique em "Try it out" e faça o login com as credenciais de um **Admin de Pátio** (ex: `pateo.admin@mottu.com` / `mottu123`).
2.  **Copie o Token:** Copie o valor do token JWT retornado.
3.  **Autorize:** Clique no botão **"Authorize"** no topo da página, cole o token no campo `bearerAuth` e confirme.
4.  **Execute:** Agora você está autenticado e pode testar qualquer outro endpoint da API.

---

### 7. Estrutura do Projeto
O projeto segue uma estrutura de pacotes organizada por **Domínio** e **Infraestrutura** (Clean Architecture).

-   `br.com.mottu.fleet`
    -   `application`: A camada de entrada (Controllers, DTOs, Handlers).
    -   `config`: Configurações do Spring (Security, Swagger, Beans).
    -   `domain`: O "coração" da aplicação.
        -   `entity`: Entidades JPA (o modelo do banco).
        -   `enums`: Enums de negócio.
        -   `exception`: Exceções de negócio customizadas.
        -   `repository`: Interfaces do Spring Data JPA.
        -   `service`: Interfaces e Implementações que contêm as **regras de negócio puras**.
    -   `infrastructure`: A camada de "músculos".
        -   `listener`: Os "Consumers" que ouvem as filas do Azure Service Bus.
        -   `publisher`: Os "Producers" que publicam mensagens nas filas.
        -   `router`: Orquestradores de lógica assíncrona.
        -   `service`: Implementações de interfaces de domínio que falam com **ferramentas externas** (ex: `AzureBlobStorageServiceImpl`).
        -   `websocket`: Classes de configuração e handler para o WebSocket.

---

### 8. Database Migrations

O schema do banco de dados é gerenciado pelo **Flyway**. Todos os scripts de criação e alteração de tabelas estão localizados em:
`src/main/resources/db/migration`

O Flyway executa as migrações automaticamente na inicialização da aplicação.