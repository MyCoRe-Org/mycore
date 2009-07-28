<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.5 $ $Date: 2009/03/27 14:23:11 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">
  
  <!-- ======== Parameter from MyCoRe LayoutServlet ======== -->
  <!--
  <xsl:param name="WebApplicationBaseURL"     />
  <xsl:param name="ServletsBaseURL"           />
  <xsl:param name="DefaultLang"               />
  <xsl:param name="CurrentLang"               />
  <xsl:param name="MCRSessionID"              />
  -->
  
  <!-- ======== handles workflow ======== -->
  
  <xsl:template match="workflow">
    <xsl.copy select="."/>
    <xsl:variable name="url">
      <xsl:choose>
        <xsl:when test="@base">
          <xsl:value-of
            select="concat($ServletsBaseURL,'MCRListWorkflowServlet',$JSessionID,'?XSL.Style=xml&amp;base=',@base,'&amp;step=',@step)"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of
            select="concat($ServletsBaseURL,'MCRListWorkflowServlet',$JSessionID,'?XSL.Style=xml&amp;type=',@type,'&amp;step=',@step)"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:apply-templates select="document($url)/mcr_workflow"/>
  </xsl:template>
  
  <xsl:template match="/mcr_workflow">
    <xsl:variable name="type">
      <xsl:value-of select="@type"/>
    </xsl:variable>
    <xsl:variable name="step">
      <xsl:value-of select="@step"/>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="item">
        <table style="margin: 3% 0px 3% 2%;" width="90%" cellpadding="0" cellspacing="0">
          <xsl:for-each select="item">
            <xsl:variable name="obj_id">
              <xsl:value-of select="@ID"/>
            </xsl:variable>
            <xsl:variable name="re_id">
              <xsl:value-of select="../@ID"/>
            </xsl:variable>
            <xsl:variable name="obj_deletewf">
              <xsl:value-of select="@deletewf"/>
            </xsl:variable>
            <xsl:variable name="obj_writedb">
              <xsl:value-of select="@writedb"/>
            </xsl:variable>
            <xsl:variable name="obj_priv">
              <xsl:copy-of select="concat('modify-',substring-before(substring-after($obj_id,'_'),'_'))"/>
            </xsl:variable>
            <tr class="result">
              <td>
                <table width="100%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td align="left" class="header" style="font-weight:bolder;">
                      <xsl:value-of select="label"/>
                    </td>
                    <td width="20" class="header"/>
                    <td align="right" class="header" style="text-align:right;">
                      <xsl:value-of select="$obj_id"/>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr class="result">
              <td class="desc" valign="top">
                <table width="100%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td class="metavalue" align="left" valign="top">
                      <xsl:for-each select="data">
                        <xsl:if test="position() != 1">;</xsl:if>
                        <xsl:value-of select="."/>
                      </xsl:for-each>
                    </td>
                    <td width="10"/>
                    <xsl:if test="$type = 'document'">
                      <td width="30" valign="top" align="center">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=author&amp;todo=wnewder">
                          <img src="{$WebApplicationBaseURL}images/workflow_deradd.gif"
                            title="{i18n:translate('component.swf.derivate.addDerivate')}" border="0"/>
                        </a>
                      </td>
                      <td width="10"/>
                    </xsl:if>
                    <td width="30" valign="top" align="center">
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditobj">
                        <img src="{$WebApplicationBaseURL}images/workflow_objedit.gif"
                          title="{i18n:translate('component.swf.object.editObject')}" border="0"/>
                      </a>
                    </td>
                    <td width="10"/>
                    <td width="30" valign="top" align="center">
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcopyobj">
                        <img src="{$WebApplicationBaseURL}images/workflow_objcopy.gif"
                          title="{i18n:translate('component.swf.object.copyObject')}" border="0"/>
                      </a>
                    </td>
                    <td width="10"/>
                    <td width="30" valign="top" align="center">
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditacl">
                        <img src="{$WebApplicationBaseURL}images/workflow_acledit.gif"
                          title="{i18n:translate('component.swf.object.editACL')}" border="0"/>
                      </a>
                    </td>
                    <td width="10"/>
                    <td width="30" valign="top" align="center">
                      <xsl:if test="$obj_writedb = 'true'">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcommit">
                          <img src="{$WebApplicationBaseURL}images/workflow_objcommit.gif"
                            title="{i18n:translate('component.swf.object.commitObject')}" border="0"/>
                        </a>
                      </xsl:if>
                    </td>
                    <td width="10"/>
                    <td width="30" valign="top" align="center">
                      <xsl:if test="$obj_deletewf = 'true'">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wdelobj">
                          <img src="{$WebApplicationBaseURL}images/workflow_objdelete.gif"
                            title="{i18n:translate('component.swf.object.delObject')}" border="0"/>
                        </a>
                      </xsl:if>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <xsl:for-each select="derivate">
              <tr>
                <td class="metavalue" align="left" valign="top">
                  <table width="100%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td valign="top" align="left">
                        <xsl:choose>
                          <xsl:when test="@title">
                            <xsl:value-of select="@title"/>&#160;
                          </xsl:when>
                          <xsl:otherwise>
                            <xsl:value-of select="@label"/>&#160;
                          </xsl:otherwise>
                        </xsl:choose>
                      </td>
                      <td width="10"/>
                      <td valign="top" width="30">
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;re_mcrid={$re_id}&amp;step=editor&amp;todo=waddfile">
                          <img src="{$WebApplicationBaseURL}images/workflow_deradd.gif"
                            title="{i18n:translate('component.swf.derivate.addFile')}" border="0"/>
                        </a>
                      </td>
                      <td width="10"/>
                      <td valign="top" width="30">
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;re_mcrid={$re_id}&amp;step=editor&amp;todo=weditder">
                          <img src="{$WebApplicationBaseURL}images/workflow_deredit.gif"
                            title="{i18n:translate('component.swf.derivate.editDerivate')}" border="0"/>
                        </a>
                      </td>
                      <td width="10"/>
                      <td valign="top" width="30">
                        <xsl:if test="$obj_deletewf = 'true'">
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;re_mcrid={$re_id}&amp;step=editor&amp;todo=wdelder">
                            <img src="{$WebApplicationBaseURL}images/workflow_derdelete.gif"
                              title="{i18n:translate('component.swf.derivate.delDerivate')}" border="0"/>
                          </a>
                        </xsl:if>
                      </td>
                      <td width="10"/>
                      <td valign="top" width="30"/>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td class="metavalue" align="left" valign="top">
                  <table width="100%" cellpadding="0" cellspacing="0">
                    <xsl:choose>
                      <xsl:when test="file">
                        <xsl:for-each select="file">
                          <tr>
                            <td valign="top">
                              <xsl:choose>
                                <xsl:when test="@main = 'true'">
                                  <img src="{$WebApplicationBaseURL}images/button_green.gif"/>
                                </xsl:when>
                                <xsl:otherwise>
                                  <xsl:variable name="extparm">
                                    <xsl:value-of select="concat('####main####',.)"/>
                                  </xsl:variable>
                                  <a
                                    href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;re_mcrid={$re_id}&amp;step=editor&amp;todo=wsetfile&amp;extparm={$extparm}">
                                    <img src="{$WebApplicationBaseURL}images/button_light.gif"
                                      title="{i18n:translate('component.swf.derivate.setFile')}" border="0"/>
                                  </a>
                                </xsl:otherwise>
                              </xsl:choose>
                            </td>
                            <td valign="top">
                              <xsl:choose>
                                <xsl:when test="true()">
                                  <xsl:variable name="fileurl"
                                    select="concat($WebApplicationBaseURL,'servlets/MCRFileViewWorkflowServlet/',text(),$JSessionID,'?type=',$type)"/>
                                  <a class="linkButton">
                                    <xsl:attribute name="href">
                                      <xsl:value-of select="$fileurl"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="target">_blank</xsl:attribute>
                                    <xsl:value-of select="."/>
                                  </a> [
                                  <xsl:value-of select="@size"/>] </xsl:when>
                                <xsl:otherwise>
                                  <xsl:value-of select="."/> [
                                  <xsl:value-of select="@size"/>] </xsl:otherwise>
                              </xsl:choose>
                            </td>
                            <td valign="top">
                              <xsl:if test="count(../file) != 1">
                                <xsl:variable name="extparm">
                                  <xsl:value-of
                                    select="concat('####nrall####',count(../file),'####nrthe####',position(),'####filename####',.)"/>
                                </xsl:variable>
                                <a
                                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;re_mcrid={$re_id}&amp;step=editor&amp;todo=wdelfile&amp;extparm={$extparm}">
                                  <img src="{$WebApplicationBaseURL}images/button_delete.gif"
                                    title="{i18n:translate('component.swf.derivate.delFile')}" border="0"/>
                                </a>
                              </xsl:if>
                            </td>
                          </tr>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:otherwise>
                        <tr>
                          <td valign="top">
                            <xsl:value-of select="i18n:translate('component.swf.empty.derivate')"/>
                          </td>
                        </tr>
                      </xsl:otherwise>
                    </xsl:choose>
                  </table>
                </td>
              </tr>
            </xsl:for-each>
            <tr colspan="3">
              <td>&#160;
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <center>
          <span class="desc">
            <xsl:value-of select="i18n:translate('component.swf.empty.workflow')"/>
          </span>
        </center>
        <p/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>