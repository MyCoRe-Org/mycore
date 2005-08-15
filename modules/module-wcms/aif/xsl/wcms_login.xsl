<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" >
	<!-- WCMSLogin  ============================================================================== -->
	<xsl:template name="wcmsLogin" >
		<table width="90%" class="wcms" border="0" cellspacing="0" cellpadding="0" align="center">
			<tr>
				<th align="left">MyCoRe-WCMS :: das Web Content Management Modul</th>
			</tr>
			<tr>
				<td height="15" colspan="2"></td>
			</tr>
			<tr>
				<td colspan="2" align="left"> <br/> Sie können hier die Webpräsenz ihres Miless/MyCoRe-Servers verwalten. 
					<br/><br/><br/> <b>Authentifizierung:</b> </td>
			</tr>
			<!-- invalid user or password entered - - - - - - - -->
			<xsl:if test="/cms/error = 'denied'">
				<tr>
					<td colspan="2">
						<br/>
					</td>
				</tr>
				<tr>
					<th colspan="2" class="red" align="left"> <b>Anmeldung fehlgeschlagen:</b><br/> WCMS-Kennung oder Passwort 
						sind nicht korrekt. Bitte versuchen Sie es noch einmal. </th>
				</tr>
			</xsl:if>
			<!-- END OF: invalid user or password entered - - - - - - - -->
			<form action="{$ServletsBaseURL}WCMSLoginServlet" method="post">
				<tr>
					<td colspan="2">
						<br></br>
						<table class="table_noGrid" border="0" cellspacing="0" cellpadding="0">
							<tr>
								<th align="left">WCMS-Kennung:</th>
								<th align="right">
									<input type="text" size="25" maxlength="16" name="userID"/>
								</th>
							</tr>
							<tr>
								<th align="left">Passwort:</th>
								<th align="right">
									<input type="password" size="25" maxlength="20" name="userPassword"/>
								</th>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td height="10" colspan="2"></td>
				</tr>
				<tr>
					<td colspan="2">
						<table border="0" cellspacing="0" cellpadding="0" align="left">
							<tr align="left">
								<td width="138"></td>
								<td>
									<img 
										src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/box_left.gif" 
										width="11" height="22" border="0" alt="" title="" />
								</td>
								<td>
									<input class="button" value="anmelden" type="submit"/>
								</td>
								<td>
									<img 
										src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/box_right.gif" 
										width="11" height="22" border="0" alt="" title="" />
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</form>
		</table>
	</xsl:template>
	<!-- END OF: WCMSLogin  ================================================================================= -->
</xsl:stylesheet>