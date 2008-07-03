/***********************************************
* AnyLink Drop Down Menu- © Dynamic Drive (www.dynamicdrive.com)
* This notice MUST stay intact for legal use
* Visit http://www.dynamicdrive.com/ for full source code
***********************************************/

var menu1=new Array()
var menu2=new Array()
		
var menuwidth='125px' //default menu width
var menubgcolor='#e2e6e9'  //menu bgcolor
var disappeardelay=250  //menu disappear speed onMouseout (in miliseconds)
var hidemenu_onclick="yes" //hide menu when user clicks within menu?

var edit=''
var addIntern_before=''
var addIntern_sub=''
var addIntern_after=''
var addExtern_before=''
var addExtern_sub=''
var addExtern_after='' 
var delete_it=''
var translate =''

/////No further editting needed

var ie4=document.all
var ns6=document.getElementById&&!document.all

if (ie4||ns6)
document.write('<div id="dropmenudiv" style="visibility:hidden;width:'+menuwidth+';background-color:'+menubgcolor+'" onMouseover="clearhidemenu()" onMouseout="dynamichide(event)"></div>')

function getposOffset(what, offsettype){
	var totaloffset=(offsettype=="left")? what.offsetLeft : what.offsetTop;
	var parentEl=what.offsetParent;
	while (parentEl!=null){
	totaloffset=(offsettype=="left")? totaloffset+parentEl.offsetLeft : totaloffset+parentEl.offsetTop;
	parentEl=parentEl.offsetParent;
	}
	return totaloffset;
}


function showhide(obj, e, visible, hidden, menuwidth){
	if (ie4||ns6)
	dropmenuobj.style.left=dropmenuobj.style.top="-500px"
	if (menuwidth!=""){
	dropmenuobj.widthobj=dropmenuobj.style
	dropmenuobj.widthobj.width=menuwidth
	}
	if (e.type=="click" && obj.visibility==hidden || e.type=="mouseover")
	obj.visibility=visible
	else if (e.type=="click")
	obj.visibility=hidden
}

function iecompattest(){
	return (document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body
}

function clearbrowseredge(obj, whichedge){
	var edgeoffset=0
	if (whichedge=="rightedge"){
	var windowedge=ie4 && !window.opera? iecompattest().scrollLeft+iecompattest().clientWidth-15 : window.pageXOffset+window.innerWidth-15
	dropmenuobj.contentmeasure=dropmenuobj.offsetWidth
	if (windowedge-dropmenuobj.x < dropmenuobj.contentmeasure)
	edgeoffset=dropmenuobj.contentmeasure-obj.offsetWidth
	}
	else{
	var topedge=ie4 && !window.opera? iecompattest().scrollTop : window.pageYOffset
	var windowedge=ie4 && !window.opera? iecompattest().scrollTop+iecompattest().clientHeight-15 : window.pageYOffset+window.innerHeight-18
	dropmenuobj.contentmeasure=dropmenuobj.offsetHeight
	if (windowedge-dropmenuobj.y < dropmenuobj.contentmeasure){ //move up?
	edgeoffset=dropmenuobj.contentmeasure+obj.offsetHeight
	if ((dropmenuobj.y-topedge)<dropmenuobj.contentmeasure) //up no good either?
	edgeoffset=dropmenuobj.y+obj.offsetHeight-topedge
	}
	}
	return edgeoffset
}

function populatemenu(what, path, template, hr, deletepath, edit, addIntern_before, addIntern_sub, addIntern_after, addExtern_before, addExtern_sub, addExtern_after, delete_it, translate){

//address for backjump after delete, if no more parents left
var address=path+deletepath.substring(1);
var hr2="";

if(hr!="")
  {
   //delete the first slash
   hr2=hr.substring(1);
  }



menu1[0]='<table><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&action=edit&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+edit+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/change.gif" border="0"/></a></td></table>'
menu1[1]='<table><tr><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&addAtPosition=predecessor&action=add_intern&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+addIntern_before+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/newdv.gif" border="0"/></a></td><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&addAtPosition=child&action=add_intern&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+addIntern_sub+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/newdr.gif" border="0"/></a></td><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&addAtPosition=successor&action=add_intern&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+addIntern_after+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/newdn.gif" border="0"/></a></td></tr></table>'
menu1[2]='<table><tr><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&addAtPosition=predecessor&action=add_extern&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+addExtern_before+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/newlinkdv.gif" border="0"/></a></td><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&addAtPosition=child&action=add_extern&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+addExtern_sub+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/newlinkdr.gif" border="0"/></a></td><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&addAtPosition=successor&action=add_extern&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+addExtern_after+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/newlinkdn.gif" border="0"/></a></td></tr></table>'
menu1[3]='<table><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&action=delete&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+address+'"><img title="'+delete_it+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/delete.gif" border="0"/></a></td></table>'

menu2[0]='<table><td><a href="'+path+'servlets/MCRWCMSChooseServlet?session=choose&action=translate&href=1'+hr+'&template=dumy.xml&XSL.check=1&XSL.address='+path+hr2+'"><img title="'+translate+'" src="'+path+'/templates/master/template_wcms/IMAGES/fastwcms/change.gif" border="0"/></a></td></table>'

	if (ie4||ns6)
	dropmenuobj.innerHTML=what.join("")
}


function dropdownmenu(obj, e, menucontents, menuwidth, path, template, hr, deletepath, edit, addIntern_before, addIntern_sub, addIntern_after, addExtern_before, addExtern_sub, addExtern_after, delete_it, translate){



	if (window.event) event.cancelBubble=true
	else if (e.stopPropagation) e.stopPropagation()
	clearhidemenu()
	dropmenuobj=document.getElementById? document.getElementById("dropmenudiv") : dropmenudiv
	populatemenu(menucontents, path, template, hr, deletepath, edit, addIntern_before, addIntern_sub, addIntern_after, addExtern_before, addExtern_sub, addExtern_after, delete_it, translate)

	if (ie4||ns6){
	showhide(dropmenuobj.style, e, "visible", "hidden", menuwidth)
	dropmenuobj.x=getposOffset(obj, "left")
	dropmenuobj.y=getposOffset(obj, "top")
	dropmenuobj.style.left=dropmenuobj.x-clearbrowseredge(obj, "rightedge")+"px"
	dropmenuobj.style.top=dropmenuobj.y-clearbrowseredge(obj, "bottomedge")+obj.offsetHeight+"px"
	}

	return clickreturnvalue()
}

function clickreturnvalue(){
	if (ie4||ns6) return false
	else return true
	}

	function contains_ns6(a, b) {
	while (b.parentNode)
	if ((b = b.parentNode) == a)
	return true;
	return false;
}

function dynamichide(e){
	if (ie4&&!dropmenuobj.contains(e.toElement))
	delayhidemenu()
	else if (ns6&&e.currentTarget!= e.relatedTarget&& !contains_ns6(e.currentTarget, e.relatedTarget))
	delayhidemenu()
}

function hidemenu(e){
	if (typeof dropmenuobj!="undefined"){
	if (ie4||ns6)
	dropmenuobj.style.visibility="hidden"
	}
}

function delayhidemenu(){
	if (ie4||ns6)
	delayhide=setTimeout("hidemenu()",disappeardelay)
}

function clearhidemenu(){
	if (typeof delayhide!="undefined")
	clearTimeout(delayhide)
	}

	if (hidemenu_onclick=="yes")
	{
	document.onclick=hidemenu
}
