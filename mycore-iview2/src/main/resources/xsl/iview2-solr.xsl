<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
  xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan xlink iview2">
  <xsl:import href="xslImport:solr-document:iview2-solr.xsl" />

  <xsl:template match="mycorederivate">
    <xsl:apply-imports />
    <xsl:message>iview2-solr.xsl imported</xsl:message>
    <xsl:variable name="iviewMainFile" select="iview2:getSupportedMainFile(@ID)" />
    <xsl:if test="string-length($iviewMainFile) &gt; 0">
      <xsl:message>detected supported iview file</xsl:message>
      <field name="iviewFile">
        <xsl:value-of select="concat(@xlink:href,$iviewMainFile)" />
      </field>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>