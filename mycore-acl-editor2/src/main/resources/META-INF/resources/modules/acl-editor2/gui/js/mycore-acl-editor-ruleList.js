/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

var RuleList = function(){
	var i18nKeys = [];
	
	function canDelete(ruleID){
		var result = true;
		$(".acle2-access-rule-parent > .acle2-access-rule option:selected").each(function() {
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
			var li = $("<li class='acle2-rule-list-entry acle2-rule-selected'></li>");
			li.attr("ruleID", "");
			li.attr("ruleDesc", "");
			li.attr("ruleText", "");
			li.text(geti18n("ACLE.list.rule.newRule"));
			li.prependTo("#acle2-rule-list");
			$("#acle2-rule-detail-ruleID").hide();
			$("#acle2-rule-detail-ruleID").prev().hide();
			$("#acle2-button-delete-rule").hide();
			$("#acle2-button-filter-rule").hide();
		},
		add: 	function(ruleID, ruleDesc, ruleText){
			var li = $("<li class='acle2-rule-list-entry'></li>");
			li.attr("ruleID", ruleID);
			li.attr("ruleDesc", ruleDesc);
			li.attr("ruleText", ruleText);
			li.text(ruleDesc + " (" + ruleID + ")");
			if(canDelete(ruleID)){
				li.addClass("acle2-canDelete");
			}
			li.appendTo("#acle2-rule-list");
		},
		remove:	function(ruleID) {
			$('#acle2-rule-list li[ruleid="' + ruleID + '"]').remove();
		},
		edit: function(ruleID, ruleDesc, ruleText) {			
			$('.acle2-rule-list-entry[ruleid="' + ruleID + '"]').attr("ruledesc", ruleDesc);
			$('.acle2-rule-list-entry[ruleid="' + ruleID + '"]').attr("ruletext", ruleText);
			$('.acle2-rule-list-entry[ruleid="' + ruleID + '"]').html(ruleDesc + " (" + ruleID + ")");
		},
		select: function(ruleID) {
			$("#acle2-rule-detail-table > .control-group.error").removeClass("control-group error");
			$(".acle2-faded").removeClass("acle2-faded");
			var entry = $('#acle2-rule-list li[ruleid="' + ruleID + '"]')
			if (ruleID != ""){
				$("#acle2-rule-detail-ruleID").show();
				$("#acle2-rule-detail-ruleID").prev().show();
				if (canDelete(ruleID)){
					$("#acle2-button-filter-rule").addClass("acle2-faded");
				}
				else{
					$("#acle2-button-delete-rule").addClass("acle2-faded");
				}
				$("#acle2-button-delete-rule").show();
				$("#acle2-button-filter-rule").show();
			}
			else{
				$("#acle2-rule-detail-ruleID").hide();
				$("#acle2-rule-detail-ruleID").prev().hide();
				$("#acle2-button-delete-rule").hide();
				$("#acle2-button-filter-rule").hide();
			}
			$("#acle2-rule-detail-ruleID").html(ruleID);
			$("#acle2-rule-detail-ruleDesc").val(entry.attr("ruledesc"));
			$(".acle2-rule-detail-ruleText").val(entry.attr("ruletext"));
			$(".acle2-rule-selected").removeClass("acle2-rule-selected");
			entry.addClass("acle2-rule-selected");
		},
		updateCanDelete: function() {
			$(".acle2-rule-list-entry").removeClass("acle2-canDelete");
			$(".acle2-rule-list-entry").each(function(i) {
				var ruleID = $(this).attr("ruleID");
				if (canDelete(ruleID) && ruleID != ""){
					$(this).addClass("acle2-canDelete");
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
