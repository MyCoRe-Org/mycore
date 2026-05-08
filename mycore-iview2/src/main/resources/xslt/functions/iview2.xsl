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

</xsl:stylesheet>
