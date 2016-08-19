/// <reference path="AltoElement.ts" />
/// <reference path="AltoFile.ts" />


module mycore.viewer.widgets.alto {
    export class AltoHTMLGenerator {

        constructor() {

        }


        public generateHtml(alto:widgets.alto.AltoFile):HTMLElement {
            var element = document.createElement("div");
            element.style.position = "absolute";
            element.style.whiteSpace = "nowrap";

            var endecoderElem = document.createElement("span");
            var mesureCanvas = document.createElement("canvas");
            let context = <CanvasRenderingContext2D>mesureCanvas.getContext('2d');

            var buff = alto.getLines().map((line)=> {
                context.font = line.getHeight() + "px " + "sans-serif";
                var fhpos = -1, fvpos = -1;
                let lineArr = line.getChildren().filter(elementInLine=>elementInLine.getType()==AltoElementType.String).map((word, wordCount, allWords) => {
                    if (fhpos == -1) {
                        fhpos = word.getHPos();
                    }
                    if (fvpos == -1) {
                        fvpos = word.getVPos();
                    }
                    endecoderElem.innerText = word.getContent();
                    let encodedWord = endecoderElem.innerHTML;
                    //let wordHtml = "<div style=\"position: absolute; top: " + word.getVPos() + "px; left: " + word.getHPos() + "px; font-size: "+ word.getHeight()+"px;\">" + encodedWord + "</div>";
                    let wordHtml = encodedWord;
                    return wordHtml;
                });

                var htmlLine = lineArr.join("&nbsp;");
                endecoderElem.innerHTML = htmlLine;

                var scale = line.getWidth() / (context.measureText(endecoderElem.innerText)).width;

                return "<p class='altoLine' style='text-align: justify; position: absolute; " +
                    "top: " + line.getVPos() + "px; " +
                    "width: " + line.getWidth() + "px;" +
                    "height:" + line.getHeight() + "px;" +
                    "left: " + line.getHPos() + "px; " +
                    "font-size: " + line.getHeight() + "px; " +
                    "transform: scale(" + scale + ",1);" +
                    "transform-origin: left top'>" + endecoderElem.innerHTML + "</p>";
            });

            element.innerHTML = buff.join("");


            return element;
        }

    }
}