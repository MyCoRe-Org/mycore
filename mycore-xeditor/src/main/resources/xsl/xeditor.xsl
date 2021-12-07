<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xed="http://www.mycore.de/xeditor"
  exclude-result-prefixes="xsl xed">

  <xsl:strip-space elements="xed:*" />

  <xsl:include href="copynodes.xsl" />
  <xsl:include href="xslInclude:xeditor" />

  <xsl:template match="xed:template" />

  <xsl:template match="xed:validate">
    <xsl:if test="@hasError">
      <xsl:apply-templates select="." mode="message" />
    </xsl:if>
  </xsl:template>

  <xsl:template match="xed:controls">
    <xsl:for-each select="xed:control">
      <xsl:apply-templates select="text()" mode="xed.control">
        <xsl:with-param name="name" select="@name" />
      </xsl:apply-templates>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="xed:*">
    <xsl:apply-templates select="node()" />
  </xsl:template>

</xsl:stylesheet>