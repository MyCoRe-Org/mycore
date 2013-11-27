var ACLEditorObjectID = function(){
	var timeOutID = null;
	var i18nKeys = [];
	var qpara = [], hash;
	
	return {
		init: function(i18n){
			i18nKeys = i18n;
			$("body").on("change", "#acle2-access-input-rule", function() {
				if($(this).children("option:selected").val() == "new"){
					$('#acle2-lightbox-new-rule').modal('show');
				}
			});
			$("body").on("click", "#acle2-new-rule-add", function() {
				$("#acle2-lightbox-new-rule-alert-area").removeClass("in");
				$("#acle2-lightbox-rule-detail-table > .form-group.has-error").removeClass("form-group has-error");
				if ($(".acle2-new-rule-text").val() != ""){
					addRule($("#acle2-new-rule-desc").val(), $(".acle2-new-rule-text").val())
					$('#acle2-lightbox-new-rule').modal('hide');
					$("#acle2-new-rule-desc").val("");
					$(".acle2-new-rule-text").val("");
				}
				else{
					$("#acle2-lightbox-new-rule-alert-area").addClass("in");
					$(".acle2-new-rule-text").parent().addClass("form-group has-error");
				}
			});
			
			$("body").on("click", ".acle2-new-rule-cancel", function() {
				$("#acle2-new-access-rule").select2("val", "");
				$("#acle2-lightbox-new-rule-alert-area").removeClass("in");
				$("#acle2-lightbox-rule-detail-table > .form-group.has-error").removeClass("form-group has-error");
				$("#acle2-new-rule-desc").val("");
				$(".acle2-new-rule-text").val("");
				$("#acle2-access-input-rule").select2("val", "");
			});
			
			$("body").on("click", "#acle2-objectid-save", function() {
				$("#acle2-table-new-access > .form-group.has-error").removeClass("form-group has-error");
				var accessRule = $("#acle2-access-input-rule").val();
				
				if(accessRule != "" && accessRule != "new"){
					if(qpara["cmd"] == "add"){
						addAccess(decodeURIComponent(qpara["objId"]), qpara["perm"] , accessRule);
					}
					if(qpara["cmd"] == "edit"){
						var access = $("#access-input");
						var json = {
									"accessIDOld": decodeURIComponent(qpara["objId"]),
									"accessPoolOld": qpara["perm"],
									"mode": "rule",
									"accessIDNew": decodeURIComponent(qpara["objId"]),
									"accessPoolNew": qpara["perm"],
									"accessRuleNew": accessRule						
								};
						editAccess(json); 
					}
				}
				else{
					showAlert(geti18n("ACLE.alert.access.fields"));
					if (accessRule == "" || accessRule == "new"){
						$("#acle2-new-access-rule").parent().addClass("form-group has-error");
					}
				}
			});
			
			$("body").on("click", "#acle2-objectid-delete", function() {
				var json = {
						  "access": [],
						};
				json.access.push({"accessID": decodeURIComponent(qpara["objId"]), "accessPool": qpara["perm"]});
				removeAccess(json); 
			});
			
			$("body").on("click", "#acle2-objectid-cancel", function() {
				window.location.replace(decodeURIComponent(qpara["redir"]));
			});									
			
			readQueryParameter();
			
			if (qpara["cmd"] == "add" || qpara["cmd"] == "edit" ){
				$("#acle2-access-id-text").html(decodeURIComponent(qpara["objId"]));
				$("#acle2-access-pool-text").html(qpara["perm"]);
				$("#acle2-objectid-save").show();
				getObjectID(decodeURIComponent(qpara["objId"]), qpara["perm"]);
				$("#acle2-loading").show();
			}
			else{
				if(qpara["cmd"] == "delete"){
					$("#acle2-access-delete").append('<pre>'+ geti18n("ACLE.labels.access.deleteAll", decodeURIComponent(qpara["objId"]), qpara["perm"]) + '</pre>');
					$("#acle2-access-delete").show();
					$("#acle2-objectid-delete").show();
				}
			}
		}
	}
	
	function getObjectID(accessID, accessPool){
		$.ajax({
			url: "objectid",
			type: "POST",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify({accessID: accessID, accessPool: accessPool}),
			success: function(data) {
				if(data.accessRuleID != "null"){
					buildRuleSelector(data, data.accessRuleID);
					$("#acle2-loading").hide();
					$("#acle2-access-text").show();
				}
				else{
					buildRuleSelector(data, "");
					$("#acle2-loading").hide();
					$("#acle2-access-text").show();
				}
			},
			error: function(error) {
				showAlert(geti18n("ACLE.alert.access.edit.error"));
				$("#acle2-loading").hide();
			}
		});
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
						$(".select2-container").select2("destroy");
						$("#acle2-access-input-rule-option").remove();
						$("#acle2-access-input-rule").append("<option class='acle2-access-rule-option' title='" + ruleText + "' value='" + ruleID + "'>" +  ruleDesc + " (" + ruleID + ")</option>");
						$("#acle2-access-input-rule").append("<option value='new' title=''>" + geti18n("ACLE.select.newRule") + "</option>");
						$("#acle2-access-input-rule").val(ruleID);
						$("#acle2-access-input-rule").select2({
							matcher: function(term, text, opt) {
								return text.toUpperCase().indexOf(term.toUpperCase())>=0
									|| opt.attr("title").toUpperCase().indexOf(term.toUpperCase())>=0;
							},
							formatResult: formatSelect
						});
						showAlert(geti18n("ACLE.alert.rule.add.success", ruleDesc, ruleID), true);
					}
					else{
						showAlert(geti18n("ACLE.alert.rule.add.error"));
						$("#acle2-access-input-rule").select2("val", "");
					}
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.rule.add.error"));
					$("#acle2-access-input-rule").select2("val", "");
				}
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
					window.location.replace(decodeURIComponent(qpara["redir"]));
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
					window.location.replace(decodeURIComponent(qpara["redir"]));
				},
				409: function(error) {
					showAlert(geti18n("ACLE.alert.access.edit.error"));
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
					if (data.access[0].success == 1){
						window.location.replace(decodeURIComponent(qpara["redir"]));
					}
					else{
						showAlert(geti18n("ACLE.alert.access.remove.error"));
					}
				},
				500: function(error) {
					showAlert(geti18n("ACLE.alert.access.remove.error"));					
				}
			}
		});
	}
	
	function buildRuleSelector(data, select) {
		var ruleSelector = $("#acle2-access-input-rule");
		$.each(data.rules, function(i, l) {
			ruleSelector.append("<option class='acle2-access-rule-option' title='" + l.ruleSt + "' value='" + l.ruleID + "'>" +  l.desc + " (" + l.ruleID + ")</option>");
		});
		ruleSelector.prepend("<option value='' title='' selected>" + geti18n("ACLE.select.select") + "</option>");
		ruleSelector.append("<option id='acle2-access-input-rule-option' value='new' title=''>" + geti18n("ACLE.select.newRule") + "</option>");
		ruleSelector.val(select);
		ruleSelector.select2({
			matcher: function(term, text, opt) {
				return text.toUpperCase().indexOf(term.toUpperCase())>=0
					|| opt.attr("title").toUpperCase().indexOf(term.toUpperCase())>=0;
			},
			formatResult: formatSelect
		});
		ruleSelector.show();
	}
	
	function formatSelect(item) {
		var span = $("<span></span>")
		span.text($(item.element).text());
		span.attr("title", $(item.element).attr("title"));
		return span;
	}
	
	function readQueryParameter() {
		var q = document.URL.split(/\?(.+)?/)[1];
		if(q != undefined){
	        q = q.split('&');
	        for(var i = 0; i < q.length; i++){
	            hash = q[i].split(/=(.+)?/);
	            qpara.push(hash[1]);
	            qpara[hash[0]] = hash[1];
	        }
		}
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

$(document).ready(function() {
    var aclEditorObjectIDInstance = new ACLEditorObjectID();
    var lang = $("#mycore-acl-editor2").attr("lang");
	if (!$.isFunction(jQuery.fn.modal)){
    	$.getScript('gui/js/bootstrap.min.js')
    		.done(function() {
    			jQuery.getJSON("../../servlets/MCRLocaleServlet/" + lang + "/ACLE.*", function(data) { 
    				aclEditorObjectIDInstance.init(data);
    			});
    		});
    }
    else{
    	jQuery.getJSON("../../servlets/MCRLocaleServlet/" + lang + "/ACLE.*", function(data) { 
    		aclEditorObjectIDInstance.init(data);
    	});
    }
});


