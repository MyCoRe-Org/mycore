<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	<xsl:include href="MyCoReLayout.xsl" />
<!--	<xsl:param name="ServletsBaseURL"/>-->
<!--    <xsl:param name="WebApplicationBaseURL"/>-->

	<xsl:variable name="PageTitle" select="'Module-ACL Editor'" />

	<xsl:template match="mcr_acl_editor">
		<xsl:copy-of select="document(concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=getPermEditor&amp;redirect=', $RequestURL))"/>	
	</xsl:template>
	
	
</xsl:stylesheet>
