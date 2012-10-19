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
            "dojo/promise/all", "dijit/registry", "dojo/dom-construct", "mycore/util/DOMUtil", "dojo/parser", "mycore/classification/Editor"
          ], function(all, registry, domConstruct, domUtil) {
            ready(function() {
              domUtil.updateBodyTheme();
              all([domUtil.loadCSS("http://ajax.googleapis.com/ajax/libs/dojo/"+classeditor.dojoVersion +"/dijit/themes/claro/claro.css"),
                   domUtil.loadCSS(classeditor.settings.cssURL + "/classificationEditor.css"),
                   domUtil.loadCSS(classeditor.settings.cssURL + "/mycore.dojo.css"),
                   domUtil.loadCSS(classeditor.settings.cssURL + "/modern-pictograms.css")]).then(function() {
                var classEditor = new mycore.classification.Editor({settings: classeditor.settings});
                domConstruct.place(classEditor.domNode, dojo.byId("classificationEditorWrapper"));
                classEditor.loadClassification(classeditor.classId, classeditor.categoryId);
                classEditor.startup();
              });
            });
          });
        });
      });
    </script>

  </xsl:template>
</xsl:stylesheet>
