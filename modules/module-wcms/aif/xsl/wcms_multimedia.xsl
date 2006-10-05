<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">

	<xsl:template name="wcmsMultimedia">
		
		<xsl:variable name="servletAnswer_XML" 
		select="document(concat('request:','servlets/WCMSChooseServlet?XSL.Style=xml&amp;mode=getMultimedia'))"
    	/>
	    
		<form name="wcmsMultimedia" target="">
		<!--<xsl:if
			test="/cms/action[@mode='intern'] = 'add' or /cms/action[@mode='intern'] = 'edit'">-->
			<table align="center" class="table_noGrid" border="1" cellspacing="0"
				cellpadding="0">
				<tr>
					<td align="center" class="green" colspan="2">
						<b>
							<xsl:value-of
								select="concat(i18n:translate('wcms.multimedia'),':')"/>
						</b>
					</td>
				</tr>
				<tr>
					<th valign="top" align="left">
						<!-- list of available imaages -->
						<xsl:value-of
							select="concat(i18n:translate('wcms.images'),':')"/>
						<br/>
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
							<xsl:text>
							</xsl:text>
						</select>
						<!-- END OF: list of available imaages -->
						<!-- list of available documents -->
						<br/>
						<br/>
						<xsl:value-of
							select="concat(i18n:translate('wcms.otherDocs'),':')"/>
						<br/>
						<select size="1">
							<xsl:for-each select="$servletAnswer_XML/cms/documents/document">
								<option>
									<xsl:attribute name="value">
										<xsl:value-of select="node()"/>
									</xsl:attribute>
									<xsl:value-of select="node()"/>
								</option>
							</xsl:for-each>
							<xsl:text>
							</xsl:text>
						</select>
						<!-- END OF: list of available documents -->
						<!-- upload -->
						<br/>
						<br/>
						<a target="blank"
							href="{$ServletsBaseURL}WCMSFileUploadServlet?action=select&amp;wcmsID=0024">
							<xsl:value-of
								select="concat(i18n:translate('wcms.uploadImg'),':')"/>
						</a>
					</th>
					<!-- picture preview -->
					<th align="right" valign="top">
						<img name="image" width="120" height="120" border="0" alt=""
							title="">
							<xsl:attribute name="src">
								<xsl:value-of
									select="concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/imagePath,'/'),$servletAnswer_XML/cms/images/image)"/>
							</xsl:attribute>
						</img>
					</th>
				</tr>
				<!-- END OF: picture preview -->
				<tr>
					<th align="left" colspan="2"
						style="font-size:10px;font-weight:normal;">
						<!-- image path -->
						<b>
							<xsl:value-of
								select="concat(i18n:translate('wcms.imgPath'),':')"/>
						</b>
						<br/>
						<xsl:value-of
							select="concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/imagePath,'/'),'...')"/>
						<!-- document path -->
						<br/>
						<b>
							<xsl:value-of
								select="concat(i18n:translate('wcms.docPath'),':')"/>
						</b>
						<br/>
						<xsl:value-of
							select="concat($WebApplicationBaseURL,substring-after($servletAnswer_XML/cms/documentPath,'/'),'...')"/>
					</th>
				</tr>
			</table>
		<!--</xsl:if>-->
		</form>
	</xsl:template>	
</xsl:stylesheet>