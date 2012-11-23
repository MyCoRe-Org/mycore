<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan i18n">
  <xsl:output method="html" doctype-system="about:legacy-compat" indent="yes" omit-xml-declaration="yes" media-type="text/html"
    version="5" />
  <xsl:include href="coreFunctions.xsl" />
  <xsl:param name="RequestURL" />
  <xsl:template match="*[@ID]">
    <xsl:variable name="verinfo" select="document(concat('versioninfo:',@ID))" />
    <ol class="versioninfo">
      <xsl:for-each select="$verinfo/versions/version">
        <xsl:sort order="descending" select="position()" data-type="number" />
        <li>
          <xsl:if test="@r">
            <xsl:variable name="noLayout">
              <xsl:call-template name="UrlDelParam">
                <xsl:with-param name="url" select="$RequestURL"/>
                <xsl:with-param name="par" select="'XSL.Style'"/>
              </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="href">
              <xsl:call-template name="UrlSetParam">
                <xsl:with-param name="url" select="$noLayout" />
                <xsl:with-param name="par" select="'r'" />
                <xsl:with-param name="value" select="@r" />
              </xsl:call-template>
            </xsl:variable>
            <span class="rev">
              <xsl:choose>
                <xsl:when test="@action='D'">
                  <xsl:value-of select="@r" />
                </xsl:when>
                <xsl:otherwise>
                  <a href="{$href}">
                    <xsl:value-of select="@r" />
                  </a>
                </xsl:otherwise>
              </xsl:choose>
            </span>
            <xsl:value-of select="' '" />
          </xsl:if>
          <xsl:if test="@action">
            <span class="action">
              <xsl:value-of select="i18n:translate(concat('metaData.versions.action.',@action))" />
            </span>
            <xsl:value-of select="' '" />
          </xsl:if>
          <span class="@date">
            <xsl:call-template name="formatISODate">
              <xsl:with-param name="date" select="@date" />
              <xsl:with-param name="format" select="i18n:translate('metaData.dateTime')" />
            </xsl:call-template>
          </span>
          <xsl:value-of select="' '" />
          <xsl:if test="@user">
            <span class="user">
              <xsl:value-of select="@user" />
            </span>
            <xsl:value-of select="' '" />
          </xsl:if>
        </li>
      </xsl:for-each>
    </ol>
  </xsl:template>
</xsl:stylesheet>