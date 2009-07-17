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
			<xsl:when test="$CurrentLang = 'ar'">
				<bdo dir="rtl">
					<xsl:choose>
						<xsl:when test="$access='true'">
							<table width="100%">
								<!-- general infos -->
								<tr>
									<th align="right"><xsl:value-of select="i18n:translate('component.broadcasting.label.setup')" />&#160;<a href="{concat($WebApplicationBaseURL,'modules/broadcasting/web/editor/editor_start_broadcasting.xml')}" ><xsl:value-of select="i18n:translate('component.broadcasting.label.edit')" /></a></th>
								</tr>
								<tr>
									<td>
										<xsl:call-template name="main"/>
									</td>
								</tr>
								<xsl:call-template name="lineBreak"/>
								<!-- receiver list -->
								<tr>
									<th align="right"><xsl:value-of select="i18n:translate('component.broadcasting.label.receiver')" /></th>
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
											<th align="right"><xsl:value-of select="i18n:translate('component.broadcasting.label.clearlist')" /></th>
										</tr>
										<tr>
											<td align="right">
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
									<td align="right"><xsl:value-of select="i18n:translate('component.broadcasting.label.noaccess')" /></td>
								</tr>
							</table>
						</xsl:otherwise>
					</xsl:choose>
				</bdo>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="$access='true'">
						<table width="100%">
							<!-- general infos -->
							<tr>
								<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.setup')" />&#160;<a href="{concat($WebApplicationBaseURL,'modules/broadcasting/web/editor/editor_start_broadcasting.xml')}" ><xsl:value-of select="i18n:translate('component.broadcasting.label.edit')" /></a></th>
							</tr>
							<tr>
								<td>
									<xsl:call-template name="main"/>
								</td>
							</tr>
							<xsl:call-template name="lineBreak"/>
							<!-- receiver list -->
							<tr>
								<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.receiver')" /></th>
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
										<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.clearlist')" /></th>
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
								<td align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.noaccess')" /></td>
							</tr>
						</table>
					</xsl:otherwise>
				</xsl:choose>
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
			<xsl:when test="$CurrentLang = 'ar'">
				<bdo dir="rtl">
				<xsl:choose>
					<xsl:when test="empty" >
						<div style="text-align:right">
							<xsl:value-of select="i18n:translate('component.broadcasting.message.receive')"/>
						</div>
					</xsl:when>
					<xsl:otherwise>
						<table style="border:solid 1px;" width="100%">
							<tr>
								<th align="right"><xsl:value-of select="i18n:translate('component.broadcasting.list.login')" /></th>
								<th align="right"><xsl:value-of select="i18n:translate('component.broadcasting.list.ip')" /></th>
								<th align="right"><xsl:value-of select="i18n:translate('component.broadcasting.list.session')" /></th>
								<th align="right"><xsl:value-of select="i18n:translate('component.broadcasting.list.key')" /></th>
							</tr>
							<xsl:apply-templates select="receiver"/>
						</table>
					</xsl:otherwise>
				</xsl:choose>
				</bdo>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="empty" >
						<xsl:value-of select="i18n:translate('component.broadcasting.message.receive')"/>
					</xsl:when>
					<xsl:otherwise>
						<table style="border:solid 1px;" width="100%">
							<tr>
								<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.list.login')" /></th>
								<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.list.ip')" /></th>
								<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.list.session')" /></th>
								<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.list.key')" /></th>
							</tr>
							<xsl:apply-templates select="receiver"/>
						</table>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template match="receiver">
		<xsl:choose>
			<xsl:when test="$CurrentLang = 'ar'">
				<tr>
					<td align="right">
						<xsl:value-of select="details/login/text()"/>
					</td>
					<td align="right">
						<xsl:value-of select="details/ip/text()"/>
					</td>
					<td align="right">
						<xsl:value-of select="details/session-id/text()"/>
					</td>
					<td align="right">
						<xsl:value-of select="key/text()"/>
					</td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
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
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->
	<xsl:template name="clearReceiverList">
		<a href="{$servlet}?mode=clearReceiverList"><xsl:value-of select="i18n:translate('component.broadcasting.label.clearlist')" /></a>
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
		<xsl:choose>
			<xsl:when test="$CurrentLang = 'ar'">
				<table style="border:solid 1px; padding-left: 5px; padding-right: 5px; text-align:right" >
					<tr>
						<th><xsl:value-of select="i18n:translate('component.broadcasting.label.power')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//power/text()"/>
						</td>
					</tr>
					<tr>
						<th><xsl:value-of select="i18n:translate('component.broadcasting.label.onair')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//onAirTime/@send"/>
						</td>
					</tr>						
					<tr>
						<th><xsl:value-of select="i18n:translate('component.broadcasting.label.refresh')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//refreshRate/text()"/>&#160;<xsl:value-of select="i18n:translate('component.broadcasting.label.sec')" />
						</td>
					</tr>
					<tr>
						<th><xsl:value-of select="i18n:translate('component.broadcasting.label.sens')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//sessionSensitive/text()"/> 
						</td>
					</tr>			
				</table>
			</xsl:when>
			<xsl:otherwise>
				<table style="border:solid 1px; padding-left: 5px; padding-right: 5px;" >
					<tr>
						<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.power')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//power/text()"/>
						</td>
					</tr>
					<tr>
						<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.onair')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//onAirTime/@send"/>
						</td>
					</tr>						
					<tr>
						<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.refresh')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//refreshRate/text()"/>&#160;<xsl:value-of select="i18n:translate('component.broadcasting.label.sec')" />
						</td>
					</tr>
					<tr>
						<th align="left"><xsl:value-of select="i18n:translate('component.broadcasting.label.sens')" /></th>
						<td>
							<xsl:value-of select="xalan:nodeset($config)//sessionSensitive/text()"/> 
						</td>
					</tr>			
				</table>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ======================================================================================== -->	
</xsl:stylesheet>