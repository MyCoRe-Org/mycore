/**
 * Clones an object. A new instance is generated
 * 
 * @param obj object to clone
 * @return new instance of the object
 */
function clone(obj) {
	if (obj == null || typeof (obj) != 'object')
		return obj;
	var temp = new obj.constructor(); // changed (twice)

	for (var key in obj)
		temp[key] = clone(obj[key]);
	return temp;
}

function deepEquals(a, b) {
	var result = true;

	function lengthTest(a, b) {
		var count = 0;
		for( var p in a)
			count++;
		for( var p in b)
			count--;
		return count == 0 ? true: false;
	}

	function typeTest(a, b) {
		return (typeof a == typeof b);
	}

	function test(a, b) {
		if (!typeTest(a, b))
			return false;
		if (typeof a == 'function' || typeof a == 'object') {
			if(!lengthTest(a,b))
				return false;
			for ( var p in a) {
				result = test(a[p], b[p]);
				if (!result)
					return false;
			}
			return result;
		}
		return (a == b);
	}
	return test(a, b);
}

function isClassification(/*dojo.data.item*/ item) {
	var id = item.id[0];
	return id.categid == null || id.categid == "";
}

function getClassificationId(/*dojo.data.item*/ item) {
	var id = item.id[0];
	return id.rootid != undefined ? id.rootid : "";
}

function getCategoryId(/*dojo.data.item*/ item) {
	var id = item.id[0];
	return id.categid != undefined ? id.categid : "";
}

function hasChildren(/*dojo.data.item*/ item) {
	return (item.haschildren && item.haschildren[0] == true) || item.children;
}

function hasChildrenLoaded(/*dojo.data.item*/ item) {
	return hasChildren(item) && item.children;
}

function isIdEqual(/*JSON*/ id, /*JSON*/ id2) {
	return id.rootid == id2.rootid && id.categid == id2.categid;
}

/**
 * Set dojo theme to body for css support. This is important
 * for Dijit Components, DnD and Tooltips.
 */
function updateBodyTheme(/*String*/ theme) {
	if(theme == null) {
		theme = "claro";
	}
    dojo.query('body').forEach(function(node) {
        dojo.attr(node, "class", "claro");
    });
}
