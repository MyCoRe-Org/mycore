<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision$ $Date$ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink">
	<xsl:template name="template_mycore-de-2">
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />
		<html>
			<head>
				<meta http-equiv="content-type" content="text/html;charset=ISO-8859-1"/>
				<title>
					<xsl:call-template name="PageTitle"/>
				</title>
				<script src="{$WebApplicationBaseURL}modules/module-wcms/uif/common/JavaScript/WCMSJavaScript.js" 
					type="text/javascript"></script>
				<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_general.css" 
					rel="stylesheet"/>
				<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_navigation.css" 
					rel="stylesheet"/>
				<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_content.css" 
					rel="stylesheet"/>
			</head>
			<!-- ================================================================================= -->
			<body leftmargin="0" rightmargin="0" topmargin="0" bgcolor="#FFFFFF">
				<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
					<colgroup>
						<col width="220"></col>
						<col></col>
					</colgroup>
					<tr valign="top">
						<!-- ........................................................................................................ -->
						<!-- general column left -->
						<td width="220" height="100%">
							<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
								<col width="20"></col>
								<col width="200"></col>
								<!-- ............................................ -->
								<!-- logo -->
								<tr valign="top">
									<td colspan="2">
										<a href="{$WebApplicationBaseURL}">
											<img src="{$ImageBaseURL}logo2.gif" width="220" height="117" border="0" 
												alt="" />
										</a>
									</td>
								</tr>
								<!-- END OF: logo -->
								<!-- ............................................ -->
								<!-- ............................................ -->
								<tr valign="top">
									<!-- place holder column left to main menu -->
									<td>
										<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="0" border="0" 
											alt="" />
									</td>
									<!-- END OF: place holder column left to main menu -->
									<!-- ............................................ -->
									<!-- main menu -->
									<td height="100%">
										<xsl:call-template name="Navigation_main">
											<xsl:with-param name="browserAddress" select="$browserAddress" />
										</xsl:call-template>
									</td>
									<!-- END OF: main menu -->
								</tr>
								<tr>
									<td>
										<br/>
									</td>
								</tr>
							</table>
						</td>
						<!-- END OF: general column left -->
						<!-- ........................................................................................................ -->
						<!-- ........................................................................................................ -->
						<!-- general column right -->
						<td height="100%">
							<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
								<tr>
									<td>
										<table align="right" border="0" cellspacing="0" cellpadding="0">
											<!-- ............................................ -->
											<!-- menu below -->
											<tr>
												<td height="33" align="right">
													<xsl:call-template name="NavigationRow">
														<xsl:with-param name="rootNode" 
															select="document($navigationBase) /navigation/navi-below" 
															/>
														<xsl:with-param name="CSSLayoutClass" 
															select="'navi_below'"/>
														<xsl:with-param name="menuPointHeigth" select="'21'" />
														<!-- use pixel values -->
														<xsl:with-param name="spaceBetweenLinks" select="'12'" 
															/>
														<!-- use pixel values -->
														<xsl:with-param name="browserAddress" 
															select="$browserAddress"/>
													</xsl:call-template>
												</td>
											</tr>
											<!-- menu below -->
											<!-- ............................................ -->
										</table>
									</td>
								</tr>
								<!-- history navigation area -->
								<tr valign="top">
									<td>
										<table class="navi_history" width="95%" align="center" border="0">
											<tr>
												<td>
													<xsl:call-template name="HistoryNavigationRow" >
														<xsl:with-param name="browserAddress" 
															select="$browserAddress"/>
														<xsl:with-param name="CSSLayoutClass" 
															select="'navi_main'"/>
													</xsl:call-template>
												</td>
											</tr>
										</table>
									</td>
								</tr>
								<tr>
									<td>
										<br/>
									</td>
								</tr>
								<!-- ............................................ -->
								<!-- content area -->
								<tr valign="top">
									<td height="100%">
										<xsl:call-template name="template_mycoresampleAlternative.write.content">
											<xsl:with-param name="browserAddress" select="$browserAddress"/>
											<xsl:with-param name="template" select="$template" />
										</xsl:call-template>
									</td>
								</tr>
								<!-- content area -->
								<!-- ............................................ -->
								<!-- ............................................ -->
								<!-- footer right -->
								<tr>
									<td align="right" class="footer" >
										<xsl:call-template name="footer" />
									</td>
								</tr>
								<!-- END OF: footer right -->
								<!-- ............................................ -->
							</table>
						</td>
						<!-- END OF: general column right -->
						<!-- ........................................................................................................ -->
					</tr>
				</table>
			</body>
			<!-- ================================================================================= -->
		</html>
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="template_mycoresampleAlternative.write.content">
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />		
		
		<table class="content" align="center" style="width:90%;" cellpadding="0" cellspacing="0">
			<!-- page title -->
			<tr >
				<th align="left">
					<xsl:choose>
						<xsl:when test=" /MyCoReWebPage/section[lang($CurrentLang)]/@title != '' ">
							<xsl:value-of select="/MyCoReWebPage/section[lang($CurrentLang)]/@title "/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="/MyCoReWebPage/section[lang($DefaultLang)]/@title "/>
						</xsl:otherwise>
					</xsl:choose>
				</th>
			</tr>
			<!-- end: page title -->
			<tr>
				<td>
					<br/>
					<xsl:apply-templates>
						<xsl:with-param name="browserAddress" select="$browserAddress" />
						<xsl:with-param name="template" select="$template" />
					</xsl:apply-templates>
				</td>
			</tr>
		</table>
	</xsl:template>
	
</xsl:stylesheet>