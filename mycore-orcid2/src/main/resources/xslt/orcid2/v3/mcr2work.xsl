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
    <xsl:if test="mods:relatedItem[@type='host']/mods:titleInfo">
      <work:journal-title>
        <xsl:if test="mods:relatedItem/mods:titleInfo/mods:nonSort">
          <xsl:value-of select="mods:relatedItem/mods:titleInfo/mods:nonSort/text()"/>
          <xsl:text> </xsl:text>
        </xsl:if>
        <xsl:value-of select="mods:relatedItem/mods:titleInfo/mods:title/text()"/>
        <xsl:if test="mods:relatedItem/mods:titleInfo/mods:subTitle">
          <xsl:text>: </xsl:text>
          <xsl:value-of select="mods:relatedItem/mods:titleInfo/mods:subTitle/text()"/>
        </xsl:if>
      </work:journal-title>
    </xsl:if>
  </xsl:template>

  <xsl:template name="workCitation"/>

  <xsl:template name="publicationDate">
    <xsl:apply-templates select="(mods:originInfo/mods:dateIssued[@encoding='w3cdtf'][@keyDate='yes'],descendant::mods:relatedItem[@type='host'][not(ancestor::mods:relatedItem[not(@type='host')])]/mods:originInfo/mods:dateIssued[@encoding='w3cdtf'][@keyDate='yes'],mods:originInfo/mods:dateIssued[@encoding='w3cdtf'])[1],descendant::mods:relatedItem[@type='host'][not(ancestor::mods:relatedItem[not(@type='host')])]/mods:originInfo/mods:dateIssued[@encoding='w3cdtf']" />
  </xsl:template>

</xsl:stylesheet>
