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
				if($(this).children("option:selected").val() == "new"){
					$('#acle2-lightbox-new-rule').modal('show');
				}
				$(this).siblings("div.acle2-access-rule").attr("title", $(this).children("option:selected").attr("title"));
			});
			
			$("body").on("change", ".acle2-access-rule-parent > .acle2-access-rule", function() {
				var access = $(this).parents(".acle2-table-access-entry");
				var json = {
							"accessIDOld": access.find(".acle2-access-id").text(),
							"accessPoolOld": access.find(".acle2-access-pool").text(),
							"mode": "rule",
							"accessIDNew": access.find(".acle2-access-id").text(),
							"accessPoolNew": access.find(".acle2-access-pool").text(),
							"accessRuleNew": access.find(".acle2-access-rule:not(.select2-container)").val()
						};
				editAccess(json); 
				$(this).siblings("div.acle2-access-rule").attr("title", $(this).children("option:selected").attr("title"));
			});
			
			$("body").on("click", "#acle2-button-new-access", function() {
				$("#acle2-table-new-access > .form-group.has-error").removeClass("form-group has-error");
				var accessID = $("#acle2-new-access-id").val();
				var accessPool = $("#acle2-new-access-pool").val();
				var accessRule = $(".acle2-new-access-rule > select").val();
				
				if(accessID != "" &&  accessPool != "" && accessRule != "" && accessRule != "new"){
					addAccess(accessID, accessPool , accessRule); 
				}
				else{
					showAlert(geti18n("ACLE.alert.access.fields"));
					if (accessID == ""){
						$("#acle2-new-access-id").parent().addClass("form-group has-error");
					}
					if (accessPool == ""){
						$("#acle2-new-access-pool").parent().addClass("form-group has-error");
					}
					if (accessRule == "" || accessRule == "new"){
						$(".acle2-new-access-rule").addClass("form-group has-error");
					}
				}
			});
						
			$("body").on("click", "#acle2-button-remove-multi-access", function() {
				var elm = $(".acle2-table-access-entry .acle2-access-select:checked").length;
				if (elm > 0){
					$(".acle2-table-access-entry .acle2-access-select:checked").each(function() {						
						var access = $(this).parents(".acle2-table-access-entry");
						var p = $("<p></p>");
						p.html(access.find(".acle2-access-id").text() + " : " + access.find(".acle2-access-pool").text() + " : " + access.find(".acle2-access-rule:not(.select2-container)").val()); 
						$("#acle2-lightbox-multi-delete-list").append(p);
					});
					$('#acle2-lightbox-multi-delete').modal('show');
				}
				else{
					showAlert(geti18n("ACLE.alert.access.edit.select.error"));
				}
			});
					
			$("body").on("click", "#acle2-lightbox-multi-delete-delete", function() {
				var json = {
						  "access": [],
						};
				$(".acle2-table-access-entry .acle2-access-select:checked").each(function() {
					var access = $(this).parents(".acle2-table-access-entry");
					access.addClass("acle2-delete");
					json.access.push({"accessID": access.find(".acle2-access-id").text(), "accessPool": access.find(".acle2-access-pool").text()});
				});
				removeAccess(json);
				$("#acle2-lightbox-multi-delete-list").html("");
				$('#acle2-lightbox-multi-delete').modal('hide');
			});
			
			$("body").on("hidden.bs.modal", "#acle2-lightbox-multi-delete", function() {
				$("#acle2-lightbox-multi-delete-list").html("");
			});
						
			$("body").on("click", "#acle2-new-rule-add", function() {
				$("#acle2-lightbox-new-rule-alert-area").removeClass("in");
				$("#acle2-lightbox-rule-detail-table > .form-group.has-error").removeClass("form-group has-error");
				if ($(".acle2-new-rule-text").val() != ""){
					addRule($("#acle2-new-rule-desc").val(), $(".acle2-new-rule-text").val());
					$('#acle2-lightbox-new-rule').modal('hide');
					$("#acle2-new-rule-desc").val("");
					$(".acle2-new-rule-text").val("");
				}
				else{
					$("#acle2-lightbox-new-rule-alert-area").addClass("in");
					$(".acle2-new-rule-text").parent().addClass("form-group has-error");
				}
			});
			
			$("body").on("hidden.bs.modal", "#acle2-lightbox-new-rule", function() {
				$("#acle2-lightbox-new-rule-alert-area").removeClass("in");
				$("#acle2-lightbox-rule-detail-table > .form-group.has-error").removeClass("form-group has-error");
				$("#acle2-new-rule-desc").val("");
				$(".acle2-new-rule-text").val("");
			});
			
			$("body").on("click", ".acle2-new-rule-cancel", function(event) {
				$(".acle2-new-access-rule > select").select2("val", "");
			});
						
			$("body").on("click", ".tab", function(event) {
				event.preventDefault();
			});
			
			$("body").on("click", ".acle2-rule-list-entry", function() {
				ruleListInstance.select($(this).attr("ruleid"));
			});
			
			$("body").on("click", "#acle2-button-delete-rule", function() {
				var ruleID = $(this).parents("#acle2-rule-detail-table").find("#acle2-rule-detail-ruleID").html();
				if ($("#acle2-rule-list .acle2-rule-selected[ruleid=" + ruleID + "]").hasClass("acle2-canDelete")){
					removeRule(ruleID);
				}
				else{
					showAlert(geti18n("ACLE.alert.rule.inUse"), false);
				}
			});
			
			$("body").on("click", "#acle2-button-save-rule", function() {
				$("#acle2-rule-detail-table > .form-group.has-error").removeClass("form-group has-error");
				if ($(".acle2-rule-detail-ruleText").val() != ""){
					if ($("#acle2-rule-detail-ruleID").html() == ""){
						addRule($("#acle2-rule-detail-ruleDesc").val(), $(".acle2-rule-detail-ruleText").val());
					}
					else{
						editRule($("#acle2-rule-detail-ruleID").html(), $("#acle2-rule-detail-ruleDesc").val(), $(".acle2-rule-detail-ruleText").val());
					}
				}
				else{
					$(".acle2-rule-detail-ruleText").parent().addClass("form-group has-error");
					showAlert(geti18n("ACLE.alert.rule.noRule"))	
				}
			});
			
			$("body").on("click", "#acle2-button-select-multi-access", function() {
				if ($(this).is(":checked")){
					$(".acle2-access-select:visible").prop("checked", true);
				}
				else{
					$(".acle2-access-select:visible").prop("checked", false);
				}
			});
			
			$("body").on("click", "#acle2-button-access-filter", function() {
				filterTable();
			});
			
			$("body").on("keydown", ".acle2-access-filter-input", function(key) {
				if(key.which == 13) {
					filterTable();
				}
			});
			
			$("body").on("click", ".acle2-button-edit", function() {
				var elm = $(this).parent();
				if(!elm.hasClass("acle2-show-input")){
					var input = $('<input type="text" class="input-sm form-control acle2-table-access-entry-input" value=""></input>');
					elm.html(input);
					input.focus();
					input.val(elm.attr("title"));
					elm.addClass("acle2-show-input");
				}
			});
			
			$("body").on("keydown", ".acle2-table-access-entry-input", function(key) {
				if(key.which == 13) {
					$(".acle2-edit").find(".acle2-show-input").removeClass("form-group has-error");
					var parent = $(this).parent();
					var entry = parent.parent();
					if($(this).val() != parent.attr("title")){
						var json = {
								"accessIDOld": entry.find(".acle2-access-id").attr("title"),
								"accessPoolOld": entry.find(".acle2-access-pool").attr("title"),
								"mode": "idPool",
								"accessIDNew": "",
								"accessPoolNew": "",
								"accessRuleNew": ""						
							};
						 
						json.accessIDNew = entry.find(".acle2-access-id").hasClass("acle2-show-input") ? entry.find(".acle2-access-id input").val() : entry.find(".acle2-access-id").text();
						json.accessPoolNew = entry.find(".acle2-access-pool").hasClass("acle2-show-input") ? entry.find(".acle2-access-pool input").val() : entry.find(".acle2-access-pool").text();
						json.accessRuleNew = entry.find(".acle2-access-rule:not(.select2-container)").val();
						$(".acle2-edit").removeClass("acle2-edit");
						entry.addClass("acle2-edit");
						editAccess(json);
					}
					else{
						parent.html($(this).val( )+ "<i class='glyphicon glyphicon-pencil acle2-icon acle2-button-edit' title='" + geti18n("ACLE.title.edit") + "'></i>");
						parent.attr("title", $(this).val());
						parent.removeClass("acle2-show-input");
						$(this).remove();
					}
				}
				if(key.which == 27) {
					$(".acle2-edit").find(".acle2-show-input").removeClass("form-group has-error");
					var parent = $(this).parent();
					var entry = parent.parent();
					accessTableInstance.edit(entry, entry.find(".acle2-access-id").attr("title"), entry.find(".acle2-access-pool").attr("title"), entry.find(".acle2-access-rule:not(.select2-container)").val());
				}
			});
			
			$("body").on("click", ".sort-table-head", function() {
				if($(this).data("sort-dir") == "asc"){
					$(".glyphicon-chevron-up").removeClass("glyphicon-chevron-up");
					$(".glyphicon-chevron-down").removeClass("glyphicon-chevron-down");
					$(this).children(".sort-icon").addClass("glyphicon-chevron-up")
				}
				else{
					$(".glyphicon-chevron-up").removeClass("glyphicon-chevron-up");
					$(".glyphicon-chevron-down").removeClass("glyphicon-chevron-down");
					$(this).children(".sort-icon").addClass("glyphicon-chevron-down")
				}
			});
			
			$("body").on("keydown", "#acle2-elem-per-page", function(key) {
				if(key.which == 13) {
					splitTable();
				}
			});
			
			$("body").on("click", "#acle2-button-filter-rule", function() {
				$("#acle2-access-filter-input-rule").val($(this).parents("#acle2-rule-detail-table").find("#acle2-rule-detail-ruleID").html());
				filterTable();
				$("#acle2-ruleAllocation-tab").tab("show");
			});
						
			$("body").on("click", "#acle2-button-edit-multi-access", function() {
				var elm = $(".acle2-table-access-entry .acle2-access-select:checked").length;
				if (elm > 0){
					$('#acle2-lightbox-multi-edit').modal('show');
					ruleSelectorInstance.append("", $("#acle2-lightbox-multi-edit-select"));
					$("#acle2-lightbox-multi-edit-select .acle2-new-access-rule-option").remove();
					$("#acle2-lightbox-multi-edit-text").html(geti18n("ACLE.labels.access.multiEditElm", elm));
				}
				else{
					showAlert(geti18n("ACLE.alert.access.edit.select.error"));
				}				
			});
			
			$("body").on("click", "#acle2-lightbox-multi-edit-edit", function() {
				$("#acle2-lightbox-multi-edit-alert-area").removeClass("in");
				$("#acle2-lightbox-multi-edit-select").removeClass("form-group has-error");
				if ($("#acle2-lightbox-multi-edit-select select").val() != ""){
					var json = {
							  "access": [],
							};
					$(".acle2-table-access-entry .acle2-access-select:checked").each(function() {
						var access = $(this).parents(".acle2-table-access-entry");
						access.addClass("acle2-multi-edit");
						json.access.push({"accessID": access.find(".acle2-access-id").text(), "accessPool": access.find(".acle2-access-pool").text(), "accessRule": $("#acle2-lightbox-multi-edit-select select").val()});
					});
					editMultiAccess(json);
					$('#acle2-lightbox-multi-edit').modal('hide');
					hideMultiEdit();
				}
				else{
					$("#acle2-lightbox-multi-edit-alert-area").addClass("in");
					$("#acle2-lightbox-multi-edit-select").addClass("form-group has-error");
				}
			});
			
			$("body").on("click", "#acle2-lightbox-multi-edit-plus", function() {
				if ($("#acle2-lightbox-multi-edit-list:visible").length == 0){
					$(".acle2-table-access-entry .acle2-access-select:checked").each(function() {						
						var access = $(this).parents(".acle2-table-access-entry");
						var p = $("<p></p>");
						p.html(access.find(".acle2-access-id").text() + " : " + access.find(".acle2-access-pool").text() + " : " + access.find(".acle2-access-rule:not(.select2-container)").val()); 
						$("#acle2-lightbox-multi-edit-list").append(p);
					});
					$("#acle2-lightbox-multi-edit-list").show();
					$("#acle2-lightbox-multi-edit-plus").addClass("glyphicon-minus");
					$("#acle2-lightbox-multi-edit-plus").removeClass("glyphicon-plus");
				}
				else{
					$("#acle2-lightbox-multi-edit-list").html("");
					$("#acle2-lightbox-multi-edit-list").hide();
					$("#acle2-lightbox-multi-edit-plus").addClass("glyphicon-plus");
					$("#acle2-lightbox-multi-edit-plus").removeClass("glyphicon-minus");
				}
			});
			
			$("body").on("hidden.bs.modal", "#acle2-lightbox-multi-edit", function() {
				hideMultiEdit();
			});
			
			$("body").on("click", ".acle2-button-filter-access", function() {
				ruleListInstance.select($(this).parents(".acle2-table-access-entry").find("select.acle2-access-rule").val());
				$("#acle2-rules-tab").tab("show");
				$('#acle2-rule-list').animate({scrollTop: $('#acle2-rule-list > .acle2-rule-selected').index() * $('#acle2-rule-list > .acle2-rule-selected').outerHeight()});
			});
			
			ruleSelectorInstance = ruleSelect;
			accessTableInstance = accessTable;
			ruleListInstance = ruleList;
			var lang = $("#mycore-acl-editor2").attr("lang");
			jQuery.getJSON("../../servlets/MCRLocaleServlet/" + lang + "/ACLE.*", function(data) { 
				i18nKeys = data;
				replacei18n();
				getAccess();
			});
			//Fix: Select2 doesn't work when embedded in a bootstrap modal
			$.fn.modal.Constructor.prototype.enforceFocus = function() {};
		}
	}
	
	function getAccess(){
		$.ajax({
			url: ".",
			type: "GET",
			dataType: "json",
			success: function(data) {
						ruleSelectorInstance.init(data.rules, i18nKeys);
						accessTableInstance.init(data, i18nKeys, ruleSelectorInstance);
						ruleListInstance.init(data.rules, i18nKeys);
						$("#acle2-access-table").bind('aftertablesort', function () {
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
			url: ".",
			type: "POST",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify({accessID: accessID, accessPool: accessPool, rule: rule}),
			statusCode: {
				200: function() {
					accessTableInstance.add(accessID, accessPool, rule, true);
					splitTable();
					$("#acle2-new-access-id").val("");
					$("#acle2-new-access-pool").val("");
					$(".acle2-new-access-rule > select").select2("val", "");
					ruleListInstance.updateCanDelete();
					showAlert(geti18n("ACLE.alert.access.add.success", accessID), true);
				},
				409: function() {
					showAlert(geti18n("ACLE.alert.access.add.exist"));
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.access.add.error"));
				}
			}
		});
	}
	
	function editAccess(json){
		$.ajax({
			url: ".",
			type: "PUT",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify(json),
			statusCode: {
				200: function() {
					accessTableInstance.edit($(".acle2-edit"), json.accessIDNew, json.accessPoolNew, json.accessRuleNew);
					ruleListInstance.updateCanDelete();
					ruleListInstance.select($(".acle2-rule-selected").attr("ruleid"));
					showAlert(geti18n("ACLE.alert.access.edit.success"), true);
					$(".acle2-edit").removeClass("acle2-edit");
				},
				409: function() {
					$(".acle2-edit").find(".acle2-show-input").addClass("form-group has-error");
					showAlert(geti18n("ACLE.alert.access.add.exist"));
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.access.edit.error"));
				}
			}
		});
	}
	
	function editMultiAccess(json){
		$.ajax({
			url: "multi",
			type: "PUT",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify(json),
			statusCode: {
				200: function(data) {
					if(accessTableInstance.editMulti(data)){
						showAlert(geti18n("ACLE.alert.access.edit.success"), true);
					}
					else{
						showAlert(geti18n("ACLE.alert.access.edit.multi.error"));
					}
					ruleListInstance.updateCanDelete();
					uncheckAccessSelect();
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.access.edit.error"));
				}
			}
		});
	}
	
	function removeAccess(json){
		$.ajax({
			url: ".",
			type: "DELETE",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify(json),
			statusCode: {
				200: function(data) {
					if(accessTableInstance.remove(data)){
						showAlert(geti18n("ACLE.alert.access.remove.success"), true);
					}
					else{
						showAlert(geti18n("ACLE.alert.access.remove.errorElm"));
						uncheckAccessSelect();
					}
					splitTable();
					ruleListInstance.updateCanDelete();
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.access.remove.error"));					
				}
			}
		});
	}

	function uncheckAccessSelect(){
		$(".acle2-access-select:checked").prop("checked", false);
		$('#acle2-button-select-multi-access:checked').prop("checked", false);
	}
	
	function addRule(ruleDesc, ruleText){
		$.ajax({
			url: "rule",
			type: "POST",
			contentType: 'application/json',
			dataType: "text",
			data: JSON.stringify({ruleDesc: ruleDesc, ruleText: ruleText}),
			statusCode: {
				200: function(ruleID) {
					if(ruleID != ""){
						ruleSelectorInstance.add(ruleID, ruleDesc, ruleText);
						ruleSelectorInstance.update();
						$(".acle2-new-access-rule > select").select2("val", ruleID);
						$(".acle2-new-access-rule > select").siblings("div.acle2-access-rule").attr("title", $(".acle2-new-access-rule > select").children("option:selected").attr("title"));
						ruleListInstance.add(ruleID, ruleDesc, ruleText);
						ruleListInstance.select(ruleID);
						$('#acle2-rule-list').animate({scrollTop : $('#acle2-rule-list').height()});
						showAlert(geti18n("ACLE.alert.rule.add.success", ruleDesc, ruleID), true);
					}
					else{
						showAlert(geti18n("ACLE.alert.rule.add.error"));
						$(".acle2-new-access-rule > select").select2("val", "");
					}
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.rule.add.error"));
					$(".acle2-new-access-rule > select").select2("val", "");
				}
			}
		});
	}
	
	function removeRule(ruleID){
		$.ajax({
			url: "rule",
			type: "DELETE",
			contentType: 'application/json',
			dataType: "text",
			data: JSON.stringify({ruleID: ruleID}),
			statusCode: {
				200: function(data) {
					if ($(".acle2-new-access-rule > select").val() == ruleID){
						$(".acle2-new-access-rule > select").select2("val", "");
					}
					ruleSelectorInstance.remove(ruleID);
					ruleSelectorInstance.update();
					ruleListInstance.remove(ruleID);
					ruleListInstance.select("");
					$('#acle2-rule-list').animate({scrollTop : 0},'fast');
					showAlert(geti18n("ACLE.alert.rule.remove.success", ruleID), true);
				},
				409: function(ruleID) {
					showAlert(geti18n("ACLE.alert.rule.remove.notExist"));
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.rule.remove.error"));
				}
			}
		});
	}
	
	function editRule(ruleID, ruleDesc, ruleText){
		$.ajax({
			url: "rule",
			type: "PUT",
			contentType: 'application/json',
			dataType: "text",
			data: JSON.stringify({ruleID: ruleID, ruleDesc: ruleDesc, ruleText: ruleText}),
			statusCode: {
				200: function() {
					ruleSelectorInstance.edit(ruleID, ruleDesc, ruleText);
					ruleSelectorInstance.update();
					ruleListInstance.edit(ruleID, ruleDesc, ruleText);
					showAlert(geti18n("ACLE.alert.rule.edit.success", ruleDesc), true);
				},
				409: function(error) {
					showAlert(geti18n("ACLE.alert.rule.edit.error"));
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.rule.edit.error"));
				}
			}
		});
	}

	function filterTable() {
		$(".acle2-table-access-entry").addClass("acle2-filter-hide");
		var filterID = $("#acle2-access-filter-input-id").val();
		filterID = filterID.replace("*", ".*");
		filterID = filterID.replace("?", ".?");
		filterID = filterID.replace("(", "\\(");
		filterID = filterID.replace(")", "\\)");
		
		if(filterID != ""){
			$(".acle2-table-access-entry")
		    .filter(function() {
		        return $(this).find(".acle2-access-id").attr("title").match(new RegExp("^" + filterID, "i"));
		    })
		    .removeClass("acle2-filter-hide");
		}
		
		var filterPool = $("#acle2-access-filter-input-pool").val();
		filterPool = filterPool.replace("*", ".*");
		filterPool = filterPool.replace("?", ".?");
		filterPool = filterPool.replace("(", "\\(");
		filterPool = filterPool.replace(")", "\\)");
		
		if(filterPool != ""){
			$(".acle2-table-access-entry")
		    .filter(function() {
		        return $(this).find(".acle2-access-pool").attr("title").match(new RegExp("^" + filterPool, "i"));
		    })
		    .removeClass("acle2-filter-hide");
		}
		
	    var filterRule = $("#acle2-access-filter-input-rule").val();
		filterRule = filterRule.replace("*", ".*");
		filterRule = filterRule.replace("?", ".?");
		filterRule = filterRule.replace("(", "\\(");
		filterRule = filterRule.replace(")", "\\)");
		
	    if(filterRule != ""){
			$(".acle2-table-access-entry")
		    .filter(function() {
		        return $(this).find(".acle2-access-rule option:selected").val().match(new RegExp("^" + filterRule, "i"));
		    })
		    .removeClass("acle2-filter-hide");
			
			$(".acle2-table-access-entry")
		    .filter(function() {
		        return $(this).find(".acle2-access-rule option:selected").html().match(new RegExp("^" + filterRule, "i"));
		    })
		    .removeClass("acle2-filter-hide");
	    }
	    if(filterID == "" && filterPool == "" && filterRule == ""){
	    	$(".acle2-table-access-entry").removeClass("acle2-filter-hide");
	    }
	    splitTable();
		accessTableInstance.zebra();
	}
		
	function showAlert(text, success) {
		$('#acle2-alert-area').removeClass("in");
		$("#acle2-alert-area").removeClass("alert-success");
		$("#acle2-alert-area").removeClass("alert-danger");
		if (timeOutID != null){
			window.clearTimeout(timeOutID);
		}

		$("#acle2-alert-area").html(text);
		if (success){
			$("#acle2-alert-area").addClass("alert-success");
			$("#acle2-alert-area").addClass("in");
		}
		else{
			$("#acle2-alert-area").addClass("alert-danger");
			$("#acle2-alert-area").addClass("in");
		}
		timeOutID = window.setTimeout(function() {
				$('#acle2-alert-area').removeClass("in")
				$("#acle2-alert-area").removeClass("alert-success");
				$("#acle2-alert-area").removeClass("alert-danger");
			}, 5000);
	}
	
	function addTypeahead() {
		var ids = new Array();
		$.each($(".acle2-access-id"), function() {
			if($.inArray($(this).attr("title"), ids) == -1){
				ids.push($(this).attr("title"));
			}
		});
		$("#acle2-access-filter-input-id").typeahead({
			  name: 'access-ids',
              source: ids
			});
		
		var pools = new Array();
		$.each($(".acle2-access-pool"), function() {
			if($.inArray($(this).attr("title"), pools) == -1){
				pools.push($(this).attr("title"));
			}
		});
		$("#acle2-access-filter-input-pool").typeahead({
			  name: 'access-pools',
              source: pools
			});
		
		var rules = new Array();
		$.each($(".acle2-access-rule option:selected"), function() {
			if($.inArray($(this).val(), rules) == -1){
				rules.push($(this).val());
			}
			if($.inArray($(this).html(), rules) == -1){
				rules.push($(this).html());
			}
		});
		$("#acle2-access-filter-input-rule").typeahead({
			  name: 'access-rules',
			  source: rules
			});
	}
	
	function splitTable() {
		var elemPerPage = $("#acle2-elem-per-page-input").val();
		$("#acle2-access-table tbody tr:not(.acle2-filter-hide)").each(function(i, row){
			page = Math.floor(i / elemPerPage) + 1;
			$(row).attr("data-page", page);
		});
		buildPaginator(1);
		$("#acle2-access-table tbody tr:not(.acle2-filter-hide)").addClass("acle2-page-hide");
		$("[data-page=1]").removeClass("acle2-page-hide");
	}
	
	function buildPaginator(page) {
		$(".pagination").html("");
		pagecount = Math.ceil($("#acle2-access-table tbody tr:not(.acle2-filter-hide)").size() / $("#acle2-elem-per-page-input").val());
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
			else{
				addPageToPaginator(1, 1, "active");
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
		$("<li></li>").append(pageButton).addClass(state).appendTo(".pagination");
	}
	
	function refreshPageNumbers() {
		var elemPerPage = $("#acle2-elem-per-page-input").val();
		$("#acle2-access-table tbody tr:not(.acle2-filter-hide)").each(function(i, row){
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
		$("#acle2-access-table tbody tr:not(.acle2-filter-hide)").addClass("acle2-page-hide");
		$(".acle2-access-select:checked").parents(".acle2-table-access-entry").removeClass("acle2-page-hide");
		$("[data-page=" + num + "]").removeClass("acle2-page-hide");
	}
	
	function formatSelect(item) {
		var span = $("<span></span>")
		span.text($(item.element).text());
		span.attr("title", $(item.element).attr("title"));
		return span;
	}
	
	function hideMultiEdit() {
		$("#acle2-lightbox-multi-edit-alert-area").removeClass("in");
		$("#acle2-lightbox-multi-edit-select").removeClass("form-group has-error");
		$("#acle2-lightbox-multi-edit-list").html("");
		$("#acle2-lightbox-multi-edit-list").hide();
		$("#acle2-lightbox-multi-edit-select").find("select").select2("destroy");
		$("#acle2-lightbox-multi-edit-select").find("select").remove();
		$("#acle2-lightbox-multi-edit-plus").addClass("glyphicon-plus");
		$("#acle2-lightbox-multi-edit-plus").removeClass("glyphicon-minus");
	}
	
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

    function replacei18n() {
		$(".i18n").each(function () {
            var placeholder = $(this).attr("placeholder")
			if (placeholder !== undefined && placeholder !== false && placeholder !== "") {
				if (placeholder.indexOf("i18n:") > -1) {
                    $(this).attr("placeholder", geti18n(placeholder.split(":")[1]));
				}
			}
        });
        $("text[i18n]").each(function () {
        	var key = $(this).attr("i18n");
            if (key !== undefined && key !== "") {
                $(this).html(geti18n(key));
            }
        });
    }
}

$(document).ready(function() {
	var aclEditorInstance = new ACLEditor();
	aclEditorInstance.init(new RuleSelector(), new AccessTable(), new RuleList());
});
