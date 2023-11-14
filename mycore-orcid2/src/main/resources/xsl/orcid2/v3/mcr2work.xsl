<?xml version="1.0" encoding="UTF-8"?>

<!-- Transforms MyCoRe object with MODS to ORCID works XML schema -->

<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:work="http://www.orcid.org/ns/work"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="xsl mods fn">

  <xsl:import href="resource:xsl/orcid2/v3/mcr2work_generic.xsl"/>

  <!--
    To map an orcid type to mods:genre provide a classification containing the mapping. For each category provide a
    label[@xml:lang='x-mapping']. The text attribute of that label contains the actual mapping like
    $MCR.ORCID2.Genre.Mapping.Classification.Prefix:$ORCID-Work-Type e.g. orcid:other
  -->
  <xsl:param name="MCR.ORCID2.Genre.Mapping.Classification"/>
  <xsl:param name="MCR.ORCID2.Genre.Mapping.Classification.Prefix"/>
  <xsl:param name="MCR.ORCID2.Genre.Mapping.Default.Genre"/>

  <xsl:template name="workType">
    <xsl:choose>
      <xsl:when test="$MCR.ORCID2.Genre.Mapping.Classification">
        <work:type>
          <xsl:call-template name="map-mods-genre-to-orcid-work-type">
            <xsl:with-param name="mods.genre" select="//modsContainer/mods:mods/mods:genre[@type= 'intern'][1]"/>
          </xsl:call-template>
        </work:type>
      </xsl:when>
      <xsl:otherwise>
        <work:type>
          <xsl:value-of select="$MCR.ORCID2.Genre.Mapping.Default.Genre"/>
        </work:type>
      </xsl:otherwise>
    </xsl:choose>
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

  <xsl:template name="map-mods-genre-to-orcid-work-type">
    <xsl:param name="mods.genre"/>
    <xsl:variable name="orcid.genre"
                  select="fn:document(fn:concat('callJava:org.mycore.common.xml.MCRXMLFunctions:getXMapping:', $MCR.ORCID2.Genre.Mapping.Classification, ':', $mods.genre,':', $MCR.ORCID2.Genre.Mapping.Classification.Prefix))"/>
    <xsl:choose>
      <xsl:when test="fn:string-length($orcid.genre) &gt; 0">
        <xsl:value-of select="$orcid.genre"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$MCR.ORCID2.Genre.Mapping.Default.Genre"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
