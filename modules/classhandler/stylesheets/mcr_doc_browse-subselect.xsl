<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2006-09-22 09:45:04 $ -->
<!-- ============================================== -->

<!-- +
     | This stylesheet controls the Web-Layout of the MCRClassificationBrowser Servlet.     |
     | This Template is embedded as a Part in the XML-Site, configurated in the Classification
     | section of the mycore.properties.
     | The complete XML stream is sent to the Layout Servlet and finally handled by this stylesheet.
     |
     | Authors: A.Schaar
     | Last changes: 2004-30-10
     + -->

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="xlink">
	
<xsl:variable name="Navigation.title"  select="i18n:translate('titles.pageTitle.chooseClass')" />
<xsl:variable name="MainTitle" select="i18n:translate('titles.mainTitle')"/>
<xsl:variable name="PageTitle" select="$Navigation.title"/>

<!-- ========== Subselect Parameter ========== -->
<xsl:param name="subselect.session" />
<xsl:param name="subselect.varpath" />
<xsl:param name="subselect.webpage" />

<xsl:variable name="subselect.params">
  <xsl:text>XSL.subselect.session=</xsl:text>
  <xsl:value-of select="$subselect.session" />
  <xsl:text>&amp;XSL.subselect.varpath=</xsl:text>
  <xsl:value-of select="$subselect.varpath" />
  <xsl:text>&amp;XSL.subselect.webpage=</xsl:text>
  <xsl:value-of select="$subselect.webpage" />
</xsl:variable>
	
<xsl:variable name="url" >	
	<xsl:value-of select="$ServletsBaseURL" />
    <xsl:text>XMLEditor?_action=end.subselect</xsl:text>
    <xsl:text>&amp;subselect.session=</xsl:text>
    <xsl:value-of select="$subselect.session" />
    <xsl:text>&amp;subselect.varpath=</xsl:text>
    <xsl:value-of select="$subselect.varpath" />
    <xsl:text>&amp;subselect.webpage=</xsl:text>
    <xsl:value-of select="$subselect.webpage" />
</xsl:variable>
	
<!-- The main template -->
<xsl:template match="classificationBrowser">

<xsl:variable name="startPath" select="startPath" />
<xsl:variable name="TrueFalse" select="showComments" />

<xsl:variable name="OnOff">
 <xsl:choose>
  <xsl:when test="$TrueFalse = 'false'">
   <xsl:value-of select="'On'" />
  </xsl:when>
  <xsl:otherwise>
    <xsl:value-of select="'Off'" />
  </xsl:otherwise>
 </xsl:choose>
</xsl:variable>

<xsl:variable name="ChangeComments" select="concat($WebApplicationBaseURL, 'browse/', uri)" />
<xsl:variable name="predecessor" select="navigationtree/@predecessor" />
<xsl:variable name="classifID" select="navigationtree/@classifID" />
<xsl:variable name="view" select="navigationtree/@view" />
<xsl:variable name="doctype" select="navigationtree/@doctype" />
<xsl:variable name="restriction" select="navigationtree/@restriction" />

<xsl:variable name="hrefstart">
   <xsl:value-of select="concat($WebApplicationBaseURL, 'browse/', startPath)" />
</xsl:variable>

<div id="classificationBrowser" >
	
<table cellspacing="0" cellpadding="0" style="width:100%; margin: 3% 0px 3% 2%;"  class="bg_background" >
<tr>
 <td style="width:60%;" class="desc">
 <form action="{$WebApplicationBaseURL}{$subselect.webpage}?XSL.editor.session.id={$subselect.session}" method="post">
   <input type="submit" class="submit" value="{i18n:translate('Browse.cancelSel')}" />
   <br/>

 </form>
 </td>
 <!-- td class="title"><xsl:copy-of select="$PageTitle" /></td -->
 <td style="text-align:right;padding-right:5px;" class="resultcmd" ><xsl:value-of select="description" /></td>
</tr>
<tr>
 <td colspan='2'>
 <div id="navigationtree">
  <table cellspacing="1" cellpadding="2" style="margin: 3% 10px 3% 2%;" >
  <xsl:for-each select="navigationtree/row">
   <xsl:variable name="href1" select="concat($WebApplicationBaseURL, 'browse', col[2]/@searchbase, '?', $subselect.params)" />
   <xsl:variable name="img1"  select="concat($WebApplicationBaseURL, 'images/', col[1]/@folder1, '.gif')" />
   <xsl:variable name="img2"  select="concat($WebApplicationBaseURL, 'images/', col[1]/@folder2, '.gif')" />
   <xsl:variable name="img3"  select="concat($WebApplicationBaseURL, 'images/folder_blank.gif')" />
   <tr class="result" >
      <td >
        <xsl:call-template name="lineLevelLoop">
          <xsl:with-param name="anz" select="col[1]/@lineLevel" />
          <xsl:with-param name="img" select="$img3" />
        </xsl:call-template>
        <xsl:choose>
         <xsl:when test="col[1]/@plusminusbase">
          <a href='{$href1}'><img border="0" src='{$img1}' /></a>
	     </xsl:when>
	     <xsl:otherwise>
	      <img border="0" src='{$img1}' />
         </xsl:otherwise>
        </xsl:choose>
     </td >
       <td class="desc">
	      <a href="{$url}&amp;_var_@categid={col[2]/@lineID}&amp;_var_@title={col[2]/text()}">
		      <xsl:value-of select="col[2]/text()" />
		  </a>
      </td>
      <td class="desc">
	      <xsl:if test="col[2]/comment != ''">
              <i> (<xsl:apply-templates select="col[2]/comment" />) </i>
          </xsl:if>
       </td>
      </tr>
    </xsl:for-each>
   </table>
  </div>
 </td>
</tr>
</table>



  <!--
  <pre><xsl:copy-of select="." /></pre>
  <pre>Begin:<xsl:value-of select="startPath" /></pre>
  <p>ID der Klassifikation:<xsl:value-of select="classifID" /></p>
  <p>Bezeichnung:<xsl:value-of select="label" /></p>
  <p>Begin:<xsl:value-of select="startPath" /></p>
  <p>Vorgänger:<xsl:value-of select="navigationtree/@predecessor" /></p>
  <p>Aktuelle Kategorie:<xsl:value-of select="navigationtree/@categID" /></p>
   -->
</div>

</xsl:template>



<!-- - - - - - - - - Identity Transformation  - - - - - - - - - -->

<xsl:template match='@*|node()'>
  <xsl:copy>
      <xsl:apply-templates select='@*|node()'/>
  </xsl:copy>
</xsl:template>

<xsl:template match="comment">
  <xsl:apply-templates select='@*|node()'/>
</xsl:template>

<xsl:template name="lineLevelLoop">
  <xsl:param name="anz" />
  <xsl:param name="img" />

  <xsl:if test="$anz > 0">
    <img border="0" width="10" src='{$img}' />
    <xsl:call-template name="lineLevelLoop">
      <xsl:with-param name="anz" select="$anz - 1" />
      <xsl:with-param name="img" select="$img" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>
<xsl:include href="MyCoReLayout.xsl" />
<xsl:include href="mcr_doc_browse-subselect.xsl" />
</xsl:stylesheet>
