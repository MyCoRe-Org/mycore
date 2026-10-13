<?xml version="1.0" encoding="UTF-8"?>

<!-- ======================================================================
 Converts MyCoRe/MODS to DataCite Schema, to register metadata for DOIs.
 See http://schema.datacite.org/meta/kernel-3/
 ====================================================================== -->

<xsl:stylesheet version="3.0"
  xmlns="http://datacite.org/schema/kernel-3"
  xmlns:mcrmods="http://www.mycore.de/xslt/mods"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:output method="xml" encoding="UTF-8" indent="yes" />

  <xsl:mode on-no-match="deep-skip" />

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="mycoreobject">
    <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" />
  </xsl:template>

  <xsl:template match="mods:mods">
    <resource xsi:schemaLocation="http://datacite.org/schema/kernel-3 http://schema.datacite.org/meta/kernel-3/metadata.xsd">
      <xsl:call-template name="identifier" />
      <xsl:call-template name="creators" />
      <xsl:call-template name="titles" />
      <xsl:call-template name="publisher" />
      <xsl:call-template name="publicationYear" />
      <xsl:call-template name="contributors" />
      <xsl:call-template name="subjects" />
      <xsl:call-template name="dates" />
      <xsl:call-template name="language" />
      <xsl:call-template name="resourceType" />
      <xsl:call-template name="descriptions" />
      <xsl:call-template name="alternateIdentifiers" />
    </resource>
  </xsl:template>

  <!-- ========== identifier (1) ========== -->

  <xsl:template name="identifier">
    <identifier identifierType="DOI">
      <xsl:choose>
        <xsl:when test="mods:identifier[@type='doi']">
          <xsl:apply-templates select="mods:identifier[@type='doi']" mode="identifier" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="ancestor::mycoreobject/service/servdates/servdate[@type='createdate']" mode="identifier" />
        </xsl:otherwise>
      </xsl:choose>
    </identifier>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='doi']" mode="identifier">
    <xsl:choose>
      <xsl:when test="starts-with(text(),'doi:')">
        <xsl:value-of select="substring-after(text(),'doi:')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mycoreobject/service/servdates/servdate[@type='createdate']" mode="identifier">
    <xsl:variable name="pattern" select="(mcrproperty:one('MCR.DOI.NISSPattern'), 'yyyyMMdd-HHmmss')[1]" />
    <xsl:variable name="formatISODate"
      select="string-join(('callJava:org.mycore.common.xml.MCRXMLFunctions:formatISODate',
        encode-for-uri(.), encode-for-uri($pattern), encode-for-uri($CurrentLang)), ':')" />
    <xsl:value-of select="mcrproperty:one('MCR.DOI.Prefix')" />
    <xsl:text>/</xsl:text>
    <xsl:value-of select="document($formatISODate)" />
  </xsl:template>

  <!-- ========== titles (1-n) ========== -->

  <xsl:template name="titles">
    <titles>
      <xsl:apply-templates select="mods:titleInfo" />
    </titles>
  </xsl:template>

  <xsl:template match="mods:titleInfo">
    <title>
      <xsl:copy-of select="@xml:lang" />
      <xsl:apply-templates select="@type" />
      <xsl:apply-templates select="mods:nonSort" />
      <xsl:apply-templates select="mods:title" />
      <xsl:apply-templates select="mods:subTitle" />
      <xsl:apply-templates select="mods:partNumber" />
      <xsl:apply-templates select="mods:partName" />
    </title>
  </xsl:template>

  <xsl:template match="mods:titleInfo/@type">
    <xsl:choose>
      <xsl:when test=".='translated'">
        <xsl:attribute name="titleType">TranslatedTitle</xsl:attribute>
      </xsl:when>
      <xsl:when test=".='alternative'">
        <xsl:attribute name="titleType">AlternativeTitle</xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:nonSort">
    <xsl:value-of select="text()" />
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="mods:title">
    <xsl:value-of select="text()" />
  </xsl:template>

  <xsl:template match="mods:subTitle">
    <xsl:text>: </xsl:text>
    <xsl:value-of select="text()" />
  </xsl:template>

  <xsl:template match="mods:partNumber|mods:partName">
    <xsl:value-of select="text()" />
    <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- ========== creators (1-n) ========== -->

  <xsl:template name="creators">
    <creators>
      <xsl:choose>
        <xsl:when test="mods:name[mods:role/mods:roleTerm='aut']"><xsl:apply-templates select="mods:name[mods:role/mods:roleTerm='aut']" /></xsl:when>
        <xsl:when test="mods:name[mods:role/mods:roleTerm='cre']"><xsl:apply-templates select="mods:name[mods:role/mods:roleTerm='cre']" /></xsl:when>
        <xsl:when test="mods:name[mods:role/mods:roleTerm='edt']"><xsl:apply-templates select="mods:name[mods:role/mods:roleTerm='edt']" /></xsl:when>
      </xsl:choose>
    </creators>
  </xsl:template>

  <xsl:template match="mods:name">
    <xsl:if test="mods:displayForm or @valueURI">
      <creator>
        <creatorName>
          <xsl:choose>
            <xsl:when test="mods:displayForm">
              <xsl:value-of select="mods:displayForm" />
            </xsl:when>
            <xsl:otherwise>
              <!-- labels of the first and last category in the parent path of the classification -->
              <xsl:variable name="categories"
                select="mcrmods:to-mycoreclass(., 'parent')//category[position() = 1 or position() = last()]" />
              <xsl:value-of select="string-join($categories ! string(label[lang($CurrentLang)]/@text), ', ')" />
            </xsl:otherwise>
          </xsl:choose>
        </creatorName>
      </creator>
    </xsl:if>
  </xsl:template>

  <!-- ========== publisher (1) ========== -->

  <xsl:template name="publisher">
    <publisher>
      <xsl:choose>
        <xsl:when test="mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher">
          <xsl:value-of select="(mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher)[1]" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="mcrproperty:one('MCR.DOI.HostingInstitution')" />
        </xsl:otherwise>
      </xsl:choose>
    </publisher>
  </xsl:template>

  <!-- ========== publicationYear (1) ========== -->

  <xsl:template name="publicationYear">
    <xsl:choose>
      <xsl:when test="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='w3cdtf']">
        <xsl:apply-templates select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='w3cdtf']" mode="publicationYear" />
      </xsl:when>
      <xsl:when test="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='marc']">
        <xsl:apply-templates select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='marc']" mode="publicationYear" />
      </xsl:when>
      <xsl:when test="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateCreated">
        <xsl:apply-templates select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateCreated" mode="publicationYear" />
      </xsl:when>
      <xsl:when test="mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='w3cdtf']">
        <xsl:apply-templates select="mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='w3cdtf']" mode="publicationYear" />
      </xsl:when>
      <xsl:when test="mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='marc']">
        <xsl:apply-templates select="mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='marc']" mode="publicationYear" />
      </xsl:when>
      <xsl:when test="mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateCreated">
        <xsl:apply-templates select="mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateCreated" mode="publicationYear" />
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:dateCreated|mods:dateIssued" mode="publicationYear">
    <publicationYear>
      <xsl:value-of select="substring(text(),1,4)" />
    </publicationYear>
  </xsl:template>


  <!-- ========== contributors (0-n) ========== -->
  <xsl:template name="contributors">
    <contributors>
      <xsl:call-template name="hostingInstitution" />
      <xsl:if test="mods:identifier[@type='project'][contains(text(), 'FP7')]" >
        <xsl:call-template name="fundingInformation" />
      </xsl:if>
    </contributors>
  </xsl:template>

  <xsl:template name="hostingInstitution">
    <contributor contributorType="HostingInstitution">
      <contributorName>
        <xsl:value-of select="mcrproperty:one('MCR.DOI.HostingInstitution')" />
      </contributorName>
    </contributor>
  </xsl:template>

  <xsl:template name="fundingInformation">
    <contributor contributorType="Funder">
      <contributorName>
        <xsl:value-of select="'European Commission'" />
      </contributorName>
      <nameIdentifier nameIdentifierScheme="info">
        <xsl:value-of select="mods:identifier[@type='project'][contains(text(), 'FP7')][1]" />
      </nameIdentifier>
    </contributor>
  </xsl:template>


  <!-- ========== subjects (0-n)========== -->

  <xsl:template name="subjects">
    <xsl:if test="mods:subject/mods:topic">
      <subjects>
        <xsl:apply-templates select="mods:subject/mods:topic" />
      </subjects>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:subject/mods:topic">
    <subject>
      <xsl:value-of select="text()" />
    </subject>
  </xsl:template>


  <!-- ========== language (0-n) ========== -->

  <xsl:template name="language">
    <xsl:if test="mods:language/mods:languageTerm[@authority='rfc5646' and @type='code']">
      <language>
        <xsl:value-of select="(mods:language/mods:languageTerm[@authority='rfc5646' and @type='code'])[1]" />
      </language>
    </xsl:if>
  </xsl:template>

  <!-- ========== resourceType (0-n) ========== -->

  <xsl:template name="resourceType">
    <resourceType resourceTypeGeneral="Text">
      <xsl:value-of select="substring-after((mods:genre/@valueURI)[1], '#')" />
    </resourceType>
  </xsl:template>

  <!-- ========== descriptions (0-n) ========== -->

  <xsl:template name="descriptions">
    <xsl:if test="mods:abstract">
      <descriptions>
        <xsl:apply-templates select="mods:abstract" />
      </descriptions>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:abstract">
    <description descriptionType="Abstract">
      <xsl:copy-of select="@xml:lang" />
      <xsl:value-of select="text()" />
    </description>
  </xsl:template>

  <!-- ========== dates (0-n) ========== -->

  <xsl:template name="dates">
    <dates>
      <xsl:apply-templates select="mods:originInfo[not(@eventType) or @eventType='publication']
        /(mods:dateOther|mods:dateIssued|mods:dateCreated|mods:dateModified)" />
    </dates>
  </xsl:template>

  <xsl:template match="mods:dateIssued[@encoding='w3cdtf']">
    <date dateType="Issued">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateCreated[@encoding='w3cdtf']">
    <date dateType="Created">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateModified[@encoding='w3cdtf']">
    <date dateType="Updated">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateOther[@type='accepted'][@encoding='w3cdtf']">
    <date dateType="Accepted">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateOther[@type='submitted'][@encoding='w3cdtf']">
    <date dateType="Submitted">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <!-- ========== alternateIdentifiers (0-n) ========== -->

  <xsl:template name="alternateIdentifiers">
    <alternateIdentifiers>
      <xsl:apply-templates select="mods:identifier[@type='urn']" />
      <xsl:apply-templates select="ancestor::mycoreobject/@ID" />
    </alternateIdentifiers>
  </xsl:template>

  <xsl:template match="mods:identifier[not(@type='doi')]">
    <alternateIdentifier alternateIdentifierType="{upper-case(@type)}">
      <xsl:value-of select="." />
    </alternateIdentifier>
  </xsl:template>

  <xsl:template match="mycoreobject/@ID">
    <alternateIdentifier alternateIdentifierType="MyCoRe">
      <xsl:value-of select="." />
    </alternateIdentifier>
  </xsl:template>

</xsl:stylesheet>
