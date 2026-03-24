<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions"/>
  <xsl:include href="resource:xslt/utils/mods-utils.xsl" />

  <!-- standard copy template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:mods">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>

      <!-- all children except relatedItem -->
      <xsl:apply-templates select="node()[not(self::mods:relatedItem)]"/>

      <!-- relatedItem ans Ende -->
      <xsl:apply-templates select="mods:relatedItem"/>
    </xsl:copy>
  </xsl:template>
	
  <xsl:template match="mods:name[@type='personal']">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
      <xsl:if test="not(mods:namePart[@type='family']) and mods:displayForm">
        <xsl:call-template name="mods.seperateName">
          <xsl:with-param name="displayForm" select="mods:displayForm" />
        </xsl:call-template>
      </xsl:if>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:detail/mods:caption">
  </xsl:template>

  <xsl:template match="mods:genre[@authority='marcgt']">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
    <xsl:choose>
      <xsl:when test="text()='journal'">
        <!-- bibutils does not detect article on RIS or Endnote export -->
        <mods:genre>academic journal</mods:genre>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:url[@access='preview']">
  </xsl:template>

</xsl:stylesheet>
