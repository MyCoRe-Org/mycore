// maybe move this to iview2.xsl
var ToolbarImporter = function (Iview2, i18n) {

	Iview2.getToolbarMgr = function() {
		if (!this.toolbarMgr) {
			this.toolbarMgr = new ToolbarManager();
		}
		return this.toolbarMgr;
	}
	
	Iview2.getToolbarCtrl = function() {
		if (!this.toolbarCtrl) {
			this.toolbarCtrl = new ToolbarController(this);
		}
		return this.toolbarCtrl;
	}
	
	// entweder Mgr macht alles und �bergabe des related... (Modelprovider) oder Models k�mmern sich untereinander und sch�ne Form (siehe unten)
	// Iview[viewID].getToolbarCtrl() oder Iview[viewID].toolbarCtrl verwenden?
	// vom Drop Down Menu nur die View oder auch ein Model im ToolbarManager?
	
	// Toolbar Manager
	Iview2.getToolbarMgr().addModel(new PreviewToolbarModelProvider("previewTb").getModel());
	// Toolbar Controller
	Iview2.getToolbarCtrl().addView(new ToolbarView("previewTbView", Iview2.getToolbarCtrl().toolbarContainer, i18n));
	
	// holt alle bisherigen Models in den Controller und setzt diese entsprechend um
	Iview2.getToolbarCtrl().catchModels();
};