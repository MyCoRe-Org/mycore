<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xalan="http://xml.apache.org/xslt"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
  xmlns:mcrxslext="http://xml.apache.org/xalan/java/org.mycore.common.xsl.extensions" version="1.0" exclude-result-prefixes="xlink i18n mcrxml iview2 mcrxslext xalan">
  <xsl:param name="MCR.Module-iview2.BaseURL" />
  <xsl:param name="MCR.Module-iview2.DeveloperMode" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorURI" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorStyle" />
  <xsl:param name="MCR.Module-iview2.LoadJQueryUI" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />

  <xsl:variable name="initCalls" select="mcrxslext:Counter.new()" />

  <xsl:variable name="jqueryUI.version" select="'1.9.2'" />
  <xsl:variable name="loadJQueryUI" select="$MCR.Module-iview2.LoadJQueryUI" />

  <xsl:template name="iview2.getViewer" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="extensions" select="''" />
    <xsl:param name="style" />

    <div class="viewerContainer min">
      <xsl:if test="string-length($style) &gt; 0">
        <xsl:attribute name="style">
          <xsl:value-of select="$style" />
        </xsl:attribute>
      </xsl:if>
      <div class="viewer" onmousedown="return false;">
        <div class="surface" style="width:100%;height:100%;z-index:30" />
        <div class="iview_well">
        </div>
      </div>
      <script type="text/javascript">
        (function(){
        "use strict";
        var nodes=document.getElementsByTagName('script');
        var currentNode=nodes[nodes.length-1];
        iview.addInstance(new iview.IViewInstance(jQuery(currentNode.parentNode),
        <xsl:value-of select="iview2:getOptions($groupID, $extensions)" />
        ));
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

    <xsl:variable name="debugMode">
      <xsl:variable name="parValue">
        <xsl:call-template name="UrlGetParam">
          <xsl:with-param name="url" select="$RequestURL" />
          <xsl:with-param name="par" select="'iview2.debug'" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="string-length($parValue)&gt;0">
          <xsl:value-of select="$parValue" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$MCR.Module-iview2.DeveloperMode" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:if test="mcrxslext:Counter.next($initCalls)=1">

      <xsl:if test="$loadJQueryUI = 'true'">
        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jqueryui/{$jqueryUI.version}/jquery-ui.min.js" />
      </xsl:if>

      <xsl:choose>
        <xsl:when test="$debugMode='true'">
          <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview2.js" />
        </xsl:when>
        <xsl:otherwise>
          <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview2.min.js" />
        </xsl:otherwise>
      </xsl:choose>
      <script type="text/javascript">
        <xsl:text>var styleFolderUri='</xsl:text>
        <xsl:value-of select="$styleFolderUri" />
        <xsl:text>';</xsl:text>
        <xsl:text>var chapterEmbedded=</xsl:text>
        <xsl:value-of select="$chapterEmbedded" />
        <xsl:text>;</xsl:text>
        <xsl:text>var chapDynResize=</xsl:text>
        <xsl:value-of select="$chapDynResize" />
        <xsl:text>;</xsl:text>
        <xsl:text>var DampInViewer=</xsl:text>
        <xsl:value-of select="$DampInViewer" />
        <xsl:text>;</xsl:text>
        <xsl:text>var i18n = i18n || new iview.i18n('</xsl:text>
        <xsl:value-of select="$WebApplicationBaseURL" />
        <xsl:text>', '</xsl:text>
        <xsl:value-of select="$CurrentLang" />
        <xsl:text>', 'component.iview2');</xsl:text>
        <xsl:text>loadCssFile('</xsl:text>
        <xsl:value-of select="$WebApplicationBaseURL" />
        <xsl:choose>
          <xsl:when test="$debugMode='true'">
            <xsl:text>modules/iview2/gfx/default/iview2.css', 'iviewCss');</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>modules/iview2/gfx/default/iview2.min.css', 'iviewCss');</xsl:text>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:if test="$loadJQueryUI = 'true'">
          <xsl:text>loadCssFile('http://ajax.googleapis.com/ajax/libs/jqueryui/</xsl:text>
          <xsl:value-of select="$jqueryUI.version" />
          <xsl:text>/themes/base/jquery-ui.css');</xsl:text>
        </xsl:if>
      </script>
    </xsl:if>
  </xsl:template>

  <xsl:template name="iview2.start">
    <xsl:param name="groupID" />
    <xsl:param name="startFile" />

    <!-- Initfunktionen -->
    <script type="text/javascript">
      <xsl:text>(function(){var instList=Iview['</xsl:text>
      <xsl:value-of select="$groupID" />
      <xsl:text>'];</xsl:text>
      <xsl:text>var cI=instList[instList.length-1];
      jQuery(document).ready(function(){
      cI.startViewer('</xsl:text>
      <xsl:value-of select="$startFile" />
      <xsl:text>');});})();</xsl:text>
    </script>
  </xsl:template>

  <xsl:template name="iview2.getImageElement">
    <xsl:param name="derivate" />
    <xsl:param name="imagePath" />
    <xsl:param name="style" select="''" />
    <xsl:param name="class" select="''" />
    <img src="{concat($WebApplicationBaseURL,'servlets/MCRTileCombineServlet/THUMBNAIL/',$derivate,$imagePath)}" style="{$style}" class="{$class}" />
  </xsl:template>

</xsl:stylesheet>