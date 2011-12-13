<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:mediaTools="xalan:///org.mycore.media.services.MCRMediaIFSTools" exclude-result-prefixes="xsl xalan mediaTools">

    <xsl:param name="WebApplicationBaseURL" />

    <xsl:template name="media.hasThumbnail">
        <xsl:param name="derivate" />
        <xsl:param name="imagePath" />
        <xsl:value-of select="mediaTools:hasThumbnailInStore($derivate, $imagePath)" />
    </xsl:template>

    <xsl:template name="media.getThumbnail">
        <xsl:param name="derivate" />
        <xsl:param name="imagePath" />
        <xsl:param name="style" select="''" />
        <xsl:param name="class" select="''" />
        <img src="{concat($WebApplicationBaseURL,'servlets/MCRMediaThumbnailServlet/',$derivate,$imagePath)}" style="{$style}"
            class="{$class}" />
    </xsl:template>
</xsl:stylesheet>