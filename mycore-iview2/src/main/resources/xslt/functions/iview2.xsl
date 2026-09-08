<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcriview2="http://www.mycore.de/xslt/iview2"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

    <xsl:function name="mcriview2:is-completely-tiled" as="xs:boolean">
        <xsl:param name="derivID" as="xs:string"/>
        <xsl:variable name="isCompletelyTiled" select="document(concat('iview2:isCompletelyTiled:', $derivID))" />
        <xsl:value-of select="count($isCompletelyTiled/true)&gt;0" />
    </xsl:function>

    <!-- Checks availability only; IIIF enforces permissions when serving the image. -->
    <xsl:function name="mcriview2:has-tiles" as="xs:boolean">
        <xsl:param name="derivateID" as="xs:string"/>
        <xsl:param name="path" as="xs:string"/>
        <xsl:sequence select="exists(document(concat('iview2:hasTiles:', $derivateID, '/',
            encode-for-uri($path)))/true)"/>
    </xsl:function>

</xsl:stylesheet>
