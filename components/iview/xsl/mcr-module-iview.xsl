<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================== -->
<!-- $Revision: 1.25 $ $Date: 2007-07-26 14:12:00 $ -->
<!--                                            -->
<!-- Image Viewer - MCR-IView 1.0, 05-2006          -->
<!-- +++++++++++++++++++++++++++++++++++++          -->
<!--                                                -->
<!-- Andreas Trappe     - concept, devel. in misc.  -->
<!-- Britta Kapitzki    - Design                    -->
<!-- Thomas Scheffler   - html prototype            -->
<!-- Stephan Schmidt    - html prototype            -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
    exclude-result-prefixes="xlink i18n mcrxml" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
    xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions">

    <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" />

    <xsl:param name="WebApplicationBaseURL" />
    <xsl:param name="RequestURL" />
    <xsl:param name="ServletsBaseURL" />
    <xsl:param name="DefaultLang" />
    <xsl:param name="CurrentLang" />
    <xsl:param name="JSessionID" />
    <xsl:param name="HttpSession" />

    <xsl:param name="MCR.Module-iview.defaultSort" />
    <xsl:param name="MCR.Module-iview.defaultSort.order" />
    <xsl:param name="MCR.Module-iview.thumbnail.size" />
    <xsl:param name="MCR.Module-iview.navi.zoom" />
    <xsl:param name="MCR.Module-iview.display" />
    <xsl:param name="MCR.Module-iview.style" />
    <xsl:param name="MCR.Module-iview.lastEmbeddedURL" />
    <xsl:param name="MCR.Module-iview.embedded" />
    <xsl:param name="MCR.Module-iview.scrollBars" />

    <!--params set manually-->
    <xsl:variable name="ownerID" select="/mcr-module/iview/content/ownerID" />
    <xsl:variable name="path" select="/mcr-module/iview/content/path" />
    <!--params set manually - navi -->
    <xsl:variable name="iview.home" select="concat($ServletsBaseURL,'MCRIViewServlet/')" />
    <xsl:variable name="fileToBeDisplayedPath">
        <xsl:call-template name="get.fileToBeDisplayedPath" />
    </xsl:variable>
    <xsl:variable name="fileToBeDisplayed">
        <xsl:call-template name="get.mcrModuleIview.substringAfter">
            <xsl:with-param name="mcrModuleIview.string" select="$fileToBeDisplayedPath" />
            <xsl:with-param name="mcrModuleIview.patternString" select="'/'" />
        </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="imageToBeDisplayedPath">
        <xsl:value-of select="concat($iview.home,$fileToBeDisplayedPath,$HttpSession,'?mode=getImage')" />
    </xsl:variable>
    <xsl:variable name="generateLayoutPath">
        <xsl:value-of select="concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout')" />
    </xsl:variable>
    <xsl:variable name="setMetadataURL">
        <xsl:value-of select="concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=setMetadata&amp;XSL.Style=xml')" />
    </xsl:variable>
    <xsl:variable name="setupJS">
        <xsl:call-template name="get.setupJS" />
    </xsl:variable>

    <xsl:variable name="fileToBeDisplayedPath.previous">
        <xsl:call-template name="get.fileToBeDisplayedPath.previous" />
    </xsl:variable>
    <xsl:variable name="fileToBeDisplayedPath.next">
        <xsl:call-template name="get.fileToBeDisplayedPath.next" />
    </xsl:variable>
    <xsl:variable name="fileToBeDisplayedPath.first">
        <xsl:call-template name="get.fileToBeDisplayedPath.first" />
    </xsl:variable>
    <xsl:variable name="fileToBeDisplayedPath.last">
        <xsl:call-template name="get.fileToBeDisplayedPath.last" />
    </xsl:variable>
    <xsl:variable name="nodeToBeDisplayedPath">
        <xsl:call-template name="get.nodeToBeDisplayedPath" />
    </xsl:variable>
    <xsl:variable name="currentNodePosition">
        <xsl:call-template name="get.currentNodePosition" />
    </xsl:variable>
    <xsl:variable name="currentZoom">
        <xsl:value-of select="/mcr-module/iview/header/currentZoom/text()" />
    </xsl:variable>

    <xsl:variable name="MCR.Module-iview.thumbnail.size.vertical" select="($MCR.Module-iview.thumbnail.size*3) div 4" />
    <xsl:variable name="quota">
        <xsl:text disable-output-escaping="yes">'</xsl:text>
    </xsl:variable>

    <!--  #####################################################################################################################-->
    <xsl:template match="/mcr-module">
        <html>
            <xsl:call-template name="viewer.head" />
            <xsl:call-template name="viewer.body2" />
        </html>
    </xsl:template>

    <!--  #####################################################################################################################-->
    <xsl:template name="viewer.head">
        <head>
            <title>MCR IView</title>
            <xsl:comment>
                <xsl:value-of select="concat('current language: ',$CurrentLang)" />
            </xsl:comment>

            <script src="{$WebApplicationBaseURL}modules/iview/web/JS/module-iview.js" type="text/javascript" />

            <link href="{$WebApplicationBaseURL}modules/iview/web/CSS/iviewstyle.css" rel="stylesheet" />

        </head>
    </xsl:template>

    <!--  #####################################################################################################################-->
    <xsl:template name="viewer.body2">

        <body style="direction: ltr;height:100%;">

            <table border="0" width="100%" cellpadding="0" cellspacing="0">

                <xsl:choose>
                    <xsl:when test="$MCR.Module-iview.display = 'minimal'">
                        <tr>
                            <td>
                                <table id="naviArea" class="outerMenu">
                                    <tr>
                                        <td colspan="4" id="iview-navigation">
                                            <xsl:call-template name="navi2" />
                                        </td>
                                    </tr>
                                </table>

                            </td>
                        </tr>
                    </xsl:when>
                    <xsl:when test="$MCR.Module-iview.display = 'extended' or $MCR.Module-iview.display = 'normal' ">
                        <tr>
                            <td>
                                <table id="naviArea" class="outerMenu">

                                    <tr id="iview-base">
                                        <td id="iview-roller">
                                            <div align="center">
                                                <xsl:if test="$MCR.Module-iview.display = 'normal'">
                                                    <a class="dummyview3" title="{i18n:translate('iview.advMenu')}"
                                                        href="{concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.display.SESSION=extended')}">
                                                        <xsl:text disable-output-escaping="yes">
                                                &amp;nbsp;</xsl:text>
                                                    </a>
                                                </xsl:if>

                                            </div>
                                        </td>
                                        <td id="iview-navigation">
                                            <xsl:call-template name="navi2" />
                                        </td>
                                        <td id="iview-simplesize">
                                            <table>
                                                <tr>
                                                    <td>
                                                        <a class="iview-simplesize" title="{i18n:translate('iview.adjustSide')}"
                                                            href="{concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.navi.zoom.SESSION=fitToScreen')}"
                                                            align="middle">
                                                            <xsl:text disable-output-escaping="yes">
                                                    &amp;nbsp;</xsl:text>
                                                        </a>
                                                    </td>
                                                    <td>
                                                        <a class="iview-simplewidth" title="{i18n:translate('iview.adjustWidth')}"
                                                            href="{concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.navi.zoom.SESSION=fitToWidth')}"
                                                            align="middle">
                                                            <xsl:text disable-output-escaping="yes">
                                                    &amp;nbsp;</xsl:text>
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                        <td>
                                            <table class="innerMenu" cellspacing="0" cellpadding="0">
                                                <td>
                                                    <table id="iview-filemenu" border="0" cellspacing="0" cellpadding="0">
                                                        <tr>
                                                            <td>
                                                                <xsl:choose>
                                                                    <xsl:when test="$MCR.Module-iview.scrollBars='true'">
                                                                        <a class="iview-scrolling"
                                                                            href="{concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.style.SESSION=image&amp;XSL.MCR.Module-iview.scrollBars.SESSION=false')}"
                                                                            align="middle">
                                                                            <xsl:value-of select="i18n:translate('iview.scrolling.switchOff')" />
                                                                        </a>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <a class="iview-scrolling"
                                                                            href="{concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.style.SESSION=image&amp;XSL.MCR.Module-iview.scrollBars.SESSION=true')}"
                                                                            align="middle">
                                                                            <xsl:value-of select="i18n:translate('iview.scrolling.switchOn')" />
                                                                        </a>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                                <td>
                                                    <table id="iview-displayswitch" cellspacing="0" cellpadding="0">
                                                        <tr>
                                                            <td>
                                                                <xsl:variable name="iviewDisplayswitchthumbs">
                                                                    <xsl:value-of
                                                                        select="concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.style.SESSION=thumbnails')" />
                                                                </xsl:variable>
                                                                <a class="iview-displayswitchthumbs" title="{i18n:translate('iview.thumbs')}"
                                                                    href="{$iviewDisplayswitchthumbs}" align="middle">
                                                                    <xsl:text disable-output-escaping="yes">
                                                            &amp;nbsp;</xsl:text>
                                                                </a>
                                                            </td>
                                                            <td>
                                                                <xsl:variable name="iviewDisplayswitchpicture">
                                                                    <xsl:value-of
                                                                        select="concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.style.SESSION=image')" />
                                                                </xsl:variable>
                                                                <a class="iview-displayswitchpicture" title="{i18n:translate('iview.viewPic')}"
                                                                    href="{$iviewDisplayswitchpicture}" align="middle">
                                                                    <xsl:text disable-output-escaping="yes">
                                                            &amp;nbsp;</xsl:text>
                                                                </a>
                                                            </td>
                                                            <td>
                                                                <xsl:variable name="iviewDisplayswitchtext">
                                                                    <xsl:value-of
                                                                        select="concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.style.SESSION=text')" />
                                                                </xsl:variable>
                                                                <a class="iview-displayswitchtext" title="{i18n:translate('iview.overview')}"
                                                                    href="{$iviewDisplayswitchtext}" align="middle">
                                                                    <xsl:text disable-output-escaping="yes">
                                                            &amp;nbsp;</xsl:text>
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>

                                                <td>
                                                    <table id="iview-viewoptions" border="0" cellspacing="0" cellpadding="0">
                                                        <tr>
                                                            <td>
                                                                <xsl:choose>
                                                                    <xsl:when test="$MCR.Module-iview.embedded='true'">
                                                                        <a class="iview-viewoptionssmall"
                                                                            title="{concat(i18n:translate('iview.scaleDown'),i18n:translate('iview.inactive'))}"
                                                                            href="#" align="middle">
                                                                            <xsl:text disable-output-escaping="yes">
                                                                    &amp;nbsp;</xsl:text>
                                                                        </a>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <a class="iview-viewoptionssmall" title="{i18n:translate('iview.scaleDown')}"
                                                                            href="{$MCR.Module-iview.lastEmbeddedURL}" align="middle">
                                                                            <xsl:text disable-output-escaping="yes">
                                                                    &amp;nbsp;</xsl:text>
                                                                        </a>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </td>
                                                            <td>
                                                                <xsl:choose>
                                                                    <xsl:when test="$MCR.Module-iview.embedded='true'">
                                                                        <a class="iview-viewoptionslarg" title="{i18n:translate('iview.scaleUp')}"
                                                                            href="{concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.embedded.SESSION=false&amp;XSL.MCR.Module-iview.display.SESSION=extended')}"
                                                                            align="middle" target="_parent">
                                                                            <xsl:text disable-output-escaping="yes">
                                                                    &amp;nbsp;</xsl:text>
                                                                        </a>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <a class="iview-viewoptionslarg"
                                                                            title="{concat(i18n:translate('iview.scaleUp'),i18n:translate('iview.inactive'))}"
                                                                            href="#" align="middle">
                                                                            <xsl:text disable-output-escaping="yes">
                                                                    &amp;nbsp;</xsl:text>
                                                                        </a>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </table>
                                        </td>
                                    </tr>

                                    <xsl:if test="$MCR.Module-iview.display = 'extended' ">
                                        <tr id="iview-enhanced">
                                            <td id="iview-smalllogo">
                                                <div align="center">
                                                    <a class="dummyview2" title="{i18n:translate('iview.hideAdvMenu')}"
                                                        href="{concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.display.SESSION=normal')}"
                                                        align="middle">
                                                        <xsl:text disable-output-escaping="yes">
                                                &amp;nbsp;</xsl:text>
                                                    </a>
                                                </div>
                                            </td>
                                            <td id="iview-sorting">
                                                <xsl:call-template name="areaAbove.sort" />
                                            </td>
                                            <td id="iview-resize">
                                                <xsl:call-template name="areaAbove.zooming" />
                                            </td>
                                            <td>

                                                <table class="innerMenu" cellspacing="0" cellpadding="0">

                                                    <tr>
                                                        <td id="iview-orientation">
                                                            <xsl:if test="$MCR.Module-iview.scrollBars='false'">
                                                                <xsl:call-template name="areaAbove.orientation" />
                                                            </xsl:if>
                                                        </td>
                                                        <td id="iview-fileproperties">
                                                            <xsl:call-template name="prepareImageInfo" />
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td colspan="4" id="iview-baseline"></td>
                                        </tr>
                                    </xsl:if>
                                </table>
                            </td>
                        </tr>
                    </xsl:when>
                </xsl:choose>

                <tr>
                    <td id="iview-content">
                        <xsl:call-template name="content" />
                    </td>
                </tr>
                <xsl:variable name="dragURL">
                    <xsl:value-of select="concat($generateLayoutPath,'&amp;XSL.MCR.Module-iview.move=draged')" />
                </xsl:variable>
                <form name="dragImage" action="{$dragURL}" method="post">
                    <input type="hidden" id="dragX" name="XSL.MCR.Module-iview.move.distanceX" value="" />
                    <input type="hidden" id="dragY" name="XSL.MCR.Module-iview.move.distanceY" value="" />
                </form>
            </table>
            <script type="text/javascript">
                <xsl:value-of select="$setupJS" />
            </script>
        </body>
    </xsl:template>
    <!--  #####################################################################################################################-->
    <xsl:template name="areaAbove.orientation">

        <table>

            <tr>
                <td colspan="3" class="h-nav">
                    <div align="center">
                        <xsl:variable name="move">
                            <xsl:value-of select="concat($generateLayoutPath,'&amp;XSL.MCR.Module-iview.move=up')" />
                        </xsl:variable>
                        <a id="iview-orientationupID" class="iview-orientationup" title="nach oben bewegen" href="{$move}">
                            <xsl:text disable-output-escaping="yes">
                                                                        &amp;nbsp;</xsl:text>
                        </a>
                    </div>
                </td>
            </tr>
            <tr>
                <td class="v-nav">
                    <xsl:variable name="move">
                        <xsl:value-of select="concat($generateLayoutPath,'&amp;XSL.MCR.Module-iview.move=left')" />
                    </xsl:variable>
                    <a id="iview-orientationleftID" class="iview-orientationleft" title="nach links bewegen" href="{$move}" align="middle">
                        <xsl:text disable-output-escaping="yes">
                                                                    &amp;nbsp;</xsl:text>
                    </a>
                </td>
                <td class="thumbnail">
                    <xsl:variable name="thumbWidth">
                        <xsl:value-of select="/mcr-module/iview/header/thumbWidth/text()" />
                    </xsl:variable>
                    <xsl:variable name="thumbHeight">
                        <xsl:value-of select="/mcr-module/iview/header/thumbHeight/text()" />
                    </xsl:variable>
                    <xsl:variable name="thumbX">
                        <xsl:value-of select="/mcr-module/iview/header/thumbHighLighting-X/text()" />
                    </xsl:variable>
                    <xsl:variable name="thumbY">
                        <xsl:value-of select="/mcr-module/iview/header/thumbHighLighting-Y/text()" />
                    </xsl:variable>
                    <div
                        style="overflow: hidden; position:relative;width:{$thumbWidth}px;height:{$thumbHeight}px;background-image:url({concat($iview.home,mcrxml:regexp(string($nodeToBeDisplayedPath), ' ', '%20'),$HttpSession,'?mode=getImage&amp;XSL.MCR.Module-iview.navi.zoom=thumbnail')});">
                        <div id="previewImage">
                            <div id="previewImage_inner" />
                        </div>
                    </div>
                </td>
                <td class="v-nav">
                    <xsl:variable name="move">
                        <xsl:value-of select="concat($generateLayoutPath,'&amp;XSL.MCR.Module-iview.move=right')" />
                    </xsl:variable>
                    <a id="iview-orientationrightID" class="iview-orientationright" title="nach rechts bewegen" href="{$move}" align="middle">
                        <xsl:text disable-output-escaping="yes">
                                                                    &amp;nbsp;</xsl:text>
                    </a>
                </td>
            </tr>
            <tr>
                <td colspan="3" class="h-nav">
                    <div align="center">
                        <xsl:variable name="move">
                            <xsl:value-of select="concat($generateLayoutPath,'&amp;XSL.MCR.Module-iview.move=down')" />
                        </xsl:variable>
                        <a id="iview-orientationdownID" class="iview-orientationdown" title="nach unten bewegen" href="{$move}" align="middle">
                            <xsl:text disable-output-escaping="yes">
                                                                        &amp;nbsp;</xsl:text>
                        </a>
                    </div>
                </td>
            </tr>

        </table>

    </xsl:template>

    <xsl:template name="get.setupJS">

        <xsl:choose>
            <xsl:when test="$MCR.Module-iview.style='image'">
                <xsl:variable name="origWidth">
                    <xsl:value-of select="/mcr-module/iview/header/origWidth/text()" />
                </xsl:variable>
                <xsl:variable name="origHeight">
                    <xsl:value-of select="/mcr-module/iview/header/origHeight/text()" />
                </xsl:variable>
                <xsl:variable name="thumbWidth">
                    <xsl:value-of select="/mcr-module/iview/header/thumbWidth/text()" />
                </xsl:variable>
                <xsl:variable name="thumbHeight">
                    <xsl:value-of select="/mcr-module/iview/header/thumbHeight/text()" />
                </xsl:variable>
                <xsl:variable name="zoom">
                    <xsl:value-of select="/mcr-module/iview/header/currentZoom/text()" />
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$MCR.Module-iview.display='extended' and $currentZoom!='fitToScreen' and $MCR.Module-iview.scrollBars!='true'">
                        <xsl:variable name="sf">
                            <xsl:value-of select="/mcr-module/iview/header/thumbHighLighting-SF/text()" />
                        </xsl:variable>
                        <xsl:variable name="x">
                            <xsl:value-of select="/mcr-module/iview/header/thumbHighLighting-X/text()" />
                        </xsl:variable>
                        <xsl:variable name="y">
                            <xsl:value-of select="/mcr-module/iview/header/thumbHighLighting-Y/text()" />
                        </xsl:variable>
                        <xsl:value-of
                            select="concat('setupContentArea();setupImage(&#34;',$setMetadataURL,'&#34;,&#34;',$imageToBeDisplayedPath,'&#34;,&#34;',$zoom,'&#34;,&#34;',$origWidth,'&#34;,&#34;',$origHeight,'&#34;);setupHighlight(&#34;',$x,'&#34;,&#34;',$y,'&#34;,&#34;',$sf,'&#34;,&#34;',$zoom,'&#34;,&#34;',$thumbWidth,'&#34;,&#34;',$thumbHeight,'&#34;);')" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of
                            select="concat('setupContentArea();setupImage(&#34;',$setMetadataURL,'&#34;,&#34;',$imageToBeDisplayedPath,'&#34;,&#34;',$zoom,'&#34;,&#34;',$origWidth,'&#34;,&#34;',$origHeight,'&#34;);')" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'setupContentArea();'" />
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <!--  #####################################################################################################################-->
    <xsl:template name="content">
        <div id="content-container">
            <xsl:attribute name="style">
                <xsl:choose>
                    <xsl:when test="$MCR.Module-iview.scrollBars = 'true'">
                        <xsl:value-of select="'overflow:scroll;'" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'overflow:hidden;'" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="$MCR.Module-iview.style != 'image'">
                <xsl:call-template name="thumbnails" />
            </xsl:if>
        </div>
    </xsl:template>
    <!--  #####################################################################################################################-->
    <xsl:template name="prepareImageInfo">
        <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
            <xsl:if test="contains($fileToBeDisplayedPath,name)">
                <xsl:call-template name="image.info">
                    <xsl:with-param name="node" select="node()" />
                </xsl:call-template>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <!--  #####################################################################################################################-->
    <xsl:template name="image.info">
        <xsl:param name="node" />
        <p>
            <xsl:value-of select="concat(i18n:translate('iview.name'),':')" />
            <xsl:value-of select="name" />
            <br />
            <xsl:value-of select="concat(i18n:translate('iview.fileSize'),':')" />
            <xsl:value-of select="size" />
            <xsl:value-of select="concat(i18n:translate('iview.bytes'),':')" />
            <br />
            <xsl:value-of select="concat(i18n:translate('iview.format'),':')" />
            <xsl:value-of select="contentType" />

            <br />
            <xsl:value-of select="concat(i18n:translate('iview.imageSize'),':')" />
            <xsl:value-of select="/mcr-module/iview/header/origWidth" />
            x
            <xsl:value-of select="/mcr-module/iview/header/origHeight" />
            px
            <br />
            <xsl:value-of select="concat(i18n:translate('iview.lastChanged'),':')" />
            <xsl:value-of select="date" />
        </p>

    </xsl:template>
    <!--  #####################################################################################################################-->
    <xsl:template name="areaAbove.sort">
        <xsl:variable name="targetURL">
            <xsl:value-of select="concat($iview.home,$nodeToBeDisplayedPath,$HttpSession,'?mode=generateLayout')" />
        </xsl:variable>
        <form action="{$targetURL}" method="post">
            <select name="XSL.MCR.Module-iview.defaultSort.SESSION">
                <!--            <select name="XSL.MCR.Module-iview.defaultSort">                -->
                <xsl:choose>
                    <xsl:when test="$MCR.Module-iview.defaultSort = 'name'">
                        <option value="name" selected="selected">
                            <xsl:value-of select="concat(i18n:translate('iview.name'),' ',i18n:translate('iview.current'))" />
                        </option>
                    </xsl:when>
                    <xsl:otherwise>
                        <option value="name">
                            <xsl:value-of select="i18n:translate('iview.name')" />
                        </option>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:choose>
                    <xsl:when test="$MCR.Module-iview.defaultSort = 'size'">
                        <option value="size" selected="selected">
                            <xsl:value-of select="concat(i18n:translate('iview.fileSize'),' ',i18n:translate('iview.current'))" />
                        </option>
                    </xsl:when>
                    <xsl:otherwise>
                        <option value="size">
                            <xsl:value-of select="i18n:translate('iview.fileSize')" />
                        </option>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:choose>
                    <xsl:when test="$MCR.Module-iview.defaultSort = 'lastModified'">
                        <option value="lastModified" selected="selected">
                            <xsl:value-of select="concat(i18n:translate('iview.lastChanged'),' ',i18n:translate('iview.current'))" />
                        </option>
                    </xsl:when>
                    <xsl:otherwise>
                        <option value="lastModified">
                            <xsl:value-of select="i18n:translate('iview.lastChanged')" />
                        </option>
                    </xsl:otherwise>
                </xsl:choose>
            </select>
            <input type="submit" value="&gt;" />
            <!--            <select name="XSL.MCR.Module-iview.defaultSort.order">-->
            <select name="XSL.MCR.Module-iview.defaultSort.order.SESSION">
                <xsl:choose>
                    <xsl:when test="$MCR.Module-iview.defaultSort.order = 'ascending'">
                        <option value="ascending" selected="selected">
                            <xsl:value-of select="concat(i18n:translate('iview.ascending'),' ',i18n:translate('iview.current'))" />
                        </option>
                        <option value="descending">
                            <xsl:value-of select="i18n:translate('iview.descending')" />
                        </option>
                    </xsl:when>
                    <xsl:otherwise>
                        <option value="ascending">
                            <xsl:value-of select="i18n:translate('iview.ascending')" />
                        </option>
                        <option value="descending" selected="selected">
                            <xsl:value-of select="concat(i18n:translate('iview.descending'),' ',i18n:translate('iview.current'))" />
                        </option>
                    </xsl:otherwise>
                </xsl:choose>
            </select>
        </form>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="areaAbove.zooming">

        <table Cleaned="text-align: left; width: 100%; height: 25px;" align="center" border="0" cellpadding="0" cellspacing="0">
            <tr>
                <td>
                    <a align="middle"
                        href="{concat($iview.home,$fileToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.navi.zoom=-0.2')}"
                        title="{i18n:translate('iview.zoomOut')}" class="iview-resizeout">
                        <xsl:text disable-output-escaping="yes">
                            &amp;nbsp;</xsl:text>
                    </a>
                </td>
                <td>
                    <a align="middle"
                        href="{concat($iview.home,$fileToBeDisplayedPath,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.navi.zoom=+0.2')}"
                        title="{i18n:translate('iview.zoomIn')}" class="iview-resizein">
                        <xsl:text disable-output-escaping="yes">
                            &amp;nbsp;</xsl:text>
                    </a>
                </td>
            </tr>
            <tr>
                <td colspan="2">
                    <form id="zoomLevel" action="{concat($iview.home,$fileToBeDisplayedPath,$HttpSession,'?mode=generateLayout')}" method="post">
                        <select onChange="document.getElementById('zoomLevel').submit()" name="XSL.MCR.Module-iview.navi.zoom.SESSION">
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'fitToScreen'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'fitToWidth'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'1.0'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.9'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.8'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.7'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.6'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.5'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.4'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.3'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.2'" />
                            </xsl:call-template>
                            <xsl:call-template name="areaAbove.zooming.preselect">
                                <xsl:with-param name="zoom" select="'0.1'" />
                            </xsl:call-template>
                        </select>
                    </form>
                </td>
            </tr>
        </table>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="areaAbove.zooming.preselect">
        <xsl:param name="zoom" />
        <xsl:choose>
            <xsl:when test="$zoom = /mcr-module/iview/header/currentZoom">
                <xsl:choose>
                    <xsl:when test="$zoom = 'fitToScreen'">
                        <option selected="selected" value="{$zoom}">
                            <xsl:value-of select="i18n:translate('iview.side')" />
                        </option>
                    </xsl:when>
                    <xsl:when test="$zoom = 'fitToWidth'">
                        <option selected="selected" value="{$zoom}">
                            <xsl:value-of select="i18n:translate('iview.width')" />
                        </option>
                    </xsl:when>
                    <xsl:otherwise>
                        <option selected="selected" value="{$zoom}">
                            <xsl:value-of select="$zoom" />
                        </option>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$zoom = 'fitToScreen'">
                        <option value="{$zoom}">
                            <xsl:value-of select="i18n:translate('iview.side')" />
                        </option>
                    </xsl:when>
                    <xsl:when test="$zoom = 'fitToWidth'">
                        <option value="{$zoom}">
                            <xsl:value-of select="i18n:translate('iview.width')" />
                        </option>
                    </xsl:when>
                    <xsl:otherwise>
                        <option value="{$zoom}">
                            <xsl:value-of select="$zoom" />
                        </option>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!--  #################################################################################################################### -->
    <xsl:template name="navi2">
        <table>
            <tr>
                <td>
                    <!-- first -->
                    <xsl:choose>
                        <xsl:when test="$fileToBeDisplayedPath.first != ''">
                            <xsl:variable name="fileToBeDisplayedPath.first.link">
                                <xsl:value-of
                                    select="concat($iview.home,$fileToBeDisplayedPath.first,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.move=reset')" />
                            </xsl:variable>
                            <a class="iview-navigationfirst" title="{i18n:translate('iview.firstPic')}" href="{$fileToBeDisplayedPath.first.link}"
                                align="middle">
                                <xsl:text disable-output-escaping="yes">
                                    &amp;nbsp;</xsl:text>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text disable-output-escaping="yes">
                                &amp;nbsp;</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
                <td>
                    <!--previous-->
                    <xsl:choose>
                        <xsl:when test="$fileToBeDisplayedPath.previous != ''">
                            <xsl:variable name="fileToBeDisplayedPath.previous.link">
                                <xsl:value-of
                                    select="concat($iview.home,$fileToBeDisplayedPath.previous,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.move=reset')" />
                            </xsl:variable>
                            <a class="iview-navigationback" title="{i18n:translate('iview.prevPic')}" href="{$fileToBeDisplayedPath.previous.link}"
                                align="middle">
                                <xsl:text disable-output-escaping="yes">
                                    &amp;nbsp;</xsl:text>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text disable-output-escaping="yes">
                                &amp;nbsp;</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
                <td>
                    <xsl:call-template name="imageSwitcher" />
                </td>
                <td>
                    <!--next-->
                    <xsl:choose>
                        <xsl:when test="$fileToBeDisplayedPath.next != ''">
                            <xsl:variable name="fileToBeDisplayedPath.next.link">
                                <xsl:value-of
                                    select="concat($iview.home,$fileToBeDisplayedPath.next,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.move=reset')" />
                            </xsl:variable>
                            <a class="iview-navigationforward" title="{i18n:translate('iview.nextPic')}" href="{$fileToBeDisplayedPath.next.link}"
                                align="middle">
                                <xsl:text disable-output-escaping="yes">
                                    &amp;nbsp;</xsl:text>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text disable-output-escaping="yes">
                                &amp;nbsp;</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
                <td>
                    <!-- last -->
                    <xsl:choose>
                        <xsl:when test="$fileToBeDisplayedPath.last != ''">
                            <xsl:variable name="fileToBeDisplayedPath.last.link">
                                <xsl:value-of
                                    select="concat($iview.home,$fileToBeDisplayedPath.last,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.move=reset')" />
                            </xsl:variable>
                            <a class="iview-navigationlast" title="{i18n:translate('iview.lastPic')}" href="{$fileToBeDisplayedPath.last.link}"
                                align="middle">
                                <xsl:text disable-output-escaping="yes">
                                    &amp;nbsp;</xsl:text>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text disable-output-escaping="yes">
                                &amp;nbsp;</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>
        </table>
    </xsl:template>

    <!--  #####################################################################################################################-->

    <xsl:template name="imageSwitcher">
        <xsl:variable name="switchURLPrefix">
            <xsl:value-of select="concat($iview.home,$path)" />
        </xsl:variable>
        <form id="imageSwitcher" action="" method="post">
            <input type="hidden" name="mode" value="generateLayout" />
            <input type="hidden" name="XSL.MCR.Module-iview.move" value="reset" />
            <p>
                <select size="1" onChange="switchImage(this.value)">
                    <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
                        <option value="{$switchURLPrefix}/{name/text()}">
                            <xsl:choose>
                                <xsl:when test="$currentNodePosition+1 = position()">
                                    <xsl:attribute name="selected">selected</xsl:attribute>
                                    <xsl:value-of select="concat(position(),'/',count(/mcr-module/iview/content/nodes/node[@type='file']))" />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="position()" />
                                </xsl:otherwise>
                            </xsl:choose>
                        </option>
                    </xsl:for-each>
                </select>
            </p>
        </form>
    </xsl:template>

    <!--  #####################################################################################################################-->

    <xsl:template name="thumbnails">
        <table border="0">
            <tr>
                <td align="center">
                    <!--if there is a parent directory display link to it-->
                    <xsl:if test="/mcr-module/iview/content/parent != ''">
                        <a href="{concat($iview.home,iview/content/parent,$HttpSession,'?mode=generateLayout')}">
                            <xsl:value-of select="i18n:translate('iview.parentDir')" />
                        </a>
                        <br />
                    </xsl:if>
                    <!--generate thumbnail list-->
                    <!--generate thumbnail list __ dirs -->
                    <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='directory']">
                        <xsl:sort select="name" order="ascending" />
                        <xsl:variable name="currentNode">
                            <xsl:value-of select="concat($iview.home,$path,'/',name)" />
                        </xsl:variable>
                        <a href="{concat($currentNode,$HttpSession,'?mode=generateLayout')}">
                            <xsl:value-of select="name" />
                        </a>
                        <br />
                    </xsl:for-each>
                    <!--generate thumbnail list __ files -->
                    <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
                        <xsl:variable name="currentNode">
                            <xsl:value-of select="concat($iview.home,$path,'/',name)" />
                        </xsl:variable>
                        <a
                            href="{concat($currentNode,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.style.SESSION=image&amp;XSL.MCR.Module-iview.move=reset' )}">
                            <xsl:choose>
                                <xsl:when test="$MCR.Module-iview.style='thumbnails'">
                                    <img style="border-style:none;" src="{concat($currentNode,'?mode=getImage&amp;XSL.MCR.Module-iview.navi.zoom=thumbnail')}" />
                                </xsl:when>
                                <xsl:otherwise>
                                    <p align="left">
                                        <xsl:call-template name="image.info">
                                            <xsl:with-param name="node" select="node()" />
                                        </xsl:call-template>
                                    </p>
                                </xsl:otherwise>
                            </xsl:choose>
                        </a>
                        <xsl:text disable-output-escaping="yes">
                            &amp;nbsp;&amp;nbsp;</xsl:text>

                    </xsl:for-each>
                </td>
            </tr>
        </table>
    </xsl:template>

    <!--  #####################################################################################################################-->
    <xsl:template name="get.fileToBeDisplayedPath">
        <xsl:choose>
            <!--fileToBeDisplayed contained in xml-->
            <xsl:when test="/mcr-module/iview/content/fileToBeDisplayed != ''">
                <xsl:value-of select="concat ($ownerID,/mcr-module/iview/content/fileToBeDisplayed) " />
            </xsl:when>


            <!--fileToBeDisplayed NOT contained in xml & node[@type=file] found -->
            <xsl:when test="(/mcr-module/iview/content/nodes/node[@type='file']/name != '')">
                <xsl:value-of select="concat($path,'/',/mcr-module/iview/content/nodes/node[@type='file']/name) " />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="''" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="get.fileToBeDisplayedPath.previous">
        <xsl:if test="($fileToBeDisplayedPath != '')">
            <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
                <xsl:if test="($fileToBeDisplayed = name/text()) and (preceding-sibling::node()[@type='file']/name != '') ">
                    <xsl:for-each select="preceding-sibling::node()[@type='file']">
                        <xsl:if test="position()=last()">
                            <xsl:value-of select="concat($path,'/',name)" />
                        </xsl:if>
                    </xsl:for-each>
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="get.fileToBeDisplayedPath.next">
        <xsl:if test="($fileToBeDisplayedPath != '')">
            <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
                <xsl:if test="($fileToBeDisplayed = name/text()) and (following-sibling::node()[@type='file']/name/text() != '')">
                    <xsl:value-of select="concat($path,'/',following-sibling::node()[@type='file']/name/text())" />
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="get.fileToBeDisplayedPath.first">
        <xsl:if test="($fileToBeDisplayedPath != '')">
            <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
                <xsl:if test="($fileToBeDisplayed = name/text()) and (preceding-sibling::node()[@type='file']/name != '')">
                    <xsl:value-of select="concat($path,'/',/mcr-module/iview/content/nodes/node[@type='file' and position()=1]/name)" />
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="get.fileToBeDisplayedPath.last">
        <xsl:if test="($fileToBeDisplayedPath != '')">
            <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
                <xsl:if test="($fileToBeDisplayed = name/text()) and (following-sibling::node()[@type='file']/name != '')">
                    <xsl:value-of select="concat($path,'/',/mcr-module/iview/content/nodes/node[@type='file' and (position() = last())]/name/text())" />
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="get.nodeToBeDisplayedPath">
        <xsl:choose>
            <xsl:when test="$fileToBeDisplayedPath != ''">
                <xsl:value-of select="$fileToBeDisplayedPath" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$path" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="get.currentNodePosition">
        <xsl:if test="($fileToBeDisplayedPath != '')">
            <xsl:for-each select="/mcr-module/iview/content/nodes/node[@type='file']">
                <!-- <xsl:if test="contains($fileToBeDisplayedPath, name/text())"> -->
                <xsl:if test="$fileToBeDisplayed = name/text()">
                    <xsl:value-of select="count(preceding-sibling::node()[@type='file'])" />
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>


    <!--  #####################################################################################################################-->
    <xsl:template name="get.mcrModuleIview.substringAfter">
        <xsl:param name="mcrModuleIview.string" />
        <xsl:param name="mcrModuleIview.patternString" />

        <xsl:choose>
            <xsl:when test="contains($mcrModuleIview.string, $mcrModuleIview.patternString)">
                <xsl:call-template name="get.mcrModuleIview.substringAfter">
                    <xsl:with-param name="mcrModuleIview.string" select="substring-after($mcrModuleIview.string, $mcrModuleIview.patternString)" />
                    <xsl:with-param name="mcrModuleIview.patternString" select="$mcrModuleIview.patternString" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$mcrModuleIview.string" />
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <!--  #####################################################################################################################-->

</xsl:stylesheet>













