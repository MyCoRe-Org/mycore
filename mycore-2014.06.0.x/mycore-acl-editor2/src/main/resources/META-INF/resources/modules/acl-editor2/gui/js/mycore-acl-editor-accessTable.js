var AccessTable = function(){
	var i18nKeys =[];
	var ruleSelectorInstance;
	
	return {
		init: 	function(accessAndRules, i18n, ruleSelector, aclEditor){
			i18nKeys = i18n;
			ruleSelectorInstance = ruleSelector;
			
			var cla = this;
			$.each(accessAndRules.access, function(i, l) {
				cla.add(l.accessID, l.accessPool, l.rule, false);
			});
			ruleSelectorInstance.append("", $(".acle2-new-access-rule"));
			this.zebra();
			$("#acle2-access-table").stupidtable();
			$("#acle2-access-table").bind('aftertablesort', function () {
				cla.zebra();
			});
			$("#acle2-loading").hide();
			$("#acle2-ruleAllocation").show();
			$("a.tab").attr("data-toggle", "tab");
			$("select.acle2-access-rule").each(function() {
				$(this).siblings("div.acle2-access-rule").attr("title", $(this).children("option:selected").attr("title"));
			});
		},
		add: 	function(accessID, accessPool, rule, prepend){
			var tr = $("<tr class='acle2-table-access-entry'></tr>");
			tr.append("<td><input type='checkbox' class='acle2-icon acle2-access-select'/></td>");
			tr.append("<td class='acle2-access-id acle2-table-access-entry-td' title='" +  accessID + "'>" + accessID + "<i class='glyphicon glyphicon-pencil acle2-icon acle2-button-edit' title='" + geti18n("ACLE.title.edit") + "'></i>" + "</td>");
			tr.append("<td class='acle2-access-pool acle2-table-access-entry-td' title='" +  accessPool + "'>" + accessPool  + "<i class='glyphicon glyphicon-pencil acle2-icon acle2-button-edit' title='" + geti18n("ACLE.title.edit") + "'></i>" + "</td>");
			var td = $("<td class='acle2-access-rule-parent'></td>");
			ruleSelectorInstance.append(rule, td);
			tr.append(td);
			tr.append('<td><i class="glyphicon glyphicon-filter acle2-icon acle2-button-filter-access" title="' + geti18n("ACLE.title.filter") + '"></i></td>');
			if(prepend == true){
				tr.prependTo("#acle2-access-table > tbody");
				$("#acle2-access-table").find("th").data("sort-dir", null);
				$(".glyphicon-chevron-up").removeClass("glyphicon-chevron-up");
				$(".glyphicon-chevron-down").removeClass("glyphicon-chevron-down");
				this.zebra();
			}
			else{
				tr.appendTo("#acle2-access-table > tbody");
			}
		},
		remove:	function(data) {
			var successAll = true;
			$(".acle2-delete").each(function() {
				var id = $(this).find(".acle2-access-id").text();
				var pool = $(this).find(".acle2-access-pool").text();
				var parent = $(this);
				$.each(data.access, function(i, l) {
					if(l.success == 1){
						if(id == l.accessID && pool == l.accessPool){
							parent.remove();
							return;
						}
					}
					else{
						successAll = false;
					}
				});
				parent.removeClass("acle2-delete");
			});
			this.zebra();
			return successAll;
		},
		edit: function(entry, accessID, accessPool, rule) {
			entry.find(".acle2-access-id").html(accessID + "<i class='glyphicon glyphicon-pencil acle2-icon acle2-button-edit' title='" + geti18n("ACLE.title.edit") + "'></i>");
			entry.find(".acle2-access-id").attr("title",accessID);
			entry.find(".acle2-access-id").removeClass("acle2-show-input");
			entry.find(".acle2-access-pool").html(accessPool + "<i class='glyphicon glyphicon-pencil acle2-icon acle2-button-edit' title='" + geti18n("ACLE.title.edit") + "'></i>");
			entry.find(".acle2-access-pool").attr("title",accessPool);
			entry.find(".acle2-access-pool").removeClass("acle2-show-input");
			entry.find(".acle2-access-rule").select2("val", rule);
		},
		editMulti: function(data) {
			var successAll = true;
			$(".acle2-multi-edit").each(function() {
				var id = $(this).find(".acle2-access-id").text();
				var pool = $(this).find(".acle2-access-pool").text();
				var parent = $(this);
				$.each(data.access, function(i, l) {
					if(l.success == 1){
						if(id == l.accessID && pool == l.accessPool){
							parent.find(".acle2-access-rule").select2("val", l.accessRule);
							return;
						}
					}
					else{
						successAll = false;
					}
				});
				parent.removeClass("acle2-multi-edit");
			});
			return successAll;
		},
		zebra:	function() {
			$("#acle2-access-table tr:not(.acle2-filter-hide)").each(function(i, row){
				$(row).removeClass("odd");
				if (i % 2 == 0){
					$(row).addClass("odd");
				}
			});
		}
	};
	function geti18n(key) {
		var string = i18nKeys[key];
		if (string != undefined){
			for (i = 0; i < arguments.length-1; i++){
				string = string.replace(new RegExp('\\{' + i + '\\}', "g"), arguments[i+1]);
			}
			return string;
		}
		else{
			return "";
		}
	}
}