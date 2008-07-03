/* WCMS ------------------------------------------------------------------------------------ */

function previewPicture(imagePath)
{
	var stuff = document.wcmsMultimedia.selectPicturePreview.options;
	var page = stuff[stuff.selectedIndex].value;
	
	if (page == "") {}
	else 
	  {
	    document.image.src=imagePath+page;	  
	  }      
}

function previewDocument(documentPath)
{
	var docstuff = document.wcmsMultimedia.selectDocumentPreview.options;
	var docpage = docstuff[docstuff.selectedIndex].value;
	
	if (docpage == "") {}
	else 
	  {
	   document.getElementById("doc").href=documentPath+docpage;
	   var rd_length = document.getElementById("doc").text.length;
	   document.getElementById("doc").firstChild.replaceData("0",rd_length,docpage);	  
	  }      
}

function size() 
{       deleteAttribute();
	var width = document.images[1].width;
        var height = document.images[1].height;
	
	if(width>height&&width>120)	
		{
		 width=120;
		 document.images[1].width=width;	
		}
	else
          	{
 		 if(height>120)
		   {
		    height=120;
		    document.images[1].height=height;	
		   }
		}
	       	
}

function deleteAttribute() {
  document.getElementById("thumb").removeAttribute("height");
  document.getElementById("thumb").removeAttribute("width");
}


function setHelpText() 
{
  /* set both help texts for 'choose action' */
  var temp = document.choose.action.options;
  var value = temp[temp.selectedIndex].value;
  
  if ( value == "edit") 
  {
	  var helpText_1 = document.createTextNode("Vorhandenen Inhalt bearbeiten heißt, dass sie schon bestehende Webseiten verändern können. Sie haben also hier die Möglichkeit den HTML-Quelltext der gewählten Seite direkt anzupassen.");  
	  var helpText_2 = document.createTextNode("Bitte wählen sie hier die Seite aus, die sie bearbeiten wollen.");  	  
  }
	  else if (value == "add_intern") 
	  {
		  var helpText_1 = document.createTextNode("Eine neue Webseite besteht aus einem Menüeintrag und zugehörigem HTML-Inhalt. Sie stellen also einen eigenen neuen Menüeintrag und die dazu gehörige HTML-Seite in das System ein.");
		  var helpText_2 = document.createTextNode("Eine neue Webseite wird immer unter einem bestimmtem Obermenüpunkt eingestellt. Wählen sie also hier den Menüpunkt aus, UNTER DEM die Seite angelegt werden soll. Hinweis: Einen Hauptmenüpunkt können sie einstellen, indem sie direkt das entsprechende Menü auswählen.");
	  }
		  else if (value == "add_extern") 
		  {
			  var helpText_1 = document.createTextNode("Ein Link besteht nur aus einem Menüeintrag. Sie stellen also hier einen neuen Menüeintrag in das System ein, der beim Anklicken auf die von ihnen angegebene Link-Adresse verweist.");
			  var helpText_2 = document.createTextNode("Eine neuer Link wird immer unter einem bestimmtem Obermenüpunkt eingestellt. Wählen sie also hier den Menüpunkt aus, UNTER DEM die der Link angelegt werden soll. Hinweis: Einen Link als Hauptmenüpunkt können sie einstellen, indem sie direkt das entsprechende Menü auswählen.");			  
		  }	  
			  else if (value == "delete") 
			  {
				  var helpText_1 = document.createTextNode("Vorhandener Inhalt löschen bedeutet, dass sie einen bestimmten Menüpunkt nebst Inhalt löschen. Hinweis: Wenn sie einen Link löschen wird natürlich nur der Menüpunkt gelöscht, da in dem Fall kein Inhalt existiert. ");
				  var helpText_2 = document.createTextNode("Wählen sie hier den Inhalt aus, den sie löschen möchten.");				  
			  }	  		  
			  
   document.getElementById("helpText.chooseAction").replaceChild(helpText_1, document.getElementById("helpText.chooseAction").firstChild);
   document.getElementById("helpText.chooseLocation").replaceChild(helpText_2, document.getElementById("helpText.chooseLocation").firstChild);   
}
function refreshClose() 
{
	window.opener.location.reload(true);
	window.close();
}

function switchKupuToHTML()
{
    if (kupu.getDocument().getEditable().style.display != 'none') {
    	if (kupu.getBrowserName() == 'Mozilla') {
        	kupu.getInnerDocument().designMode = 'Off';
    	};
        kupu._initialized = false;
        var data = kupu.getInnerDocument().documentElement.getElementsByTagName('body')[0].innerHTML;
        document.getElementById('kupu-editor-textarea').value = data;
        kupu.getDocument().getEditable().style.display = 'none';
        document.getElementById('kupu-editor-textarea').style.display = 'block';
        kupu._initialized = true;
	};
	document.editContent.submit();
};

function starteAktion(welcheAktion)
{

	switch (welcheAktion) {
		case "edit":
			document.forms['choose'].elements['action'].value='edit';
			document.choose.submit();
			break;
		case "predecessor":
			document.forms['choose'].elements['addAtPosition'].value='predecessor';
			pruefeBox("createLink");
			document.choose.submit();
			break;
		case "child":
			document.forms['choose'].elements['addAtPosition'].value='child';
			pruefeBox("createLink");
			document.choose.submit();
			break;
		case "successor":
			document.forms['choose'].elements['addAtPosition'].value='successor';
			pruefeBox("createLink");
			document.choose.submit();
			break;
		case "translate":
			document.forms['choose'].elements['action'].value='translate';
			document.choose.submit();
			break;
		case "delete":
			document.forms['choose'].elements['action'].value='delete';
			document.choose.submit();
			break;
		case "undo":
			break;
		case "view":
			var webBase = document.forms['choose'].elements['webBase'].value;
			var href = document.forms['choose'].elements['href'].value;
			var url = webBase + href.substr(2, href.length);
			if (document.forms['choose'].elements['openNewWindow'].checked == true) {
				wcmsView = window.open(url, "wcmsView");
				wcmsView.focus();
			} else {
				document.location= url;
			}
			break;
		default:
			break;
	}
};

function pruefeBox(welcheBox) {
	switch (welcheBox) {
		case "createLink":
			if (document.forms['choose'].elements['createLink'].checked==false)
			{
				document.forms['choose'].elements['action'].value='add_intern';
			} else {
				document.forms['choose'].elements['action'].value='add_extern';
			}
			break;
		default:
			break;
	}
};

function zeigeElement() {
	if (document.all.optionen.style.display == ""){
		document.all.optionen.style.display = "none";
	}

	if (document.all.optionen.style.display == "none")
	{
		document.all.optionen.style.display = "block";
		document.all.speichern.style.borderBottomWidth = "0";
		document.location= "#optionen";
		
	} else {
		document.all.optionen.style.display = "none";
		document.all.speichern.style.borderBottomWidth = "2px";
		document.location= "#toolbar";
	}
};


function schreibeDatum() {
	var Datum = new (Date);
	var Tag = Datum.getDate();
	var Wochentag = Datum.getDay();
	var Monat = Datum.getMonth();
	var Jahr = Datum.getFullYear();
	var Tagname = new Array("So","Mo","Di","Mi","Do","Fr","Sa");
	var Monatname = new Array("Januar","Februar","März","April","Mai","Juni","Juli","August","September","Oktober","November","Dezember");
	var Stunde = Datum.getHours();
	var Minute = Datum.getMinutes();
	var Sekunde = Datum.getSeconds();
	if(Sekunde<10)
	{
		Sekunde = "0" + Sekunde;
	}
	if(Minute<10)
	{
		Minute = "0" + Minute;
	}
	if(Stunde<10)
	{
		Stunde = "0" + Stunde;
	}
	/* document.write(Tagname[Wochentag]+", "+Tag+". "+Monatname[Monat]+" "+Jahr+" - "+Stunde+":"+Minute); */
	document.write(Stunde+":"+Minute+":"+Sekunde);
};


/* END OF: WCMS ------------------------------------------------------------------------------------ */

