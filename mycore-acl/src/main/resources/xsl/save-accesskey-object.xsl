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
    <xsl:variable name="accessKeys" select="document(concat('accesskeys:', /mycoreobject/@ID))" />
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:copy-of select="node()"/>
      <servflag type="accesskeys" inherited="0" form="plain">
        <xsl:value-of select="$accessKeys" />
      </servflag>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
