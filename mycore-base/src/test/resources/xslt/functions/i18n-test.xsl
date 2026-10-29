<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/functions/i18n.xsl"/>

  <xsl:param name="key" as="xs:string"/>
  <xsl:param name="arg0" as="xs:string"/>
  <xsl:param name="arg1" as="xs:string"/>

  <xsl:template match="test-translate-with-params">
    <result>
      <xsl:value-of select="mcri18n:translate-with-params($key, ($arg0, $arg1))"/>
    </result>
  </xsl:template>

</xsl:stylesheet>
