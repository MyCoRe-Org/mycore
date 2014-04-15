<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:variable name="response" select="/response" />
  <xsl:template match="/">
    <xsl:apply-templates select="response/result/doc[1]" />
  </xsl:template>
  <xsl:template match="doc">
    <xsl:variable name="objId" select="str[@name='id']" />
    <xsl:apply-templates select="document(concat('mcrobject:',$objId))" mode="attachResponse" />
  </xsl:template>
  <xsl:template match="/*" mode="attachResponse">
    <xsl:copy>
      <xsl:copy-of select="@*|node()" />
      <xsl:copy-of select="$response" />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>