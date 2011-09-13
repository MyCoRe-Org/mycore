<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:mcr="http://www.mycore.org/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport" exclude-result-prefixes="xlink mods mcrxsl mcrmods mcr" version="1.0">
  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

  <xsl:template match='*' mode="copy">
    <xsl:element name="{local-name()}" namespace="{namespace-uri()}">
      <xsl:apply-templates select="@*|node()" mode="copy" />
    </xsl:element>
  </xsl:template>

  <xsl:template match='@*' mode="copy">
    <xsl:attribute name="{local-name()}" namespace="{namespace-uri()}">
      <xsl:value-of select="." />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match='text()' mode="copy">
    <xsl:value-of select="." />
  </xsl:template>

  <xsl:template match="@mcr:classId" />
  <xsl:template match="@mcr:categId" />
  <xsl:template match="*[@mcr:classId]">
    <xsl:copy>
      <xsl:variable name="classNodes" select="mcrmods:getClassNodes(.)" />
      <xsl:apply-templates select='$classNodes/@*|$classNodes/node()' mode="copy" />
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>