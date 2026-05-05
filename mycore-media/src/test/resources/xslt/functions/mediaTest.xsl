<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrmedia="http://www.mycore.de/xslt/media"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/functions/media.xsl" />

  <xsl:param name="derivateId" as="xs:string" />
  <xsl:param name="path" as="xs:string" />
  <xsl:param name="userAgent" as="xs:string?" />

  <xsl:template match="get-sources">
    <result>
      <xsl:copy-of select="mcrmedia:get-sources($derivateId, $path, $userAgent)" />
    </result>
  </xsl:template>
</xsl:stylesheet>
