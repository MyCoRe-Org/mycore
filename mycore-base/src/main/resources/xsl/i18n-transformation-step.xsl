<?xml version="1.0" encoding="UTF-8"?>

<!--
  See MCR-2252,
  can be used to i18n webpages, xeditor forms, xsl files
  as a single, final transformation step
-->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:i="http://www.mycore.org/i18n"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="xsl i i18n">

  <xsl:include href="copynodes.xsl" />

  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />

  <xsl:variable name="CurrentLangPrefix" select="concat('|',$CurrentLang,':')" />
  <xsl:variable name="DefaultLangPrefix" select="concat('|',$DefaultLang,':')" />
  <xsl:variable name="CodePrefix" select="'|code:'" />

  <xsl:template match="i:*">
    <xsl:choose>
      <xsl:when test="$CurrentLang=local-name()">
        <xsl:apply-templates select="text()|*" />
      </xsl:when>
      <xsl:when test="($DefaultLang=local-name()) and not(../i:*[$CurrentLang=local-name(.)])">
        <xsl:apply-templates select="text()|*" />
      </xsl:when>
      <xsl:when test="'code'=local-name()">
        <xsl:value-of select="i18n:translate(.)" disable-output-escaping="yes" />
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@*[starts-with(.,'|')][contains(.,':')]">
    <xsl:attribute name="{name()}">
      <xsl:choose>
        <xsl:when test="contains(.,$CurrentLangPrefix)">
          <xsl:value-of select="substring-before(substring-after(.,$CurrentLangPrefix),'|')" />
        </xsl:when>
        <xsl:when test="contains(.,$DefaultLangPrefix)">
          <xsl:value-of select="substring-before(substring-after(.,$DefaultLangPrefix),'|')" />
        </xsl:when>
        <xsl:when test="contains(.,$CodePrefix)">
          <xsl:value-of select="i18n:translate(substring-before(substring-after(.,$CodePrefix),'|'))" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="." />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>
