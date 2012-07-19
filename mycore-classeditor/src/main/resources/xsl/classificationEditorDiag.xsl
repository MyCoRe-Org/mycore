<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="classificationEditorDiag">
    <xsl:call-template name="classeditor.loadSettings" />
    <!-- JS -->
    <xsl:call-template name="classeditor.includeDojoJS" />
    <xsl:call-template name="classeditor.includeJS" />
    <!-- CSS -->
    <xsl:call-template name="classeditor.includeDojoCSS" />
    <xsl:call-template name="classeditor.includeCSS" />

    <script type="text/javascript">
      function openClasseditor() {
          var diag = dijit.byId("classiDiag");
          if (diag == undefined) {
            diag = create();
          }
          diag.show();
      }

      function create() {
        updateBodyTheme();
        var classEditor = new classeditor.Editor(classeditor.settings);
        var diag = new dijit.Dialog({
          id : "classiDiag",
          content : classEditor.domNode
        });
        dojo.addClass(diag.domNode, "classeditorDialog");
        classEditor.create();
        diag.set("title", SimpleI18nManager.getInstance().get("component.classeditor"));
        classEditor.loadClassification(classeditor.classId, classeditor.categoryId);
        return diag;
      }
    </script>

    <div class="claro">
      <button data-dojo-type="dijit.form.Button" data-dojo-props="onClick: openClasseditor">Open Classification Editor Dialog</button>
    </div>

  </xsl:template>

</xsl:stylesheet>