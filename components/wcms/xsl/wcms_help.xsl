<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- =====================================================================================
========================================================================================={

title: wcms_choose.xsl

Hilfeseite zum wcms.

template:
	- wcmsHelp

}=========================================================================================
====================================================================================== -->

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan" >

<!-- ====================================================================================={

section: Template: name="wcmsHelp"

	- Hilfeseite

}===================================================================================== -->

	<xsl:template name="wcmsHelp">
		<!-- Menueleiste einblenden, Parameter = ausgewaehlter Menuepunkt -->
		<xsl:call-template name="menuleiste">
			<xsl:with-param name="menupunkt" select="'Hilfe'" />
		</xsl:call-template>
	</xsl:template>

<!-- =================================================================================== -->

</xsl:stylesheet>