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
    <xsl:variable name="labelDescription" select="concat(i18n:translate('acl-editor.label.description'),':')" />
    <xsl:variable name="labelRule" select="concat(i18n:translate('acl-editor.label.rule'),':')" />
    <xsl:variable name="labelRuleId" select="concat(i18n:translate('acl-editor.label.ruleID'),':')" />
    <xsl:variable name="labelDelete" select="concat(i18n:translate('acl-editor.label.delete'),':')" />

    <xsl:variable name="inUseInfoMsg" select="i18n:translate('acl-editor.msg.delInfo')" />
    <xsl:variable name="inUseInfoMsgID" select="'aclInUseInfoMsg'" />

    <xsl:variable name="aclRuleEditorJS" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/JS/aclRuleEditor.js')" />
    <xsl:variable name="aclClickButtonsJS" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/JS/aclClickButtons.js')" />
    <xsl:variable name="aclRuleEditorCSS" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/CSS/aclRuleEditor.css')" />

    <xsl:template match="/mcr_access_rule_set">
        <div>
            <!-- Setting JS and CSS files -->
            <script type="text/javascript" src="{$aclRuleEditorJS}" language="JavaScript"></script>
            <script type="text/javascript" src="{$aclClickButtonsJS}" language="JavaScript"></script>
            <link rel="stylesheet" type="text/css" href="{$aclRuleEditorCSS}"></link>

            <div id="aclRuleEditor" onmouseover="aclRuleEditorSetup()">
                <div id="aclCreateNewRuleBox">
                    <div class="aclRuleEditorlabel">
                        <xsl:value-of select="i18n:translate('acl-editor.label.newRule')" />
                    </div>
                    <div class="aclCreateNewRule">
                        <form id="aclCreateNewRuleForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                            action="{concat($dataRequest, '&amp;action=createNewRule')}" method="post" accept-charset="UTF-8">
                            <input type="hidden" name="redir" value="{concat($aclEditorURL,$currentEditor)}" />
                            <table class="aclRuleTable">
                                <tr>
                                    <td class="label">
                                        <xsl:value-of select="$labelDescription" />
                                    </td>
                                    <td>
                                        <!-- New rule description -->
                                        <input class="input" id="aclNewRuleDesc" name="newRuleDesc" />
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        <xsl:value-of select="$labelRule" />
                                    </td>
                                    <td>
                                        <!-- New rule string -->
                                        <textarea class="textarea" id="aclNewRuleStr" name="newRule" />
                                    </td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td>
                                        <input id="createNewRuleButon" class="button" type="button" value="{i18n:translate('acl-editor.button.create')}"
                                            msgDesc="{i18n:translate('acl-editor.msg.emptyDesc')}" msgRule="{i18n:translate('acl-editor.msg.emptyRule')}" />
                                    </td>
                                </tr>
                            </table>
                        </form>
                    </div><!-- End aclCreateNewRuleForm -->

                </div><!-- End aclCreateNewRuleBox -->

                <div id="aclEditRuleBox">
                    <div class="aclRuleEditorlabel">
                        <xsl:value-of select="i18n:translate('acl-editor.label.ruleSys')" />
                    </div>
                    <div class="aclRuleTableBox">
                        <div class="menu">
                            <table>
                                <tr>
                                    <td class="openAll">
                                        <!-- Details button -->
                                        <div id="detailsAllButton" class="clickButtonOut" altLabel="{i18n:translate('acl-editor.button.collapsAll')}">
                                            <xsl:value-of select="i18n:translate('acl-editor.button.expandAll')" />
                                        </div>
                                    </td>
                                    <td class="deleteAll">
                                        <!-- Delete all rules button -->
                                        <div id="delAllRulesButton" class="clickButtonOut" cmd="{concat($dataRequest, '&amp;action=delAllRules')}"
                                            msg="{i18n:translate('acl-editor.msg.delAllRules')}">
                                            <xsl:value-of select="i18n:translate('acl-editor.button.delAllRules')" />
                                        </div>
                                    </td>
                                    <td class="checkBoxRow">

                                        <table>
                                            <tr>
                                                <td class="space"></td>
                                                <td>
                                                    <xsl:value-of select="concat(i18n:translate('acl-editor.label.markAll'),':')" />
                                                </td>
                                                <td>
                                                    <input class="checkBox" type="checkbox" id="delAllRulesCheckBox" />
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </div>
                        <form id="aclEditRuleBoxForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                            action="{concat($dataRequest, '&amp;action=submitRule')}" method="post" accept-charset="UTF-8">
                            <input type="hidden" name="redir" value="{concat($aclEditorURL,$currentEditor)}" />

                            <xsl:for-each select="mcr_access_rule">
                                <xsl:variable name="ruleFieldID" select="concat('RuleField$',RID)" />
                                <xsl:variable name="ruleFieldButtonID" select="concat('RuleFieldButton$',RID)" />
                                <xsl:variable name="checkBoxID" select="concat('CheckBox$',RID)" />
                                <xsl:variable name="ruleInUseID" select="concat('RuleInUse$',RID)" />
                                <table class="aclRuleTable" id="{RID}">
                                    <tr name="rule_line" id="{concat('Rule$',RID)}">
                                        <td class="buttonCol">
                                            <!-- Button for details -->
                                            <div class="detailsSwitch clickButtonOut" id="{$ruleFieldButtonID}">+</div>
                                        </td>
                                        <td>
                                            <xsl:value-of select="$labelRuleId" />
                                        </td>
                                        <td class="ruleID">
                                            <xsl:value-of select="RID" />
                                        </td>

                                        <!-- Delete checkbox -->
                                        <xsl:choose>
                                            <xsl:when test="inUse = 'true'">
                                                <td id="{$ruleInUseID}" class="delInactiv" hoverMsg="{$inUseInfoMsg}" msgID="{$inUseInfoMsgID}">
                                                    <table>
                                                        <tr>
                                                            <td>
                                                                <xsl:value-of select="$labelDelete" />
                                                            </td>
                                                            <td>
                                                                <input class="checkBox" type="checkbox" disabled="disabled" name="delete_rule" value="{RID}" />
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </xsl:when>
                                            <xsl:when test="inUse = 'false'">
                                                <td class="delActiv">
                                                    <table>
                                                        <tr>
                                                            <td>
                                                                <xsl:value-of select="$labelDelete" />
                                                            </td>
                                                            <td>
                                                                <input id="{$checkBoxID}" class="checkBox" type="checkbox" name="delete_rule" value="{RID}" />
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </xsl:when>
                                        </xsl:choose>
                                    </tr>
                                    <tr>
                                        <td class="buttonCol"></td>
                                        <td class="label">
                                            <xsl:value-of select="$labelDescription" />
                                        </td>
                                        <td id="Description">
                                            <input class="input" name="{concat('RuleDesc$',RID)}" value="{DESCRIPTION}" onkeypress="setChanged(event)" />
                                        </td>
                                    </tr>
                                    <tr class="ruleField" id="{$ruleFieldID}">
                                        <td class="buttonCol"></td>
                                        <td class="label">
                                            <xsl:value-of select="$labelRule" />
                                        </td>
                                        <td id="ruleField">
                                            <textarea class="textarea" name="{concat('Rule$',RID)}" onkeypress="setChanged(event)">
                                                <xsl:value-of select="RULE" />
                                            </textarea>
                                        </td>
                                    </tr>
                                </table>
                            </xsl:for-each>
                            <input class="button" type="submit" value="{i18n:translate('acl-editor.button.saveChg')}" />
                        </form>
                    </div>
                </div><!-- End aclEditRuleBox -->
                <div id="{$inUseInfoMsgID}" class="msgWindow">
                    <xsl:value-of select="$inUseInfoMsg" />
                </div>
            </div><!-- End aclRuleEditor -->












            <!-- ##################################################################################### --><!--
                <div id="ACL-Rule-Editor" onMouseover="initRuleEditor()">
                <form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                action="{concat($dataRequest, '&amp;action=submitRule')}" method="post" accept-charset="UTF-8">
                <input type="hidden" name="redir" value="{concat($aclEditorURL,$currentEditor)}" />
                <hr />
                <table id="rule_table">
                
                <tr id="delAllBox">
                <td id="openAll">
                <div class="button"
                onMouseover="initOpenAll(this,'{i18n:translate('acl-editor.button.expandAll')}','{i18n:translate('acl-editor.button.collapsAll')}')">
                <xsl:value-of select="i18n:translate('acl-editor.button.expandAll')" />
                </div>
                <div class="button"
                onclick="deleteAllFromDB('{concat($dataRequest, '&amp;action=delAllRules')}','{i18n:translate('acl-editor.msg.delAllRules')}')">
                <xsl:value-of select="i18n:translate('acl-editor.button.delAllRules')" />
                </div>
                </td>
                <td class="checkBox">
                <xsl:value-of select="i18n:translate('acl-editor.label.markAll')" />
                :
                <input type="checkbox" id="delAll" />
                </td>
                </tr>
                <xsl:for-each select="mcr_access_rule">
                
                <table style="width:100%; border:solid 1px black;">
                <tr name="rule_line" id="{concat('Rule$',RID)}">
                <td id="left">
                Button zum aufklappen 
                <div class="visButton" name="visButton" onclick="changeVisibility('{concat('RuleField$',RID)}',this)">+</div>
                </td>
                <td>
                <xsl:value-of select="i18n:translate('acl-editor.label.ruleID')" />
                :
                </td>
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
                <div class="delInfo">
                <xsl:value-of select="i18n:translate('acl-editor.msg.delInfo')" />
                </div>
                </xsl:when>
                <xsl:when test="inUse = 'false'">
                <input type="checkbox" name="delete_rule" value="{RID}" />
                </xsl:when>
                </xsl:choose>
                </td>
                </tr>
                <tr>
                <td id="left"></td>
                <td>
                <xsl:value-of select="i18n:translate('acl-editor.label.description')" />
                :
                </td>
                <td id="Description">
                <input size="80px" name="{concat('RuleDesc$',RID)}" value="{DESCRIPTION}" onkeypress="setChanged(event)" />
                </td>
                </tr>
                <tr class="ruleField" id="{concat('RuleField$',RID)}">
                <td id="left"></td>
                <td>
                <xsl:value-of select="i18n:translate('acl-editor.label.rule')" />
                :
                </td>
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
            -->
        </div>
    </xsl:template>

</xsl:stylesheet>
