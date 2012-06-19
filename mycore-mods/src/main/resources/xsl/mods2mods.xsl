<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="mycoreobject[contains(@ID,'_mods_')]" mode="mods">
    <xsl:apply-templates mode="mods2mods" />
  </xsl:template>
  <xsl:template match="@*|node()" mode="mods2mods">
    <xsl:apply-templates mode="mods2mods" />
  </xsl:template>
  <xsl:template match="mods:*|text()[namespace-uri(..)='http://www.loc.gov/mods/v3']" mode="mods2mods">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates mode="mods2mods" />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>