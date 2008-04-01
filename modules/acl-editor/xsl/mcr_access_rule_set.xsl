<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">

	<!-- 
		see mcr_acl_editor_common.xsl for definition of following variables
		
		redirectURL
		servletName
		editorURL
		aclEditorURL
		dataRequest
		permEditor
		ruleEditor
	-->
	<xsl:include href="mcr_acl_editor_common.xsl" />
	<xsl:variable name="currentEditor" select="concat('&amp;editor=', $ruleEditor)" />

	<xsl:template match="/mcr_access_rule_set">
		<div id="ACL-Rule-Editor" onMouseover="initRuleEditor()">

			<div id="createRule">
				<input type="button" value="neue Regel" onclick="changeVisibility($('createNewRule'))" />
				<form id="createNewRule" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
					action="{concat($dataRequest, '&amp;action=createNewRule')}" method="post" accept-charset="UTF-8">
					<table>

						<tr>
							<td>
								<!-- Rule string -->
								<textarea cols="50" rows="4" name="newRule" value="" />
							</td>
						</tr>
						<tr>
							<td>
								<!-- Rule Description -->
								<input name="newRuleDesc" value="" />
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

			<form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
				action="{concat($dataRequest, '&amp;action=submitRule')}" method="post" accept-charset="UTF-8">
                <input type="hidden" name="redir" value="{concat($aclEditorURL,$currentEditor)}" />
				<table id="rule_table">
					<tr id="rule_head">
						<td>
							<input id="delAll" type="button" value="alles Löschen" />
						</td>
						<td>RuleID</td>
						<td>Description</td>
						<td>Rule</td>
					</tr>
					<xsl:for-each select="mcr_access_rule">

						<tr id="rule_line">
							<td id="delete">
								<input type="checkbox" name="delete_rule" value="{RID}" />
							</td>
							<td>
								<input type="button" name="{RID}" value="{RID}" onclick="changeVisibility(N('{concat('Rule$',RID)}')[0])" />
							</td>
							<td>
								<input name="{concat('RuleDesc$',RID)}" value="{DESCRIPTION}" onkeypress="setChanged(event)" />
							</td>

							<td>
								<textarea cols="50" rows="4" style="display:none;" name="{concat('Rule$',RID)}" onkeypress="setChanged(event)">
									<xsl:value-of select="RULE" />
								</textarea>
							</td>
						</tr>
					</xsl:for-each>
				</table>
				<input type="submit" value="Speichern" />
			</form>
		</div>
	</xsl:template>

</xsl:stylesheet>
