System.register([], function(exports_1, context_1) {
    "use strict";
    var __moduleName = context_1 && context_1.id;
    var Settings;
    return {
        setters:[],
        execute: function() {
            class Settings {
                constructor(hS, aS) {
                    this.historySize = hS;
                    this.autoscroll = aS;
                }
            }
            exports_1("Settings", Settings);
        }
    }
});
