<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2004-03-11 14:50:35 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output 
  method="xml" 
  encoding="UTF-8" 
/>

<xsl:template match="/">
  <items>
    <xsl:apply-templates select="mcr_results/mcr_result/mycoreclass/categories/category" />
  </items>
</xsl:template>

<xsl:template match="category">
  <item value="{@ID}">
    <xsl:apply-templates select="label"    />
    <xsl:apply-templates select="category" />
  </item>
</xsl:template>

<xsl:template match="label">
  <label>
    <xsl:copy-of select="@xml:lang" />
    <xsl:value-of select="@text" />
  </label>
</xsl:template>

</xsl:stylesheet>
