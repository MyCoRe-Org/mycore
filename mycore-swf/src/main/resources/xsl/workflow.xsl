<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:encoder="xalan://java.net.URLEncoder">
  
  <!-- ======== Parameter from MyCoRe LayoutServlet ======== -->
  <!--
  <xsl:param name="WebApplicationBaseURL"     />
  <xsl:param name="ServletsBaseURL"           />
  <xsl:param name="DefaultLang"               />
  <xsl:param name="CurrentLang"               />
  <xsl:param name="direction"                 />
  <xsl:param name="MCRSessionID"              />
  -->
  
  <xsl:template match="workflow">
    <xsl:variable name="url">
      <xsl:choose>
        <xsl:when test="@base">
          <xsl:value-of
            select="concat($ServletsBaseURL,'MCRListWorkflowServlet',$JSessionID,'?XSL.Style=xml&amp;base=',@base,'&amp;step=',@step,'&amp;with_derivate=',@with_derivate)"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of
            select="concat($ServletsBaseURL,'MCRListWorkflowServlet',$JSessionID,'?XSL.Style=xml&amp;type=',@type,'&amp;step=',@step,'&amp;with_derivate=',@with_derivate)"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:apply-templates select="document($url)/mcr_workflow"/>
  </xsl:template>
  
  <xsl:template match="/mcr_workflow">
    <xsl:variable name="base">
      <xsl:value-of select="@base"/>
    </xsl:variable>
    <xsl:variable name="type">
      <xsl:value-of select="@type"/>
    </xsl:variable>
    <xsl:variable name="step">
      <xsl:value-of select="@step"/>
    </xsl:variable>
    <xsl:variable name="with_derivate">
      <xsl:choose>
        <xsl:when test="@with_derivate">
          <xsl:value-of select="@with_derivate"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="false"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <div id="swf_workflow_common_list">
      <xsl:choose>
        <xsl:when test="$direction = 'rtl'">
          <!-- start rtl languages -->
          <xsl:choose>
            <xsl:when test="item">
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
                <div class="item">
                  <table class="item_table" cellspacing="0" >
                    <tr>
                      <td class="item_col item_col1_rtl"/>
                      <td class="item_col item_col2_rtl"/>
                      <td class="item_col item_col3_rtl"/>
                      <td class="item_col item_col4_rtl"/>
                    </tr>
                    <tr class="headline_rtl">
                      <td class="headline_label_rtl" colspan="4">
                        <xsl:value-of select="label"/>
                      </td>
                    </tr>
                    <tr class="headline_rtl">
                      <td class="headline_id_rtl" colspan="4">
                        <xsl:value-of select="$obj_id"/>
                      </td>
                    </tr>
                    <tr class="dataline_rtl">
                      <td class="dataline_icons_rtl" colspan="2">
                        <xsl:if test="$with_derivate = 'true'">
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=author&amp;todo=wnewder">
                            <div  class="icon_rtl icon_workflow_deradd_rtl" title="{i18n:translate('component.swf.derivate.addDerivate')}"/>
                          </a>
                        </xsl:if>
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditobj">
                            <div  class="icon_rtl icon_workflow_objedit" title="{i18n:translate('component.swf.object.editObject')}"/>
                        </a>
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcopyobj">
                            <div  class="icon_rtl icon_workflow_objcopy" title="{i18n:translate('component.swf.object.copyObject')}"/>
                        </a>
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditacl">
                            <div  class="icon_rtl icon_workflow_acledit" title="{i18n:translate('component.swf.object.editACL')}"/>
                        </a>
                        <xsl:if test="$obj_writedb = 'true'">
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcommit">
                            <div  class="icon_rtl icon_workflow_objcommit" title="{i18n:translate('component.swf.object.commitObject')}"/>
                          </a>
                        </xsl:if>
                        <xsl:if test="$obj_deletewf = 'true'">
                          <xsl:variable name="delpath"
                            select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelobj')"/>
                          <xsl:variable name="delscript" select="concat('doDelObj',$obj_id,'()')"/>
                          <script type="text/javascript"> function
                            <xsl:value-of select="$delscript"/> { strInput =
                            confirm('<xsl:value-of select="i18n:translate('component.swf.workflow.deleteMsg')"/>');
                            if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } </script>
                          <a href="javascript:{$delscript}">
                            <div  class="icon_rtl icon_workflow_objdelete" title="{i18n:translate('component.swf.object.delObject')}"/>
                          </a>
                        </xsl:if>
                      </td>
                      <td class="dataline_label_rtl" colspan="2">
                        <xsl:for-each select="data">
                          <xsl:if test="position() != 1">
                            <br/>
                          </xsl:if>
                          <xsl:value-of select="."/>
                        </xsl:for-each>
                      </td>
                    </tr>
                    <xsl:for-each select="derivate">
                      <tr class="derivateline_rtl">
                        <td class="derivateline_icons_rtl" colspan="2">
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=waddfile">
                            <div  class="icon_rtl icon_workflow_deradd_rtl" title="{i18n:translate('component.swf.derivate.addFile')}"/>
                          </a>
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=weditder">
                            <div  class="icon_rtl icon_workflow_deredit" title="{i18n:translate('component.swf.derivate.editDerivate')}"/>
                          </a>
                          <xsl:if test="$obj_deletewf = 'true'">
                            <xsl:variable name="delpath"
                              select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelder')"/>
                            <xsl:variable name="delscript" select="concat('doDelDer',@ID,'()')"/>
                            <script type="text/javascript"> function
                              <xsl:value-of select="$delscript"/> { strInput =
                              confirm('<xsl:value-of select="i18n:translate('component.swf.workflow.deleteMsg')"/>');
                              if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } </script>
                            <a href="javascript:{$delscript}">
                              <div class="icon_rtl icon_workflow_derdelete" title="{i18n:translate('component.swf.derivate.delDerivate')}"/>
                            </a>
                          </xsl:if>
                        </td>
                        <td class="derivateline_label_rtl" colspan="2">
                          <xsl:choose>
                            <xsl:when test="@title">
                              <xsl:value-of select="@title"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="@label"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        </td>
                      </tr>
                      <xsl:choose>
                        <xsl:when test="file">
                          <xsl:for-each select="file">
                            <tr class="derivatefiles_rtl">
                              <td class="derivatefiles_delete_rtl">
                                <xsl:choose>
                                  <xsl:when test="count(../file) != 1">
                                    <xsl:variable name="extparm">
                                      <xsl:value-of
                                        select="encoder:encode( string(concat('####nrall####',count(../file),'####nrthe####',position(),'####filename####',.)))"/>
                                    </xsl:variable>
                                    <xsl:variable name="delpath"
                                      select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',../@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelfile&amp;extparm=',$extparm)"/>
                                    <xsl:variable name="delscript"
                                      select="concat('doDelDer',../@ID,'_',position(),'()')"/>
                                    <script language="javascript"> function
                                      <xsl:value-of select="$delscript"/> { strInput =
                                      confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>');
                                      if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } </script>
                                    <a href="javascript:{$delscript}">
                                      <img class="button" src="{$WebApplicationBaseURL}images/button_delete.gif"
                                        title="{i18n:translate('component.swf.derivate.delFile')}"/>
                                    </a>
                                  </xsl:when>
                                  <xsl:otherwise>
                                    <img class="button" style="width:16px"
                                      src="{$WebApplicationBaseURL}images/nav-empty.gif"/>
                                  </xsl:otherwise>
                                </xsl:choose>
                              </td>
                              <td class="derivatefiles_filename_rtl" colspan="2">
                                <xsl:variable name="fileurl"
                                  select="concat($WebApplicationBaseURL,'servlets/MCRFileViewWorkflowServlet/',text(),$JSessionID,'?type=',$type,'&amp;base=',$base)"/>
                                <xsl:text>[</xsl:text>
                                <xsl:value-of select="@size"/>
                                <xsl:text>]&#160;</xsl:text>
                                <a class="linkButton">
                                  <xsl:attribute name="href">
                                    <xsl:value-of select="$fileurl"/>
                                  </xsl:attribute>
                                  <xsl:attribute name="target">_blank</xsl:attribute>
                                  <xsl:value-of select="."/>
                                </a>
                              </td>
                              <td class="derivatefiles_active_rtl">
                                <xsl:choose>
                                  <xsl:when test="@main = 'true'">
                                    <img class="button" src="{$WebApplicationBaseURL}images/button_green_rtl.gif"/>
                                  </xsl:when>
                                  <xsl:otherwise>
                                    <xsl:variable name="extparm">
                                      <xsl:value-of select="encoder:encode( string(concat('####main####',.)))"/>
                                    </xsl:variable>
                                    <a
                                      href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={../@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=wsetfile&amp;extparm={$extparm}">
                                      <img class="button" src="{$WebApplicationBaseURL}images/button_light_rtl.gif"
                                        title="{i18n:translate('component.swf.derivate.setFile')}"/>
                                    </a>
                                  </xsl:otherwise>
                                </xsl:choose>
                              </td>
                            </tr>
                          </xsl:for-each>
                        </xsl:when>
                        <xsl:otherwise>
                          <div class="derivatefiles_rtl">
                            <xsl:value-of select="i18n:translate('component.swf.empty.derivate')"/>
                          </div>
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:for-each>
                  </table>
                </div>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <div class="text_rtl">
                <xsl:value-of select="i18n:translate('component.swf.empty.workflow')"/>
              </div>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <!-- stop rtl languages -->
        <!-- start ltr languages -->
        <xsl:otherwise>
          <xsl:choose>
            <xsl:when test="item">
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
                <div class="item">
                  <table class="item_table" cellspacing="0" >
                    <tr>
                      <td class="item_col item_col1_ltr"/>
                      <td class="item_col item_col2_ltr"/>
                      <td class="item_col item_col3_ltr"/>
                      <td class="item_col item_col4_ltr"/>
                    </tr>
                    <tr class="headline_ltr">
                      <td class="headline_label_ltr" colspan="4">
                        <xsl:value-of select="label"/>
                      </td>
                    </tr>
                    <tr class="headline_ltr">
                      <td class="headline_id_ltr" colspan="4">
                        <xsl:value-of select="$obj_id"/>
                      </td>
                    </tr>
                    <tr class="dataline_ltr">
                      <td class="dataline_label_ltr" colspan="2">
                        <xsl:for-each select="data">
                          <xsl:if test="position() != 1">
                            <br/>
                          </xsl:if>
                          <xsl:value-of select="."/>
                        </xsl:for-each>
                      </td>
                      <td class="dataline_icons_ltr" colspan="2">
                        <xsl:if test="$with_derivate = 'true'">
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=author&amp;todo=wnewder">
                            <div  class="icon_ltr icon_workflow_deradd_ltr" title="{i18n:translate('component.swf.derivate.addDerivate')}"/>
                          </a>
                        </xsl:if>
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditobj">
                            <div  class="icon_ltr icon_workflow_objedit" title="{i18n:translate('component.swf.object.editObject')}"/>
                        </a>
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcopyobj">
                            <div  class="icon_ltr icon_workflow_objcopy" title="{i18n:translate('component.swf.object.copyObject')}"/>
                        </a>
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditacl">
                            <div  class="icon_ltr icon_workflow_acledit" title="{i18n:translate('component.swf.object.editACL')}"/>
                        </a>
                        <xsl:if test="$obj_writedb = 'true'">
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcommit">
                            <div  class="icon_ltr icon_workflow_objcommit" title="{i18n:translate('component.swf.object.commitObject')}"/>
                          </a>
                        </xsl:if>
                        <xsl:if test="$obj_deletewf = 'true'">
                          <xsl:variable name="delpath"
                            select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelobj')"/>
                          <xsl:variable name="delscript" select="concat('doDelObj',$obj_id,'()')"/>
                          <script type="text/javascript"> function
                            <xsl:value-of select="$delscript"/> { strInput =
                            confirm('<xsl:value-of select="i18n:translate('component.swf.workflow.deleteMsg')"/>');
                            if(strInput==true) window.location=
                            '<xsl:value-of select="$delpath"/>' } </script>
                          <a href="javascript:{$delscript}">
                            <div  class="icon_ltr icon_workflow_objdelete" title="{i18n:translate('component.swf.object.delObject')}"/>
                          </a>
                        </xsl:if>
                      </td>
                    </tr>
                    <xsl:for-each select="derivate">
                      <tr class="derivateline_ltr">
                        <td class="derivateline_label_ltr" colspan="2">
                          <xsl:choose>
                            <xsl:when test="@title">
                              <xsl:value-of select="@title"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="@label"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        </td>
                        <td class="derivateline_icons_ltr" colspan="2">
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=waddfile">
                            <div  class="icon_ltr icon_workflow_deradd_ltr" title="{i18n:translate('component.swf.derivate.addFile')}"/>
                          </a>
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=weditder">
                            <div  class="icon_ltr icon_workflow_deredit" title="{i18n:translate('component.swf.derivate.editDerivate')}"/>
                          </a>
                          <xsl:if test="$obj_deletewf = 'true'">
                            <xsl:variable name="delpath"
                              select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelder')"/>
                            <xsl:variable name="delscript" select="concat('doDelDer',@ID,'()')"/>
                            <script language="javascript"> function
                              <xsl:value-of select="$delscript"/> { strInput = 
                              confirm('<xsl:value-of select="i18n:translate('component.swf.workflow.deleteMsg')"/>');
                              if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } </script>
                            <a href="javascript:{$delscript}">
                              <div class="icon_ltr icon_workflow_derdelete" title="{i18n:translate('component.swf.derivate.delDerivate')}"/>
                            </a>
                          </xsl:if>
                        </td>
                      </tr>
                      <xsl:choose>
                        <xsl:when test="file">
                          <xsl:for-each select="file">
                            <tr class="derivatefiles_ltr">
                              <td class="derivatefiles_active_ltr">
                                <xsl:choose>
                                  <xsl:when test="@main = 'true'">
                                    <img class="button" src="{$WebApplicationBaseURL}images/button_green_ltr.gif"/>
                                  </xsl:when>
                                  <xsl:otherwise>
                                    <xsl:variable name="extparm">
                                      <xsl:value-of select="encoder:encode( string(concat('####main####',.)))"/>
                                    </xsl:variable>
                                    <a
                                      href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={../@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=wsetfile&amp;extparm={$extparm}">
                                      <img class="button" src="{$WebApplicationBaseURL}images/button_light_ltr.gif"
                                        title="{i18n:translate('component.swf.derivate.setFile')}"/>
                                    </a>
                                  </xsl:otherwise>
                                </xsl:choose>
                              </td>
                              <td class="derivatefiles_filename_ltr" colspan="2">
                                <xsl:variable name="fileurl"
                                  select="concat($WebApplicationBaseURL,'servlets/MCRFileViewWorkflowServlet/',text(),$JSessionID,'?type=',$type,'&amp;base=',$base)"/>
                                <a class="linkButton">
                                  <xsl:attribute name="href">
                                    <xsl:value-of select="$fileurl"/>
                                  </xsl:attribute>
                                  <xsl:attribute name="target">_blank</xsl:attribute>
                                  <xsl:value-of select="."/>
                                </a> &#160;[
                                <xsl:value-of select="@size"/>] </td>
                              <td class="derivatefiles_delete_ltr">
                                <xsl:choose>
                                  <xsl:when test="count(../file) != 1">
                                    <xsl:variable name="extparm">
                                      <xsl:value-of
                                        select="encoder:encode( string(concat('####nrall####',count(../file),'####nrthe####',position(),'####filename####',.)))"/>
                                    </xsl:variable>
                                    <xsl:variable name="delpath"
                                      select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',../@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelfile&amp;extparm=',$extparm)"/>
                                    <xsl:variable name="delscript"
                                      select="concat('doDelDer',../@ID,'_',position(),'()')"/>
                                    <script language="javascript"> function
                                      <xsl:value-of select="$delscript"/> { strInput = 
                                      confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>');
                                      if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } </script>
                                    <a href="javascript:{$delscript}">
                                      <img class="button" src="{$WebApplicationBaseURL}images/button_delete.gif"
                                        title="{i18n:translate('component.swf.derivate.delFile')}"/>
                                    </a>
                                  </xsl:when>
                                  <xsl:otherwise>
                                    <img class="button" style="width:16px"
                                      src="{$WebApplicationBaseURL}images/nav-empty.gif"/>
                                  </xsl:otherwise>
                                </xsl:choose>
                              </td>
                            </tr>
                          </xsl:for-each>
                        </xsl:when>
                        <xsl:otherwise>
                          <div class="derivatefiles_ltr">
                            <xsl:value-of select="i18n:translate('component.swf.empty.derivate')"/>
                          </div>
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:for-each>
                  </table>
                </div>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <div class="text_ltr">
                <xsl:value-of select="i18n:translate('component.swf.empty.workflow')"/>
              </div>
            </xsl:otherwise>
          </xsl:choose>
          <!-- stop rtl languages -->
        </xsl:otherwise>
        <!-- stop rtl languages -->
      </xsl:choose>
    </div>
    
  </xsl:template>
  
</xsl:stylesheet>