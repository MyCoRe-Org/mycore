<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:import href="xslImport:solr-document"/>

  <xsl:template match="/">
    <add>
      <xsl:apply-templates mode="doc"/>
    </add>
  </xsl:template>
  
  <xsl:template match="/*" mode="doc">
    <doc>
      <xsl:apply-templates select="."/>
    </doc>
  </xsl:template>


</xsl:stylesheet>
