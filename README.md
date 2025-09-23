# F.L.E.E.T. - Sistema de Gerenciamento de Pátio Mottu

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

---

## Índice

1.  [Sobre o Projeto](#1-sobre-o-projeto)
2.  [Arquitetura da Solução](#2-arquitetura-da-solução)
3.  [Tecnologias Utilizadas](#3-tecnologias-utilizadas)
4.  [Instalação e Execução](#4--instalação-e-execução)
5.  [Acessando a Aplicação](#5--acessando-a-aplicação)
6.  [Testando a API via Swagger](#6--testando-a-api-via-swagger)
7.  [Estrutura do Projeto](#7-estrutura-do-projeto)
8.  [Database Migrations](#8-database-migrations)

---

### 1. Sobre o Projeto

O **F.L.E.E.T.** (Fleet Localization and Efficient Tracking) é a solução de backend desenvolvida em Java para o desafio do **Challenge FIAP 2025 - Mottu**. O sistema visa modernizar o processo de gerenciamento de motocicletas no pátio, substituindo o controle manual por um sistema de tracking automatizado, em tempo real e de alta visibilidade.

---

### 2. Arquitetura da Solução

O backend foi construído com uma arquitetura containerizada com Docker.

-   **Backend:** API RESTful construída com Java 17 e Spring Boot.
-   **Banco de Dados:** MySQL 8, com schema gerenciado por Flyway.
-   **Segurança:**
    -   **Painel Web (Super Admin):** Autenticação Stateful com Spring Security (sessão via cookie).
    -   **API (Admin do Pátio):** Autenticação Stateless com JSON Web Tokens (JWT).
-   **Documentação:** A API é autodocumentada utilizando Springdoc (Swagger/OpenAPI).

---

### 3. Tecnologias Utilizadas

-   **Linguagem:** Java 17
-   **Framework Principal:** Spring Boot 3
-   **Persistência:** Spring Data JPA / Hibernate
-   **Banco de Dados:** MySQL 8
-   **Migrations:** Flyway
-   **Segurança:** Spring Security
-   **Templates (Painel Admin):** Thymeleaf
-   **API:** REST com `ResponseEntity`
-   **Documentação:** Springdoc (Swagger/OpenAPI v3)
-   **Validação:** Jakarta Bean Validation
-   **Containerização:** Docker & Docker Compose
-   **Geoespacial:** JTS (Java Topology Suite) para manipulação de polígonos (`GEOMETRY`)

---

### 4. Instalação e Execução

Para executar este projeto em seu ambiente de desenvolvimento, siga os passos abaixo.

#### Pré-requisitos

-   [Git](https://git-scm.com/)
-   [Java (JDK) 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
-   [Apache Maven](https://maven.apache.org/download.cgi)
-   [Docker Desktop](https://www.docker.com/products/docker-desktop/)

#### Instalação e Execução

1.  **Clone o repositório:**
    ```bash
    git clone <URL_DO_SEU_REPOSITORIO>
    cd <NOME_DA_PASTA_DO_PROJETO>
    ```

2.  **Configure o Ambiente:**
    O projeto usa um arquivo `.env` para gerenciar as variáveis de ambiente. Crie este arquivo a partir do exemplo fornecido.

    ```bash
    # No Windows (PowerShell)
    cp .env.example .env

    # No Linux ou macOS
    cp .env.example .env
    ```

    O arquivo `.env.example` já vem com valores padrão para o ambiente de desenvolvimento:
    ```env
    DB_USER=fleetuser
    DB_PASSWORD=fleetpass
    DB_NAME=fleetdb
    DB_PORT=3306
    JWT_KEY=a2c8a2b5e0f7e4d3c1b9a8e7f6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7
    JWT_EXPIRATION=86400000
    ```

3.  **Construa e Suba os Containers:**
    O Docker Compose fará todo o trabalho de construir a imagem da aplicação Java e subir o container do banco de dados.

    ```bash
    docker-compose up --build
    ```
    Na primeira vez, isso pode levar alguns minutos. Após o processo, a aplicação estará rodando e acessível. Para parar a aplicação, pressione `Ctrl + C`. Para parar e remover os containers, use `docker-compose down`.

---

### 5. Acessando a Aplicação

A aplicação expõe duas interfaces principais:

#### **Painel Super Admin (Thymeleaf)**
-   **URL:** `http://localhost:8080/login`
-   **Função:** Gerenciar o onboarding de novas unidades Mottu (Pátios e seus Administradores).
-   **Credenciais de Desenvolvimento:**
    -   **Email:** `super@fleet.com`
    -   **Senha:** `superadmin123`
    *(Este usuário é criado automaticamente pelo `DataInitializer` em ambiente de desenvolvimento).*

#### **Documentação da API (Swagger)**
-   **URL:** `http://localhost:8080/swagger-ui.html`
-   **Função:** Visualizar, documentar e testar todos os endpoints da API REST.

---

### 6. Testando a API via Swagger

Para testar os endpoints protegidos (como o CRUD de funcionários), siga este fluxo:

1.  **Obtenha um Token:** Vá para o endpoint `POST /api/auth/login`, clique em "Try it out" e faça o login com as credenciais de um **Admin de Pátio** (ex: `pateo.admin@mottu.com` / `mottu123`, criado pelo seeder).
2.  **Copie o Token:** Copie o valor do token JWT retornado no corpo da resposta.
3.  **Autorize:** Clique no botão **"Authorize"** no topo da página, cole o token no campo **`bearerAuth`** e confirme.
4.  **Execute:** Agora você está autenticado e pode testar qualquer outro endpoint da API.

---

### 7. Estrutura do Projeto
O projeto segue uma estrutura de pacotes organizada por camadas:

-   `br.com.mottu.fleet`
    -   `application`: Contém Controllers, DTOs, Handlers e Validadores (a camada de interface com o mundo exterior).
    -   `config`: Contém as classes de configuração do Spring (Security, Swagger, Beans, Seeders de dev).
    -   `domain`: O core da aplicação.
        -   `entity`: As entidades JPA que mapeiam o banco de dados.
        -   `enums`: Os tipos enumerados de negócio.
        -   `exception`: As exceções customizadas.
        -   `repository`: As interfaces do Spring Data JPA.
        -   `service`: As classes que contêm as regras de negócio.

---

### 8. Database Migrations

O schema do banco de dados é gerenciado pelo **Flyway**. Todos os scripts de criação e alteração de tabelas estão localizados em:
`src/main/resources/db/migration`

O Flyway executa as migrações automaticamente na inicialização da aplicação, garantindo que o banco de dados esteja sempre na versão correta.