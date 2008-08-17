<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">

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
                <!--				<input type="button" value="neue Regel" onclick="changeVisibility($('createNewRule'))" />-->

                <div id="createNewRule">
                    <div style="font-weight: bold; padding-top: 3px; padding-bottom:3px"><xsl:value-of select="i18n:translate('acl-editor.label.newRule')" />:</div>
                    <form id="createNewRule" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                        action="{concat($dataRequest, '&amp;action=createNewRule')}" method="post" accept-charset="UTF-8">
                        <table>
                            <tr>
                                <td><xsl:value-of select="i18n:translate('acl-editor.label.description')" />:</td>
                                <td>
                                    <!-- Rule Description -->
                                    <input size="80px" name="newRuleDesc" value="" />
                                </td>
                            </tr>
                            <tr>
                                <td><xsl:value-of select="i18n:translate('acl-editor.label.rule')" />:</td>
                                <td>
                                    <!-- Rule string -->
                                    <textarea cols="80" rows="4" name="newRule" value="" />
                                </td>
                            </tr>

                            <tr>
                                <td>
                                    <input type="submit" value="{i18n:translate('acl-editor.button.create')}" />
                                </td>
                            </tr>
                        </table>
                        <br />
                    </form>
                </div>

            </div>
            <br />
            <form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                action="{concat($dataRequest, '&amp;action=submitRule')}" method="post" accept-charset="UTF-8">
                <input type="hidden" name="redir" value="{concat($aclEditorURL,$currentEditor)}" />
                <hr/>
                <table id="rule_table">
                    
                    <tr id="delAllBox">
                        <td id="openAll">
                            <div class="button" onMouseover="initOpenAll(this,'{i18n:translate('acl-editor.button.expandAll')}','{i18n:translate('acl-editor.button.collapsAll')}')">
                                <xsl:value-of select="i18n:translate('acl-editor.button.expandAll')" />
                            </div>
                            <div class="button" onclick="deleteAllFromDB('{concat($dataRequest, '&amp;action=delAllRules')}','{i18n:translate('acl-editor.msg.delAllRules')}')">
                                <xsl:value-of select="i18n:translate('acl-editor.button.delAllRules')" />
                            </div>
                        </td>
                        <td class="checkBox">
                            <xsl:value-of select="i18n:translate('acl-editor.label.markAll')" />:
                            <input type="checkbox" id="delAll" />
                        </td>
                    </tr>
                    <xsl:for-each select="mcr_access_rule">

                        <table style="width:100%; border:solid 1px black;">
                            <tr name="rule_line" id="{concat('Rule$',RID)}">
                                <td id="left">
                                    <!-- Button zum aufklappen -->
                                    <div class="visButton" name="visButton" onclick="changeVisibility('{concat('RuleField$',RID)}',this)">+</div>
                                </td>
                                <td><xsl:value-of select="i18n:translate('acl-editor.label.ruleID')" />:</td>
                                <td id="RuleID">
                                    <b>
                                        <xsl:value-of select="RID" />
                                    </b>
                                </td>
                                <xsl:variable name="chkBoxClass">
                                    <xsl:choose>
                                        <xsl:when test="inUse = 'true'">
                                            <xsl:value-of select="'delInactiv'" />
                                        </xsl:when>
                                        <xsl:when test="inUse = 'false'">
                                            <xsl:value-of select="'delActiv'" />
                                        </xsl:when>
                                    </xsl:choose>
                                </xsl:variable>
                                
                                <td id="{$chkBoxClass}">
                                    <xsl:value-of select="concat(i18n:translate('acl-editor.label.delete'),':')" />
                                    <xsl:choose>
                                        <xsl:when test="inUse = 'true'">
                                            <input type="checkbox" disabled="disabled" name="delete_rule" value="{RID}" />
                                            <div class="delInfo"><xsl:value-of select="i18n:translate('acl-editor.msg.delInfo')" /></div>
                                        </xsl:when>
                                        <xsl:when test="inUse = 'false'">
                                            <input type="checkbox" name="delete_rule" value="{RID}" />
                                        </xsl:when>
                                    </xsl:choose>
                                </td>
                            </tr>
                            <tr>
                                <td id="left"></td>
                                <td><xsl:value-of select="i18n:translate('acl-editor.label.description')" />:</td>
                                <td id="Description">
                                    <input size="80px" name="{concat('RuleDesc$',RID)}" value="{DESCRIPTION}" onkeypress="setChanged(event)" />
                                </td>
                            </tr>
                            <tr class="ruleField" id="{concat('RuleField$',RID)}">
                                <td id="left"></td>
                                <td><xsl:value-of select="i18n:translate('acl-editor.label.rule')" />:</td>
                                <td class="ruleField" id="ruleField">
                                    <textarea cols="80" rows="4" name="{concat('Rule$',RID)}" onkeypress="setChanged(event)">
                                        <xsl:value-of select="RULE" />
                                    </textarea>
                                </td>
                            </tr>
                        </table>
                        <br />
                    </xsl:for-each>
                    <input type="submit" value="{i18n:translate('acl-editor.button.saveChg')}" />
                </table>

            </form>
        </div>
    </xsl:template>

</xsl:stylesheet>
