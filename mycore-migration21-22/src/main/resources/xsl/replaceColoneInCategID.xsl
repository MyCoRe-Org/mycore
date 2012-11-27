<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink">
  <xsl:include href="copynodes.xsl" />
<!--   <xsl:template match="*[@class='MCRMetaClassification']/*[contains(@categid,':')]"> -->
  <xsl:template match="@categid[contains(.,':')]">
    <xsl:attribute name="categid">
        <xsl:value-of select="translate(.,':','-')" />
    </xsl:attribute>
  </xsl:template>
</xsl:stylesheet>