# Smart Notes AI

A mini notes application built with **Spring Boot + PostgreSQL + Angular + Gemini API**.

The app is still a normal notes app first. It now includes authentication, so every user must log in and can only access their own notes. The AI layer adds useful study features:

- Generate a short summary from a note
- Extract key points
- Generate 3 quiz questions

The AI part is optimized for speed:

- AI results are cached in PostgreSQL
- Repeated clicks return cached results instantly when the note content did not change
- The AI client is reused instead of rebuilt on every request
- Very long notes are limited before being sent to the model
- Angular disables AI buttons while a request is running

---

## Tech stack

### Backend

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- Spring AI
- PostgreSQL
- Maven

### Frontend

- Angular 18
- Nginx for Docker production serving
- Relative `/api` calls proxied to the Spring Boot backend

### AI provider

Recommended configuration uses **Gemini through the OpenAI-compatible endpoint**. If `.env` is missing or the key is still a placeholder, login/register still works and only the AI buttons stay disabled:

```env
AI_PROVIDER=openai
OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
SPRING_AI_OPENAI_CHAT_COMPLETIONS_PATH=/chat/completions
OPENAI_MODEL=gemini-2.5-flash-lite
OPENAI_API_KEY=YOUR_GEMINI_API_KEY
AI_MAX_INPUT_CHARACTERS=4500
```

The variable names still say `OPENAI_` because Spring AI uses the OpenAI-compatible client. The API key value should be your Gemini API key.

---

## Project structure

```text
smart-notes-ai/
├── Dockerfile                    # Spring Boot backend Dockerfile
├── docker-compose.yml            # PostgreSQL + backend + Angular frontend
├── frontend/                     # Angular frontend
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── src/app/
├── pom.xml
├── .env.example
└── src/main/
    ├── java/com/example/smartnotes/
    │   ├── controller/           # Thymeleaf controllers still kept
    │   ├── controller/api/       # REST API used by Angular
    │   ├── dto/api/              # REST API DTOs
    │   ├── model/
    │   ├── repository/
    │   └── service/
    └── resources/
        ├── application.yml
        ├── static/
        └── templates/            # Old Thymeleaf UI kept for backup
```

---

## Run with Docker Compose

### 1) Create your `.env`

Copy the example file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Then edit `.env` and put your Gemini API key:

```env
OPENAI_API_KEY=YOUR_REAL_GEMINI_API_KEY
```

Also change the JWT secret for real deployments:

```env
APP_JWT_SECRET=replace-this-with-a-long-random-secret-value
APP_JWT_EXPIRATION_HOURS=24
```

### 2) Start everything

```bash
docker compose up --build -d
```

Open the Angular frontend and create an account from the Register tab:

```text
http://localhost:4200
```

Backend health check:

```text
http://localhost:8080/api/health
```

Backend API requires login:

```text
http://localhost:8080/api/notes
```

The old Thymeleaf templates are still in the project, but the Angular UI is now the recommended frontend because it includes login/register/logout screens.

---

## Run locally without Docker frontend

Start PostgreSQL and Spring Boot first, then run Angular:

```bash
cd frontend
npm install
npm start
```

Angular opens on:

```text
http://localhost:4200
```

The Angular dev server uses `proxy.conf.json`, so `/api` is forwarded to:

```text
http://localhost:8080
```


## If Register does not work

First check that the backend container is running:

```bash
docker compose ps
docker logs smart-notes-app --tail=120
```

Also test the backend health URL in the browser:

```text
http://localhost:8080/api/health
```

If the frontend shows that the backend is not ready, the problem is not the Angular form. It means the Spring Boot container failed to start or is still restarting.

---

## REST API used by Angular

```text
POST   /api/auth/register
POST   /api/auth/login
GET    /api/auth/me
GET    /api/dashboard
GET    /api/ai/status
GET    /api/categories
GET    /api/tags
GET    /api/notes
GET    /api/notes/{id}
POST   /api/notes
PUT    /api/notes/{id}
DELETE /api/notes/{id}
POST   /api/notes/{id}/ai/summary
POST   /api/notes/{id}/ai/key-points
POST   /api/notes/{id}/ai/quiz
```

---

## Optional: use local Ollama instead

Ollama is now optional and placed behind a Docker Compose profile so Gemini does not wait for it.

```bash
docker compose --profile local-ai up --build -d
```

Then use:

```env
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_MODEL=llama3.2:1b
```

---

## Notes about authentication

- Passwords are stored with BCrypt hashes, not plain text.
- The backend returns a signed Bearer token after login/register.
- Angular stores the token in `localStorage` and sends it in the `Authorization` header.
- Every note query is filtered by the authenticated user.
- Accessing another user's note ID returns `404`, so note ownership is not leaked.

## Notes about speed

The first AI request can still take more time than cached requests because it must contact Gemini. After one successful AI action for a note, clicking the same action again returns the cached result instantly unless the note content changes.
