<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2008/04/11 11:47:59 $ -->
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
	<xsl:choose>
     <xsl:when test="$CurrentLang = 'ar'" >
       [&#160;<xsl:value-of select="$CurrentUser"/>&#160;]
       <xsl:value-of select="concat(':', i18n:translate('userlogin.tasks.currentAccount'))"/>&#160;&#160;
     </xsl:when>
     <xsl:otherwise>
       <xsl:value-of select="concat(i18n:translate('userlogin.tasks.currentAccount'),' :')"/>&#160;&#160;
       [&#160;<xsl:value-of select="$CurrentUser"/>&#160;]
     </xsl:otherwise>
	</xsl:choose>
</xsl:variable>
    
<xsl:variable name="MainTitle" select="i18n:translate('titles.mainTitle')"/>
<xsl:variable name="PageTitle" select="i18n:translate('titles.pageTitle.login')"/>

<xsl:template name="userAction">
	<p />
    <form action="{$ServletsBaseURL}MCRLoginServlet{$HttpSession}" method="post">
        <input type="hidden" name="url" value="{backto_url}"/>
        <table>
            <!-- Here come the input fields... -->
		<xsl:choose>
		<xsl:when test="$CurrentLang = 'ar' ">
			<tr>
				<td class="inputField_ar"><input name="uid" type="text" maxlength="30"/></td>
                <td class="inputCaption_ar"><xsl:value-of select="concat(i18n:translate('userlogin.tasks.login.account'),' ')"/></td>               
			</tr>
			<tr>
				<td class="inputField_ar"><input name="pwd" type="password" maxlength="30"/></td>
                <td class="inputCaption_ar"><xsl:value-of select="concat(i18n:translate('userlogin.tasks.login.password'),' ')"/></td>           
			</tr>		
		</xsl:when>
	    <xsl:otherwise>
            <tr>
                <td class="inputCaption"><xsl:value-of select="concat(i18n:translate('userlogin.tasks.login.account'),' :')"/></td>
                <td class="inputField"><input name="uid" type="text" maxlength="30"/></td>
            </tr>
            <tr>
                <td class="inputCaption"><xsl:value-of select="concat(i18n:translate('userlogin.tasks.login.password'),' :')"/></td>
                <td class="inputField"><input name="pwd" type="password" maxlength="30"/></td>
            </tr>
	    </xsl:otherwise>
	    </xsl:choose>
		
        </table>
		<p/>
		
		<xsl:choose>
			<xsl:when test="$CurrentLang = 'ar' ">
				<input class="button" type="submit" value="{i18n:translate('buttons.login')} &gt;&gt;" name="LoginSubmit"/>
			</xsl:when>
			<xsl:otherwise>
				<input class="button_ar" type="submit" value="{i18n:translate('buttons.login')} &gt;&gt;" name="LoginSubmit"/>
			</xsl:otherwise>
	    </xsl:choose>
    </form>
</xsl:template>
	
<xsl:template name="userStatus">
    <xsl:if test="/mcr_user/@invalid_password='true' or /mcr_user/@unknown_user='true' or /mcr_user/@user_disabled='true'">
		<p class="status">
            <xsl:value-of select="i18n:translate('userlogin.tasks.login.failed')"/>
        </p>
    </xsl:if>
    <xsl:if test="/mcr_user/@invalid_password='true'">
		<p class="status">
            <xsl:value-of select="i18n:translate('userlogin.tasks.login.invalidPwd')"/>
        </p>
    </xsl:if>
    <xsl:if test="/mcr_user/@unknown_user='true'">
		<p class="status">
            <xsl:value-of select="i18n:translate('userlogin.tasks.login.userUnknown')"/>
        </p>
    </xsl:if>
    <xsl:if test="/mcr_user/@user_disabled='true'">
		<p class="status">
            <xsl:value-of select="i18n:translate('userlogin.tasks.login.userDisabled')"/>
        </p>
    </xsl:if>
</xsl:template>

</xsl:stylesheet>

