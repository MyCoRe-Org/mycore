REST-API Konfiguration
======================

Maven Build
-----------
mvn clean install -am -pl mycore-restapi -DskipTests



Einrichten des schreibenden Zugangs
-----------------------------------

- Rolle: rest-api anlegen
- Nutzer mit dieser Rolle und "normalen" Rechten neu anlegen 
  oder diese Rolle bestehenden Nutzern zuweisen.

- Client IPs setzen in Property: MCR.RestAPI.v1.Filter.Write.IPs.Pattern


