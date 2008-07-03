<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.9 $ $Date: 2007-02-16 11:53:43 $ -->
<!-- ============================================== -->

<!-- +
     | This stylesheet controls the Web-Layout of the "Select"-mode of the UserServlet. After a
     | successful login using the LoginServlet the request is forwarded (with the request
     | parameter mode=Select) to the UserServlet. This servlet checks the privileges of the
     | current user. Depending on the privileges an XML stream with the following syntax
     | (an example) ist generated and forwarded to the LayoutServlet:
     |
     | <mcr_user pwd_change_ok="true|false">
     |   <guest_id>aragorn</guest_id>
     |   <guest_pwd>mensch</guest_pwd>
     |   <backto_url>http://...</backto_url>
     | </mcr_user>
     |
     | Author: Detlev Degenhardt
     + -->

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:encoder="xalan://java.net.URLEncoder"
  exclude-result-prefixes="xlink encoder i18n">
	
<xsl:include href="mcr_user-Common.xsl"/>
<xsl:include href="MyCoReLayout.xsl" />

<xsl:variable name="heading">
    <xsl:value-of select="i18n:translate('component.userlogin.tasks.currentAccount')"/>&#160;&#160;
    [&#160;<xsl:value-of select="$CurrentUser"/>&#160;]
</xsl:variable>

<xsl:variable name="MainTitle" select="i18n:translate('common.titles.mainTitle')"/>
<xsl:variable name="PageTitle" select="i18n:translate('component.userlogin.titles.pageTitle.selectTask')"/>

<xsl:template name="userAction">
	<p />
    <xsl:value-of select="i18n:translate('component.userlogin.tasks.selectTask.todo')" /> <!-- "What do you want to do?" -->
	<p />
    <ul>
        <li>
            <a href="{$href-login}&amp;uid={$CurrentUser}">
                <xsl:value-of select="i18n:translate('component.userlogin.tasks.selectTask.return')" /> <!-- "Back to the MyCoRe application" -->
            </a>
        </li>
        <li>
            <a href="{$href-user}&amp;mode=CreatePwdDialog">
                <xsl:value-of select="i18n:translate('component.userlogin.tasks.selectTask.changePwd')" /> <!-- "Change password" -->
            </a>
        </li>
        <li>
            <a href="{$href-login}">
                <xsl:value-of select="i18n:translate('component.userlogin.tasks.selectTask.changeUser')" /> <!-- "Change user" -->
            </a>
        </li>
        <li>
            <a href="{$href-user}&amp;mode=ShowUser">
                <xsl:value-of select="i18n:translate('component.userlogin.tasks.selectTask.showUser')" /> <!-- "Show data of the current user account" -->
            </a>
        </li>
        <li>
            <a href="{$href-login}&amp;uid={$guest_id}&amp;pwd={$guest_pwd}">
                <xsl:value-of select="i18n:translate('component.userlogin.tasks.selectTask.logout')"/> <!-- "Logout and work as guest user" -->
            </a>
        </li>
    </ul>
</xsl:template>
	
<xsl:template name="userStatus">
      <xsl:if test="/mcr_user/@pwd_change_ok='true'">
		<p class="status">
            <xsl:value-of select="i18n:translate('component.userlogin.tasks.selectTask.changePwdOk')"/>
        </p>
      </xsl:if>
</xsl:template>

</xsl:stylesheet>
