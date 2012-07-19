<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="classificationEditor">
    <xsl:call-template name="classeditor.loadSettings">
      <xsl:with-param name="classeditor.class" select="@classId"/>
      <xsl:with-param name="classeditor.categ" select="@categId"/>
      <xsl:with-param name="classeditor.showId" select="@showId='true'"/>
    </xsl:call-template>
    <!-- JS -->
    <xsl:call-template name="classeditor.includeDojoJS" />
    <xsl:call-template name="classeditor.includeJS" />
    <!-- CSS -->
    <xsl:call-template name="classeditor.includeDynamicCSS" />

    <div id="classificationEditorWrapper">
    </div>

    <script type="text/javascript">
      function setup(topic) {
        async.parallel([
          function(callback) {
            topic.subscribe("dojo/included", function(){
              callback();
            });
          }, function(callback) {
            classeditor.includeDojoCSS(callback);
          }, function(callback) {
            classeditor.includeCSS(callback);
          }
        ], function(err, results) {
          if(err != null) {
            alert('error while loading css file ' + err.href);
            console.log(err.error);
            return;
          }
          updateBodyTheme();
          var classEditor = new classeditor.Editor(classeditor.settings);
          dojo.byId("classificationEditorWrapper").appendChild(classEditor.domNode);
          classEditor.create();
          classEditor.loadClassification(classeditor.classId, classeditor.categoryId);
        });
      }
      require(["dojo/topic", "dojo/ready", "dijit/registry", "dojo/domReady!"], setup);
    </script>

  </xsl:template>
</xsl:stylesheet>
