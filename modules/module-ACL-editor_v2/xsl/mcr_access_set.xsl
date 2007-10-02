<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!--  MyCoRe - Module-Broadcasting 					-->
<!--  												-->
<!-- Module-Broadcasting 1.0, 04-2007  				-->
<!-- +++++++++++++++++++++++++++++++++++++			-->
<!--  												-->
<!-- Andreas Trappe 	- idea, concept, dev.		-->
<!--												-->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
    <xsl:include href="MyCoReLayout.xsl" />

    <xsl:variable name="PageTitle" select="'Module-ACL Editor'" />

    <xsl:template match="/mcr_access_set">
        <xsl:variable name="ruleItems" select="document(concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=getRuleAsItems'))" />
        <div id="ACL-Editor">
            <script type="text/javascript" src="{concat($WebApplicationBaseURL,'modules/module-ACL-editor_v2/web/JS/aclEditor.js')}" language="JavaScript"></script>

            <div id="createNewPerm">
                <img id="createNewPermImg" onclick="changeVisibility($('createNewPermForm'))" alt="Logo"
                    src="http://141.35.23.203:8291/templates/master/template_mycoresample-1/IMAGES/logo.gif" />
                neue Regel
                <form id="createNewPermForm" style="visibility: hidden;" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                    action="{concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=dataRequest&amp;action=createNewPerm')}" method="post"
                    accept-charset="UTF-8">
                    <table>
                        <tr>
                            <td>
                                <input name="newPermACPOOL" value="" />
                            </td>
                            <td>
                                <input name="newPermOBJID" value="" />
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

            <xsl:apply-templates select="mcr_access_filter" />

            <form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
                action="{concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=dataRequest&amp;action=submit')}" method="post" accept-charset="UTF-8">
                <table>
                    <xsl:for-each select="mcr_access">

                        <tr>
                            <td>
                                <input name="ACL-{concat(node(), 'ACPOOL')}" value="{ACPOOL}" />
                            </td>
                            <td>
                                <input name="{concat(node(), 'OBJID')}" value="{OBJID}" />
                            </td>
                            <td>
                                <!--                            <input name="{concat(node(), 'RID')}" value="{RID}" />-->
                                <xsl:apply-templates select="xalan:nodeset($ruleItems)/items">
                                    <xsl:with-param name="rid" select="RID" />
                                    <xsl:with-param name="name" select="concat(ACPOOL,OBJID)" />
                                </xsl:apply-templates>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </form>
        </div>
    </xsl:template>

    <xsl:template match="mcr_access_filter">
        <form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan"
            action="{concat($ServletsBaseURL,'MCRACLEditorServlet_v2?mode=dataRequest&amp;action=setFilter')}" method="post" accept-charset="UTF-8">
            <table>
                <tr>
                    <td>
                        <input name="AcPoolFilter" value="{acpool}" />
                    </td>
                    <td>
                        <input name="ObjIdFilter" value="{objid}" />
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

    <xsl:template match="items">
        <xsl:param name="rid" />
        <xsl:param name="name" />

        <select size="1" name="{$name}">
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