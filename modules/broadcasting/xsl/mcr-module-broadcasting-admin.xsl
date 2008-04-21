<?xml version="1.0" encoding="UTF-8"?>

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
	xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
	>
	
	<xsl:include href="MyCoReLayout.xsl"/>
	<xsl:variable name="PageTitle" select="'Module-Broadcasting Monitoring'"/>
	<xsl:variable name="access">
		<xsl:call-template name="get.access" />
	</xsl:variable>
	
	<!-- ======================================================================================== -->
	
	<xsl:template match="/mcr-module-broadcasting-admin">
		<xsl:choose>
			<xsl:when test="$access='true'">
				<table width="100%">
					<!-- general infos -->
					<tr>
						<th align="left"><xsl:value-of select="i18n:translate('broadcasting.label.setup')" />&#160;<a href="{concat($WebApplicationBaseURL,'modules/broadcasting/web/editor/editor_start_broadcasting.xml')}" ><xsl:value-of select="i18n:translate('broadcasting.label.edit')" /></a></th>
					</tr>
					<tr>
						<td>
							<xsl:call-template name="main"/>
						</td>
					</tr>
					<xsl:call-template name="lineBreak"/>
					<!-- receiver list -->
					<tr>
						<th align="left"><xsl:value-of select="i18n:translate('broadcasting.label.receiver')" /></th>
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
								<th align="left"><xsl:value-of select="i18n:translate('broadcasting.label.clearlist')" /></th>
							</tr>
							<tr>
								<td>
									<xsl:call-template name="clearReceiverList"/>
								</td>
							</tr>
						</xsl:otherwise>
					</xsl:choose>
				</table>
			</xsl:when>
			<xsl:otherwise>
				<table width="100%">
					<tr>
						<td align="left"><xsl:value-of select="i18n:translate('broadcasting.label.noaccess')" /></td>
					</tr>
				</table>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="get.access">
		<xsl:value-of select="/mcr-module-broadcasting-admin/receiverList/@access"/>
	</xsl:template>
	<!-- ======================================================================================== -->	
	<xsl:template match="receiverList">
		<xsl:choose>
			<xsl:when test="empty" >
				<xsl:value-of select="i18n:translate('broadcasting.message.receive')"/>
			</xsl:when>
			<xsl:otherwise>
				<table style="border:solid 1px;" width="100%">
					<tr>
						<th align="left"><xsl:value-of select="i18n:translate('broadcasting.list.login')" /></th>
						<th align="left"><xsl:value-of select="i18n:translate('broadcasting.list.ip')" /></th>
						<th align="left"><xsl:value-of select="i18n:translate('broadcasting.list.session')" /></th>
						<th align="left"><xsl:value-of select="i18n:translate('broadcasting.list.key')" /></th>
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
		<a href="{$servlet}?mode=clearReceiverList"><xsl:value-of select="i18n:translate('broadcasting.label.clearlist')" /></a>
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
				<th align="left"><xsl:value-of select="i18n:translate('broadcasting.label.power')" /></th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//power/text()"/>
				</td>
			</tr>
			<tr>
				<th align="left"><xsl:value-of select="i18n:translate('broadcasting.label.onair')" /></th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//onAirTime/@send"/>
				</td>
			</tr>						
			<tr>
				<th align="left"><xsl:value-of select="i18n:translate('broadcasting.label.refresh')" /></th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//refreshRate/text()"/>&#160;<xsl:value-of select="i18n:translate('broadcasting.label.sec')" />
				</td>
			</tr>
			<tr>
				<th align="left"><xsl:value-of select="i18n:translate('broadcasting.label.sens')" /></th>
				<td>
					<xsl:value-of select="xalan:nodeset($config)//sessionSensitive/text()"/> 
				</td>
			</tr>			
		</table>
	</xsl:template>
	<!-- ======================================================================================== -->	
</xsl:stylesheet>