<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to display data of a login user -->

<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcrurl="http://www.mycore.de/xslt/url"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n" exclude-result-prefixes="xsl mcrurl mcri18n">

  <xsl:include href="MyCoReLayout-3.xsl" />
  <xsl:include href="classificationBrowser-3.xsl" />

  <xsl:variable name="PageID" select="'select-group'" />

  <xsl:variable name="PageTitle" select="concat(mcri18n:translate('component.user2.admin.roleSelectDisplay'),/user/@name)" />
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
    <xsl:variable name="url1" select="mcrurl:set-param($RequestURL, 'categID', @categID)" />
    <xsl:variable name="url2" select="mcrurl:set-param($url1, 'action', 'chooseCategory')" />
    <xsl:variable name="url" select="mcrurl:add-session($url2)" />
    <li>
      <a href="{$url}">
        <xsl:value-of select="@label" />
      </a>
    </li>
  </xsl:template>
</xsl:stylesheet>