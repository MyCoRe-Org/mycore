<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
    <xsl:param name="ServletsBaseURL" />
    <xsl:param name="WebApplicationBaseURL" />
    <xsl:param name="RequestURL"/>
    <xsl:param name="JSessionID" />    

    <!-- redirectURL is the position where you came from -->
    <xsl:variable name="redirectURL">
        <xsl:if test="//redirect">
            <xsl:value-of select="concat('&amp;redir=', /mcr_access_set/redirect)" />
        </xsl:if>
    </xsl:variable>

    <xsl:variable name="servletName" select="'MCRACLEditorServlet_v2'"/>
    <xsl:variable name="editorURL" select="concat($ServletsBaseURL, $servletName, $JSessionID)"/>
    <xsl:variable name="dataRequest" select="concat($editorURL, '?mode=dataRequest')"/>
    <xsl:variable name="aclEditorURL" select="concat($editorURL, '?mode=getACLEditor')"/>
    
    <xsl:variable name="permEditor" select="'permEditor'"/>
    <xsl:variable name="embPermEditor" select="'embPermEditor'"/>
    <xsl:variable name="ruleEditor" select="'ruleEditor'"/>
    
    <!-- Actions for embedded editor -->
    <xsl:variable name="add" select="'add'"/>
    <xsl:variable name="edit" select="'edit'"/>
    <xsl:variable name="delete" select="'delete'"/>
    
    <xsl:template name="buildFilter">
        <xsl:param name="objIdFilter" />
        <xsl:param name="acPoolFilter" />

        <xsl:variable name="objId" select="'&amp;objid='" />
        <xsl:variable name="acPool" select="'&amp;acpool='" />

        <xsl:choose>
            <xsl:when test="($objIdFilter != '') and ($acPoolFilter != '')">
                <xsl:value-of select="concat($objId, $objIdFilter, $acPool, $acPoolFilter)" />
            </xsl:when>
            <xsl:when test="$objIdFilter != ''">
                <xsl:value-of select="concat($objId, $objIdFilter)" />
            </xsl:when>
            <xsl:when test="$acPoolFilter != ''">
                <xsl:value-of select="concat($acPool, $acPoolFilter)" />
            </xsl:when>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
