<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:import href="xslImport:solr-document:iview2-solr.xsl"/>

  <xsl:template match="mycorederivate">
    <xsl:apply-imports/>
    <xsl:apply-templates select="derivate/internals/internal" mode="iview2"/>
  </xsl:template>

  <xsl:template match="mycorederivate/derivate/internals/internal" mode="iview2">
    <xsl:if test="@maindoc and count(document(concat('iview2:isFileSupported:', @maindoc))/true)&gt;0">
      <field name="iviewFile">
        <xsl:value-of select="@maindoc"/>
      </field>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
