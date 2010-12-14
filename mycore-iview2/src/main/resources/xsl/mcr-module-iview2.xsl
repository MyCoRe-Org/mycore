<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:iview2="xalan://org.mycore.frontend.iview2.MCRIView2XSLFunctions" version="1.0"
  exclude-result-prefixes="xlink i18n mcrxml iview2">
  <xsl:param name="MCR.Module-iview2.BaseURL" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" />
  <xsl:template name="iview2.getViewer" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="zoomBar" select="'true'" />
    <xsl:param name="chapter" select="'true'" />
    <xsl:param name="cutOut" select="'true'" />
    <xsl:param name="overview" select="'true'" />
    <xsl:param name="style" />
    
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
      var baseUris='["'+'<xsl:value-of select="$baseUris"/>'.split(',').join('","')+'"]';
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useZoomBar',<xsl:value-of select="$zoomBar" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useChapter',<xsl:value-of select="$chapter" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useCutOut',<xsl:value-of select="$cutOut" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useOverview',<xsl:value-of select="$overview" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'baseUri', baseUris);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'webappBaseUri', '"<xsl:value-of select="$WebApplicationBaseURL"/>"');
    </script>
    <div id="viewerContainer{$groupID}" class="viewerContainer min">
      <xsl:if test="string-length($style) &gt; 0">
        <xsl:attribute name="style">
          <xsl:value-of select="$style"/>
        </xsl:attribute>
      </xsl:if>
      <div id="blackBlank{$groupID}" class="blackBlank">
      </div>
      <div id="viewer{$groupID}" class="viewer min" onmousedown="return false;">
        <div class="surface" style="width:100%;height:100%;z-index:30">
        </div>
        <div class="well">
          <div id="preload{$groupID}" class="preload">
            <img height="100%" width="100%" id="preloadImg{$groupID}" alt="{i18n:translate('component.iview2.preview')}" />
          </div>
        </div>
      </div>
      <script>
     	Iview['<xsl:value-of select="$groupID" />'].viewerContainer = jQuery(document).find("#viewerContainer<xsl:value-of select="$groupID" />");
      </script>
      <xsl:call-template name="iview2.getToolbar">
        <xsl:with-param name="groupID" select="$groupID" />
        <xsl:with-param name="optOut" select="'false'" />
        <xsl:with-param name="forward" select="'true'" />
        <xsl:with-param name="backward" select="'true'" />
      </xsl:call-template>
    </div>

  </xsl:template>
  <xsl:template name="iview2.getToolbar" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="idAdd" />
    <xsl:param name="create" />
    <xsl:param name="optOut" select="'false'" />
    <xsl:param name="zoomIn" select="$optOut" />
    <xsl:param name="zoomOut" select="$optOut" />
    <xsl:param name="normalView" select="$optOut" />
    <xsl:param name="fullView" select="$optOut" />
    <xsl:param name="toWidth" select="$optOut" />
    <xsl:param name="toScreen" select="$optOut" />
    <xsl:param name="backward" select="$optOut" />
    <xsl:param name="forward" select="$optOut" />
    <xsl:param name="openThumbs" select="$optOut" />
    <xsl:param name="chapterOpener" select="$optOut" />
    <xsl:param name="permalink" select="$optOut" />
    
    <!-- online src-->
    
	<!-- jQuery Lib Files -->
    <script type="text/javascript" src="http://www.google.com/jsapi"></script>
	<script type="text/javascript">
      google.load("jquery", "1.4.2");
      google.load("jqueryui", "1.8.6");
    </script>
	<!-- button -->
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/lib/jqueryUI/ui.button.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/lib/jqueryUI/ui.widget.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/lib/jqueryUI/ui.core.js" />	
	
	<!-- menu -->
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/lib/fg-menu/fg.menu.js" />
	
	<!-- Importer Skript -->
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarImporter.js" />
	
	<!--  Model, Controller, View -->
	<!--  needs Event.js -->
	
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarManager.js" />
	
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarModel.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarButtonsetModel.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarDividerModel.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarTextModel.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarButtonModel.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarController.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/ToolbarView.js" />
	
	<!--  ModelProvider -->
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/StandardToolbarModelProvider.js" />
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/iview2.toolbar/PreviewToolbarModelProvider.js" />
	
	<!--  CSS -->
	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/web/lib/jqueryUI/themes/base/ui.base.css" />
	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/web/lib/jqueryUI/themes/base/ui.theme.css" />
	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/web/lib/jqueryUI/ui.toolbar.css" />
	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/web/lib/fg-menu/fg.menu.css" />
	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/web/gfx/default/iview2.toolbar.css" />
	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/web/gfx/default/iview2.permalink.css" />
    
    <!-- Permalink -->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/Permalink.js" />
    
	
     
    <div id="toolbars{$groupID}" class="toolbars" onmousedown="return false;">      
        <script type="text/javascript">
      		$(document).ready(function () {
      	
				// toolbar brauch ne Liste mit Controllern für die Übergabe (bspw. Chapter)
				      		
				var viewID = '<xsl:value-of select="$groupID" />';
				
				var titles = {
					'zoomIn' : '<xsl:value-of select="i18n:translate('component.iview2.zoomIn')"/>',
					'zoomOut' : '<xsl:value-of select="i18n:translate('component.iview2.zoomOut')"/>',
					'fitToWidth' : '<xsl:value-of select="i18n:translate('component.iview2.toWidth')"/>',
					'fitToScreen' : '<xsl:value-of select="i18n:translate('component.iview2.toScreen')"/>',
					'openOverview' : '<xsl:value-of select="i18n:translate('component.iview2.openThumbs')"/>',
					'openChapter' : '<xsl:value-of select="i18n:translate('component.iview2.chapterOpener')"/>',
					'backward' : '<xsl:value-of select="i18n:translate('component.iview2.backward')"/>',
					'forward' : '<xsl:value-of select="i18n:translate('component.iview2.forward')"/>',
					'permalink' : '<xsl:value-of select="i18n:translate('component.iview2.permalink')"/>',
					'close' : '<xsl:value-of select="i18n:translate('component.iview2.normalView')"/>',
					'pageBox' : '<xsl:value-of select="i18n:translate('component.iview2.pageBox')"/>'
				};
			
				Iview[viewID].ToolbarImporter = new ToolbarImporter(Iview[viewID], titles);
      		});
        </script>
        </div>
  </xsl:template>
  <xsl:template name="iview2.init">
    <xsl:param name="groupID" />
    <xsl:param name="prefix"/>
    <xsl:param name="tilesize" select="'256'" />
    
    <!-- startUp settings -->
    <xsl:param name="maximized" select="'false'" />
    <xsl:param name="zoomWidth" select="'false'" />
    <xsl:param name="zoomScreen" select="'false'" />
    
    <!-- design settings -->
    <xsl:param name="effects" select="'true'" />
	<!-- chapter settings -->
	<xsl:param name="chapterEmbedded" select="'false'" />
    <xsl:param name="chapDynResize" select="'false'" />
    
    <!-- thumbnail settings -->
    <xsl:param name="DampInViewer" select="'true'" />
    <script type="text/javascript" src="http://www.google.com/jsapi"></script>
    <script type="text/javascript">google.load("jquery", "1.4.2");</script>
    <!-- JQuery Framework -->
    <script type="text/javascript">
      <xsl:text>var prefix='</xsl:text>
      <xsl:value-of select="$prefix" />
      <xsl:text>';</xsl:text>
      <xsl:text>var tilesize=</xsl:text>
      <xsl:value-of select="$tilesize" />
      <xsl:text>;</xsl:text>
      <xsl:text>var maximized=</xsl:text>
      <xsl:value-of select="$maximized" />
      <xsl:text>;</xsl:text>
      <xsl:text>var zoomWidth=</xsl:text>
      <xsl:value-of select="$zoomWidth" />
      <xsl:text>;</xsl:text>
      <xsl:text>var zoomScreen=</xsl:text>
      <xsl:value-of select="$zoomScreen" />
      <xsl:text>;</xsl:text>
      <xsl:text>jQuery.fx.off=!</xsl:text>
      <xsl:value-of select="$effects" />
      <xsl:text>;</xsl:text>
      <xsl:text>var chapterEmbedded=</xsl:text>
      <xsl:value-of select="$chapterEmbedded" />
      <xsl:text>;</xsl:text>
      <xsl:text>var chapDynResize=</xsl:text>
      <xsl:value-of select="$chapDynResize" />
      <xsl:text>;</xsl:text>
      <xsl:text>var DampInViewer=</xsl:text>
      <xsl:value-of select="$DampInViewer" />
      <xsl:text>;</xsl:text>
    </script>
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/LAB.min.js"/>
    <!-- LAB JS Loader Lib -->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/jquery.mousewheel.min.js" />
    <!-- JQuery Mousewheel support -->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/ManageEvents.js" />
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/PanoJS.js" />
    <!-- Viewer -->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/Event.js"/>
    <!-- Event Registration utility -->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/XML.js" />
    <!--XML Funktionen-->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/Utils.js" />
    <!--Allgemeine Util Funktionen-->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/scrollBars.js" />
    <!--Scrollbar Klasse-->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/cutOut.js" />
    <!--Ausschnittbildchen Klasse-->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/METS.js" />
    <!--METS Klasse-->
	<script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/Thumbnail.js" />
    <!--Hauptdatei-->
    <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/web/js/init.js"/>
    <!-- Init Funktionen -->
    <script type="text/javascript">
      function addIviewProperty(viewID, propertyName, val) {
      if (typeof (Iview) == "undefined") eval("Iview = new Object()");
      if (typeof (Iview[viewID]) == "undefined") {
      Iview[viewID] = new Object();
      }
      eval('Iview["'+viewID+'"].'+propertyName+'= '+val+';');
      }
    </script>
  </xsl:template>
  <xsl:template name="iview2.start">
    <xsl:param name="groupID" />
    <xsl:param name="style" select="'default'" />
    <!-- params out of config.xml -->
    <xsl:param name="styleFolderUri" select="'gfx/'" />
    <xsl:param name="startFile" />

    <link id="cssSheet{$groupID}" rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/web/gfx/{$style}/style.css" />
    <!-- Initfunktionen -->
    <script type="text/javascript">
    <!-- Philipp möchte verbessern -->
      var styleName='<xsl:value-of select="$style" />';
      var styleFolderUri='<xsl:value-of select="$styleFolderUri" />';
      addIviewProperty('<xsl:value-of select="$groupID" />', 'startFile', "'<xsl:value-of select="$startFile" />'");
      
      function startViewer(viewID) {
        if (Iview[viewID].started) return;
        Iview[viewID].started = true;
        $LAB.setGlobalDefaults({"AllowDuplicates": false, "BasePath": '../modules/iview2/web/js/'});
        loading(viewID);
      }
      ManageEvents.addEventListener(window, 'load', function() { startViewer('<xsl:value-of select="$groupID"/>');}, false);
    </script>
  </xsl:template>
  <xsl:template name="iview2.getZoomBar">
    <xsl:param name="groupID" />
    <xsl:param name="parent" select="'viewer'" />
    <xsl:param name="direction" select="'true'" />
    <xsl:param name="horizontal" select="'true'" />
    <xsl:param name="idAdd" />
    <xsl:choose>
      <xsl:when test="$parent = 'viewer'">
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />', 'zoomBarParent', '"viewer<xsl:value-of select="$groupID" />"');
          addIviewProperty('<xsl:value-of select="$groupID" />', 'zoomBarDirection', '<xsl:value-of select="$direction" />');
          addIviewProperty('<xsl:value-of select="$groupID" />', 'zoomBarHorz', '<xsl:value-of select="$horizontal" />"');
        </script>
      </xsl:when>
      <xsl:when test="$parent = 'here'">
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','zoomBarParent', '"zoomBarContainer<xsl:value-of select="$groupID" />"');
          addIviewProperty('<xsl:value-of select="$groupID" />', 'zoomBarDirection', '<xsl:value-of select="$direction" />');
          addIviewProperty('<xsl:value-of select="$groupID" />', 'zoomBarHorz', '<xsl:value-of select="$horizontal" />');
        </script>
        <div id="zoomBarContainer{$groupID}" class="zoomBarContainer{$idAdd}"></div>
      </xsl:when>
      <xsl:otherwise>
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','zoomBarParent', '"<xsl:value-of select="$parent" />"');
          addIviewProperty('<xsl:value-of select="$groupID" />', 'zoomBarDirection', '<xsl:value-of select="$direction" />');
          addIviewProperty('<xsl:value-of select="$groupID" />', 'zoomBarHorz', '<xsl:value-of select="$horizontal" />');
        </script>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="iview2.getThumbnail">
    <xsl:param name="groupID" />
    <xsl:param name="parent" select="'viewer'" />
    <xsl:param name="idAdd" />
    <xsl:choose>
      <xsl:when test="$parent = 'viewer'">
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','ausschnittParent', '"viewer<xsl:value-of select="$groupID" />"');
        </script>
      </xsl:when>
      <xsl:when test="$parent = 'here'">
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','ausschnittParent', '"thumbnailContainer<xsl:value-of select="$groupID" />"');
        </script>
        <div id="thumbnailContainer{$groupID}" class="thumbnailContainer{$idAdd}"></div>
      </xsl:when>
      <xsl:otherwise>
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','ausschnittParent', '"<xsl:value-of select="$parent" />"');
        </script>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="iview2.getChapter">
    <xsl:param name="groupID" />
    <xsl:param name="parent" select="'viewer'" />
    <xsl:param name="idAdd" />
    <xsl:choose>
      <xsl:when test="$parent ='viewer'">
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','chapterParent', '"#viewerContainer<xsl:value-of select="$groupID" />"');
        </script>
      </xsl:when>
      <xsl:when test="$parent = 'here'">
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','chapterParent','"#chapterContainer<xsl:value-of select="$groupID" />"');
        </script>
        <div id="chapterContainer{$groupID}" class="chapterContainer{$idAdd}"></div>
      </xsl:when>
      <xsl:otherwise>
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','chapterParent','"<xsl:value-of select="$parent" />"');
        </script>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="iview2.getImageElement">
    <xsl:param name="derivate" />
    <xsl:param name="imagePath" />
    <xsl:param name="style" select="''" />
    <xsl:param name="class" select="''" />
    <img src="{concat($WebApplicationBaseURL,'servlets/MCRThumbnailServlet/',$derivate,$imagePath)}" style="{$style}" class="{$class}"/>
  </xsl:template>
</xsl:stylesheet>