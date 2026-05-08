<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrurl="http://www.mycore.de/xslt/url"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:param name="url" as="xs:string?" />
  <xsl:param name="par" as="xs:string?" />
  <xsl:param name="value" as="xs:string?" />
  <xsl:param name="fn-name" as="xs:string" />

  <xsl:template match="/">
    <result>
      <xsl:choose>
        <xsl:when test="$fn-name = 'get-param'">
          <xsl:value-of select="mcrurl:get-param($url, $par)" />
        </xsl:when>
        <xsl:when test="$fn-name = 'set-param'">
          <xsl:value-of select="mcrurl:set-param($url, $par, $value)" />
        </xsl:when>
        <xsl:when test="$fn-name = 'del-param'">
          <xsl:value-of select="mcrurl:del-param($url, $par)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:message terminate="yes">
            Unknown function: <xsl:value-of select="$fn-name" />
          </xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </result>
  </xsl:template>

</xsl:stylesheet>
