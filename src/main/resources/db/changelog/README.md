# Datenbank Migrationen mit Liquibase

## Was sind Datenbank Migrationen?

Datenbank Migrationen sind versionierte Änderungen am Datenbankschema (Tabellen, Spalten, Indizes, etc.). Sie ermöglichen es, Schemaänderungen nachvollziehbar, wiederholbar und automatisiert über verschiedene Umgebungen (Dev, Test, Prod) hinweg auszurollen.

**Warum Migrationen?**
- Versionskontrolle für die Datenbankstruktur
- Reproduzierbare Deployments
- Rollback-Möglichkeiten
- Nachvollziehbare Historie aller Schemaänderungen

## Wie funktioniert Liquibase?

Liquibase trackt in der Tabelle `DATABASECHANGELOG`, welche Änderungen (ChangeSets) bereits ausgeführt wurden. Jedes ChangeSet wird eindeutig über `id`, `author` und `filepath` identifiziert und nur einmal ausgeführt.

## Struktur unserer Migrationen

```
db/
├── db.changelog-master.yaml          # Master-Datei, die alle Migrationen included
├── db.changelog-create-organizations.yaml
├── db.changelog-create-users.yaml
├── db.changelog-add-user-state-index.yaml
└── ...
```

Die `db.changelog-master.yaml` enthält alle includes in der richtigen Reihenfolge:

```yaml
databaseChangeLog:
  - include:
      file: db.changelog-create-organizations.yaml
  - include:
      file: db.changelog-create-users.yaml
  # ... weitere Migrationen
```

## Eine neue Migration hinzufügen

### 1. Neue Changelog-Datei erstellen

Erstelle eine neue Datei z.B. `db.changelog-add-email-index.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: add-index-users-email
      author: dein-name
      changes:
        - createIndex:
            indexName: idx_users_email
            tableName: users
            columns:
              - column:
                  name: email
```

### 2. In master.yaml includen

Füge die neue Datei am **Ende** der `db.changelog-master.yaml` hinzu:

```yaml
databaseChangeLog:
  # ... bestehende includes
  - include:
      file: db.changelog-add-email-index.yaml
      relativeToChangelogFile: true
```

### 3. Migration ausführen

Die Migration wird beim nächsten Deployment automatisch ausgeführt. Lokal kannst du sie mit dem entsprechenden Liquibase-Command testen.

## Wichtige Regeln

1. **Niemals bestehende ChangeSets ändern** - Erstelle stattdessen ein neues ChangeSet für Korrekturen
2. **Eindeutige IDs** - Jedes ChangeSet braucht eine unique ID über alle Migrationen hinweg
3. **Reihenfolge beachten** - Tabellen müssen vor ihren Foreign Keys existieren
4. **Tabelle + Constraints zusammen** - Eine Tabelle und ihre Constraints gehören in eine Datei
5. **Sprechende Namen** - Dateinamen wie `db.changelog-add-user-state-index.yaml` sind selbsterklärend

## Häufige Operationen

### Tabelle erstellen
```yaml
- createTable:
    tableName: my_table
    columns:
      - column:
          name: id
          type: bigint
          autoIncrement: true
      - column:
          name: name
          type: varchar(255)
          constraints:
            nullable: false
```

### Index erstellen
```yaml
- createIndex:
    indexName: idx_table_column
    tableName: my_table
    columns:
      - column:
          name: my_column
```

### Spalte hinzufügen
```yaml
- addColumn:
    tableName: my_table
    columns:
      - column:
          name: new_column
          type: varchar(100)
```

### Foreign Key hinzufügen
```yaml
- addForeignKeyConstraint:
    baseTableName: users
    baseColumnNames: organization_id
    referencedTableName: organizations
    referencedColumnNames: id
    constraintName: fk_users_organization
```

## Weiterführende Ressourcen

- **Liquibase Dokumentation**: https://docs.liquibase.com/
- **Konzept Datenbank Migrationen**: https://www.martinfowler.com/articles/evodb.html (Martin Fowler - Evolutionary Database Design)
- **Best Practices**: https://docs.liquibase.com/concepts/bestpractices.html

## Troubleshooting

**Problem: Migration wird nicht ausgeführt**
- Prüfe, ob das ChangeSet bereits in `DATABASECHANGELOG` existiert
- Checke die Reihenfolge der includes in der master.yaml

**Problem: Foreign Key Fehler**
- Stelle sicher, dass die referenzierte Tabelle vorher erstellt wird
- Prüfe die Reihenfolge der includes

**Problem: Checksummen-Fehler**
- Ein bestehendes ChangeSet wurde geändert (nicht erlaubt!)
- Lösung: Änderung rückgängig machen und neues ChangeSet erstellen