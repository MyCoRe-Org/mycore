<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>

<!-- ============================================== -->
<!-- $Revision: 1.4 $ $Date: 2009/03/20 10:42:33 $ -->
<!-- ============================================== -->

<!-- +
| This stylesheet controls the Web-Layout of the Login Servlet. The Login Servlet
| gathers information about the session, user ID, password and calling URL and
| then tries to login the user by delegating the login request to the user manager.
| Depending on whether the login was successful or not, the Login Servlet generates
| the following XML output stream:
|
| <mcr_user unknown_user="true|false"
|           user_disabled="true|false"
|           invalid_password="true|false">
|   <guest_id>...</guest_id>
|   <guest_pwd>...</guest_pwd>
|   <backto_url>...<backto_url>
| </mcr_user>
|
| The XML stream is sent to the Layout Servlet and finally handled by this stylesheet.
|
| Authors: Detlev Degenhardt, Thomas Scheffler, Kathleen Neumann
| Last changes: 2012-03-16
+ -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xlink encoder i18n">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:param name="FormTarget" select="concat($ServletsBaseURL,'MCRLoginServlet')" />
  <xsl:param name="Realm" select="'local'" />
  <xsl:variable name="loginToRealm" select="document(concat('realm:',$Realm))" />

  <xsl:variable name="PageTitle" select="i18n:translate('component.user2.login.form.title')" />

  <xsl:template match="/login">
    <!-- +
    | There are three possible error-conditions: wrong password, unknown user and disabled
    | user. If one of these conditions occured, the corresponding information will be
    | presented at the top of the page.
    + -->
    <xsl:apply-templates select="." mode="userStatus" />
    <xsl:apply-templates select="." mode="userAction" />
  </xsl:template>

  <xsl:template match="login" mode="userAction">
    <form action="{$FormTarget}{$HttpSession}" method="post" role="form" class="form-login">
      <h2 class="form-login-heading">
        <xsl:value-of select="i18n:translate('component.user2.login.heading')" />
      </h2>
      <xsl:apply-templates select="$loginToRealm" mode="form" />
      <input type="hidden" name="action" value="login" />
      <input type="hidden" name="url" value="{returnURL}" />
      <xsl:variable name="userNameText" select="i18n:translate('component.user2.login.form.userName')" />
      <xsl:variable name="passwordText" select="i18n:translate('component.user2.login.form.password')" />
      <fieldset>
        <!-- Here come the input fields... -->
        <div>
          <xsl:apply-templates select="." mode="controlGroupClass" />
          <label class="control-label" for="user">
            <xsl:value-of select="concat($userNameText,' :')" />
          </label>
          <div class="controls">
            <input type="text" name="uid" class="form-control input-large" placeholder="{$userNameText}" title="{$userNameText}" />
          </div>
        </div>
        <div>
          <xsl:apply-templates select="." mode="controlGroupClass" />
          <label class="control-label" for="password">
            <xsl:value-of select="concat($passwordText,' :')" />
          </label>
          <div class="controls">
            <input type="password" name="pwd" class="form-control input-large" placeholder="{$passwordText}" title="{$passwordText}" />
          </div>
        </div>
      </fieldset>
      <div class="form-actions">
        <xsl:choose>
          <xsl:when test="$direction = 'rtl' ">
            <button class="btn btn-default" type="button" onClick="self.location.href='{returnURL}'" tabindex="999">
              <xsl:value-of select="i18n:translate('component.user2.button.cancel')" />
            </button>
            <xsl:value-of select="' '" />
            <button class="btn btn-primary" type="submit" name="LoginSubmit">
              <xsl:value-of select="i18n:translate('component.user2.button.login')" />
            </button>
          </xsl:when>
          <xsl:otherwise>
            <button class="btn btn-primary" type="submit" name="LoginSubmit">
              <xsl:value-of select="i18n:translate('component.user2.button.login')" />
            </button>
            <xsl:value-of select="' '" />
            <button class="btn btn-default" type="button" onClick="self.location.href='{returnURL}'" tabindex="999">
              <xsl:value-of select="i18n:translate('component.user2.button.cancel')" />
            </button>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </form>
  </xsl:template>

  <xsl:template match="login" mode="userStatus">
    <xsl:if test="@loginFailed='true'">
      <div class="alert alert-danger" role="alert">
        <strong>
          <xsl:value-of select="i18n:translate('component.user2.login.failed')" />
        </strong>
        <xsl:choose>
          <xsl:when test="errorMessage">
            <br/>
            <xsl:value-of select="i18n:translate('component.user2.login.failed.reason', errorMessage)" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(' ',i18n:translate('component.user2.login.invalidUserPwd'))" />
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </xsl:if>
  </xsl:template>
  <xsl:template match="login" mode="controlGroupClass">
    <xsl:attribute name="class">
      <xsl:value-of select="'form-group'" />
      <xsl:if test="@loginFailed='true'">
        <xsl:value-of select="' has-error'" />
      </xsl:if>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="realm" mode="form">
    <xsl:if test="login/@realmParameter">
      <input type="hidden" name="{login/@realmParameter}" value="{@id}" />
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
