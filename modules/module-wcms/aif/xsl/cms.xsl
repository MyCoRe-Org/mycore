<?xml version="1.0" encoding="ISO-8859-1" ?>

<!-- =====================================================================================
========================================================================================={

title: cms.xsl

Die Schaltzentrale der WCMS-Administrationsoberflaeche. 

	- erzeugt den Seitentitel
	- bindet weitere Stylevorlagen (xsl) ein
	- steuert den Aufruf von Templates

include:
	- MyCoReLayout.xsl
	- wcms_login.xsl
	- wcms_admin.xsl
	- wcms_choose.xsl
	- wcms_edit.xsl
	- wcms_final.xsl
	- wcms_fileUpload.xsl
	- wcms_help.xsl

template:
	- cms (match)
	- wcms.headline (name)

}=========================================================================================
====================================================================================== -->

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" >

	<!-- Erzeugen des Seitentitels-->
	<xsl:variable name="PageTitle">
		<xsl:value-of select="'WCMS'"/>
	</xsl:variable>

	<!-- Einbinden weiterer Stylesheets -->
	<xsl:include href="MyCoReLayout.xsl" />
	<xsl:include href="wcms_login.xsl" />
	<xsl:include href="wcms_admin.xsl" />
	<xsl:include href="wcms_choose.xsl" />
	<xsl:include href="wcms_edit.xsl" />
	<xsl:include href="wcms_final.xsl" />
	<xsl:include href="wcms_fileUpload.xsl" />
	<xsl:include href="wcms_help.xsl" />
	<xsl:include href="wcms_multimedia.xsl" />	

<!-- ====================================================================================={

section: Template: match="cms"

	- Prueft das Kindelement "section" auf Schluesselbegriffe 
	und startet entsprechende Templates

		o login - wcmslogin
		o welcome - wcmsAdministration
		o logs - wcmsAdministration
		o manageGlobal - wcmsAdministration
		o choose - choose
		o action - action
		o final - wcmsFinalPage
		o fileUpload - wcmsfileUpload

}===================================================================================== -->

	<xsl:template match="cms">
		<span class="wcms">
			<xsl:choose>
				<xsl:when test="/cms/session = 'login' " >
					<xsl:call-template name="wcmsLogin" />
				</xsl:when>
				<xsl:when test="/cms/session = 'welcome' " >
					<xsl:call-template name="wcmsChoose" >
						<xsl:with-param name="href" select="/cms/href" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="/cms/session = 'help' " >
					<xsl:call-template name="wcmsHelp" />
				</xsl:when>
				<xsl:when test="/cms/session = 'logs' " >
					<xsl:call-template name="wcmsAdministration" />
				</xsl:when>
				<xsl:when test="/cms/session = 'managGlobal' " >
					<xsl:call-template name="wcmsAdministration" />
				</xsl:when>
				<xsl:when test="/cms/session = 'choose' " >
					<xsl:call-template name="wcmsChoose" >
						<xsl:with-param name="href" select="/cms/href" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test=" /cms/session = 'action' " >
					<xsl:call-template name="wcmsEditContent" >
						<xsl:with-param name="href" select="/cms/href" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test=" /cms/session = 'final' " >
					<xsl:call-template name="wcmsChoose" >
						<xsl:with-param name="href" select="/cms/href" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test=" /cms/session = 'fileUpload' " >
					<xsl:call-template name="wcmsFileUpload" />
				</xsl:when>
				<xsl:when test="/cms/session = 'multimedia' " >
					<xsl:call-template name="wcmsMultimedia" />
				</xsl:when>
			</xsl:choose>
		</span>
	</xsl:template>

<!-- ====================================================================================={

section: Template: name="wcms.headline"

	- Erzeugt die Ueberschrift
	- Zeigt den Benutzernamen
	- generiert einen Infotext aus Parametern

parameters:
	infoText - anzuzeigener Text
	align - Textausrichtung

}===================================================================================== -->

	<xsl:template name="wcms.headline">
		<xsl:param name="infoText"/>
		<xsl:param name="align"/>
		<tr>
			<th width="50%" align="left">
				<xsl:value-of select="i18n:translate('wcms.title')" />
			</th>
			<th width="50%" align="right">
				<xsl:value-of select="concat(i18n:translate('wcms.admin.WCMSUser'),' :')"/>
				<xsl:value-of select="/cms/userID" />
				<xsl:text> (</xsl:text>
				<xsl:value-of select="/cms/userClass" />
				<xsl:text>) </xsl:text>
			</th>
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

<!-- =================================================================================== -->

</xsl:stylesheet>