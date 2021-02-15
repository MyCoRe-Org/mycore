<?xml version="1.0"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink" 
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:mcrclass="http://www.mycore.de/xslt/classification"
  exclude-result-prefixes="fn">
  
  <xsl:function name="mcrclass:current-label" as="element()?">
    <xsl:param name="class" as="element()" />
    <xsl:choose>
      <xsl:when test="$class/label[@xml:lang=$CurrentLang]">
        <xsl:sequence select="$class/label[@xml:lang=$CurrentLang]" />
      </xsl:when>
      <xsl:when test="$class/label[@xml:lang=$DefaultLang]">
        <xsl:sequence select="$class/label[@xml:lang=$DefaultLang]" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="$class/label[1]" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
  <xsl:function name="mcrclass:current-label-text" as="xs:string?">
    <xsl:param name="class" as="element()" />
    <xsl:variable name="label" select="mcrclass:current-label($class)" />
    <xsl:choose>
      <xsl:when test="$label">
        <xsl:sequence select="$label/@text" />
      </xsl:when>
      <xsl:when test="$class/@ID">
        <xsl:sequence select="fn:concat('??', $class/@ID, '@', $CurrentLang, '??')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="()" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
</xsl:stylesheet>
