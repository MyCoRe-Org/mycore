<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:java="http://xml.apache.org/xalan/java" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">
    <xsl:param name="toc.pageSize" select="20" />
    <xsl:param name="toc.pos" select="1" />
    <xsl:variable name="permEditorURL" select="concat($WebApplicationBaseURL,'servlets/MCRACLEditorServlet_v2?mode=getACLEditor&amp;editor=permEditor')" />
    <xsl:variable name="permEditorURL_setFilter"
        select="concat($WebApplicationBaseURL,'servlets/MCRACLEditorServlet_v2?mode=dataRequest&amp;action=setFilter&amp;ObjIdFilter=',//objid,'&amp;AcPoolFilter=',//acpool)" />


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

    <xsl:template match="/mcr_access_set">
        <xsl:variable name="ruleItems" select="document(concat($dataRequest, '&amp;action=getRuleAsItems'))" />


        <div id="ACL-Perm-Editor" onMouseover="initPermEditor()">
            <!-- ACL-Perm-Editor will be included into ACL Editor so link for JavaScript and CSS will be defined in mcr_acl_edior.xsl -->
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
                    <!-- Filter 
                        <xsl:apply-templates select="mcr_access_filter" />-->
                </xsl:when>
            </xsl:choose>

            <xsl:choose>
                <xsl:when test="not(@cmd = $add) and not(@cmd = $delete)">
                    <!-- Mapping Table -->
                    <form name="MappingTableForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                        action="{concat($dataRequest, '&amp;action=submitPerm', $redirectURL)}" method="post" accept-charset="UTF-8">
                        <input type="hidden" name="redir" value="{$aclEditorURL}" />
                        
                        <br />
                        <br />
                        <table id="mapping_table" style="border:solid 1px;">
                            <xsl:if test="not(@emb = 'true')">
                                <tr>
                                    <th align="left">
                                        <b>System contained rule assignments:</b>
                                        <br />
                                        <br />
                                        <br />
                                    </th>
                                </tr>
                                <!-- tabs -->
                                <tr>
                                    <td>
                                        <xsl:call-template name="mcr_access.printTOCNavi">
                                            <xsl:with-param name="childrenXML" select="." />
                                        </xsl:call-template>

                                    </td>
                                </tr>
                                <!-- Filter -->

                                <tr>
                                    <td>
                                        <xsl:apply-templates select="mcr_access_filter" />
                                        <br />
                                    </td>
                                </tr>
                            </xsl:if>

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
                            <!-- create rows -->
                            <xsl:for-each select="mcr_access[(position()>=$toc.pos.verif) and ($toc.pos.verif+$toc.pageSize>position())]">
                                <tr id="mapping_line">
                                    <xsl:choose>
                                        <xsl:when test="not(../@cmd = $edit)">
                                            <td>
                                                <table
                                                    style="width:100%;border-left:solid 20px;border-left-color:gray;border-top:solid 1px;border-left-color:gray;">
                                                    <tr>
                                                        <td>ID:</td>
                                                        <td id="OBJID">
                                                            <b>
                                                                <xsl:value-of select="OBJID" />
                                                            </b>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td>Permission:</td>
                                                        <td id="ACPOOL">
                                                            <b>
                                                                <xsl:value-of select="ACPOOL" />
                                                            </b>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td>Rule:</td>
                                                        <td>
                                                            <b>
                                                                <xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
                                                                    <xsl:with-param name="rid" select="RID" />
                                                                    <xsl:with-param name="name" select="concat(OBJID,'$',ACPOOL)" />
                                                                </xsl:apply-templates>
                                                            </b>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td>Delete:</td>
                                                        <td id="delete">
                                                            <b>
                                                                <input type="checkbox" name="delete_mapping" value="{concat(OBJID,'$',ACPOOL)}" />
                                                            </b>
                                                        </td>
                                                    </tr>
                                                </table>
                                                <br />
                                            </td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td>
                                                <xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
                                                    <xsl:with-param name="rid" select="RID" />
                                                    <xsl:with-param name="name" select="concat(OBJID,'$',ACPOOL)" />
                                                </xsl:apply-templates>
                                            </td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                            <tr>
                                <td>
                                    <input type="submit" value="Speichern" />
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <xsl:if test="@cmd = $add">
                                        <input type="button" value="Abbrechen" onclick="history.back()" />
                                    </xsl:if>
                                </td>
                            </tr>

                        </table>
                    </form>
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

        <div id="createNewPerm">
            <br />
            <form id="createNewPermForm" name="NewPermForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                action="{concat($dataRequest, '&amp;action=createNewPerm', $redirectURL)}" method="post" accept-charset="UTF-8">
                <input type="hidden" name="redir" value="{$aclEditorURL}" />
                <table style="border:solid 1px;">
                    <xsl:if test="not(@emb = 'true')">
                        <tr>
                            <th align="left">
                                <b>Add new rule assignment:</b>
                                <br />
                                <br />
                                <br />
                            </th>
                        </tr>
                    </xsl:if>
                    <xsl:choose>
                        <xsl:when test="not(@cmd = $add)">
                            <tr>
                                <td>ID:</td>
                                <td>
                                    <input name="newPermOBJID" value="{$objId}" size="60" />
                                </td>
                            </tr>
                            <tr>
                                <td>Permission:</td>
                                <td>
                                    <input name="newPermACPOOL" value="{$acPool}" size="60" />
                                </td>
                            </tr>
                        </xsl:when>
                        <xsl:otherwise>
                            <input type="hidden" name="newPermOBJID" value="{$objId}" />
                            <input type="hidden" name="newPermACPOOL" value="{$acPool}" />
                        </xsl:otherwise>
                    </xsl:choose>
                    <tr>
                        <td>Rule:</td>
                        <td>
                            <xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
                                <xsl:with-param name="rid" select="$rid" />
                                <xsl:with-param name="name" select="'newPermRID'" />
                            </xsl:apply-templates>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <input type="submit" value="Anlegen" />
                        </td>
                    </tr>


                </table>
            </form>
        </div>
    </xsl:template>

    <!-- ========================================================================================================================= -->

    <!-- Template for filter -->
    <xsl:template match="mcr_access_filter">
        <form name="AclFilterForm" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
            action="{concat($dataRequest, '&amp;XSL.toc.pos.SESSION=1&amp;action=setFilter', $redirectURL)}" method="post" accept-charset="UTF-8">
            <table>
                <tr>
                    <td colspan="5">
                        <b>Filter list:</b>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td>Insert ID:</td>
                    <td colspan="3">Insert permission:</td>
                </tr>
                <tr>
                    <td>

                    </td>
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
        <form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan" action="{$permEditorURL}" method="post"
            accept-charset="UTF-8">
            <table>
                <tr>
                    <td>
                        <b>Gruppierung der Ergebnisse:</b>
                    </td>
                </tr>
                <tr>
                    <td>
                        <xsl:value-of select="'Ergebnisse: '" />
                        <b>
                            <xsl:value-of select="$numChildren" />
                        </b>

                        <xsl:value-of select="', Ergebnisse/Seite: '" />

                        <input name="XSL.toc.pageSize.SESSION" value="{$toc.pageSize}" size="3" />

                        <xsl:call-template name="mcr_access.printTOCNavi.chooseHitPage">
                            <xsl:with-param name="children" select="$childrenXML" />
                        </xsl:call-template>
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
            <xsl:value-of select="', Ergebnisseiten: '" />
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
