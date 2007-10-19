<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	<xsl:param name="ServletsBaseURL" />
	<xsl:param name="WebApplicationBaseURL" />

	<!-- redirectURL is the position where you came from -->
	<xsl:variable name="redirectURL">
		<xsl:if test="/mcr_access_set/redirect">
			<xsl:value-of select="concat('&amp;redirect=', /mcr_access_set/redirect)" />
		</xsl:if>
	</xsl:variable>

	<xsl:variable name="servletName" select="'MCRACLEditorServlet_v2'"/>
	<xsl:variable name="editorURL" select="concat($ServletsBaseURL, $servletName)"/>
	<xsl:variable name="dataRequest" select="concat($editorURL, '?mode=dataRequest')"/>
	
	<xsl:variable name="permEditor" select="'permEditor'"/>
	<xsl:variable name="ruleEditor" select="'ruleEditor'"/>
</xsl:stylesheet>
