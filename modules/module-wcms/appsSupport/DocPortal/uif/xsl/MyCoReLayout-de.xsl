<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision$ $Date$ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink">
		
	<xsl:variable name="Layout.ProjectName" select="'DocPortal'" />
	<xsl:variable name="Layout.SearchFor" select="'Suche nach:'" />
	<xsl:variable name="Layout.MainPage" select="'Home'" />
	<xsl:variable name="Layout.Admin" select="'Admin'" />
	<xsl:variable name="Layout.AuthorDoc" select="'Dokument-Autor'" />
	<xsl:variable name="Layout.EditorDoc" select="'Dokument-Bearbeiter'" />
	<xsl:variable name="Layout.AuthorDis" select="'Diss/Hab-Autor'" />
	<xsl:variable name="Layout.EditorDis" select="'Diss/Hab-Bearbeiter'" />
	<xsl:variable name="Layout.Login" select="'Login'" />
	<xsl:variable name="Layout.Help" select="'Hilfe'" />
	<xsl:variable name="Layout.Documents" select="'Dokumenten'" />
	<xsl:variable name="Layout.Persons" select="'Personen'" />
	<xsl:variable name="Layout.Institutions" select="'Institutionen'" />
	<xsl:variable name="Layout.simple" select="'einfach'" />
	<xsl:variable name="Layout.extended" select="'erweitert'" />
	
	<xsl:variable name="CurrentLang">
		<xsl:text>de</xsl:text>
	</xsl:variable>
	
	<xsl:include href="MyCoReLayout.xsl" />
	
</xsl:stylesheet>  