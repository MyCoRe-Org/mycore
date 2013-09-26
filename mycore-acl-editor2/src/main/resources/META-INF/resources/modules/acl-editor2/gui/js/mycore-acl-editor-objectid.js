var ACLEditorObjectID = function(){
	var timeOutID = null;
	var i18nKeys = [];
	var qpara = [], hash;
	
	return {
		init: function(i18n){
			i18nKeys = i18n;
			$("body").on("change", "#access-input-rule", function() {
				if($(this).children("option:selected").val() == "new"){
					$('#lightbox-new-rule').modal('show');
				}
			});
			$("body").on("click", "#new-rule-add", function() {
				$("#lightbox-new-rule-alert-area").removeClass("in");
				$("#lightbox-rule-detail-table > .form-group.has-error").removeClass("form-group has-error");
				if ($(".new-rule-text").val() != ""){
					addRule($("#new-rule-desc").val(), $(".new-rule-text").val())
					$('#lightbox-new-rule').modal('hide');
					$("#new-rule-desc").val("");
					$(".new-rule-text").val("");
				}
				else{
					$("#lightbox-new-rule-alert-area").addClass("in");
					$(".new-rule-text").parent().addClass("form-group has-error");
				}
			});
			
			$("body").on("click", ".new-rule-cancel", function() {
				$("#new-access-rule").select2("val", "");
				$("#lightbox-new-rule-alert-area").removeClass("in");
				$("#lightbox-rule-detail-table > .form-group.has-error").removeClass("form-group has-error");
				$("#new-rule-desc").val("");
				$(".new-rule-text").val("");
				$("#access-input-rule").select2("val", "");
			});
			
			$("body").on("click", "#objectid-save", function() {
				$("#table-new-access > .form-group.has-error").removeClass("form-group has-error");
				var accessRule = $("#access-input-rule").val();
				
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
					showAlert(i18nKeys["ACLE.alert.access.fields"]);
					if (accessRule == "" || accessRule == "new"){
						$("#new-access-rule").parent().addClass("form-group has-error");
					}
				}
			});
			
			$("body").on("click", "#objectid-delete", function() {
				var json = {
						  "access": [],
						};
				json.access.push({"accessID": decodeURIComponent(qpara["objId"]), "accessPool": qpara["perm"]});
				removeAccess(json); 
			});
			
			$("body").on("click", "#objectid-cancel", function() {
				window.location.replace(decodeURIComponent(qpara["redir"]));
			});									
			
			readQueryParameter();
			
			if (qpara["cmd"] == "add" || qpara["cmd"] == "edit" ){
				console.log("add/edit");
				$("#access-id-text").html(decodeURIComponent(qpara["objId"]));
				$("#access-pool-text").html(qpara["perm"]);
				$("#objectid-save").show();
				getObjectID(decodeURIComponent(qpara["objId"]), qpara["perm"]);
				$("#loading").show();
			}
			else{
				if(qpara["cmd"] == "delete"){
					$("#access-delete").append('<pre>'+ i18nKeys["ACLE.labels.access.delete.1"] + decodeURIComponent(qpara["objId"]) + i18nKeys["ACLE.labels.access.delete.2"] + qpara["perm"] + i18nKeys["ACLE.labels.access.delete.3"] + '</pre>');
					$("#access-delete").show();
					$("#objectid-delete").show();
				}
			}
		}
	}
	
	function getObjectID(accessID, accessPool){
		$.ajax({
			url: "/rsc/ACLE/objectid",
			type: "POST",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify({accessID: accessID, accessPool: accessPool}),
			success: function(data) {
				if(data.accessRuleID != "null"){
					buildRuleSelector(data, data.accessRuleID);
					$("#loading").hide();
					$("#access-text").show();
				}
				else{
					buildRuleSelector(data, "");
					$("#loading").hide();
					$("#access-text").show();
				}
			},
			error: function(error) {
				showAlert(i18nKeys["ACLE.alert.access.edit.error"]);
				$("#loading").hide();
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
					if(ruleID != ""){
						$(".select2-container").select2("destroy");
						$("#access-input-rule-option").remove();
						$("#access-input-rule").append("<option class='access-rule-option' title='" + ruleText + "' value='" + ruleID + "'>" +  ruleDesc + " (" + ruleID + ")</option>");
						$("#access-input-rule").append("<option id='new-access-rule-option' value='new' title=''>" + i18nKeys["ACLE.select.newRule"] + "</option>");
						$("#access-input-rule").val(ruleID);
						$("#access-input-rule").select2({
							matcher: function(term, text, opt) {
								return text.toUpperCase().indexOf(term.toUpperCase())>=0
									|| opt.attr("title").toUpperCase().indexOf(term.toUpperCase())>=0;
							},
							formatResult: formatSelect
						});
						showAlert(i18neys["ACLE.alert.rule.add.success.1"] + ruleDesc + i18nKeys["ACLE.alert.rule.add.success.2"] + ruleID + i18nKeys["ACLE.alert.rule.add.success.3"], true);
					}
					else{
						showAlert(i18nKeys["ACLE.alert.rule.add.error"]);
						$("#access-input-rule").select2("val", "");
					}
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.rule.add.error"]);
					$("#access-input-rule").select2("val", "");
				}
			}
		});
	}
		
	function addAccess(accessID, accessPool, rule){
		console.log(accessID);
		console.log(accessPool);
		console.log(rule);
		$.ajax({
			url: "/rsc/ACLE",
			type: "POST",
			contentType: 'application/json',
			dataType: "json",
			data: JSON.stringify({accessID: accessID, accessPool: accessPool, rule: rule}),
			statusCode: {
				200: function() {
					window.location.replace(decodeURIComponent(qpara["redir"]));
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
					window.location.replace(decodeURIComponent(qpara["redir"]));
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
					window.location.replace(decodeURIComponent(qpara["redir"]));
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.access.remove.error"]);					
				}
			}
		});
	}
	
	function buildRuleSelector(data, select) {
		var ruleSelector = $("#access-input-rule");
		$.each(data.rules, function(i, l) {
			ruleSelector.append("<option class='access-rule-option' title='" + l.ruleSt + "' value='" + l.ruleID + "'>" +  l.desc + " (" + l.ruleID + ")</option>");
		});
		ruleSelector.prepend("<option value='' title='' selected>" + i18nKeys["ACLE.select.select"] + "</option>");
		ruleSelector.append("<option id='access-input-rule-option' value='new' title=''>" + i18nKeys["ACLE.select.newRule"] + "</option>");
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
		$('#alert-area').removeClass("in");
		$("#alert-area").removeClass("alert-success");
		$("#alert-area").removeClass("alert-danger");
		if (timeOutID != null){
			window.clearTimeout(timeOutID);
		}
		$("#alert-area").html(text);
		if (success){
			$("#alert-area").addClass("alert-success");
			$("#alert-area").addClass("in");
		}
		else{
			$("#alert-area").addClass("alert-danger");
			$("#alert-area").addClass("in");
		}
		timeOutID = window.setTimeout(function() {
				$('#alert-area').removeClass("in")
				$("#alert-area").removeClass("alert-success");
				$("#alert-area").removeClass("alert-danger");
			}, 5000);
	}
}

$(document).ready(function() {
    var aclEditorObjectIDInstance = new ACLEditorObjectID();
    var lang = $("#mycore-acl-editor2").attr("lang");
	if (!$.isFunction(jQuery.fn.modal)){
    	$.getScript('/rsc/ACLE/gui/js/bootstrap.min.js')
    		.done(function() {
    			console.log("bootstrap.min.js loaded");
    			jQuery.getJSON("/servlets/MCRLocaleServlet/" + lang + "/ACLE.*", function(data) { 
    				aclEditorObjectIDInstance.init(data);
    			});
    		});
    }
    else{
    	jQuery.getJSON("/servlets/MCRLocaleServlet/" + lang + "/ACLE.*", function(data) { 
    		aclEditorObjectIDInstance.init(data);
    	});
    }
});


