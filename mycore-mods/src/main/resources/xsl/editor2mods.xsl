<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:mcr="http://www.mycore.org/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport" exclude-result-prefixes="xlink mods mcrxsl mcrmods mcr" version="1.0">
  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@mcr:classId" />
  <xsl:template match="@mcr:categId" />
  <xsl:template match="*[@mcr:classId]">
    <xsl:variable name="classNodes" select="mcrmods:getClassNodes(.)" />
    <xsl:apply-templates select='@*|node()|$classNodes/@*|$classNodes/node()' />
  </xsl:template>

</xsl:stylesheet>