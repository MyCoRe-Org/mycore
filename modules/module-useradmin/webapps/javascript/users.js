/*Author: Radi Radichev*/

var url;  //URL für den Request
var req;	//Request Variable
var data;	//JSON Document 

var userToUpdate;
var groupToUpdate;

var itemIndex=0;
var groupIndex=0;
var userIndex=0;

function initialize() {

	userToUptade="";
	groupToUptade="";
	req="";
	data="";
	
	$("userManagement").style.display="block";
	
	//Den Mülleimer initialisieren
	Droppables.add("trash",{
		accept:"usersElement",
		onDrop:function(element,drop) {
			userToUpdate=element.lastChild.innerHTML;
			groupToUpdate=element.parentNode.parentNode.id;
			if(groupToUpdate=="userManagement") {
				groupToUpdate=null;
			}
			if(userToUpdate==undefined) {
				userToUpdate=element.innerHTML;
			}
			new Effect.Puff(element.id);
			var result=confirm(confirmDeleteUser);
			if(result==true) {
				deleteUser();
			} else {
				updateGroup();
			}
		}});
	//Einen Request zum Server schicken, damit die Daten angezeigt werden.
	url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
	sendRequest(url,showData);
	
}

//Request aufbauen und senden, funct ist eine Funktion die ausgeführt werden soll wenn der Request endet.
function sendRequest(url,funct) {

try {
        if( window.XMLHttpRequest ) {
          req = new XMLHttpRequest();
        } else if( window.ActiveXObject ) {
          req = new ActiveXObject( "Microsoft.XMLHTTP" );
        } else {
          alert( "Ihr Webbrowser unterstuetzt leider kein Ajax!" );
        }
        req.open( "GET", url, true );
        req.onreadystatechange = funct;
        req.send( null );
      } catch( e ) {
        alert( "Fehler: " + e );
      }
	
}

//Alle Daten anzeigen.
function showData() {
	//Zähler für eindeutige IDs
	groupIndex=0;
	userIndex=0;

	var usrHeader =	document.createElement("h1");
	usrHeader.innerHTML=userHdr;
	var grpHeader =	document.createElement("h1");
	grpHeader.innerHTML=groupHdr;
	$("usersList").innerHTML="";
    $("usersList").appendChild(usrHeader);
	$("groupsList").innerHTML="";
	$("groupsList").appendChild(grpHeader);
	
	data=req.responseText.parseJSON();
	
	data.users.each(
		function(user) {
			itemIndex++;
			$("usersList").appendChild(createUserElement(user));
			new Draggable(user,{revert:true});
		}
	);
	
	data.groups.each(
		function(group) {
			groupIndex++;
			$("groupsList").appendChild(createGroupElement(group.name));
			$("Content_"+group.name).style.display="none";
			Droppables.add(group.name, {
				accept:"usersElement",
				onDrop:function(element,drop) {
					userToUpdate=element.lastChild.innerHTML;
					groupToUpdate=drop.firstChild.innerHTML;
					updateGroup();
				}
			}
			);
			data.groups[groupIndex-1].users.each(
				function(user) {
					userIndex++;
					$(group.name).lastChild.appendChild(createGroupUser(user,group.name));
					new Draggable(user+"_"+group.name,{revert:true});
				}
			);
		}
	);
	
	if(data.error[0]!="none" && data.error[0]!="0")
	{
		alert(data.error[0]);
		url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
    	sendRequest(url,showData);
	}
	
}

//nur die veränderte Gruppe aktualisieren
function showGroup() {
	data=req.responseText.parseJSON();
	
	$(data.gruppe.name).innerHTML="";
	
	$(data.gruppe.name).appendChild(createGroupElement(data.gruppe.name));
	Droppables.add(data.gruppe.name, {
		accept:"usersElement",
		onDrop:function(element,drop) {
			userToUpdate=element.lastChild.innerHTML;
			groupToUpdate=drop.firstChild.innerHTML;
			updateGroup();
		}
	}
	);
	data.gruppe.users.each(
		function(user) {
			userIndex++;
			$(data.gruppe.name).lastChild.appendChild(createGroupUser(user,data.gruppe.name));
			new Draggable(user+"_"+data.gruppe.name,{revert:true});
		}
	);
	
	if(data.error[0]!="none" && data.error[0]!="0")
	{
		alert(data.error[0]);
		url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
    	sendRequest(url,showData);
	}
}
//Löschen eines Nutzers
function deleteUser() {
	if(groupToUpdate!=null) {
		url=servletBaseURL+"MCRUserAjaxServlet?mode=delete&user="+userToUpdate+"&group="+groupToUpdate;
		sendRequest(url,showGroup);
    } else {
    	url=servletBaseURL+"MCRUserAjaxServlet?mode=delete&user="+userToUpdate+"&group="+groupToUpdate;
    	sendRequest(url,showData);
    }
}

//Hinzufügen eines Nutzers in eine Gruppe
function updateGroup() {
	url=servletBaseURL+"MCRUserAjaxServlet?mode=update&user="+userToUpdate+"&group="+groupToUpdate;
    sendRequest(url,showGroup);
}

//Aufbauen des Nutzer-Elements in die Gruppenliste
function createGroupUser(user,group) {
	var User=document.createElement("div");
	User.className="usersElement";
	User.setAttribute("id",user+"_"+group);
	User.innerHTML=user;
	return User;
}

//Aufbauen des Nutzer-Elements in die Nutzerliste
function createUserElement(user) {
	var userContainer=document.createElement("div");
	var usrImg=document.createElement("img");
	var usrElement=document.createElement("div")
	usrImg.setAttribute("id","avatar");
	usrImg.setAttribute("src",userImg);
	userContainer.className="usersElement";
	userContainer.setAttribute("id",user);
	usrElement.innerHTML=user;
	userContainer.appendChild(usrImg);
	userContainer.appendChild(usrElement);
	return userContainer;
}

//Aufbauen eines Gruppen-Element
function createGroupElement(group) {
	var id=group;
	var groupElement=document.createElement("div");
	var link=document.createElement("a");
	link.setAttribute("href","#");
	link.setAttribute("onclick","Effect.toggle('Content_"+group+"','blind')");
	link.innerHTML=id;
	groupElement.className="groupsElement";
	groupElement.setAttribute("id",id);
	groupElement.appendChild(link);
	var content=document.createElement("div");
	content.className="groupsContent";
	id="Content_"+group;
	content.setAttribute("id",id);
	groupElement.appendChild(content);
	return groupElement;
}


