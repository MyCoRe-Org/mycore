<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrobject="http://www.mycore.de/xslt/object"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:param name="id" as="xs:string" />

  <xsl:template match="/">
    <result>
      <xsl:copy-of select="mcrobject:get($id)" />
    </result>
  </xsl:template>

</xsl:stylesheet>
