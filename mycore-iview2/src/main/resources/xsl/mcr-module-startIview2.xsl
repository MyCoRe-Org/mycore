<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions">
  <xsl:variable name="jai.available" select="mcrxml:classAvailable('javax.media.jai.JAI')" />

  <xsl:include href="mcr-module-iview2.xsl" />

  <xsl:template name="iview2.getSupport">
    <xsl:param name="derivID" />
    <xsl:value-of xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions" select="iview2:getSupportedMainFile($derivID)" />
  </xsl:template>

  <xsl:template name="derivateView">
    <xsl:param name="derivateID" />
    <xsl:variable name="supportedMainFile">
      <xsl:call-template name="iview2.getSupport">
        <xsl:with-param name="derivID" select="$derivateID" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="$supportedMainFile != ''">
      <xsl:call-template name="iview2.init">
        <xsl:with-param name="groupID" select="$derivateID" />
      </xsl:call-template>
      <xsl:call-template name="iview2.getThumbnail">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="parent" select="'viewer'" />
      </xsl:call-template>
      <xsl:call-template name="iview2.getChapter">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="parent" select="'viewer'" />
      </xsl:call-template>
      <xsl:call-template name="iview2.getViewer">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="zoomBar" select="'false'" />
        <xsl:with-param name="chapter" select="'true'" />
        <xsl:with-param name="cutOut" select="'true'" />
        <xsl:with-param name="overview" select="'true'" />
        <xsl:with-param name="style" select="'width:256px; height:256px;'" />
      </xsl:call-template>
      <xsl:call-template name="iview2.start">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="style" select="'default'" />
        <xsl:with-param name="startFile" select="$supportedMainFile" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="derivateLinkView">
    <xsl:param name="file" />
    <xsl:param name="derivateID" />
    <!-- MCR IView2 ..start -->
    <xsl:if test="$file != ''">
      <xsl:call-template name="iview2.init">
        <xsl:with-param name="groupID" select="$derivateID" />
      </xsl:call-template>
      <xsl:call-template name="iview2.getThumbnail">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="parent" select="viewer" />
      </xsl:call-template>
      <xsl:call-template name="iview2.getChapter">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="parent" select="'viewer'" />
      </xsl:call-template>
      <xsl:call-template name="iview2.getViewer">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="zoomBar" select="'false'" />
        <xsl:with-param name="chapter" select="'true'" />
        <xsl:with-param name="cutOut" select="'true'" />
        <xsl:with-param name="overview" select="'true'" />
        <xsl:with-param name="style" select="'width:256px; height:256px;'" />
      </xsl:call-template>
      <xsl:call-template name="iview2.start">
        <xsl:with-param name="groupID" select="$derivateID" />
        <xsl:with-param name="style" select="'default'" />
        <xsl:with-param name="startFile" select="$file" />
      </xsl:call-template>
    </xsl:if>
    <!-- MCR IView2 ..end -->
  </xsl:template>
</xsl:stylesheet>