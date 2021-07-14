<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
> 
  <xsl:import href="resource:xsl/save-object.xsl" />

  <xsl:template match="/">
    <xsl:apply-templates select="*"/>
  </xsl:template>

  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="servflags[@class='MCRMetaLangText']">
    <xsl:variable name="accessKeys" select="document(concat('accesskey:', /mycoreobject/@ID))/servflag" />
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:copy-of select="node()"/>
      <xsl:if test="$accessKeys">
        <xsl:copy-of select="$accessKeys" />
      </xsl:if>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
