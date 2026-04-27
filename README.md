# smart-notes-ai

A mini web application built with **Spring Boot + Thymeleaf + PostgreSQL + Spring AI**.

It is a normal notes application first, with a small useful AI layer on top.

## Features

### Notes management
- Create notes
- Edit notes
- Delete notes
- View note details
- Assign one category per note
- Assign multiple tags per note
- Search notes by title or content
- Filter notes by category
- Filter notes by tag

### AI features
For each note, you can:
- Generate a concise summary
- Extract key points
- Generate exactly 3 quiz questions

### Small extras
- Dashboard with total note count
- Latest notes section
- Sample seed data
- Bootstrap-based UI
- Friendly error handling

## Tech stack

### Backend
- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Boot DevTools
- Lombok
- PostgreSQL
- Maven

### AI
- Spring AI
- Configurable provider:
  - OpenAI
  - Ollama

### Frontend
- Thymeleaf
- Bootstrap 5

### Docker
- Dockerfile
- docker-compose.yml
- optional docker-compose.ollama.yml

---

## Project structure

```text
smart-notes-ai/
├── Dockerfile
├── docker-compose.yml
├── docker-compose.ollama.yml
├── pom.xml
├── README.md
├── .env.example
└── src
    └── main
        ├── java/com/example/smartnotes
        │   ├── SmartNotesApplication.java
        │   ├── config/
        │   ├── controller/
        │   ├── dto/
        │   ├── exception/
        │   ├── model/
        │   ├── repository/
        │   └── service/
        └── resources
            ├── application.yml
            ├── static/css/app.css
            └── templates/
```

## Database model

### Category
- id
- name

### Tag
- id
- name

### Note
- id
- title
- content
- category
- tags
- createdAt
- updatedAt

Relationships:
- One category -> many notes
- Many tags <-> many notes

---

## Run locally

### 1) Start PostgreSQL
You can use your own PostgreSQL instance, or Docker:

```bash
docker run --name smart-notes-db   -e POSTGRES_DB=smart_notes_ai   -e POSTGRES_USER=smartnotes   -e POSTGRES_PASSWORD=smartnotes   -p 5432:5432   -d postgres:16-alpine
```

### 2) Configure environment variables

Windows PowerShell:
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/smart_notes_ai"
$env:SPRING_DATASOURCE_USERNAME="smartnotes"
$env:SPRING_DATASOURCE_PASSWORD="smartnotes"

$env:AI_PROVIDER="none"
# or "openai" or "ollama"

$env:OPENAI_API_KEY="your-key"
$env:OPENAI_MODEL="gpt-4o-mini"

$env:OLLAMA_BASE_URL="http://localhost:11434"
$env:OLLAMA_MODEL="llama3.2"
```

Linux/macOS:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/smart_notes_ai
export SPRING_DATASOURCE_USERNAME=smartnotes
export SPRING_DATASOURCE_PASSWORD=smartnotes

export AI_PROVIDER=none
# or openai or ollama

export OPENAI_API_KEY=your-key
export OPENAI_MODEL=gpt-4o-mini

export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama3.2
```

### 3) Run the app
```bash
mvn spring-boot:run
```

Open:
```text
http://localhost:8080
```

---

## Run with Docker Compose

### Standard app + PostgreSQL
```bash
docker compose up --build
```

### App + PostgreSQL + Ollama
```bash
docker compose -f docker-compose.yml -f docker-compose.ollama.yml up --build
```

Open:
```text
http://localhost:8080
```

---

## AI configuration

### Disable AI
```bash
AI_PROVIDER=none
```

### Use OpenAI
```bash
AI_PROVIDER=openai
OPENAI_API_KEY=your-key
OPENAI_MODEL=gpt-4o-mini
```

### Use Ollama
Make sure Ollama is running locally and the model exists:
```bash
ollama pull llama3.2
```

Then:
```bash
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
```

---

## Seed data

The app starts with:
- Categories: Study, Work, Personal, Ideas
- Tags: java, spring, exam, project, productivity, summary
- 3 sample notes

---

## Pages

- `/` dashboard
- `/notes` notes list + search/filter
- `/notes/new` create note
- `/notes/{id}` note details
- `/notes/{id}/edit` edit note

AI actions on note details page:
- `POST /notes/{id}/ai/summary`
- `POST /notes/{id}/ai/key-points`
- `POST /notes/{id}/ai/quiz`

---

## Validation rules

- Title: required, 3 to 150 characters
- Content: required, minimum 10 characters
- Category: required
- Tags: optional

---

## Screenshots placeholders

Add screenshots here later:

- `docs/screenshots/dashboard.png`
- `docs/screenshots/notes-list.png`
- `docs/screenshots/note-details.png`

---

## Future improvements

- Pagination
- User authentication
- Rich text editor
- AI-generated tag suggestions
- Export notes to PDF
- Vector search / semantic note search
- Note sharing
- Unit and integration tests

---

## Notes

- The app is designed to stay beginner-friendly.
- AI errors are shown as friendly messages in the UI.
- If AI is not configured, CRUD still works normally.

## License

Student/demo project.
