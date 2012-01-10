<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport" exclude-result-prefixes="mcrmods" version="1.0">

  <xsl:output
    method="xml"
    indent="yes"
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

</xsl:stylesheet>