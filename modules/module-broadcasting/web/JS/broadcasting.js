/************************************************/
/*  											*/
/* Module - MCR-Broadcasting 1.0, 05-2007  		*/
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. 			*/
/*  											*/
/************************************************/
// ================================================================================================== //
function receiveBroadcast(sender, registerServlet) {
	
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
			   var message = answerXML.getElementsByTagName("message")[0].firstChild.nodeValue;		   	   		   
			   alert(message);
		   }
			   
		   // ask in peridical time spaces for new messages 
			var reCall = "receiveBroadcast('"+sender+"','"+registerServlet+"')";
	        setTimeout(reCall, 3000);
	   }
	  }		
	}
	req.send(null);		
}
