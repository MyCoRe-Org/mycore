<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="classificationEditor">
    <xsl:call-template name="classeditor.loadSettings" />
    <!-- JS -->
    <xsl:call-template name="classeditor.includeDojoJS" />
    <xsl:call-template name="classeditor.includeJS" />
    <!-- CSS -->
    <xsl:call-template name="classeditor.includeDojoCSS" />
    <xsl:call-template name="classeditor.includeCSS" />

    <div id="classificationEditorWrapper">
    </div>

    <script type="text/javascript">
      function setup() {
        updateBodyTheme();
        var classEditor = new classeditor.Editor(classeditor.settings);
        dojo.byId("classificationEditorWrapper").appendChild(classEditor.domNode);
        classEditor.create();
        classEditor.loadClassification(classeditor.classId, classeditor.categoryId);
      }
      dojo.ready(setup);
    </script>

  </xsl:template>
</xsl:stylesheet>
