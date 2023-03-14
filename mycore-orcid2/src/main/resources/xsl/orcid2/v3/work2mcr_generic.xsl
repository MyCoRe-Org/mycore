<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
  xmlns="http://www.w3.org/TR/REC-html40"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:work="http://www.orcid.org/ns/work"
  xmlns:common="http://www.orcid.org/ns/common"
  xmlns:bulk="http://www.orcid.org/ns/bulk"
  xmlns:activities="http://www.orcid.org/ns/activities">

  <xsl:template match="activities:works|activities:group|bulk:bulk">
    <xsl:copy>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="work:work|work:work-summary">
    <xsl:copy>
      <mods:mods>
        <xsl:apply-templates select="work:type" />
        <xsl:apply-templates select="work:title" />
        <xsl:apply-templates select="work:contributors" />
        <xsl:apply-templates select="common:publication-date" />
        <xsl:apply-templates select="work:short-description" />
        <xsl:apply-templates select="common:url" />
        <xsl:apply-templates select="common:external-ids/common:external-id[common:external-id-relationship='self']" />
        <xsl:apply-templates select="common:language-code" />
      </mods:mods>
      <xsl:apply-templates select="work:citation[work:citation-type='bibtex']" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="work:citation[work:citation-type='bibtex']">
    <bibTeX>
      <xsl:value-of select="work:citation-value" />
    </bibTeX>
  </xsl:template>

  <xsl:template match="work:title">
    <mods:titleInfo>
      <xsl:apply-templates select="common:title" />
      <xsl:apply-templates select="common:subtitle" />
    </mods:titleInfo>
    <xsl:apply-templates select="common:translated-title" />
  </xsl:template>

  <xsl:template match="common:title">
    <mods:title>
      <xsl:value-of select="text()" />
    </mods:title>
  </xsl:template>

  <xsl:template match="common:subtitle">
    <mods:subTitle>
      <xsl:value-of select="text()" />
    </mods:subTitle>
  </xsl:template>

  <xsl:template match="common:translated-title">
    <mods:titleInfo type="translated">
      <xsl:for-each select="@language-code">
        <xsl:attribute name="xml:lang">
          <xsl:value-of select="." />
        </xsl:attribute>
      </xsl:for-each>
      <mods:title>
        <xsl:value-of select="text()" />
      </mods:title>
    </mods:titleInfo>
  </xsl:template>

  <xsl:template match="work:journal-title">
    <mods:titleInfo>
      <mods:title>
        <xsl:value-of select="text()"/>
      </mods:title>
    </mods:titleInfo>
  </xsl:template>

  <xsl:template match="work:contributors">
    <xsl:apply-templates select="work:contributor" />
  </xsl:template>

  <xsl:template match="work:contributor">
    <mods:name type="personal">
      <xsl:apply-templates select="work:credit-name" />
      <xsl:apply-templates select="work:contributor-attributes" />
      <xsl:apply-templates select="common:contributor-orcid" />
    </mods:name>
  </xsl:template>
  
  <!-- Split "Doe, John F." into mods:namePart -->
  <xsl:template match="work:credit-name[contains(.,',')]">
    <mods:namePart type="family">
      <xsl:value-of select="normalize-space(substring-before(.,','))" />
    </mods:namePart>
    <mods:namePart type="given">
      <xsl:value-of select="normalize-space(substring-after(.,','))" />
    </mods:namePart>
  </xsl:template>
  
  <!-- Split "John F. Doe" into mods:namePart -->
  <xsl:template match="work:credit-name[not(contains(.,','))]">
    <mods:namePart type="family">
      <xsl:value-of select="tokenize(.,' ')[last()]" />
    </mods:namePart>
    <mods:namePart type="given">
      <xsl:for-each select="tokenize(.,' ')">
        <xsl:if test="position() != last()">
          <xsl:if test="position() &gt; 1">
            <xsl:text> </xsl:text>
          </xsl:if>
          <xsl:value-of select="." />
        </xsl:if>
      </xsl:for-each>
    </mods:namePart>
  </xsl:template>

  <xsl:template match="work:contributor-attributes">
    <xsl:apply-templates select="work:contributor-role" />
  </xsl:template>

  <!-- ORCID.org contributor roles to MARC relator codes mapping -->
  <xsl:template match="work:contributor-role">
    <mods:role>
      <mods:roleTerm type="code" authority="marcrelator">
        <xsl:choose>
          <xsl:when test=".='author'">aut</xsl:when>
          <xsl:when test=".='assignee'">asg</xsl:when>
          <xsl:when test=".='editor'">edt</xsl:when>
          <xsl:when test=".='chair-or-translator'">trl</xsl:when>
          <xsl:when test=".='co-investigator'">ctb</xsl:when>
          <xsl:when test=".='co-inventor'">ctb</xsl:when>
          <xsl:when test=".='graduate-student'">ctb</xsl:when>
          <xsl:when test=".='other-inventor'">ctb</xsl:when>
          <xsl:when test=".='principal-investigator'">rth</xsl:when>
          <xsl:when test=".='postdoctoral-researcher'">res</xsl:when>
          <xsl:when test=".='support-staff'">ctb</xsl:when>
          <xsl:otherwise>aut</xsl:otherwise>
        </xsl:choose>
      </mods:roleTerm>
    </mods:role>
  </xsl:template>

  <xsl:template match="common:contributor-orcid">
    <mods:nameIdentifier type="orcid">
      <xsl:value-of select="common:path" />
    </mods:nameIdentifier>
  </xsl:template>

  <xsl:template match="common:publication-date">
    <mods:originInfo>
      <mods:dateIssued encoding="w3cdtf">
        <xsl:value-of select="common:year/text()" />
        <xsl:if test="common:month/text()">
          <xsl:text>-</xsl:text>
          <xsl:value-of select='format-number(common:month/text(),"00"' />
          <xsl:if test="common:day/text()">
            <xsl:text>-</xsl:text>
            <xsl:value-of select='format-number(common:day/text(),"00' />
          </xsl:if>
        </xsl:if>
      </mods:dateIssued>
    </mods:originInfo>
  </xsl:template>

  <xsl:template match="work:short-description">
    <mods:abstract>
      <xsl:value-of select="text()" />
    </mods:abstract>
  </xsl:template>

  <xsl:template match="common:url">
    <mods:location>
      <mods:url>
        <xsl:value-of select="text()" />
      </mods:url>
    </mods:location>
  </xsl:template>

  <!-- DOI, OCLC, PubMed, PubMedCentral, URN to mods:identifier -->
  <xsl:template match="common:external-id[contains('doi oclc pmid pmc urn',common:external-id-type)]">
    <mods:identifier type="{common:external-id-type}">
      <xsl:value-of select="common:external-id-value/text()" />
    </mods:identifier>
  </xsl:template>
  
  <!-- External ID from SCOPUS to mods:identifier -->
  <xsl:template match="common:external-id[common:external-id-type='eid'][common:external-id-value[starts-with(.,'2-s2.0-')]]">
    <mods:identifier type="scopus">
      <xsl:value-of select="substring-after(common:external-id-value/text(),'2-s2.0-')" />
    </mods:identifier>
  </xsl:template>

  <!-- ISBN, ISSN to mods:identifier -->
  <xsl:template match="common:external-id[contains('isbn issn',common:external-id-type)]">
    <mods:identifier type="{common:external-id-type}">
      <xsl:value-of select="upper-case(common:external-id-value/text())" />
    </mods:identifier>
  </xsl:template>

  <!-- Ignore remaining IDs -->
  <xsl:template match="common:external-id" />
  <xsl:template match="common:external-ids" />

  <!-- Ignore last modification date -->
  <xsl:template match="common:last-modified-date" />

  <xsl:template match="common:language-code">
    <mods:language>
      <mods:languageTerm authority="rfc5646" type="code">
        <xsl:value-of select="document(concat('language:',text()))/language/@xmlCode" />
      </mods:languageTerm>
    </mods:language>
  </xsl:template>
  
  <xsl:template match="@*|text()" />

</xsl:stylesheet>
