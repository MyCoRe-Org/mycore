<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2008/04/11 09:09:38 $ -->
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
<xsl:variable name="PageTitle" select="i18n:translate('titles.pageTitle.selectTask')"/>

<xsl:template name="userAction">
	<center>
		<xsl:value-of select="i18n:translate('userlogin.tasks.selectTask.hint')"/>
	</center>
</xsl:template>

<xsl:template name="userStatus">
</xsl:template>

</xsl:stylesheet>
