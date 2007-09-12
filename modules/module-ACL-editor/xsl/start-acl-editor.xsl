<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">

    <xsl:template name="aclEditor.getAddress">
        <xsl:param name="objIdFilter" select="'#$#null#$#'" />
        <xsl:param name="acpoolFilter" select="'#$#null#$#'" />

        <xsl:variable name="tmpURL" select="concat($WebApplicationBaseURL, 'modules/module-ACL-editor/web/editor/editor-ACL_start.xml')" />

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
</xsl:stylesheet>