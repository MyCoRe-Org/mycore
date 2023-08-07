<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
>
  <xsl:include href="default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />
  <xsl:include href="xslInclude:components-3" />

  <xsl:variable name="direction">
    <xsl:value-of select="mcri18n:text-direction($CurrentLang)" />
  </xsl:variable>

  <xsl:template match="/">
    <site title="{$PageTitle}">
      <xsl:apply-templates />
    </site>
  </xsl:template>
  
</xsl:stylesheet>