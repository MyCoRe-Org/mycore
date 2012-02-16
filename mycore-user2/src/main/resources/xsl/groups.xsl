<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to display data of a login user -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" exclude-result-prefixes="xsl xalan i18n">

  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="classificationBrowser.xsl" />

  <xsl:variable name="PageID" select="'select-group'" />

  <xsl:variable name="PageTitle" select="concat(i18n:translate('component.user2.admin.groupSelectDisplay'),/user/@name)" />
  <xsl:template match="groups[@classID]">
    <xsl:call-template name="mcrClassificationBrowser">
      <xsl:with-param name="classification" select="@classID"/>
      <xsl:with-param name="category" select="@categID"/>
      <xsl:with-param name="style" select="'groupSubselect'"/>
    </xsl:call-template>
  </xsl:template>
</xsl:stylesheet>