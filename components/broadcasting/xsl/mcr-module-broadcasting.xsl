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

<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
	>
	
	<xsl:param name="WebApplicationBaseURL"/>
	<xsl:param name="CurrentGroups"/>
	<xsl:param name="CurrentUser"/>
	<xsl:param name="HttpSession"/>
	<xsl:param name="JSessionID"/>
	
	<xsl:variable name="servlet" select="concat($WebApplicationBaseURL,'servlets/MCRBroadcastingServlet',$HttpSession)"/>
	<xsl:variable name="servletURIRes" select="'request:servlets/MCRBroadcastingServlet'"/>
	<xsl:variable name="sender">
		<xsl:value-of
			select="concat($WebApplicationBaseURL,$HttpSession,'/modules/broadcasting/config/mcr-module-broadcasting.xml')"/>
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
									<!-- send message -->
									<xsl:call-template name="send.message"/>
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
		<signal><xsl:value-of select="i18n:translate('component.broadcasting.signal.off')" /></signal>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="send.message">
		<xsl:variable name="message.body">
			<xsl:call-template name="get.message"/>
		</xsl:variable>
		<signal><xsl:value-of select="i18n:translate('component.broadcasting.signal.on')" /></signal>
		<message.header>
			<xsl:copy-of select="message.header/text()"/>
		</message.header>
		<message.body>
			<xsl:copy-of select="$message.body"/>
		</message.body>
		<message.tail>
			<xsl:copy-of select="message.tail/text()"/>
		</message.tail>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="get.message">
		<xsl:variable name="userMessage">
			<!-- check if user got message -->
			<xsl:for-each select="receivers//users">
				<xsl:if test="user[text()=$CurrentUser]">
					<xsl:value-of select="message.body/text()"/>
				</xsl:if>
			</xsl:for-each>
		</xsl:variable>
		<!-- no user message -> check if group got message -->
		<xsl:variable name="groupMessage">
			<xsl:if test="$userMessage=''">
				<xsl:variable name="groupMessTmp">
					<!-- check each <groups> group :-) -->
					<xsl:for-each select="receivers//groups">
						<xsl:variable name="messageFound">
							<!-- check each group within -->
							<xsl:for-each select="group">
								<xsl:if test="contains($CurrentGroups,text())">
									<xsl:value-of select="'true'"/>
								</xsl:if>
							</xsl:for-each>
						</xsl:variable>
						<xsl:if test="contains($messageFound,'true')">
							<xsl:value-of select="concat(message.body/text(),'#$#$#$#')"/>
						</xsl:if>
					</xsl:for-each>
				</xsl:variable>
				<!-- assign message, if one has been found -->
				<xsl:if test="$groupMessTmp!=''">
					<xsl:choose>
						<xsl:when test="contains($groupMessTmp,'#$#$#$#')">
							<xsl:value-of select="substring-before($groupMessTmp,'#$#$#$#')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$groupMessTmp"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
			</xsl:if>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$userMessage!=''">
				<xsl:value-of select="$userMessage"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$groupMessage"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="get.onAir">
		<!-- TODO, implement <from> <to> -->
		<xsl:choose>
			<xsl:when test="//onAirTime[@send='ever']">
				<xsl:value-of select="'true'"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'false'"/>
			</xsl:otherwise>
		</xsl:choose>
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
	<xsl:template name="get.registerCall">
		<xsl:param name="xmlConfig" />
		<xsl:variable name="sessionSensitive">
			<xsl:copy-of select="xalan:nodeset($xmlConfig)//sessionSensitive/text()"/>
		</xsl:variable>
		<xsl:copy-of select="concat($servlet,'?mode=addReceiver&amp;sessionSensitive=',$sessionSensitive)"/>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="module-broadcasting.getHeader">
		<xsl:variable name="config">
			<xsl:call-template name="get.config"/>
		</xsl:variable>
		<xsl:variable name="refreshRate">
			<xsl:value-of select="xalan:nodeset($config)//refreshRate/text()"/>
		</xsl:variable>
		<xsl:variable name="registerCall">
			<xsl:call-template name="get.registerCall">
				<xsl:with-param name="xmlConfig" select="xalan:nodeset($config)"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:if test="$initJS='true'">
			<script language="JavaScript" src="{$WebApplicationBaseURL}modules/broadcasting/web/JS/broadcasting.js"
				type="text/javascript"/>
			<script type="text/javascript">
				<xsl:value-of select="concat('receiveBroadcast(&#34;',$sender,'&#34;,&#34;',$registerCall,'&#34;,&#34;',$refreshRate,'&#34;);')"/>
			</script>
			<meta name="expires" content="-1" />
		</xsl:if>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="get.alreadyReceived">
		<xsl:variable name="sessionSensitive">
			<xsl:call-template name="get.sessionSensitive"/>
		</xsl:variable>
		<xsl:variable name="alreadyReceived">
			<xsl:copy-of select="document(concat($servletURIRes,'?mode=hasReceived&amp;sessionSensitive=',$sessionSensitive))"/>
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
	<xsl:template name="get.sessionSensitive">
		<xsl:copy-of select="//sessionSensitive/text()"/>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="get.config">
		<xsl:copy-of select="document('webapp:modules/broadcasting/config/mcr-module-broadcasting.xml')"/>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="get.initJS">
		<xsl:variable name="config">
			<xsl:call-template name="get.config"/>
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