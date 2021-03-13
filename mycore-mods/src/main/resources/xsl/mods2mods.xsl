<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:include href="mods-enhancer.xsl"/>
  <xsl:include href="xslInclude:mods"/>
  <xsl:template match="/mycoreobject">
    <xsl:apply-templates select="." mode="mods" />
  </xsl:template>
</xsl:stylesheet>