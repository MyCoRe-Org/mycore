/// <reference path="AltoStyle.ts" />
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
            let context:CanvasRenderingContext2D = <CanvasRenderingContext2D>mesureCanvas.getContext('2d');
            
            var fontFamily = "sans-serif";

            var buff = alto.getBlocks().map((block) => {
                var blockDiv:string = "<div";
                blockDiv += " class='altoBlock'";
                blockDiv += " style='top: " + block.getVPos() + "px;";
                blockDiv += " left: " + block.getHPos() + "px;";
                blockDiv += " width: " + block.getWidth() + "px;";
                blockDiv += " height: " + block.getHeight() + "px;";
                blockDiv += "'>";

                block.getChildren().map((line) => {
                    // get line content
                    let lineArr = line.getChildren().filter(elementInLine=>elementInLine.getType()==AltoElementType.String).map((word, wordCount, allWords) => {
                        endecoderElem.innerText = word.getContent();
                        let encodedWord = endecoderElem.innerHTML;
                        let wordHtml = encodedWord;
                        return wordHtml;
                    });
                    var htmlLine = lineArr.join("&nbsp;");
                    endecoderElem.innerHTML = htmlLine;                    

                    // build line
                    var lineDiv:string = "<p";
                    lineDiv += " class='altoLine'";
                    lineDiv += " style='height: " + line.getHeight() + "px;";
                    lineDiv += " width: " + line.getWidth() + "px;";
                    lineDiv += " left: " + line.getHPos() + "px;";
                    lineDiv += " top: " + line.getVPos() + "px;";

                    // build style
                    var lineStyle:AltoStyle = line.getStyle();
                    if(lineStyle != null) {
                        var ctxFont = "";
                        var lineHeight = line.getHeight() - 4;
                        var lineFontStyle = lineStyle.getFontStyle();
                        if(lineFontStyle != null) {
                            if(lineFontStyle == "italic") {
                                lineDiv += " font-style: italic;";
                                ctxFont = "italic ";
                            } else if(lineFontStyle == "bold") {
                                lineDiv += " font-weight: bold;";
                                ctxFont = "bold ";
                            }
                        }
                        ctxFont += lineHeight + "px " + fontFamily;
                        context.font = ctxFont;
                        var scale = line.getWidth() / (context.measureText(endecoderElem.innerText)).width;
                        lineDiv += " font-size: " + lineHeight + "px;";
                        lineDiv += " transform: scale(" + scale + ", 1);";
                    }

                    lineDiv += "'>" + endecoderElem.innerHTML + "</p>";
                    blockDiv += lineDiv;
                });
                blockDiv += "</div>";
                return blockDiv;
            });
            element.innerHTML = buff.join("");
            return element;
        }

    }
}
