<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:mets="http://www.loc.gov/METS/"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:mode on-no-match="shallow-copy" />

  <!-- remove header, amd and dmd sections -->
  <xsl:template match="mets:metsHdr" />
  <xsl:template match="mets:amdSec" />
  <xsl:template match="mets:dmdSec" />
  <xsl:template match="@ADMID|@DMDID" />

  <!-- mets:mptr is not needed -->
  <xsl:template match="mets:mptr" />

  <xsl:template match="mets:smLink">
    <xsl:variable name="from" select="@xlink:from" />
    <xsl:variable name="fromNode" select="//mets:div[@ID = $from]" />
    <xsl:variable name="to" select="@xlink:to" />
    <xsl:choose>
      <xsl:when test="not(../mets:smLink[@xlink:to = $to and @xlink:from = $fromNode//mets:div/@ID])">
        <xsl:copy>
          <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:comment>removed node</xsl:comment>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mets:structMap[@TYPE = 'LOGICAL']//mets:div">
    <xsl:copy>
      <xsl:attribute name="ORDER">
        <xsl:number />
      </xsl:attribute>
      <xsl:if test="not(@LABEL)">
        <xsl:variable name="labelKey" select="concat('component.mets.dfgStructureSet.', @TYPE)" />
        <xsl:attribute name="LABEL"
          select="if (@TYPE and mcri18n:exists($labelKey)) then mcri18n:translate($labelKey) else '-'" />
      </xsl:if>
      <xsl:apply-templates select="node()|@*" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
