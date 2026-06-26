<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrlanguage="http://www.mycore.de/xslt/language"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/functions/language.xsl" />

  <xsl:param name="text" as="xs:string?" />
  <xsl:param name="fn-name" as="xs:string" />

  <xsl:template match="/">
    <result>
      <xsl:choose>
        <xsl:when test="$fn-name = 'detect-language'">
          <xsl:value-of select="mcrlanguage:detect-language($text)" />
        </xsl:when>
        <xsl:when test="$fn-name = 'detect-language-by-character'">
          <xsl:value-of select="mcrlanguage:detect-language-by-character($text)" />
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
