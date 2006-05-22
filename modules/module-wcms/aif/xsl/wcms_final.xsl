<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" >
	<!-- finalPage  ============================================================================== -->
	<xsl:template name="wcmsFinalPage" >
		<xsl:param name="href" />
		<table width="90%" cellspacing="0" cellpadding="0" align="center">
			<xsl:call-template name="wcms.headline" >
				<xsl:with-param name="infoText" select="'Systemstatus (Schritt 3/3)'"/>
			</xsl:call-template>
			<tr>
				<td>
					<br/>
				</td>
			</tr>
			<!-- give general information about done action -->
			<tr>
				<th align="left" class="green" colspan="2">
					Seite abgespeichert...<br/>
					Parser: <xsl:value-of select="/cms/usedParser"/>...<br/>
					Änderungen erfolgreich...
				</th>
			</tr>
			<!-- END OF: give general information about done action -->
			<tr>
				<td colspan="2">
					<br/>
					<br/>
					<table width="100%" cellspacing="0" cellpadding="0" class="table">
						<!-- show page -->
						<tr>
							<th align="left" width="200">Seite:</th>
							<td colspan="2">
								<b>
									<xsl:for-each 
										select="document($navigationBase)/navigation//item[@href]">
										<xsl:if test="@href = $href">
											<xsl:for-each select="ancestor-or-self::item">
												<xsl:value-of select="./label" />
												<xsl:if test="position() != last()"> > </xsl:if>
											</xsl:for-each>
										</xsl:if>
									</xsl:for-each>
									<xsl:if test="/cms/action = 'delete'" > / <xsl:value-of select="/cms/label" /> 
										</xsl:if>
								</b>
							</td>
						</tr>
						<!-- END OF: show page -->
						<!-- show done action -->
						<tr>
							<th align="left">ausgeführte Aktion:</th>
							<td colspan="2">
								<xsl:choose>
									<xsl:when test=" /cms/action = 'add' and /cms/action[@mode='intern']" > Seite 
										angelegt </xsl:when>
									<xsl:when test=" /cms/action = 'add' and /cms/action[@mode='extern']" > Link angelegt 
										</xsl:when>
									<xsl:when test=" /cms/action = 'edit' and /cms/action[@mode='intern']" > Seite 
										verändert </xsl:when>
									<xsl:when test=" /cms/action = 'edit' and /cms/action[@mode='extern']" > 
										Informationen zu Link verändert </xsl:when>
									<xsl:when test=" /cms/action = 'delete' and /cms/action[@mode='intern']" > Seite 
										gelöscht </xsl:when>
									<xsl:when test=" /cms/action = 'delete' and /cms/action[@mode='extern']" > Link 
										gelöscht </xsl:when>
								</xsl:choose>
							</td>
						</tr>
						<!-- END OF: show done action -->
						<!-- show further opportunities -->
						<tr>
							<th align="left">weitere WCMS-Nutzung:</th>
							<td colspan="2">
								<xsl:if test=" /cms/action != 'delete' and /cms/action[@mode = 'extern'] " >
									<img 
										src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
										width="16" height="8" border="0" alt="" title="" />
									<a href="{$href}" target="_blank">Seite ansehen</a>
									<br/>
								</xsl:if>
								<xsl:if test=" /cms/action != 'delete' and /cms/action[@mode = 'intern'] " >
									<img 
										src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
										width="16" height="8" border="0" alt="" title="" />
									<a href="{concat($WebApplicationBaseURL, substring-after($href,'/') )}" 
										target="_blank">Seite ansehen</a>
									<br/>
								</xsl:if>
								<img 
									src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
									width="16" height="8" border="0" alt="" title="" />
								<a href="javascript: document.useAgain.submit()">DBT-WCMS nochmals nutzen</a>
								<br/>
								<br/>
								<img 
									src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
									width="16" height="8" border="0" alt="" title="" />
								<a href="javascript: window.close()">abmelden</a>
							</td>
						</tr>
						<!-- END OF: show further opportunities -->
					</table>
				</td>
			</tr>
		</table>
		<form name="useAgain" action="{$ServletsBaseURL}WCMSLoginServlet" method="post">
			<input type="hidden" name="sessionID" >
				<xsl:attribute name="value">
					<xsl:value-of select="/cms/sessionID" />
				</xsl:attribute>
			</input>
		</form>
	</xsl:template>
	<!-- END OF: finalPage  ================================================================================= -->
</xsl:stylesheet>