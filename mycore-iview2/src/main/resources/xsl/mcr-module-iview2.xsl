<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" version="1.0"
  exclude-result-prefixes="xlink i18n mcrxml">
  <xsl:param name="MCR.Module-iview2.BaseURL" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" />
  <xsl:template name="iview2.getViewer" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="zoomBar" select="'true'" />
    <xsl:param name="chapter" select="'true'" />
    <xsl:param name="cutOut" select="'true'" />
    <xsl:param name="overview" select="'true'" />
    
    <xsl:variable name="derivxml" select="concat('ifs:/',@xlink:href)" />
    <xsl:variable name="details" select="document($derivxml)" />
    <script type="text/javascript">
      var baseUris='["'+'<xsl:value-of select="$MCR.Module-iview2.BaseURL"/>'.split(',').join('","')+'"]';
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useZoomBar',<xsl:value-of select="$zoomBar" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useChapter',<xsl:value-of select="$chapter" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useCutOut',<xsl:value-of select="$cutOut" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'useOverview',<xsl:value-of select="$overview" />);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'baseUri', baseUris);
      addIviewProperty('<xsl:value-of select="$groupID" />', 'webappBaseUri', '"<xsl:value-of select="$WebApplicationBaseURL"/>"');
      <xsl:choose>
        <xsl:when test="$details/mcr_directory/children/child/name[text()='mets.xml']">
          addIviewProperty('<xsl:value-of select="$groupID" />', 'hasMets', true);
        </xsl:when>
        <xsl:otherwise>
          addIviewProperty('<xsl:value-of select="$groupID" />', 'hasMets', false);
        </xsl:otherwise>
      </xsl:choose>
    </script>
    <div id="viewerContainer{$groupID}" class="viewerContainer min">
      <div id="blackBlank{$groupID}" class="blackBlank">
      </div>
      <div id="viewer{$groupID}" class="viewer min" onmousedown="return false;">
        <div class="surface" style="width:100%;height:100%;z-index:30">
        </div>
        <div class="well">
          <div id="preload{$groupID}" class="preload">
            <xsl:call-template name="iview2.getToolbar">
              <xsl:with-param name="groupID" select="$groupID" />
              <xsl:with-param name="optOut" select="'false'" />
              <xsl:with-param name="forward" select="'true'" />
              <xsl:with-param name="backward" select="'true'" />
            </xsl:call-template>
            <img height="100%" width="100%" id="preloadImg{$groupID}" alt="{i18n:translate('component.iview2.preview')}" />
          </div>
        </div>
      </div>
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
    <xsl:param name="inputPage" select="$optOut" />
    <xsl:param name="formPage" select="$optOut" />

    <script type="text/javascript">
      addIviewProperty('<xsl:value-of select="$groupID" />', 'headerObjects', '"<xsl:value-of select="$create" />"');
    </script>
    <div id="buttonSurface{$groupID}" class="buttonSurface{$idAdd} min">
      <xsl:choose>
        <xsl:when test="$idAdd = ''">
          <div id="header{$groupID}" class="header {$groupID}" onmousedown="return false;" />
        </xsl:when>
        <xsl:otherwise>
          <div class="BS_header {$groupID}" onmousedown="return false;" />
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$fullView = 'true'">
          <div class="BSE_fullView {$groupID}" onclick="maximizeHandler('{$groupID}')" title="{i18n:translate('component.iview2.fullView')}">
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$normalView = 'true'">
          <div class="BSE_normalView {$groupID}" onclick="maximizeHandler('{$groupID}')" title="{i18n:translate('component.iview2.normalView')}">
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$zoomIn = 'true'">
          <div class="BSE_zoomInBehind {$groupID}">
            <div class="BSE_zoomIn {$groupID}"
              onclick="Iview['{$groupID}'].viewerBean.zoom(1); if(Iview['{$groupID}'].useZoomBar) Iview['{$groupID}'].zoomBar.moveBarToLevel(Iview['{$groupID}'].viewerBean.zoomLevel);"
              title="{i18n:translate('component.iview2.zoomIn')}">
            </div>
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$zoomOut = 'true'">
          <div class="BSE_zoomOutBehind {$groupID}">
            <div class="BSE_zoomOut {$groupID}"
              onclick="Iview['{$groupID}'].viewerBean.zoom(-1); if(Iview['{$groupID}'].useZoomBar) Iview['{$groupID}'].zoomBar.moveBarToLevel(Iview['{$groupID}'].viewerBean.zoomLevel);"
              title="{i18n:translate('component.iview2.zoomOut')}">
            </div>
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$toWidth = 'true'">
          <div class="BSE_toWidth {$groupID}" onclick="pictureWidth('{$groupID}')" title="{i18n:translate('component.iview2.toWidth')}">
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$toScreen = 'true'">
          <div class="BSE_toScreen {$groupID}" onclick="pictureScreen('{$groupID}')" title="{i18n:translate('component.iview2.toScreen')}">
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$backward = 'true'">
          <div class="BSE_backwardBehind {$groupID}">
            <div class="BSE_backward {$groupID}" onclick="Iview['{$groupID}'].pagenumber--;navigatePage(Iview['{$groupID}'].pagenumber, '{$groupID}');" title="{i18n:translate('component.iview2.backward')}">
            </div>
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$forward = 'true'">
          <div class="BSE_forwardBehind {$groupID}">
            <div class="BSE_forward {$groupID}" onclick="Iview['{$groupID}'].pagenumber++;navigatePage(Iview['{$groupID}'].pagenumber, '{$groupID}');" title="{i18n:translate('component.iview2.forward')}">
            </div>
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$openThumbs = 'true'">
          <div class="BSE_openThumbs {$groupID}" onclick="openOverview('{$groupID}')" title="{i18n:translate('component.iview2.openThumbs')}">
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$chapterOpener = 'true'">
          <div class="BSE_chapterOpener {$groupID}" onclick="openChapter(true,'{$groupID}')" title="{i18n:translate('component.iview2.chapterOpener')}">
          </div>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$permalink = 'true'">
          <div class="BSE_permalink {$groupID}" onclick="displayURL(this, '{$groupID}');" title="{i18n:translate('component.iview2.permalink')}">
          </div>
          <div class="BSE_url {$groupID}">
            <input class="BSE_permaUrl {$groupID}" type="text" readonly="true" onclick="this.select();" />
            <div class="BSE_displayOut {$groupID}" onclick="hideURL(this);">
            </div>
          </div>
        </xsl:when>
      </xsl:choose>
      <!--
      <xsl:choose>
        <xsl:when test="$inputPage = 'true'">
          <script>
            importPageInput('<xsl:value-of select="$groupID" />', $("buttonSurface<xsl:value-of select="$groupID" />"));
          </script>
        </xsl:when>
      </xsl:choose>
-->
      <xsl:choose>
        <xsl:when test="$formPage = 'true'">
          <script>   
            importPageForm('<xsl:value-of select="$groupID" />', $("buttonSurface<xsl:value-of select="$groupID" />"));
          </script>
        </xsl:when>
      </xsl:choose>
    </div>

  </xsl:template>
  <xsl:template name="iview2.init">
    <xsl:param name="groupID" />
    <xsl:param name="prefix"/>
    <xsl:param name="tilesize" select="'256'" />
    <xsl:param name="maximized" select="'false'" />
    <xsl:param name="zoomWidth" select="'false'" />
    <xsl:param name="zoomScreen" select="'false'" />
    <xsl:param name="blendEffects" select="'true'" />
    <xsl:param name="chapHover" select="'true'" />
    <xsl:param name="chapHoverDelay" select="'100'" />
    <xsl:param name="chapHoverStep" select="'10'" />
    <script type="text/javascript">
    <!-- Philipp möchte verbessern -->
      <xsl:text>var prefix='</xsl:text>
      <xsl:value-of select="$prefix" />
      <xsl:text>';</xsl:text>
      <xsl:text>var tilesize='</xsl:text>
      <xsl:value-of select="$tilesize" />
      <xsl:text>';</xsl:text>
      <xsl:text>var maximized=</xsl:text>
      <xsl:value-of select="$maximized" />
      <xsl:text>;</xsl:text>
      <xsl:text>var zoomWidth=</xsl:text>
      <xsl:value-of select="$zoomWidth" />
      <xsl:text>;</xsl:text>
      <xsl:text>var zoomScreen='</xsl:text>
      <xsl:value-of select="$zoomScreen" />
      <xsl:text>';</xsl:text>
      <xsl:text>var blendEffects=</xsl:text>
      <xsl:value-of select="$blendEffects" />
      <xsl:text>;</xsl:text>
      <xsl:text>var chapHover=</xsl:text>
      <xsl:value-of select="$chapHover" />
      <xsl:text>;</xsl:text>
      <xsl:text>var chapHoverDelay='</xsl:text>
      <xsl:value-of select="$chapHoverDelay" />
      <xsl:text>';</xsl:text>
      <xsl:text>var chapHoverStep='</xsl:text>
      <xsl:value-of select="$chapHoverStep" />
      <xsl:text>';</xsl:text>
    </script>
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/ManageEvents.js" />
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/EventUtils.js" />
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/PanoJS.js" />
    <!-- Viewer -->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/XML.js" />
    <!--XML Funktionen-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/Utils.js" />
    <!--Allgemeine Util Funktionen-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/navigation.js" />
    <!--Navigation Functions-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/scrollBars.js" />
    <!--Scrollbar Klasse-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/blendWorks.js" />
    <!--Blend Funktionen-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/cutOut.js" />
    <!--Ausschnittbildchen Klasse-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/chapter.js" />
    <!--Chapter Klasse-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/overview.js" />
    <!--Overview Klasse-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/Thumbnail.js" />
    <!--Hauptdatei-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/zoomBar.js" />
    <!--ZoomBar-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/pageInput.js" />
    <!--PageInput-->
    <script type="text/javascript" language="JavaScript" src="{$WebApplicationBaseURL}modules/iview2/web/js/pageForm.js" />
    <!--PageForm-->
    <!-- params out of config.xml -->
    <script type="text/javascript">function startViewer(viewID) {
      if (Iview[viewID].started) return;
      Iview[viewID].started = true;
      //TODO: vorher 1000, jetz sporadischer Fehler: StyleFolderUri not found
      window.setTimeout("loading('"+viewID+"')", 100);
      }
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
    <xsl:param name="pagenumber" select="'1'" />
    <xsl:param name="mets_uri" select="'../../images/Pics/Mets.xml'" />
    <script type="text/javascript" src="{$WebApplicationBaseURL}/modules/iview2/web/js/init.js" onreadystatechange="startViewer('{$groupID}')"
      onload="startViewer('{$groupID}')" />

    <link id="cssSheet{$groupID}" rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}/modules/iview2/web/gfx/{$style}/style.css" />
    <!-- Initfunktionen -->
    <script type="text/javascript">
    <!-- Philipp möchte verbessern -->
      <xsl:text>var styleName='</xsl:text>
      <xsl:value-of select="$style" />
      <xsl:text>';</xsl:text>
      <xsl:text>var styleFolderUri='</xsl:text>
      <xsl:value-of select="$styleFolderUri" />
      <xsl:text>';</xsl:text>
      <xsl:text>var pagenumber='</xsl:text>
      <xsl:value-of select="$pagenumber" />
      <xsl:text>';</xsl:text>
      <xsl:text>var mets_uri='</xsl:text>
      <xsl:value-of select="$mets_uri" />
      <xsl:text>';</xsl:text>
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
          addIviewProperty('<xsl:value-of select="$groupID" />','chapterParent', '"viewer<xsl:value-of select="$groupID" />"');
        </script>
      </xsl:when>
      <xsl:when test="$parent = 'here'">
        <script type="text/javascript">
          addIviewProperty('<xsl:value-of select="$groupID" />','chapterParent','"chapterContainer<xsl:value-of select="$groupID" />"');
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
</xsl:stylesheet>