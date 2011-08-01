<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions" exclude-result-prefixes="iview2">

  <xsl:include href="mcr-module-iview2.xsl" />

  <xsl:template name="iview2.getSupport">
    <xsl:param name="derivID" />
    <xsl:value-of select="iview2:getSupportedMainFile($derivID)" />
  </xsl:template>

  <xsl:template name="derivateView">
    <xsl:param name="derivateID" />
    <xsl:call-template name="derivateLinkView">
      <xsl:with-param name="derivateID" select="$derivateID"/>
      <xsl:with-param name="file" select="iview2:getSupportedMainFile($derivateID)"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="derivateLinkView">
    <xsl:param name="file" />
    <xsl:param name="derivateID" />
    <xsl:if test="$file != ''">
      <xsl:call-template name="iview2.init" />
      <xsl:call-template name="iview2.getViewer">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="style" select="'width:256px; height:256px;'" />
      </xsl:call-template>
      <xsl:call-template name="iview2.start">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="startFile" select="$file" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>