<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:i="http://www.mycore.org/i18n"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="xsl i i18n">

  <xsl:include href="copynodes.xsl" />
  
  <xsl:param name="CurrentLang" />
  
  <xsl:template match="i:*">
    <xsl:choose>
      <xsl:when test="$CurrentLang=local-name()">
        <xsl:apply-templates select="text()|*" />
      </xsl:when>
      <xsl:when test="'code'=local-name()">
        <xsl:value-of select="i18n:translate(.)" />
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@*[starts-with(.,'|')][contains(.,':')]">
    <xsl:attribute name="{name()}">
      <xsl:choose>
        <xsl:when test="contains(.,'|code:')">
          <xsl:value-of select="i18n:translate(substring-before(substring-after(.,'|code:'),'|'))" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="substring-before(substring-after(.,concat('|',$CurrentLang,':')),'|')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>
