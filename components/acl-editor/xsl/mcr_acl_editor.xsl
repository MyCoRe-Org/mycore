<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	<xsl:include href="MyCoReLayout.xsl" />

	<!-- 
		see mcr_acl_editor_common.xsl for definition of following variables
		
		redirectURL
		servletName
		editorURL
		aclEditorURL
		dataRequest
		permEditor
		ruleEditor
		
		add
		edit
		delete
	-->

	<xsl:include href="mcr_acl_editor_common.xsl" />

	<xsl:variable name="PageTitle" select="'ACL Editor'" />

	<xsl:variable name="javaScript" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/JS/aclEditor.js')" />
	<xsl:variable name="css" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/CSS/acl_editor.css')" />

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
					<a href="{concat($aclEditorURL, '&amp;editor=ruleEditor', $filter)}">
						<xsl:value-of select="$ruleEditor" />
					</a>

					<xsl:variable name="permEditor" select="concat($dataRequest, '&amp;action=getPermEditor', $filter)" />
					<xsl:copy-of select="document($permEditor)" />

				</xsl:when>
				<xsl:when test="editor = $ruleEditor">
					<a href="{concat($aclEditorURL, '&amp;editor=permEditor', $filter)}">
						<xsl:value-of select="$permEditor" />
					</a>

					<xsl:variable name="ruleEditor" select="concat($dataRequest, '&amp;action=getRuleEditor')" />
					<xsl:copy-of select="document($ruleEditor)" />

				</xsl:when>
				<xsl:when test="editor = $embPermEditor">
					<xsl:variable name="redirectURL" select="redirect"/>
					<xsl:variable name="embPermEditor" select="concat($dataRequest, '&amp;action=getPermEditor&amp;emb=true&amp;cmd=', cmd, $filter)" />
					<xsl:copy-of select="document(concat($embPermEditor, '&amp;redir=', $redirectURL))" />

				</xsl:when>
			</xsl:choose>

		</div>
	</xsl:template>

	<xsl:template name="getEmbPermEditor">
		<xsl:param name="objId" />
		<xsl:param name="acPool" />

		<xsl:if test="($objId != '') and ($acPool != '')">
			<xsl:variable name="filter">
				<xsl:call-template name="buildFilter">
					<xsl:with-param name="objIdFilter" select="$objId" />
					<xsl:with-param name="acPoolFilter" select="$acPool" />
				</xsl:call-template>
			</xsl:variable>

			<xsl:variable name="embPermEditor" select="concat($aclEditorURL, '&amp;editor=embPermEditor', $filter)" />
			<xsl:copy-of select="document($embPermEditor)" />
		</xsl:if>

	</xsl:template>

</xsl:stylesheet>
