/************************************************/
/*  											*/
/* Module - MCR-Broadcasting 1.0, 05-2007  		*/
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. 			*/
/* Matthias Eichner - 2012 (use jquery)			*/
/************************************************/

var sender;
var registerServlet;
var refreshRate;

function initializeBroadcast(newSender, newRegisterServlet, newRefreshRate) {
	sender = newSender;
	registerServlet = newRegisterServlet;
	refreshRate = newRefreshRate;
}

$(document).ready(function() {
	receiveBroadcast();
});

function receiveBroadcast() {

	$.ajax({
		url: sender,
		cache: false,
		dataType: "xml",
		success: function(answerXML) {
			var signal = answerXML.getElementsByTagName("signal")[0].firstChild.nodeValue;
			if (signal=="on") {
				var messageHeader 	= answerXML.getElementsByTagName("message.header")[0].firstChild.nodeValue;		   	   		   
				var message 		= answerXML.getElementsByTagName("message.body")[0].firstChild.nodeValue;		   	   		      			   
				var messageTail 	= answerXML.getElementsByTagName("message.tail")[0].firstChild.nodeValue;		   	   		   
				message				= messageHeader+"\n\n"+message+"\n\n"+messageTail;
				alert(message);
				addReceiver();
			}
			var refreshRateMilliSec = refreshRate*1000;
	        setTimeout(receiveBroadcast, refreshRateMilliSec);
		}
	});

}

function addReceiver() {
	$.ajax({
		url: registerServlet,
		cache: false,
		dataType: "xml",
		success: function(answerXML) {
			answerXML.getElementsByTagName("addReceiver")[0].firstChild.nodeValue;
		}
	});
}
