var RuleList = function(){
	var i18nKeys = [];
	
	function canDelete(ruleID){
		var result = true;
		$(".access-rule-parent > .access-rule option:selected").each(function() {
			if ($(this).attr("value") == ruleID){
				result = false;
				return false;
			}
		});
		return result;
	}
	
	return {
		init: 	function(rules, i18n){
			i18nKeys = i18n;
			var cla = this;
			$.each(rules, function(i, l) {
				cla.add(l.ruleID, l.desc, l.ruleSt);
			});
			var li = $("<li class='rule-list-entry rule-selected'></li>");
			li.attr("ruleID", "");
			li.attr("ruleDesc", "");
			li.attr("ruleText", "");
			li.text(i18nKeys["ACLE.list.rule.newRule"]);
			li.prependTo("#rule-list");
			$("#rule-detail-ruleID").hide();
			$("#rule-detail-ruleID").prev().hide();
			$("#button-delete-rule").hide();
			$("#button-filter-rule").hide();
		},
		add: 	function(ruleID, ruleDesc, ruleText){
			var li = $("<li class='rule-list-entry'></li>");
			li.attr("ruleID", ruleID);
			li.attr("ruleDesc", ruleDesc);
			li.attr("ruleText", ruleText);
			li.text(ruleDesc + " (" + ruleID + ")");
			if(canDelete(ruleID)){
				li.addClass("canDelete");
			}
			li.appendTo("#rule-list");
		},
		remove:	function(ruleID) {
			$('#rule-list li[ruleid="' + ruleID + '"]').remove();
		},
		edit: function(ruleID, ruleDesc, ruleText) {			
			$('.rule-list-entry[ruleid="' + ruleID + '"]').attr("ruledesc", ruleDesc);
			$('.rule-list-entry[ruleid="' + ruleID + '"]').attr("ruletext", ruleText);
			$('.rule-list-entry[ruleid="' + ruleID + '"]').html(ruleDesc + " (" + ruleID + ")");
		},
		select: function(ruleID) {
			$("#rule-detail-table > .control-group.error").removeClass("control-group error");
			$(".faded").removeClass("faded");
			var entry = $('#rule-list li[ruleid="' + ruleID + '"]')
			if (ruleID != ""){
				$("#rule-detail-ruleID").show();
				$("#rule-detail-ruleID").prev().show();
				if (canDelete(ruleID)){
					$("#button-filter-rule").addClass("faded");
				}
				else{
					$("#button-delete-rule").addClass("faded");
				}
				$("#button-delete-rule").show();
				$("#button-filter-rule").show();
			}
			else{
				$("#rule-detail-ruleID").hide();
				$("#rule-detail-ruleID").prev().hide();
				$("#button-delete-rule").hide();
				$("#button-filter-rule").hide();
			}
			$("#rule-detail-ruleID").html(ruleID);
			$("#rule-detail-ruleDesc").val(entry.attr("ruledesc"));
			$(".rule-detail-ruleText").val(entry.attr("ruletext"));
			$(".rule-selected").removeClass("rule-selected");
			entry.addClass("rule-selected");
		},
		updateCanDelete: function() {
			$(".rule-list-entry").removeClass("canDelete");
			$(".rule-list-entry").each(function(i) {
				var ruleID = $(this).attr("ruleID");
				if (canDelete(ruleID) && ruleID != ""){
					$(this).addClass("canDelete");
				}
			});
		}
	};
}