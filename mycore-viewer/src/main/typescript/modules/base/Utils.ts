/// <reference path="definitions/jquery.d.ts" />

interface iterator<T> {
    hasPrevious(): boolean;
    hasNext(): boolean;
    previous(): T;
    next(): T;
    current(): T;
}

var VIEWER_COMPONENTS:Array<any> = IVIEW_COMPONENTS || [];

function addViewerComponent(component:any) {
    VIEWER_COMPONENTS.push(component);
}

function addIviewComponent(component:any) {
    console.warn("addIviewComponent shouldnt be used anymore!");
    VIEWER_COMPONENTS.push(component);
}

class ArrayIterator<T> implements iterator<T> {

    constructor(private _array:Array<T>) {
        this.iterator = 0;
    }

    private iterator:number;

    hasPrevious():boolean {
        return this.iterator != 0;
    }

    hasNext():boolean {
        return this.iterator + 1 < this._array.length;
    }

    previous():T {
        return this._array[--this.iterator];
    }

    next():T {
        return this._array[++this.iterator];
    }

    current():T {
        return this._array[this.iterator];
    }

    reset():void {
        this.iterator = 0;
    }
}

class Position2D {
    constructor(private _x:number, private _y:number) {
    }

    public toString() {
        return this.x + ":" + this.y;
    }

    public move(vec:MoveVector):Position2D {
        return new Position2D(this._x + vec.x, this._y + vec.y);
    }

    public roundDown():Position2D {
        return new Position2D(Math.floor(this.x), Math.floor(this.y));
    }

    public roundUp():Position2D {
        return new Position2D(Math.ceil(this.x), Math.ceil(this.y));
    }

    public scale(scale:number):Position2D {
        return new Position2D(this._x * scale, this._y * scale);
    }

    public copy():Position2D {
        return new Position2D(this.x, this.y);
    }

    public static fromString(str:string):Position2D {
        var values = str.split(':');
        return new Position2D(parseInt(values[0], 10), parseInt(values[1], 10));
    }

    public get x() {
        return this._x;
    }

    public get y() {
        return this._y;
    }

    public set y(y:number) {
        throw "y of position is unmodifyable!";
    }

    public set x(x:number) {
        throw "x of position is unmodifyable!";
    }

    public rotate(rotation:number):Position2D {
        var x = this._x;
        var y = this._y;
        switch (rotation) {
            case 90:
                var s = x;
                x = -y;
                y = s;
            case 180:
                var s = x;
                x = -y;
                y = s;
            case 270:
                var s = x;
                x = -1 * y;
                y = s;
            case 0:
        }
        return new Position2D(x, y);
    }

    public rotateAround(center:Position2D, rotation:number):Position2D {
        rotation = rotation * -1; // invert for clockwise rotation
        var diffX = (this.x - center.x);
        var diffY = (this.y - center.y);
        var x = center.x + diffX * Math.cos(rotation) - diffY * Math.sin(rotation);
        var y = center.y + diffX * Math.sin(rotation) + diffY * Math.cos(rotation);
        return new Position2D(x,y);
    }

    public toPosition3D(z:number):Position3D {
        return new Position3D(this.x, this.y, z);
    }

    public equals(p:any):boolean {
        if ("x" in p && "y" in p) {
            return p.x == this.x && p.y == this.y;
        } else {
            return false;
        }
    }

    public min(x:number, y:number):Position2D {
        return new Position2D(Math.min(this.x, x), Math.min(this.y, y));
    }

    public max(x:number, y:number):Position2D {
        return new Position2D(Math.max(this.x, x), Math.max(this.y, y));
    }
}

class MoveVector extends Position2D {
    constructor(x:number, y:number) {
        super(x, y);
    }
}

class Position3D {
    constructor(public x:number, public y:number, public z:number) {
    }

    public toString() {
        return this.z + ":" + this.x + ":" + this.y;
    }

    public toPosition2D():Position2D {
        return new Position2D(this.x, this.y);
    }
}

class Size2D {
    constructor(public width:number, public height:number) {
    }

    public toString() {
        return this.width + ":" + this.height;
    }

    public roundUp():Size2D {
        return new Size2D(Math.ceil(this.width), Math.ceil(this.height));
    }

    public scale(scale:number):Size2D {
        return new Size2D(this.width * scale, this.height * scale);
    }

    public copy():Size2D {
        return new Size2D(this.width, this.height);
    }

    public getRotated(deg:number) {
        if (deg == 0 || deg == 180) {
            return this.copy();
        } else {
            var rotatedSize = new Size2D(this.height, this.width);
            return rotatedSize;
        }
    }

    public maxSide() {
        return Math.max(this.width, this.height);
    }

    public getSurface() {
        return this.width * this.height;
    }

    roundDown() {
        return new Size2D(Math.floor(this.width), Math.floor(this.height));
    }
}

class Rect {

    constructor(public pos:Position2D, public size:Size2D) {
    }

    public getPoints() {
        return {
            upperLeft: this.pos,
            upperRight: new Position2D(this.pos.x + this.size.width, this.pos.y),
            lowerLeft: new Position2D(this.pos.x, this.pos.y + this.size.height),
            lowerRight: new Position2D(this.pos.x + this.size.width, this.pos.y + this.size.height)
        };
    }

    public getX():number {
        return this.pos.x;
    }

    public getY():number {
        return this.pos.y;
    }

    public getWidth():number {
        return this.size.width;
    }

    public getHeight():number {
        return this.size.height;
    }
    
    public scale(scale:number):Rect {
        return new Rect(this.pos.scale(scale), this.size.scale(scale));
    }

    public getIntersection(r2:Rect):Rect {
        var r1 = this;
        var xmin = Math.max(r1.pos.x, r2.pos.x);
        var xmax1 = r1.pos.x + r1.size.width;
        var xmax2 = r2.pos.x + r2.size.width;
        var xmax = Math.min(xmax1, xmax2);
        if (xmax > xmin) {
            var ymin = Math.max(r1.pos.y, r2.pos.y);
            var ymax1 = r1.pos.y + r1.size.height;
            var ymax2 = r2.pos.y + r2.size.height;
            var ymax = Math.min(ymax1, ymax2);
            if (ymax > ymin) {
                return new Rect(new Position2D(xmin, ymin), new Size2D(xmax - xmin, ymax - ymin));
            }
        }
        return null;
    }

    public intersects(p:Position2D):boolean {
        return p.x < this.pos.x + this.size.width && p.x > this.pos.x && p.y < this.pos.y + this.size.height && p.y > this.pos.y;
    }

    public rotate(deg:number) {
        return new Rect(this.pos.rotate(deg), this.size.getRotated(deg));
    }


    public flipX():Rect {
        var x = this.pos.x + this.size.width;
        var width = -this.size.width;
        return new Rect(new Position2D(x, this.pos.y), new Size2D(width, this.size.height));
    }

    public flipY():Rect {
        var y = this.pos.y + this.size.height;
        var height = -this.size.height;
        return new Rect(new Position2D(this.pos.x, y), new Size2D(this.size.width, height));
    }

    public flip(deg:number):Rect {
        if(deg == 90) {
            return this.flipX();
        } else if(deg == 180) {
            return this.flipX().flipY();
        } else if(deg == 270) {
            return this.flipY();
        }
        return this.copy();
    }

    public equals(obj) {
        if (obj instanceof Rect || ("pos" in obj && "size" in obj && "_x" in obj.pos && "_y" in obj.pos
            && "width" in obj.size && "height" in obj.size)) {
            return obj.pos.x == this.pos.x && obj.pos.y == this.pos.y && obj.size.width == this.size.width && obj.size.height == this.size.height;
        }
    }

    public getMiddlePoint():Position2D {
        return new Position2D(this.pos.x + (this.size.width / 2), this.pos.y + (this.size.height / 2));
    }

    public toString() {
        return this.pos.toString() + " - " + this.size.toString();
    }

    public getRotated(deg:number):Rect {
        var midPos = this.getMiddlePoint();
        var rotatedSize = this.size.getRotated(deg);
        var newUpperLeft = new Position2D(midPos.x - (rotatedSize.width / 2), midPos.y - (rotatedSize.height / 2));
        return new Rect(newUpperLeft, rotatedSize);
    }

    /**
     * Tries to maximize the bounds.
     */
    public maximize(x:number, y:number, width:number, height:number):Rect {
        var right1:number = this.pos.x + this.size.width;
        var right2:number = x + width;
        var bottom1:number = this.pos.y + this.size.height;
        var bottom2:number = y + height;
        var newX:number = x < this.pos.x ? x : this.pos.x;
        var newY:number = y < this.pos.y ? y : this.pos.y;
        var newWidth:number = Math.max(right1, right2) - newX;
        var newHeight:number = Math.max(bottom1, bottom2) - newY;
        return Rect.fromXYWH(newX, newY, newWidth, newHeight);
    }

    /**
     * Increase the rect with the given number of pixel's on all sides.
     * This is like adding a padding.
     */
    public increase(pixel:number):Rect {
        var x = this.pos.x - pixel;
        var y = this.pos.y - pixel;
        var width = this.size.width + (2 * pixel);
        var height = this.size.height + (2 * pixel);
        return Rect.fromXYWH(x, y, width, height);
    }

    public copy():Rect {
        return new Rect(this.pos.copy(), this.size.copy());
    }

    public static fromXYWH(x:number, y:number, width:number, height:number) {
        return new Rect(new Position2D(x, y), new Size2D(width, height));
    }

    public static fromULLR(upperLeft:Position2D, lowerRight:Position2D):Rect {
        return new Rect(new Position2D(upperLeft.x, upperLeft.y), new Size2D(lowerRight.x - upperLeft.x, lowerRight.y - upperLeft.y));
    }

    public static getBounding(...n:Rect[]):Rect {
        var max = Math.pow(2, 31);
        var top = max, left = max, bottom = -max, right = -max;

        n.forEach((nthRect)=> {
            top = Math.min(top, nthRect.pos.y);
            bottom = Math.max(bottom, nthRect.pos.y + nthRect.size.height);
            left = Math.min(left, nthRect.pos.x);
            right = Math.max(right, nthRect.pos.x + nthRect.size.width);
        });

        return Rect.fromULLR(new Position2D(left, top), new Position2D(right, bottom));
    }

}

class Utils {
    public static LOG_HALF = Math.log(1 / 2);

    public static canvasToImage(canvas:HTMLCanvasElement):HTMLImageElement {
        var image = document.createElement("img");
        image.src = canvas.toDataURL();
        return image;
    }

    public static getVar<T>(obj:any, path:string, defaultReturn:T = null, check = (extracted:T) => true):T {
        // check direct
        if (path in obj) {
            if (check(obj[ path ])) {
                return obj[ path ];
            }
        }

        // check child objects
        var pathPartEnd = path.indexOf(".");
        if (pathPartEnd == -1) {
            pathPartEnd = path.length;
        }

        var part = path.substring(0, pathPartEnd);
        if (part in obj && obj[ part ] != null && typeof obj[ part ] != "undefined") {
            if (pathPartEnd != path.length) {
                return Utils.getVar(obj[ part ], path.substring(part.length + 1), defaultReturn, check);
            } else {
                // path is complete
                if (check(obj[ part ])) {
                    return obj[ part ];
                }
            }
        }

        return defaultReturn;
    }

    public static synchronize<T>(conditions:Array<(synchronizeObj:T) => boolean>, then:(synchronizeObj:T) => void) {
        return (synchronizeObj:any) => {
            var matchingConditions = conditions.filter((condition:(synchronizeObj:any) => boolean) => {
                return condition(synchronizeObj);
            });
            if (matchingConditions.length == conditions.length) {
                then(synchronizeObj);
            }
        };
    }

    public static createRandomId() {
        return "nnnnnn-nnnn-nnnn-nnnnnnnn".split("n").map((n) => {
            return n + Math.ceil(15 * Math.random()).toString(36);
        }).join("");
    }

    public static hash(str:string) {
        var hash = 0;
        for (var i = 0; i < str.length; i++) {
            hash = ((hash<<5) -hash) + str.charCodeAt(i);
            hash = hash & hash;
        }
        return hash;
    }

    public static stopPropagation = function (e:JQueryMouseEventObject) {
        e.stopImmediatePropagation(); // this prevents the global mouse event handler (wich stops selection)
    };

    public static selectElementText(element) {
      element.select();
    }
}

class MyCoReMap<K, V> {

    private static BASE_KEY_TO_HASH_FUNCTION = (key) => {
        return key.toString();
    };
    private static KEY_PREFIX:string = "MyCoReMap.";

    constructor(arr?:any) {
        if (typeof arr != "undefined") {
            for (var key in arr) {
                this.set(<any>key, arr[key]);
            }
        }
    }

    private arr = {};
    private keyArray:Array<K> = [];
    private keyToHashFunction:(key:K) => void = MyCoReMap.BASE_KEY_TO_HASH_FUNCTION;

    public set(key:K, value:V) {
        this.arr[this.getHash(key)] = value;
        this.keyArray.push(key);
    }

    public get(key:K):V {
        return this.arr[this.getHash(key)];
    }

    public setKeyToHashFunction(keyToHashFunction:(key:K) => void) {
        this.keyToHashFunction = keyToHashFunction;
    }

    public hasThen(key:K, consumer:(value:V)=>void):void {
        if(this.has(key)){
            consumer(this.get(key));
        }
    }

    public get keys():Array<K> {
        return this.keyArray.slice();
    }

    public get values():Array<V> {
        var values:Array<V> = new Array();
        for (var i in this.arr) {
            values.push(this.arr[i]);
        }
        return values;
    }

    public has(key:K) {
        if (typeof key == "undefined" || key == null) {
            return false;
        }
        var elem = this.arr[this.getHash(key)];
        return typeof elem != "undefined" && elem != null;
    }

    public forEach(call:(key:K, value:V) => void) {
        for (var key of this.keyArray) {
            call(key, this.get(key));
        }
    }

    public filter(call:(key:K, value:V) => boolean):MyCoReMap<K, V> {
        var newMap:MyCoReMap<K, V> = new MyCoReMap<K, V>();
        for (var key of this.keyArray) {
            var value:V = this.get(key);
            if(call(key, value)) {
                newMap.set(key, value);
            }
        }
        return newMap;
    }

    public copy():MyCoReMap<K, V> {
        var copy = new MyCoReMap<K, V>();
        for (var key of this.keyArray) {
            copy.set(key, this.get(key));
        }
        return copy;
    }

    public remove(key:K) {
        delete this.arr[this.getHash(key)];
        var index = this.keyArray.indexOf(key);
        if(index >= 0) {
            this.keyArray.splice(index, 1);
        }
    }

    public clear() {
        var keys = this.keyArray.slice();
        for (var key of keys) {
            this.remove(key);
        }
    }

    public mergeIn(...maps:MyCoReMap<K, V>[]) {
        var that = this;
        for (var mapIndex in maps) {
            var currentMap = maps[mapIndex];
            currentMap.forEach((k, v) => {
                that.set(<any>k, v);
            });
        }
    }

    public isEmpty():boolean {
        return Object.keys(this.arr).length <= 0;
    }

    private getHash(key:K):string {
        return MyCoReMap.KEY_PREFIX + this.keyToHashFunction(key);
    }

}

class ViewerError {
    constructor(message:string, error:any = null) {
        this.informations = new MyCoReMap<string, any>();

        this.informations.set("message", message);
        this.informations.set("error", error);
        this.informations.set("callee", arguments.callee);


        console.log(this.toString());
        console.trace();
    }

    toString() {
        return this.informations.get("message");
    }

    private informations:MyCoReMap<string, any>;
}

/**
 * Used for Observer-Pattern.
 */
class ViewerProperty<T> {

    constructor(private _from:any, private _name:string, private _value:T = null) {
        this.observerArray = new Array<ViewerPropertyObserver<T>>();
    }

    private propertyChanging:boolean = false;
    private observerArray:Array<ViewerPropertyObserver<T>>;

    public get name():string {
        return this._name;

    }

    public get value():T {
        return this._value;
    }

    public set value(value:T) {
        var old = this.clone();
        this._value = value;
        this.notifyPropertyChanged(old, this);
    }

    public get from() {
        return this._from;
    }

    private clone():ViewerProperty<T> {
        return new ViewerProperty<T>(this._from, this.name, this.value);
    }

    public removeAllObserver():void {
        while (this.observerArray.pop());
    }

    public removeObserver(observer:ViewerPropertyObserver<T>):void {
        var index = this.observerArray.indexOf(observer);
        this.observerArray.splice(index, 1);
    }

    public addObserver(observer:ViewerPropertyObserver<T>):void {
        this.observerArray.push(observer);
    }

    public notifyPropertyChanged(_old:ViewerProperty<T>, _new:ViewerProperty<T>):void {
        this.propertyChanging = true;
        this.observerArray.forEach((element:any) => {
            if (this.propertyChanging) {
                element.propertyChanged(_old, _new);
            }
        });
        this.propertyChanging = false;
    }

}

function ViewerFormatString(pattern:string, args:any) {
    var replaceArg = (pattern:string, i:any, arg:string) => pattern.replace("{" + i + "}", arg);
    var resultPattern:string = pattern;
    for (var index in args) {
        resultPattern = replaceArg(resultPattern, index, args[index]);
    }
    return resultPattern;
}


interface ViewerPropertyObserver<T> {
    propertyChanged(_old:ViewerProperty<T>, _new:ViewerProperty<T>);
}

interface ContainerObserver<T1, T2> {
    childAdded(that:T1, component:T2): void;
    childRemoved(that:T1, component:T2): void;
}

var viewerRequestAnimationFrame:(callback:() => void) => void = (function (window_:any = <any>window) {
    return window.requestAnimationFrame ||
        window_.webkitRequestAnimationFrame ||
        window_.mozRequestAnimationFrame ||
        function (callback) {
            window.setTimeout(callback, 1000 / 60);
        };
})(window);


var viewerCancelRequestAnimationFrame:(callback:() => void) => void = (function (window_:any = <any>window) {
    return window.cancelAnimationFrame ||
        window_.webkitCancelRequestAnimationFrame ||
        window_.mozCancelRequestAnimationFrame ||
        function (callback) {
            window.clearTimeout(callback);
        };
})(window);

/**
 * Wrapper for localStorage of the browser. If localStorage is not supported it saves items to a Map(Only for one Document).
 */
class ViewerUserSettingStore {

    private static LOCK = false;
    private static _INSTANCE:ViewerUserSettingStore;

    /**
     * Gets the singleton instance of IviewUserSettingsStore.
     * @returns {ViewerUserSettingStore}
     */
    public static getInstance() {
        if (typeof ViewerUserSettingStore._INSTANCE == "undefined" || ViewerUserSettingStore._INSTANCE == null) {
            ViewerUserSettingStore._INSTANCE = new ViewerUserSettingStore();
        }

        return ViewerUserSettingStore._INSTANCE;
    }


    constructor() {
        if (typeof ViewerUserSettingStore._INSTANCE !== "undefined" && ViewerUserSettingStore._INSTANCE !== null) {
            throw new ViewerError("Its a Singleton use instance instead!");
        }
        this._browserStorageSupport = typeof Storage !== "undefined";

        if (!this.browserStorageSupport) {
            this._sessionMap = new MyCoReMap<string, string>();
        }
    }

    /**
     * Gets a Value from the Store
     * @param key
     * @returns {*}
     */
    public getValue(key:string) {
        if (this.browserStorageSupport) {
            return window.localStorage.getItem(key);
        } else {
            return this._sessionMap.get(key);
        }
    }

    /**
     * Sets a Value in the Store
     * @param key
     * @param value
     */
    public setValue(key:string, value:string) {
        if (this.browserStorageSupport) {
            window.localStorage.setItem(key, value);
        } else {
            this._sessionMap.set(key, value);
        }
    }

    /**
     * Checks a Value is in the Store
     * @param key
     * @returns {boolean}
     */
    public hasValue(key:string) {
        if (this.browserStorageSupport) {
            return window.localStorage.getItem(key) !== null
        } else {
            return this._sessionMap.has(key);
        }
    }

    private _browserStorageSupport:boolean;
    private _sessionMap:MyCoReMap<string, string>;

    /**
     * Does the Browser support localStorage
     * @returns {boolean}
     */
    public get browserStorageSupport() {
        return this._browserStorageSupport;
    }
}


//TODO: extends this
function isFullscreen() {
    var d = (<any>document);
    return d.fullscreenElement != null || d.webkitFullscreenElement != null;
}

var viewerDeviceSupportTouch = ('ontouchstart' in window);

function viewerCrossBrowserWheel(element:HTMLElement, handler:(e:{
    deltaX: number; deltaY: number; orig: any
    ; pos: Position2D; altKey?: boolean, ctrlKey?:boolean
}) => void) {
    var internHandler = (e:any) => {
        e.preventDefault();
        var x = (e.clientX - jQuery(element).offset().left);
        var y = (e.clientY - jQuery(element).offset().top);


        var pos = new Position2D(x, y).scale(window.devicePixelRatio);

        if ("deltaX" in e) {
            handler({deltaX: e.deltaX, deltaY: e.deltaY, orig: e, pos: pos, altKey: e.altKey, ctrlKey: e.ctrlKey});
            return;
        }

        var horizontal:boolean = e.shiftKey;
        if ("axis" in e) {
            horizontal = (e.axis == 1);

            if ("detail" in e) {
                var pixel = e.detail;

                var obj = {deltaX: 0, deltaY: 0, orig: e, pos: pos, altKey: e.altKey, ctrlKey: e.ctrlKey};

                if (horizontal) {
                    obj.deltaX = pixel;
                } else {
                    obj.deltaY = pixel;
                }

                handler(obj);
                return;
            }

        }

        if ("wheelDelta" in e) {
            var val = <number>-e.wheelDelta;
            var deltaObj = <any>((e.shiftKey) ? {"deltaX": val, "deltaY": 0} : {"deltaX": 0, "deltaY": val});
            deltaObj.orig = e;
            deltaObj.pos = pos;
            deltaObj.altKey = e.altKey;
            handler(deltaObj);
        }
    };
    if (element.addEventListener) {
        // Chrome
        element.addEventListener("mousewheel", <any>internHandler, false);
        // Firefox
        element.addEventListener("MozMousePixelScroll", <any>internHandler, false);
        // IE
        //element.attachEvent("onwheel", <any>internHandler);
    }
}

interface GivenViewerPromise<T1,T2> {
    then(handler:(result:T1)=>void):void;
    onreject(handler:(reason:T2)=>void):void;
}

class ViewerPromise<T1,T2> implements GivenViewerPromise<T1,T2> {

    constructor() {
    }

    private static DEFAULT = aAny=> {
    };

    private _result:T1 = null;
    private _rejectReason:any = null;

    private _then:(result:T1)=>void = ViewerPromise.DEFAULT;
    private _onReject:(reason:any)=>void = ViewerPromise.DEFAULT;

    public then(handler:(result:T1)=>void):void {
        this._then = handler;
        if (this._result != null) {
            handler(this._result);
        }
        return;
    }

    public onreject(handler:(reason:T2)=>void):void {
        this._onReject = handler;
        if (this._rejectReason != null) {
            this._onReject(this._rejectReason);
        }
    }

    public reject(reason:T2):void {
        this._rejectReason = reason;
        if (this._onReject != ViewerPromise.DEFAULT) {
            (this._onReject)(reason);
        }
        return;
    }

    public resolve(reason:T1):void {
        this._result = reason;
        if (this._then != ViewerPromise.DEFAULT) {
            (this._then)(reason);
        }
        return;
    }

}

class ViewerParameterMap extends MyCoReMap<string, string> {
    constructor() {
        super();
    }

    public toParameterString() {
        var stringBuilder = new Array<string>()
        this.forEach((key, value) => {
            stringBuilder.push(key + "=" + value);
        });
        var s = stringBuilder.join("&");
        return s.length > 0 ? "?" + s : "";
    }

    public static fromCurrentUrl():ViewerParameterMap {
        return ViewerParameterMap.fromUrl(window.location.href);
    }

    public static fromUrl(url):ViewerParameterMap {
        var map = new ViewerParameterMap();
        var parameter = url.split("?")[1];
        if (typeof parameter != "undefined") {
            var parameterWithoutHash = parameter.split("#")[0];
            var mapElements = parameter.split("&");

            for (var currentElementIndex in mapElements) {
                var currentElement = mapElements[currentElementIndex];
                var keyValueArray = currentElement.split("=");
                map.set(keyValueArray[0], decodeURIComponent(keyValueArray[1]));
            }
        }

        return map;
    }

}

function singleSelectShim(xml:Document, xpath:string, nsMap:MyCoReMap<string, string>):Node {

    if ("evaluate" in document) {
        /**
         * Every Browser Solution
         */
        var nsResolver = (nsPrefix:string) => {
            return nsMap.get(nsPrefix);
        };

        return (<any>xml).evaluate(xpath, xml.documentElement, nsResolver, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    } else {
        /**
         * MS IE Solution
         */
        var documentAsMSXML = new ActiveXObject("Msxml2.DOMDocument.6.0");
        documentAsMSXML.async = false;
        documentAsMSXML.load(xml);

        var nsCollector = "";
        nsMap.keys.forEach((key:string) => {
            var part = "xmlns:" + key + "='" + nsMap.get(key) + "' ";
            nsCollector = nsCollector + part;
        });
        documentAsMSXML.setProperty("SelectionNamespaces", nsCollector);
        documentAsMSXML.setProperty("SelectionLanguage", "XPath");
        return documentAsMSXML.documentElement.selectSingleNode(xpath);
    }
};

class XMLUtil {

    public static iterateChildNodes(element:Node, iter:(node:Node) => void) {
        var childNodes = element.childNodes;
        for (var i = 0; i < childNodes.length; i++) {
            iter(childNodes.item(i));
        }
    }

    public static nodeListToNodeArray(nodeList:NodeList):Array<Node> {
        var array = new Array();
        for (var i = 0; i < nodeList.length; i++) {
            array.push(nodeList.item(i));
        }

        return array;
    }

    public static getOneChild(element:Node, isTheOne:(node:Node) => boolean):Node {
        var childNodes = element.childNodes;
        return XMLUtil.getOneOf(childNodes, isTheOne);
    }

    public static getOneOf(childNodes:NodeList, isTheOne:(node:Node) => boolean):Node {
        for (var i = 0; i < childNodes.length; i++) {
            var currentChild = childNodes.item(i);
            if (isTheOne(currentChild)) return currentChild;
        }
        return null;
    }

    private static METS_NAMESPACE_URI = "http://www.loc.gov/METS/";
    private static XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";

    public static NS_MAP = (() => {
        var nsMap = new MyCoReMap<string, string>();
        nsMap.set("mets", XMLUtil.METS_NAMESPACE_URI);
        nsMap.set("xlink", XMLUtil.XLINK_NAMESPACE_URI);
        return nsMap;
    })();
}


class ClassDescriber {
    static getName(inputClass) {
        var funcNameRegex = /function (.{1,})\(/;
        var results = (funcNameRegex).exec((<any> inputClass).constructor.toString());
        return (results && results.length > 1) ? results[1] : "";
    }

    static ofEqualClass(class1, class2) {
        return ClassDescriber.getName(class1) == ClassDescriber.getName(class2);
    }

}
