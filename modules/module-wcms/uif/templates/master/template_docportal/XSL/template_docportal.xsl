<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision$ $Date$ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink">

	<!-- ======================================================================================================== -->
	<xsl:template name="template_docportal">
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />

		<html>
			<head>
				<title>
					<xsl:call-template name="PageTitle"/>
				</title>
				<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
				<link href="{$WebApplicationBaseURL}css/mycore.css" rel="stylesheet" type="text/css" />
				<script src="{$WebApplicationBaseURL}modules/module-wcms/uif/common/JavaScript/WCMSJavaScript.js" 
					type="text/javascript"></script>
				<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_general.css" 
					rel="stylesheet"/>
				<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_navigation.css" 
					rel="stylesheet"/>
				<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_content.css" 
					rel="stylesheet"/>
			</head>
			<body>
				<xsl:choose>
					<xsl:when test="$CurrentLang = 'de' ">
						<xsl:call-template name="writeHeader">
							<xsl:with-param name="browserAddress" select="$browserAddress"/>
							<xsl:with-param name="Layout.ProjectName" select="'DocPortal'" />
							<xsl:with-param name="Layout.SearchFor" select="'Suche nach:'" />
							<xsl:with-param name="Layout.MainPage" select="'Home'" />
							<xsl:with-param name="Layout.Author" select="'Autor-Seite'" />
							<xsl:with-param name="Layout.Login" select="'Login'" />
							<xsl:with-param name="Layout.Help" select="'Hilfe'" />
							<xsl:with-param name="Layout.Documents" select="'Dokumenten'" />
							<xsl:with-param name="Layout.Persons" select="'Personen'" />
							<xsl:with-param name="Layout.Institutions" select="'Institutionen'" />
							<xsl:with-param name="Layout.Classifications" select="'Klassifikationen'" />
							<xsl:with-param name="Layout.browse" select="'browsen'" />
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="writeHeader">
							<xsl:with-param name="browserAddress" select="$browserAddress"/>
							<xsl:with-param name="Layout.ProjectName" select="'DocPortal'" />
							<xsl:with-param name="Layout.SearchFor" select="'search for:'" />
							<xsl:with-param name="Layout.MainPage" select="'home'" />
							<xsl:with-param name="Layout.Author" select="'authors page'" />
							<xsl:with-param name="Layout.Login" select="'log in'" />
							<xsl:with-param name="Layout.Help" select="'help'" />
							<xsl:with-param name="Layout.Documents" select="'documents'" />
							<xsl:with-param name="Layout.Persons" select="'persons'" />
							<xsl:with-param name="Layout.Institutions" select="'institutions'" />
							<xsl:with-param name="Layout.Classifications" select="'classifications'" />
							<xsl:with-param name="Layout.browse" select="'browsen'" />
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:call-template name="template_docportal.write.content">
					<xsl:with-param name="browserAddress" select="$browserAddress"/>
					<xsl:with-param name="template" select="$template" />
				</xsl:call-template>
			</body>
		</html>
	</xsl:template>
	<!-- ======================================================================================================== -->
	<xsl:template name="writeHeader">
		<xsl:param name="browserAddress" />
		<xsl:param name="browserAddress" />
		<xsl:param name="Layout.ProjectName" />
		<xsl:param name="Layout.SearchFor" />
		<xsl:param name="Layout.MainPage" />
		<xsl:param name="Layout.Author" />
		<xsl:param name="Layout.Login" />
		<xsl:param name="Layout.Help" />
		<xsl:param name="Layout.Documents" />
		<xsl:param name="Layout.Persons" />
		<xsl:param name="Layout.Institutions" />
		<xsl:param name="Layout.Classifications" />
		<xsl:param name="Layout.browse" />		
		
		<div id="leftBanner">
			<img src="{$WebApplicationBaseURL}/images/sample-project.gif" style="width:54px; height:395px" alt="MyCoRe-Sample" />
		</div>
		<div id="menuBanner">&#160;</div>
		<!-- MyCoRe-Logo top left -->
		<div id="logoSpace">
			<img src="{$WebApplicationBaseURL}/images/mycore-logo.gif" style="width:187px; height:132px;" alt="&lt;MyCoRe&gt;" 
				title="www.mycore.de" />
		</div>
		<!-- MyCoRe-Sample -->
		<div id="projectID">
			<xsl:value-of select="$Layout.ProjectName" />
		</div>
		<div id="MCRSessionID"><xsl:value-of select="$MCRSessionID" />&#160;</div>
		<!-- Top-Right Menu -->
		<div id="NSMenu">
			<tr>
				<td>
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="0" border="0" alt="" />
				</td>
				<td height="22">
					<xsl:call-template name="NavigationRow">
						<xsl:with-param name="rootNode" select="document($navigationBase)/navigation/navi-below" />
						<xsl:with-param name="CSSLayoutClass" select="'navi_below'"/>
						<xsl:with-param name="menuPointHeigth" select="'21'" />
						<!-- use pixel values -->
						<xsl:with-param name="spaceBetweenLinks" select="'12'" />
						<!-- use pixel values -->
						<xsl:with-param name="browserAddress" select="$browserAddress"/>
					</xsl:call-template>
				</td>
			</tr>
			<!-- end: wcms menu  -->
		</div>
		<!-- "Search for" next to logo -->
		<div id="searchFor">
			<span class="topmenu"><xsl:value-of select="$Layout.SearchFor" />&#160;</span>
		</div>
		<div id="searchMenu" class="clearfix">
			<table cellspacing="0" cellpadding="0">
				<tr>
					<th class="searchmenu" style="margin:10px;"> &#160;&#160;<a class="searchmenu" 
						href="{$ServletsBaseURL}MCRSearchMaskServlet?lang={$CurrentLang}&amp;type=alldocs&amp;layout=simpledocument&amp;mode=CreateSearchMask"><strong><xsl:value-of 
						select="$Layout.Documents" /></strong></a>&#160;&#160; </th>
					<th>&#160;</th>
					<th class="searchmenu" style="margin:10px;"> &#160;&#160;<a class="searchmenu" 
						href="{$ServletsBaseURL}MCRSearchMaskServlet?lang={$CurrentLang}&amp;type=alldocs&amp;layout=allpers&amp;mode=CreateSearchMask"><strong><xsl:value-of 
						select="$Layout.Persons" /></strong></a>&#160;&#160; </th>
					<th>&#160;</th>
					<th class="searchmenu" style="margin:10px;"> &#160;&#160;<a class="searchmenu" 
						href="{$ServletsBaseURL}MCRSearchMaskServlet?lang={$CurrentLang}&amp;type=institution&amp;mode=CreateSearchMask"><strong><xsl:value-of 
						select="$Layout.Institutions" /></strong></a>&#160;&#160; </th>
					<th>&#160;</th>
					<th class="searchmenu" style="margin:10px;"> &#160;&#160;<a class="searchmenu" 
						href="{$ServletsBaseURL}../browse/origin"><strong><xsl:value-of select="$Layout.Classifications" 
						/></strong></a>&#160;&#160; </th>
				</tr>
			</table>
		</div>
	</xsl:template>
	<!-- ======================================================================================================== -->
	<xsl:template name="template_docportal.write.content">
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />
		<xsl:apply-templates >
			<xsl:with-param name="template" select="$template" />
			<xsl:with-param name="browserAddress" select="$browserAddress" />
		</xsl:apply-templates>
	</xsl:template>
	<!-- ======================================================================================================== -->
</xsl:stylesheet>