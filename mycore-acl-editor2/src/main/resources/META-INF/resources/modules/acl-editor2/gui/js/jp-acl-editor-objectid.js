var ACLEditorObjectID = function(){
	var timeOutID = null;
	var i18nKeys =[];
	
	return {
		init: function(i18n){
			i18nKeys = i18n;
			$("body").on("change", "#access-input-rule", function() {
				var access = $("#access-input");
				var json = {
							"accessIDOld": access.find("#access-input-id").val(),
							"accessPoolOld": access.find("#access-input-pool").val(),
							"mode": "rule",
							"accessIDNew": access.find("#access-input-id").val(),
							"accessPoolNew": access.find("#access-input-pool").val(),
							"accessRuleNew": access.find("#access-input-rule").val()						
						};
				editAccess(json); 
			});	
						
			$("body").on("keydown", ".access-filter-input", function(key) {
				if(key.which == 13) {
					$("#access-input > .control-group.error").removeClass("control-group error");
					var accessID = $("#access-input-id").val();
					var accessPool = $("#access-input-pool").val();
					
					if(accessID != "" &&  accessPool != ""){
						getObjectID(accessID, accessPool);
						$("#loading").show();
					}
					else{
						showAlert(i18nKeys["ACLE.alert.access.fields"]);
						if (accessID == ""){
							$("#access-input-id").addClass("control-group error");
						}
						if (accessPool == ""){
							$("#access-input-pool").addClass("control-group error");
						}
					}
				}
			});
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
					showAlert(i18nKeys["ACLE.alert.access.load.success"], true);
					$("#loading").hide();
					buildRuleSelector(data);
				}
				else{
					showAlert(i18nKeys["ACLE.alert.access.load.notFound"]);
					$("#loading").hide();
				}
			},
			error: function(error) {
				showAlert(i18nKeys["ACLE.alert.access.edit.error"]);
				$("#loading").hide();
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
					showAlert(i18nKeys["ACLE.alert.access.edit.success"], true);
				},
				500: function(error) {
					showAlert(i18nKeys["ACLE.alert.access.edit.error"]);
				}
			}
		});
	}

	
	function buildRuleSelector(data) {
		var ruleSelector = $("#access-input-rule");
		$.each(data.rules, function(i, l) {
			ruleSelector.append("<option class='access-rule-option' title='" + l.ruleSt + "' value='" + l.ruleID + "'>" +  l.desc + " (" + l.ruleID + ")</option>");
		});
		ruleSelector.val(data.accessRuleID);
		ruleSelector.select2();
		ruleSelector.show();
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
}

$(document).ready(function() {
    var aclEditorObjectIDInstance = new ACLEditorObjectID();
    var lang = $("#languageSelect").children("img:first").attr("alt");
	if (lang != "en") lang = "de";
	jQuery.getJSON("/servlets/MCRLocaleServlet/" + lang + "/ACLE.*", function(data) { 
		aclEditorObjectIDInstance.init(data);
	});
});


