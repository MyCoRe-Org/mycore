<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.openarchives.org/OAI/2.0/"
  xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <xsl:output method="xml" encoding="UTF-8" />
  <xsl:param name="MCR.Metadata.DefaultLang" />

  <xsl:template match="/mycoreclass">
    <ListSets>
      <xsl:apply-templates select="categories//category">
        <xsl:with-param name="prefix" select="@ID" />
      </xsl:apply-templates>
    </ListSets>
  </xsl:template>

  <xsl:template match="category">
    <xsl:param name="prefix" />
    <xsl:variable name="id" select="concat($prefix,':',@ID)" />
    <set>
      <setSpec>
        <xsl:value-of select="$id" />
      </setSpec>
      <xsl:choose>
        <xsl:when test="label[lang('en')]">
          <!-- preferred for DINI 2013 -->
          <xsl:apply-templates select="label[lang('en')]" />
        </xsl:when>
        <xsl:when test="label[lang($MCR.Metadata.DefaultLang)]">
          <xsl:apply-templates select="label[lang($MCR.Metadata.DefaultLang)]" />
        </xsl:when>
        <xsl:when test="label[lang('de')]">
          <!-- MyCoRe default -->
          <xsl:apply-templates select="label[lang('de')]" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="label[1]" />
        </xsl:otherwise>
      </xsl:choose>
    </set>
  </xsl:template>

  <xsl:template match="label">
    <setName>
      <xsl:value-of select="@text" />
    </setName>
    <xsl:apply-templates select="@description" />
  </xsl:template>

  <xsl:template match="@description">
    <xsl:message>
      Description: <xsl:value-of select="." />
    </xsl:message>
    <setDescription>
      <oai_dc:dc xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
        <dc:description>
          <xsl:value-of select="." />
        </dc:description>
      </oai_dc:dc>
    </setDescription>
  </xsl:template>

</xsl:stylesheet>
