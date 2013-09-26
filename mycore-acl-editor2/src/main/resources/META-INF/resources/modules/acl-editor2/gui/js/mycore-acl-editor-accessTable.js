var AccessTable = function(){
	var i18nKeys =[];
	var ruleSelectorInstance;
	var aclEditorInstance;
	
	return {
		init: 	function(accessAndRules, i18n, ruleSelector, aclEditor){
			i18nKeys = i18n;
			ruleSelectorInstance = ruleSelector;
			aclEditorInstance = aclEditor;
			
			var cla = this;
			$.each(accessAndRules.access, function(i, l) {
				cla.add(l.accessID, l.accessPool, l.rule, false);
			});
			ruleSelectorInstance.append("", $(".new-access-rule"));
			this.zebra();
			$("#access-table").stupidtable();
			$("#access-table").bind('aftertablesort', function () {
				cla.zebra();
			});
			$("#loading").hide();
			$("#ruleAllocation").show();
			$("a.tab").attr("data-toggle", "tab");
		},
		add: 	function(accessID, accessPool, rule, prepend){
			var tr = $("<tr class='table-access-entry'></tr>");
			tr.append("<td><input type='checkbox' class='icon-check-empty icon-large icon access-select'/></td>");
			tr.append("<td class='access-id table-access-entry-td' title='" +  accessID + "'>" + accessID + "</td>");
			tr.append("<td class='access-pool table-access-entry-td' title='" +  accessPool + "'>" + accessPool + "</td>");
			var td = $("<td class='access-rule-parent'></td>");
			ruleSelectorInstance.append(rule, td);
			tr.append(td);
			tr.append("<td></td>");
			if(prepend == true){
				tr.prependTo("#access-table > tbody");
				$("#access-table").find("th").data("sort-dir", null);
				$(".icon-caret-up").removeClass("icon-caret-up");
				$(".icon-caret-down").removeClass("icon-caret-down");
				this.zebra();
			}
			else{
				tr.appendTo("#access-table > tbody");
			}
		},
		remove:	function(data) {
			$(".delete").each(function() {
				var id = $(this).find(".access-id").text();
				var pool = $(this).find(".access-pool").text();
				var parent = $(this);
				$.each(data.access, function(i, l) {
					if(id == l.accessID && pool == l.accessPool){
						if(l.success == 1){
							parent.remove();
							return;
						}
						else{
							showAlert(i18nKeys["ACLE.alert.access.remove.error.1"] + id + " : " + pool + i18nKeys["ACLE.alert.access.remove.error.2"]);
							parent.removeClass("delete");
							return;
						}
					}
				});
				parent.removeClass("delete");
			});
			this.zebra();
		},
		edit: function(entry, accessID, accessPool, rule) {
			entry.find(".access-id").html(accessID);
			entry.find(".access-id").attr("title",accessID);
			entry.find(".access-id").removeClass("show-input");
			entry.find(".access-pool").html(accessPool);
			entry.find(".access-pool").attr("title",accessPool);
			entry.find(".access-pool").removeClass("show-input");
			entry.find(".access-rule").select2("val", rule);
		},
		editMulti: function(data) {
			$(".multi-edit").each(function() {
				var id = $(this).find(".access-id").text();
				var pool = $(this).find(".access-pool").text();
				var parent = $(this);
				$.each(data.access, function(i, l) {
					if(id == l.accessID && pool == l.accessPool){
						if(l.success == 1){
							parent.find(".access-rule").select2("val", l.accessRule);
							return;
						}
						else{
							showAlert(i18nKeys["ACLE.alert.access.edit.multi.error.1"] + id + " : " + pool + i18nKeys["ACLE.alert.access.edit.multi.error.2"]);
							parent.removeClass("multi-edit");
							return;
						}
					}
				});
				parent.removeClass("multi-edit");
			});
		},
		zebra:	function() {
			$("#access-table tr:not(.filter-hide)").each(function(i, row){
				$(row).removeClass("odd");
				if (i % 2 == 0){
					$(row).addClass("odd");
				}
			});
		}
	};
}