<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2006-02-06 13:30:49 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output 
  method="xml" 
  encoding="UTF-8" 
/>

<xsl:param name="CurrentLang"/>

<xsl:template match="/">
  <items>
    <xsl:apply-templates select="mcr_results/mcr_result/mycoreclass/categories" />
  </items>
</xsl:template>

<xsl:template match="categories">
  <xsl:for-each select="category">
  <xsl:sort select="label[lang($CurrentLang)]/@text" case-order="upper-first" />
  <item value="{@ID}">
    <xsl:apply-templates select="label"    />
    <xsl:apply-templates select="category" />
  </item>
  </xsl:for-each>
</xsl:template>

<xsl:template match="category">
  <xsl:for-each select=".">
  <xsl:sort select="label[lang($CurrentLang)]/@text" case-order="upper-first" />
  <item value="{@ID}">
    <xsl:apply-templates select="label"    />
    <xsl:apply-templates select="category" />
  </item>
  </xsl:for-each>
</xsl:template>

<xsl:template match="label">
  <label>
    <xsl:copy-of select="@xml:lang" />
    <xsl:value-of select="@text" />
  </label>
</xsl:template>

</xsl:stylesheet>
