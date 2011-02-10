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
    
    <xsl:choose>
      <xsl:when test="$direction = 'rtl'">
        <!-- start rtl languages -->
        <div id="swf_workflow_common_list">
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
                <div class="item_const_rtl item_var">
                  <div class="headline_const_rtl headline_var">
                    <div class="headline_id_const_rtl headline_id_var">
                      <xsl:value-of select="$obj_id"/>
                    </div>
                    <div class="headline_label_const_rtl headline_label_var">
                      <xsl:value-of select="label"/>
                    </div>
                  </div>
                  <div class="dataline_const_rtl dataline_var">
                    <div class="dataline_icons_const_rtl dataline_icons_var">
                      <xsl:if test="$with_derivate = 'true'">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=author&amp;todo=wnewder">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_deradd_rtl.gif"
                            title="{i18n:translate('component.swf.derivate.addDerivate')}"/>
                        </a>
                      </xsl:if>
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditobj">
                        <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('component.swf.object.editObject')}"/>
                      </a>
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcopyobj">
                        <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objcopy.gif" title="{i18n:translate('component.swf.object.copyObject')}"/>
                      </a>
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditacl">
                        <img class="icon" src="{$WebApplicationBaseURL}images/workflow_acledit.gif" title="{i18n:translate('component.swf.object.editACL')}"/>
                      </a>
                      <xsl:if test="$obj_writedb = 'true'">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcommit">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objcommit.gif"
                            title="{i18n:translate('component.swf.object.commitObject')}"/>
                        </a>
                      </xsl:if>
                      <xsl:if test="$obj_deletewf = 'true'">
                        <xsl:variable name="delpath"
                          select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelobj')"/>
                        <xsl:variable name="delscript" select="concat('doDelObj',$obj_id,'()')"/>
                        <script language="javascript"> 
                          function <xsl:value-of select="$delscript"/> { 
                            strInput = confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>'); 
                            if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } 
                        </script>
                        <a href="javascript:{$delscript}">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objdelete.gif"
                            title="{i18n:translate('component.swf.object.delObject')}"/>
                        </a>
                      </xsl:if>
                    </div>
                    <div class="dataline_label_const_rtl dataline_label_var">
                      <xsl:for-each select="data">
                        <xsl:if test="position() != 1">
                          <br/>
                        </xsl:if>
                        <xsl:value-of select="."/>
                      </xsl:for-each>
                    </div>
                  </div>
                  <xsl:for-each select="derivate">
                    <div class="derivateline_const_rtl derivateline_var">
                      <div class="derivateline_icons_const_rtl derivateline_icon_var">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=waddfile">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{i18n:translate('component.swf.derivate.addFile')}"/>
                        </a>
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=weditder">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_deredit.gif"
                            title="{i18n:translate('component.swf.derivate.editDerivate')}"/>
                        </a>
                        <xsl:if test="$obj_deletewf = 'true'">
                          <xsl:variable name="delpath"
                            select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelder')"/>
                          <xsl:variable name="delscript" select="concat('doDelDer',@ID,'()')"/>
                          <script language="javascript"> 
                            function <xsl:value-of select="$delscript"/> { 
                              strInput = confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>'); 
                              if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } 
                          </script>
                          <a href="javascript:{$delscript}">
                            <img class="icon" src="{$WebApplicationBaseURL}images/workflow_derdelete.gif"
                              title="{i18n:translate('component.swf.derivate.delDerivate')}"/>
                          </a>
                        </xsl:if>
                      </div>
                      <div class="derivateline_label_const_rtl derivateline_label_var">
                        <xsl:choose>
                          <xsl:when test="@title">
                            <xsl:value-of select="@title"/>
                          </xsl:when>
                          <xsl:otherwise>
                            <xsl:value-of select="@label"/>
                          </xsl:otherwise>
                        </xsl:choose>
                      </div>
                    </div>
                    <xsl:choose>
                      <xsl:when test="file">
                        <xsl:for-each select="file">
                          <div class="derivatefiles_const_rtl derivatefiles_var">
                            <div class="derivatefiles_delete_const_rtl derivatefiles_delete_var">
                              <xsl:if test="count(../file) != 1">
                                <xsl:variable name="extparm">
                                  <xsl:value-of
                                    select="encoder:encode( string(concat('####nrall####',count(../file),'####nrthe####',position(),'####filename####',.)))"/>
                                </xsl:variable>
                                <xsl:variable name="delpath"
                                  select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',../@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelfile&amp;extparm=',$extparm)"/>
                                <xsl:variable name="delscript" select="concat('doDelDer',../@ID,'_',position(),'()')"/>
                                <script language="javascript"> 
                                  function <xsl:value-of select="$delscript"/> { 
                                    strInput = confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>'); 
                                    if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } 
                                </script>
                                <a href="javascript:{$delscript}">
                                  <img class="button" src="{$WebApplicationBaseURL}images/button_delete.gif"
                                    title="{i18n:translate('component.swf.derivate.delFile')}"/>
                                </a>
                              </xsl:if>
                            </div>
                            <div class="derivatefiles_filename_const_rtl derivatefiles_filename_var">
                              <xsl:variable name="fileurl"
                                select="concat($WebApplicationBaseURL,'servlets/MCRFileViewWorkflowServlet/',text(),$JSessionID,'?type=',$type,'&amp;base=',$base)"/>
                              [<xsl:value-of select="@size"/>]&#160;
                              <a class="linkButton">
                                <xsl:attribute name="href">
                                  <xsl:value-of select="$fileurl"/>
                                </xsl:attribute>
                                <xsl:attribute name="target">_blank</xsl:attribute>
                                <xsl:value-of select="."/>
                              </a> 
                            </div>
                            <div class="derivatefiles_active_const_rtl derivatefiles_active_var">
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
                            </div>
                          </div>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:otherwise>
                        <div class="derivatefiles_const_rtl derivatefiles_var">
                          <xsl:value-of select="i18n:translate('component.swf.empty.derivate')"/>
                        </div>
                      </xsl:otherwise>
                    </xsl:choose>
                  </xsl:for-each>
                </div>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <div class="text_const_rtl text_var">
                <xsl:value-of select="i18n:translate('component.swf.empty.workflow')"/>
              </div>
            </xsl:otherwise>
          </xsl:choose>
        </div>
        <!-- stop rtl languages -->
      </xsl:when>
      <!-- start ltr languages -->
      <xsl:otherwise>
        <div id="swf_workflow_common_list">
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
                <div class="item_const_ltr item_var">
                  <div class="headline_const_ltr headline_var">
                    <div class="headline_label_const_ltr headline_label_var">
                      <xsl:value-of select="label"/>
                    </div>
                    <div class="headline_id_const_ltr headline_id_var">
                      <xsl:value-of select="$obj_id"/>
                    </div>
                  </div>
                  <div class="dataline_const_ltr dataline_var">
                    <div class="dataline_label_const_ltr dataline_label_var">
                      <xsl:for-each select="data">
                        <xsl:if test="position() != 1">
                          <br/>
                        </xsl:if>
                        <xsl:value-of select="."/>
                      </xsl:for-each>
                    </div>
                    <div class="dataline_icons_const_ltr dataline_icons_var">
                      <xsl:if test="$with_derivate = 'true'">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=author&amp;todo=wnewder">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_deradd_ltr.gif"
                            title="{i18n:translate('component.swf.derivate.addDerivate')}"/>
                        </a>
                      </xsl:if>
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditobj">
                        <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('component.swf.object.editObject')}"/>
                      </a>
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcopyobj">
                        <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objcopy.gif" title="{i18n:translate('component.swf.object.copyObject')}"/>
                      </a>
                      <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=weditacl">
                        <img class="icon" src="{$WebApplicationBaseURL}images/workflow_acledit.gif" title="{i18n:translate('component.swf.object.editACL')}"/>
                      </a>
                      <xsl:if test="$obj_writedb = 'true'">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={$obj_id}&amp;step=editor&amp;todo=wcommit">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objcommit.gif"
                            title="{i18n:translate('component.swf.object.commitObject')}"/>
                        </a>
                      </xsl:if>
                      <xsl:if test="$obj_deletewf = 'true'">
                        <xsl:variable name="delpath"
                          select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelobj')"/>
                        <xsl:variable name="delscript" select="concat('doDelObj',$obj_id,'()')"/>
                        <script language="javascript"> 
                          function <xsl:value-of select="$delscript"/> { 
                            strInput = confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>'); 
                            if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } 
                        </script>
                        <a href="javascript:{$delscript}">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_objdelete.gif"
                            title="{i18n:translate('component.swf.object.delObject')}"/>
                        </a>
                      </xsl:if>
                    </div>
                  </div>
                  <xsl:for-each select="derivate">
                    <div class="derivateline_const_ltr derivateline_var">
                      <div class="derivateline_label_const_ltr derivateline_label_var">
                        <xsl:choose>
                          <xsl:when test="@title">
                            <xsl:value-of select="@title"/>
                          </xsl:when>
                          <xsl:otherwise>
                            <xsl:value-of select="@label"/>
                          </xsl:otherwise>
                        </xsl:choose>
                      </div>
                      <div class="derivateline_icons_const_ltr derivateline_icon_var">
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=waddfile">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{i18n:translate('component.swf.derivate.addFile')}"/>
                        </a>
                        <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?se_mcrid={@ID}&amp;re_mcrid={$obj_id}&amp;step=editor&amp;todo=weditder">
                          <img class="icon" src="{$WebApplicationBaseURL}images/workflow_deredit.gif"
                            title="{i18n:translate('component.swf.derivate.editDerivate')}"/>
                        </a>
                        <xsl:if test="$obj_deletewf = 'true'">
                          <xsl:variable name="delpath"
                            select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelder')"/>
                          <xsl:variable name="delscript" select="concat('doDelDer',@ID,'()')"/>
                          <script language="javascript"> 
                            function <xsl:value-of select="$delscript"/> { 
                              strInput = confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>'); 
                              if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } 
                          </script>
                          <a href="javascript:{$delscript}">
                            <img class="icon" src="{$WebApplicationBaseURL}images/workflow_derdelete.gif"
                              title="{i18n:translate('component.swf.derivate.delDerivate')}"/>
                          </a>
                        </xsl:if>
                      </div>
                    </div>
                    <xsl:choose>
                      <xsl:when test="file">
                        <xsl:for-each select="file">
                          <div class="derivatefiles_const_ltr derivatefiles_var">
                            <div class="derivatefiles_active_const_ltr derivatefiles_active_var">
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
                            </div>
                            <div class="derivatefiles_filename_const_ltr derivatefiles_filename_var">
                              <xsl:variable name="fileurl"
                                select="concat($WebApplicationBaseURL,'servlets/MCRFileViewWorkflowServlet/',text(),$JSessionID,'?type=',$type,'&amp;base=',$base)"/>
                              <a class="linkButton">
                                <xsl:attribute name="href">
                                  <xsl:value-of select="$fileurl"/>
                                </xsl:attribute>
                                <xsl:attribute name="target">_blank</xsl:attribute>
                                <xsl:value-of select="."/>
                              </a> &#160;[
                              <xsl:value-of select="@size"/>] </div>
                            <div class="derivatefiles_delete_const_ltr derivatefiles_delete_var">
                              <xsl:choose>
                                <xsl:when test="count(../file) != 1">
                                  <xsl:variable name="extparm">
                                    <xsl:value-of
                                      select="encoder:encode( string(concat('####nrall####',count(../file),'####nrthe####',position(),'####filename####',.)))"/>
                                  </xsl:variable>
                                  <xsl:variable name="delpath"
                                    select="concat($ServletsBaseURL,'MCRStartEditorServlet',$HttpSession,'?se_mcrid=',../@ID,'&amp;re_mcrid=',$obj_id,'&amp;step=editor&amp;todo=wdelfile&amp;extparm=',$extparm)"/>
                                  <xsl:variable name="delscript" select="concat('doDelDer',../@ID,'_',position(),'()')"/>
                                  <script language="javascript"> 
                                    function <xsl:value-of select="$delscript"/> { 
                                      strInput = confirm('<xsl:value-of select="i18n:translate('component.common-parts.isf.deleteMsg')"/>'); 
                                      if(strInput==true) window.location='<xsl:value-of select="$delpath"/>' } 
                                  </script>
                                  <a href="javascript:{$delscript}">
                                    <img class="button" src="{$WebApplicationBaseURL}images/button_delete.gif"
                                      title="{i18n:translate('component.swf.derivate.delFile')}"/>
                                  </a>
                                </xsl:when>
                                <xsl:otherwise>
                                  <img class="button" style="width:16px" src="{$WebApplicationBaseURL}images/nav-empty.gif"/>
                                </xsl:otherwise>
                              </xsl:choose>
                            </div>
                          </div>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:otherwise>
                        <div class="derivatefiles_const_ltr derivatefiles_var">
                          <xsl:value-of select="i18n:translate('component.swf.empty.derivate')"/>
                        </div>
                      </xsl:otherwise>
                    </xsl:choose>
                  </xsl:for-each>
                </div>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <div class="text_const_ltr text_var">
                <xsl:value-of select="i18n:translate('component.swf.empty.workflow')"/>
              </div>
            </xsl:otherwise>
          </xsl:choose>
        </div> <!-- stop rtl languages -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>