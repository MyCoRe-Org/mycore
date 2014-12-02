<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mets="http://www.loc.gov/METS/">

  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <xsl:template match="mods:mods|mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods">
    <div class="metadata well">
      <!-- Print Authors -->
      <xsl:if test="mods:name/mods:displayForm[../mods:role/mods:roleTerm='aut']">
        <span class="names">
          <xsl:apply-templates select="mods:name" />
          <xsl:value-of select="': ' "></xsl:value-of>
        </span>
      </xsl:if>

      <!-- Print Titles -->
      <xsl:if test="mods:titleInfo">
        <span class="titles">
          <xsl:apply-templates select="mods:titleInfo" />
        </span>
      </xsl:if>

      <!-- Print Titles -->
      <xsl:if test="mods:originInfo[mods:place]">
        <span class="places">
          <xsl:apply-templates select="mods:originInfo" />
        </span>
      </xsl:if>
    </div>
  </xsl:template>

  <!-- Authors -->
  <xsl:template match="mods:name/mods:displayForm[../mods:role/mods:roleTerm='aut']">
    <span class="author">
      <xsl:value-of select="." />
    </span>
  </xsl:template>

  <!-- Title -->
  <xsl:template match="mods:titleInfo/mods:title">
    <span class="title">
      <xsl:choose>
        <xsl:when test="../../mods:identifier[@type='uri']">
          <a href="{../../mods:identifier[@type='uri']}">
            <xsl:value-of select="." />
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="." />
        </xsl:otherwise>
      </xsl:choose>
    </span>
  </xsl:template>

  <!-- place -->
  <xsl:template match="mods:originInfo[mods:place and not(contains(mods:edition, '[Electronic ed.]'))]">
    <span class="place">
      <xsl:for-each select="mods:place/mods:placeTerm">
        <xsl:value-of select="." />
      </xsl:for-each>
      <xsl:if test="mods:dateIssued">
        <span class="date">
          <xsl:value-of select="mods:dateIssued" />
        </span>
      </xsl:if>
    </span>
  </xsl:template>

  <xsl:template match="text()" />

</xsl:stylesheet>