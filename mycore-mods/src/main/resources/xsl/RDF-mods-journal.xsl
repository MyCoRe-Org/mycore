<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:bibo="http://purl.org/ontology/bibo/" xmlns:foaf="http://xmlns.com/foaf/0.1/"
  xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/"
  xmlns:zdb="http://ld.zdb-services.de/resource/" xmlns:dnb_intern="http://dnb.de/" xmlns:isbd="http://iflastandards.info/ns/isbd/elements/"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" exclude-result-prefixes="rdf bibo foaf owl dc dcterms zdb dnb_intern isbd rdfs">
  <xsl:template match="/rdf:RDF">
    <mycoreobject>
      <metadata>
        <def.modsContainer class="MCRMetaXML" heritable="false" notinherit="true">
          <modsContainer inherited="0">
            <xsl:apply-templates select="bibo:Periodical" />
          </modsContainer>
        </def.modsContainer>
      </metadata>
    </mycoreobject>
  </xsl:template>
  <xsl:template match="bibo:Periodical">
    <mods:mods>
      <xsl:apply-templates />
      <mods:identifier type="zdbid">
        <xsl:value-of select="substring-after(@rdf:about, 'http://ld.zdb-services.de/resource/')" />
      </mods:identifier>
      <mods:titleInfo type="translated" xml:lang="en" xlink:type="simple">
        <mods:title><!-- Titel übersetzt --></mods:title>
      </mods:titleInfo>
    </mods:mods>
  </xsl:template>
  <xsl:template match="dcterms:title">
    <mods:titleInfo altRepGroup="1" xlink:type="simple">
      <mods:title>
        <xsl:value-of select="." />
      </mods:title>
    </mods:titleInfo>
  </xsl:template>
  <xsl:template match="bibo:shortTitle">
    <mods:titleInfo displayLabel="Short form of the title" type="alternative" xlink:type="simple">
      <mods:title>
        <xsl:value-of select="." />
      </mods:title>
    </mods:titleInfo>
  </xsl:template>
  <xsl:template match="dc:publisher">
    <mods:originInfo>
      <mods:publisher>
        <xsl:value-of select="." />
      </mods:publisher>
    </mods:originInfo>
  </xsl:template>
  <xsl:template match="bibo:issn">
    <mods:identifier type="issn">
      <xsl:value-of select="." />
    </mods:identifier>
  </xsl:template>
</xsl:stylesheet>