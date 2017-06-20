<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager">
  <xsl:output method="html" indent="yes" doctype-public="-//IETF//DTD HTML 5.0//EN" />

  <xsl:param name="RequestURL" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="Referer" />
  <xsl:param name="returnUrl" select="$Referer" />

  <xsl:variable name="dojoV" select="'1.6.2'" />

  <xsl:template match="/wcms2">
    <xsl:variable name="resourcesPath" select="concat($WebApplicationBaseURL, 'modules/wcms2/resources')" />
    <xsl:variable name="jsPath" select="concat($WebApplicationBaseURL, 'modules/wcms2/js')" />
    <xsl:variable name="imgPath" select="concat($WebApplicationBaseURL, 'modules/wcms2/images')" />
    <xsl:variable name="cssPath" select="concat($WebApplicationBaseURL, 'modules/wcms2/css')" />
    <xsl:variable name="wcmsBasePath" select="concat($WebApplicationBaseURL, 'modules/wcms2')" />

    <html dir="ltr">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>MyCoRe WCMS2</title>

        <script>
          var wcms = wcms || {
            settings: {
              baseURL: "<xsl:value-of select='$WebApplicationBaseURL' />",
              wcmsURL: "<xsl:value-of select='$WebApplicationBaseURL' />" + "rsc/wcms"
            }
          };
          var webApplicationBaseURL = "<xsl:value-of select='$WebApplicationBaseURL' />";
          var imagePath = "<xsl:value-of select='$imgPath' />";
          var resourcesPath = "<xsl:value-of select='$resourcesPath' />";
          var returnUrl = "<xsl:value-of select='$returnUrl' />";
          wcms.startLang= "<xsl:value-of select='$CurrentLang' />";
          var langArr = new Array("de", "en");

          djConfig = {
            isDebug: true,
            parseOnLoad: true,
            baseUrl: webApplicationBaseURL + "modules/wcms2/node_modules/dojo/",
            modulePaths: {
            "dojoclasses": "../../js/dojoclasses"
            },
            xdWaitSeconds: 10
          };
        </script>

        <!-- do includes -->
        <script type="text/javascript" src="{$wcmsBasePath}/node_modules/dojo/dojo.js"></script>

        <script type="text/javascript" src="{$jsPath}/dojoInclude.js"></script>
        <!-- util -->
        <script type="text/javascript" src="{$jsPath}/util/Hashtable.js"></script>
        <script type="text/javascript" src="{$jsPath}/util/WCMSUtils.js"></script>
        <script type="text/javascript" src="{$jsPath}/util/ErrorUtils.js"></script>
        <!-- common -->
        <script type="text/javascript" src="{$jsPath}/common/MenuBuilder.js"></script>
        <script type="text/javascript" src="{$jsPath}/common/EventHandler.js"></script>
        <script type="text/javascript" src="{$jsPath}/common/Preloader.js"></script>
        <script type="text/javascript" src="{$jsPath}/common/I18nManager.js"></script>
        <script type="text/javascript" src="{$jsPath}/common/UndoableEdit.js"></script>
        <script type="text/javascript" src="{$jsPath}/common/CompoundEdit.js"></script>
        <script type="text/javascript" src="{$jsPath}/common/UndoableMergeEdit.js"></script>
        <script type="text/javascript" src="{$jsPath}/common/UndoManager.js"></script>
        <!-- gui -->
        <script type="text/javascript" src="{$jsPath}/gui/ContentEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/AbstractDialog.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/SimpleDialog.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/ErrorDialog.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/ExceptionDialog.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/SimpleArrayEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/PreloaderFrame.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/LoadingDialog.js"></script>
        <script type="text/javascript" src="{$jsPath}/gui/ContentLoader.js"></script>
        <!-- access -->
        <script type="text/javascript" src="{$jsPath}/access/AccessTab.js"></script>
        <script type="text/javascript" src="{$jsPath}/access/AccessTreeTable.js"></script>
        <script type="text/javascript" src="{$jsPath}/access/RuleDialog.js"></script>
        <!-- navigation -->
        <script type="text/javascript" src="{$jsPath}/navigation/NavigationContent.js"></script>

        <script type="text/javascript" src="{$jsPath}/navigation/editor/ItemEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/editor/RootItemEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/editor/MenuItemEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/editor/InsertItemEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/editor/GroupEditor.js"></script>

        <script type="text/javascript" src="{$jsPath}/navigation/NavigationTab.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/Tree.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/LabelEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/I18nEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/TypeEditor.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/EditContentDialog.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/MoveContentDialog.js"></script>

        <script type="text/javascript" src="{$jsPath}/navigation/undo/InsertUndo.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/undo/RemoveUndo.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/undo/DndUndo.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/undo/EditUndo.js"></script>
        <script type="text/javascript" src="{$jsPath}/navigation/undo/RestoreUndo.js"></script>

        <script type="text/javascript" src="{$wcmsBasePath}/node_modules/ckeditor/ckeditor.js"></script>
        <!-- main -->
        <script type="text/javascript" src="{$jsPath}/Header.js"></script>
        <script type="text/javascript" src="{$jsPath}/WCMS2.js"></script>

        <link rel="stylesheet" type="text/css"
          href="{$wcmsBasePath}/node_modules/dijit/themes/claro/claro.css"></link>
        <link rel="stylesheet" type="text/css" href="{$cssPath}/wcms2.css"></link>
        <link rel="stylesheet" type="text/css" href="{$cssPath}/dojo.treetable.css"></link>

        <script type="text/javascript">
          function setup() {
            var WCMS2 = new wcms.WCMS2();
            WCMS2.start();
          }
          dojo.ready(setup);
        </script>
      </head>

      <body class="claro">
        <div id="mainContainer">
          <!-- MyCoRe WCMS Text -->
          <div id="headerText">MyCoRe WCMS2</div>
          <!-- Content -->
          <div id="content"></div>
        </div>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>