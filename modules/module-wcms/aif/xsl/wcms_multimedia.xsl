<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">

	<xsl:template name="wcmsMultimedia">
		
		<xsl:variable name="servletAnswer_XML" 
		select="document(concat('request:','servlets/WCMSChooseServlet',$JSessionID,'?XSL.Style=xml&amp;mode=getMultimedia'))"
    	/>
	    
		<form name="wcmsMultimedia" target="">
		<!--<xsl:if
			test="/cms/action[@mode='intern'] = 'add' or /cms/action[@mode='intern'] = 'edit'">-->
			<table align="center" class="table_noGrid">
				<tr>
					<td align="center" class="green" colspan="2">
						<h2>
							<xsl:value-of
								select="concat(i18n:translate('wcms.multimedia'),':')"/>
						</h2>
						<br/>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:value-of
							select="concat(i18n:translate('wcms.images'),':')"/>
						<br/>
					</td>
				</tr>
				<tr cellspacing="2">
					<td valign="top" halign="left">
						<!-- list of available imaages -->
						<input type="hidden" name="JavaScriptImagePath_hidden"
							value="{concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/imagePath,'/'))}"/>
						<select size="1" name="selectPicturePreview"  
							onchange="previewPicture(document.wcmsMultimedia.JavaScriptImagePath_hidden.value)">
							<xsl:for-each select="$servletAnswer_XML/cms/images/image">
								<option>
									<xsl:attribute name="value">
										<xsl:value-of select="node()"/>
									</xsl:attribute>
									<xsl:value-of select="node()"/>
								</option>
							</xsl:for-each>
						</select>
						<br/>
						<br/>
						<!-- END OF: list of available imaages -->
						<!-- upload -->
						<a 
							href="{$ServletsBaseURL}WCMSFileUploadServlet{$HttpSession}">
							<xsl:value-of
								select="concat(i18n:translate('wcms.uploadImg'),':')"/>
						</a>
					</td>
					<td width="120" height="120" valign="top" halign="middle">
						<!-- picture preview -->
						<img id="thumb" height="" width="" name="image" onload="size()" onchange="size()">
							<xsl:attribute name="src">
								<xsl:value-of
									select="concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/imagePath,'/'),$servletAnswer_XML/cms/images/image)"/>
							</xsl:attribute>
						</img>	
					</td>
				</tr>
				<tr>
					<td>
						<!-- list of available documents -->
						<xsl:value-of
							select="concat(i18n:translate('wcms.otherDocs'),':')"/>
						<br/>
						<input type="hidden" name="JavaScriptDocument_hidden"
							value="{concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/documentPath,'/'))}"/>
						<select size="1" name="selectDocumentPreview"  
							onchange="previewDocument(document.wcmsMultimedia.JavaScriptDocument_hidden.value)">
							<xsl:for-each select="$servletAnswer_XML/cms/documents/document">
								<option>
									<xsl:attribute name="value">
										<xsl:value-of select="node()"/>
									</xsl:attribute>
									<xsl:value-of select="node()"/>
								</option>
							</xsl:for-each>
						</select>
						<!-- END OF: list of available documents -->
					</td>
					<td>
							<!--<xsl:value-of
								select="concat(i18n:translate('wcms.docPath'),':')"/>-->
						<a id="doc">
							<xsl:attribute name="href">
								<xsl:value-of
									select="concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/documentPath,'/'),$servletAnswer_XML/cms/documents/document)"/>
							</xsl:attribute>
							<xsl:value-of
								select="$servletAnswer_XML/cms/documents/document"/>
					    </a>
					</td>
				</tr>
				<tr>
					<td>
						<!-- upload -->
						<br/>
						<a 
							href="{$ServletsBaseURL}WCMSFileUploadServlet{$HttpSession}">
							<xsl:value-of
								select="concat(i18n:translate('wcms.uploadDoc'),':')"/>
						</a>
					</td>
				</tr>	
				<!-- END OF: picture preview -->
				<!--<tr>
					<th align="left" colspan="2"
						style="font-size:10px;font-weight:normal;">
						<!-#- image path -#->
						<b>
							<xsl:value-of
								select="concat(i18n:translate('wcms.imgPath'),':')"/>
						</b>
						<br/>
						<xsl:value-of
							select="concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/imagePath,'/'),'...')"/>
						<!-#- document path -#->
						<br/>
						<b>
							<xsl:value-of
								select="concat(i18n:translate('wcms.docPath'),':')"/>
						</b>
						<br/>
						<xsl:value-of
							select="concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/documentPath,'/'),'...')"/>
					</th>
				</tr>-->
			</table>
		<!--</xsl:if>-->
		</form>
	</xsl:template>	
</xsl:stylesheet>