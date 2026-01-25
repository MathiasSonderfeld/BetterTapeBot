# BetterTapeBot

Ein Telegram Bot zur gemeinschaftlichen Verwaltung und Erfassung von Filmtiteln. Benutzer können sich registrieren, einloggen und Tapes mit Titel, Autor und Star erfassen.

## Features

- **Benutzerregistrierung** mit Username und PIN
- **Login-System** mit Freischaltcode für neue Benutzer
- **Tape-Verwaltung**: Hinzufügen, Auflisten und Suchen
- **Filteroptionen**: Suche nach Star (`/starring`) oder Autor (`/directing`)
- **Benachrichtigungen**: Optionale Subscription für neue Tapes
- **Admin-Funktionen**: Benutzerverwaltung, Broadcast-Nachrichten
- **DSGVO-konform**: Integrierte Datenschutzerklärung

## Technologie-Stack

- **Java 25** mit Spring Boot 4.0
- **PostgreSQL** als Datenbank
- **Liquibase** für Datenbank-Migrationen
- **Telegram Bot API** (Long Polling)
- **Lombok** für Boilerplate-Reduktion
- **Testcontainers** für Integrationstests

## Bot-Befehle

### Allgemein
| Befehl | Beschreibung |
|--------|-------------|
| `/register` | Neuen Benutzer registrieren |
| `/login` | Einloggen |
| `/dsgvo` | Datenschutzbestimmungen anzeigen |
| `/me` | Eigene Benutzerinfo anzeigen |
| `/help` | Hilfe anzeigen |
| `/reset` | Chat-Status zurücksetzen |

### Nach Login
| Befehl | Beschreibung |
|--------|-------------|
| `/code` | Aktuellen Freischaltcode anzeigen |
| `/users` | Alle registrierten Benutzer anzeigen |
| `/add` | Neues Tape hinzufügen |
| `/last` | Letztes Tape anzeigen |
| `/all` | Alle Tapes auflisten |
| `/starring` | Tapes eines Stars filtern |
| `/directing` | Tapes eines Autors filtern |
| `/subscription` | Benachrichtigungen ein-/ausschalten |
| `/logout` | Ausloggen |

### Admin
| Befehl | Beschreibung |
|--------|-------------|
| `/admin` | Admin-Modus aktivieren |
| `/deleteuser` | Benutzer löschen |
| `/deletetape` | Tape löschen |
| `/resetuser` | Benutzer-Status zurücksetzen |
| `/newadmin` | Neuen Admin hinzufügen |
| `/removeadmin` | Admin-Rechte entziehen |
| `/broadcast` | Nachricht an alle senden |
| `/exit` | Admin-Modus verlassen |

## Setup

### Voraussetzungen

- Java 25+
- PostgreSQL Datenbank
- Telegram Bot Token (via [@BotFather](https://t.me/BotFather))

### Konfiguration

Konfiguriere die Anwendung über Umgebungsvariablen oder `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tapebot
    username: postgres
    password: password

better-tape-bot:
  telegram:
    token: "DEIN_BOT_TOKEN"
```

### Lokale Entwicklung

```bash
# Projekt bauen
./gradlew build

# Anwendung starten
./gradlew bootRun
```

### Docker

```bash
# JAR bauen
./gradlew build

# Docker Image bauen
docker build -t bettertapebot .

# Container starten
docker run -d \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/tapebot \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e BETTER_TAPE_BOT_TELEGRAM_TOKEN=DEIN_BOT_TOKEN \
  bettertapebot
```

## CI/CD

Das Projekt nutzt GitHub Actions für automatisierte Builds. Bei jedem Push auf `main` wird:

1. Das Projekt mit Gradle gebaut
2. Ein Docker Image erstellt
3. Das Image in die GitHub Container Registry gepusht

```
ghcr.io/<username>/bettertapebot:latest
ghcr.io/<username>/bettertapebot:<version>
```

## Datenmodell

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   users     │       │    tapes    │       │ user_states │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ username PK │◄──────┤ author FK   │       │ chat_id PK  │
│ pin         │◄──────┤ star FK     │       │ user_state  │
│ is_admin    │       │ id          │       │ username FK │
│ wants_abon. │       │ title       │       └─────────────┘
└─────────────┘       │ date_added  │
                      └─────────────┘
```

## Tests

```bash
# Alle Tests ausführen (nutzt Testcontainers für PostgreSQL)
./gradlew test
```

## Lizenz

Privates Projekt.
