/************************************************/
/*  											*/
/* Module - MCR-Broadcasting 1.0, 05-2007  		*/
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. 			*/
/*  											*/
/************************************************/

function receiveBroadcast(sender, registerServlet, refreshRate) {

	// open up request	
	var req;
	if (window.XMLHttpRequest) {
		req = new XMLHttpRequest();
	}
		else if (window.ActiveXObject) {
			req = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
	//fake address to kidd stupid internet explorer :-)
	var randomNumber = Math.random();
	var senderWithNumber = sender+"?XSL.dummyNumber="+randomNumber;
	req.open("GET", senderWithNumber, true);
	
	//disable cache
	req.setRequestHeader("Pragma", "no-cache");
	req.setRequestHeader("Cache-Control", "must-revalidate");
	req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
	
	//call server's xml document
	req.onreadystatechange=function() {
	  if (req.readyState==4) {
	   if (req.status==200) {

		   var answerXML = req.responseXML;		   
		   var signal = answerXML.getElementsByTagName("signal")[0].firstChild.nodeValue;		   	   

	       // alert message, if there is one
		   if (signal=="on") {
   			   var messageHeader 	= answerXML.getElementsByTagName("message.header")[0].firstChild.nodeValue;		   	   		   
			   var message 			= answerXML.getElementsByTagName("message.body")[0].firstChild.nodeValue;		   	   		      			   
			   var messageTail 		= answerXML.getElementsByTagName("message.tail")[0].firstChild.nodeValue;		   	   		   
			   message=messageHeader+"\n\n"+message+"\n\n"+messageTail;
			   alert(message);
			   addReceiver(registerServlet);
		   }
			   
		   // ask in peridical time slots for new messages 
			var reCall = "receiveBroadcast('"+sender+"','"+registerServlet+"','"+refreshRate+"')";
			var refreshRateMilliSec = refreshRate*1000;
	        setTimeout(reCall, refreshRateMilliSec);
	   }
	  }		
	}
	req.send(null);		
}

function addReceiver(registerServlet) {
	
	var req;
	// transmit
	if (window.XMLHttpRequest) {
		req = new XMLHttpRequest();
	}
		else if (window.ActiveXObject) {
			req = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
	//fake address to kidd stupid internet explorer :-)
	var randomNumber = Math.random();
	var senderWithNumber = registerServlet; //+"&dummyNumber="+randomNumber;
	req.open("GET", registerServlet, true);
	
	req.onreadystatechange=function() {
	  if (req.readyState==4) {
	   if (req.status==200) {
		   var answerXML = req.responseXML;
		   var message = answerXML.getElementsByTagName("addReceiver")[0].firstChild.nodeValue;
	   }
	  }		
	}
	req.send(null);		
}