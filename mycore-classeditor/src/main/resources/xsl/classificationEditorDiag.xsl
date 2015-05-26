<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="classificationEditorDiag">

    <xsl:call-template name="classeditor.loadSettings">
      <xsl:with-param name="classeditor.class" select="@classId"/>
      <xsl:with-param name="classeditor.categ" select="@categId"/>
      <xsl:with-param name="classeditor.showId" select="@showId='true'"/>
    </xsl:call-template>
    <xsl:call-template name="classeditor.includeDojoJS" />
    <xsl:call-template name="classeditor.includeJS" />

    <script type="text/javascript">
      require(["dojo/ready", "dojo/promise/all", "mycore/util/DOMUtil"], function(ready, all, domUtil) {
        ready(function() {
          var preloadCSS = [
            domUtil.loadCSS("//ajax.googleapis.com/ajax/libs/dojo/"+classeditor.dojoVersion +"/dijit/themes/claro/claro.css"),
            domUtil.loadCSS(classeditor.settings.cssURL + "/classificationEditor.css"),
            domUtil.loadCSS(classeditor.settings.cssURL + "/mycore.dojo.css")
          ];
          // check if font-awesome is already loaded
          if(query("link[href*='font-awesome']").length == 0) {
            preloadCSS.push(domUtil.loadCSS(classeditor.settings.cssURL + "/font-awesome.min.css"));
          }
          all(preloadCSS).then(function() {
            require([
              "dijit/registry", "dojo/dom-construct", "dojo/on", "dojo/parser",
              "dijit/form/Button", "dijit/Dialog", "mycore/classification/Editor"
            ], function(registry, domConstruct, on) {
              ready(function() {
                domUtil.updateBodyTheme();
                on(registry.byId("openClasseditor"), "click", function() {
                  var diag = registry.byId("classiDiag");
                  if (diag == undefined) {
                    var classEditor = new mycore.classification.Editor({settings: classeditor.settings});
                    diag = new dijit.Dialog({
                      id : "classiDiag",
                      content : classEditor
                    });
                    dojo.addClass(diag.domNode, "classeditorDialog");
                    diag.show();
                    classEditor.loadClassification(classeditor.classId, classeditor.categoryId);
                    classEditor.startup();
                  } else {
                    diag.show();
                  }
                });
              });
            });
          });
        });
      });
    </script>

    <div>
      <button data-dojo-type="dijit.form.Button" id="openClasseditor">Open Classification Editor Dialog</button>
    </div>

  </xsl:template>

</xsl:stylesheet>