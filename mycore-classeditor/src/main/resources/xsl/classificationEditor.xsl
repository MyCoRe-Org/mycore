<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="classificationEditor">

    <xsl:call-template name="classeditor.loadSettings">
      <xsl:with-param name="classeditor.class" select="@classId"/>
      <xsl:with-param name="classeditor.categ" select="@categId"/>
      <xsl:with-param name="classeditor.showId" select="@showId='true'"/>
    </xsl:call-template>
    <xsl:call-template name="classeditor.includeDojoJS" />
    <xsl:call-template name="classeditor.includeJS" />

    <div id="classificationEditorWrapper"></div>

    <script type="text/javascript">
      require(["dojo/ready"], function(ready) {
        ready(function() {
          require([
            "dojo/promise/all", "dijit/registry", "dojo/dom", "dojo/dom-construct", "dojo/query", "mycore/util/DOMUtil", "dojo/parser", "mycore/classification/Editor"
          ], function(all, registry, dom, domConstruct, query, domUtil) {
            ready(function() {
              domUtil.updateBodyTheme();
              var preloadCSS = [
                domUtil.loadCSS(classeditor.settings.webURL + "/node_modules/dijit/themes/claro/claro.css"),
                domUtil.loadCSS(classeditor.settings.cssURL + "/classificationEditor.css"),
                domUtil.loadCSS(classeditor.settings.cssURL + "/mycore.dojo.css")
              ];
              // check if font-awesome is already loaded
              if(query("link[href*='font-awesome']").length == 0) {
                preloadCSS.push(domUtil.loadCSS(classeditor.settings.webURL + "/node_modules/font-awesome/css/font-awesome.min.css"));
              }
              all(preloadCSS).then(function() {
                try {
                  var classEditor = new mycore.classification.Editor({settings: classeditor.settings});
                  var wrapper = dom.byId("classificationEditorWrapper");
                  if(wrapper == null) {
                    console.log("Unable to find element with id: 'classificationEditorWrapper'.");
                  }
                  domConstruct.place(classEditor.domNode, wrapper);
                  classEditor.loadClassification(classeditor.classId, classeditor.categoryId);
                  classEditor.startup();
                } catch(error) {
                  console.log(error);
                }
              });
            });
          });
        });
      });
    </script>

  </xsl:template>
</xsl:stylesheet>
