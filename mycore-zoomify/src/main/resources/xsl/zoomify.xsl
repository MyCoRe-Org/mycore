<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.6 $ $Date: 2009/03/03 11:01:07 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="i18n">

<xsl:param name="ImagePath"/>
<xsl:param name="Orderlabel" />
<xsl:param name="mcrid"/>
<xsl:param name="remcrid"/>
<xsl:param name="semcrid" />
<xsl:param name="index" />
<xsl:param name="max" />
<xsl:param name="label" />

<!-- - - - - variables for starting zoomify viewer - - - - -->

<xsl:variable name="zoomify.codebase"         select="'http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,40,0'" />
<xsl:variable name="zoomify.width"            select="'750'" />
<xsl:variable name="zoomify.height"           select="'450'" />
<xsl:variable name="zoomify.id"               select="'theMovie'" />
<xsl:variable name="zoomify.plugin"           select="'http://www.macromedia.com/shockwave/download/index.cgi?P1_Prod_Version=ShockWaveFlash'" />
<xsl:variable name="zoomify.classid"          select="'clsid:D27CDB6E-AE6D-11cf-96B8-444553540000'" />
<xsl:variable name="zoomify.path"             select="concat('zoomifyImagePath=',$WebApplicationBaseURL,'servlets/MCRZipFileNodeServlet',$ImagePath)" />
<xsl:variable name="zoomify.app"              select="concat($WebApplicationBaseURL,'ZoomifyViewer.swf')" />
<xsl:variable name="zoomify.back"             select="concat($WebApplicationBaseURL,'receive/',$remcrid)" />

<xsl:template match="zoomify">
  <xsl:variable name="url"> 
    <xsl:value-of select="concat($WebApplicationBaseURL,'servlets/MCRStartZoomifyServlet',$HttpSession,'?re_mcrid=',$remcrid,'&amp;se_mcrid=',$semcrid,'&amp;tf_mcrid=',$mcrid,'&amp;step=commit&amp;todo=showZoomify')"/>
  </xsl:variable>
  <xsl:variable name="zoomify.next"             select="concat($url,'&amp;mode=next')" />
  <xsl:variable name="zoomify.prev"             select="concat($url,'&amp;mode=prev')" />
  <xsl:variable name="zoomify.first"            select="concat($url,'&amp;mode=first')" />
  <xsl:variable name="zoomify.last"             select="concat($url,'&amp;mode=last')" />
  
  <!--<xsl:variable name="httpSession"> 
    <xsl:value-of select="substring-after($JSessionID,'=')"/>
  </xsl:variable>-->

  <table width="750" height="20">
	<tr>
		<td width="33%" align="left"><xsl:value-of select="i18n:translate('component.zoomify.page.zoomify.source')" /><xsl:text>: </xsl:text><xsl:value-of select="$label" /></td>
		<td width="33%" align="center"><xsl:value-of select="i18n:translate('component.zoomify.page.zoomify.orderlabel')" /><xsl:text></xsl:text><xsl:value-of select="$Orderlabel"></xsl:value-of></td>
		<td width="33%" align="right"><xsl:value-of select="i18n:translate('component.zoomify.page.zoomify.page')" /><xsl:text> </xsl:text><xsl:value-of select="$index" /><xsl:text>/</xsl:text><xsl:value-of select="$max" /></td>
	</tr>
  </table>
	
	<p></p>

  <object 
    classid  = "{$zoomify.classid}"
    codebase = "{$zoomify.codebase}"
    width    = "{$zoomify.width}" 
    height   = "{$zoomify.height}" >
    <param name="FlashVars" value="{$zoomify.path}"/>
	<param name="SRC" value="{$zoomify.app}" />
	<param name="MENU" value="FALSE" />
    <param name="codebase"         value="{$zoomify.plugin}" />
            
    <noembed> <xsl:text>Fehler</xsl:text> </noembed>

    <comment> <!-- for netscape -->
      <xsl:element name="embed">
        <xsl:attribute name="FlashVars">     <xsl:value-of select="$zoomify.path"/>           </xsl:attribute> 
        <xsl:attribute name="SRC">           <xsl:value-of select="$zoomify.app"/>            </xsl:attribute>
		<xsl:attribute name="MENU">          <xsl:value-of select="FALSE" />                  </xsl:attribute> 
        <xsl:attribute name="width">         <xsl:value-of select="$zoomify.width"/>          </xsl:attribute> 
        <xsl:attribute name="height">        <xsl:value-of select="$zoomify.height"/>         </xsl:attribute> 
        <xsl:attribute name="pluginspage">   <xsl:value-of select="$zoomify.plugin"/>         </xsl:attribute> 
                
        <noembed> <xsl:text>Fehler</xsl:text> </noembed>
       </xsl:element>
    </comment>

  </object>
  <p></p>
  <table width="750" height="20">
	<tr>
		<td width="33%" align="left">
			<xsl:if test="$index > 1">
                <!--<a href="{$zoomify.prev}"><xsl:value-of select="i18n:translate('component.zoomify.page.zoomify.prev')" /></a>-->
                <a href="{$zoomify.first}"><img src="../images/nav-first.gif" /></a>
                <a href="{$zoomify.prev}"><img src="../images/nav-back.gif" /></a>
            </xsl:if>
		</td>
		<td width="33%" align="center">
			<a href="{$zoomify.back}"><xsl:value-of select="i18n:translate('component.zoomify.page.zoomify.back')" /></a>
		</td>
		<td width="33%" align="right">
            <xsl:if test="$index &lt; $max">
                <!--<a href="{$zoomify.next}"><xsl:value-of select="i18n:translate('component.zoomify.page.zoomify.next')" /></a>-->
                <a href="{$zoomify.next}"><img src="../images/nav-forward.gif" /></a>
                <a href="{$zoomify.last}"><img src="../images/nav-last.gif" /></a>
            </xsl:if>
        </td>
	</tr>
  </table>
	
</xsl:template>

</xsl:stylesheet>
