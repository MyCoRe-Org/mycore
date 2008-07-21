<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2007-11-16 10:50:07 $ -->
<!-- ============================================== -->

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
>

<!-- ======== Parameter from MyCoRe LayoutServlet ======== -->
<!--
<xsl:param name="WebApplicationBaseURL"     />
<xsl:param name="ServletsBaseURL"           />
<xsl:param name="DefaultLang"               />
<xsl:param name="CurrentLang"               />
<xsl:param name="MCRSessionID"              />
-->

<!-- ======== internal langauge dependency variables ======== -->

<xsl:variable name="Empty.Workflow">
 <xsl:value-of select="i18n:translate('component.swf.empty.workflow')" />
</xsl:variable>

<xsl:variable name="Empty.Derivate">
 <xsl:value-of select="i18n:translate('component.swf.empty.derivate')" />
</xsl:variable>

<xsl:variable name="Derivate.AddDerivate">
 <xsl:value-of select="i18n:translate('component.swf.derivate.addDerivate')" />
</xsl:variable>

<xsl:variable name="Derivate.AddFile">
 <xsl:value-of select="i18n:translate('component.swf.derivate.addFile')" />
</xsl:variable>

<xsl:variable name="Derivate.DelDerivate">
 <xsl:value-of select="i18n:translate('component.swf.derivate.delDerivate')" />
</xsl:variable>

<xsl:variable name="Derivate.EditDerivate">
 <xsl:value-of select="i18n:translate('component.swf.derivate.editDerivate')" />
</xsl:variable>

<xsl:variable name="Derivate.SetFile">
 <xsl:value-of select="i18n:translate('component.swf.derivate.setFile')" />
</xsl:variable>

<xsl:variable name="Derivate.DelFile">
 <xsl:value-of select="i18n:translate('component.swf.derivate.delFile')" />
</xsl:variable>

<xsl:variable name="Object.EditObject">
 <xsl:value-of select="i18n:translate('component.swf.object.editObject')" />
</xsl:variable>

<xsl:variable name="Object.CommitObject">
 <xsl:value-of select="i18n:translate('component.swf.object.commitObject')" />
</xsl:variable>

<xsl:variable name="Object.DelObject">
 <xsl:value-of select="i18n:translate('component.swf.object.delObject')" />
</xsl:variable>

<xsl:variable name="Object.EditACL">
 <xsl:value-of select="i18n:translate('component.swf.object.editACL')" />
</xsl:variable>

<!-- ======== handles workflow ======== -->

<xsl:template match="workflow">
	<xsl.copy select="."/>
 <xsl:variable name="url" select="concat($ServletsBaseURL,'MCRListWorkflowServlet',$JSessionID,'?XSL.Style=xml&amp;type=',@type,'&amp;step=',@step)" />
 <xsl:apply-templates select="document($url)/mcr_workflow" />
</xsl:template>

<xsl:template match="/mcr_workflow">
 <xsl:variable name="type"><xsl:value-of select="@type" /></xsl:variable>
 <xsl:variable name="step"><xsl:value-of select="@step" /></xsl:variable>
 <xsl:choose>
  <xsl:when test="item">
   <table style="width:98%;margin: 3% 0px 3% 2%;" cellpadding="0" cellspacing="0" >
    <xsl:for-each select="item">
     <xsl:variable name="obj_id"><xsl:value-of select="@ID" /></xsl:variable>
     <xsl:variable name="obj_deletewf"><xsl:value-of select="@deletewf" /></xsl:variable>
     <xsl:variable name="obj_writedb"><xsl:value-of select="@writedb" /></xsl:variable>
     <xsl:variable name="obj_priv">
      <xsl:copy-of select="concat('modify-',substring-before(substring-after($obj_id,'_'),'_'))"/>
     </xsl:variable>
     <tr class="result">
      <td>
       <table width="100%" cellpadding="0" cellspacing="0" >
        <tr>
         <td align="left" class="header" style="font-weight:bolder;">
          <xsl:value-of select="label" />
         </td>
         <td width="20" class="header" />
         <td align="right" class="header" style="text-align:right;">
          <xsl:value-of select="$obj_id" />
         </td>
        </tr>
       </table>
      </td>
     </tr>
     <tr class="result">
      <td class="desc" valign="top">
       <table width="100%" cellpadding="0" cellspacing="0" >
        <tr>
         <td class="metavalue" align="left" valign="top">
          <xsl:for-each select="data">
           <xsl:if test="position() != 1">;</xsl:if>
           <xsl:value-of select="." />
          </xsl:for-each>
         </td>
         <td width="10" />
         <xsl:if test="$type = 'document'">
          <td width="30" valign="top" align="center">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
            <input name="lang" type="hidden" value="{$CurrentLang}" />
            <input name="se_mcrid" type="hidden">
             <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
            </input>
            <input name="type" type="hidden" value="{$type}" />
            <input name="step" type="hidden" value="author" />
            <input name="todo" type="hidden" value="wnewder" />
            <input type="image" src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{$Derivate.AddDerivate}"/>
           </form>
          </td>
          <td width="10" />
         </xsl:if>
         <td width="30" valign="top" align="center">
          <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
           <input name="lang" type="hidden" value="{$CurrentLang}" />
           <input name="se_mcrid" type="hidden">
           <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
           </input>
            <input name="type" type="hidden" value="{$type}" />
            <input name="step" type="hidden" value="{$step}" />
            <input name="todo" type="hidden" value="weditobj" />
            <input type="image" src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{$Object.EditObject}"/>
           </form>
          </td>
          <td width="10" />
         <td width="30" valign="top" align="center">
          <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
           <input name="lang" type="hidden" value="{$CurrentLang}" />
           <input name="se_mcrid" type="hidden">
           <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
           </input>
           <input name="tf_mcrid" type="hidden">
           <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
           </input>
            <input name="type" type="hidden" value="acl" />
            <input name="step" type="hidden" value="{$step}" />
            <input name="todo" type="hidden" value="weditacl" />
            <input type="image" src="{$WebApplicationBaseURL}images/workflow_acledit.gif" title="{$Object.EditACL}"/>
           </form>
          </td>
          <td width="10" />
          <td width="30" valign="top" align="center">
           <xsl:if test="$obj_writedb = 'true'" >
            <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
             <input name="lang" type="hidden" value="{$CurrentLang}" />
             <input name="se_mcrid" type="hidden">
              <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
             </input>
             <input name="type" type="hidden" value="{$type}" />
             <input name="step" type="hidden" value="{$step}" />
             <input name="todo" type="hidden" value="wcommit" />
             <input type="image" src="{$WebApplicationBaseURL}images/workflow_objcommit.gif" title="{$Object.CommitObject}" />
            </form>
		   </xsl:if>
          </td>
          <td width="10" />
          <td width="30" valign="top" align="center">
           <xsl:if test="$obj_deletewf = 'true'" >
            <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
             <input name="lang" type="hidden" value="{$CurrentLang}" />
             <input name="se_mcrid" type="hidden">
              <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
             </input>
             <input name="type" type="hidden" value="{$type}" />
             <input name="step" type="hidden" value="{$step}" />
             <input name="todo" type="hidden" value="wdelobj" />
             <input type="image" src="{$WebApplicationBaseURL}images/workflow_objdelete.gif" title="{$Object.DelObject}" />
            </form>
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
                <xsl:value-of select="@title" />&#160;
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="@label" />&#160;
              </xsl:otherwise>
            </xsl:choose>  
          </td>
          <td width="10" />
          <td valign="top" width="30">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
            <input name="lang" type="hidden" value="{$CurrentLang}" />
            <input name="se_mcrid" type="hidden">
             <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
            </input>
            <input name="re_mcrid" type="hidden">
             <xsl:attribute name="value"><xsl:value-of select="../@ID" /></xsl:attribute>
            </input>
            <input name="type" type="hidden" value="{$type}" />
            <input name="step" type="hidden" value="{$step}" />
            <input name="todo" type="hidden" value="waddfile" />
            <input type="image" src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{$Derivate.AddFile}" />
           </form>
          </td>
          <td width="10" />
          <td valign="top" width="30">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
            <input name="lang" type="hidden" value="{$CurrentLang}" />
            <input name="se_mcrid" type="hidden">
             <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
            </input>
            <input name="re_mcrid" type="hidden">
             <xsl:attribute name="value"><xsl:value-of select="../@ID" /></xsl:attribute>
            </input>
            <input name="type" type="hidden" value="{$type}" />
            <input name="step" type="hidden" value="{$step}" />
            <input name="todo" type="hidden" value="weditder" />
            <input type="image" src="{$WebApplicationBaseURL}images/workflow_deredit.gif" title="{$Derivate.EditDerivate}" />
           </form>
          </td>
          <td width="10" />
          <td valign="top" width="30">
           <xsl:if test="$obj_deletewf = 'true'" >
            <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
             <input name="lang" type="hidden" value="{$CurrentLang}" />
             <input name="se_mcrid" type="hidden">
              <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
             </input>
             <input name="re_mcrid" type="hidden">
              <xsl:attribute name="value"><xsl:value-of select="../@ID" /></xsl:attribute>
             </input>
             <input name="type" type="hidden" value="{$type}" />
             <input name="step" type="hidden" value="{$step}" />
             <input name="todo" type="hidden" value="wdelder" />
             <input type="image" src="{$WebApplicationBaseURL}images/workflow_derdelete.gif" title="{$Derivate.DelDerivate}" />
            </form>
		   </xsl:if>
          </td>
          <td width="10" />
          <td valign="top" width="30" />
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
                <img src="{$WebApplicationBaseURL}images/button_green.gif" />
               </xsl:when>
               <xsl:otherwise>
                <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
                <input name="lang" type="hidden" value="{$CurrentLang}" />
                <input name="se_mcrid" type="hidden">
                 <xsl:attribute name="value"><xsl:value-of select="../@ID" /></xsl:attribute>
                </input>
                <input name="re_mcrid" type="hidden">
                 <xsl:attribute name="value"><xsl:value-of select="../../@ID" /></xsl:attribute>
                </input>
                <input name="type" type="hidden" value="{$type}" />
                <input name="step" type="hidden" value="{$step}" />
                <input name="todo" type="hidden" value="wsetfile" />
                <input name="extparm" type="hidden">
                 <xsl:attribute name="value">####main####<xsl:value-of select="." /></xsl:attribute>
                </input>
                <input type="image" src="{$WebApplicationBaseURL}images/button_light.gif" title="{$Derivate.SetFile}" />
                </form>
               </xsl:otherwise>
              </xsl:choose>
             </td>
             <td valign="top">
              <xsl:choose>
               <xsl:when test="true()">
                <xsl:variable name="fileurl" select="concat($WebApplicationBaseURL,'servlets/MCRFileViewWorkflowServlet/',text(),$JSessionID,'?type=',$type)" />
                <a class="linkButton">
                 <xsl:attribute name="href"><xsl:value-of select="$fileurl"/></xsl:attribute>
                 <xsl:attribute name="target">_blank</xsl:attribute>
                 <xsl:value-of select="." />
                </a>
                [<xsl:value-of select="@size" />]
               </xsl:when>
               <xsl:otherwise>
                <xsl:value-of select="." />
                [<xsl:value-of select="@size" />]
               </xsl:otherwise>
              </xsl:choose>
             </td>
             <td valign="top">
              <xsl:if test="count(../file) != 1" >
              <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet{$HttpSession}" method="get">
               <input name="lang" type="hidden" value="{$CurrentLang}" />
               <input name="se_mcrid" type="hidden">
                <xsl:attribute name="value"><xsl:value-of select="../@ID" /></xsl:attribute>
               </input>
               <input name="re_mcrid" type="hidden">
                <xsl:attribute name="value"><xsl:value-of select="../../@ID" /></xsl:attribute>
               </input>
               <input name="type" type="hidden" value="{$type}" />
               <input name="step" type="hidden" value="{$step}" />
               <input name="todo" type="hidden" value="wdelfile" />
               <input name="extparm" type="hidden">
                <xsl:attribute name="value">####nrall####<xsl:copy-of select="count(../file)"/>####nrthe####<xsl:copy-of select="position()"/>####filename####<xsl:value-of select="." /></xsl:attribute>
               </input>
               <input type="image" src="{$WebApplicationBaseURL}images/button_delete.gif" title="{$Derivate.DelFile}" />
              </form>
              </xsl:if>
             </td>
            </tr>
           </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
           <tr>
            <td valign="top">
             <xsl:value-of select="$Empty.Derivate" />
            </td>
           </tr>
          </xsl:otherwise>
         </xsl:choose>
        </table>
       </td>
      </tr>
     </xsl:for-each>
     <tr colspan="3"><td>&#160;</td></tr>
    </xsl:for-each>
   </table>
  </xsl:when>
  <xsl:otherwise>
   <center><span class="desc"><xsl:value-of select="$Empty.Workflow" /></span></center>
   <p />
  </xsl:otherwise>
 </xsl:choose>
</xsl:template>

</xsl:stylesheet>
