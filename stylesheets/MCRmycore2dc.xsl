<?xml version="1.0" encoding="UTF-8" ?>

<!--
    Document   : MCRmycore2dc.xsl
    Created on : 26. September 2002, 12:14
    Author     : gressho
    Description: Tranformation of MyCoRe metadata to DublinCore.
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet> 
