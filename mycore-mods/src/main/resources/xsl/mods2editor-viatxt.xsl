<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport" exclude-result-prefixes="mcrmods" version="1.0">

  <xsl:output
    method="xml"
    encoding="UTF-8" />

  <xsl:template match="/">
    <mycoreobject>
      <xsl:apply-templates />
    </mycoreobject>
  </xsl:template>

  <xsl:template match="*">
    <xsl:text>&lt;</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:if test="name()='mods'">
      <xsl:text> xmlns="http://www.loc.gov/mods/v3"</xsl:text>
    </xsl:if>
    <xsl:for-each select="@*">
      <xsl:value-of select="' '" />
      <xsl:value-of select="name()" />
      <xsl:value-of select="'='" />
      <xsl:text>"</xsl:text>
      <xsl:value-of select="." />
      <xsl:text>"</xsl:text>
    </xsl:for-each>
    <xsl:text>&gt;</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>&lt;/</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:text>&gt;</xsl:text>
  </xsl:template>
  
  <xsl:template match="text()">
    <xsl:call-template name="escape-xml">
      <xsl:with-param name="text" select="."/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="escape-xml">
    <xsl:param name="text" />
    <xsl:if test="$text != ''">
      <xsl:variable name="head" select="substring($text, 1, 1)" />
      <xsl:variable name="tail" select="substring($text, 2)" />
      <xsl:choose>
        <xsl:when test="$head = '&amp;'">&amp;amp;amp;amp;</xsl:when>
        <xsl:when test="$head = '&lt;'">&amp;amp;amp;lt;</xsl:when>
        <xsl:when test="$head = '&gt;'">&amp;amp;amp;gt;</xsl:when>
        <xsl:when test="$head = '&quot;'">&amp;amp;amp;quot;</xsl:when>
        <xsl:when test="$head = &quot;&apos;&quot;">&amp;amp;amp;apos;</xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$head" />
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="escape-xml">
        <xsl:with-param name="text" select="$tail" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>