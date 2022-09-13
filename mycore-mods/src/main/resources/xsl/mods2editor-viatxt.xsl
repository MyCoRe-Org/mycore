<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="/">
    <raw>
      <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
      <xsl:copy-of select="." />
      <xsl:text disable-output-escaping="yes">]]</xsl:text>
      <xsl:text disable-output-escaping="yes">&gt;</xsl:text>
    </raw>
  </xsl:template>

</xsl:stylesheet>
