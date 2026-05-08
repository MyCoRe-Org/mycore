<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrsolr="http://www.mycore.de/xslt/solr"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/functions/solr.xsl" />

  <xsl:param name="value" as="xs:string?" />

  <xsl:template match="escape-search-value">
    <result>
      <xsl:value-of select="mcrsolr:escape-search-value($value)" />
    </result>
  </xsl:template>
</xsl:stylesheet>
