<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:iview2="xalan://org.mycore.frontend.iview2.MCRIView2XSLFunctions" version="1.0"
  exclude-result-prefixes="xlink i18n mcrxml iview2">
  <xsl:param name="MCR.Module-iview2.BaseURL" />
  <xsl:param name="MCR.Module-iview2.DeveloperMode" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorURI" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorStyle" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:variable name="jqueryUI.version" select="'1.8.12'"/>
  <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" />
  <xsl:template name="iview2.getViewer" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="chapter" select="'true'" />
    <xsl:param name="cutOut" select="'true'" />
    <xsl:param name="overview" select="'true'" />
    <xsl:param name="style" />
    
    <div id="viewerContainer{$groupID}" class="viewerContainer min">
      <xsl:if test="string-length($style) &gt; 0">
        <xsl:attribute name="style">
          <xsl:value-of select="$style"/>
        </xsl:attribute>
      </xsl:if>
      <div id="viewer{$groupID}" class="viewer min" onmousedown="return false;">
        <div class="surface" style="width:100%;height:100%;z-index:30">
        </div>
        <div class="well">
          <div class="preload">
            <img height="100%" width="100%" id="preloadImg{$groupID}" alt="{i18n:translate('component.iview2.preview')}" />
          </div>
        </div>
      </div>
      <script type="text/javascript">
        <xsl:variable name="baseUris">
          <xsl:choose>
            <xsl:when test="string-length($MCR.Module-iview2.BaseURL)&lt;10">
              <xsl:value-of select="concat($ServletsBaseURL,'MCRTileServlet')" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$MCR.Module-iview2.BaseURL" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
          var currentNode=(function(){
            var nodes=document.getElementsByTagName('script');
            return nodes[nodes.length-1];
          })();
          (function initViewer(viewID){
            var options = {
              'useChapter' : <xsl:value-of select="$chapter" />,
              'useCutOut' : <xsl:value-of select="$cutOut" />,
              'useOverview' : <xsl:value-of select="$overview" />,
              'baseUri' : "<xsl:value-of select="$baseUris"/>".split(","),
              'webappBaseUri' : "<xsl:value-of select="$WebApplicationBaseURL"/>",
              'pdfCreatorURI' : "<xsl:value-of select="$MCR.Module-iview2.PDFCreatorURI"/>",
              'pdfCreatorStyle' : "<xsl:value-of select="$MCR.Module-iview2.PDFCreatorStyle"/>",
              'derivateId' : viewID,
            };
            if (typeof Iview[viewID] ==="undefined"){
              Iview[viewID] = [];
            }
            Iview[viewID].push(new iview.IViewInstance(jQuery(currentNode.parentNode), options));
          })('<xsl:value-of select="$groupID" />');
      </script>
    </div>
  </xsl:template>

  <xsl:template name="iview2.init">
    <xsl:param name="groupID" />
    
    <!-- design settings -->
    <xsl:param name="styleFolderUri" select="'gfx/'" />
    <xsl:param name="effects" select="'true'" />
	<!-- chapter settings -->
	<xsl:param name="chapterEmbedded" select="'false'" />
    <xsl:param name="chapDynResize" select="'false'" />
    
    <!-- thumbnail settings -->
    <xsl:param name="DampInViewer" select="'true'" />
    <script type="text/javascript" src="http://www.google.com/jsapi"></script>
    <script type="text/javascript">
    <!-- JQuery Framework -->
      google.load("jquery", "1");
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
      var Iview = Iview || {};
      var i18n = i18n || new iview.i18n('<xsl:value-of select="$WebApplicationBaseURL"/>', '<xsl:value-of select="$CurrentLang"/>');
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
  </xsl:template>
  <xsl:template name="iview2.start">
    <xsl:param name="groupID" />
    <xsl:param name="startFile" />

    <!-- Initfunktionen -->
    <script type="text/javascript">
      jQuery(document).ready(function(){
          var instList=Iview['<xsl:value-of select="$groupID"/>'];
          instList[instList.length-1].startViewer('<xsl:value-of select="$startFile" />');
        }
      );
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