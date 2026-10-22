<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:param name="terminate" select="'no'" />
  <xsl:template match="/">
    <xsl:message>Hello from xsl:message</xsl:message>
    <xsl:if test="$terminate = 'yes'">
      <xsl:message terminate="yes">No host present!</xsl:message>
    </xsl:if>
    <xsl:if test="$terminate = 'empty'">
      <xsl:message terminate="yes" />
    </xsl:if>
    <result />
  </xsl:template>
</xsl:stylesheet>
