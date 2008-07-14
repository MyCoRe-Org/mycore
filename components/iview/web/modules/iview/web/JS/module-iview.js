/************************************************/
/*                                              */
/* Image Viewer - MCR-IView 1.0, 05-2006        */
/* +++++++++++++++++++++++++++++++++++++        */
/*                                              */
/* Andreas Trappe   - concept, devel. in misc.  */
/* Britta Kapitzki  - Design                    */
/* Thomas Scheffler - html prototype            */
/* Stephan Schmidt  - html prototype            */
/*                                              */
/************************************************/

function setupContentArea() {
    
    var node = document.getElementById("content-container");
    
    /*var height = getHeight();
    var width = node.offsetWidth;
    width = getWidth();*/
    /*node.style.width = width+"px";
    node.style.height = height+"px";*/
    
    node.style.width = getWidth()+"px";
    node.style.height = getHeight()+"px";
    //alert("setupContentArea successfully...");
    
    
}

function setupImage(setMetadataURL, imageURL, zoom, origWidth, origHeight) {

    // init
    var req;
    setMetadataURL = appendSizeOfImageAreaOnURL(setMetadataURL);
    
    // transmit
    if (window.XMLHttpRequest) {
        req = new XMLHttpRequest();
    }
        else if (window.ActiveXObject) {
            req = new ActiveXObject("Microsoft.XMLHTTP");
        }
        
    req.open("GET", setMetadataURL, true);
    
    req.onreadystatechange=function() {
    //alert("befor req.readyState==4 successfully, started to getImage...");
      if (req.readyState==4) {
      //alert("req.readyState==4 successfully, started to getImage...");
       if (req.status==200) {
           //var title = req.responseXML.getElementsByTagName("setMetadata")[0];
           //document.getElementsByTagName("title").value=title;
           //alert("AJAX transmission of screen resolution successfully, started to getImage...");
           getImage(imageURL, zoom, origWidth,origHeight);
       }
      }     
    }
    req.send(null);     
}

var init = false;
function setupHighlight(x, y, sf, zoom, thumbWidth, thumbHeight) {
    if (zoom=='fitToWidth') {
        zoom = getWidth() / (thumbWidth/sf);
    }
    var node = document.getElementById("previewImage");
    var width = parseInt((getWidth()/zoom)*sf);
    var height = parseInt((getHeight()/zoom)*sf);
    //alert("width: " + getWidth() + " - " + getHeight() + " - sf: " + sf +"\nwidth: " + width + " - height: " + height);
    if (width>parseInt(thumbWidth)) {
        width=thumbWidth;
    }
    if (height>parseInt(thumbHeight)) {
        height=thumbHeight;
    }
    
    if (width!="0" & height!="0") {
        node.style.position="relative";
        node.style.top=(y-2)+"px";
        //node.style.top=30+"px";
        
        node.style.background = "transparent";
        node.style.border = "rgb(194, 136, 0) ridge 2px";
        node.style.overlow = "hidden";
        
        var node_inner = document.getElementById("previewImage_inner");
        node_inner.style.background = "rgb(244, 186, 48)";
        
        if (navigator.appName.indexOf("Microsoft")!= -1 &&parseInt(navigator.appVersion)>=4){
        
            node_inner.style.filter= "alpha(opacity=40)";
            }
        else
            node_inner.style.MozOpacity = 0.40;
        
        if (zoom=="fitToWidth") {
            node.style.left="-2px";         
            node.style.width = thumbWidth+"px";
            // original bild 
            var widthOrig = thumbWidth/sf;
            var currentScaleFactor = getWidth()/widthOrig;
            node.style.height = (getHeight()/currentScaleFactor)*sf+"px";
            
            node_inner.style.left="-2px";           
            node_inner.style.width = thumbWidth+"px";
            // original bild 
            node_inner.style.height = (getHeight()/currentScaleFactor)*sf+"px";                 
        }
        else {
            //alert(x + " # " + zoom + " # " + sf + " # " + height);
            node.style.left=(x-2)+"px";     
            node.style.width = width+"px";
            node.style.height = height+"px";
            
            node_inner.style.left=(x-2)+"px";       
            node_inner.style.width = width+"px";
            node_inner.style.height = height+"px";  
        }
        
        node.sf = sf;
        node.zoom = zoom;
        node.thumbWidth = thumbWidth;
        node.thumbHeight = thumbHeight;
        DragThumb.init(node);
    } 
}

function switchImage(url) {
    document.getElementById('imageSwitcher').action = url;
    document.getElementById('imageSwitcher').submit();
}

/************************************************/
/************************************************/
/*** internal functions *************************/
/************************************************/
/************************************************/

function getImage(imageURL, zoom, origWidth, origHeight) {
    // init dom
    var node = document.getElementById("content-container");
    var div = document.createElement("div");
    var img = document.createElement("img");
    
    var width = getWidth();
    var height = getHeight();
    
    var sf1 = width/origWidth;
    var sf2 = height/origHeight;
    var sf = (sf1 < sf2) ? sf1 : sf2;
        
    img.id = "iPic";
    img.src = imageURL;
    img.style.position = "relative";
    if (testIE()) {
        //if (isNaN(zoom)){
        if (zoom == 'fitToScreen') {
            
            if (origWidth * sf < width){
                img.style.width = origWidth * sf + "px";
            }
            else
                img.style.width = width + "px";
                
            if (origHeight * sf < height){
                img.style.height = origHeight * sf + "px";
            }
            else
                img.style.height = height + "px";
        }
        else if (zoom == 'fitToWidth') {
            img.style.width = width + "px";
            
            if (origHeight * sf2 < height){
                img.style.height = origHeight * sf2 + "px";
            }
            else{
                img.style.height = height + "px";
            }
        }
        else{
            if (parseFloat(zoom) * origWidth < width){
            img.style.width = origWidth * parseFloat(zoom) + "px";
            }
            else{
                img.style.width = width + "px";
            }
            
            if (parseFloat(zoom) * origHeight < height){
                img.style.height = origHeight * parseFloat(zoom) + "px";
            }
            else{
                img.style.height = height + "px";
            }
                
        }
    }
    
    div.id = "imageView";
    div.style.position = "relative";
    //div.style.overflow = "scroll";
    div.style.width = width + "px";
    div.style.height = height + "px";
    //div.style.background = "url(" + imageURL + ") no-repeat";
    
    if(scrollMode())
    	{
    	 if (zoom == 'fitToScreen') {
			if (origWidth * sf < width){
				img.style.width = origWidth * sf + "px";
			}
			else
				img.style.width = width + "px";
				
			if (origHeight * sf < height){
				img.style.height = origHeight * sf + "px";
			}
			else
				img.style.height = height + "px";
		}
		else if (zoom == 'fitToWidth') {
			if(origHeight * sf1 > height)
				{
				 img.style.width = width - getScrollbarWidth() + "px";
			     img.style.height = origHeight * ((width - getScrollbarWidth())/(origWidth - getScrollbarWidth())) + "px";
				}
			else
				{
				 img.style.width = width + "px";
			     img.style.height = origHeight * sf1 + "px";
				}			
		}
		else{
			img.style.width = origWidth * parseFloat(zoom) + "px";
			img.style.height = origHeight * parseFloat(zoom) + "px";
		}
    }
    
    div.appendChild(img);
    node.appendChild(div)
    Drag.init(document.getElementById("iPic"));
}

function scrollMode() { 
    var test = document.getElementById("content-container").getAttribute("style");
	    
    if(testIE())
    	{
    	 if(test["overflow"].match("scroll"))
    	 	{
    	 	 return true;
    	 	}
    	 else 
    	 	{
    	 		return false;
    	 	}
    	}
     else
     	{
     	 if(test.match("overflow: scroll"))
    	 	{
    	 	 return true;
    	 	}
    	 else 
    	 	{
    	 	return false;
    	 	}
     	}	
}

function appendSizeOfImageAreaOnURL(url) {
    var width = getWidthOfImage();
    var height = getHeightOfImage();    
    if (url.indexOf("?") != -1) {
        url += "&XSL.browser.res.width.SESSION=" + width + "&XSL.browser.res.height.SESSION="+height;
    }
    else { url += "?XSL.browser.res.width.SESSION="+width+"&XSL.browser.res.height.SESSION="+height; }  
    return url;
}

/************************************************/
/*** calculate available resolution *************/
/************************************************/

function getHeight() {
    var browserHeight = getBrowserHeight();
    var headerHeight = document.getElementById("naviArea").offsetHeight;
    var height = browserHeight - headerHeight - getScrollbarHeight();
    return height;
}

function getWidth() {
    //alert("getBrowserWidth() =" + getBrowserWidth() );
    return getBrowserWidth() - getScrollbarWidth();
}

function getWidthOfImage() {
    return getWidth()-17;
}
function getHeightOfImage() {
    return getHeight();
}

function getScrollbarHeight(){
    if (testIE()){
        return 15;
    }
        else
        return 29;
}
function getScrollbarWidth(){
    if (testIE()){
        return 13;
    }
        else
        return 15;
}

function testIE(){
 if (navigator.appName=="Microsoft Internet Explorer")
    return true;
 else
    return false;
}

function getBrowserHeight () {

  if (window.innerHeight) {
    return window.innerHeight;
  } 
      else if (document.documentElement && document.documentElement.clientHeight) {
        return document.documentElement.clientHeight;
      } 
          else if (document.body && document.body.clientHeight) {
            return document.body.clientHeight;
          } 
              else if (document.body && document.body.offsetHeight) {
                return document.body.offsetHeight;
              } 
              else {
                return 0;
              }
}
function getBrowserWidth () {
    
  if (window.innerWidth) {
    return window.innerWidth;
  } 
      else if (document.documentElement && document.documentElement.clientWidth) {
        return document.documentElement.clientWidth;
      } 
        else if (document.body && document.body.clientWidth) {
            return document.body.clientWidth;
        } 
            else if (document.body && document.body.offsetWidth) {
                return document.body.offsetWidth;
            } 
            else {
                return 0;
            }
}

/************************************************/
/* Moving Div                                   */
/************************************************/
var Drag = {
obj:null,
oldTop:0,  //Y
oldLeft:0, //X
    
init:function(o){
    o.onmousedown = Drag.start;
    var obj = Drag.obj = this;
    
    obj.currentTop = parseInt(obj.offsetTop);
    obj.currentLeft = parseInt(obj.offsetLeft);
    o.style.cursor = "pointer";
},

start:function(e){
    e = Drag.fixE(e);
    var obj = Drag.obj = this;
    
    // save current position of drag-object
    obj.currentTop = parseInt(obj.offsetTop);
    obj.currentLeft = parseInt(obj.offsetLeft);
    oldTop = obj.currentTop;
    oldLeft = obj.currentLeft;
    
    // save current position of the mouse cursor
    obj.mouseX = parseInt(e.clientX);
    obj.mouseY = parseInt(e.clientY);
    
    // when mouse is moving call drag function
    document.onmousemove = Drag.drag;
    // when release mouse button call stop function
    document.onmouseup = Drag.stop;
    obj.style.cursor = "move";
    return false;
},

drag:function(e){
    e = Drag.fixE(e);
    var obj = Drag.obj;
    
    // new position of the mouse
    newX = e.clientX;
    newY = e.clientY;
    
    // the differece of the move
    dx =  newX - obj.mouseX;
    dy =  newY - obj.mouseY;
        
    // new position of drag-object
    obj.currentTop = obj.currentTop + dy;
    obj.currentLeft = obj.currentLeft + dx;
    
    // set new position for drag-object
    obj.style.top = obj.currentTop + "px";
    obj.style.left = obj.currentLeft +"px";
    
    // save the new mouse position
    obj.mouseX = newX;
    obj.mouseY = newY;
    
    return false;
},

stop:function(){
    var formX = document.getElementById("dragX");
    var formY = document.getElementById("dragY");
  
  
    formX.value = Drag.obj.currentLeft - oldLeft;
    
    formY.value = Drag.obj.currentTop - oldTop;
    
    document.onmousemove = null;
    document.onmouseup = null;
    Drag.obj.style.cursor = "pointer";
    Drag.obj=null;
    document.dragImage.submit();
    //alert(dragURL);
},

fixE : function(e)
{
    if (typeof e == 'undefined') e = window.event;
    if (typeof e.layerX == 'undefined') e.layerX = e.offsetX;
    if (typeof e.layerY == 'undefined') e.layerY = e.offsetY;
    return e;
}

};

function fitWidth(o){
    o.style.width = window.innerwidth+"px";
}

function fitScreen(o){
    o.style.width = window.innerwidth+"px";
    o.style.height = window.innerheight+"px";
}

var DragThumb = {
obj:null,
oldTop:0,  //Y
oldLeft:0, //X
    
init:function(o){
    o.onmousedown = DragThumb.start;
    var obj = DragThumb.obj = this;
    
    obj.currentTop = parseInt(obj.offsetTop);
    obj.currentLeft = parseInt(obj.offsetLeft);
    o.style.cursor = "pointer";
},

start:function(e){
    e = DragThumb.fixE(e);
    var obj = DragThumb.obj = this;
    
    // save current position of drag-object
    obj.currentTop = parseInt(obj.offsetTop);
    obj.currentLeft = parseInt(obj.offsetLeft);
    oldTop = obj.currentTop;
    oldLeft = obj.currentLeft;
    
    // save current position of the mouse cursor
    obj.mouseX = parseInt(e.clientX);
    obj.mouseY = parseInt(e.clientY);
    
    // when mouse is moving call drag function
    document.onmousemove = DragThumb.drag;
    // when release mouse button call stop function
    document.onmouseup = DragThumb.stop;
    obj.style.cursor = "move";
    return false;
},

drag:function(e){
    e = DragThumb.fixE(e);
    var obj = DragThumb.obj;
    
    
    // new position of the mouse
    newX = e.clientX;
    newY = e.clientY;
    
    // the differece of the move
    dx =  newX - obj.mouseX;
    dy =  newY - obj.mouseY;
        
    // new position of drag-object
    var newTop = obj.currentTop + dy;
    var newLeft = obj.currentLeft + dx;
    
    // set new position for drag-object
    if (newTop + parseInt(obj.style.height) <= obj.thumbHeight && newTop >= -2){
        obj.currentTop = newTop;
        obj.style.top = obj.currentTop + "px";
        obj.mouseY = newY;
    }
    if (newLeft + parseInt(obj.style.width) <= obj.thumbWidth && newLeft >= -2){
        obj.currentLeft = newLeft;
        obj.style.left = obj.currentLeft +"px";
        obj.mouseX = newX;
    }
    
    // save the new mouse position
    
    
    
    return false;
},

stop:function(){
    var formX = document.getElementById("dragX");
    var formY = document.getElementById("dragY");
    //alert("formXY\n" + formX.value + " # " + formY.value);
    var thumbscale = DragThumb.obj.sf;
    var currentscale = DragThumb.obj.zoom;
    
    formX.value = parseInt((oldLeft - DragThumb.obj.currentLeft)/thumbscale * currentscale);
    
    formY.value = parseInt((oldTop - DragThumb.obj.currentTop )/thumbscale * currentscale);
    
    /*alert(thumbscale + " # " + currentscale +"\n" + formX.value + " # " + formY.value
           +"\n" + DragThumb.obj.currentLeft + " # " + DragThumb.obj.currentTop
           +"\n" + oldLeft + " # " + oldTop);*/
    document.onmousemove = null;
    document.onmouseup = null;
    DragThumb.obj.style.cursor = "pointer";
    DragThumb.obj=null;
    document.dragImage.submit();
    //alert(dragURL);
},

fixE : function(e)
{
    if (typeof e == 'undefined') e = window.event;
    if (typeof e.layerX == 'undefined') e.layerX = e.offsetX;
    if (typeof e.layerY == 'undefined') e.layerY = e.offsetY;
    return e;
}

};

