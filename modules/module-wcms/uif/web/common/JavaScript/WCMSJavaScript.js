/* WCMS ------------------------------------------------------------------------------------ */
function previewPicture(imagePath)
{
	var stuff = document.editContent.selectPicturePreview.options;
	var page = stuff[stuff.selectedIndex].value;
	
	if (page == "") {}
	else 
	  {
/*	    document.image.src=page;		  */
	    document.image.src=imagePath+page;		  
	  }
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
/* END OF: WCMS ------------------------------------------------------------------------------------ */

