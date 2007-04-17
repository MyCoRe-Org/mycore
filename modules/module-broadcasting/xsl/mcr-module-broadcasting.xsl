<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!--  MyCoRe - Module-Broadcasting 					-->
<!--  												-->
<!-- Module-Broadcasting 1.0, 05-2006  				-->
<!-- +++++++++++++++++++++++++++++++++++++			-->
<!--  												-->
<!-- Andreas Trappe 	- idea, concept, dev.		-->
<!--												-->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	
	<xsl:param name="WebApplicationBaseURL"/>
	<xsl:param name="CurrentGroups"/>
	<xsl:param name="CurrentUser"/>
	<xsl:param name="HttpSession"/>
	<xsl:param name="JSessionID"/>
	
	<xsl:variable name="servlet" select="concat($WebApplicationBaseURL,'servlets/MCRBroadcastingServlet',$HttpSession)"/>
	<xsl:variable name="servletURIRes" select="'request:servlets/MCRBroadcastingServlet'"/>
	<xsl:variable name="sender">
		<xsl:value-of
			select="concat($WebApplicationBaseURL,$HttpSession,'/modules/module-broadcasting/config/mcr-module-broadcasting.xml')"/>
	</xsl:variable>
	<xsl:variable name="initJS">
		<xsl:call-template name="get.initJS"/>
	</xsl:variable>
	
	<!-- ======================================================================================== -->
	<xsl:template match="/mcr-module-broadcasting">

		<mcr-module-broadcasting>
			
			<!-- already received ? -->
			<xsl:variable name="alreadyReceived">
				<xsl:call-template name="get.alreadyReceived"/>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="$alreadyReceived='true'">
					<xsl:call-template name="send.noSignal"/>
				</xsl:when>
				<xsl:otherwise>
					<!-- on air? -->
					<xsl:variable name="onAir">
						<xsl:call-template name="get.onAir"/>
					</xsl:variable>
					<xsl:choose>
						<xsl:when test="$onAir='false'">
							<xsl:call-template name="send.noSignal"/>
						</xsl:when>
						<xsl:otherwise>
							
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			
			<!--			<xsl:choose>
			<xsl:when test="$initJS='true'">
			<onAir>
			<xsl:value-of select="program/message/text()"/>
			</onAir>
			</xsl:when>
			<xsl:otherwise>
			<noSignal/>
			</xsl:otherwise>
			</xsl:choose>-->
			
		</mcr-module-broadcasting>
		
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="send.noSignal">
		<noSignal/>
	</xsl:template>
	<!-- ======================================================================================== -->	
	<xsl:template name="get.onAir">
		onAirTime
	</xsl:template>	
	<!-- ======================================================================================== -->	
	<xsl:template name="module-broadcasting.getHeader">
		<xsl:if test="$initJS='true'">
			<script language="JavaScript" src="{$WebApplicationBaseURL}modules/module-broadcasting/web/JS/broadcasting.js"
				type="text/javascript"/>
			<script type="text/javascript">
				<xsl:value-of select="concat('receiveBroadcast(&#34;',$sender,'&#34;,&#34;',$servlet,'&#34;);')"/>
			</script>
		</xsl:if>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="get.alreadyReceived">
		<xsl:variable name="alreadyReceived">
			<xsl:copy-of select="document(concat($servletURIRes,'?mode=hasReceived'))"/>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="xalan:nodeset($alreadyReceived)/mcr-module-broadcasting/true">
				<xsl:value-of select="'true'"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'false'"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->	
	<xsl:template name="get.initJS">
		<!-- power on ? -->
		<xsl:choose>
			<xsl:when test="/mcr-module-broadcasting/power/text()='on'">
				<!-- gast ?= receiver -->
				<xsl:choose>
					<xsl:when test="/mcr-module-broadcasting/receivers/@allowGuestGroup='true'">
						<xsl:value-of select="'true'"/>
					</xsl:when>
					<xsl:otherwise>
						<!-- user ?= gast -->
						<xsl:choose>
							<xsl:when test="$CurrentUser='gast'">
								<xsl:value-of select="'false'"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="'true'"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'false'"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->
</xsl:stylesheet>

			<xsl:when test="$CurrentUser!='gast' and contains($CurrentGroups,//group/text())">
				<xsl:variable name="alreadyReceived">
					<xsl:copy-of select="document(concat($servletURIRes,'?mode=hasReceived'))"/>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="xalan:nodeset($alreadyReceived)/mcr-module-broadcasting/true">
						<xsl:value-of select="'false'"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="'true'"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>








