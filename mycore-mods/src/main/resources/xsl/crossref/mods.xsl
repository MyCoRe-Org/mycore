<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:cr="http://www.crossref.org/schema/4.4.1"
                version="3.0"
>

  <xsl:include href="crossref-helper-4.4.1.xsl"/>
  <xsl:include href="mods2journal.xsl" />
  <xsl:include href="mods2book.xsl" />

  <xsl:template match="/">
    <xsl:apply-templates select="mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods"/>
  </xsl:template>

  <xsl:template name="printFullTitle">
    <xsl:param name="titleInfoNode"/>
    <xsl:if test="$titleInfoNode">
      <xsl:apply-templates select="$titleInfoNode/mods:nonSort" mode="printFullTitle"/>
      <xsl:apply-templates select="$titleInfoNode/mods:title" mode="printFullTitle"/>
      <xsl:apply-templates select="$titleInfoNode/mods:subTitle" mode="printFullTitle"/>
      <xsl:apply-templates select="$titleInfoNode/mods:partNumber" mode="printFullTitle"/>
      <xsl:apply-templates select="$titleInfoNode/mods:partName" mode="printFullTitle"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:nonSort" mode="printFullTitle">
    <xsl:value-of select="text()" />
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="mods:title" mode="printFullTitle">
    <xsl:value-of select="text()" />
  </xsl:template>

  <xsl:template match="mods:subTitle" mode="printFullTitle">
    <xsl:text>: </xsl:text>
    <xsl:value-of select="text()" />
  </xsl:template>

  <xsl:template match="mods:partNumber|mods:partName" mode="printFullTitle">
    <xsl:value-of select="text()" />
    <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template name="publicationYear">
    <xsl:param name="modsNode"/>
    <xsl:variable name="publicationNode" select="$modsNode/mods:originInfo[@eventType='publication']"/>
    <xsl:if test="$publicationNode/mods:dateIssued[@encoding='w3cdtf']/text()">
      <cr:publication_date>
        <cr:year>
          <xsl:value-of
              select="substring-before($publicationNode/mods:dateIssued[@encoding='w3cdtf']/text(), '-')"/>
        </cr:year>
      </cr:publication_date>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>