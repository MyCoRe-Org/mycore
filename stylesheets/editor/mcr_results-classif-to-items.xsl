<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2004-03-11 10:06:12 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output 
  method="xml" 
  encoding="UTF-8" 
/>

<xsl:template match="/mcr_results/mcr_result/mycoreclass/categories">
  <items>
    <xsl:apply-templates select="category" />
  </items>
</xsl:template>

<xsl:template match="category">
  <item value="{@ID}" label="{label/@text}">
    <xsl:apply-templates select="category" />
  </item>
</xsl:template>

</xsl:stylesheet>
