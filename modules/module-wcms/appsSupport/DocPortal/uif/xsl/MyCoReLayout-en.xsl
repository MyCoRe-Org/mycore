<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision$ $Date$ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink">
		
	<xsl:variable name="Layout.ProjectName" select="'Sample'" />
	<xsl:variable name="Layout.SearchFor" select="'Search for:'" />
	<xsl:variable name="Layout.MainPage" select="'Home'" />
	<xsl:variable name="Layout.Admin" select="'Admin'" />
	<xsl:variable name="Layout.AuthorDoc" select="'Document-Author'" />
	<xsl:variable name="Layout.EditorDoc" select="'Document-Editor'" />
	<xsl:variable name="Layout.AuthorDis" select="'Diss/Hab-Author'" />
	<xsl:variable name="Layout.EditorDis" select="'Diss/Hab-Editor'" />
	<xsl:variable name="Layout.Login" select="'Login'" />
	<xsl:variable name="Layout.Help" select="'Help'" />
	<xsl:variable name="Layout.Documents" select="'Documents'" />
	<xsl:variable name="Layout.Persons" select="'Persons'" />
	<xsl:variable name="Layout.Institutions" select="'Institutions'" />
	<xsl:variable name="Layout.simple" select="'simple'" />
	<xsl:variable name="Layout.extended" select="'extended'" />
	
	<xsl:variable name="CurrentLang">
		<xsl:text>en</xsl:text>
	</xsl:variable>
	
	<xsl:include href="MyCoReLayout.xsl" />
	
</xsl:stylesheet>