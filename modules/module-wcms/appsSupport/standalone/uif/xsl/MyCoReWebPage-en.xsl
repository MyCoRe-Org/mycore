<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2004-12-28 23:38:21 $ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink" >
	<xsl:variable name="EmptyWorkflow">
		<xsl:text>The workflow is empty.</xsl:text>
	</xsl:variable>
	<xsl:variable name="CurrentLang">
		<xsl:text>en</xsl:text>
	</xsl:variable>
<!--
		<xsl:include href="MyCoReLayout-en.xsl" />
-->		
	<xsl:include href="MyCoReWebPage.xsl" />
</xsl:stylesheet>