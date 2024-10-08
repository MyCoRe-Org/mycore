<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrclass="http://www.mycore.de/xslt/classification"
                exclude-result-prefixes="#all">
  <xsl:include href="resource:xslt/functions/classification.xsl"/>

  <xsl:param name="classid" as="xs:string"/>
  <xsl:param name="categid" as="xs:string"/>
  <xsl:param name="lang" as="xs:string"/>
  <xsl:param name="CurrentLang" as="xs:string?" select="'de'"/>
  <xsl:param name="DefaultLang" as="xs:string?" select="'en'"/>

  <xsl:template match="test-category">
    <result>
      <xsl:copy-of select="mcrclass:category($classid, $categid)"/>
    </result>
  </xsl:template>

  <xsl:template match="test-current-label">
    <xsl:variable name="class" select="mcrclass:category($classid, $categid)"/>
    <result>
      <xsl:copy-of select="mcrclass:current-label($class)"/>
    </result>
  </xsl:template>

  <xsl:template match="test-label">
    <xsl:variable name="class" select="mcrclass:category($classid, $categid)"/>
    <result>
      <xsl:copy-of select="mcrclass:label($lang, $class)"/>
    </result>

  </xsl:template>

  <xsl:template match="test-current-label-text">
    <xsl:variable name="class" select="mcrclass:category($classid, $categid)"/>
    <result>
      <xsl:value-of select="mcrclass:current-label-text($class)"/>
    </result>
  </xsl:template>

  <xsl:template match="test-label-text">
    <xsl:variable name="class" select="mcrclass:category($classid, $categid)"/>
    <result>
      <xsl:copy-of select="mcrclass:label-text($lang, $class)"/>
    </result>

  </xsl:template>

</xsl:stylesheet>
