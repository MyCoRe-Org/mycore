<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
  xmlns="http://www.openarchives.org/OAI/2.0/"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:oai_datacite="http://schema.datacite.org/oai/oai-1.0/"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:mcrurn="xalan://org.mycore.urn.MCRXMLFunctions"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mcr="xalan://org.mycore.common.xml.MCRXMLFunctions"
  exclude-result-prefixes="xsl xlink mods mcrurn mcr"
>

  <xsl:param name="ServletsBaseURL" select="''" />
  <xsl:param name="WebApplicationBaseURL" select="''" />
  <xsl:param name="HttpSession" select="''" />
  <xsl:param name="MCR.URN.Resolver.MasterURL" select="''" />

  <xsl:include href="mods2mods.xsl" />
  <xsl:include href="mods2record.xsl" />
  <xsl:include href="mods-utils.xsl" />

  <xsl:template match="mycoreobject" mode="metadata">
    <xsl:apply-templates mode="mods2mods" />
  </xsl:template>

</xsl:stylesheet>
