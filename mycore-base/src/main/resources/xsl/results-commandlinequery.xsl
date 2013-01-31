<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:mcr="http://www.mycore.org/"
  exclude-result-prefixes="mcr">

  <xsl:output method="text" />

  <xsl:variable name="nl">
    <xsl:text>
</xsl:text>
  </xsl:variable>
  
  <!-- Trefferliste ausgeben -->
  <xsl:template match="/mcr:results">
    <xsl:copy-of select="$nl" />
    <xsl:text>Anzahl der Treffer: </xsl:text>
    <xsl:value-of select="@numHits" />
    <xsl:copy-of select="$nl" />
    <xsl:copy-of select="$nl" />
    <xsl:apply-templates select="mcr:hit" />
    <xsl:copy-of select="$nl" />
    <xsl:copy-of select="$nl" />
  </xsl:template>

  <!-- This is a default template, see document.xsl for a sample of a custom one-->
  <xsl:template match="mcr:hit">
    <xsl:value-of select="@host" />
    <xsl:text>: </xsl:text>
    <xsl:value-of select="@id" />
    <xsl:copy-of select="$nl" />
  </xsl:template>

</xsl:stylesheet>
