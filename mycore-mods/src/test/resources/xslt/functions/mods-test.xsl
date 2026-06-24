<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrmods="http://www.mycore.de/xslt/mods"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/functions/mods.xsl" />

  <xsl:param name="pages" as="xs:string?" />
  <xsl:param name="fn-name" as="xs:string" />

  <xsl:template match="/">
    <result>
      <xsl:choose>
        <xsl:when test="$fn-name = 'pages-to-extent'">
          <xsl:copy-of select="mcrmods:pages-to-extent($pages)" />
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
