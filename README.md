<h1>BetterTapeBot</h1>

<p>
  <strong>A Telegram Bot to track possible movie names based on funny quotes</strong>
</p>

<p>
  <a href="https://github.com/MathiasSonderfeld/BetterTapeBot/actions/workflows/build.yaml">
    <img src="https://github.com/MathiasSonderfeld/BetterTapeBot/actions/workflows/build.yaml/badge.svg" alt="CI Status"/>
  </a>
  <img src="https://img.shields.io/badge/Java-25-e76f00?logo=openjdk&logoColor=e76f00" alt="Java 25"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.2-6cb52d?logo=spring" alt="Spring Boot 4.0.2"/>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-AGPL%20v3-blue.svg" alt="License: AGPL v3"/>
  </a>
</p>

---

## ✨ Features

- **User Registration** — Sign up with username and PIN
- **Login System** — Activation codes for new user verification
- **Tape Management** — Add, list, and search movie tapes
- **Filter Options** — Search by star (`/starring`) or director (`/directing`)
- **Notifications** — Optional subscription for new tape alerts
- **Admin Tools** — User management, broadcast messages
- **GDPR Compliant** — Built-in privacy policy

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 25** | Runtime |
| **Spring Boot** | Application Framework |
| **PostgreSQL** | Database |
| **Liquibase** | Database Migrations |
| **Telegram Bot API** | Long Polling Integration |
| **Lombok** | Boilerplate Reduction |
| **Testcontainers** | Integration Testing |

## 📖 Bot Commands

<details>
<summary><b>General Commands</b></summary>

| Command | Description |
|---------|-------------|
| `/register` | Register a new account |
| `/login` | Log into your account |
| `/dsgvo` | View privacy policy |
| `/me` | Show your user info |
| `/help` | Display help |
| `/reset` | Reset chat state |

</details>

<details>
<summary><b>Logged-in Commands</b></summary>

| Command | Description |
|---------|-------------|
| `/code` | Show current activation code |
| `/users` | List all registered users |
| `/add` | Add a new tape |
| `/last` | Show the last added tape |
| `/all` | List all tapes |
| `/starring` | Filter tapes by star |
| `/directing` | Filter tapes by director |
| `/subscription` | Toggle notifications |
| `/logout` | Log out |

</details>

<details>
<summary><b>Admin Commands</b></summary>

| Command | Description |
|---------|-------------|
| `/admin` | Enter admin mode |
| `/deleteuser` | Delete a user |
| `/deletetape` | Delete a tape |
| `/resetuser` | Reset user state |
| `/newadmin` | Promote user to admin |
| `/removeadmin` | Revoke admin privileges |
| `/broadcast` | Send message to all users |
| `/exit` | Exit admin mode |

</details>

## 🚀 Getting Started

### Prerequisites

- Java 25+
- PostgreSQL database
- Telegram Bot Token (via [@BotFather](https://t.me/BotFather))

### Configuration

Configure the application via environment variables or `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tapebot
    username: postgres
    password: password

better-tape-bot:
  telegram:
    token: "YOUR_BOT_TOKEN"
```

### Local Development

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

### 🐳 Docker

```bash
# Build JAR
./gradlew build

# Build Docker image
docker build -t bettertapebot .

# Run container
docker run -d \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/tapebot \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e BETTER_TAPE_BOT_TELEGRAM_TOKEN=YOUR_BOT_TOKEN \
  bettertapebot
```

## 🔄 CI/CD

The project uses GitHub Actions for automated builds. On every push to `main`:

1. ✅ Build project with Gradle
2. 🐳 Create Docker image
3. 📦 Push to GitHub Container Registry

```
ghcr.io/<username>/bettertapebot:latest
ghcr.io/<username>/bettertapebot:<version>
```

## 📊 Data Model

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    tapes    │       │    users    │       │ user_states │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id          │       │ username PK │       │ chat_id PK  │
│ title       │       │ pin         │       │ user_state  │
│ author FK   ├──────►│ is_admin    │◄──────┤ username FK │
│ star FK     ├──────►│ wants_abon. │       └─────────────┘
│ date_added  │       └─────────────┘
└─────────────┘
```

## 🧪 Testing

```bash
# Run all tests
# uses Testcontainers for PostgreSQL
# requires Docker Runtime Environment
./gradlew test
```

## 📄 License

This project is licensed under the [AGPL v3](LICENSE).

---