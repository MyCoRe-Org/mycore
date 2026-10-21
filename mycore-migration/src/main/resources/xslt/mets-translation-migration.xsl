<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mets="http://www.loc.gov/METS/"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:output method="xml" indent="yes" />

  <xsl:mode on-no-match="shallow-copy" />

  <!-- splits the translation files into one fileGrp per language, e.g. tei/de/... -->
  <xsl:template match="mets:fileGrp[@USE = 'TRANSLATION']">
    <xsl:for-each-group select="mets:file"
      group-by="substring-before(substring-after(mets:FLocat/@xlink:href, 'tei/'), '/')">
      <xsl:comment>Migrated fileGrp</xsl:comment>
      <mets:fileGrp USE="TEI.{upper-case(current-grouping-key())}">
        <xsl:apply-templates select="current-group()" />
      </mets:fileGrp>
    </xsl:for-each-group>
  </xsl:template>

  <xsl:template match="mets:fileGrp[@USE = 'TRANSCRIPTION']">
    <xsl:comment>Migrated fileGrp</xsl:comment>
    <mets:fileGrp USE="TEI.TRANSCRIPTION">
      <xsl:apply-templates />
    </mets:fileGrp>
  </xsl:template>

</xsl:stylesheet>
