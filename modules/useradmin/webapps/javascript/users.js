/*Author: Radi Radichev*/

var url;  //URL für den Request
var req;	//Request Variable
var data;	//JSON Document 
var background; //Background Color

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
		accept:["usersElement","groupsElement"],
		onDrop:function(element,drop) {
			userToUpdate=element.lastChild.innerHTML;
			groupToUpdate=element.parentNode.parentNode.id;
			if(groupToUpdate=="userManagement") {
				groupToUpdate=null;
			}
			if(userToUpdate==undefined) {
				userToUpdate=element.innerHTML;
			}
			if(element.className=="groupsElement")
			{
				deleteGroup(element.id);
			} else {
				new Effect.Puff(element.id);
				var result=confirm(confirmDeleteUser);
				if(result==true) {
					deleteUser();
				} else {
					updateGroup();
				}
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

	var usrHeader =	document.createElement("h2");
	usrHeader.innerHTML=userHdr;
	var grpHeader =	document.createElement("h2");
	grpHeader.innerHTML=groupHdr;
	var usrList = document.createElement("ul");
	usrList.setAttribute("id", "usersList");
	var grpList = document.createElement("ul");
	grpList.setAttribute("id", "groupsList");
	$("users").innerHTML="";
    $("users").appendChild(usrHeader);
    $("users").appendChild(usrList);
	$("groups").innerHTML="";
	$("groups").appendChild(grpHeader);
	$("groups").appendChild(grpList);
	
	data=req.responseText.parseJSON();
	
	data.users.each(
		function(user) {
			itemIndex++;
			$("usersList").appendChild(createUserElement(user.userID,user.name));
			new Draggable(user.userID,{revert:true});
		}
	);
	
	data.groups.each(
		function(group) {
			groupIndex++;
			$("groupsList").appendChild(createGroupElement(group.name,group.desc));
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
			new Draggable(group.name,{revert:true,handle:group.name+"_link"});
		}
	);
	
	highlightGroups();
	
	if(data.error[0]!="none" && data.error[0]!="0")
	{
		alert(data.error[0]);
		url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
    	sendRequest(url,showData);
	}
	
}


function highlightGroups() {

	data.users.each(
		function(user) {
			$(user.userID).setAttribute("onmouseover","highlight('"+user.userID+"')");
			$(user.userID).setAttribute("onmouseout","turnBack('"+user.userID+"')");
		}
	);
}

function highlight(username) {
	groupsIndex=0;
	groupsArray=new Array();
	data.groups.each(
		function(group){
			groupsIndex++;
			data.groups[groupsIndex-1].users.each(
				function(user) {
					if(user==username)
						groupsArray.push(group.name);
				}
			);
		}
	);
	for(var i=0;i<groupsArray.length;i++)
	{
		background=$(groupsArray[i]).style.backgroundColor;
		$(groupsArray[i]).style.backgroundColor="#14516e";
	}
} 

function turnBack(username) {
	groupsIndex=0;
	groupsArray=new Array();
	data.groups.each(
		function(group){
			groupsIndex++;
			data.groups[groupsIndex-1].users.each(
				function(user) {
					if(user==username)
						groupsArray.push(group.name);
				}
			);
		}
	);
	for(var i=0;i<groupsArray.length;i++)
	{
		$(groupsArray[i]).style.backgroundColor=background;
	}
} 
//nur die veränderte Gruppe aktualisieren
function showGroup() {
	data=req.responseText.parseJSON();
	
	$(data.gruppe.name).innerHTML="";
	
	$(data.gruppe.name).appendChild(createGroupElement(data.gruppe.name,data.gruppe.desc));
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
	new Draggable(data.gruppe.name,{revert:true,handle:data.gruppe.name+"_link"});
	url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
	sendRequest(url,showData);
}
//Löschen eines Nutzers
function deleteUser() {
	url=servletBaseURL+"MCRUserAjaxServlet?mode=delete&user="+userToUpdate+"&group="+groupToUpdate;
	if(groupToUpdate!=null) {
		sendRequest(url,showGroup);
    } else {
    	sendRequest(url,showData);
    }
}

function deleteGroup(id) {
	var result=confirm(groupDel);
	if(result==true)	{
		url=servletBaseURL+"MCRUserAjaxServlet?mode=deleteGroup&group="+id;
		sendRequest(url,completeDeleteGroup);
	} 
}

function completeDeleteGroup() {
	var data=req.responseText.parseJSON();
	if(data.response=="ok") {
		url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
    	sendRequest(url,showData);
    } else if(data.error=="hasMembers") {
    	var result=confirm(groupNotEmpty);
    	if ( result==true) {
    		url=servletBaseURL+"MCRUserAjaxServlet?mode=deleteGroupComplete&group="+data.group;
			sendRequest(url,completeDeleteGroup);
    	} else {
    		url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
    		sendRequest(url,showData);
    	}
    } else if(data.error="primaryGroup") {
    	alert(primaryGroup+data.users);
    	url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
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
	var User=document.createElement("li");
	User.className="usersElement";
	User.setAttribute("id",user+"_"+group);
	User.innerHTML=user;
	return User;
}

//Aufbauen des Nutzer-Elements in die Nutzerliste
function createUserElement(user,name) {
	var userContainer=document.createElement("li");
	var usrImg=document.createElement("img");
	var usrElement=document.createElement("div")
	usrImg.className="avatar";
	usrImg.setAttribute("src",userImg);
	userContainer.className="usersElement";
	userContainer.setAttribute("id",user);
	userContainer.setAttribute("TITLE","header=[Real Name] body=["+name+"]");
	usrElement.innerHTML=user;
	userContainer.appendChild(usrImg);
	userContainer.appendChild(usrElement);
	return userContainer;
}

//Aufbauen eines Gruppen-Element
function createGroupElement(group,description) {
	var id=group;
	var groupElement=document.createElement("li");
	var link=document.createElement("span");
	link.className="clickable";
	link.setAttribute("onclick","Effect.toggle('Content_"+group+"','blind')");
	link.innerHTML=id;
	var handle=document.createElement("img");
	handle.setAttribute("src",grpHandle);
	handle.setAttribute("id",id+"_link");
	groupElement.className="groupsElement";
	groupElement.setAttribute("id",id);
	groupElement.appendChild(link);
	groupElement.appendChild(handle);
	groupElement.setAttribute("TITLE","header=[Description] body=["+description+"]");
	var content=document.createElement("ul");
	content.className="groupsContent";
	id="Content_"+group;
	content.setAttribute("id",id);
	groupElement.appendChild(content);
	return groupElement;
}


