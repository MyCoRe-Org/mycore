<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                exclude-result-prefixes="mets xlink">

  <xsl:key name="metsFlocat-by-lang" match="mets:file"
           use="substring-before(substring-after(mets:FLocat/@xlink:href,'tei/'), '/')"/>

  <xsl:output method="xml" indent="yes"/>
  <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'"/>
  <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>

  <xsl:template match="mets:fileGrp[@USE='TRANSLATION']">
    <xsl:apply-templates
        select="mets:file[generate-id() = generate-id(key('metsFlocat-by-lang', substring-before(substring-after(mets:FLocat/@xlink:href,'tei/'), '/'))[1])]"
        mode="group">
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="mets:file" mode="group">
    <xsl:variable name="current-use" select="substring-before(substring-after(mets:FLocat/@xlink:href,'tei/'), '/')" />
    <xsl:comment>Migrated fileGrp</xsl:comment>
    <mets:fileGrp USE="TEI.{translate($current-use, $lowercase, $uppercase)}">
      <xsl:apply-templates select="key('metsFlocat-by-lang', $current-use)"/>
    </mets:fileGrp>
  </xsl:template>


  <xsl:template match="mets:fileGrp[@USE='TRANSCRIPTION']">
    <xsl:comment>Migrated fileGrp</xsl:comment>
    <mets:fileGrp USE="TEI.TRANSCRIPTION">
      <xsl:apply-templates/>
    </mets:fileGrp>
  </xsl:template>

  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()'/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>