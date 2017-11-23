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

var RuleSelector = function(){
	var i18nKeys = [];
	var selector = $('<select/>',
		    {
		        size: 	'1',
		        class:	'acle2-access-rule'
		    });
	
	function formatSelect(item) {
		var span = $("<span></span>")
		span.text($(item.element).text());
		span.attr("title", $(item.element).attr("title"));
		return span;
	}
	
	return {
		init: 	function(rules, i18n){
			i18nKeys = i18n;
			var cla = this;
			$.each(rules, function(i, l) {
				cla.add(l.ruleID, l.desc, l.ruleSt)
			});
		},
		add: 	function(ruleID, ruleDesc, rule){
			selector.append("<option title='" + rule + "' value='" + ruleID + "'>" + ruleDesc + " (" + ruleID + ")</option>");
		},
		remove:	function(ruleID) {
			selector.find('option[value="' + ruleID + '"]').remove();
		},
		edit: function(ruleID, ruleDesc, ruleText) {
			selector.find('option[value="' + ruleID + '"]').attr("title", ruleText).html(ruleDesc + " (" + ruleID + ")");
		},
		update:	function() {
			var cla = this;
			$(".acle2-access-rule:not(.select2-container)").each(function() {
				var ruleID = $(this).select2("val") ;
				$(this).select2("destroy");
				cla.append(ruleID, $(this).parent());
				$(this).remove();
			});
			$("select.acle2-access-rule").each(function() {
				$(this).siblings("div.acle2-access-rule").attr("title", $(this).children("option:selected").attr("title"));
			});
		},
		append: function(ruleID, elem) {
			var newSelector = selector.clone();
			if (ruleID == "" || elem.hasClass("acle2-new-access-rule")){
				newSelector.prepend("<option value='' title='' selected>" + geti18n("ACLE.select.select") + "</option>");
				newSelector.append("<option class='acle2-new-access-rule-option' value='new' title=''>" + geti18n("ACLE.select.newRule") + "</option>");
			}
			newSelector.val(ruleID);
			newSelector.appendTo(elem);
			newSelector.select2({
				matcher: function(term, text, opt) {
						return text.toUpperCase().indexOf(term.toUpperCase())>=0
							|| opt.attr("title").toUpperCase().indexOf(term.toUpperCase())>=0;
					},
				formatResult: formatSelect
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
