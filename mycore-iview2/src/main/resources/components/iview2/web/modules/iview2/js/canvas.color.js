(function() {
    "use strict";
    
    /*NOTE: to change color it's need to call these functions:
     * this.setPixelModifier(50, 0, 0);
     * this.changeCanvasColorIntensity();
     */
    
    function cv_getPixelModifier(){
        return this.pixelModifier;           
    }
    
    function cv_setPixelModifier(red, green, blue){
        red = (red < 0) ? 0 : red;
        green = (green < 0) ? 0 : green;
        blue = (blue < 0) ? 0 : blue;
       
        red = (red > 255) ? 255 : red;
        green = (green > 255) ? 255 : green;
        blue = (blue > 255) ? 255 : blue;
       
        this.pixelModifier.red = red; 
        this.pixelModifier.green = green;
        this.pixelModifier.blue = blue;
    }
    
    function cv_changeCanvasColorIntensity() {
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation;
        var cnvWidth = this.context2D.canvas.width;
        var cnvHeight = this.context2D.canvas.height;
                   
        if(rotation == 90 || rotation == 270){
            cnvHeight  = cnvWidth;
        }
       
        var canvasArea = this.context2D.getImageData(0, 0, cnvWidth, cnvHeight);
        var numberOfPixel = cnvWidth * cnvHeight * 4;
       
        for(var channel = 0; channel < numberOfPixel; channel += 4){
            canvasArea.data[channel]  += this.pixelModifier.red;
            canvasArea.data[channel+1] += this.pixelModifier.green;
            canvasArea.data[channel+2] += this.pixelModifier.blue;
        }
               
        this.context2D.putImageData(canvasArea, 0, 0);
    }
    
    if (iview.isCanvasAvailable) {
        
        iview.Canvas.Color = (function() {

            function constructor(iviewInst) {               
                iview.IViewObject.call(this, iviewInst);
            }
            return constructor;
            
            
        });
        
        jQuery(document).bind(iview.IViewInstance.INIT_EVENT, function(event, iViewInst) {
            iViewInst.color = new iview.Canvas.Color(iViewInst);
            
            // override necessary functions
            iViewInst.canvas.pixelModifier = {'red' : 0, 'green' : 0, 'blue' : 0};
            iViewInst.canvas.getPixelModifier = cv_getPixelModifier;
            iViewInst.canvas.setPixelModifier = cv_setPixelModifier;
            iViewInst.canvas.changeCanvasColorIntensity = cv_changeCanvasColorIntensity;
        });
    }
    
})();