<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" exclude-result-prefixes="i18n xlink xsl mods" >
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
            <mods:mods>
              <xsl:apply-templates select="mods:mods/*" />
              <mods:identifier invalid="yes" type="uri">
                <xsl:value-of select="concat('//gso.gbv.de/DB=2.1/PPNSET?PPN=', mods:mods/mods:recordInfo/mods:recordIdentifier[@source='DE-601'])" />
              </mods:identifier>
            </mods:mods>
          </modsContainer>
        </def.modsContainer>
      </metadata>
    </mycoreobject>
  </xsl:template>

  <xsl:template match="*">
    <xsl:element name="mods:{name()}">
      <xsl:copy-of select="namespace::*" />
      <xsl:apply-templates select="node()|@*" />
    </xsl:element>
  </xsl:template>

  <xsl:template match="mods:originInfo[not(@eventType)]">
    <xsl:if test="not(//mods:mods/mods:originInfo/@eventType='publication')">
      <mods:originInfo eventType="publication">
        <xsl:apply-templates select="node()|@*" />
      </mods:originInfo>
    </xsl:if>
  </xsl:template>

  <!-- add encoding to a mods:dateIssued without encoding -->
  <!-- but only if there is no start or end Point with encoding -->
  <!-- and only if there is no other mods:dateIssued -->
  <!-- so you dont have multiple mods:dateIssued with the same encoding -->
  <xsl:template match="mods:dateIssued[not(@encoding)]">
      <xsl:if test="not(../mods:dateIssued[@point='start' or @point='end' and @encoding]) and not(following-sibling::mods:dateIssued)">
        <!-- TODO: check date format first! -->
        <mods:dateIssued encoding="w3cdtf">
          <xsl:apply-templates select="node()|@*" />
        </mods:dateIssued>
      </xsl:if>
  </xsl:template>

</xsl:stylesheet>
