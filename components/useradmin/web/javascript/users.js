/*Author: Radi Radichev*/

var url;  //URL f�r den Request
var data;	//JSON Document 
var req;
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
	//Den M�lleimer initialisieren
	Droppables.add("trash",{
		accept:["usersElement","groupsElement"],
		onDrop:function(element,drop) {
	        var msg = confirmDeleteUser;
			userToUpdate=element.lastChild.innerHTML;
			groupToUpdate=element.parentNode.parentNode.parentNode.id;
			if(groupToUpdate=="userManagement") {
				groupToUpdate=null;
			}
			if(userToUpdate==undefined) {
			    msg = confirmRemoveUserFromGroup;
				userToUpdate=element.innerHTML;
			}
			if(element.className=="groupsElement")
			{
				deleteGroup(element.id);
			} else {
				new Effect.Puff(element.id);
				var result=confirm(msg);
				if(result==true) {
					deleteUser();
				} else {
					url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
					sendRequest(url,showData);
				}
			}
		}});
	//Einen Request zum Server schicken, damit die Daten angezeigt werden.
	url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
	sendRequest(url,showData);
}

//Request aufbauen und senden, funct ist eine Funktion die ausgef�hrt werden soll wenn der Request endet.
function sendRequest(url,funct) {
	new Ajax.Request(url,{
		onSuccess:function(transport) {
			funct(transport);
		}
	});
}


//Alle Daten anzeigen.
function showData(req) {
    
	//Z�hler f�r eindeutige IDs
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
	
	responseTxt=req.responseText;
	data=responseTxt.evalJSON();
	if (document.all) { Droppables.drops = [] }
	
	data.users.each(
		function(user) {
			itemIndex++;
			$("usersList").appendChild(createUserElement(user.userID,user.name, user.surname));
			new Draggable(user.userID,{revert:true});
		}
	);
	
	Droppables.add("trash",{
		accept:["usersElement","groupsElement"],
		onDrop:function(element,drop) {
			userToUpdate=element.lastChild.innerHTML;
			groupToUpdate=element.parentNode.parentNode.parentNode.id;
			if(groupToUpdate=="users") {
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
					url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
					sendRequest(url,showData);
				}
			}
		}});
		
	data.groups.each(
		function(group) {
			groupIndex++;
			$("groupsList").appendChild(createGroupElement(group.name,group.desc));
			$("Content_"+group.name).style.display="none";
			var tempEl=document.getElementById("link_"+group.name);
			Event.observe(tempEl,'click',function() {
				Effect.toggle('Content_'+group.name,'appear');
			},false);			
			Droppables.add(group.name, {
				accept:"usersElement",
				onDrop:function(element,drop) {
					userToUpdate=element.lastChild.innerHTML;
					groupToUpdate=drop.firstChild.innerHTML;
					updateGroup();
				}
			}
			);
			var outer=document.createElement("div");
			$(group.name).lastChild.appendChild(outer);
			data.groups[groupIndex-1].users.each(
				function(user) {
					userIndex++;
					outer.appendChild(createGroupUser(user,group.name));
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
			var userEl=document.getElementById(user.userID);
			Event.observe(userEl,'mouseover',function() {
				markGroups(user.userID);
			});
			Event.observe(userEl,'mouseout',function() {
				turnBack(user.userID);
			});
		}
	);
}

function markGroups(username) {
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
//nur die ver�nderte Gruppe aktualisieren
function showGroup(req) {
	responseTxt=req.responseText;
	data=responseTxt.evalJSON();
	$(data.gruppe.name).innerHTML="";
	 if (document.all) { Droppables.drops = [] }
	$(data.gruppe.name).appendChild(createGroupElement(data.gruppe.name,data.gruppe.desc));
	
	Droppables.add("trash",{
		accept:["usersElement","groupsElement"],
		onDrop:function(element,drop) {
			userToUpdate=element.lastChild.innerHTML;
			groupToUpdate=element.parentNode.parentNode.parentNode.id;
			if(groupToUpdate=="users") {
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
					url=servletBaseURL+"MCRUserAjaxServlet?mode=users";
					sendRequest(url,showData);
				}
			}
		}});
		
	Droppables.add(data.gruppe.name, {
		accept:"usersElement",
		onDrop:function(element,drop) {
			userToUpdate=element.lastChild.innerHTML;
			groupToUpdate=drop.firstChild.innerHTML;
			updateGroup();
		}
	}
	);
	var outer=document.createElement("div");
	$(data.gruppe.name).lastChild.appendChild(outer);
	data.gruppe.users.each(
		function(user) {
			userIndex++;
			outer.appendChild(createGroupUser(user,data.gruppe.name));
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
//L�schen eines Nutzers
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

function completeDeleteGroup(req) {
	//var data=req.responseText.parseJSON();
	responseTxt=req.responseText;
	data=responseTxt.evalJSON();
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
//Hinzuf�gen eines Nutzers in eine Gruppe
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
function createUserElement(user,name, surname) {
	var userContainer=document.createElement("li");
	var usrImg=document.createElement("img");
	var usrElement=document.createElement("div")
	var usrName=document.createElement("div")
	usrImg.className="avatar";
	usrImg.setAttribute("src",userImg);
	Element.addClassName(userContainer,"usersElement");
	Element.writeAttribute(userContainer,{'name':surname});
	Element.writeAttribute(userContainer,{'id':user});
	Element.writeAttribute(userContainer,{'title':'header=[Real Name] body=['+name+']'});
	usrElement.innerHTML='"' + user + '"';
	
	userContainer.appendChild(usrImg);
	if (surname != "" || name != ""){
        usrName.innerHTML= surname + ", " + name;
        userContainer.appendChild(usrName);
    }
	userContainer.appendChild(usrElement);
	return userContainer;
}

//Aufbauen eines Gruppen-Element
function createGroupElement(group,description) {
	var id=group;
	var groupElement=document.createElement("li");
	var link=document.createElement("a");
	link.className="clickable";
	link.setAttribute("href","#");
	link.setAttribute("id","link_"+id);
	link.innerHTML=id;
	var handle=document.createElement("img");
	handle.setAttribute("src",grpHandle);
	handle.setAttribute("id",id+"_link");
	Element.addClassName(groupElement,"groupsElement");
	Element.writeAttribute(groupElement,{'id':id});
	Element.writeAttribute(groupElement,{'title':'header=[Description] body=['+description+']'});
	groupElement.appendChild(link);
	groupElement.appendChild(handle);
	var content=document.createElement("ul");
	content.className="groupsContent";
	id="Content_"+group;
	content.setAttribute("id",id);
	groupElement.appendChild(content);
	return groupElement;
}

function aufklappen(id) {
	Effect.toggle('Content_'+id,'appear',{ delay: 0.001 });
}