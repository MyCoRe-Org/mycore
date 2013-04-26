<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  exclude-result-prefixes="xsl xalan"
>

<xsl:include href="webpage.xsl" />
<xsl:variable name="printable"  select="'no'" />
<xsl:include href="section.xsl"    />

<xsl:variable name="page.title" select="i18n:translate('component.user2.message.inputError')" /> 
<xsl:variable name="PageID" select="'admin'" />

<xsl:template match="user_action_messages"> 
    <xsl:choose>
      <xsl:when test="@section = 'pw_changed'">
        <div class="section">
          <xsl:value-of select="i18n:translate('component.user2.message.passwordChangedFrom')" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" class="editorButton">
          <xsl:attribute name="value">
            <xsl:value-of select="i18n:translate('component.user2.message.backToForm')" />
          </xsl:attribute>
        </input>
      </xsl:when>
      <xsl:when test="@section = 'user_created'">
        <div class="section">
          <xsl:value-of select="i18n:translate('component.user2.message.userCreated')" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" class="editorButton">
          <xsl:attribute name="value">
            <xsl:value-of select="i18n:translate('component.user2.message.backToForm')" />
          </xsl:attribute>
        </input>
      </xsl:when>
      <xsl:when test="@section = 'user_deleted'">
        <div class="section">
        <p>
          <xsl:value-of select="i18n:translate('component.user2.message.userDeleted')" />
        </p>
          <xsl:for-each select="section">
            <xsl:value-of select="./@value" />
          </xsl:for-each>
        </div>
      </xsl:when>
      <xsl:when test="@section = 'action_not_found'">
        <div class="section">
          <xsl:value-of select="i18n:translate('component.user2.message.badValue')" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" class="editorButton">
          <xsl:attribute name="value">
            <xsl:value-of select="i18n:translate('component.user2.message.backToForm')" />
          </xsl:attribute>
        </input>
      </xsl:when>
      <xsl:when test="@section = 'user_changed'">
        <div class="section">
          <xsl:value-of select="i18n:translate('component.user2.message.userDataChanged')" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" class="editorButton">
          <xsl:attribute name="value">
            <xsl:value-of select="i18n:translate('component.user2.message.backToForm')" />
          </xsl:attribute>
        </input>
      </xsl:when>
      <xsl:otherwise>
        <div class="section">
          <xsl:value-of select="i18n:translate('component.user2.message.userErrorMessage')" />
          <ul>
            <xsl:for-each select="*">
              <li>
                <xsl:apply-templates select="." /> 
              </li>
            </xsl:for-each>
          </ul>
          <input type="button" onClick="javascript:history.back()" class="editorButton">
            <xsl:attribute name="value">
              <xsl:value-of select="i18n:translate('component.user2.message.backToForm')" />
            </xsl:attribute>
          </input>
        </div>
      </xsl:otherwise>
    </xsl:choose>    
</xsl:template>        

<xsl:template match="*">
  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:copy-of select="." />
    </xsl:for-each>
    <xsl:apply-templates select="node()" />
  </xsl:copy>
</xsl:template>

<xsl:template match="no_user_data">
  <xsl:value-of select="i18n:translate('component.user2.message.noUserData')" />
</xsl:template>

<xsl:template match="no_access">
  <xsl:value-of select="i18n:translate('component.user2.message.noAccess')" />
</xsl:template>

<xsl:template match="no_pwold">
  <xsl:value-of select="i18n:translate('component.user2.message.noPWOld')" />
</xsl:template>

<xsl:template match="no_pwnew1">
  <xsl:value-of select="i18n:translate('component.user2.message.noPWNew1')" />
</xsl:template>
 
<xsl:template match="no_pwnew2">
  <xsl:value-of select="i18n:translate('component.user2.message.noPWNew2')" />
</xsl:template>

<xsl:template match="no_pwnewident">
  <xsl:value-of select="i18n:translate('component.user2.message.noPWNewIdent')" />
</xsl:template>

<xsl:template match="not_allowed_changepw">
  <xsl:value-of select="i18n:translate('component.user2.message.notAllowedChangepw')" />
</xsl:template>

<xsl:template match="not_allowed_createuser">
  <xsl:value-of select="i18n:translate('component.user2.message.notAllowedCreateUser')" />
</xsl:template>

<xsl:template match="user_exists">
  <xsl:value-of select="i18n:translate('component.user2.message.userExists')" />
</xsl:template>

<xsl:template match="no_le_exists">
  <xsl:value-of select="i18n:translate('component.user2.message.noLeExists')" />
</xsl:template>

<xsl:template match="not_allowed_delete">
  <xsl:value-of select="i18n:translate('component.user2.message.notAllowedDelete')" />
</xsl:template>

<xsl:template match="not_allowed_changeuser">
  <xsl:value-of select="i18n:translate('component.user2.message.notAllowedChangeUser')" />
</xsl:template>

<xsl:template match="not_allowed_changegroup">
  <xsl:value-of select="i18n:translate('component.user2.message.notAllowedChangeRole')" />
</xsl:template>

<xsl:template match="not_allowed_changeowngroup">
  <xsl:value-of select="i18n:translate('component.user2.message.notAllowedChangeOwnRole')" />
</xsl:template>

<xsl:template match="no_user_selected">
  <xsl:value-of select="i18n:translate('component.user2.message.noUserSelected')" />
</xsl:template>

</xsl:stylesheet>
