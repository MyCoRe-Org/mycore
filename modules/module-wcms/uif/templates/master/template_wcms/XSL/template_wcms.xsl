<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink">
	<!-- ================================================================================= -->		
	<!-- ================================================================================= -->	
	<xsl:template name="template_wcms" >
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />

		<xsl:call-template name="template_wcms.header" >
			<xsl:with-param name="browserAddress" select="$browserAddress" />
			<xsl:with-param name="template" select="$template" />
		</xsl:call-template>
		
		<html>
			<xsl:choose>
				<xsl:when test=" (/cms/session = 'action') or (/cms/session = 'translate') " >
					<body leftmargin="0" rightmargin="0" topmargin="0" bgcolor="#FFFFFF" onload="kupu = startKupu()">
						<xsl:call-template name="template_wcms.body" >
							<xsl:with-param name="browserAddress" select="$browserAddress" />
							<xsl:with-param name="template" select="$template" />
						</xsl:call-template>
					</body>
				</xsl:when>
				<xsl:otherwise>
					<body leftmargin="0" rightmargin="0" topmargin="0" bgcolor="#FFFFFF">
						<xsl:call-template name="template_wcms.body" >
							<xsl:with-param name="browserAddress" select="$browserAddress" />
							<xsl:with-param name="template" select="$template" />
						</xsl:call-template>
					</body>
				</xsl:otherwise>
			</xsl:choose>
		</html>
		
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->	
		
	<xsl:template name="template_wcms.header">
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />
		<head>
			<meta http-equiv="content-type" content="text/html;charset=ISO-8859-1"/>
			<title>
				<xsl:call-template name="PageTitle"/>
			</title>
			<script src="{$WebApplicationBaseURL}modules/module-wcms/uif/common/JavaScript/WCMSJavaScript.js" type="text/javascript"></script>
			<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_general.css" 
				rel="stylesheet"/>
			<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_navigation.css" 
				rel="stylesheet"/>
			<link href="{$WebApplicationBaseURL}modules/module-wcms/templates/master/{$template}/CSS/style_content.css" 
				rel="stylesheet"/>
			<link href="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupustyles.css" rel="stylesheet" type="text/css"/>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/sarissa.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupuhelpers.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupueditor.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupubasetools.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupuloggers.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupucontentfilters.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupucontextmenu.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupuinit.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupustart.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupusaveonpart.js">
			</script>
			<script type="text/javascript" src="{$WebApplicationBaseURL}modules/module-wcms/thirdParty/kupu/kupusourceedit.js">
			</script>
		</head>
	</xsl:template>	
	<!-- ================================================================================= -->
	<xsl:template name="template_wcms.body">
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />
		<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
			<tr valign="top">
				<!-- ........................................................................................................ -->
				<!-- general column right -->
				<td height="100%">
					<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td>
								<br/>
							</td>
						</tr>
						<!-- ............................................ -->
						<!-- content area -->
						<tr valign="top">
							<td height="100%">
								<xsl:call-template name="template_wcms.write.content" >
									<xsl:with-param name="browserAddress" select="$browserAddress" />
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
	</xsl:template>
	<!-- ============================================================================================= -->
	<xsl:template name="template_wcms.write.content">
		<xsl:param name="browserAddress" />
		<xsl:param name="template" />
		<xsl:apply-templates>
			<xsl:with-param name="browserAddress" select="$browserAddress" />
			<xsl:with-param name="template" select="$template" />
		</xsl:apply-templates>
	</xsl:template>
	<!-- ============================================================================================= -->
</xsl:stylesheet>