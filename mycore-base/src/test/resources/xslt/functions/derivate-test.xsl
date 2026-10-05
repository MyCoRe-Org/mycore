<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrderivate="http://www.mycore.de/xslt/derivate"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:param name="derivate-id" as="xs:string" />
  <xsl:param name="path" as="xs:string" />

  <xsl:template match="/">
    <result>
      <xsl:copy-of select="mcrderivate:get-file($derivate-id, $path)" />
    </result>
  </xsl:template>

</xsl:stylesheet>
