<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:mets="http://www.loc.gov/METS/" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
                version="1.0">


  <xsl:template match="mets:smLink">
    <xsl:variable name="from" select="@xlink:from" />
    <xsl:variable name="fromNode" select="//mets:div[@ID=$from]" />

    <xsl:variable name="to" select="@xlink:to" />

    <xsl:choose>
      <xsl:when test="not(../mets:smLink[@xlink:to=$to and @xlink:from = $fromNode//mets:div/@ID])">
        <xsl:copy>
          <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:comment>removed node</xsl:comment>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

  <!-- remove amd and dmd section -->
  <xsl:template match="mets:amdSec"></xsl:template>
  <xsl:template match="mets:dmdSec"></xsl:template>
  <xsl:template match="mets:metsHdr"></xsl:template>


  <xsl:template match="mets:mptr">
    <!-- don't meed mets:mptr -->
  </xsl:template>

  <xsl:template match="@ADMID|@DMDID">
  </xsl:template>

  <xsl:template match="mets:structMap[@TYPE='LOGICAL']//mets:div">
    <xsl:copy>
      <xsl:attribute name="ORDER">
        <xsl:number />
      </xsl:attribute>
      <xsl:if test="not(@LABEL)">
        <xsl:attribute name="LABEL">
          <xsl:choose>
            <xsl:when
              test="@TYPE and not(contains(i18n:translate(concat('component.mets.dfgStructureSet.', @TYPE)), '???'))">
              <xsl:value-of select="i18n:translate(concat('component.mets.dfgStructureSet.', @TYPE))" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="'-'" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="node()|@*" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
