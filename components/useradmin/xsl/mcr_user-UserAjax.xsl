<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="xlink i18n" xmlns:acl="xalan://org.mycore.access.MCRAccessManager">

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:variable name="MainTitle" select="i18n:translate('titles.mainTitle')" />
  <xsl:variable name="PageTitle" select="i18n:translate('component.userlogin.titles.pageTitle.contactData')" />

  <xsl:template match="/mcr_user">

    <script src="{$WebApplicationBaseURL}javascript/json2.js" type="text/javascript"></script>
    <script src="{$WebApplicationBaseURL}javascript/prototype.js" type="text/javascript"></script>
    <script src="{$WebApplicationBaseURL}javascript/scriptaculous.js" type="text/javascript"></script>
    <script src="{$WebApplicationBaseURL}javascript/boxover.js" type="text/javascript"></script>
    <script src="{$WebApplicationBaseURL}javascript/effects.js" type="text/javascript"></script>
    <script type="text/javascript">
      <xsl:value-of select="concat('var servletBaseURL=&#x22;',$ServletsBaseURL,'&#x22;;')" />
      <xsl:value-of
        select="concat('var confirmDeleteUser=&#x22;',i18n:translate('users.confirm.deleteUser'),'&#x22;;')" />
      <xsl:value-of
        select="concat('var confirmRemoveUserFromGroup=&#x22;',i18n:translate('users.confirm.removeUserFromGroup'),'&#x22;;')" />
      <xsl:value-of
        select="concat('var cantDeleteUser=&#x22;',i18n:translate('users.error.cantDeleteUser'),'&#x22;;')" />
      <xsl:value-of
        select="concat('var userImg=&#x22;',$WebApplicationBaseURL,'templates/master/',$template,'/IMAGES/system-users.png','&#x22;;')" />
      <xsl:value-of
        select="concat('var grpHandle=&#x22;',$WebApplicationBaseURL,'templates/master/',$template,'/IMAGES/handle.png','&#x22;;')" />
      <xsl:value-of select="concat('var userHdr=&#x22;',i18n:translate('users.manage.users'),'&#x22;;')" />
      <xsl:value-of select="concat('var groupHdr=&#x22;',i18n:translate('users.manage.groups'),'&#x22;;')" />
      <xsl:value-of select="concat('var groupDel=&#x22;',i18n:translate('users.confirm.groupDelete'),'&#x22;;')" />
      <xsl:value-of select="concat('var primaryGroup=&#x22;',i18n:translate('users.error.primaryGroup'),'&#x22;;')" />
      <xsl:value-of select="concat('var groupNotEmpty=&#x22;',i18n:translate('users.confirm.groupNotEmpty'),'&#x22;;')" />
    </script>
    <script src="{$WebApplicationBaseURL}javascript/users.js" type="text/javascript"></script>

    <link href="{$WebApplicationBaseURL}templates/master/{$template}/CSS/style_userManagement.css" rel="stylesheet"
      type="text/css" />
    <xsl:choose>
      <xsl:when test="acl:checkPermission('modify-group') and acl:checkPermission('delete-user')">
        <noscript>
          <p>Bitte aktivieren Sie JavaScript, um die Nutzerverwaltung benutzen zu können</p>
        </noscript>
        <div id="userManagement">
          <div id="users" ></div>
          <div id="groups" ></div>
          <img src="{$WebApplicationBaseURL}templates/master/{$template}/IMAGES/trash.png" alt="Trash" id="trash" ></img>
        </div>
        <script type="text/javascript"><!--//--><![CDATA[//><!--
        initialize();
        //--><!]]></script>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="i18n:translate('metaData.accessDenied')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>