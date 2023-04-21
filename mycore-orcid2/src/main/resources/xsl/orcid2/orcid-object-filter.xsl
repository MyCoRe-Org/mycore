<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:mods="http://www.loc.gov/mods/v3"
  exclude-result-prefixes="xsl">

  <xsl:include href="copynodes.xsl" />

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

</xsl:stylesheet>
