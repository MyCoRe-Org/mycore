<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink">
  <xsl:param name="parentId" />

  <xsl:include href="xslInclude:PPN-mods-simple"/>
  <xsl:include href="copynodes.xsl" />

  <xsl:template match="/">
    <mycoreobject>
      <xsl:if test="string-length($parentId) &gt; 0">
        <structure>
          <parents class="MCRMetaLinkID" notinherit="true" heritable="false">
            <parent xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="locator" xlink:href="{$parentId}" />
          </parents>
        </structure>
      </xsl:if>
      <metadata>
        <def.modsContainer class="MCRMetaXML" heritable="false" notinherit="true">
          <modsContainer inherited="0">
            <xsl:copy>
              <xsl:apply-templates select='@*' />
              <xsl:apply-templates select='node()' />
            </xsl:copy>
          </modsContainer>
        </def.modsContainer>
      </metadata>
    </mycoreobject>
  </xsl:template>
</xsl:stylesheet>