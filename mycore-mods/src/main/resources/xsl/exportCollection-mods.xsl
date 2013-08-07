<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:include href="mycoreobject-mods.xsl" />
  <xsl:template match="/exportCollection">
    <xsl:choose>
      <xsl:when test="count(mycoreobject) = 1">
        <xsl:apply-templates mode="mods" />
      </xsl:when>
      <xsl:otherwise>
        <mods:modsCollection>
          <xsl:apply-templates mode="mods" />
        </mods:modsCollection>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>