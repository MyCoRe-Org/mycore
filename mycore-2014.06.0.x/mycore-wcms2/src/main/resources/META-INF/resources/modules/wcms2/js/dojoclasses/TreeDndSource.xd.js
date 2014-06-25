/**
 * TODO: delete this class in future wcms releases when upgrading to a newer
 * dojo version. this is just a hack to handle cross-domain loading.
 */
window[(typeof (djConfig) != "undefined" && djConfig.scopeMap && djConfig.scopeMap[0][1])
		|| "dojo"]._xdResourceLoaded(function(_1, _2, _3) {
	return {
		depends : [ [ "provide", "dojoclasses.TreeDndSource" ],
				[ "require", "dijit.tree.dndSource" ] ],
		defineResource : function(_4, _5, _6) {
			if (!_4._hasResource["dojoclasses.TreeDndSource"]) {
				_4._hasResource["dojoclasses.TreeDndSource"] = true;
				_4.provide("dojoclasses.TreeDndSource");
				_4.require("dijit.tree.dndSource");
				_4.declare("dojoclasses.TreeDndSource", _5.tree.dndSource, {
					onMouseDown : function(e) {
						// this is a workaround to fix dnd support in tree
						// the id is only set if the scrollbar is hit, this
						// allows us
						// to abort the event and don't do an invalid drag
						var element = document.elementFromPoint(e.clientX,
								e.clientY);
						var id = element.id;
						if (id != null && id != "") {
							return;
						}
						this.inherited("onMouseDown", arguments);
					}
				});
			}
		}
	};
});