/************************************************/
/*  											*/
/* Module - MCR-Broadcasting 1.0, 05-2007  		*/
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. 			*/
/*  											*/
/************************************************/
// ================================================================================================== //
function receiveBroadcast(sender, registerServlet, refreshRate) {
	
	var req;
	// transmit
	if (window.XMLHttpRequest) {
		req = new XMLHttpRequest();
	}
		else if (window.ActiveXObject) {
			req = new ActiveXObject("Microsoft.XMLHTTP");
		}
	req.open("GET", sender, true);
	
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
		   }
			   
		   // ask in peridical time spaces for new messages 
			var reCall = "receiveBroadcast('"+sender+"','"+registerServlet+"','"+refreshRate+"')";
			var refreshRateMilliSec = refreshRate*1000;
	        setTimeout(reCall, refreshRateMilliSec);
	   }
	  }		
	}
	req.send(null);		
}
