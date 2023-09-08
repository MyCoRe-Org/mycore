<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="3.0"
  xmlns="http://www.w3.org/TR/REC-html40"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:work="http://www.orcid.org/ns/work"
  xmlns:common="http://www.orcid.org/ns/common">

  <xsl:param name="MCR.ORCID2.Work.SourceURL" />

  <xsl:template match="mycoreobject">
    <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" />
  </xsl:template>

  <xsl:template match="mods:mods">
    <work:work xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.orcid.org/ns/work record_3.0/work-3.0.xsd">
      <xsl:call-template name="workTitle" />
      <xsl:call-template name="journal-title" />
      <xsl:apply-templates select="mods:abstract" />
      <xsl:call-template name="workCitation" />
      <xsl:call-template name="workType" />
      <xsl:apply-templates select="(descendant-or-self::mods:originInfo/mods:dateIssued[@encoding='w3cdtf'][@keyDate='yes']|descendant-or-self::mods:originInfo/mods:dateIssued[@encoding='w3cdtf'])[1]" />
      <xsl:call-template name="externalIDs" />
      <xsl:apply-templates select="(mods:location/mods:url)[1]" />
      <xsl:call-template name="workContributors" />
      <xsl:apply-templates select="mods:language" />
    </work:work>
  </xsl:template>

  <xsl:template name="workTitle">
    <work:title>
      <xsl:apply-templates select="mods:titleInfo"/>
    </work:title>
  </xsl:template>

  <xsl:template match="mods:mods/mods:titleInfo[not(@type='translated')][1]">
    <common:title>
      <xsl:apply-templates select="mods:nonSort" />
      <xsl:apply-templates select="mods:title" />
    </common:title>
    <xsl:apply-templates select="mods:subTitle" />
  </xsl:template>

  <xsl:template match="mods:mods/mods:titleInfo[not(@type='translated')][position() &gt; 1]"/>

  <xsl:template match="mods:mods/mods:titleInfo/mods:nonSort">
    <xsl:value-of select="text()" />
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="mods:mods/mods:titleInfo/mods:subTitle">
    <common:subtitle>
      <xsl:value-of select="text()" />
    </common:subtitle>
  </xsl:template>

  <xsl:template match="mods:mods/mods:titleInfo[@type='translated'][1]">
    <common:translated-title>
      <xsl:apply-templates select="@xml:lang" />
      <xsl:apply-templates select="mods:nonSort" />
      <xsl:apply-templates select="mods:title" />
      <xsl:apply-templates select="mods:subTitle" />
    </common:translated-title>
  </xsl:template>

  <xsl:template match="mods:mods/mods:titleInfo[@type='translated'][position() &gt; 1]"/>
  
  <xsl:template match="mods:mods/mods:titleInfo[@type='translated']/@xml:lang">
    <xsl:attribute name="language-code">
      <xsl:value-of select="." />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="mods:mods/mods:titleInfo[@type='translated']/mods:subTitle">
    <xsl:text>: </xsl:text>
    <xsl:value-of select="text()" />
  </xsl:template>

  <xsl:template match="mods:mods/mods:titleInfo/mods:title">
    <xsl:value-of select="text()" />
  </xsl:template>

  <xsl:template match="mods:abstract">
    <work:short-description>
      <xsl:value-of select="text()" />
    </work:short-description>
  </xsl:template>

  <xsl:template match="mods:mods/mods:originInfo/mods:dateIssued[@encoding='w3cdtf']">
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
      <xsl:apply-templates select="//mods:identifier" />
      <xsl:if test="not(mods:identifier[@type='doi' or @type='scopus' or @type='isbn' or @type='issn' or @type='urn' or @type='pubmed' or @type='pubmedcentral'])">
        <xsl:call-template name="source-work-id" />
      </xsl:if>
    </common:external-ids>
  </xsl:template>

  <xsl:template name="source-work-id">
    <common:external-id>
      <common:external-id-type>source-work-id</common:external-id-type>
      <xsl:variable name="mcrid" select="ancestor::mycoreobject/@ID" />
      <common:external-id-value>
        <xsl:variable name="createdate" select="fn:encode-for-uri(ancestor::mycoreobject/service/servdates[@class='MCRMetaISO8601Date']/servdate[@type='createdate']/text())" />
        <xsl:value-of select="document(concat('hash:', $mcrid, ':sha1:', $createdate))/string" />
      </common:external-id-value>
      <common:external-id-url>
        <xsl:value-of select="$MCR.ORCID2.Work.SourceURL" />
        <xsl:value-of select="$mcrid" />
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

  <xsl:template match="mods:identifier[@type='pubmed']">
    <common:external-id>
      <common:external-id-type>pmid</common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='pubmedcentral']">
    <common:external-id>
      <common:external-id-type>pmc</common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
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
    <common:url>
      <xsl:value-of select="text()" />
    </common:url>
  </xsl:template>

  <xsl:template name="workContributors">
    <xsl:if test="mods:name">
      <work:contributors>
        <xsl:apply-templates select="mods:name"/>
      </work:contributors>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:mods/mods:name">
    <work:contributor>
      <xsl:apply-templates select="mods:nameIdentifier[@type='orcid'][1]" />
      <xsl:call-template name="creditName" />
      <xsl:call-template name="contributorAttributes" />
    </work:contributor>
  </xsl:template>

  <xsl:template match="mods:mods/mods:name/mods:nameIdentifier[@type='orcid']">
    <common:contributor-orcid>
      <common:uri>https://orcid.org/<xsl:value-of select="text()" /></common:uri>
      <common:path><xsl:value-of select="text()" /></common:path>
      <common:host>orcid.org</common:host>
    </common:contributor-orcid>
  </xsl:template>

  <xsl:template name="creditName">
    <xsl:if test="mods:namePart">
      <work:credit-name>
        <xsl:choose>
          <xsl:when test="mods:namePart[@type='given'] and mods:namePart[@type='family']">
            <xsl:value-of select="normalize-space(concat(mods:namePart[@type='given'], ' ', mods:namePart[@type='family']))" />
          </xsl:when>
          <xsl:when test="mods:namePart[@type='given'] or mods:namePart[@type='family']">
            <xsl:value-of select="normalize-space(concat(mods:namePart[@type='given'], mods:namePart[@type='family']))" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="normalize-space(mods:namePart)" />
          </xsl:otherwise>
        </xsl:choose>
      </work:credit-name>
    </xsl:if>
  </xsl:template>

  <xsl:template name="contributorAttributes">
    <xsl:if test="mods:role/mods:roleTerm[@type='code'][@authority='marcrelator'][. = 'aut' or . = 'asg' or . = 'edt' or . = 'trl' or . = 'hst']">
      <work:contributor-attributes>
        <xsl:apply-templates select="(mods:role/mods:roleTerm[@type='code'][@authority='marcrelator'][. = 'aut' or . = 'asg' or . = 'edt' or . = 'trl' or . = 'hst'])[1]" />
      </work:contributor-attributes>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:mods/mods:name/mods:role/mods:roleTerm[@type='code'][@authority='marcrelator'][. = 'aut' or . = 'asg' or . = 'edt' or . = 'trl' or . = 'hst']">
    <work:contributor-role>
      <xsl:choose>
        <xsl:when test=".='aut'">author</xsl:when>
        <xsl:when test=".='asg'">assignee</xsl:when>
        <xsl:when test=".='edt'">editor</xsl:when>
        <xsl:when test=".='trl'">chair-or-translator</xsl:when>
        <xsl:when test=".='hst'">chair-or-translator</xsl:when>
      </xsl:choose>
    </work:contributor-role>
  </xsl:template>

  <xsl:template match="mods:language">
    <common:language-code>
      <xsl:value-of select="mods:languageTerm[@type='code']" />
    </common:language-code>
  </xsl:template>

  <xsl:template match="*">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="@*|text()" />

</xsl:stylesheet>
