<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions" version="1.0"
  exclude-result-prefixes="xlink i18n mcrxml iview2">
  <xsl:param name="MCR.Module-iview2.BaseURL" />
  <xsl:param name="MCR.Module-iview2.DeveloperMode" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorURI" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorStyle" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:variable name="jquery.version" select="'1.6.2'"/>
  <xsl:variable name="jqueryUI.version" select="'1.8.14'"/>

  <xsl:template name="iview2.getViewer" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="extensions" select="''"/>
    <xsl:param name="style" />
    
    <div class="viewerContainer min">
      <xsl:if test="string-length($style) &gt; 0">
        <xsl:attribute name="style">
          <xsl:value-of select="$style"/>
        </xsl:attribute>
      </xsl:if>
      <div class="viewer min" onmousedown="return false;">
        <div class="surface" style="width:100%;height:100%;z-index:30" />
        <div class="well">
          <div class="preload">
            <img height="100%" width="100%" alt="{i18n:translate('component.iview2.preview')}" />
          </div>
        </div>
      </div>
      <script type="text/javascript">
          (function(){
            "use strict";
            var nodes=document.getElementsByTagName('script');
            var currentNode=nodes[nodes.length-1];
            iview.addInstance(new iview.IViewInstance(jQuery(currentNode.parentNode), <xsl:value-of select="iview2:getOptions($groupID, $extensions)" />));
          })();
      </script>
    </div>
  </xsl:template>

  <xsl:template name="iview2.init">
    <!-- design settings -->
    <xsl:param name="styleFolderUri" select="'gfx/'" />
	<!-- chapter settings -->
	<xsl:param name="chapterEmbedded" select="'false'" />
    <xsl:param name="chapDynResize" select="'false'" />
    
    <!-- thumbnail settings -->
    <xsl:param name="DampInViewer" select="'true'" />
    <xsl:if test="mcrxml:putVariable('iview2.init','done')!='done'">
      <script type="text/javascript" src="http://www.google.com/jsapi"/>
      <script type="text/javascript">
      <!-- JQuery Framework -->
        google.load("jquery", "<xsl:value-of select="$jquery.version"/>");
        google.load("jqueryui", "<xsl:value-of select="$jqueryUI.version"/>");
      </script>
      
      <xsl:choose>
        <xsl:when test="$MCR.Module-iview2.DeveloperMode='true'">
          <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview2.js"/>
        </xsl:when>
        <xsl:otherwise>
          <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview2.min.js"/>
        </xsl:otherwise>
      </xsl:choose>
      <script type="text/javascript">
        var styleFolderUri='<xsl:value-of select="$styleFolderUri" />';
        var chapterEmbedded=<xsl:value-of select="$chapterEmbedded" />;
        var chapDynResize=<xsl:value-of select="$chapDynResize" />;
        var DampInViewer=<xsl:value-of select="$DampInViewer" />;
        var i18n = i18n || new iview.i18n('<xsl:value-of select="$WebApplicationBaseURL"/>', '<xsl:value-of select="$CurrentLang"/>', 'component.iview2');
        <xsl:text>loadCssFile('</xsl:text>
        <xsl:value-of select="$WebApplicationBaseURL"/>
        <xsl:choose>
          <xsl:when test="$MCR.Module-iview2.DeveloperMode='true'">
            <xsl:text>modules/iview2/gfx/default/iview2.css', 'iviewCss');</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>modules/iview2/gfx/default/iview2.min.css', 'iviewCss');</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text>loadCssFile('http://ajax.googleapis.com/ajax/libs/jqueryui/</xsl:text>
        <xsl:value-of select="$jqueryUI.version"/>
        <xsl:text>/themes/base/jquery-ui.css');</xsl:text>
      </script>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="iview2.start">
    <xsl:param name="groupID" />
    <xsl:param name="startFile" />

    <!-- Initfunktionen -->
    <script type="text/javascript">
      (function(){
        var instList=Iview['<xsl:value-of select="$groupID"/>'];
        var cI=instList[instList.length-1];
        jQuery(document).ready(function(){
            cI.startViewer('<xsl:value-of select="$startFile" />');
          }
        );
      })();
    </script>
  </xsl:template>

  <xsl:template name="iview2.getImageElement">
    <xsl:param name="derivate" />
    <xsl:param name="imagePath" />
    <xsl:param name="style" select="''" />
    <xsl:param name="class" select="''" />
    <img src="{concat($WebApplicationBaseURL,'servlets/MCRThumbnailServlet/',$derivate,$imagePath)}" style="{$style}" class="{$class}"/>
  </xsl:template>
  
</xsl:stylesheet>