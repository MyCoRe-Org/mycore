/*
 * "Constructor" for the clickButton object
 */
function createClickButton(buttonID, spec){
    var buttonElement;
    
    if (buttonElement = document.getElementById(buttonID)){
        if (!buttonElement.loaded) {
            buttonElement.loaded = true;
            var text = spec.text || "";
            var target = spec.target || "";
            var cssMouseOver = spec.cssMouseOver || "";
            var cssMouseOut = spec.cssMouseOut || "";
            var onclick = spec.onclick || null;
            try { onclick = eval(onclick); } catch (e) { onclick = null; }
        
            buttonElement.object = new clickButton(buttonElement, onclick, text, target, cssMouseOver, cssMouseOut);
        }
    
        return buttonElement.object;
    } else{
        return null;
    }
}

/*
 * Defintion of a click button with OO-Javascript
 */
function clickButton(button, onclick, text, target, cssMouseOver, cssMouseOut) {
    if (button == null)
        return;
        
    this.onclick = onclick;
        
    this.enabled = true;
        
    this._init(button, text, target, cssMouseOver, cssMouseOut);
}

clickButton.prototype._init = function(button, text, target, cssMouseOver, cssMouseOut){
    var _self = this;
    
    try{
        button.style.MozUserSelect="none"
    }catch(error){
        button.onselectstart=function(){return false}
    }
    
    button.cssMouseOver = cssMouseOver;
    button.cssMouseOut = cssMouseOut;
    button.onmouseover = _self._mouseOverHandler;
    button.onmouseout = _self._mouseOutHandler;
    button.onmousemove = doNothing;
    button.ondblclick = doNothing;
    button.onselect = doNothing;
    button.onselectstart = doNothing;
    button.onmousedown = doNothing;
    button.onclick = _self.onclick;
    button.target = target;
    
    this.elem = button;
}

function doNothing(){return false;}

clickButton.prototype._mouseOverHandler = function(event){
    var _self = this;
    
    this.className = _self.cssMouseOver;
}

clickButton.prototype._mouseOutHandler = function(event){
    var _self = this;
    
    this.className = _self.cssMouseOut;
}
