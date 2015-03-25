<?xml version="1.0" encoding="UTF-8"?>

<!-- ======================================================================
 Converts MyCoRe/MODS to DataCite Schema, to register metadata for DOIs.
 See http://schema.datacite.org/meta/kernel-3/
 ====================================================================== -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="http://datacite.org/schema/kernel-3"
  exclude-result-prefixes="xsl xlink mods xalan">

  <xsl:include href="coreFunctions.xsl" />

  <xsl:output method="xml" encoding="UTF-8" indent="yes" xalan:indent-amount="2" />

  <xsl:param name="MCR.DOI.Prefix" select="''" />
  <xsl:param name="MCR.DOI.HostingInstitution" select="''" />
  <xsl:param name="MCR.DOI.NISSPattern" select="'yyyyMMdd-HHmmss'" />

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
      <xsl:call-template name="hostingInstitution" />
      <xsl:call-template name="subjects" />
      <xsl:call-template name="dates" />
      <xsl:apply-templates select="mods:language" />
      <xsl:call-template name="resourceType" />
      <xsl:call-template name="descriptions" />
      <xsl:call-template name="alternateIdentifiers" />
    </resource>
  </xsl:template>

  <!-- ========== identifier ========== -->

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
    <xsl:value-of select="$MCR.DOI.Prefix" />
    <xsl:text>/</xsl:text>
    <xsl:call-template name="formatISODate">
      <xsl:with-param name="date" select="." />
      <xsl:with-param name="format" select="$MCR.DOI.NISSPattern" />
    </xsl:call-template>
  </xsl:template>

  <!-- ========== titles ========== -->

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

  <!-- ========== creators ========== -->

  <xsl:template name="creators">
    <creators>
      <xsl:apply-templates select="mods:name" />
    </creators>
  </xsl:template>

  <xsl:template match="mods:name">
    <xsl:variable name="marcrelator" select="mods:role/mods:roleTerm[@type='code' and @authority='marcrelator']" />
    <xsl:variable name="rolepath" select="document(concat('classification:editor:-1:parents:marcrelator:',$marcrelator))" />
    <xsl:if test="$rolepath/items/item/@value='cre'">
      <creator>
        <creatorName>
          <xsl:value-of select="mods:displayForm" />
        </creatorName>
      </creator>
    </xsl:if>
  </xsl:template>

  <!-- ========== publisher ========== -->

  <xsl:template name="publisher">
    <publisher>
      <xsl:choose>
        <xsl:when test="mods:originInfo/mods:publisher">
          <xsl:value-of select="mods:originInfo/mods:publisher" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$MCR.DOI.HostingInstitution" />
        </xsl:otherwise>
      </xsl:choose>
    </publisher>
  </xsl:template>

  <!-- ========== publicationYear ========== -->

  <xsl:template name="publicationYear">
    <xsl:choose>
      <xsl:when test="mods:originInfo/mods:dateIssued">
        <xsl:apply-templates select="mods:originInfo/mods:dateIssued" mode="publicationYear" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="mods:originInfo/mods:dateCreated" mode="publicationYear" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:dateCreated|mods:dateIssued" mode="publicationYear">
    <publicationYear>
      <xsl:value-of select="substring(text(),1,4)" />
    </publicationYear>
  </xsl:template>

  <!-- ========== hostingInstitution ========== -->

  <xsl:template name="hostingInstitution">
    <contributors>
      <contributor contributorType="HostingInstitution">
        <contributorName>
          <xsl:value-of select="$MCR.DOI.HostingInstitution" />
        </contributorName>
      </contributor>
    </contributors>
  </xsl:template>

  <!-- ========== subjects ========== -->

  <xsl:template name="subjects">
    <subjects>
      <xsl:apply-templates select="mods:subject/mods:topic" />
    </subjects>
  </xsl:template>

  <xsl:template match="mods:subject/mods:topic">
    <subject>
      <xsl:value-of select="text()" />
    </subject>
  </xsl:template>

  <!-- ========== language ========== -->

  <xsl:template match="mods:language[mods:languageTerm[@authority='rfc4646' and @type='code']]">
    <language>
      <xsl:variable name="uri" select="concat('language:',mods:languageTerm[@authority='rfc4646' and @type='code'])" />
      <xsl:value-of select="document($uri)/language/@biblCode" />
    </language>
  </xsl:template>

  <!-- ========== resourceType ========== -->

  <xsl:template name="resourceType">
    <resourceType resourceTypeGeneral="Text" />
  </xsl:template>

  <!-- ========== descriptions ========== -->

  <xsl:template name="descriptions">
    <descriptions>
      <xsl:apply-templates select="mods:abstract" />
    </descriptions>
  </xsl:template>

  <xsl:template match="mods:abstract">
    <description descriptionType="Abstract">
      <xsl:copy-of select="@xml:lang" />
      <xsl:value-of select="text()" />
    </description>
  </xsl:template>

  <!-- ========== dates ========== -->

  <xsl:template name="dates">
    <dates>
      <xsl:apply-templates select="mods:originInfo/mods:dateOther|mods:originInfo/mods:dateIssued|mods:originInfo/mods:dateCreated|mods:originInfo/mods:dateModified" />
    </dates>
  </xsl:template>

  <xsl:template match="mods:dateIssued">
    <date dateType="Issued">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateCreated">
    <date dateType="Created">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateModified">
    <date dateType="Updated">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateOther[@type='accepted']">
    <date dateType="Accepted">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <xsl:template match="mods:dateOther[@type='submitted']">
    <date dateType="Submitted">
      <xsl:value-of select="text()" />
    </date>
  </xsl:template>

  <!-- ========== alternateIdentifiers ========== -->

  <xsl:template name="alternateIdentifiers">
    <alternateIdentifiers>
      <xsl:apply-templates select="mods:identifier[@type='urn']" />
      <xsl:apply-templates select="ancestor::mycoreobject/@ID" />
    </alternateIdentifiers>
  </xsl:template>

  <xsl:template match="mods:identifier[not(@type='doi')]">
    <alternateIdentifier alternateIdentifierType="{translate(@type,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')}">
      <xsl:value-of select="." />
    </alternateIdentifier>
  </xsl:template>

  <xsl:template match="mycoreobject/@ID">
    <alternateIdentifier alternateIdentifierType="MyCoRe">
      <xsl:value-of select="." />
    </alternateIdentifier>
  </xsl:template>

  <!-- ========== ignore the rest ========== -->

  <xsl:template match="*|@*" />

</xsl:stylesheet>
