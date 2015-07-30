<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="i18n"
>
  <xsl:include href="layout-utils.xsl" />
  <xsl:template match="/">
    <site title="{$PageTitle}">
      <xsl:apply-templates />
    </site>
  </xsl:template>

</xsl:stylesheet>