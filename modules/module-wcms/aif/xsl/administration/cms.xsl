<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" >
	<xsl:variable name="PageTitle">
		<xsl:value-of select="'WCMS'"/>
	</xsl:variable>
	<xsl:include href="MyCoReLayout.xsl" />
	<xsl:include href="wcms_login.xsl" />
	<xsl:include href="wcms_admin.xsl" />
	<xsl:include href="wcms_choose.xsl" />
	<xsl:include href="wcms_edit.xsl" />
	<xsl:include href="wcms_final.xsl" />
	<xsl:include href="wcms_fileUpload.xsl" />

	<xsl:include href="wcms_kupu.xsl" />

	<!-- ================================================================================= -->
	<xsl:template match="cms">
		<span class="wcms">
			<xsl:choose>
				<xsl:when test="/cms/session = 'login' " >
					<xsl:call-template name="wcmsLogin" />
				</xsl:when>
				<xsl:when test="/cms/session = 'welcome' or /cms/session = 'logs' " >
					<xsl:call-template name="wcmsAdministration" />
				</xsl:when>
				<xsl:when test="/cms/session = 'choose' " >
					<xsl:call-template name="wcmsChooseAction" />
				</xsl:when>
				<xsl:when test=" /cms/session = 'action' " >
					<xsl:call-template name="wcmsEditContent" >
						<xsl:with-param name="href" select="/cms/href" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test=" /cms/session = 'final' " >
					<xsl:call-template name="wcmsFinalPage" >
						<xsl:with-param name="href" select="/cms/href" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test=" /cms/session = 'fileUpload' " >
					<xsl:call-template name="wcmsFileUpload" />
				</xsl:when>
			</xsl:choose>
		</span>
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="wcms.headline">
		<xsl:param name="infoText"/>
		<xsl:param name="align"/>
		<tr>
			<th width="50%" align="left">MyCoRe-WCMS :: das Web Content Management Modul</th>
			<th width="50%" align="right"> WCMS-Nutzer: '<xsl:value-of select="/cms/userID" />' (<xsl:value-of 
				select="/cms/userClass" />) </th>
		</tr>
		<xsl:choose>
			<xsl:when test="$align = 'left'">
				<tr>
					<th class="gray_noBorder" align="left" colspan="2">
						<br/>
						<xsl:value-of select="$infoText"/>
						<br/>
					</th>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<th class="gray_noBorder" align="right" colspan="2">
						<br/>
						<xsl:value-of select="$infoText"/>
						<br/>
					</th>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
		<tr>
			<td colspan="2">
				<br/>
			</td>
		</tr>
	</xsl:template>
	<!-- ================================================================================= -->
</xsl:stylesheet>