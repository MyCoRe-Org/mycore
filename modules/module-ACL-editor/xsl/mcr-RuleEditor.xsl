<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!--  MyCoRe - Module-Broadcasting 					-->
<!--  												-->
<!-- Module-Broadcasting 1.0, 04-2007  				-->
<!-- +++++++++++++++++++++++++++++++++++++			-->
<!--  												-->
<!-- Andreas Trappe 	- idea, concept, dev.		-->
<!--												-->
<!-- ============================================== -->

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xalan">
	<xsl:include href="MyCoReLayout.xsl" />
	<xsl:include href="editor.xsl" />
	<xsl:variable name="PageTitle" select="'Module-ACL Editor'" />

	<xsl:template match="/mcr-RuleEditor">
		<xsl:call-template name="include.editor">
			<xsl:with-param name="uri"
				select="webapp:modules/module-ACL-editor/web/editor/editor-ACL_start.xml" />
			<xsl:with-param name="ref" select="ACL-Rule-editor" />
		</xsl:call-template>
	</xsl:template>
</xsl:stylesheet>