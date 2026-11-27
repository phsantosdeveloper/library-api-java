# Library API

API REST para gerenciamento de biblioteca (autores, categorias, livros e empréstimos), com autenticação OAuth2 + JWT e controle de acesso por perfil.

## Tecnologias

Java 25 · Spring Boot 4 · Spring Security (OAuth2 + JWT) · PostgreSQL · H2 · Docker

## Como executar

```bash
git clone https://github.com/phsantosdeveloper/library-api-java.git
cd library-api-java
cp .env.example .env   # preencha as variáveis
```

**Com Docker (PostgreSQL):**

```bash
docker compose up --build
```

**Local com H2:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

A API sobe em `http://localhost:8080`.

## Autenticação

`POST /oauth2/token` com `grant_type=password`, usando `CLIENT_ID` e `CLIENT_SECRET` via Basic Auth.

Usuário de exemplo (perfil `test`): `ana.souza@example.com` / `12345678` (ADMIN)

## Testes

```bash
./mvnw test
```
