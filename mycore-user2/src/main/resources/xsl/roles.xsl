<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to display data of a login user -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" exclude-result-prefixes="xsl xalan i18n">

  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="classificationBrowser.xsl" />
  <xsl:param name="RequestURL" />

  <xsl:variable name="PageID" select="'select-group'" />

  <xsl:variable name="PageTitle" select="concat(i18n:translate('component.user2.admin.roleSelectDisplay'),/user/@name)" />
  <xsl:template match="roles[@classID]">
    <xsl:call-template name="mcrClassificationBrowser">
      <xsl:with-param name="classification" select="@classID" />
      <xsl:with-param name="category" select="@categID" />
      <xsl:with-param name="style" select="'roleSubselect'" />
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="roles[group]">
    <ul>
      <xsl:apply-templates select="role" />
    </ul>
  </xsl:template>
  <xsl:template match="role">
    <xsl:variable name="url1">
      <xsl:call-template name="UrlSetParam">
        <xsl:with-param name="url" select="$RequestURL" />
        <xsl:with-param name="par" select="'categID'" />
        <xsl:with-param name="value" select="@categID" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="url2">
      <xsl:call-template name="UrlSetParam">
        <xsl:with-param name="url" select="$url1" />
        <xsl:with-param name="par" select="'action'" />
        <xsl:with-param name="value" select="'chooseCategory'" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="url">
      <xsl:call-template name="UrlAddSession">
        <xsl:with-param name="url" select="$url2" />
      </xsl:call-template>
    </xsl:variable>
    <li>
      <a href="{$url}">
        <xsl:value-of select="@label" />
      </a>
    </li>
  </xsl:template>
</xsl:stylesheet>