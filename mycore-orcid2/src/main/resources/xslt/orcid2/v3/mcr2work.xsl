<?xml version="1.0" encoding="UTF-8"?>

<!-- Transforms MyCoRe object with MODS to ORCID works XML schema -->

<xsl:stylesheet version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:work="http://www.orcid.org/ns/work"
  exclude-result-prefixes="xsl mods">

  <xsl:import href="resource:xslt/orcid2/v3/mcr2work_generic.xsl" />

  <xsl:template name="workType">
    <work:type>journal-article</work:type> <!-- TODO -->
  </xsl:template>

  <xsl:template name="journal-title">
    <xsl:variable name="host-title-info"
      select="(mods:relatedItem[@type='host'])[1]/(mods:titleInfo[not(@type)], mods:titleInfo)[1]" />
    <xsl:if test="$host-title-info/mods:title">
      <work:journal-title>
        <xsl:value-of select="string-join(($host-title-info/mods:nonSort, $host-title-info/mods:title), ' ')" />
        <xsl:if test="$host-title-info/mods:subTitle">
          <xsl:text>: </xsl:text>
          <xsl:value-of select="$host-title-info/mods:subTitle" />
        </xsl:if>
      </work:journal-title>
    </xsl:if>
  </xsl:template>

  <xsl:template name="workCitation"/>

</xsl:stylesheet>
