<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions">
  <xsl:variable name="jai.available" select="mcrxml:classAvailable('javax.media.jai.JAI')" />

  <xsl:include href="mcr-module-iview2.xsl" />
  <xsl:template name="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="zoomBar" select="'true'" />
    <xsl:param name="chapter" select="'true'" />
    <xsl:param name="cutOut" select="'true'" />
    <xsl:param name="overview" select="'true'" />
    <xsl:param name="style" select="'red'" />
    <xsl:param name="zoomIn" select="'false'" />
    <xsl:param name="zoomOut" select="'false'" />
    <xsl:param name="normalView" select="'false'" />
    <xsl:param name="fullView" select="'false'" />
    <xsl:param name="toWidth" select="'false'" />
    <xsl:param name="toScreen" select="'false'" />
    <xsl:param name="backward" select="'false'" />
    <xsl:param name="forward" select="'false'" />
    <xsl:param name="pageCounter" select="'false'" />
    <xsl:param name="inputPage" select="'false'" />
    <xsl:param name="openThumbs" select="'false'" />
    <xsl:param name="chapterOpener" select="'false'" />
    <xsl:param name="permalink" select="'false'" />
    
    <xsl:param name="idAdd" select="''" />

    <xsl:call-template name="iview2.init">
      <xsl:with-param name="groupID" select="$groupID"/>
    </xsl:call-template>
    <xsl:call-template name="iview2.getToolbar">
      <xsl:with-param name="groupID" select="$groupID" />
      <xsl:with-param name="idAdd" select="$idAdd" />
      <xsl:with-param name="zoomIn" select="$zoomIn" />
      <xsl:with-param name="zoomOut" select="$zoomOut" />
      <xsl:with-param name="normalView" select="$normalView" />
      <xsl:with-param name="fullView" select="$fullView" />
      <xsl:with-param name="toWidth" select="$toWidth" />
      <xsl:with-param name="toScreen" select="$toScreen" />
      <xsl:with-param name="backward" select="$backward" />
      <xsl:with-param name="forward" select="$forward" />
      <xsl:with-param name="pageCounter" select="$pageCounter" />
      <xsl:with-param name="inputPage" select="$inputPage" />
      <xsl:with-param name="openThumbs" select="$openThumbs" />
      <xsl:with-param name="chapterOpener" select="$chapterOpener" />
      <xsl:with-param name="permalink" select="$permalink" />
      <xsl:with-param name="inputPage" select="$inputPage" />
    </xsl:call-template>
    <xsl:call-template name="iview2.getViewer">
      <xsl:with-param name="groupID" select="$groupID" />
      <xsl:with-param name="zoomBar" select="$zoomBar" />
      <xsl:with-param name="chapter" select="$chapter" />
      <xsl:with-param name="cutOut" select="$cutOut" />
      <xsl:with-param name="overview" select="$overview"/>
      <xsl:with-param name="style" select="$style" />
    </xsl:call-template>

    <xsl:call-template name="iview2.getThumbnail">
      <xsl:with-param name="groupID" select="$groupID" />
      <xsl:with-param name="parent" select="'viewer'" />
    </xsl:call-template>
    <xsl:call-template name="iview2.getChapter">
      <xsl:with-param name="groupID" select="$groupID" />
      <xsl:with-param name="parent" select="'viewer'" />
    </xsl:call-template>
    <xsl:call-template name="iview2.start">
      <xsl:with-param name="groupID" select="$groupID" />
      <xsl:with-param name="style" select="'orig'" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="iview2.getSupport">
    <xsl:param name="derivID" />
    <xsl:value-of xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
      select="iview2:getSupportedMainFile($derivID)" />
  </xsl:template>
</xsl:stylesheet>