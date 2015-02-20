<?xml version="1.0" encoding="UTF-8"?>

<!-- ======================================================================
  Transforms MODS metadata to Highwire Press tags for the HTML <head>,
  for example

  <meta name="citation_author" content="Doe, John" />

  These tags may be embedded into any HTML frontpage outputting metadata.
  The tags are indexed by Google Scholar, see their inclusion guidelines:
  http://scholar.google.com/intl/en/scholar/inclusion.html

  Usage:
  <xsl:apply-templates select="mods:mods" mode="highwire" />
======================================================================  -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mods="http://www.loc.gov/mods/v3"
  exclude-result-prefixes="xsl mods"
>

  <xsl:template match="mods:mods" mode="highwire">
    <xsl:apply-templates select="mods:titleInfo" mode="highwire" />
    <xsl:apply-templates select="mods:name" mode="highwire" />
    <xsl:apply-templates select="mods:originInfo" mode="highwire" />
    <xsl:apply-templates select="mods:identifier" mode="highwire" />
    <xsl:apply-templates select="mods:relatedItem" mode="highwire" />
  </xsl:template>

  <!-- ========== citation_title ========== -->
  <!-- ========== citation_journal_title ========== -->

  <xsl:template match="mods:titleInfo" mode="highwire">
    <meta>
      <xsl:attribute name="name">
        <xsl:choose>
          <xsl:when test="parent::mods:relatedItem[@type='host']">citation_journal_title</xsl:when>
          <xsl:otherwise>citation_title</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="content">
        <xsl:apply-templates mode="highwire" />
      </xsl:attribute>
    </meta>
  </xsl:template>

  <xsl:template match="mods:nonSort" mode="highwire">
    <xsl:value-of select="." />
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="mods:title" mode="highwire">
    <xsl:value-of select="." />
  </xsl:template>

  <xsl:template match="mods:subTitle" mode="highwire">
    <xsl:text>: </xsl:text>
    <xsl:value-of select="." />
  </xsl:template>

  <xsl:template match="mods:partNumber" mode="highwire">
    <xsl:text>. </xsl:text>
    <xsl:value-of select="." />
  </xsl:template>

  <xsl:template match="mods:partName" mode="highwire">
    <xsl:text>: </xsl:text>
    <xsl:value-of select="." />
  </xsl:template>

  <!-- ========== citation_conference_title ========== -->

  <xsl:template match="mods:name[@type='conference']" mode="highwire">
    <meta name="citation_conference_title">
      <xsl:apply-templates select="mods:namePart" mode="highwire.content" />
    </meta>
  </xsl:template>

  <!-- ========== citation_author ========== -->

  <xsl:template match="mods:name[@type='personal']" mode="highwire">
    <xsl:variable name="roles">
      <xsl:for-each select="mods:role/mods:roleTerm[@type='code' and @authority='marcrelator']">
        <xsl:for-each select="document(concat('classification:editor:0:parents:marcrelator:',.))/descendant::item">
          <xsl:value-of select="concat(' ',@value,' ')" />
        </xsl:for-each>
      </xsl:for-each>
    </xsl:variable>

    <xsl:if test="contains($roles,' cre ')">
      <meta name="citation_author">
        <xsl:attribute name="content">
        <xsl:choose>
          <xsl:when test="mods:namePart[@type='family']">
            <xsl:value-of select="mods:namePart[@type='family']" />
            <xsl:for-each select="mods:namePart[@type='given']">
              <xsl:text>, </xsl:text>
              <xsl:value-of select="." />
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="mods:namePart">
            <xsl:value-of select="mods:namePart" />
          </xsl:when>
          <xsl:when test="mods:displayForm">
            <xsl:value-of select="mods:displayForm" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="text()|*" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      </meta>
    </xsl:if>
  </xsl:template>

  <!-- ========== citation_publication_date ========== -->

  <xsl:template match="mods:originInfo[mods:dateIssued or mods:dateCreated or mods:dateModified]" mode="highwire">
    <meta name="citation_publication_date">
      <xsl:attribute name="content">
        <xsl:choose>
          <xsl:when test="mods:dateIssued">
            <xsl:apply-templates select="mods:dateIssued" mode="highwire" />
          </xsl:when>
          <xsl:when test="mods:dateCreated">
            <xsl:apply-templates select="mods:dateCreated" mode="highwire" />
          </xsl:when>
          <xsl:when test="mods:dateModified">
            <xsl:apply-templates select="mods:dateModified" mode="highwire" />
          </xsl:when>
        </xsl:choose>
      </xsl:attribute>
    </meta>
  </xsl:template>

  <!-- FIXME: workaround while some mods applications have no encoding attribute -->
  <xsl:template match="mods:dateIssued | mods:dateCreated | mods:dateModified" mode="highwire">
    <xsl:value-of select="translate(text(),'-','/')" />
  </xsl:template>

  <xsl:template match="mods:*[@encoding='w3cdtf']" mode="highwire">
    <xsl:value-of select="translate(text(),'-','/')" />
  </xsl:template>

  <!-- ========== citation_issn ========== -->
  <!-- ========== citation_isbn ========== -->

  <xsl:template match="mods:identifier[contains('isbn issn doi',@type)]" mode="highwire">
    <meta name="citation_{@type}">
      <xsl:apply-templates select="." mode="highwire.content" />
    </meta>
  </xsl:template>

  <!-- ========== journal, volume, issue, pages ========== -->

  <xsl:template match="mods:relatedItem[@type='host']" mode="highwire">
    <xsl:apply-templates select="mods:titleInfo" mode="highwire" />
    <xsl:apply-templates select="mods:originInfo" mode="highwire" />
    <xsl:apply-templates select="mods:identifier" mode="highwire" />
    <xsl:apply-templates select="mods:part" mode="highwire" />
  </xsl:template>

  <xsl:template match="mods:part" mode="highwire">
    <xsl:apply-templates select="mods:detail|mods:extent" mode="highwire" />
  </xsl:template>

  <!-- ========== citation_volume ========== -->

  <xsl:template match="mods:detail[@type='volume']" mode="highwire">
    <meta name="citation_volume">
      <xsl:apply-templates select="mods:number" mode="highwire.content" />
    </meta>
  </xsl:template>

  <!-- ========== citation_issue ========== -->

  <xsl:template match="mods:detail[@type='issue']" mode="highwire">
    <meta name="citation_issue">
      <xsl:apply-templates select="mods:number" mode="highwire.content" />
    </meta>
  </xsl:template>

  <xsl:template match="mods:extent[starts-with(@unit,'page')]" mode="highwire">
    <xsl:apply-templates select="mods:start|mods:end" mode="highwire" />
  </xsl:template>

  <!-- ========== citation_firstpage ========== -->

  <xsl:template match="mods:start" mode="highwire">
    <meta name="citation_firstpage">
      <xsl:apply-templates select="." mode="highwire.content" />
    </meta>
  </xsl:template>

  <!-- ========== citation_lastpage ========== -->

  <xsl:template match="mods:end" mode="highwire">
    <meta name="citation_lastpage">
      <xsl:apply-templates select="." mode="highwire.content" />
    </meta>
  </xsl:template>

  <!-- ========== internal templates ========== -->

  <xsl:template match="*" mode="highwire.content">
    <xsl:attribute name="content"><xsl:value-of select="." /></xsl:attribute>
  </xsl:template>

  <!-- ========== ignore all other content ========== -->

  <xsl:template match="*|@*|text()" mode="highwire" />

</xsl:stylesheet>
