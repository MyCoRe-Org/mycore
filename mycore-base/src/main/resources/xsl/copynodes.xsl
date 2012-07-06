<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:template match='@*|node()'>
    <xsl:param name="ID" />
    <!-- default template: just copy -->
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
      <xsl:if test="$ID">
        <xsl:attribute name="ID">
          <xsl:value-of select="$ID" />
        </xsl:attribute>
      </xsl:if>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>