<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="/">
    <xsl:copy>
      <xsl:value-of select="raw/text()" disable-output-escaping="yes" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>