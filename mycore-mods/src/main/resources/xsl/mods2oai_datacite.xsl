<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:oai_datacite="http://schema.datacite.org/oai/oai-1.0/"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  exclude-result-prefixes="xsl" >
  
  <xsl:include href="xslInclude:datacite" />

  <xsl:template match="mycoreobject">
    <oai_datacite:oai_datacite
      xsi:schemaLocation="http://schema.datacite.org/oai/oai-1.0/ http://schema.datacite.org/oai/oai-1.0/oai.xsd">
      <oai_datacite:isReferenceQuality>true</oai_datacite:isReferenceQuality>
      <oai_datacite:schemaVersion>3.1</oai_datacite:schemaVersion>
      <oai_datacite:payload>
        <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" />
      </oai_datacite:payload>
    </oai_datacite:oai_datacite>
  </xsl:template>

</xsl:stylesheet>
