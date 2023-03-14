<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:work="http://www.orcid.org/ns/work"
  xmlns:common="http://www.orcid.org/ns/common"
  exclude-result-prefixes="xsl">

  <xsl:import href="resource:xsl/orcid2/v3/work2mcr_generic.xsl" />

  <!-- ORCID.org work types to MODS genre mapping -->
  <xsl:template match="work:type">
    <xsl:variable name="genre">
      <xsl:choose>
        <!--xsl:when test="text()='book'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='book-chapter'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='book-review'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='dictionary-entry'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='dissertation'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='dissertation-thesis'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='encyclopedia-entry'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='edited-book'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='journal-article'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='journal-issue'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='magazine-article'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='manual'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='online-resource'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='newsletter-article'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='newspaper-article'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='preprint'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='report'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='research-tool'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='supervised-student-publication'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='test'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='translation'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='website'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='working-paper'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='conference-abstract'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='conference-paper'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='conference-poster'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='disclosure'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='license'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='patent'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='registered-copyright'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='trademark'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='annotation'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='artistic-performance'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='data-management-plan'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='data-set'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='invention'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='lecture-speech'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='physical-object'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='research-technique'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='software'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='spin-off-company'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='standards-and-policy'"><xsl:value-of select="text()" /></xsl:when-->
        <!--xsl:when test="text()='technical-standard'"><xsl:value-of select="text()" /></xsl:when-->
        <xsl:otherwise><xsl:value-of select="text()" /></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="host">
      <xsl:choose>
        <xsl:when test="text()='book-chapter'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='dictionary-entry'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='encyclopedia-entry'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='journal-article'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='journal-issue'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='newspaper-article'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='conference-abstract'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='conference-paper'"><xsl:value-of select="text()" /></xsl:when>
        <xsl:when test="text()='magazine-article'"><xsl:value-of select="text()" /></xsl:when>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="string-length($genre) &gt; 0">
      <mods:genre type="intern"><xsl:value-of select="$genre" /></mods:genre>
    </xsl:if>
    <xsl:if test="string-length($host) &gt; 0">
      <mods:relatedItem type="host">
        <mods:genre type="intern"><xsl:value-of select="$host" /></mods:genre>
        <xsl:apply-templates select="../work:journal-title" />
        <xsl:apply-templates select="../common:external-ids/common:external-id[common:external-id-relationship='part-of']" />
      </mods:relatedItem>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
