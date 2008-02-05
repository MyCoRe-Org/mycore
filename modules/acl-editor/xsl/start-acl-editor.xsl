<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">

	<!-- 
		see mcr_acl_editor_common.xsl for definition of following variables
		
		redirectURL
		servletName
		editorURL
		aclEditorURL
		dataRequest
		permEditor
		ruleEditor
		
		add
		edit
		delete
	-->

	<xsl:include href="mcr_acl_editor_common.xsl" />

    <xsl:template name="aclEditor.getAddress">
        <xsl:param name="objIdFilter" select="'#$#null#$#'" />
        <xsl:param name="acpoolFilter" select="'#$#null#$#'" />

        <xsl:variable name="tmpURL" select="concat($WebApplicationBaseURL, 'servlets/MCRACLEditorServlet_v2?mode=getACLEditor')" />

        <xsl:choose>
            <xsl:when test="$objIdFilter='#$#null#$#' and $acpoolFilter='#$#null#$#'">
                <xsl:value-of select="$tmpURL" />
            </xsl:when>
            <xsl:when test="$objIdFilter!='#$#null#$#' and $acpoolFilter!='#$#null#$#'">
                <xsl:value-of select="concat($tmpURL, '?objid=', $objIdFilter,'&amp;acpool=', $acpoolFilter)" />
            </xsl:when>
            <xsl:when test="$objIdFilter!='#$#null#$#'">
                <xsl:value-of select="concat($tmpURL, '?objid=', $objIdFilter)" />
            </xsl:when>
            <xsl:when test="$acpoolFilter!='#$#null#$#'">
                <xsl:value-of select="concat($tmpURL, '?acpool=', $acpoolFilter)" />
            </xsl:when>

        </xsl:choose>

    </xsl:template>
    
    <xsl:template name="aclEditor.embMapping.getAddress">
		<xsl:param name="objId" />
		<xsl:param name="permission" />
		<xsl:param name="action" />

		<xsl:variable name="filter">
			<xsl:call-template name="buildFilter">
				<xsl:with-param name="objIdFilter" select="$objId" />
				<xsl:with-param name="acPoolFilter" select="$permission" />
			</xsl:call-template>
		</xsl:variable>

		<xsl:value-of select="concat($aclEditorURL, '&amp;editor=embPermEditor&amp;cmd=', $action, '&amp;redir=', $RequestURL, $filter)" />
<!--		<xsl:value-of select="concat($dataRequest, '&amp;action=getPermEditor&amp;emb=true&amp;cmd=', $action, $filter)" />-->

	</xsl:template>
</xsl:stylesheet>