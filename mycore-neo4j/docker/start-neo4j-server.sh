#!/bin/bash

docker start neo4j || docker run -d --name neo4j -p 7474:7474 -p 7687:7687 -p 7473:7473 neo4j:5.1.0-community
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' neo4j