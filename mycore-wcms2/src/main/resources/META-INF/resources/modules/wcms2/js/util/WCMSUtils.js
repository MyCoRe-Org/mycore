/*-------------------------------------------------------------------------
 * General utils
 *------------------------------------------------------------------------*/

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

function cloneList(list) {
	if (list == null || typeof (list) != 'array')
		return list;
	var temp = [];
	for(var obj in list)
		list.push(clone(obj));
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

/**
 * Compares two objects. If both objects are null, undefined or empty (for strings),
 * this method handles them as equal.
 * 
 * @param o1 the first string
 * @param o2 the second string
 * @return true if they are equal, otherwise false
 */
function equal(o1, o2) {
	if( (o1 == null || o1 == undefined || o1 == "") &&
	    (o2 == null || o2 == undefined || o2 == "") )
	    return true;
	return o1 == o2;
}

/*-------------------------------------------------------------------------
 * Array utils
 *------------------------------------------------------------------------*/

/**
 * Compares two arrays if they are equal. This works only for
 * one-dimensional arrays.
 * 
 * @param a1 the first array
 * @param a2 the second one
 * @return true if they are equal, otherwise false
 */
function arrayEquals(/* Array */a1, /* Array */a2) {
	console.log(a1);
	console.log(a2);
	var counter1 = 0;
	var counter2 = 0;
	for ( var i in a1)
		counter1++;
	for ( var i in a2)
		counter2++;
	if (counter1 != counter2)
		return false;

	var ta1 = dojo.clone(a1);
	var ta2 = dojo.clone(a2);
	ta1.sort();
	ta2.sort();
	for ( var i in ta1)
		if (ta1[i] !== ta2[i])
			return false;
	return true;
}

/**
 * Removes the element from the array.
 * 
 * @param array
 * @param element
 */
function arrayRemoveElement(/* Array */array, /* Object */element) {
	var tempArr = [];
	for ( var i = 0; i < array.length; i++) {
		if (!deepEquals(array[i], element))
			tempArr.push(array[i]);
	}
	return tempArr;
}

function arrayRemoveById(/* Array */array, /*int*/ id) {
	var tempArr = [];
	for ( var i = 0; i < array.length; i++)
		if (array[i].id != id)
			tempArr.push(array[i]);
	return tempArr;
}

/**
 * Deletes all properties of an object which are undefined or null.
 * 
 * @param o
 */
function deleteUndefinedProperties(/*Object*/ o) {
	for (var i in o) {
		if (o[i] === null || o[i] === undefined) {
			delete o[i];
		}
	}
}

/*-------------------------------------------------------------------------
 * MyCoRe utils
 *------------------------------------------------------------------------*/
function getTemplateList(/*function*/ onSuccess) {
	var xhrArgs = {
		url : wcms.settings.wcmsURL + "/navigation/templates",
		handleAs : "json",
		load : function(data) {
			onSuccess(data);
		},
		error : function(error) {
			console.log("error while retrieving template list. " + error);
		}
	};
	dojo.xhrGet(xhrArgs);
}
