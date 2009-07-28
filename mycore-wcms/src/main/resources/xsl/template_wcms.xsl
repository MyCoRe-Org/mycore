<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink">

<!-- ================================================================================= -->		
	<xsl:template name="template_wcms" >
            
		<html>                  
			<xsl:call-template name="template_wcms.header" />                  
                  
			<xsl:choose>
				<xsl:when test=" (/cms/session = 'action') or (/cms/session = 'translate') " >
					<body leftmargin="0" rightmargin="0" topmargin="0" bgcolor="#FFFFFF">
						<xsl:call-template name="template_wcms.body" />
					</body>
				</xsl:when>
				<xsl:otherwise>
					<body leftmargin="0" rightmargin="0" topmargin="0" bgcolor="#FFFFFF">
						<xsl:call-template name="template_wcms.body" />
					</body>
				</xsl:otherwise>
			</xsl:choose>
		</html>

	</xsl:template>

<!-- ================================================================================= -->	
		
	<xsl:template name="template_wcms.header">
		<head>
			<title>
				<xsl:call-template name="PageTitle"/>
			</title>

			<xsl:variable 
				name="JSPfad" 
				select="concat($WebApplicationBaseURL,'templates/master/template_wcms/JAVASCRIPT')" />
			<xsl:variable 
				name="CSSPfad" 
				select="concat($WebApplicationBaseURL,'templates/master/template_wcms/CSS')" />

			<link 
				href="{$CSSPfad}/style_general.css" 
				rel="stylesheet"/>
			
			<link 
				href="{$CSSPfad}/tabManager.css" 
				rel="stylesheet"/>

			<script
				type="text/javascript"
				src="{$JSPfad}/WCMSJavaScript.js" />

			<script
				type="text/javascript"
				src="{$WebApplicationBaseURL}fck/fckeditor.js" />
			
			<script
				type="text/javascript"
				src="{$JSPfad}/tabManager.js" />
			<xsl:call-template name="module-broadcasting.getHeader"/>			
		</head>
	</xsl:template>	

	<!-- ================================================================================= -->
	<xsl:template name="template_wcms.body">
		<xsl:variable 
			name="BilderPfad" 
			select="concat($WebApplicationBaseURL,'templates/master/template_wcms/IMAGES')" />

<!-- /DIV header -->
		<div id="header">
	<!-- Seitenkopf -->
		<!-- MyCoRe Logo -->
			<div id="mycore-logo">
				<p>
					<img src="{$BilderPfad}/mylogo.gif" />
				</p>
			</div>
		<xsl:choose>	
	        <xsl:when test="/cms/session='multimedia'">
			</xsl:when>
			<xsl:otherwise>		
			<!-- Name oder Logo des Projektes -->
				<div id="projekt-name">
					<p>
						<xsl:value-of select="$MainTitle"/>
					</p>
					<!-- Hamburg University Press -->
				</div>
	
			<!-- Name des Benutzers -->
				<div id="benutzer-name">
					<p>
						<xsl:text>Benutzer: </xsl:text>
						<xsl:choose>
							<xsl:when test="/cms/userID!=''">
								<xsl:value-of select="/cms/userID" />
								<xsl:text> (</xsl:text><xsl:value-of select="/cms/userClass" />)
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>unangemeldet</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</p>
				</div>
	        </xsl:otherwise>
		</xsl:choose>
		<!-- Container schliessen, Float beenden -->
			<div id="clearhead">
				&#160;
			</div>

	<!-- Ende: Seitenkopf -->
		</div>
<!-- /DIV header -->

<!-- DIV main -->
		<div id="main">
			<xsl:apply-templates />
		</div>
<!-- /DIV main -->

	</xsl:template>
	<!-- ============================================================================================= -->
</xsl:stylesheet>
