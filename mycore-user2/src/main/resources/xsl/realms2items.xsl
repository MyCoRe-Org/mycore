<?xml version="1.0" encoding="UTF-8"?>

<!-- Converts realms.xml to a list of items for use in user-editor.xml form -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="xsl">
  <xsl:output indent="no" method="xml" encoding="UTF-8" />

  <xsl:template match="/realms">
    <items>
    <!-- Display local realm as first choice -->
      <xsl:apply-templates select="realm[@id = /realms/@local]" />
      <xsl:apply-templates select="realm[@id != /realms/@local]" />
    </items>
  </xsl:template>

  <xsl:template match="realm">
    <xsl:if test="(string-length(@setable) = 0) or (@setable = 'true')">
      <item value="{@id}">
        <xsl:copy-of select="label" />
      </item>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>