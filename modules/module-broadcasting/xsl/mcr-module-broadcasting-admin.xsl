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

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xalan">
	
	<xsl:include href="MyCoReLayout.xsl"/>
	<xsl:variable name="PageTitle" select="'Module-Broadcasting Monitoring'"/>
	
	<!-- ======================================================================================== -->
	<xsl:template match="/mcr-module-broadcasting-admin">
		<table width="100%">
			<!-- general infos -->
			<tr>
				<th align="left">General setup:</th>
			</tr>
			<tr>
				<td>
					<xsl:call-template name="main"/>
				</td>
			</tr>
			
			<xsl:call-template name="lineBreak"/>
			
			<!-- receiver list -->
			<tr>
				<th align="left">Receiver list:</th>
			</tr>
			<tr>
				<td>
					<xsl:apply-templates select="receiverList"/>
				</td>
			</tr>
			
			<xsl:call-template name="lineBreak"/>
			
			<xsl:choose>
				<xsl:when test="receiverList/empty"/>
				<xsl:otherwise>
					<!-- clear receiver list -->
					<tr>
						<th align="left">Clear receiver list:</th>
					</tr>
					<tr>
						<td>
							<xsl:call-template name="clearReceiverList"/>
						</td>
					</tr>
				</xsl:otherwise>
			</xsl:choose>
			
		</table>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template match="receiverList">
		<xsl:choose>
			<xsl:when test="empty" >
				<xsl:copy-of select="'Empty. No messages have been received, yet.'"/>
			</xsl:when>
			<xsl:otherwise>
				<table style="border:solid 1px;" width="100%">
					<tr>
						<th align="left">Login name</th>
						<th align="left">IP</th>
						<th align="left">Session-ID</th>
						<th align="left">Safed key</th>
					</tr>
					<xsl:apply-templates select="receiver"/>
				</table>
			</xsl:otherwise>
		</xsl:choose>

	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template match="receiver">
		<tr>
			<td>
				<xsl:value-of select="details/login/text()"/>
			</td>
			<td>
				<xsl:value-of select="details/ip/text()"/>
			</td>
			<td>
				<xsl:value-of select="details/session-id/text()"/>
			</td>
			<td>
				<xsl:value-of select="key/text()"/>
			</td>
		</tr>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="clearReceiverList">
		<a href="{$servlet}?mode=clearReceiverList">Clear receiver list </a>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="lineBreak">
		<tr>
			<td>
				<br/>
				<br/>
			</td>
		</tr>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="main">
		<xsl:variable name="config">
			<xsl:call-template name="get.config"/>
		</xsl:variable>
		<table style="border:solid 1px;" >
			<tr>
				<th align="left">Power:</th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//power/text()"/>
				</td>
			</tr>
			<tr>
				<th align="left">on air time:</th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//onAirTime/@send"/>
				</td>
			</tr>						
			<tr>
				<th align="left">refresh rate:</th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//refreshRate/text()"/> sec
				</td>
			</tr>
			<tr>
				<th align="left">session sensitive:</th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//sessionSensitive/text()"/> 
				</td>
			</tr>			
		</table>
	</xsl:template>
	<!-- ======================================================================================== -->	
</xsl:stylesheet>