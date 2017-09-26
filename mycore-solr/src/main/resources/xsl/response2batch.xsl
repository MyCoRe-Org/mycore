<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <xsl:variable name="normalResult" select="response/result/doc[str[@name='returnId'] and not(str[@name='returnId'] = preceding-sibling::doc/str[@name='returnId'])]" />
    <xsl:variable name="groupedResult" select="response/lst[@name='grouped']/lst[@name='returnId']/arr[@name='groups']/lst/str[@name='groupValue']" />
    <add>
      <xsl:apply-templates select="$normalResult|$groupedResult" />
    </add>
  </xsl:template>
  <xsl:template match="doc">
    <xsl:variable name="objId" select="str[@name='returnId']" />
    <xsl:copy-of select="document(concat('mcrobject:',$objId))" />
  </xsl:template>
  <xsl:template match="str[@name='groupValue']">
    <xsl:copy-of select="document(concat('mcrobject:',.))" />
  </xsl:template>
</xsl:stylesheet>