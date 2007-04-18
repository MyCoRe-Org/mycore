<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!--  MyCoRe - Module-Broadcasting 					-->
<!--  												-->
<!-- Module-Broadcasting 1.0, 04-2007  				-->
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
							<!-- is user a receiver ? -->
							<xsl:variable name="isReceiver">
								<xsl:call-template name="get.isReceiver"/>
							</xsl:variable>
							<xsl:choose>
								<xsl:when test="$isReceiver='false'">
									<xsl:call-template name="send.noSignal"/>
								</xsl:when>
								<xsl:otherwise>
									<!-- register receiver as already received a message -->
									<xsl:call-template name="registerUser" />
									<!-- send message -->
									<xsl:call-template name="send.message" />
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			
		</mcr-module-broadcasting>
		
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="send.noSignal">
		<signal>off</signal>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="send.message">
		
		<xsl:variable name="message">
			
			<!-- check if user got message -->
			<xsl:for-each select="receivers//users">
				<xsl:if test="user[text()=$CurrentUser]">
					<xsl:value-of select="message/text()"/>
				</xsl:if>
			</xsl:for-each>
			
			<!-- if no user with message found, check if group got message -->
			
		</xsl:variable>
		
		
		<signal>on</signal>		
		<message><xsl:value-of select="$message" /></message>
		
	</xsl:template>	
	<!-- ======================================================================================== -->	
	<xsl:template name="get.onAir">
		<!-- TODO -->
		<xsl:value-of select="'true'" />
	</xsl:template>	
	<!-- ======================================================================================== -->	
	<xsl:template name="get.isReceiver">
		<xsl:variable name="tmp">
			<xsl:call-template name="get.isReceiver.tmp"/>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="contains($tmp,'true')">
				<xsl:value-of select="'true'"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'false'"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>	
	<!-- ======================================================================================== -->		
	<xsl:template name="get.isReceiver.tmp">
		
		<xsl:choose>
			<xsl:when test="$CurrentUser='gast' and receivers[@allowGuestGroup='false']"/>
			<xsl:otherwise>
				<!-- validate groups -->
				<xsl:if test="receivers//group">
					<xsl:for-each select="receivers//group">
						<xsl:if test="contains($CurrentGroups,text())">
							<xsl:value-of select="'true'"/>
						</xsl:if>
					</xsl:for-each>
				</xsl:if>
				<!-- validate users -->
				<xsl:if test="receivers//user">
					<xsl:for-each select="receivers//user">
						<xsl:if test="contains($CurrentUser,text())">
							<xsl:value-of select="'true'"/>
						</xsl:if>
					</xsl:for-each>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>

	</xsl:template>	
	<!-- ======================================================================================== -->
	<xsl:template name="registerUser">
		<xsl:variable name="tmp">
			<xsl:copy-of select="document(concat($servletURIRes,'?mode=addReceiver'))"/>
		</xsl:variable>
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
			<xsl:when test="xalan:nodeset($alreadyReceived)/mcr-module-broadcasting/hasReceived/text()='true'">
				<xsl:value-of select="'true'"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'false'"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->	
	<xsl:template name="get.initJS">
		<xsl:variable name="config">
			<xsl:copy-of select="document('webapp:modules/module-broadcasting/config/mcr-module-broadcasting.xml')"/>
		</xsl:variable>
		<!-- power on ? -->
		<xsl:choose>
			<xsl:when test="xalan:nodeset($config)//power/text()='on'">
				<!-- gast ?= receiver -->
				<xsl:choose>
					<xsl:when test="xalan:nodeset($config)//receivers[@allowGuestGroup='true']">
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