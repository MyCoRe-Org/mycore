<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:java="http://xml.apache.org/xalan/java" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">
    <xsl:param name="toc.pageSize" select="20" />
    <xsl:param name="toc.pos" select="1" />

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

    <xsl:variable name="permEditorURL"
        select="concat($WebApplicationBaseURL,'servlets/MCRACLEditorServlet_v2',$HttpSession,'?mode=getACLEditor&amp;editor=permEditor')" />
    <xsl:variable name="permEditorURL_setFilter"
        select="concat($WebApplicationBaseURL,'servlets/MCRACLEditorServlet_v2',$HttpSession,'?mode=dataRequest&amp;action=setFilter&amp;ObjIdFilter=',//objid,'&amp;AcPoolFilter=',//acpool)" />


    <xsl:variable name="aclPermEditorCSS" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/CSS/aclPermEditor.css')" />
    <xsl:variable name="aclPermEditorJS" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/JS/aclPermEditor.js')" />
    <xsl:variable name="aclClickButtonsJS" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/JS/aclClickButtons.js')" />

    <xsl:variable name="labelObjID" select="concat(i18n:translate('acl-editor.label.objID'),':')" />
    <xsl:variable name="labelPermission" select="concat(i18n:translate('acl-editor.label.permission'),':')" />
    <xsl:variable name="labelRule" select="concat(i18n:translate('acl-editor.label.rule'),':')" />

    <xsl:template match="/mcr_access_set">
        <xsl:variable name="ruleItems" select="document(concat($dataRequest, '&amp;action=getRuleAsItems'))" />


        <div id="aclPermEditor" onMouseover="aclPermEditorSetup()">
            <script type="text/javascript" src="{$aclPermEditorJS}" language="JavaScript"></script>
            <script type="text/javascript" src="{$aclClickButtonsJS}" language="JavaScript"></script>
            <link rel="stylesheet" type="text/css" href="{$aclPermEditorCSS}"></link>
            <xsl:choose>
                <xsl:when test="(@emb = 'true') and (@cmd = $add)">
                    <!-- New Mapping -->
                    <xsl:call-template name="createNewMapping">
                        <xsl:with-param name="objId" select="mcr_access_filter/objid" />
                        <xsl:with-param name="acPool" select="mcr_access_filter/acpool" />
                        <xsl:with-param name="ruleItems" select="$ruleItems" />
                        <xsl:with-param name="hide" select="'true'" />
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="not(@emb = 'true')">
                    <!-- New Mapping -->
                    <xsl:call-template name="createNewMapping">
                        <xsl:with-param name="ruleItems" select="$ruleItems" />
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>

            <xsl:choose>
                <xsl:when test="not(@cmd = $add) and not(@cmd = $delete)">
                    <!-- Mapping Table -->
                    <xsl:variable name="labelScra" select="i18n:translate('acl-editor.label.scra')" />

                    <div id="aclEditPermBox">
                        <xsl:if test="not(@emb = 'true')">
                            <div class="aclPermEditorLabel">
                                <xsl:value-of select="$labelScra" />
                            </div>

                            <div class="permFilterBox">
                                <xsl:apply-templates select="mcr_access_filter" />
                            </div>

                            <div class="permTocNaviResultsBox">
                                <xsl:call-template name="mcr_access.printTOCNavi">
                                    <xsl:with-param name="childrenXML" select="." />
                                </xsl:call-template>
                            </div>
                        </xsl:if>

                        <div class="aclPermTableBox">
                            <xsl:if test="not(@emb = 'true')">
                            <div class="menu">
                                <table>
                                    <tr>
                                        <td>
                                            <xsl:variable name="delURL">
                                                <xsl:value-of
                                                    select="concat($dataRequest, '&amp;action=delAllPerms&amp;objid=',//objid,'&amp;acpool=',//acpool)" />
                                            </xsl:variable>
                                            <div class="clickButtonOut" id="delAllAclPerms" cmd="{$delURL}"
                                                msg="{i18n:translate('acl-editor.msg.delAllPerms')}">
                                                <xsl:value-of select="i18n:translate('acl-editor.button.delAllPerms')" />
                                            </div>
                                        </td>
                                        <td class="space"></td>
                                        <td>
                                            <div class="checkBoxRow">
                                                <table>
                                                    <tr>
                                                        <td id="checkBoxRowLabel">
                                                            <xsl:value-of select="concat(i18n:translate('acl-editor.label.markAll'),':')" />
                                                        </td>
                                                        <td>
                                                            <input class="checkBox" type="checkbox" id="delAllCheckBox"
                                                                labelChecked="{concat(i18n:translate('acl-editor.label.delAllChecked'),':')}" />
                                                        </td>
                                                    </tr>
                                                </table>
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                            </xsl:if>

                            <form id="aclEditPermBoxForm" name="MappingTableForm" xmlns:encoder="xalan://java.net.URLEncoder"
                                xmlns:xalan="http://xml.apache.org/xalan" action="{concat($dataRequest, '&amp;action=submitPerm', $redirectURL)}" method="post"
                                accept-charset="UTF-8">
                                <input type="hidden" name="redir" value="{$aclEditorURL}" />

                                <!-- handle tabs reqs -->
                                <xsl:variable name="toc.pos.verif">
                                    <xsl:choose>
                                        <xsl:when test="$toc.pageSize>count(./mcr_access)">
                                            <xsl:value-of select="1" />
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="$toc.pos" />
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:variable>
                                <xsl:choose>
                                    <xsl:when test="count(./mcr_access)>0">
                                        <xsl:for-each select="mcr_access[(position()>=$toc.pos.verif) and ($toc.pos.verif+$toc.pageSize>position())]">
                                            <xsl:variable name="aclPermTableID" select="concat(OBJID,'$',ACPOOL)" />
                                            <table id="{$aclPermTableID}" class="aclPermTable">
                                                <xsl:choose>
                                                    <xsl:when test="not(../@cmd = $edit)">
                                                        <tr>
                                                            <td class="label">
                                                                <xsl:value-of select="$labelObjID" />
                                                            </td>
                                                            <td id="OBJID" class="value">
                                                                <div>
                                                                    <xsl:value-of select="OBJID" />
                                                                </div>
                                                            </td>
                                                            <td class="delCheckBoxRow">
                                                                <table>
                                                                    <tr>
                                                                        <td>
                                                                            <xsl:value-of select="concat(i18n:translate('acl-editor.label.delete'),':')" />
                                                                        </td>
                                                                        <td>
                                                                            <input id="{concat('checkBox$',$aclPermTableID)}" class="checkBox" type="checkbox"
                                                                                value="{concat(OBJID,'$',ACPOOL)}" />
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                            </td>
                                                        </tr>
                                                        <tr>
                                                            <td class="label">
                                                                <xsl:value-of select="$labelPermission" />
                                                            </td>
                                                            <td class="value" id="ACPOOL">
                                                                <xsl:value-of select="ACPOOL" />
                                                            </td>
                                                            <td></td>
                                                        </tr>
                                                        <tr>
                                                            <td class="label">
                                                                <xsl:value-of select="$labelRule" />
                                                            </td>
                                                            <td class="ruleSelctBox">
                                                                <!--
                                                                    <div id="{concat('select$',$aclPermTableID)}">
                                                                    <xsl:value-of select="RID"></xsl:value-of>
                                                                    </div>
                                                                -->
                                                                <xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
                                                                    <xsl:with-param name="rid" select="RID" />
                                                                    <xsl:with-param name="selectId" select="concat('select$',$aclPermTableID)" />
                                                                    <xsl:with-param name="name" select="$aclPermTableID" />
                                                                </xsl:apply-templates>

                                                            </td>
                                                            <td></td>
                                                        </tr>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <tr>
                                                            <td>
                                                                <xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
                                                                    <xsl:with-param name="rid" select="RID" />
                                                                    <xsl:with-param name="selectId" select="concat('select$',$aclPermTableID)" />
                                                                    <xsl:with-param name="name" select="$aclPermTableID" />
                                                                </xsl:apply-templates>
                                                            </td>
                                                        </tr>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </table>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <table class="aclPermTable">
                                            <tr>
                                                <td>
                                                    <table>
                                                        <tr>
                                                            <td>
                                                                <xsl:value-of select="i18n:translate('acl-editor.msg.noFilterResults')" />
                                                            </td>
                                                            <td></td>

                                                        </tr>
                                                        <tr>
                                                            <td>
                                                                <xsl:value-of select="$labelObjID" />
                                                            </td>
                                                            <td>
                                                                <xsl:value-of select="$labelPermission" />
                                                            </td>
                                                        </tr>
                                                        <tr>
                                                            <td>
                                                                <xsl:value-of select="mcr_access_filter/objid" />
                                                            </td>
                                                            <td>
                                                                <xsl:value-of select="mcr_access_filter/acpool" />
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <table class="saveButtonBar">
                                    <tr>
                                        <td>
                                            <input class="button" type="submit" value="{i18n:translate('acl-editor.button.saveChg')}" />
                                        </td>
                                    </tr>
                                    <xsl:if test="@cmd = $add">
                                        <tr>
                                            <td>

                                                <input class="button" type="button" value="{i18n:translate('acl-editor.button.cancel')}"
                                                    onclick="history.back()" />

                                            </td>
                                        </tr>
                                    </xsl:if>
                                </table>

                            </form>
                        </div>
                    </div>
                </xsl:when>
                <xsl:when test="@cmd = $delete">
                    <form name="MappingTableForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                        action="{concat($dataRequest, '&amp;action=submitPerm', $redirectURL)}" method="post" accept-charset="UTF-8">
                        <xsl:for-each select="mcr_access">
                            Mapping fuer "
                            <xsl:value-of select="OBJID" />
                            " mit der Regel "
                            <xsl:value-of select="ACPOOL" />
                            " wirklich loeschen?
                            <br />
                            <input type="hidden" name="{concat('deleted$',OBJID,'$',ACPOOL)}" value="{concat('deleted$',OBJID,'$',ACPOOL)}" />
                            <input type="submit" value="Loeschen" />
                            <input type="button" value="Nicht loeschen" onclick="history.back()" />
                        </xsl:for-each>
                    </form>
                </xsl:when>
            </xsl:choose>
        </div>
    </xsl:template>

    <!-- Template for input field -->
    <xsl:template name="inputField">
        <xsl:param name="name" />
        <xsl:param name="value" />
        <xsl:param name="editable" />

        <xsl:choose>
            <xsl:when test="boolean($editable)">
                <input name="{$name}" value="{$value}" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ========================================================================================================================= -->

    <!-- Template for creating new access mapping -->
    <xsl:template name="createNewMapping">
        <xsl:param name="ruleItems" />
        <xsl:param name="objId" />
        <xsl:param name="acPool" />
        <xsl:param name="rid" />
        <xsl:param name="hide" />

        <xsl:variable name="display">
            <xsl:choose>
                <xsl:when test="$hide = 'true'">
                    <xsl:value-of select="'display: none;'" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="'display: block;'" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="labelAddRuleAssignment" select="i18n:translate('acl-editor.label.addRuleAss')" />

        <div id="aclCreateNewPermBox">
            <xsl:if test="not(@emb = 'true')">
                <div class="aclPermEditorLabel">
                    <xsl:value-of select="$labelAddRuleAssignment" />
                </div>
            </xsl:if>

            <form id="createNewPermForm" name="NewPermForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                action="{concat($dataRequest, '&amp;action=createNewPerm', $redirectURL)}" method="post" accept-charset="UTF-8">
                <input type="hidden" name="redir" value="{$aclEditorURL}" />

                <table class="aclCreateNewPermTable">
                    <xsl:choose>
                        <xsl:when test="not(@cmd = $add)">
                            <tr>
                                <td class="label">
                                    <xsl:value-of select="$labelObjID" />
                                </td>
                                <td>
                                    <input id="newPermObjId" class="input" name="newPermOBJID" value="{$objId}" />
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    <xsl:value-of select="$labelPermission" />
                                </td>
                                <td>
                                    <input id="newPermAcpool" class="input" name="newPermACPOOL" value="{$acPool}" />
                                </td>
                            </tr>
                        </xsl:when>
                        <xsl:otherwise>
                            <input id="newPermObjId" type="hidden" name="newPermOBJID" value="{$objId}" />
                            <input id="newPermAcpool" type="hidden" name="newPermACPOOL" value="{$acPool}" />
                        </xsl:otherwise>
                    </xsl:choose>
                    <tr>
                        <td class="label">
                            <xsl:value-of select="$labelRule" />
                        </td>
                        <td>
                            <xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
                                <xsl:with-param name="rid" select="$rid" />
                                <xsl:with-param name="selectId" select="'createNewPermFormSelBox'" />
                                <xsl:with-param name="name" select="'newPermRID'" />
                            </xsl:apply-templates>
                        </td>
                    </tr>
                    <tr>
                        <td></td>
                        <td colspan="2">
                            <input id="newPermSubmitButton" class="button" type="button" value="{i18n:translate('acl-editor.button.create')}"
                                msgSelBox="{i18n:translate('acl-editor.msg.noRuleSelected')}" msgObjId="{i18n:translate('acl-editor.msg.emptyObjId')}"
                                msgAcPool="{i18n:translate('acl-editor.msg.emptyAcPool')}" />
                        </td>
                    </tr>


                </table>
            </form>
        </div>
    </xsl:template>

    <!-- ========================================================================================================================= -->

    <!-- Template for filter -->
    <xsl:template match="mcr_access_filter">
        <xsl:variable name="labelFilterList" select="concat(i18n:translate('acl-editor.label.filterList'),':')" />
        <xsl:variable name="labelInsertID" select="concat(i18n:translate('acl-editor.label.insertID'),':')" />
        <xsl:variable name="labelInsertPerm" select="concat(i18n:translate('acl-editor.label.insertPerm'),':')" />
        <xsl:variable name="filterRedirURL"
            select="concat($dataRequest, '&amp;XSL.toc.pos.SESSION=1&amp;action=setFilter&amp;ObjIdFilter=',objid,'&amp;AcPoolFilter=',acpool,$redirectURL)" />
        <xsl:variable name="filterURL" select="concat($dataRequest, '&amp;XSL.toc.pos.SESSION=1&amp;action=setFilter',$redirectURL)" />

        <form name="AclFilterForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan" action="{$filterURL}" method="post"
            accept-charset="UTF-8">
            <input type="hidden" name="redir" value="{$filterURL}" />
            <table>
                <tr>
                    <td class="label" colspan="5">
                        <xsl:value-of select="$labelFilterList" />
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td>
                        <xsl:value-of select="$labelInsertID" />
                    </td>
                    <td>
                        <xsl:value-of select="$labelInsertPerm" />
                    </td>
                    <td></td>
                    <td></td>
                </tr>
                <tr>
                    <td>

                    </td>
                    <td>
                        <input class="input" name="ObjIdFilter" value="{objid}" />
                    </td>
                    <td>
                        <input class="input" name="AcPoolFilter" value="{acpool}" />
                    </td>
                    <td>
                        <input class="button" type="submit" value="{i18n:translate('acl-editor.button.filter')}" />
                    </td>
                    <td>
                        <input class="button"
                            onClick="self.location.href='{concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=dataRequest&amp;action=deleteFilter')}'"
                            value="{i18n:translate('acl-editor.button.clearFilter')}" type="button" />
                    </td>

                </tr>
            </table>
        </form>
    </xsl:template>

    <!-- Template for drop down box of Rid's -->
    <!--<xsl:template match="items">
        <div id="aclRuleSelBox" class="hidden">
        <xsl:for-each select="item">
        <div label="{@label}" value="{@value}" />
        </xsl:for-each>
        </div>
        
        </xsl:template>
    -->
    <xsl:template match="items">
        <xsl:param name="rid" />
        <xsl:param name="selectId" />
        <xsl:param name="selectInputId" />
        <xsl:param name="name" />


        <select id="{$selectId}" class="input" size="1" name="{$name}">
            <option value="'bitte waehlen'">
                <xsl:value-of select="i18n:translate('acl-editor.label.choose')" />
            </option>
            <xsl:for-each select="item">
                <option value="{@value}">
                    <xsl:if test="@value=$rid">
                        <xsl:attribute name="selected">selected<xsl:value-of select="i18n:translate('acl-editor.label.selected')" />
        </xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="@label" />

                </option>
            </xsl:for-each>
        </select>

    </xsl:template>

    <!-- =============================================================================================================================== -->

    <xsl:template name="mcr_access.printTOCNavi">
        <xsl:param name="location" />
        <xsl:param name="childrenXML" />

        <xsl:variable name="pred">
            <xsl:value-of select="number($toc.pos)-(number($toc.pageSize)+1)" />
        </xsl:variable>
        <xsl:variable name="succ">
            <xsl:value-of select="number($toc.pos)+number($toc.pageSize)+1" />
        </xsl:variable>
        <xsl:variable name="numChildren">
            <xsl:value-of select="count(xalan:nodeset($childrenXML)/mcr_access)" />
        </xsl:variable>

        <xsl:variable name="labelGor" select="concat(i18n:translate('acl-editor.label.gor'),':')" />
        <xsl:variable name="labelResults" select="concat(i18n:translate('acl-editor.label.results'),':')" />
        <xsl:variable name="labelResultsPerPage" select="concat(i18n:translate('acl-editor.label.resultsPerPage'),':')" />

        <form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan" action="{$permEditorURL}" method="post"
            accept-charset="UTF-8">
            <input type="hidden" name="redir" value="{$permEditorURL}" />

            <table class="permTocNaviResultsTable">
                <tr>
                    <td class="label">
                        <xsl:value-of select="$labelGor" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <table>
                            <tr>
                                <td>
                                    <xsl:value-of select="$labelResults" />
                                </td>

                                <td class="numChildren">
                                    <!-- &#32; is space  -->
                                    <xsl:value-of select="concat($numChildren,',&#32;')" />
                                </td>

                                <td>
                                    <xsl:value-of select="$labelResultsPerPage" />
                                </td>

                                <td>
                                    <input class="input" name="XSL.toc.pageSize.SESSION" value="{$toc.pageSize}" size="3" />
                                </td>

                                <td>
                                    <xsl:call-template name="mcr_access.printTOCNavi.chooseHitPage">
                                        <xsl:with-param name="children" select="$childrenXML" />
                                    </xsl:call-template>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </form>
    </xsl:template>

    <!-- =============================================================================================================================== -->

    <!-- ===================================================================================================== -->

    <xsl:template name="mcr_access.printTOCNavi.chooseHitPage">
        <xsl:param name="children" />

        <xsl:variable name="numberOfChildren">
            <xsl:value-of select="count(xalan:nodeset($children)/mcr_access)" />
        </xsl:variable>
        <xsl:variable name="numberOfHitPages">
            <xsl:value-of select="ceiling(number($numberOfChildren) div number($toc.pageSize))" />
        </xsl:variable>
        <xsl:if test="number($numberOfChildren)>number($toc.pageSize)">
            <xsl:value-of select="concat(',&#32;', i18n:translate('acl-editor.label.resultPages'))" />
            :
            <xsl:for-each select="xalan:nodeset($children)/mcr_access[number($numberOfHitPages)>=position()]">
                <xsl:variable name="jumpToPos">
                    <xsl:value-of select="(position()*number($toc.pageSize))-number($toc.pageSize)" />
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="number($jumpToPos)+1=number($toc.pos)">
                        <xsl:value-of select="concat(' [',position(),'] ')" />
                    </xsl:when>
                    <xsl:otherwise>
                        <a href="{concat($permEditorURL_setFilter,'&amp;XSL.toc.pos.SESSION=',$jumpToPos+1)}">
                            <xsl:value-of select="concat(' ',position(),' ')" />
                        </a>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:if>

    </xsl:template>

    <!-- ===================================================================================================== -->

</xsl:stylesheet>
