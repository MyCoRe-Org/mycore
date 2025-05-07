<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!-- standard copy template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
  <xsl:template match="entry[@uri]">
    <xsl:apply-templates />
    <xsl:if test="not(*)">
      <xsl:apply-templates select="document(@uri)" />
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>