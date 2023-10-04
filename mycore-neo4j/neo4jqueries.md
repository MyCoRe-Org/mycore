# Grundlegende Neo4j-Abfragen

Abfrage an `url:port/servlets/Neo4JProxyServlet?q=`

**Hinweis:** Mit diesem Servlet werden nur Leseabfragen mit Rückgabeanweisungen unterstützt.

## Knoten:

### Alle Knoten abrufen (im Browser mit einer Begrenzung von 300):

```cypher
MATCH (n) RETURN n
```

### Einen bestimmten Knoten abrufen:

```cypher
MATCH (n {id:'<MCR_TYPE_ID>'}) RETURN n
```

### Knoten nach Typ abrufen:

```cypher
MATCH (n:label) RETURN n
```

## Beziehungen:

### Alle Beziehungen abrufen (im Browser mit einer Begrenzung von 300):

```cypher
MATCH ()-[r]-() RETURN r
```

### Beziehungen nach Typ abrufen:

```cypher
MATCH ()-[r:type]-() RETURN r
```

### Beziehungen für einen bestimmten Knoten abrufen (bitte nicht verwenden):

```cypher
MATCH (n {id:'<MCR_TYPE_ID>'})-[r]-() RETURN n, r
```

**Hinweis:** Fehlende Informationen zum Zielknoten

### Beziehungen für einen bestimmten Knoten abrufen (der bessere Weg):

```cypher
MATCH (a {id:'<MCR_TYPE_ID>'})-[r]-(b) RETURN a, b, r
```

oder

```cypher
MATCH p=(a {id:'<MCR_TYPE_ID>'})-[]-() RETURN p
```

oder

```cypher
MATCH p=(a {id:'<MCR_TYPE_ID>'})--() RETURN p
```

**Hinweis:** Unabhängig von der gewählten Methode bleibt der Pfadabstand zwischen den Knoten bei 1.

### *Alle* Beziehungen in einem Graphen für einen bestimmten Knoten abrufen (vielleicht der beste Weg):

```cypher
MATCH p=({id:'<MCR_TYPE_ID>'})-[*]-() RETURN p
```
oder
```cypher
MATCH p=(a {id:'<MCR_TYPE_ID>'})-[r*]-(b) RETURN p
```
**Hinweis:** `MATCH p=({id:"MyMssPerson_agent_00004934"})-[*]-() RETURN p` in den Daten von mymss-portal

### Einen Graphen mit einem bestimmten Knoten und einer bestimmten Beziehung abrufen:

Bestimmte Beziehung am Anfang

```cypher
MATCH p=({id:"MyMssPerson_agent_00004934"})-[:agentAgRelOn_hasGrandFather]-()-[*]-() RETURN p
```
oder spezifische Beziehung irgendwo dazwischen
```cypher
MATCH p=({id:"MyMssPerson_agent_00004934"})-[*]-()-[:agentAgRelOn_hasGrandFather]-()-[*]-() RETURN p
```
oder spezifische Beziehung am Ende
```cypher
MATCH p=({id:"MyMssPerson_agent_00004934"})-[*]-()-[:agentAgRelOn_hasGrandFather]-() RETURN p
```

**Hinweis:** `MATCH p=({id:"MyMssPerson_agent_00004934"})-[*]-()-[:agentAgRelOn_hasGrandFather]-()-[*]-() RETURN p` in den Daten von mymss-portal

### Von einem Knoten ausgehend alle Kanten bis maximal Tiefe n anzeigen
```cypher
MATCH p=({id:"MyMssPerson_agent_00000315"})-[*1..n]-()
return p
```

### Von einem Knoten ausgehend alle Kanten bis maximal Tiefe n anzeigen und zusätzlich Kanten zwischen den verbundenen Knoten
```cypher
MATCH n=({id:"MyMssPerson_agent_00002627"})-[*1..n]-(m)
optional match (m)-[p]-()
return n,p
```

### Den kürzesten Weg zwischen zwei Knoten finden

```cypher
MATCH (a {id:"MyMssPerson_agent_00011629"}),(b {id:"MyMssPerson_agent_00006458"}),
p = shortestPath((a)-[*]-(b)) 
RETURN p
```

```cypher
match p=shortestPath((a)-[*]-(b)) where a.id="MyMssPerson_agent_00011629" and b.id="MyMssPerson_agent_00006458" return p
```

### Kürzester Weg zwischen zwei Knoten, ohne einen bestimmten Knoten zu passieren (brauchen wir im Qalamos z.B. um Verfasser: unbekannt auszuschließen)

```cypher
match p=shortestPath((a)-[*]-(b)) 
where 
    a.id="MyMssPerson_agent_00011629" and 
    b.id="MyMssPerson_agent_00006458" and
    NONE(n in nodes(p) where n.id = "MyMssPerson_agent_00000120")
return p
```

## Sonstiges:

### Alle eindeutigen Knotenlabels abrufen:

```cypher
MATCH (n) RETURN distinct labels(n)
```

### Die Anzahl der Knoten für jedes Label abrufen:

```cypher
MATCH (n) RETURN distinct labels(n), count(*)
```

### Alle eindeutigen Beziehungstypen abrufen:

```cypher
MATCH ()-[r]-() RETURN distinct type(r)
```

### Die Anzahl der Beziehungen für jeden Typ abrufen:

```cypher
MATCH ()-[r]-() RETURN distinct type(r), count(*)
```
