<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
     version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:mods="http://www.loc.gov/mods/v3">

  <xsl:include href="copynodes.xsl" />
  <xsl:include href="mods-utils.xsl"/>

  <xsl:template match="mods:language/mods:languageTerm/@authority[. ='rfc4646']">
    <xsl:attribute name="authority">
      <xsl:text>rfc5646</xsl:text>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>
