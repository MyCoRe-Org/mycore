var ACLEditor = function(){
	var $this = this;
	var i18nKeys =[];
	var timeOutID = null;
	
	var ruleSelectorInstance;
	var accessTableInstance;
	var ruleListInstance;
	
	return {
		init: function(ruleSelect, accessTable, ruleList){
			$("body").on("change", "select", function() {
				if($(this).children("option:selected").attr("title") == ""){
					$('#lightbox-new-rule').modal('show');
				}
			});
			
			$("body").on("change", ".access-rule", function() {
				var access = $(this).parents(".table-access-entry");
				var json = {
							"accessIDOld": access.find(".access-id").text(),
							"accessPoolOld": access.find(".access-pool").text(),
							"mode": "rule",
							"accessIDNew": access.find(".access-id").text(),
							"accessPoolNew": access.find(".access-pool").text(),
							"accessRuleNew": access.find(".access-rule:not(.select2-container)").val()						
						};
				editAccess(json); 
			});
			
			$("body").on("click", "#button-new-access", function() {
				$("#table-new-access > .control-group.error").removeClass("control-group error");
				var accessID = $("#new-access-id").val();
				var accessPool = $("#new-access-pool").val();
				var accessRule = $("#new-access-rule").val();
				
				if(accessID != "" &&  accessPool != "" && accessRule != "" && accessRule != "new"){
					addAccess(accessID, accessPool , accessRule); 
				}
				else{
					showAlert(i18nKeys["ACLE.alert.access.fields"]);
					if (accessID == ""){
						$("#new-access-id").parent().addClass("control-group error");
					}
					if (accessPool == ""){
						$("#new-access-pool").parent().addClass("control-group error");
					}
					if (accessRule == "" || accessRule == "new"){
						$("#new-access-rule").parent().addClass("control-group error");
					}
				}
			});
						
			$("body").on("click", ".access-select", function() {
				if ($(this).hasClass("icon-check-empty")){
					$(this).addClass("icon-check");
					$(this).removeClass("icon-check-empty");
				}
				else{
					$(this).addClass("icon-check-empty");
					$(this).removeClass("icon-check");
				}
			});
						
			$("body").on("click", "#button-remove-multi-access", function() {
				var json = {
						  "access": [],
						};
				$(".table-access-entry .icon-check").each(function() {
					var access = $(this).parents(".table-access-entry");
					access.addClass("delete");
					json.access.push({"accessID": access.find(".access-id").text(), "accessPool": access.find(".access-pool").text()});
				});
				removeAccess(json); 
			});
						
			$("body").on("click", "#new-rule-add", function() {
				$("#lightbox-alert-area").removeClass("in");
				$("#rule-detail-table > .control-group.error").removeClass("control-group error");
				if ($(".new-rule-text").attr("value") != ""){
					addRule($("#new-rule-desc").val(), $(".new-rule-text").val())
					$('#lightbox-new-rule').modal('hide');
					$("#new-rule-desc").val("");
					$(".new-rule-text").val("");
				}
				else{
					$("#lightbox-alert-area").addClass("in");
					$(".new-rule-text").parent().addClass("control-group error");
				}
			});
			
			$("body").on("click", ".new-rule-cancel", function() {
				$("#new-access-rule").select2("val", "");
				$("#lightbox-alert-area").removeClass("in");
				$("#rule-detail-table > .control-group.error").removeClass("control-group error");
				$("#new-rule-desc").val("");
				$(".new-rule-text").val("");
			});
			
			$("body").on("click", ".tab", function(event) {
				event.preventDefault();
			});
			
			$("body").on("click", ".rule-list-entry", function() {
				ruleListInstance.select($(this).attr("ruleid"));
			});
			
			$("body").on("click", "#button-delete-rule", function() {
				var rule = $(this).parents("#rule-detail-table");
				if (canDelete(rule.find("#rule-detail-ruleID").html())){
					removeRule(rule.find("#rule-detail-ruleID").html());
				}
				else{
					showAlert(i18nKeys["ACLE.alert.rule.inUse"], false);
				}
			});
			
			$("body").on("click", "#button-save-rule", function() {
				$("#rule-detail-table > .control-group.error").removeClass("control-group error");
				if ($(".rule-detail-ruleText").attr("value") != ""){
					if ($("#rule-detail-ruleID").html() == ""){
						addRule($("#rule-detail-ruleDesc").val(), $(".rule-detail-ruleText").attr("value"));
					}
					else{
						editRule($("#rule-detail-ruleID").html(), $("#rule-detail-ruleDesc").val(), $(".rule-detail-ruleText").attr("value"));
					}
				}
				else{
					$(".rule-detail-ruleText").parent().addClass("control-group error");
					showAlert(i18nKeys["ACLE.alert.rule.noRule"])	
				}
			});
			
			$("body").on("click", "#button-select-multi-access", function() {
				if ($(this).hasClass("icon-check-empty")){
					$(".icon-check-empty:visible").addClass("icon-check");
					$(".icon-check-empty:visible").prop("checked", true);
					$(".icon-check-empty:visible").removeClass("icon-check-empty");
				}
				else{
					$(".icon-check:visible").addClass("icon-check-empty");
					$(".icon-check:visible").prop("checked", false);
					$(".icon-check:visible").removeClass("icon-check");
				}
			});
			
			$("body").on("click", "#button-access-filter", function() {
				filterTable();
			});
			
			$("body").on("keydown", ".access-filter-input", function(key) {
				if(key.which == 13) {
					filterTable();
				}
			});
			
			$("body").on("click", ".table-access-entry-td", function() {
				if(!$(this).hasClass("show-input")){
					var input = $('<input type="text" class="input-xlarge table-access-entry-input" value="' + $(this).attr("title") + '"></input>');
					$(this).html(input);
					input.focus();
					$(this).addClass("show-input");
				}
			});
			
			$("body").on("keydown", ".table-access-entry-input", function(key) {
				if(key.which == 13) {
					$(".edit").find(".show-input").removeClass("control-group error");
					var parent = $(this).parent();
					var entry = parent.parent();
					if($(this).val() != parent.attr("title")){
						var json = {
								"accessIDOld": entry.find(".access-id").attr("title"),
								"accessPoolOld": entry.find(".access-pool").attr("title"),
								"mode": "idPool",
								"accessIDNew": "",
								"accessPoolNew": "",
								"accessRuleNew": ""						
							};
						 
						json.accessIDNew = entry.find(".access-id").hasClass("show-input") ? entry.find(".access-id input").val() : entry.find(".access-id").text();
						json.accessPoolNew = entry.find(".access-pool").hasClass("show-input") ? entry.find(".access-pool input").val() : entry.find(".access-pool").text();
						json.accessRuleNew = entry.find(".access-rule:not(.select2-container)").val();
						entry.addClass("edit");
						editAccess(json);
					}
					else{
						parent.html($(this).val());
						parent.attr("title", $(this).val());
						parent.removeClass("show-input");
						$(this).remove();
					}
				}
				if(key.which == 27) {
					$(".edit").find(".show-input").removeClass("control-group error");
					var parent = $(this).parent();
					var entry = parent.parent();
					accessTableInstance.edit(entry, entry.find(".access-id").attr("title"), entry.find(".access-pool").attr("title"), entry.find(".access-rule:not(.select2-container)").val());
				}
			});
			
			$("body").on("click", ".sort-table-head", function() {
				if($(this).data("sort-dir") == "asc"){
					$(".icon-chevron-up").removeClass("icon-chevron-up");
					$(".icon-chevron-down").removeClass("icon-chevron-down");
					$(this).children(".sort-icon").addClass("icon-chevron-up")
				}
				else{
					$(".icon-chevron-up").removeClass("icon-chevron-up");
					$(".icon-chevron-down").removeClass("icon-chevron-down");
					$(this).children(".sort-icon").addClass("icon-chevron-down")
				}
			});
			
			$("body").on("keydown", "#elem-per-page", function(key) {
				if(key.which == 13) {
					splitTable();
				}
			});
			
			ruleSelectorInstance = ruleSelect;
			accessTableInstance = accessTable;
			ruleListInstance = ruleList;
			var lang = $("#jportal_acl_editor_module").attr("lang");
			jQuery.getJSON("/servlets/MCRLocaleServlet/" + lang + "/ACLE.*", function(data) { 
				i18nKeys = data;
				getAccess();
			});
		}
	}
	
	function getAccess(){
		$.ajax({
			url: "/rsc/ACLE/",
			type: "GET",
			dataType: "json",
			success: function(data) {
						ruleSelectorInstance.init(data.rules, i18nKeys);
						accessTableInstance.init(data, i18nKeys, ruleSelectorInstance, $this);
						ruleListInstance.init(data.rules, i18nKeys);
						$("#access-table").bind('aftertablesort', function () {
							refreshPageNumbers();
						});
						splitTable();
						addTypeahead();
					},
			error: function(error) {
						alert(error);
					}
		});
	}
	
	function addAccess(accessID, accessPool, rule){
		$.ajax({
			url: "/rsc/ACLE",
			type: "POST",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify({accessID: accessID, accessPool: accessPool, rule: rule}),
			statusCode: {
				200: function() {
					accessTableInstance.add(accessID, accessPool, rule, true);
					splitTable();
					$("#new-access-id").val("");
					$("#new-access-pool").val("");
					$("#new-access-rule").select2("val", "");
					showAlert(i18nKeys["ACLE.alert.access.add.success.1"] + accessID + i18nKeys["ACLE.alert.access.add.success.2"], true);
				},
				409: function() {
					showAlert(i18nKeys["ACLE.alert.access.add.exist"]);
				},					
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.access.add.error"]);
				}
			}
		});
	}
	
	function editAccess(json){
		$.ajax({
			url: "/rsc/ACLE/",
			type: "PUT",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify(json),
			statusCode: {
				200: function() {
					accessTableInstance.edit($(".edit"), json.accessIDNew, json.accessPoolNew, json.accessRuleNew);
					showAlert(i18nKeys["ACLE.alert.access.edit.success"], true);
				},
				409: function() {
					$(".edit").find(".show-input").addClass("control-group error");
					showAlert(i18nKeys["ACLE.alert.access.add.exist"]);
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.access.edit.error"]);
				}
			}
		});
	}
	
	function removeAccess(json){
		$.ajax({
			url: "/rsc/ACLE/",
			type: "DELETE",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify(json),
			statusCode: {
				200: function(data) {
					accessTableInstance.remove(data);
					splitTable();
					showAlert(i18nKeys["ACLE.alert.access.remove.success"], true);
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.access.remove.error"]);					
				}
			}
		});
	}
	
	function addRule(ruleDesc, ruleText){
		$.ajax({
			url: "/rsc/ACLE/rule",
			type: "POST",
			contentType: 'application/json',
			dataType: "text",
			data: JSON.stringify({ruleDesc: ruleDesc, ruleText: ruleText}),
			statusCode: {
				200: function(ruleID) {
					ruleSelectorInstance.add(ruleID, ruleDesc, ruleText);
					ruleSelectorInstance.update();
					$("#new-access-rule").select2("val", ruleID);
					ruleListInstance.add(ruleID, ruleDesc, ruleText);
					ruleListInstance.select(ruleID);
					$('#rule-list').animate({scrollTop : $('#rule-list').height()},'fast');
					showAlert(i18nKeys["ACLE.alert.rule.add.success.1"] + ruleDesc + i18nKeys["ACLE.alert.rule.add.success.2"] + ruleID + i18nKeys["ACLE.alert.rule.add.success.3"], true);	
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.rule.add.error"]);
				}
			}
		});
	}
	
	function removeRule(ruleID){
		$.ajax({
			url: "/rsc/ACLE/rule",
			type: "DELETE",
			contentType: 'application/json',
			dataType: "text",
			data: JSON.stringify({ruleID: ruleID}),
			statusCode: {
				200: function(data) {
					ruleSelectorInstance.remove(ruleID);
					ruleSelectorInstance.update();
					ruleListInstance.remove(ruleID);
					ruleListInstance.select("");
					$('#rule-list').animate({scrollTop : 0},'fast');
					showAlert(i18nKeys["ACLE.alert.rule.remove.success.1"] + ruleID + i18nKeys["ACLE.alert.rule.remove.success.2"], true);
				},
				409: function(ruleID) {
					showAlert(i18nKeys["ACLE.alert.rule.remove.notExist"]);
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.rule.remove.error"]);
				}
			}
		});
	}
	
	function editRule(ruleID, ruleDesc, ruleText){
		$.ajax({
			url: "/rsc/ACLE/rule",
			type: "PUT",
			contentType: 'application/json',
			dataType: "text",
			data: JSON.stringify({ruleID: ruleID, ruleDesc: ruleDesc, ruleText: ruleText}),
			statusCode: {
				200: function() {
					ruleSelectorInstance.edit(ruleID, ruleDesc, ruleText);
					ruleSelectorInstance.update();
					ruleListInstance.edit(ruleID, ruleDesc, ruleText);
					showAlert(i18nKeys["ACLE.alert.rule.edit.success.1"] + ruleDesc + i18nKeys["ACLE.alert.rule.edit.success.2"], true);
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.rule.edit.error"]);
				}
			}
		});
	}
		
	function canDelete(ruleID){
		result = true;
		$(".access-rule option:selected").each(function() {
			if ($(this).attr("value") == ruleID){
				result = false;
			}
		});
		return result;
	}
	
	function updateRuleSelector() {
		$(".access-rule").each(function() {
			var rule = $(this).children("option:selected").attr("value");
			$(this).replaceWith(ruleSelector.clone().val(rule));
		});
		$("#new-access-rule").html(ruleSelector.html());
		$("#new-access-rule").prepend("<option value='' selected>" + i18nKeys["ACLE.select.select"] + "</option>");
		$("#new-access-rule").append("<option id='new-access-rule-option' value='new' title=''>" + i18nKeys["ACLE.select.newRule"] + "</option>");
		$(".access-rule").select2({
			matcher: function(term, text, opt) {
					return text.toUpperCase().indexOf(term.toUpperCase())>=0
						|| opt.attr("title").toUpperCase().indexOf(term.toUpperCase())>=0;
				},
			formatResult: formatSelect
		});
	}
	
	function filterTable() {
		$(".table-access-entry").addClass("filter-hide");
		var filterID = $("#access-filter-input-id").val();
		filterID = filterID.replace("*", ".*");
		filterID = filterID.replace("?", ".?");
		filterID = filterID.replace("(", "\\(");
		filterID = filterID.replace(")", "\\)");
		
		if(filterID != ""){
			$(".table-access-entry")
		    .filter(function() {
		        return $(this).find(".access-id").attr("title").match(new RegExp("^" + filterID, "i"));
		    })
		    .removeClass("filter-hide");
		}
		
		var filterPool = $("#access-filter-input-pool").val();
		filterPool = filterPool.replace("*", ".*");
		filterPool = filterPool.replace("?", ".?");
		filterPool = filterPool.replace("(", "\\(");
		filterPool = filterPool.replace(")", "\\)");
		
		if(filterPool != ""){
			$(".table-access-entry")
		    .filter(function() {
		        return $(this).find(".access-pool").attr("title").match(new RegExp("^" + filterPool, "i"));
		    })
		    .removeClass("filter-hide");
		}
		
	    var filterRule = $("#access-filter-input-rule").val();
		filterRule = filterRule.replace("*", ".*");
		filterRule = filterRule.replace("?", ".?");
		filterRule = filterRule.replace("(", "\\(");
		filterRule = filterRule.replace(")", "\\)");
		
	    if(filterRule != ""){
			$(".table-access-entry")
		    .filter(function() {
		        return $(this).find(".access-rule option:selected").attr("value").match(new RegExp("^" + filterRule, "i"));
		    })
		    .removeClass("filter-hide");
			
			$(".table-access-entry")
		    .filter(function() {
		        return $(this).find(".access-rule option:selected").html().match(new RegExp("^" + filterRule, "i"));
		    })
		    .removeClass("filter-hide");
	    }
	    if(filterID == "" && filterPool == "" && filterRule == ""){
	    	$(".table-access-entry").removeClass("filter-hide");
	    }
	    splitTable();
		accessTableInstance.zebra();
	}
		
	function showAlert(text, success) {
		$('#alert-area').removeClass("in");
		$("#alert-area").removeClass("alert-success");
		$("#alert-area").removeClass("alert-error");
		if (timeOutID != null){
			window.clearTimeout(timeOutID);
		}

		$("#alert-area").html(text);
		if (success){
			$("#alert-area").addClass("alert-success");
			$("#alert-area").addClass("in");
		}
		else{
			$("#alert-area").addClass("alert-error");
			$("#alert-area").addClass("in");
		}
		timeOutID = window.setTimeout(function() {
				$('#alert-area').removeClass("in")
				$("#alert-area").removeClass("alert-success");
				$("#alert-area").removeClass("alert-error");
			}, 5000);
	}
	
	function addTypeahead() {
		var ids = new Array();
		$.each($(".access-id"), function() {
			if($.inArray($(this).attr("title"), ids) == -1){
				ids.push($(this).attr("title"));
			}
		});
		$("#access-filter-input-id").typeahead({source: ids});
		
		var pools = new Array();
		$.each($(".access-pool"), function() {
			if($.inArray($(this).attr("title"), pools) == -1){
				pools.push($(this).attr("title"));
			}
		});
		$("#access-filter-input-pool").typeahead({source: pools});
		
		var rules = new Array();
		$.each($(".access-rule option:selected"), function() {
			if($.inArray($(this).attr("value"), rules) == -1){
				rules.push($(this).attr("value"));
			}
			if($.inArray($(this).html(), rules) == -1){
				rules.push($(this).html());
			}
		});
		$("#access-filter-input-rule").typeahead({source: rules});
	}
	
	function splitTable() {
		var elemPerPage = $("#elem-per-page-input").val();
		$("#access-table tbody tr:not(.filter-hide)").each(function(i, row){
			page = Math.floor(i / elemPerPage) + 1;
			$(row).attr("data-page", page);
		});
		buildPaginator(1);
		$("#access-table tbody tr:not(.filter-hide)").addClass("page-hide");
		$("[data-page=1]").removeClass("page-hide");
	}
	
	function buildPaginator(page) {
		$(".pagination ul").html("");
		pagecount = Math.ceil($("#access-table tbody tr:not(.filter-hide)").size() / $("#elem-per-page-input").val());
		if(pagecount > 3){
			if(page > 2){
				if (page < pagecount - 1){
					//e.g. first ... 2 3 4 ... last
					addPageToPaginator(1, 1, "");
					addPageToPaginator(0, "...", "disabled");
					for(var i = page -1 ; i <= page + 1; i++){
						if(i == page){
							addPageToPaginator(i, i, "active");
						}
						else{
							addPageToPaginator(i, i, "");
						}
					}
					addPageToPaginator(0, "...", "disabled");
					addPageToPaginator(pagecount, pagecount, "");
				}
				else{
					//e.g. first ... 4 5 6
					addPageToPaginator(1, 1, "");
					addPageToPaginator(0, "...", "disabled");
					for(var i = pagecount - 2 ; i <= pagecount; i++){
						if(i == page){
							addPageToPaginator(i, i, "active");
						}
						else{
							addPageToPaginator(i, i, "");
						}
					}
				}
			}
			else{
				//e.g. 1 2 3 ... last
				for(var i = 1; i <= 3; i++){
					if(i == page){
						addPageToPaginator(i, i, "active");
					}
					else{
						addPageToPaginator(i, i, "");
					}
				}
				addPageToPaginator(0, "...", "disabled");
				addPageToPaginator(pagecount, pagecount, "");
			}
		}
		else{
			if(pagecount != 1){
				//e.g. 1 2 3
				for(var i = 1; i <= pagecount; i++){
					if(i == page){
						addPageToPaginator(i, i, "active");
					}
					else{
						addPageToPaginator(i, i, "");
					}
					
				}
			}
		}
	}
	
	function addPageToPaginator(href, name, state) {
		if (state == ""){
			var pageButton = $('<a href="#" onclick="return false;">' + name + '</a>');
			pageButton.bind("click", function() {
				showPage(href);
				buildPaginator(href);
			});
		}
		else{
			var pageButton = $('<span>' + name + '</span>');
		}
		$("<li></li>").append(pageButton).addClass(state).appendTo(".pagination ul");
	}
	
	function refreshPageNumbers() {
		var elemPerPage = $("#elem-per-page-input").val();
		$("#access-table tbody tr:not(.filter-hide)").each(function(i, row){
			page = Math.floor(i / elemPerPage) + 1;
			$(row).attr("data-page", page);
		});
		if ($(".pagination .active span").html() == undefined){
			showPage(1);
		}
		else{
			showPage($(".pagination .active span").html());
		}	
	}
	
	function showPage(num) {
		$("#access-table tbody tr:not(.filter-hide)").addClass("page-hide");
		$(".table-access-entry .icon-check").parents(".table-access-entry").removeClass("page-hide");
		$("[data-page=" + num + "]").removeClass("page-hide");
	}
	
	function formatSelect(item) {
		var span = $("<span></span>")
		span.text($(item.element).text());
		span.attr("title", $(item.element).attr("title"));
		return span;
	}
}

$(document).ready(function() {
	var aclEditorInstance = new ACLEditor();
    if (!$.isFunction(jQuery.fn.typeahead)){
    	$.getScript('/rsc/ACLE/gui/js/bootstrap.min.js')
    		.done(function() {
    			console.log("bootstrap.min.js loaded");
    			aclEditorInstance.init(new RuleSelector(), new AccessTable(), new RuleList());
    		});
    }
    else{
    	aclEditorInstance.init(new RuleSelector(), new AccessTable(), new RuleList());
    }
	
    
});
