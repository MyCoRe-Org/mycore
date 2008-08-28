<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.4 $ $Date: 2007-08-16 06:51:55 $ -->
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

<xsl:include href="MyCoReLayout.xsl"/>

<xsl:variable name="Navigation.title" select="i18n:translate('component.classhandler.titles.pageTitle.classBrowse')" />
<xsl:variable name="MainTitle" select="i18n:translate('common.titles.mainTitle')"/>
<xsl:variable name="PageTitle" select="$Navigation.title"/>

<!-- The main template -->
<xsl:template match="classificationBrowser">

<xsl:variable name="startPath" select="startPath" />
<xsl:variable name="hrefstart" select="concat($WebApplicationBaseURL, 'browse/', startPath,$HttpSession)" />
<xsl:variable name="path"  select="concat($WebApplicationBaseURL, 'browse/', uri)" />
	
<div id="classificationBrowser" >
<table id="metaHeading" cellpadding="0" cellspacing="0">
  <tr>
     <td class="titles">
         <xsl:value-of select="concat(i18n:translate('component.classhandler.browse.numOf'),' :')" />
            <xsl:value-of select="cntDocuments" />
     </td>
     <td class="browseCtrl">
            <a href='{$hrefstart}' ><xsl:value-of select="description" /></a>
     </td>
    </tr>
</table>
<!-- IE Fix for padding and border -->
<hr/>

<div id="navigationtree">
<!-- Navigation table -->
<table id="browseClass" cellspacing="0" cellpadding="0">
	
<xsl:variable name="type" select="startPath" />
<xsl:variable name="search" select="searchField" />

<xsl:for-each select="navigationtree">	
 <xsl:variable name="predecessor" select="@predecessor" />
 <xsl:variable name="classifID" select="@classifID" />
 <xsl:variable name="view" select="@view" />
 <xsl:variable name="restriction" select="@restriction" />

 <xsl:for-each select="row">
   <xsl:variable name="href1" select="concat($WebApplicationBaseURL, 'browse', col[2]/@searchbase, $HttpSession)" />
   <xsl:variable name="actnode" select="position()" />  
   <xsl:variable name="query">
    <xsl:value-of select="concat('(',$search,'+=+', col[2]/@lineID, ')')"/>
    <xsl:if test="string-length(../@doctype)>0">
      <xsl:value-of select="concat('+and+', ../@doctype)"/>
    </xsl:if>
   </xsl:variable>
   <xsl:variable name="href2" select="concat($ServletsBaseURL, 'MCRSearchServlet', $HttpSession, '?query=',$query,'&amp;numPerPage=10','&amp;mask=browse/',$type)" />
   <xsl:variable name="img1"  select="concat($WebApplicationBaseURL, 'images/', col[1]/@folder1, '.gif')" />
   <xsl:variable name="img2"  select="concat($WebApplicationBaseURL, 'images/', col[1]/@folder2, '.gif')" />
   <xsl:variable name="img3"  select="concat($WebApplicationBaseURL, 'images/folder_blank.gif')" />
   <xsl:variable name="childpos" select="col[1]/@childpos" />	  
	  
   <tr class="result" valign="top" >
      <td class="image" >
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
       <xsl:variable name="h2" select="string-length(col[2]/@numDocs)" />
       <xsl:variable name="h3" select="4 - $h2" />
       <xsl:variable name="h4" select="col[2]/@numDocs" />
       <xsl:variable name="h6">
         <xsl:if test="$h3 > 0">
           <xsl:value-of select="substring('____', 1, $h3)"/>
         </xsl:if><xsl:value-of select="$h4"/>
       </xsl:variable>

       <td  class="desc" nowrap="yes">
       <xsl:value-of select="i18n:translate('component.classhandler.browse.docs',$h6)"/> 
       </td>
       <td class="desc">
          <xsl:choose>
            <xsl:when test="col[2]/@numDocs > 0">
              <a href='{$href2}'><xsl:value-of select="col[2]/text()" /></a>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="col[2]/text()" />
            </xsl:otherwise>
          </xsl:choose>
       </td>
       <td class="desc">
	      <xsl:choose>
            <xsl:when test="col[2]/comment != ''">
              <i> (<xsl:apply-templates select="col[2]/comment" />) </i>
            </xsl:when>
            <xsl:otherwise>&#160;&#160;           </xsl:otherwise>
          </xsl:choose>
       </td>
     </tr>
	</xsl:for-each>
  </xsl:for-each>
 </table>
</div>	 
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

</xsl:stylesheet>
