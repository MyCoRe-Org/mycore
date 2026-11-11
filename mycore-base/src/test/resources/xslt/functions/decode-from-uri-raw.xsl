<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrurl="http://www.mycore.de/xslt/url"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:param name="value" as="xs:string" />
  <xsl:param name="suppress-url" as="xs:boolean" select="false()"/>
  <xsl:param name="suppress-form" as="xs:boolean" select="false()"/>

  <xsl:template match="/">
    <result>
      <xsl:if test="not($suppress-url)">
        <url-decoded>
          <xsl:value-of select="mcrurl:decode-from-uri($value)"/>
        </url-decoded>
      </xsl:if>
      <xsl:if test="not($suppress-form)">
        <form-decoded>
          <xsl:value-of select="mcrurl:decode-from-uri($value, true())"/>
        </form-decoded>
      </xsl:if>
    </result>
  </xsl:template>

</xsl:stylesheet>
