<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:include href="mods-utils.xsl"/>
	
	<!-- standard copy template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
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
</xsl:stylesheet>