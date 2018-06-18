<?xml version="1.0" encoding="UTF-8"?>

<!-- Transforms MyCoRe object with MODS to ORCID works XML schema -->

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:work="http://www.orcid.org/ns/work"
  xmlns:common="http://www.orcid.org/ns/common"
  exclude-result-prefixes="xsl mods">

  <xsl:template match="mycoreobject">
    <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" />
  </xsl:template>

  <xsl:template match="mods:mods">
    <work:work visibility="public" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.orcid.org/ns/work /work-2.1.xsd ">
      <xsl:call-template name="workTitle" />
      <xsl:apply-templates select="mods:abstract[1]" />
      <xsl:call-template name="workType" />
      <xsl:apply-templates select="(descendant-or-self::mods:originInfo/mods:dateIssued[@encoding='w3cdtf'])[1]" />
      <xsl:call-template name="externalIDs" />
      <xsl:apply-templates select="mods:location/mods:url" />
      <xsl:call-template name="workContributors" />
      <xsl:apply-templates select="mods:language" />
    </work:work>
  </xsl:template>
  
  <xsl:template name="workTitle">
    <work:title>
      <xsl:apply-templates select="mods:titleInfo[not(@type='translated')][1]" />
      <xsl:apply-templates select="mods:titleInfo[@type='translated'][1]" />
    </work:title>
  </xsl:template>
  
  <xsl:template match="mods:titleInfo">
    <common:title>
      <xsl:apply-templates select="mods:nonSort" />
      <xsl:value-of select="mods:title" />
    </common:title>
    <xsl:apply-templates select="mods:subTitle" />
  </xsl:template>

  <xsl:template match="mods:nonSort">
    <xsl:value-of select="text()" />
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="mods:subTitle">
    <common:subtitle>
      <xsl:value-of select="text()" />
    </common:subtitle>
  </xsl:template>
  
  <xsl:template match="mods:titleInfo[@type='translated']">
    <common:translated-title>
      <xsl:apply-templates select="@xml:lang" />
      <xsl:apply-templates select="mods:nonSort" />
      <xsl:value-of select="mods:title" />
      <xsl:for-each select="mods:subTitle">
        <xsl:text>: </xsl:text>
        <xsl:value-of select="text()" />
      </xsl:for-each>
    </common:translated-title>
  </xsl:template>
  
  <xsl:template match="mods:titleInfo[@type='translated']/@xml:lang">
    <xsl:attribute name="language-code">
      <xsl:value-of select="." />
    </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="mods:releatedItem[@type='host']">
    <xsl:apply-templates select="mods:titleInfo[1]" />
    <xsl:apply-templates select="mods:identifier" />
  </xsl:template>
  
  <xsl:template match="mods:relatedItem[@type='host']/mods:titleInfo">
    <work:journal-title>
      <xsl:apply-templates select="mods:nonSort" />
      <xsl:value-of select="mods:title" />
    </work:journal-title>
  </xsl:template>

  <xsl:template match="mods:abstract">
    <work:short-description>
      <xsl:value-of select="text()" />
    </work:short-description>
  </xsl:template>
  
  <xsl:template name="workType">
    <work:type>journal-article</work:type>
  </xsl:template>

  <xsl:template match="mods:originInfo/mods:dateIssued[@encoding='w3cdtf']">
    <common:publication-date>
      <common:year>
        <xsl:value-of select="substring(text(),1,4)" />
      </common:year>
      <xsl:if test="string-length(.) &gt;= 7">
        <common:month>
          <xsl:value-of select="substring(text(),6,2)" />
        </common:month>
      </xsl:if>
      <xsl:if test="string-length(.) &gt;= 10">
        <common:day>
          <xsl:value-of select="substring(text(),9,2)" />
        </common:day>
      </xsl:if>
    </common:publication-date>
  </xsl:template>
  
  <xsl:template name="externalIDs">
      <common:external-ids>
        <xsl:apply-templates select="ancestor::mycoreobject/@ID" />
        <xsl:apply-templates select="mods:identifier" />
      </common:external-ids>
  </xsl:template>

  <xsl:param name="MCR.ORCID.Works.SourceURL" />
  
  <xsl:template match="mycoreobject/@ID">
    <common:external-id>
      <common:external-id-type>source-work-id</common:external-id-type>
      <common:external-id-value>
        <xsl:value-of select="substring-before(.,'_mods_')" /> 
        <xsl:text>:</xsl:text>
        <xsl:value-of select="number(substring-after(.,'_mods_'))" />
      </common:external-id-value>
      <common:external-id-url>
        <xsl:value-of select="$MCR.ORCID.Works.SourceURL" />
        <xsl:value-of select="." />
      </common:external-id-url>
      <common:external-id-relationship>self</common:external-id-relationship>
    </common:external-id>
  </xsl:template>
  
  <xsl:template match="mods:identifier[@type='doi']">
    <common:external-id>
      <common:external-id-type>doi</common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://doi.org/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>
  
  <xsl:template match="mods:identifier[@type='scopus']">
    <common:external-id>
      <common:external-id-type>eid</common:external-id-type>
      <common:external-id-value>2-s2.0-<xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>http://www.scopus.com/inward/record.url?eid=2-s2.0-<xsl:value-of select="text()" />&amp;partnerID=HzOxMe3b</common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>
  
  <xsl:template match="mods:identifier[@type='isbn']|mods:identifier[@type='issn']">
    <common:external-id>
      <common:external-id-type><xsl:value-of select="@type" /></common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>
  
  <xsl:template match="mods:identifier[@type='urn']">
    <common:external-id>
      <common:external-id-type>urn</common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://nbn-resolving.org/html/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template name="external-id-relationship">
    <common:external-id-relationship>
      <xsl:choose>
        <xsl:when test="ancestor::mods:relatedItem[@type='host']">part-of</xsl:when>
        <xsl:otherwise>self</xsl:otherwise>
      </xsl:choose>
    </common:external-id-relationship>
  </xsl:template>

  <xsl:template match="mods:location/mods:url">
    <work:url>
      <xsl:value-of select="text()" />
    </work:url>
  </xsl:template>

  <xsl:template name="workContributors">
    <work:contributors>
      <xsl:apply-templates select="mods:name[@type='personal']" />
    </work:contributors>
  </xsl:template>
  
  <xsl:template match="mods:name[@type='personal']">
    <work:contributor>
      <xsl:apply-templates select="mods:nameIdentifier[@type='orcid']" />
      <xsl:call-template name="creditName" />
      <xsl:apply-templates select="mods:role" />
    </work:contributor>
  </xsl:template>
  
  <xsl:template match="mods:nameIdentifier[@type='orcid']">
    <common:contributor-orcid>
      <common:uri>https://orcid.org/<xsl:value-of select="text()" /></common:uri>
      <common:path><xsl:value-of select="text()" /></common:path>
      <common:host>orcid.org</common:host>
    </common:contributor-orcid>
  </xsl:template>
  
  <xsl:template name="creditName">
    <work:credit-name>
      <xsl:value-of select="mods:namePart[@type='family']" />
      <xsl:text>, </xsl:text>
      <xsl:value-of select="mods:namePart[@type='given']" />
    </work:credit-name>
  </xsl:template>
  
  <xsl:template match="mods:role">
    <work:contributor-attributes>
      <xsl:apply-templates select="mods:roleTerm[@type='code']" />
    </work:contributor-attributes>
  </xsl:template>
  
  <xsl:template match="mods:roleTerm">
    <work:contributor-role>
      <xsl:choose>
        <xsl:when test=".='aut'">author</xsl:when>
        <xsl:when test=".='asg'">assignee</xsl:when>
        <xsl:when test=".='edt'">editor</xsl:when>
        <xsl:when test=".='trl'">chair-or-translator</xsl:when>
        <xsl:otherwise></xsl:otherwise>
      </xsl:choose>
    </work:contributor-role>
  </xsl:template>
  
  <xsl:template match="mods:language">
    <common:language-code>
      <xsl:value-of select="mods:languageTerm[@authority='rfc4646'][@type='code']" />
    </common:language-code>
  </xsl:template>
  
  <xsl:template match="*|text()" />

</xsl:stylesheet>
