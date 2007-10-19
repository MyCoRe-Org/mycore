<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	<xsl:include href="MyCoReLayout.xsl" />

	<!-- 
		see mcr_acl_editor_common.xsl for definition of following
		
		redirectURL
		servletName
		editorURL
		dataRequest
		permEditor
		ruleEditor
	-->

	<xsl:include href="mcr_acl_editor_common.xsl" />

	<xsl:variable name="PageTitle" select="'ACL Editor'" />

	<xsl:variable name="javaScript" select="concat($WebApplicationBaseURL,'modules/module-ACL-editor_v2/web/JS/aclEditor.js')" />
	<xsl:variable name="css" select="concat($WebApplicationBaseURL,'modules/module-ACL-editor_v2/web/CSS/acl_editor.css')" />

	<xsl:template match="mcr_acl_editor">
		<xsl:variable name="filter">
			<xsl:call-template name="buildFilter">
				<xsl:with-param name="objIdFilter" select="mcr_access_filter/objid" />
				<xsl:with-param name="acPoolFilter" select="mcr_access_filter/acpool" />
			</xsl:call-template>
		</xsl:variable>

		<div id="ACL-Editor">
			<script type="text/javascript" src="{$javaScript}" language="JavaScript" />
			<link rel="stylesheet" type="text/css" href="{$css}" />

			<xsl:choose>
				<xsl:when test="editor = $permEditor">
					<a href="{concat($editorURL, '?mode=getACLEditor&amp;editor=ruleEditor', $filter)}">
						<xsl:value-of select="$ruleEditor" />
					</a>

					<xsl:variable name="permEditor" select="concat($dataRequest, '&amp;action=getPermEditor', $filter)" />
					<xsl:copy-of select="document($permEditor)" />

				</xsl:when>
				<xsl:when test="editor = $ruleEditor">
					<a href="{concat($editorURL, '?mode=getACLEditor&amp;editor=permEditor', $filter)}">
						<xsl:value-of select="$permEditor" />
					</a>

					<xsl:variable name="ruleEditor" select="concat($dataRequest, '&amp;action=getRuleEditor')" />
					<xsl:copy-of select="document($ruleEditor)" />

				</xsl:when>
			</xsl:choose>
			
		</div>
	</xsl:template>

	<xsl:template name="buildFilter">
		<xsl:param name="objIdFilter" />
		<xsl:param name="acPoolFilter" />

		<xsl:variable name="objId" select="'&amp;objid='" />
		<xsl:variable name="acPool" select="'&amp;acpool='" />

		<xsl:choose>
			<xsl:when test="($objIdFilter != '') and ($acPoolFilter != '')">
				<xsl:value-of select="concat($objId, $objIdFilter, $acPool, $acPoolFilter)" />
			</xsl:when>
			<xsl:when test="$objIdFilter != ''">
				<xsl:value-of select="concat($objId, $objIdFilter)" />
			</xsl:when>
			<xsl:when test="$acPoolFilter != ''">
				<xsl:value-of select="concat($acPool, $acPoolFilter)" />
			</xsl:when>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
