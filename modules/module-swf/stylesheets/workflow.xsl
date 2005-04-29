<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2005-04-29 10:15:29 $ -->
<!-- ============================================== -->

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
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
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Der Workflow ist leer.</xsl:when>
  <xsl:otherwise>The workflow is empty.</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Empty.Derivate">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Leeres Derivate</xsl:when>
  <xsl:otherwise>Empty Derivate</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Derivate.AddDerivate">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Hinzufügen eines Datenobjektes</xsl:when>
  <xsl:otherwise>Add a data object</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Derivate.AddFile">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Hinzufügen einer Datei</xsl:when>
  <xsl:otherwise>Add a file</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Derivate.DelDerivate">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Löschen dieses Datenobjektes</xsl:when>
  <xsl:otherwise>Delete this data object</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Derivate.EditDerivate">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Bearbeiten des Datenobjekt Label</xsl:when>
  <xsl:otherwise>Edit this data object label</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Derivate.SetFile">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Setzen der Hauptdatei</xsl:when>
  <xsl:otherwise>Set the main file</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Derivate.DelFile">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Löschen dieser Datei</xsl:when>
  <xsl:otherwise>Remove this file</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Object.EditObject">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Bearbeiten dieses Dokumentes</xsl:when>
  <xsl:otherwise>Edit this document</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Object.CommitObject">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Laden dieses Dokumentes in den Server</xsl:when>
  <xsl:otherwise>commit this document to the server</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="Object.DelObject">
 <xsl:choose>
  <xsl:when test="$CurrentLang = 'de'">Löschen dieses Dokumentes</xsl:when>
  <xsl:otherwise>Delete this document</xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<!-- ======== handles workflow ======== -->

<xsl:template match="workflow">
 <xsl:variable name="url" select="concat($WebApplicationBaseURL,'servlets/MCRListWorkflowServlet',$JSessionID,'?XSL.Style=xml&amp;type=',@type,'&amp;step=',@step)" />
 <xsl:apply-templates select="document($url)/mcr_workflow" />
</xsl:template>

<xsl:template match="/mcr_workflow">
 <xsl:variable name="PrivOfUser">
  <xsl:call-template name="PrivOfUser" />
 </xsl:variable>
 <xsl:variable name="type"><xsl:value-of select="@type" /></xsl:variable>
 <xsl:variable name="step"><xsl:value-of select="@step" /></xsl:variable>
 <xsl:choose>
  <xsl:when test="item">
   <table width="100%" cellpadding="0" cellspacing="0" id="simpleWorkflow" >
    <xsl:for-each select="item">
     <xsl:variable name="obj_id"><xsl:value-of select="@ID" /></xsl:variable>
     <xsl:variable name="obj_priv">
      <xsl:copy-of select="concat('modify-',substring-before(substring-after($obj_id,'_'),'_'))"/>
     </xsl:variable>
     <tr>
      <td>
       <table width="100%" cellpadding="0" cellspacing="0" id="simpleWorkflow" >
        <tr>
         <td align="left" class="textboldnormal">
          <xsl:value-of select="label" />
         </td>
         <td width="20" class="textboldnormal" />
         <td align="right" class="textboldnormal">
          <xsl:value-of select="$obj_id" />
         </td>
        </tr>
       </table>
      </td>
     </tr>
     <tr>
      <td>
       <table width="100%" cellpadding="0" cellspacing="0" id="simpleWorkflow" >
        <tr>
         <td class="metavalue" align="left" valign="top">
          <xsl:for-each select="data">
           <xsl:if test="position() != 1">;</xsl:if>
           <xsl:value-of select="." />
          </xsl:for-each>
         </td>
         <td width="10" />
         <xsl:if test="$type = 'document' or $type = 'disshab'">
          <td width="30" valign="top" align="center">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
            <input name="lang" type="hidden" value="{$CurrentLang}" />
            <input name="se_mcrid" type="hidden">
             <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
            </input>
            <input name="type" type="hidden" value="{$type}" />
            <input name="step" type="hidden" value="author" />
            <input name="todo" type="hidden" value="wnewder" />
            <input type="image" src="{$WebApplicationBaseURL}images/static/workflow_deradd.gif" title="{$Derivate.AddDerivate}"/>
           </form>
          </td>
          <td width="10" />
         </xsl:if>
         <td width="30" valign="top" align="center">
          <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
           <input name="lang" type="hidden" value="{$CurrentLang}" />
           <input name="se_mcrid" type="hidden">
           <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
           </input>
            <input name="type" type="hidden" value="{$type}" />
            <input name="step" type="hidden" value="{$step}" />
            <input name="todo" type="hidden" value="weditobj" />
            <input type="image" src="{$WebApplicationBaseURL}images/static/workflow_objedit.gif" title="{$Object.EditObject}"/>
           </form>
          </td>
          <td width="10" />
          <td width="30" valign="top" align="center">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
           <input name="lang" type="hidden" value="{$CurrentLang}" />
           <input name="se_mcrid" type="hidden">
            <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
           </input>
           <input name="type" type="hidden" value="{$type}" />
           <input name="step" type="hidden" value="{$step}" />
           <input name="todo" type="hidden" value="wcommit" />
           <input type="image" src="{$WebApplicationBaseURL}images/static/workflow_objcommit.gif" title="{$Object.CommitObject}" />
          </form>
         </td>
         <td width="10" />
         <td width="30" valign="top" align="center">
          <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
          <input name="lang" type="hidden" value="{$CurrentLang}" />
           <input name="se_mcrid" type="hidden">
            <xsl:attribute name="value"><xsl:value-of select="@ID" /></xsl:attribute>
           </input>
           <input name="type" type="hidden" value="{$type}" />
           <input name="step" type="hidden" value="{$step}" />
           <input name="todo" type="hidden" value="wdelobj" />
           <input type="image" src="{$WebApplicationBaseURL}images/static/workflow_objdelete.gif" title="{$Object.DelObject}" />
          </form>
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
           <xsl:value-of select="@label" />&#160;
          </td>
          <td width="10" />
          <td valign="top" width="30">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
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
            <input type="image" src="{$WebApplicationBaseURL}images/static/workflow_deradd.gif" title="{$Derivate.AddFile}" />
           </form>
          </td>
          <td width="10" />
          <td valign="top" width="30">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
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
            <input type="image" src="{$WebApplicationBaseURL}images/static/workflow_deredit.gif" title="{$Derivate.EditDerivate}" />
           </form>
          </td>
          <td width="10" />
          <td valign="top" width="30">
           <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
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
            <input type="image" src="{$WebApplicationBaseURL}images/static/workflow_derdelete.gif" title="{$Derivate.DelDerivate}" />
           </form>
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
                <img src="{$WebApplicationBaseURL}images/static/button_green.gif" />
               </xsl:when>
               <xsl:otherwise>
                <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
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
                <input type="image" src="{$WebApplicationBaseURL}images/static/button_light.gif" title="{$Derivate.SetFile}" />
                </form>
               </xsl:otherwise>
              </xsl:choose>
             </td>
             <td valign="top"> 
              <xsl:choose>
               <xsl:when test="contains($PrivOfUser,$obj_priv)">
                <xsl:variable name="fileurl" select="concat($WebApplicationBaseURL,'servlets/MCRFileViewWorkflowServlet?type=',$type,'&amp;file=',text())" />
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
              <form action="{$WebApplicationBaseURL}servlets/MCRStartEditorServlet" method="get">
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
               <input type="image" src="{$WebApplicationBaseURL}images/static/button_delete.gif" title="{$Derivate.DelFile}" />
              </form>
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
     <tr colspan="5"><td>&#160;</td></tr>
    </xsl:for-each>
   </table>
  </xsl:when>
  <xsl:otherwise>
   <center><span class="textnormalnormal"><xsl:value-of select="$Empty.Workflow" /></span></center>
   <p />
  </xsl:otherwise>
 </xsl:choose>
</xsl:template>

</xsl:stylesheet>
