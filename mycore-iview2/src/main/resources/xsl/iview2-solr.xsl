<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:iview2Tool="xalan://org.mycore.iview2.services.MCRIView2Tools" xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan xlink iview2Tool">
  <xsl:import href="xslImport:solr-document:iview2-solr.xsl" />

  <xsl:template match="mycorederivate">
    <xsl:apply-imports />
    <xsl:apply-templates select="derivate/internals/internal" mode="iview2" />
  </xsl:template>

  <xsl:template match="mycorederivate/derivate/internals/internal" mode="iview2">
    <xsl:if test="@maindoc and iview2Tool:isFileSupported(@maindoc)">
      <field name="iviewFile">
        <xsl:value-of select="@maindoc" />
      </field>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
