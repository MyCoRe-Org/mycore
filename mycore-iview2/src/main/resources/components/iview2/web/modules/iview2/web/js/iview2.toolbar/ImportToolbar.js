var ImportToolbar = function (viewID, titles) {

				// needs to delete, when viewID isn't used
				Iview[viewID].viewID_to_remove = viewID;

				Iview[viewID].getToolbarMgr = function() {
					if (!this.toolbarMgr) {
						this.toolbarMgr = new ToolbarManager();
					}
					return this.toolbarMgr;
				}
				
				Iview[viewID].getToolbarCtrl = function() {
					if (!this.toolbarCtrl) {
						this.toolbarCtrl = new ToolbarController();
						
						//ToolbarController.prototype.getViewer = function() {
						this.toolbarCtrl.getViewer = function() {
							return Iview[viewID];
						}
					}
					return this.toolbarCtrl;
				}
				
				// entweder Mgr macht alles und �bergabe des related... (Modelprovider) oder Models k�mmern sich untereinander und sch�ne Form (siehe unten)
				// Iview[viewID].getToolbarCtrl() oder Iview[viewID].toolbarCtrl verwenden?
				// vom Drop Down Menu nur die View oder auch ein Model im ToolbarManager?
				
				// Toolbar Manager
				Iview[viewID].getToolbarMgr().setTitles(titles);
				
				Iview[viewID].getToolbarMgr().addModel(new PreviewToolbarModelProvider("previewTb", titles).getModel());
				
				// Toolbar Controller
				Iview[viewID].getToolbarCtrl().addView(new ToolbarView("previewTbView", Iview[viewID].viewerContainer.find(".toolbars")));
				
				// holt alle bisherigen Models in den Controller und setzt diese entsprechend um
				Iview[viewID].getToolbarCtrl().catchModels();

				
				// Permalink
				Iview[viewID].getPermalinkCtrl = function() {
					if (!this.permalinkCtrl) {
						this.permalinkCtrl = new iview.Permalink.Controller();
						
						//iview.Permalink.Controller.prototype.getViewer = function() {
						this.permalinkCtrl.getViewer = function() {
							return Iview[viewID];
						}
					}
					return this.permalinkCtrl;
				}

				Iview[viewID].getPermalinkCtrl().addView(new iview.Permalink.View("permalinkView", Iview[viewID].viewerContainer.find(".toolbars").parent()));
};