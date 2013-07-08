<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:bibo="http://purl.org/ontology/bibo/"
  xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:dcterms="http://purl.org/dc/terms/" xmlns:zdb="http://ld.zdb-services.de/resource/" xmlns:dnb_intern="http://dnb.de/"
  xmlns:isbd="http://iflastandards.info/ns/isbd/elements/" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" exclude-result-prefixes="rdf bibo foaf owl dc dcterms zdb dnb_intern isbd rdfs">
  <xsl:include href="xslInclude:RDF-mods-journal"/>
  <xsl:template match="/rdf:RDF">
    <mycoreobject>
      <metadata>
        <def.modsContainer class="MCRMetaXML" heritable="false" notinherit="true">
          <modsContainer inherited="0">
            <xsl:apply-templates select="rdf:Description" />
          </modsContainer>
        </def.modsContainer>
      </metadata>
    </mycoreobject>
  </xsl:template>
  <xsl:template match="rdf:Description">
    <mods:mods>
      <!-- process input in deterministic order as editor forms depend on it -->
      <xsl:apply-templates select="@rdf:type" />
      <xsl:apply-templates select="dc:title" />
      <xsl:apply-templates select="isbd:p1005|isbd:P1005" />
      <xsl:apply-templates select="bibo:shortTitle" />
      <xsl:if test="dc:publisher | isbd:p1016 | isbd:P1016">
        <mods:originInfo>
          <xsl:apply-templates select="dc:publisher" />
          <xsl:apply-templates select="isbd:p1016|isbd:P1016" />
        </mods:originInfo>
      </xsl:if>
      <xsl:apply-templates select="bibo:issn" />
      <mods:identifier type="zdbid">
        <xsl:value-of select="substring-after(@rdf:about, 'http://ld.zdb-services.de/resource/')" />
      </mods:identifier>
    </mods:mods>
  </xsl:template>
  <xsl:template match="dc:title">
    <mods:titleInfo altRepGroup="1" xlink:type="simple">
      <mods:title>
        <xsl:value-of select="." />
      </mods:title>
    </mods:titleInfo>
  </xsl:template>
  <xsl:template match="isbd:p1005|isbd:P1005">
    <mods:titleInfo type="translated" xml:lang="en">
      <mods:title>
        <xsl:value-of select="." />
      </mods:title>
    </mods:titleInfo>
  </xsl:template>
  <xsl:template match="bibo:shortTitle">
    <mods:titleInfo displayLabel="Short form of the title" type="alternative">
      <mods:title>
        <xsl:value-of select="." />
      </mods:title>
    </mods:titleInfo>
  </xsl:template>
  <xsl:template match="dc:publisher">
    <mods:publisher>
      <xsl:value-of select="." />
    </mods:publisher>
  </xsl:template>
  <xsl:template match="isbd:p1016|isbd:P1016">
    <mods:place>
      <mods:placeTerm type="text">
        <xsl:value-of select="." />
      </mods:placeTerm>
    </mods:place>
  </xsl:template>
  <xsl:template match="bibo:issn">
    <mods:identifier type="issn">
      <xsl:value-of select="." />
    </mods:identifier>
  </xsl:template>
  <xsl:template match="@rdf:type">
    <xsl:message>
      <xsl:value-of select="'Configure MCR.URIResolver.xslIncludes.RDF-mods-journal and overwrite template for @rdf:type.'"/>
    </xsl:message>
  </xsl:template>
</xsl:stylesheet>