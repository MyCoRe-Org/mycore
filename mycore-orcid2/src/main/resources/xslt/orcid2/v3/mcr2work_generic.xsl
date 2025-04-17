<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="3.0"
  xmlns="http://www.w3.org/TR/REC-html40"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mcrstring="http://www.mycore.de/xslt/stringutils"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:work="http://www.orcid.org/ns/work"
  xmlns:common="http://www.orcid.org/ns/common">

  <xsl:include href="resource:xslt/functions/stringutils.xsl"/>

  <xsl:param name="MCR.ORCID2.Work.SourceURL" />
  <xsl:param name="MCR.ORCID2.Mods.DateIssued.XPath" />
  <xsl:variable name="short-description-max-length" select="5000"/>
  <xsl:variable name="string-150-max-length" select="150"/>

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
      <xsl:variable name="modsDateIssuedXpath">
        <xsl:evaluate xpath="$MCR.ORCID2.Mods.DateIssued.XPath" context-item="." />
      </xsl:variable>
      <xsl:apply-templates select="$modsDateIssuedXpath" />
      <xsl:call-template name="externalIDs" />
      <xsl:apply-templates select="(mods:location/mods:url)[1]" />
      <xsl:call-template name="workContributors" />
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

  <xsl:template match="mods:abstract[fn:string-length(text()) &gt; 0]">
    <work:short-description>
      <xsl:value-of select="mcrstring:shorten(text(), ($short-description-max-length - 1), '…')"/>
    </work:short-description>
  </xsl:template>

  <xsl:template match="mods:dateIssued[@encoding='w3cdtf']">
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
      <xsl:if test="not(mods:identifier[@type='doi' or @type='scopus' or @type='isbn' or @type='issn' or @type='urn' or @type='pmid' or @type='pmc' or @type='worldcat' or @type='ppn' or @type='hdl'])">
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

  <xsl:template match="mods:identifier[@type='isbn']">
    <common:external-id>
      <common:external-id-type><xsl:value-of select="@type" /></common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://www.worldcat.org/isbn/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='issn']">
    <common:external-id>
      <common:external-id-type><xsl:value-of select="@type" /></common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://portal.issn.org/resource/issn/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='urn']">
    <common:external-id>
      <common:external-id-type><xsl:value-of select="@type" /></common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://nbn-resolving.org/html/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='pmid']">
    <common:external-id>
      <common:external-id-type><xsl:value-of select="@type" /></common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://pubmed.ncbi.nlm.nih.gov/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='pmc']">
    <common:external-id>
      <common:external-id-type><xsl:value-of select="@type" /></common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://europepmc.org/article/pmc/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='worldcat']">
    <common:external-id>
      <common:external-id-type>oclc</common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://www.worldcat.org/oclc/<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='ppn']">
    <common:external-id>
      <common:external-id-type>k10plus</common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://opac.k10plus.de/DB=2.299/PPNSET?PPN=<xsl:value-of select="text()" /></common:external-id-url>
      <xsl:call-template name="external-id-relationship" />
    </common:external-id>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='hdl']">
    <common:external-id>
      <common:external-id-type>handle</common:external-id-type>
      <common:external-id-value><xsl:value-of select="text()" /></common:external-id-value>
      <common:external-id-url>https://hdl.handle.net/<xsl:value-of select="text()" /></common:external-id-url>
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
    <xsl:if test="mods:name[mods:role/mods:roleTerm[@type='code'][@authority='marcrelator'][. = 'aut' or . = 'asg' or . = 'edt' or . = 'trl' or . = 'hst']]">
      <work:contributors>
        <xsl:apply-templates select="mods:name[mods:role/mods:roleTerm[@type='code'][@authority='marcrelator'][. = 'aut' or . = 'asg' or . = 'edt' or . = 'trl' or . = 'hst']]"/>
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
      <xsl:variable name="creditname">
        <xsl:choose>
          <xsl:when test="mods:namePart[@type='given'] and mods:namePart[@type='family']">
            <xsl:value-of select="concat(mods:namePart[@type='given'][1], ' ', mods:namePart[@type='family'][1])" />
          </xsl:when>
          <xsl:when test="mods:namePart[@type='given'] or mods:namePart[@type='family']">
            <xsl:value-of select="concat(mods:namePart[@type='given'][1], mods:namePart[@type='family'][1])" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="mods:namePart" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <work:credit-name>
        <xsl:value-of select="mcrstring:shorten(normalize-space($creditname), ($string-150-max-length - 1), '…')" />
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

  <xsl:template match="*">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="@*|text()" />

</xsl:stylesheet>
