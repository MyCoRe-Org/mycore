<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcriview2="http://www.mycore.de/xslt/iview2"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/functions/iview2.xsl" />

  <xsl:param name="derivateId" as="xs:string" />
  <xsl:param name="path" as="xs:string" />

  <xsl:template match="has-tiles">
    <result>
      <xsl:value-of select="mcriview2:has-tiles($derivateId, $path)" />
    </result>
  </xsl:template>
</xsl:stylesheet>
