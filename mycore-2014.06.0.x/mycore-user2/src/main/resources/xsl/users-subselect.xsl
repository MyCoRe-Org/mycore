<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to search for users and display the list users found -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xsl xalan i18n encoder"
>

  <xsl:include href="users.xsl" />

  <xsl:variable name="PageID" select="'select-user'" />

  <xsl:variable name="PageTitle" select="i18n:translate('component.user2.admin.userSelect.title')" />
  
  <!-- ========== XED Subselect detection ========== -->
  <xsl:variable name="xedSession">
    <xsl:call-template name="UrlGetParam">
      <xsl:with-param name="url" select="$RequestURL" />
      <xsl:with-param name="par" select="'_xed_subselect_session'" />
    </xsl:call-template>
  </xsl:variable>

  <xsl:template match="/users" mode="headAdditional" priority="10">
    <xsl:variable name="cancelURL">
      <xsl:value-of select="concat($ServletsBaseURL,'XEditor?_xed_submit_return= ')" />
      <xsl:value-of select="concat('&amp;_xed_session=',encoder:encode($xedSession,'UTF-8'))" />
    </xsl:variable>

    <form method="post" action="{$cancelURL}">
      <input value="{i18n:translate('component.user2.button.cancelSelect')}" class="btn btn-default" type="submit" />
    </form>
  </xsl:template>

  <xsl:template match="user" mode="link" priority="10">
    <a>
      <xsl:attribute name="href">
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor?_xed_submit_return= ')" />
        <xsl:value-of select="concat('&amp;_xed_session=',encoder:encode($xedSession,'UTF-8'))" />
        <xsl:value-of select="concat('&amp;@name=',encoder:encode(@name,'UTF-8'))" />
        <xsl:value-of select="concat('&amp;@realm=',encoder:encode(@realm,'UTF-8'))" />
      </xsl:attribute>
      <xsl:value-of select="@name" />
    </a>
  </xsl:template>

</xsl:stylesheet>
