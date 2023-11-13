<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:work="http://www.orcid.org/ns/work"
                xmlns:common="http://www.orcid.org/ns/common"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="fn xsl">

  <xsl:import href="resource:xsl/orcid2/v3/work2mcr_generic.xsl"/>

  <xsl:param name="MCR.ORCID2.Genre.Mapping.Classification"/>
  <xsl:param name="MCR.ORCID2.Genre.Mapping.Classification.Mapping.Prefix"/>
  <xsl:param name="MCR.ORCID2.Genre.Mapping.Default.Genre"/>

  <!-- ORCID.org work types to MODS genre mapping -->
  <xsl:template match="work:type">
    <xsl:variable name="x-mapping"
                  select="fn:concat($MCR.ORCID2.Genre.Mapping.Classification.Mapping.Prefix, ':', text())"/>
    <xsl:variable name="genreMapped"
                  select="fn:document(fn:concat('notnull:classification:metadata:-1:children:', $MCR.ORCID2.Genre.Mapping.Classification))//category[label[@xml:lang='x-mapping' and contains(@text, $x-mapping)]]/@ID"/>

    <xsl:variable name="genre">
      <xsl:choose>
        <xsl:when test="fn:string-length($genreMapped) &gt; 0">
          <xsl:value-of select="$genreMapped"/>
        </xsl:when>
        <!-- Find all object types supported by ORCID here https://info.orcid.org/ufaqs/what-work-types-does-orcid-support/# -->
        <xsl:when test="text()='book'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="text()"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="host">
      <xsl:choose>
        <xsl:when test="text()='book-chapter'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='dictionary-entry'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='encyclopedia-entry'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='journal-article'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='journal-issue'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='newspaper-article'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='conference-abstract'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='conference-paper'">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:when test="text()='magazine-article'">
          <xsl:value-of select="text()"/>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsl:if test="string-length($genre) &gt; 0">
      <mods:genre type="intern">
        <xsl:value-of select="$genre"/>
      </mods:genre>
    </xsl:if>

    <xsl:if test="string-length($host) &gt; 0">
      <mods:relatedItem type="host">
        <mods:genre type="intern">
          <xsl:value-of select="$host"/>
        </mods:genre>
        <xsl:apply-templates select="../work:journal-title"/>
        <xsl:apply-templates
          select="../common:external-ids/common:external-id[common:external-id-relationship='part-of']"/>
      </mods:relatedItem>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
