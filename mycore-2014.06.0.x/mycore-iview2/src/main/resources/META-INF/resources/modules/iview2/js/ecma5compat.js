//this is for opera, ff<4, ie<9
if (typeof Object.create !== 'function') {
  Object.create = function(o, properties) {
    var F = function() {
    };
    F.prototype = o;
    var r = new F();
    if (typeof properties !== "undefined") {
      Object.defineProperties(r, properties);
    }
    return r;
  };
}

if (typeof Object.defineProperties !== 'function') {
  Object.defineProperties = function(o, properties) {
    if (typeof o !== "object")
      throw new TypeError("Argument is not an object");
    for (name in properties) {
      // TODO
      o[name] = properties[name].value;
    }
  };
}
