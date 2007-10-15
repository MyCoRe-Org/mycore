<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	<xsl:include href="MyCoReLayout.xsl" />

	<xsl:variable name="PageTitle" select="'Module-ACL Editor'" />

	<xsl:template match="/mcr_access_rule_set">
		<div id="ACL-Editor">
			<script type="text/javascript" src="{concat($WebApplicationBaseURL,'modules/module-ACL-editor_v2/web/JS/aclEditor.js')}" language="JavaScript"></script>
			<link rel="stylesheet" type="text/css" href="{concat($WebApplicationBaseURL,'modules/module-ACL-editor_v2/web/CSS/acl_editor.css')}" />

			<div id="createRule">
				<input type="button" value="neue Regel" onclick="changeVisibility($('createNewRule'))" />
				<form id="createNewRule" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
					action="{concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=dataRequest&amp;action=createNewRule')}" method="post" accept-charset="UTF-8">
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
				action="{concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=dataRequest&amp;action=submitRule')}" method="post" accept-charset="UTF-8">
				<table>
					<xsl:for-each select="mcr_access_rule">
						<tr>
							<td>
								<table>
									<tr>
										<td>
											<input type="button" name="{RID}" value="{RID}" onclick="changeVisibility(N('{concat('Rule$',RID)}'))"/>
										</td>
										<td>
											<input name="{concat('RuleDesc$',RID)}" value="{DESCRIPTION}" onkeypress="setChanged(event)"/>
										</td>
									</tr>
								</table>
							</td>
						</tr>
						<tr>
							<td>
								<textarea cols="50" rows="4" style="display:none;" name="{concat('Rule$',RID)}" onkeypress="setChanged(event)">
									<xsl:value-of select="RULE"/>
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
