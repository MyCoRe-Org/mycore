/*****************************************************************************
 *
 * Copyright (c) 2003-2004 Kupu Contributors. All rights reserved.
 *
 * This software is distributed under the terms of the Kupu
 * License. See LICENSE.txt for license text. For a list of Kupu
 * Contributors see CREDITS.txt.
 *
 *****************************************************************************/

// $Id: kupueditor.js 6772 2004-09-28 11:55:11Z guido $

//----------------------------------------------------------------------------
// Main classes
//----------------------------------------------------------------------------

/* KupuDocument
    
    This essentially wraps the iframe.
    XXX Is this overkill?
    
*/

function KupuDocument(iframe) {
    /* Model */
    
    // attrs
    this.editable = iframe; // the iframe
    this.window = this.editable.contentWindow;
    this.document = this.window.document;

    this._browser = _SARISSA_IS_IE ? 'IE' : 'Mozilla';
    
    // methods
    this.execCommand = function(command, arg) {
        /* delegate execCommand */
        // XXX Is the command always a string? Can't it be '' or 0 or so?
        if (!arg) arg = null;
        this.document.execCommand(command, false, arg);
    };
    
    this.reloadSource = function() {
        /* reload the source */
        
        // XXX To temporarily work around problems with resetting the
        // state after a reload, currently the whole page is reloaded.
        // XXX Nasty workaround!! to solve refresh problems...
        document.location = document.location;
    };

    this.getDocument = function() {
        /* returns a reference to the window.document object of the iframe */
        return this.document;
    };

    this.getWindow = function() {
        /* returns a reference to the window object of the iframe */
        return this.window;
    };

    this.getSelection = function() {
        if (this._browser == 'Mozilla') {
            return new MozillaSelection(this);
        } else {
            return new IESelection(this);
        };
    };

    this.getEditable = function() {
        return this.editable;
    };
};

/* KupuEditor

    This controls the document, should be used from the UI.
    
*/

function KupuEditor(document, config, logger) {
    /* Controller */
    
    // attrs
    this.document = document; // the model
    this.config = config; // an object that holds the config values
    this.log = logger; // simple logger object
    this.tools = {}; // mapping id->tool
    this.filters = new Array(); // contentfilters
    
    this._designModeSetAttempts = 0;
    this._initialized = false;

    // some properties to save the selection, required for IE to remember where 
    // in the iframe the selection was
    this._previous_range = null;

    // this property is true if the content is changed, false if no changes are made yet
    this.content_changed = false;

    // methods
    this.initialize = function() {
        /* Should be called on iframe.onload, will initialize the editor */
        //DOM2Event.initRegistration();
        this._initializeEventHandlers();
        this.getDocument().getWindow().focus();
        if (this.getBrowserName() == "IE") {
            var body = this.getInnerDocument().getElementsByTagName('body')[0];
            body.setAttribute('contentEditable', 'true');
            // provide an 'afterInit' method on KupuEditor.prototype
            // for additional bootstrapping (after editor init)
            this._initialized = true;
            if (this.afterInit) {
                this.afterInit();
            };
            this._saveSelection();
        } else {
            this._setDesignModeWhenReady();
        };
        this.logMessage('Editor initialized');
    };
    
    this.setContextMenu = function(menu) {
        /* initialize the contextmenu */
        menu.initialize(this);
    };

    this.registerTool = function(id, tool) {
        /* register a tool */
        this.tools[id] = tool;
        tool.initialize(this);
    };

    this.getTool = function(id) {
        /* get a tool by id */
        return this.tools[id];
    };

    this.registerFilter = function(filter) {
        /* register a content filter method

            the method will be called together with any other registered
            filters before the content is saved to the server, the methods
            can be used to filter any trash out of the content. they are
            called with 1 argument, which is a reference to the rootnode
            of the content tree (the html node)
        */
        this.filters.push(filter);
        filter.initialize(this);
    };

    this.updateStateHandler = function(event) {
        /* check whether the event is interesting enough to trigger the 
        updateState machinery and act accordingly */
        var interesting_codes = new Array(8, 13, 37, 38, 39, 40, 46);
        // unfortunately it's not possible to do this on blur, since that's
        // too late. also (some versions of?) IE 5.5 doesn't support the
        // onbeforedeactivate event, which would be ideal here...
        if (this.getBrowserName() == 'IE') {
            this._saveSelection();
        };

        if (event.type == 'click' || 
                (event.type == 'keyup' && 
                    interesting_codes.contains(event.keyCode))) {
            // Filthy trick to make the updateState method get called *after*
            // the event has been resolved. This way the updateState methods can
            // react to the situation *after* any actions have been performed (so
            // can actually stay up to date).
            this.updateState(event);
        }
    };
    
    this.updateState = function(event) {
        /* let each tool change state if required */
        // first see if the event is interesting enough to trigger
        // the whole updateState machinery
        var selNode = this.getSelectedNode();
        for (var id in this.tools) {
            try {
                this.tools[id].updateState(selNode, event);
            } catch (e) {
                if (e == UpdateStateCancelBubble) {
                    this.updateState(event);
                    break;
                } else {
                    this.logMessage('Exception while processing updateState on ' + id + ': ' + e, 2);
                };
            };
        };
    };
    
    this.saveDocument = function(redirect, synchronous) {
        /* save the document, redirect if te arg is provided and the save is successful 
        
            the (optional) redirect argument can be used to make the client jump to
            another URL when the save action was successful.
        */
        
        // if no dst is available, bail out
        if (!this.config.dst) {
            this.logMessage('No destination URL available!', 2);
            return;
        }
    
        // make sure people can't edit or save during saving
        if (!this._initialized) {
            return;
        }
        this._initialized = false;
        
        // set the window status so people can see we're actually saving
        window.status= "Please wait while saving document...";

        // pass the content through the filters
        this.logMessage("Starting HTML cleanup");
        var transform = this._filterContent(this.getInnerDocument().documentElement);

        // serialize to a string
        var contents = this._serializeOutputToString(transform);
        
        this.logMessage("Cleanup done, sending document to server");
        var request = Sarissa.getXmlHttpRequest();
    
        if (!synchronous) {
            request.onreadystatechange = (new ContextFixer(this._saveCallback, 
                                               this, request, redirect)).execute;
            request.open("PUT", this.config.dst, true);
            request.setRequestHeader("Content-type", this.config.content_type);
            request.send(contents);
            this.logMessage("Request sent to server");
        } else {
            this.logMessage('Sending request to server');
            request.open("PUT", this.config.dst, false);
            request.setRequestHeader("Content-type", this.config.content_type);
            request.send(contents);
            if (redirect) { // && (!request.status || request.status == '200' || request.status == '204'))
                window.document.location = redirect;
            } else if (request.status != '200' && request.status != '204'){
                alert('Error saving your data.\nResponse status: ' + 
                    request.status + 
                    '.\nCheck your server log for more information.')
                window.status = "Error saving document"
            } else {
                if (this.config.reload_after_save) {
                    this.reloadSrc();
                };
                // we're done so we can start editing again
                window.status= "Document saved";
            };
        };
        this.content_changed = false;
    };
    
    this.prepareForm = function(form, id) {
        /* add a field to the form and place the contents in it

            can be used for simple POST support where Kupu is part of a
            form
        */
        // make sure people can't edit or save during saving
        if (!this._initialized) {
            return;
        }
        this._initialized = false;
        
        // set the window status so people can see we're actually saving
        window.status= "Please wait while saving document...";

        // set a default id
        if (!id) {
            id = 'kupu';
        };
        
        // pass the content through the filters
        this.logMessage("Starting HTML cleanup");
        var transform = this._filterContent(this.getInnerDocument().documentElement);
        
        // XXX need to fix this.  Sometimes a spurious "\n\n" text 
        // node appears in the transform, which breaks the Moz 
        // serializer on .xml
        var contents =  this._serializeOutputToString(transform);
        
        this.logMessage("Cleanup done, sending document to server");
        
        // now create the form input, since IE 5.5 doesn't support the 
        // ownerDocument property we use window.document as a fallback (which
        // will almost by definition be correct).
        var document = form.ownerDocument ? form.ownerDocument : window.document;
        var ta = document.createElement('textarea');
        ta.style.visibility = 'hidden';
        var text = document.createTextNode(contents);
        ta.appendChild(text);
        ta.setAttribute('name', id);
        
        // and add it to the form
        form.appendChild(ta);
    };

    this.execCommand = function(command, param) {
        /* general stuff like making current selection bold, italics etc. 
            and adding basic elements such as lists
            */
        if (!this._initialized) {
            this.logMessage('Editor not initialized yet!');
            return;
        };
        if (command != 'useCSS') {
            this.content_changed = true;
        };
        if (this.getBrowserName() == "IE") {
            this._restoreSelection();
        } else {
            this.getDocument().getWindow().focus();
        };
        this.getDocument().execCommand(command, param);
        var message = 'Command ' + command + ' executed';
        if (param) {
            message += ' with parameter ' + param;
        }
        this.updateState();
        this.logMessage(message);
    };
    
    this.getSelection = function() {
        /* returns a Selection object wrapping the current selection */
        this._restoreSelection();
        return this.getDocument().getSelection();
    };
    
    this.getSelectedNode = function() {
        /* returns the selected node (read: parent) or none */
        this._restoreSelection();
        var sel = this.getSelection();
        return sel.getSelectedNode();
    };

    this.getNearestParentOfType = function(node, type) {
        /* well the title says it all ;) */
        // just to be sure...
        this._restoreSelection();
        var type = type.toLowerCase();
        while (node) {
            if (node.nodeName.toLowerCase() == type) {
                return node
            }   
            var node = node.parentNode;
        }
    
        return false;
    };

    this.removeNearestParentOfType = function(node, type) {
        var nearest = this.getNearestParentOfType(node, type);
	if (!nearest) {
	    return false;
	};
	var parent = nearest.parentNode;
	while (nearest.childNodes.length) {
	    var child = nearest.firstChild;
	    child = nearest.removeChild(child);
	    parent.insertBefore(child, nearest);
	};
	parent.removeChild(nearest);
    };

    this.getDocument = function() {
        /* returns a reference to the document object that wraps the iframe */
        return this.document;
    };

    this.getInnerDocument = function() {
        /* returns a reference to the window.document object of the iframe */
        return this.getDocument().getDocument();
    };

    this.insertNodeAtSelection = function(insertNode, selectNode) {
        /* insert a newly created node into the document */
        if (!this._initialized) {
            this.logMessage('Editor not initialized yet!');
            return;
        };

        this.content_changed = true;

        var win = this.getDocument().getWindow();
        var browser = this.getBrowserName();
        if (browser == "IE") {
            this._restoreSelection();
        } else {
            win.focus();
        };
        
        var ret = this.getSelection().replaceWithNode(insertNode, selectNode);
        
        if (browser == 'IE') {
            this._saveSelection();
        };

        return ret;
    };

    this.logMessage = function(message, severity) {
        /* log a message using the logger, severity can be 0 (message, default), 1 (warning) or 2 (error) */
        this.log.log(message, severity);
    };

    this.registerContentChanger = function(element) {
        /* set this.content_changed to true (marking the content changed) when the 
            element's onchange is called
        */
        addEventHandler(element, 'change', function() {this.content_changed = true;}, this);
    };
    
    // helper methods
    this.getBrowserName = function() {
        /* returns either 'Mozilla' (for Mozilla, Firebird, Netscape etc.) or 'IE' */
        if (_SARISSA_IS_MOZ) {
            return "Mozilla";
        } else if (_SARISSA_IS_IE) {
            return "IE";
        } else {
            throw "Browser not supported!";
        }
    };
    
    // private methods
    this._addEventHandler = addEventHandler;

    this._saveCallback = function(request, redirect) {
        /* callback for Sarissa */
        if (request.readyState == 4) {
            if (redirect) { // && (!request.status || request.status == '200' || request.status == '204'))
                window.document.location = redirect;
            } else if (request.status != '200' && request.status != '204'){
                alert('Error saving your data.\nResponse status: ' + 
                    request.status + 
                    '.\nCheck your server log for more information.')
                window.status = "Error saving document"
            } else {
                if (this.config.reload_after_save) {
                    this.reloadSrc();
                };
                // we're done so we can start editing again
                window.status= "Document saved";
            };
            this._initialized = true;
        };
    };
    
    this.reloadSrc = function() {
        /* reload the src, called after a save when reload_src is set to true */
        // XXX Broken!!!
        /*
        if (this.getBrowserName() == "Mozilla") {
            this.getInnerDocument().designMode = "Off";
        }
        */
        // XXX call reloadSrc() which has a workaround, reloads the full page
        // instead of just the iframe...
        this.getDocument().reloadSource();
        if (this.getBrowserName() == "Mozilla") {
            this.getInnerDocument().designMode = "On";
        };
        /*
        var selNode = this.getSelectedNode();
        this.updateState(selNode);
        */
    };

    this._initializeEventHandlers = function() {
        /* attache the event handlers to the iframe */
        // Initialize DOM2Event compatibility
        // XXX should come back and change to passing in an element
        this._addEventHandler(this.getInnerDocument(), "click", this.updateStateHandler, this);
        this._addEventHandler(this.getInnerDocument(), "dblclick", this.updateStateHandler, this);
        this._addEventHandler(this.getInnerDocument(), "keyup", this.updateStateHandler, this);
        this._addEventHandler(this.getInnerDocument(), "keyup", function() {this.content_changed = true}, this);
        if (this.getBrowserName() == "IE") {
            this._addEventHandler(this.getInnerDocument(), "focus", this._clearSelection, this);
        };
    };

    this._setDesignModeWhenReady = function() {
        /* Rather dirty polling loop to see if Mozilla is done doing it's
            initialization thing so design mode can be set.
        */
        this._designModeSetAttempts++;
        if (this._designModeSetAttempts > 25) {
            alert('Couldn\'t set design mode. Kupu will not work on this browser.');
            return;
        };
        var success = false;
        try {
            this._setDesignMode();
            success = true;
        } catch (e) {
            // register a function to the timer_instance because 
            // window.setTimeout can't refer to 'this'...
            timer_instance.registerFunction(this, this._setDesignModeWhenReady, 100);
        };
        if (success) {
            // provide an 'afterInit' method on KupuEditor.prototype
            // for additional bootstrapping (after editor init)
            if (this.afterInit) {
                this.afterInit();
            };
        };
    };

    this._setDesignMode = function() {
        this.getInnerDocument().designMode = "On";
        this.execCommand("undo");
        // note the negation: the argument doesn't work as expected...
        this._initialized = true;
        // XXX somehow calling execCommand('useCSS',...) here doesn't seem to have effect unless it's
        // called with a timeout... don't know why, crappy workaround...
        timer_instance.registerFunction(this, this.execCommand, 0, "useCSS", !this.config.use_css);
    };

    this._saveSelection = function() {
        /* Save the selection, works around a problem with IE 
        where the selection in the iframe gets lost */
        this._clearSelection();
        var currange = this.getInnerDocument().selection.createRange();
        this._previous_range = currange;
    };

    this._restoreSelection = function() {
        /* re-selects the previous selection in IE */
        if (this._previous_range && !this._isDocumentSelected()) {
            try {
                this._previous_range.select();
            } catch (e) {
                this.logMessage('Error placing back selection');
            };
        };
    };

    this._isDocumentSelected = function() {
        var editable_body = this.getInnerDocument().getElementsByTagName('body')[0];
        var selrange = this.getInnerDocument().selection.createRange();
        var someelement = selrange.parentElement ? selrange.parentElement() : selrange.item(0);

        while (someelement.nodeName.toLowerCase() != 'body') {
            someelement = someelement.parentNode;
        };
        
        return someelement == editable_body;
    };

    this._clearSelection = function() {
        /* clear the last stored selection */
        this._previous_range = null;
    };

    this._filterContent = function(documentElement) {
        /* pass the content through all the filters */
        // first copy all nodes to a Sarissa document so it's usable
        var xhtmldoc = Sarissa.getDomDocument();
        var doc = this._convertToSarissaNode(xhtmldoc, documentElement);
        // now pass it through all filters
        for (var i=0; i < this.filters.length; i++) {
            var doc = this.filters[i].filter(xhtmldoc, doc);
        };
        // fix some possible structural problems, such as an empty or missing head, title
        // or script or textarea tags without closing tag...
        this._fixXML(doc, xhtmldoc);
        return doc;
    };

    this._fixXML = function(doc, document) {
        /* fix some structural problems in the XML that make it invalid XTHML */
        // find if we have a head and title, and if not add them
        var heads = doc.getElementsByTagName('head');
        var titles = doc.getElementsByTagName('title');
        if (!heads.length) {
            // assume we have a body, guess Kupu won't work without one anyway ;)
            var body = doc.getElementsByTagName('body')[0];
            var head = document.createElement('head');
            body.parentNode.insertBefore(head, body);
            var title = document.createElement('title');
            var titletext = document.createTextNode('');
            head.appendChild(title);
            title.appendChild(titletext);
        } else if (!titles.length) {
            var head = heads[0];
            var title = document.createElement('title');
            var titletext = document.createTextNode('');
            head.appendChild(title);
            title.appendChild(titletext);
        };
        // create a closing element for all elements that require one in XHTML
        // XXX probably need more of those here...
        var dualtons = new Array('script', 'textarea', 'title');
        for (var i=0; i < dualtons.length; i++) {
            var elname = dualtons[i];
            var els = doc.getElementsByTagName(elname);
            for (var j=0; j < els.length; j++) {
                var el = els[j];
                if (!el.hasChildNodes()) {
                    var child = document.createTextNode('');
                    el.appendChild(child);
                };
            };
        };
    };

    this._convertToSarissaNode = function(ownerdoc, htmlnode) {
        /* Given a string of non-well-formed HTML, return a string of 
           well-formed XHTML.
        
           This function works by leveraging the already-excellent HTML 
           parser inside the browser, which generally can turn a pile 
           of crap into a DOM.  We iterate over the HTML DOM, appending 
           new nodes (elements and attributes) into a node.
        
           The primary problems this tries to solve for crappy HTML: mixed 
           element names, elements that open but don't close, 
           and attributes that aren't in quotes.  This can also be adapted 
           to filter out tags that you don't want and clean up inline styles.
        
           Inspired by Guido, adapted by Paul from something in usenet. 
        */

        var i, name, val;
        var nodename = htmlnode.nodeName;
        try {
            var xhtmlnode = ownerdoc.createElement(nodename.toLowerCase());
        } catch (e) {
            var xhtmlnode = ownerdoc.createElement('span');
        };
    
        var atts = htmlnode.attributes;
        for (var i = 0; i < atts.length; i++) {
            name = atts[i].nodeName;
            val = atts[i].nodeValue;
            if (!(val == null || val == "" || name == "contentEditable" ||
                  ((name == "rowSpan" || name == "colSpan") && val == 1) )) {
                xhtmlnode.setAttribute(name.toLowerCase(), val);
            }
        } 
    
        var kids = htmlnode.childNodes;
        if (kids.length == 0) {
            if (htmlnode.text && htmlnode.text != "") {
                var text = htmlnode.text;
                var tnode = ownerdoc.createTextNode(text);
                xhtmlnode.appendChild(tnode);
            }
        } else { 
            for (var i = 0; i < kids.length; i++) {
                if (kids[i].nodeType == 1) {
                    var newkid = this._convertToSarissaNode(ownerdoc, kids[i]);
                    if (newkid != null) {
                        xhtmlnode.appendChild(newkid);
                    };
                } else if (kids[i].nodeType == 3) {
                    xhtmlnode.appendChild(ownerdoc.createTextNode(kids[i].nodeValue));
                } else if (kids[i].nodeType == 4) {
                    xhtmlnode.appendChild(ownerdoc.createCDATASection(kids[i].nodeValue));
                } 
            }
        } 
    
        return xhtmlnode;
    };

    this._serializeOutputToString = function(transform) {
        // XXX need to fix this.  Sometimes a spurious "\n\n" text 
        // node appears in the transform, which breaks the Moz 
        // serializer on .xml
            
        if (this.config.strict_output) {
            var contents =  '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" ' + 
                            '"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">\n' + 
                            '<html xmlns="http://www.w3.org/1999/xhtml">' + 
                            transform.getElementsByTagName("head")[0].xml +
                            transform.getElementsByTagName("body")[0].xml +
                            '</html>';
        } else {
            var contents = '<html>' + 
                            transform.getElementsByTagName("head")[0].xml +
                            transform.getElementsByTagName("body")[0].xml +
                            '</html>';
        };

        if (this.config.compatible_singletons) {
            contents = contents.replace(/<([^>]+)\/>/g, "<$1 />");
        };
        
        return contents;
    };
}

