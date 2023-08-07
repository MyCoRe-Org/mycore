<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to search for users and display the list users found -->

<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
                xmlns:mcrurl="http://www.mycore.de/xslt/url"
                exclude-result-prefixes="xsl mcri18n mcrurl"
>

  <xsl:include href="users-3.xsl" />

  <!-- ========== XED Subselect detection ========== -->
  <xsl:variable name="xedSession" select="mcrurl:get-param($RequestURL, '_xed_subselect_session')" />

  <xsl:template match="/users" mode="headAdditional" priority="10">
    <xsl:variable name="cancelURL">
      <xsl:value-of select="concat($ServletsBaseURL,'XEditor?_xed_submit_return= ')" />
      <xsl:value-of select="concat('&amp;_xed_session=',encode-for-uri($xedSession))" />
    </xsl:variable>

    <form method="post" action="{$cancelURL}">
      <input value="{mcri18n:translate('component.user2.button.cancelSelect')}" class="btn btn-default" type="submit" />
    </form>
  </xsl:template>

  <xsl:template match="user" mode="link" priority="10">
    <a>
      <xsl:attribute name="href">
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor?_xed_submit_return= ')" />
        <xsl:value-of select="concat('&amp;_xed_session=',encode-for-uri($xedSession))" />
        <xsl:value-of select="concat('&amp;@name=',encode-for-uri(@name))" />
        <xsl:value-of select="concat('&amp;@realm=',encode-for-uri(@realm))" />
      </xsl:attribute>
      <xsl:value-of select="@name" />
    </a>
  </xsl:template>

</xsl:stylesheet>
