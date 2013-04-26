<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to search for users and display the list users found -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xsl xalan i18n encoder">

  <xsl:include href="users.xsl" />
  <xsl:param name="subselect.session" />
  <xsl:param name="subselect.varpath" />
  <xsl:param name="subselect.webpage" />

  <xsl:template match="/users" mode="headAdditional" priority="10">
    <xsl:variable name="cancelURL">
      <xsl:value-of select="$WebApplicationBaseURL" />
      <xsl:value-of select="$subselect.webpage" />
      <xsl:if test="not(contains($subselect.webpage,'XSL.editor.session.id'))">
        <xsl:text>XSL.editor.session.id=</xsl:text>
        <xsl:value-of select="$subselect.session" />
      </xsl:if>
    </xsl:variable>
    <form class="action" method="post" action="{$cancelURL}">
      <input value="{i18n:translate('component.user2.button.cancelSelect')}" class="action" type="submit" />
    </form>
  </xsl:template>

  <xsl:template match="user" mode="link" priority="10">
    <a>
      <xsl:attribute name="href">
              <xsl:value-of
        select="concat($ServletsBaseURL,'XMLEditor?_action=end.subselect&amp;_var_@name=',@name,
              '&amp;_var_@realm=',@realm,
              '&amp;subselect.session=',$subselect.session,
              '&amp;subselect.varpath=',$subselect.varpath,
              '&amp;subselect.webpage=',encoder:encode($subselect.webpage,'UTF-8'))" />
        </xsl:attribute>
      <xsl:value-of select="@name" />
    </a>
  </xsl:template>

</xsl:stylesheet>
