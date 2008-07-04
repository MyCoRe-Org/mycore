<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2008/04/11 11:47:59 $ -->
<!-- ============================================== -->

<!-- +
     | This stylesheet controls the Web-Layout of the "CreatePwdDialog"- and "ChangePwd"-modes
     | of the UserServlet. In the first mode empty password fields for the change password
     | dialog are presented. The passwords are sent back to the UserServlet. In case there
     | are errors (e.g. mismatching passwords) the UserServlet will send the error messages
     | back to this stylesheet (using the LayoutServlet). The following syntax of the XML-stream
     | is provided by the UserServlet:
     |
     | <mcr_user new_pwd_mismatch="true|false"
     |           old_pwd_mismatch="true|false">
     |   <error>...</error>                       (Here messages from exceptions might appear.)
     |   <guest_id>...</guest_id>
     |   <guest_pwd>...</guest_pwd>
     |   <backto_url>...<backto_url>
     | </mcr_user>
     |
     | The XML stream is sent to the Layout Servlet and finally handled by this stylesheet.
     |
     | Authors: Detlev Degenhardt
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
       <xsl:value-of select="concat(':', i18n:translate('component.userlogin.tasks.currentAccount'))"/>&#160;&#160;
     </xsl:when>
     <xsl:otherwise>
       <xsl:value-of select="concat(i18n:translate('component.userlogin.tasks.currentAccount'),' :')"/>&#160;&#160;
       [&#160;<xsl:value-of select="$CurrentUser"/>&#160;]
     </xsl:otherwise>
	</xsl:choose>
</xsl:variable>

<xsl:variable name="MainTitle" select="i18n:translate('common.titles.mainTitle')"/>
<xsl:variable name="PageTitle" select="i18n:translate('component.userlogin.titles.pageTitle.changePass')"/>

<xsl:template name="userAction">
    <!-- +
         | There are 3 possible error-conditions: the provided new passwords are not equal, the
         | old password is incorrect or something happened while setting the password in the
         | core system of mycore (i.e. the user manager). If one of these conditions occured,
         | the corresponding information will be presented at the top of the page.
         + -->

	<p />
    <form action="{$ServletsBaseURL}MCRUserServlet{$HttpSession}?mode=ChangePwd" method="post">
        <input type="hidden" name="url" value="{backto_url}"/>
        <table>
		  <xsl:choose>
           <xsl:when test="$CurrentLang = 'ar'" >
			<tr>
                <td class="inputField"><input name="pwd_1" type="password" maxlength="30"/></td>
				<td class="inputCaption_ar"><xsl:value-of select="concat(':' , i18n:translate('component.userlogin.tasks.changePass.newPass'))"/></td>                
            </tr>
            <tr>
                <td class="inputField" ><input name="pwd_2" type="password" maxlength="30"/></td>
				<td class="inputCaption_ar"><xsl:value-of select="concat(':' , i18n:translate('component.userlogin.tasks.changePass.repeatPass'))"/></td>              
            </tr>
            <tr >
                <th colspan="2">
                    <xsl:value-of select="i18n:translate('component.userlogin.tasks.changePass.securityNote')"/>
                </th>
            </tr>
            <tr>
                <td class="inputField" ><input name="oldpwd" type="password" maxlength="30"/></td>
				<td class="inputCaption_ar"><xsl:value-of select="concat(':' , i18n:translate('component.userlogin.tasks.changePass.oldPass'))"/></td>               
            </tr>
		   </xsl:when>
           <xsl:otherwise>
            <tr>
                <td class="inputCaption"><xsl:value-of select="concat(i18n:translate('component.userlogin.tasks.changePass.newPass'), ':')"/></td>
                <td class="inputField"><input name="pwd_1" type="password" maxlength="30"/></td>
            </tr>
            <tr>
                <td class="inputCaption"><xsl:value-of select="concat(i18n:translate('component.userlogin.tasks.changePass.repeatPass'), ':')"/></td>
                <td class="inputField"><input name="pwd_2" type="password" maxlength="30"/></td>
            </tr>
            <tr >
                <th colspan="2">
                    <xsl:value-of select="i18n:translate('component.userlogin.tasks.changePass.securityNote')"/>
                </th>
            </tr>
            <tr>
                <td class="inputCaption"><xsl:value-of select="concat(i18n:translate('component.userlogin.tasks.changePass.oldPass'), ':')"/></td>
                <td class="inputField" ><input name="oldpwd" type="password" maxlength="30"/></td>
            </tr>
		  </xsl:otherwise>
	     </xsl:choose>
        </table>
		
		<p/>
		<xsl:choose>
			<xsl:when test="$CurrentLang = 'ar' ">
				<input type="submit" class="button_ar" value="{i18n:translate('component.userlogin.tasks.changePass.submit')} &gt;&gt;" name="ChangePwdSubmit"/>
			</xsl:when>
			<xsl:otherwise>
				<input type="submit" class="button" value="{i18n:translate('component.userlogin.tasks.changePass.submit')} &gt;&gt;" name="ChangePwdSubmit"/>
			</xsl:otherwise>
	    </xsl:choose>
    </form>
</xsl:template>

<xsl:template name="userStatus">
    <xsl:if test="/mcr_user/@new_pwd_mismatch='true' or /mcr_user/@old_pwd_mismatch='true' or /mcr_user/error">
		<p class="status">
            <xsl:value-of select="i18n:translate('component.userlogin.tasks.changePass.failed')"/>
        </p>
    </xsl:if>
    <xsl:if test="@new_pwd_mismatch='true'">
		<p class="status">
            <xsl:value-of select="i18n:translate('component.userlogin.tasks.changePass.newPassMismatch')"/>
        </p>
    </xsl:if>
    <xsl:if test="@old_pwd_mismatch='true'">
		<p class="status">
            <xsl:value-of select="i18n:translate('component.userlogin.tasks.changePass.oldPassMismatch')"/>
        </p>
    </xsl:if>
    <xsl:if test="/mcr_user/error">
		<p class="status">
            <xsl:value-of select="/mcr_user/error"/>
        </p>
    </xsl:if>
</xsl:template>

</xsl:stylesheet>
