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
| Authors: Detlev Degenhardt, Thomas Scheffler
| Last changes: 2004-03-08
+ -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xlink encoder i18n">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:param name="MCR.Users.Guestuser.UserName" />

  <xsl:variable name="MainTitle" select="i18n:translate('component.user2.login.form.title')" />
  <xsl:variable name="PageTitle" select="i18n:translate('component.user2.login.form.title')" />

  <xsl:template match="/login">
    <div id="userlogin">
        <!-- At first we display the current user in a head line. -->
      <p class="header">
        <xsl:variable name="currentAccount">
          <xsl:value-of select="'&lt;strong&gt;'" />
          <xsl:choose>
            <xsl:when test="@guest='true'">
              <xsl:value-of select="i18n:translate('component.user2.login.guest')" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat(@user,' [',@realm,']')" />
            </xsl:otherwise>
          </xsl:choose>
          <xsl:value-of select="'&lt;/strong&gt;'" />
        </xsl:variable>
        <xsl:value-of select="i18n:translate('component.user2.login.currentAccount', $currentAccount)" disable-output-escaping="yes" />
      </p>
        
        <!-- +
        | There are three possible error-conditions: wrong password, unknown user and disabled
        | user. If one of these conditions occured, the corresponding information will be
        | presented at the top of the page.
        + -->
      <xsl:apply-templates select="." mode="userStatus" />
      <xsl:apply-templates select="." mode="userAction" />
    </div>
  </xsl:template>

  <xsl:template match="login" mode="userAction">
    <form action="{$ServletsBaseURL}MCRLoginServlet{$HttpSession}" method="post">
      <input type="hidden" name="action" value="login" />
      <input type="hidden" name="url" value="{returnURL}" />
      <table>
        <!-- Here come the input fields... -->
        <xsl:choose>
          <xsl:when test="$direction = 'rtl' ">
            <tr>
              <td class="inputField-rtl">
                <input name="uid" type="text" maxlength="30" />
              </td>
              <td class="inputCaption-rtl">
                <xsl:value-of select="concat(i18n:translate('component.user2.login.form.userName'),' ')" />
              </td>
            </tr>
            <tr height="5px" />
            <tr>
              <td class="inputField-rtl">
                <input name="pwd" type="password" maxlength="30" />
              </td>
              <td class="inputCaption-rtl">
                <xsl:value-of select="concat(i18n:translate('component.user2.login.form.password'),' ')" />
              </td>
            </tr>
          </xsl:when>
          <xsl:otherwise>
            <tr>
              <td class="inputCaption">
                <xsl:value-of select="concat(i18n:translate('component.user2.login.form.userName'),' :')" />
              </td>
              <td class="inputField">
                <input name="uid" type="text" maxlength="30" />
              </td>
            </tr>
            <tr height="5px" />
            <tr>
              <td class="inputCaption">
                <xsl:value-of select="concat(i18n:translate('component.user2.login.form.password'),' :')" />
              </td>
              <td class="inputField">
                <input name="pwd" type="password" maxlength="30" />
              </td>
            </tr>
          </xsl:otherwise>
        </xsl:choose>

      </table>
      <xsl:choose>
        <xsl:when test="$direction = 'rtl' ">
          <input class="button-rtl" onClick="self.location.href='{$ServletsBaseURL}MCRLoginServlet{$HttpSession}?action=cancel'" type="button"
            tabindex="999" value="{i18n:translate('component.user2.button.cancel')}" />
          <xsl:value-of select="' '" />
          <input class="button-rtl" type="submit" value="{i18n:translate('component.user2.button.login')}" name="LoginSubmit" />
        </xsl:when>
        <xsl:otherwise>
          <input class="button" type="submit" value="{i18n:translate('component.user2.button.login')}" name="LoginSubmit" />
          <xsl:value-of select="' '" />
          <input class="button" onClick="self.location.href='{returnURL}'" type="button"
            tabindex="999" value="{i18n:translate('component.user2.button.cancel')}" />
        </xsl:otherwise>
      </xsl:choose>
    </form>
  </xsl:template>

  <xsl:template match="login" mode="userStatus">
    <xsl:if test="@loginFailed='true'">
      <p class="status">
        <xsl:value-of select="i18n:translate('component.user2.login.failed')" />
      </p>
      <p class="status">
        <xsl:value-of select="i18n:translate('component.user2.login.invalidUserPwd')" />
      </p>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
