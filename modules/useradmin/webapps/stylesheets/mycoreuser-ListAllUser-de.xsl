<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- +
     | This stylesheet provides the language dependent values (german language) for
     | the presentation of the data of the list off all mycore users. It is called by the
     | MCRUserAdminServlet in the "ListAllUser"-mode by forwarding an XML representation of
     | a user object to the LayoutServlet. After defining the language dependent
     | values this stylesheet finally includes the stylesheet mycoreuser-ListAlluser.xsl.
     |
     | Author: Anja Schaar
     | Last changes: 2005-20-06
     + -->

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink">

<xsl:variable name="Userdata.title"              select="'Liste aller Nutzer'" />
<xsl:variable name="Userdata.numID"              select="'Numerische ID'" />
<xsl:variable name="Userdata.ID"                 select="'Nutzerkennzeichen'" />
<xsl:variable name="Userdata.enabled"            select="'Status'" />
<xsl:variable name="Userdata.state_enabled"      select="'Akiv'" />
<xsl:variable name="Userdata.state_disabled"     select="'inAktiv'" />
<xsl:variable name="Userdata.primary_group"      select="'Primäre Gruppe'" />
<xsl:variable name="Userdata.firstname"          select="'Vorname'" />
<xsl:variable name="Userdata.lastname"           select="'Nachname'" />
<xsl:variable name="Userdata.institution"        select="'Institution'" />
<xsl:variable name="Userdata.faculty"            select="'Fakultät'" />
<xsl:variable name="Userdata.edit"               select="'Bearbeiten'" />

<xsl:variable name="MainTitle" select="'@libri'"/>
<xsl:variable name="PageTitle" select="$Userdata.title"/>

<xsl:variable name="Servlet" select="'UserAdminServlet'"/>

<xsl:include href="MyCoReLayout-de.xsl" />
<xsl:include href="mycoreuser-ListAllUser.xsl" />

</xsl:stylesheet>
