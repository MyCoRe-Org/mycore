/// <reference path="AltoStyle.ts" />
/// <reference path="AltoElement.ts" />
/// <reference path="AltoFile.ts" />

namespace mycore.viewer.widgets.alto {
    export class AltoHTMLGenerator {

        constructor() {
        }


        public generateHtml(alto:widgets.alto.AltoFile, pageID:string):HTMLElement {
            let fontFamily = "sans-serif";
            let element = document.createElement("div");

            element.style.position = "absolute";
            element.style.whiteSpace = "nowrap";
            element.style.fontFamily = "sans-serif";

            let endecoderElem = document.createElement("span");
            let mesureCanvas = document.createElement("canvas");
            let ctx:CanvasRenderingContext2D = <CanvasRenderingContext2D>mesureCanvas.getContext("2d");
            let outline:number = alto.pageHeight * 0.002;

            let blockBefore:AltoElement = null;
            let buff = alto.getBlocks().map((block) => {
                let drawOutline:boolean = blockBefore !== null &&
                                          (blockBefore.getVPos() + blockBefore.getHeight() + outline < block.getVPos() ||
                                           blockBefore.getHPos() + blockBefore.getWidth() + outline < block.getHPos());
                let blockFontSize:number = this.getFontSize(ctx, block, fontFamily);
                blockFontSize *= 0.9;


                let blockDiv:string = "<div";
                blockDiv += " class='altoBlock'";
                blockDiv += " style='";
                blockDiv += ` transform: translate(${Math.round(block.getHPos())}px,${Math.round(block.getVPos())}px );`;
                blockDiv += ` width: ${block.getWidth()}px;`;
                blockDiv += ` height: ${block.getHeight()}px;`;
                blockDiv += ` font-size: ${blockFontSize}px;`;
                if(drawOutline) {
                    blockDiv += ` outline: ${outline}px solid white;`;
                }
                blockDiv += "'>";

                block.getChildren().map((line) => {


                    // build line
                    let lineDiv:string = "<p";
                    lineDiv += " class='altoLine'";
                    lineDiv += ` style='height: ${line.getHeight()}px;`;
                    lineDiv += ` width: ${line.getWidth()}px;`;
                    lineDiv += ` transform: translate(${Math.round(line.getHPos()-block.getHPos())}px, ${Math.round(line.getVPos()-block.getVPos())}px);`;

                    // build style
                    let lineStyle:AltoStyle = line.getStyle();

                    if(lineStyle != null) {
                        let lineFontStyle = lineStyle.getFontStyle();
                        if(lineFontStyle != null) {
                            if(lineFontStyle == "italic") {
                                lineDiv += " font-style: italic;";
                            } else if(lineFontStyle == "bold") {
                                lineDiv += " font-weight: bold;";
                            }
                        }
                    }

                    lineDiv += "'>" + this.getLineAsElement(line) + "</p>";
                    blockDiv += lineDiv;
                });
                blockDiv += "</div>";
                blockBefore = block;
                return blockDiv;
            });
            element.innerHTML = buff.join("");
            return element;
        }

        private getWordsArray(line:AltoElement):Array<AltoElement> {
            return line.getChildren()
                       .filter(elementInLine => elementInLine.getType() === AltoElementType.String);
        }

        private getLineAsString(line:AltoElement):string {
            let span = document.createElement("span");
            return this.getWordsArray(line).map((line:AltoElement)=>{
                span.innerText = line.getContent();
                return span.innerHTML;
            }).join(" ");
        }

        private getFontSize(ctx:CanvasRenderingContext2D, block:AltoElement, fontFamily:string) {
            let getFontStyle = (line:AltoElement) => {
                let lineStyle:AltoStyle = line.getStyle();
                if(lineStyle !== null) {
                    let lineFontStyle = lineStyle.getFontStyle();
                    return lineFontStyle !== null ? (lineFontStyle  + " ") : "";
                }
                return "";
            };
            let getLineHeight = (line:AltoElement, startSize:number):number => {
                let lineString:string = this.getLineAsString(line);
                ctx.font = getFontStyle(line) + startSize + "px " + fontFamily;
                let widthScale = block.getWidth() / ctx.measureText(lineString).width;
                return widthScale > 1 ? startSize : startSize * widthScale;
            };
            if(block.getChildren().length === 1) {
                let line = block.getChildren()[0];
                return getLineHeight(line, line.getHeight());
            }
            let maxSize:number = 9999;
            block.getChildren().forEach(line => {
                maxSize = getLineHeight(line, maxSize);
            });
            return maxSize;
        }

        private getLineAsElement(line: mycore.viewer.widgets.alto.AltoElement) {
            let span = document.createElement("word");
            return this.getWordsArray(line).map(word=>{
                span.innerText = word.getContent();
                return `<span data-vpos="${word.getVPos()}" data-hpos="${word.getHPos()}" data-word="${word.getContent()}">${span.innerHTML}</span>`
            }).join(" ")
        }
    }
}
