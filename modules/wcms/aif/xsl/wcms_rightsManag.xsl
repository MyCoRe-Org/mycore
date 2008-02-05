<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
    xmlns:layoutUtils="xalan:///org.mycore.frontend.MCRLayoutUtilities" xmlns:wcmsUtils="xalan:///org.mycore.frontend.wcms.MCRWCMSUtilities">
    <xsl:include href="start-acl-editor.xsl" />

    <xsl:variable name="perm">
        <xsl:call-template name="wcms.rightsManagement.getPermission" />
    </xsl:variable>
    <xsl:variable name="filteredUser">
        <xsl:call-template name="wcms.rightsManagement.getfilteredUser" />
    </xsl:variable>

    <!--  ============================================================================================ -->
    <xsl:template match="cms/rightsManagement">
        <xsl:call-template name="wcms.rightsManagement.head" />
        <br />
        <xsl:call-template name="wcms.rightsManagement.userList" />
        <br />
        <div id="statistic-width">
            <div id="statistic">
                <div class="inhalt">
                    <table>
                        <th>
                            <xsl:value-of select="i18n:translate('wcms.rightsManag.webpage')" />
                        </th>
                        <th>
                            <xsl:value-of select="i18n:translate('wcms.rightsManag.acl')" />
                        </th>
                        <th>
                            <xsl:value-of select="i18n:translate('wcms.rightsManag.edit')" />
                        </th>
                        <!-- generate table -->
                        <xsl:choose>
                            <!-- filter is set, get filtered navi.xml -->
                            <xsl:when test="$perm=$write and $filteredUser!=''">
                                <xsl:variable name="filteredNavi" select="wcmsUtils:getWritableNavi($filteredUser)" />
                                <!-- all valid entries -->
                                <xsl:for-each select="$filteredNavi//node()[@href]">
                                    <xsl:call-template name="wcms.rightsManagement.row" />
                                </xsl:for-each>
                            </xsl:when>
                            <!-- filter NOT set, user normal navi.xml -->
                            <xsl:otherwise>
                                <!-- root -->
                                <xsl:for-each select="$loaded_navigation_xml">
                                    <xsl:call-template name="wcms.rightsManagement.row" />
                                </xsl:for-each>
                                <!-- all sub entries -->
                                <xsl:for-each select="$loaded_navigation_xml//node()[@href]">
                                    <xsl:call-template name="wcms.rightsManagement.row" />
                                </xsl:for-each>
                            </xsl:otherwise>
                        </xsl:choose>
                    </table>
                </div>
            </div>
        </div>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.userList">
        <table>
            <tr>
                <td>
                    <xsl:call-template name="wcms.rightsManagement.userList.admins" />
                </td>
                <xsl:if test="$perm=$write">
                    <td>
                        <xsl:call-template name="wcms.rightsManagement.userList.complete" />
                    </td>
                </xsl:if>
            </tr>
        </table>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.userList.admins">
        <xsl:choose>
            <xsl:when test="$perm=$write">
                <xsl:value-of select="i18n:translate('wcms.rightsManag.write.cb.descr')" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="i18n:translate('wcms.rightsManag.read.cb.descr')" />
            </xsl:otherwise>
        </xsl:choose>
        <br />
        <select size="1">
            <xsl:for-each select="./users[@filter='administrators']/user">
                <option>
                    <xsl:value-of select="concat(user.contact/contact.firstname/text(),user.contact/contact.lastname/text(),' (',@ID,')')" />
                </option>
            </xsl:for-each>
        </select>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.userList.complete">
        <xsl:variable name="filteredUser" select="@filteredUser" />
        <xsl:choose>
            <xsl:when test="$perm=$write">
                <xsl:value-of select="i18n:translate('wcms.rightsManag.write.filter.descr')" />
            </xsl:when>
        </xsl:choose>
        <br />
        <form action="{$ServletsBaseURL}MCRWCMSAdminServlet{$JSessionID}" id="userFilter">
            <input type="hidden" name="action" value="{$manage-wcms}" />
            <select size="1" name="filter" onChange="document.getElementById('userFilter').submit()">
                <xsl:choose>
                    <xsl:when test="not(@filteredUser)">
                        <option value="#$#$#" selected="selected">
                            <xsl:value-of select="i18n:translate('wcms.rightsManag.write.filter.noRestr')" />
                        </option>
                    </xsl:when>
                    <xsl:otherwise>
                        <option value="#$#$#">
                            <xsl:value-of select="i18n:translate('wcms.rightsManag.write.filter.noRestr')" />
                        </option>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:for-each select="./mycoreuser/user">
                    <xsl:choose>
                        <xsl:when test="$filteredUser=@ID">
                            <option value="{@ID}" selected="selected">
                                <xsl:value-of select="concat(user.contact/contact.firstname/text(),user.contact/contact.lastname/text(),' (',@ID,')')" />
                            </option>
                        </xsl:when>
                        <xsl:otherwise>
                            <option value="{@ID}">
                                <xsl:value-of select="concat(user.contact/contact.firstname/text(),user.contact/contact.lastname/text(),' (',@ID,')')" />
                            </option>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </select>
        </form>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.head">
        <xsl:call-template name="menuleiste">
            <xsl:with-param name="menupunkt" select="i18n:translate(concat('wcms.rightsManag_',$perm))" />
        </xsl:call-template>
        <xsl:call-template name="zeigeSeitenname">
            <xsl:with-param name="seitenname" select="i18n:translate(concat('wcms.rightsManag_',$perm,'.descr'))" />
        </xsl:call-template>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.getInsertion">
        <xsl:copy-of select="' '" />
        <xsl:for-each select="ancestor::node()">
            <xsl:if test="position()>1">
                <xsl:copy-of select="'------'" />
            </xsl:if>
        </xsl:for-each>
        <xsl:copy-of select="' '" />
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.row">
        <xsl:variable name="hasRule" select="layoutUtils:hasRule($perm,@href)" />
        <tr>
            <!-- label -->
            <td>
                <xsl:call-template name="wcms.rightsManagement.getInsertion" />
                <xsl:choose>
                    <xsl:when test="$hasRule='true'">
                        <span style="color:#FF6060;">
                            <xsl:call-template name="wcms.rightsManagement.label" />
                        </span>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="wcms.rightsManagement.label" />
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- rule -->
            <td>
                <xsl:choose>
                    <xsl:when test="$hasRule='true'">
                        <xsl:variable name="ruleID" select="layoutUtils:getRuleID($perm,@href)" />
                        <xsl:variable name="ruleDes" select="layoutUtils:getRuleDescr($perm,@href)" />
                        <xsl:value-of select="concat($ruleDes,' (',$ruleID,')')" />
                    </xsl:when>
                </xsl:choose>
            </td>
            <!-- editor buttons -->
            <td>
                <xsl:choose>
                    <xsl:when test="$hasRule='true'">
                        <xsl:variable name="aclEditorAddress_edit">
                            <xsl:call-template name="aclEditor.embMapping.getAddress">
                                <xsl:with-param name="objId" select="layoutUtils:getWebpageACLID(@href)" />
                                <xsl:with-param name="permission" select="$perm" />
                                <xsl:with-param name="action" select="'edit'" />
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:variable name="aclEditorAddress_delete">
                            <xsl:call-template name="aclEditor.embMapping.getAddress">
                                <xsl:with-param name="objId" select="layoutUtils:getWebpageACLID(@href)" />
                                <xsl:with-param name="permission" select="$perm" />
                                <xsl:with-param name="action" select="'delete'" />
                            </xsl:call-template>
                        </xsl:variable>
                        <a href="{$aclEditorAddress_edit}">
                            <img width="18" height="13" src="{concat($WebApplicationBaseURL,'/modules/wcms/aif/web/images/editRule.gif')}"
                                title="{i18n:translate('wcms.rightsManag.acl.edit')}" alt="{i18n:translate('wcms.rightsManag.acl.edit')}" />
                        </a>
                        ,
                        <a href="{$aclEditorAddress_delete}">
                            <img width="18" height="13" src="{concat($WebApplicationBaseURL,'/modules/wcms/aif/web/images/deleteRule.gif')}"
                                title="{i18n:translate('wcms.rightsManag.acl.delete')}" alt="{i18n:translate('wcms.rightsManag.acl.delete')}" />
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:variable name="aclEditorAddress_add">
                            <xsl:call-template name="aclEditor.embMapping.getAddress">
                                <xsl:with-param name="objId" select="layoutUtils:getWebpageACLID(@href)" />
                                <xsl:with-param name="permission" select="$perm" />
                                <xsl:with-param name="action" select="'add'" />
                            </xsl:call-template>
                        </xsl:variable>
                        <a href="{$aclEditorAddress_add}">
                            <img width="18" height="13" src="{concat($WebApplicationBaseURL,'/modules/wcms/aif/web/images/addRule.gif')}"
                                title="{i18n:translate('wcms.rightsManag.acl.add')}" alt="{i18n:translate('wcms.rightsManag.acl.add')}" />
                        </a>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
        </tr>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.label">
        <xsl:choose>
            <xsl:when test="./label">
                <xsl:value-of select="./label[lang($CurrentLang)]/text()" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat(' ',i18n:translate('wcms.rightsManag.root'))" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.getPermission">
        <xsl:choose>
            <xsl:when test="/cms/rightsManagement[@mode=$manage-read]">
                <xsl:value-of select="$read" />
            </xsl:when>
            <xsl:when test="/cms/rightsManagement[@mode=$manage-wcms]">
                <xsl:value-of select="$write" />
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    <!--  ============================================================================================ -->
    <xsl:template name="wcms.rightsManagement.getfilteredUser">
        <xsl:choose>
            <xsl:when test="/cms/rightsManagement[@filteredUser]">
                <xsl:value-of select="/cms/rightsManagement/@filteredUser" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="''" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--  ============================================================================================ -->


</xsl:stylesheet>












