# Neo4j Einrichtungsanleitung

Diese Anleitung führt Sie durch den Prozess der Einrichtung von Neo4j, entweder als direkte Installation oder mit Docker.

## Voraussetzungen

Stellen Sie sicher, dass Sie die folgenden Voraussetzungen erfüllen, bevor Sie mit der Einrichtung von Neo4j beginnen:

- Ein unterstütztes Betriebssystem (Windows, macOS oder Linux)
- Java Development Kit (JDK) Version 17 oder höher installiert
- Ausreichend Arbeitsspeicher (empfohlen: mindestens 4 GB)
- Ausreichend freier Festplattenspeicher (empfohlen: mindestens 10 GB)

## Direkte Installation

Folgen Sie diesen Schritten, um Neo4j direkt auf Ihrem System zu installieren:

1. Gehen Sie zur offiziellen Neo4j-Website: [https://neo4j.com/](https://neo4j.com/download-center/?ref=subscription#community)

2. Klicken Sie auf den "pricing" um zur Downloadseite zu gelangen.

3. Wählen Sie "Graph Database", dann "Self-Managed" und klicken Sie bei "Community Edition" auf "Download for Free".

4. Wählen Sie die gewünschte Version von Neo4j aus und laden Sie sie herunter, als Empfehlung wählen Sie eine Version >5.0.

5. Extrahieren Sie das heruntergeladene Archiv an einem Ort Ihrer Wahl.

6. Öffnen Sie das extrahierte Verzeichnis und navigieren Sie zum Unterordner "bin".

7. Starten Sie den Neo4j-Server, indem Sie die entsprechende Befehlszeile für Ihr Betriebssystem verwenden:
    - Windows: Führen Sie `neo4j.bat console` aus.
    - macOS/Linux: Führen Sie `./neo4j console` aus.

8. Öffnen Sie Ihren Webbrowser und geben Sie `http://localhost:7474/` ein, um auf das Neo4j-Browser-Interface zuzugreifen.

9. Befolgen Sie die Anweisungen auf dem Bildschirm, um das Standardkennwort zu ändern und mit der Konfiguration Ihrer Neo4j-Datenbank zu beginnen.

## Einrichtung mit Docker

Folgen Sie diesen Schritten, um Neo4j mit Docker einzurichten:

1. Stellen Sie sicher, dass Docker auf Ihrem System installiert ist. Informationen zur Installation finden Sie auf der offiziellen Docker-Website: [https://www.docker.com/](https://www.docker.com/)

2. Öffnen Sie ein Terminal oder eine Befehlszeile.

3. Führen Sie den folgenden Befehl aus, um das Neo4j-Docker-Image herunterzuladen:

   ```
   docker pull neo4j
   ```

4. Nachdem der Download abgeschlossen ist, führen Sie den folgenden Befehl aus, um einen Neo4j-Container zu erstellen und auszuführen:

   ```
   docker run --name my-neo4j -p 7474:7474 -p 7687:7687 -d -v $HOME/neo4j/data:/data -v $HOME/neo4j/logs:/logs neo4j
   ```

   Dieser Befehl erstellt einen Container mit dem Namen "my-neo4j", bindet die Ports 7474 und 7687 an Ihren Host, erstellt Volumen für Daten und Protokolle und verwendet das offizielle Neo4j-Docker-Image.

5. Öffnen Sie Ihren Webbrowser und geben Sie `http://localhost:7474/` ein, um auf das Neo4j-Browser-Interface zuzugreifen.

6. Befolgen Sie die Anweisungen auf dem Bildschirm, um das Standardkennwort zu ändern und mit der Konfiguration Ihrer Neo4j-Datenbank zu beginnen.

## Verbindung zur Neo4j-Datenbank

Um sich mit Ihrer Neo4j-Datenbank zu verbinden, verwenden Sie die offizielle Neo4j-Treiberbibliothek in Ihrer bevorzugten Programmiersprache. Informationen zur Verwendung des Treibers finden Sie in der Neo4j-Dokumentation: [https://neo4j.com/developer/get-started/](https://neo4j.com/developer/get-started/)

Stellen Sie sicher, dass Sie die erforderlichen Verbindungsparameter wie Host, Port, Benutzername und Kennwort entsprechend konfigurieren, um erfolgreich eine Verbindung zur Datenbank herzustellen.

**Hinweis:** Die Standardwerte für Host und Port sind `localhost:7687` mit User neo4j und Passwort neo4j

## Weitere Ressourcen

- Offizielle Neo4j-Dokumentation: [https://neo4j.com/docs/](https://neo4j.com/docs/)
- Neo4j Community-Support: [https://community.neo4j.com/](https://community.neo4j.com/)

**Hinweis:** Stellen Sie sicher, dass Sie die entsprechende Dokumentation für Ihre spezifische Version von Neo4j konsultieren, um genaue Informationen und Anleitungen zu erhalten.


# Neo4J MCR-Modul

Das Neo4J MCR-Modul stellt eine Zwischentechnologie, für die Verbindung von Neo4j mit dem MyCore Framework dar.

## Voraussetzung

Stellen Sie sicher, dass Sie die folgenden Voraussetzungen erfüllen, bevor Sie mit der Einrichtung von Neo4j beginnen:

- Ein unterstütztes Betriebssystem (Windows, macOS oder Linux)
- Java Development Kit (JDK) Version 17 oder höher installiert
- Neo4j ist auf dem System oder einem erreichbaren System installiert

### Maven

     <dependency>
      <groupId>de.uni-leipzig.urz</groupId>
      <artifactId>dptbase-neo4j</artifactId>
     </dependency>

### Konfiguration

Eine Konfiguration erfolgt über die Datei `mycore.properties` direkt im Modul. In der `mycore.private.properties` Datei können die Werte überschrieben werden, darin sind auch folgende Eigenschaften zu setzen:
- MCR.Neo4J.ServerURL=bolt://localhost:7687
- MCR.Neo4J.user=\<NutzerName>
- MCR.Neo4J.password=\<Passwort>
- MCR.Neo4J.colors=\<objectType>:\<colorHexValue>;\<objectType>:\<colorHexValue>;\<objectType>:\<colorHexValue>

Nutzername und Passwort sollte der im Neo4j verwendeten Konfiguration entsprechen.

Farben für Knoten können mittels `MCR.Neo4J.colors` definiert werden. Dabei werden der Objekttyp (z.B. `agent`) und die Farbe als Hexadezimalwert (z.B. `ff0000`) ohne `#`, getrennt durch einen Doppelpunkt, angegeben. Mehrere solcher Objekttyp-Farbe-Paare werden jeweils durch ein Semikolon getrennt, z.B. `agent:ff0000;edition:00ff00;work:00ffff`.

# Grundlegende Cypher-Abfragen

Hier sind einige grundlegende Cypher-Abfragen, für einen einfachen Einstieg in die Abfragesprache für Neo4j.


## Erstellen von Knoten und Beziehungen

Um Knoten in Neo4j zu erstellen, verwenden Sie die `CREATE`-Klausel, gefolgt von der Angabe des Knotenlabels und der Eigenschaften.

```cypher
CREATE (:Label {property1: value1, property2: value2})
```
Um sicherzustellen, dass Knoten nicht dupliziert werden, können Sie die `MERGE`-Klausel mit der `ON CREATE`-Klausel verwenden. Wenn der zu erstellende Knoten bereits existiert, wird er ausgewählt und keine Änderungen vorgenommen.

```cypher
MERGE (:Label {property: value})
ON CREATE SET node.newProperty = newValue
```

Um Beziehungen zwischen Knoten zu erstellen, verwenden Sie die `CREATE`-Klausel mit dem entsprechenden Beziehungstyp und den Quell- und Zielknoten.

```cypher
CREATE (node1)-[:RELATIONSHIP_TYPE]->(node2)
```
Um Beziehungen zwischen Knoten zu erstellen, verwenden Sie die `MERGE`-Klausel mit dem entsprechenden Beziehungstyp und den Quell- und Zielknoten. Die `ON CREATE`-Klausel kann verwendet werden, um Eigenschaften der Beziehung festzulegen, wenn sie neu erstellt wird.

```cypher
MATCH (node1:Label1), (node2:Label2)
MERGE (node1)-[:RELATIONSHIP_TYPE]->(node2)
ON CREATE SET node1.newProperty = newValue
```


## Abfragen von Knoten und Beziehungen

Um alle Knoten einer bestimmten Art abzufragen, verwenden Sie die `MATCH`-Klausel mit dem Knotenlabel.

```cypher
MATCH (node:Label)
RETURN node
```

Um alle Beziehungen zwischen Knoten abzufragen, verwenden Sie die `MATCH`-Klausel mit dem Beziehungstyp.

```cypher
MATCH ()-[relationship:RELATIONSHIP_TYPE]->()
RETURN relationship
```

## Filtern von Ergebnissen

Um bestimmte Knoten basierend auf Eigenschaften zu filtern, verwenden Sie die `WHERE`-Klausel.

```cypher
MATCH (node:Label)
WHERE node.property = value
RETURN node
```

## Auswählen spezifischer Eigenschaften

Um nur bestimmte Eigenschaften von Knoten oder Beziehungen abzurufen, verwenden Sie die `RETURN`-Klausel und geben Sie die gewünschten Eigenschaften an.

```cypher
MATCH (node:Label)
RETURN node.property1, node.property2
```

## Sortieren von Ergebnissen

Um die Ergebnisse nach einer bestimmten Eigenschaft zu sortieren, verwenden Sie die `ORDER BY`-Klausel.

```cypher
MATCH (node:Label)
RETURN node
ORDER BY node.property ASC
```

## Begrenzen der Ergebnismenge

Um die Anzahl der zurückgegebenen Ergebnisse zu begrenzen, verwenden Sie die `LIMIT`-Klausel.

```cypher
MATCH (node:Label)
RETURN node
LIMIT 10
```

## Bearbeitung von Knoten

Um einen Knoten zu bearbeiten, verwenden Sie die `MERGE`-Klausel zusammen mit der `ON MATCH`-Klausel. In der `ON MATCH`-Klausel können Sie die zu aktualisierenden Eigenschaften angeben.

```cypher
MERGE (node:Label {property: value})
ON MATCH SET node.updatedProperty = updatedValue
```

Dieser Befehl sucht nach einem Knoten mit dem angegebenen Label und der Eigenschaft und aktualisiert seine `updatedProperty`, wenn der Knoten vorhanden ist. Wenn der Knoten nicht vorhanden ist, wird er erstellt.

## Bearbeitung von Beziehungen

Um eine Beziehung zu bearbeiten, verwenden Sie die `MERGE`-Klausel zusammen mit der `ON MATCH`-Klausel. In der `ON MATCH`-Klausel können Sie die zu aktualisierenden Eigenschaften der Beziehung angeben.

```cypher
MATCH (node1:Label1), (node2:Label2)
MERGE (node1)-[relationship:RELATIONSHIP_TYPE]->(node2)
ON MATCH SET relationship.updatedProperty = updatedValue
```

Dieser Befehl sucht nach einer Beziehung des angegebenen Typs zwischen den Knoten `node1` und `node2` und aktualisiert die `updatedProperty` der Beziehung, wenn sie vorhanden ist. Wenn die Beziehung nicht vorhanden ist, wird sie erstellt.


Dies sind nur grundlegende Beispiele für Cypher-Abfragen. Neo4j bietet eine umfangreiche Abfragesprache, mit der Sie komplexe Abfragen und Analysen durchführen können. Weitere Informationen zu fortgeschrittenen Cypher-Abfragen finden Sie in der offiziellen Neo4j-Dokumentation.

## Weitere Ressourcen

- Offizielle Neo4j-Dokumentation zu Cypher: [https://neo4j.com/docs/cypher-manual/current/](https://neo4j.com/docs/cypher-manual/current/)
- Neo4j Cheat Sheet: [https://neo4j.com/docs/cypher-refcard/current/](https://neo4j.com/docs/cypher-refcard/current/)

