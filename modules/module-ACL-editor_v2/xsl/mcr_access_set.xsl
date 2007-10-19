<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:java="http://xml.apache.org/xalan/java">

	<!-- 
		see mcr_acl_editor_common.xsl for definition of following
		
		redirectURL
		servletName
		editorURL
		dataRequest
	-->
	<xsl:include href="mcr_acl_editor_common.xsl" />

	<xsl:template match="/mcr_access_set">
		<xsl:variable name="ruleItems" select="document(concat($dataRequest, '&amp;action=getRuleAsItems'))" />


		<div id="ACL-Perm-Editor" onMouseover="initPermEditor()">
			<!-- ACL-Perm-Editor will be included into ACL Editor so link for JavaScript and CSS will be defined in mcr_acl_edior.xsl -->

			<!-- New Mapping -->
			<xsl:call-template name="createNewMapping">
				<xsl:with-param name="ruleItems" select="$ruleItems" />
			</xsl:call-template>

			<!-- Filter -->
			<xsl:apply-templates select="mcr_access_filter" />

			<!-- Mapping Table -->
			<form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
				action="{concat($dataRequest, '&amp;action=submitPerm')}" method="post" accept-charset="UTF-8">
				<table id="mapping_table">
					<tr id="mapping_head">
						<td>
							<input id="delAll" type="button" value="alles Löschen" />
						</td>
						<td>ObjId</td>
						<td>AcPool</td>
						<td>RID</td>
					</tr>
					<xsl:for-each select="mcr_access">
						<tr id="mapping_line">
							<td id="delete">
								<input type="checkbox" name="delete_mapping" value="{concat(OBJID,'$',ACPOOL)}" />
							</td>
							<td id="OBJID">
								<xsl:value-of select="OBJID" />
							</td>
							<td id="ACPOOL">
								<xsl:value-of select="ACPOOL" />
							</td>
							<td>
								<xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
									<xsl:with-param name="rid" select="RID" />
									<xsl:with-param name="name" select="concat(OBJID,'$',ACPOOL)" />
								</xsl:apply-templates>
							</td>
						</tr>
					</xsl:for-each>
				</table>
				<input type="submit" value="Speichern" />
			</form>
		</div>
	</xsl:template>

	<!-- Template for creating new access mapping -->
	<xsl:template name="createNewMapping">
		<xsl:param name="ruleItems" />

		<div id="createNewPerm">
			<input type="button" value="neue Regel" onclick="changeVisibility($('createNewPermForm'))" />
			<form id="createNewPermForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
				action="{concat($dataRequest, '&amp;action=createNewPerm', $redirectURL)}" method="post" accept-charset="UTF-8">
				<table>
					<tr>
						<td>
							<input name="newPermOBJID" value="" />
						</td>
						<td>
							<input name="newPermACPOOL" value="" />
						</td>
						<td>
							<xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
								<xsl:with-param name="rid" select="RID" />
								<xsl:with-param name="name" select="'newPermRID'" />
							</xsl:apply-templates>
						</td>
					</tr>
					<tr>
						<td>
							<input type="submit" value="Anlegen" />
						</td>
					</tr>
				</table>
			</form>

		</div>
	</xsl:template>

	<!-- Template for filter -->
	<xsl:template match="mcr_access_filter">
		<form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
			action="{concat($dataRequest, '&amp;action=setFilter', $redirectURL)}" method="post" accept-charset="UTF-8">
			<table>
				<tr>
					<td>
						<input name="ObjIdFilter" value="{objid}" />
					</td>
					<td>
						<input name="AcPoolFilter" value="{acpool}" />
					</td>
					<td>
						<input type="submit" value="Filtern" />
					</td>
					<td>
						<input onClick="self.location.href='{concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=dataRequest&amp;action=deleteFilter')}'"
							value="Filter loeschen" type="button" />
					</td>
				</tr>
			</table>
		</form>
	</xsl:template>

	<!-- Template for drop down box of Rid's -->
	<xsl:template match="items">
		<xsl:param name="rid" />
		<xsl:param name="name" />

		<select size="1" name="{$name}">
			<xsl:if test="$rid != ''">
				<xsl:attribute name="onchange">
					<xsl:value-of select="'setChanged(event)'" />
				</xsl:attribute>
			</xsl:if>
			<option value="'bitte waehlen'">bitte waehlen</option>
			<xsl:for-each select="item">
				<option value="{@value}">
					<xsl:if test="@value=$rid">
						<xsl:attribute name="selected">selected</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="@label" />

				</option>
			</xsl:for-each>
		</select>
	</xsl:template>
</xsl:stylesheet>
