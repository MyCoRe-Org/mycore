<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:include href="copynodes.xsl" />

  <xsl:template match="/mods">
    <mycoreobject>
      <metadata>
        <def.modsContainer class="MCRMetaXML" heritable="false" notinherit="true">
          <modsContainer inherited="0">
            <mods>
              <xsl:copy>
                <xsl:apply-templates select='@*' />
                <xsl:apply-templates select='node()' />
              </xsl:copy>
            </mods>
          </modsContainer>
        </def.modsContainer>
      </metadata>
    </mycoreobject>
  </xsl:template>
</xsl:stylesheet>