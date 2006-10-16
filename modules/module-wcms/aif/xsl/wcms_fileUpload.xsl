<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" >
	<!-- wcmsAdmin  ============================================================================== -->
	<xsl:template name="wcmsFileUpload" >
		<xsl:choose>
			<xsl:when test=" /cms/status = 'upload' " >
				<xsl:call-template name="wcmsFileUpload.upload" />
			</xsl:when>
			<xsl:when test=" /cms/status = 'done' " >
				<xsl:call-template name="wcmsFileUpload.done" />
			</xsl:when>
			<xsl:when test="/cms/status = 'failed' " >
				<xsl:call-template name="wcmsFileUpload.failed" />
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- END OF: wcmsAdmin  ================================================================================= -->
	<!-- upload  ============================================================================== -->
	<xsl:template name="wcmsFileUpload.upload" >
		<form name="documentUpload" method="POST" action="{$WebApplicationBaseURL}servlets/WCMSFileUploadServlet{$HttpSession}" 
			enctype="multipart/form-data">
			<table width="40" border="0" cellspacing="0" cellpadding="0" align="center">
				<!--<xsl:call-template name="wcms.headline" >
					<xsl:with-param name="infoText" select="'Bild, Datei hochladen (Schritt 1/2)'"/>
				</xsl:call-template>-->
				<br/>
				<tr>
					<td colspan="2">
						<br/>
					</td>
				</tr>
				 		  <tr>

		    <td align="center" class="green"><b>'Bild, Datei hochladen (Schritt 1/2)'</b></td>

		  </tr> 
				<tr>
					<td>				
							<input name="document" type="file" size="25" maxlength="100000" accept="text/*" />
					</td>
				</tr>
				<tr>
					<td>
						<br/>
					</td>
				</tr>
				<tr>
					<td>
						<table cellspacing="0" cellpadding="0" align="left" class="reset">
							<!-- submit -->
							<tr>
								<td align="right">
									<img 
										src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/box_left.gif" 
										width="11" height="22" border="0" alt="" title="" />
								</td>
								<td align="right" valign="top">
									<input class="button" type="submit" value="jetzt hochladen" />
								</td>
								<td align="right">
									<img 
										src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/box_right.gif" 
										width="11" height="22" border="0" alt="" title="" />
								</td>
							</tr>
							<!-- END OF: submit -->
						</table>
					</td>
				</tr>
				<!-- END OF: safe button -->
			</table>
		</form>
	</xsl:template>
	<!-- END OF: upload  ================================================================================= -->
	<!-- done  ============================================================================== -->
	<xsl:template name="wcmsFileUpload.done" >
		<table width="90%" border="0" cellspacing="0" cellpadding="0" align="center">
			<!--<xsl:call-template name="wcms.headline" >
				<xsl:with-param name="infoText" select="'Bild, Datei hochladen (Schritt 2/2)'"/>
			</xsl:call-template>-->
			<br/>
			<tr>
				<td colspan="2">
					<br/>
					'Bild, Datei hochladen (Schritt 2/2)'
					<br/>
				</td>
			</tr>
			<!-- give general information about done action -->
			<tr>
				<th class="green" colspan="2"> Hochladen erfolgreich! </th>
			</tr>
			<!-- END OF: give general information about done action -->
			<tr>
				<td colspan="2">
					<br/>
					<br/>
					<b>
						<xsl:variable name="contentType" select="/cms/contentType" />
						<xsl:choose>
							<xsl:when test=" contains( $contentType, 'image' ) " > Sie haben ein Bild auf den Server kopiert. 
								</xsl:when>
							<xsl:otherwise> Sie haben eine Datei auf den Server kopiert. </xsl:otherwise>
						</xsl:choose>
					</b>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<br/>
					<br/>
					<a href="{$WebApplicationBaseURL}modules/module-wcms/aif/web/multimedia.xml">zurück zum WCMS</a>
					<!--  	<a href ="Javascript:var URL = unescape(window.opener.location.pathname);window.opener.location.href = URL; window.close();">zur?ck zum WCMS</a>	   -->
				</td>
			</tr>
		</table>
	</xsl:template>
	<!-- END OF: done  ================================================================================= -->
	<!-- failed  ==================================================================================== -->
	<xsl:template name="wcmsFileUpload.failed" >
		<table width="90%" border="0" cellspacing="0" cellpadding="0" align="center">
			<!-- sort order information -->
			<tr>
				<td> file upload hat nicht geklappt </td>
			</tr>
		</table>
	</xsl:template>
	<!-- END OF: failed  ================================================================================= -->
</xsl:stylesheet>