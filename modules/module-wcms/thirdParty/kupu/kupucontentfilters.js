/*****************************************************************************
 *
 * Copyright (c) 2003-2004 Kupu Contributors. All rights reserved.
 *
 * This software is distributed under the terms of the Kupu
 * License. See LICENSE.txt for license text. For a list of Kupu
 * Contributors see CREDITS.txt.
 *
 *****************************************************************************/

// $Id: kupucontentfilters.js 6772 2004-09-28 11:55:11Z guido $


//----------------------------------------------------------------------------
// 
// ContentFilters
//
//  These are (or currently 'this is') filters for HTML cleanup and 
//  conversion. Kupu filters should be classes that should get registered to
//  the editor using the registerFilter method with 2 methods: 'initialize'
//  and 'filter'. The first will be called with the editor as its only
//  argument and the latter with a reference to the ownerdoc (always use 
//  that to create new nodes and such) and the root node of the HTML DOM as 
//  its arguments.
//
//----------------------------------------------------------------------------

function NonXHTMLTagFilter() {
    /* filter out non-XHTML tags*/
    
    // A mapping from element name to whether it should be left out of the 
    // document entirely. If you want an element to reappear in the resulting 
    // document *including* it's contents, add it to the mapping with a 1 value.
    // If you want an element not to appear but want to leave it's contents in 
    // tact, add it to the mapping with a 0 value. If you want an element and
    // it's contents to be removed from the document, don't add it.
    if (arguments.length) {
        // allow an optional filterdata argument
        this.filterdata = arguments[0];
    } else {
        // provide a default filterdata dict
        this.filterdata = {'html': 1,
                            'body': 1,
                            'head': 1,
                            'title': 1,
                            
                            'a': 1,
                            'abbr': 1,
                            'acronym': 1,
                            'address': 1,
                            'b': 1,
                            'base': 1,
                            'blockquote': 1,
                            'br': 1,
                            'caption': 1,
                            'cite': 1,
                            'code': 1,
                            'col': 1,
                            'colgroup': 1,
                            'dd': 1,
                            'dfn': 1,
                            'div': 1,
                            'dl': 1,
                            'dt': 1,
                            'em': 1,
                            'h1': 1,
                            'h2': 1,
                            'h3': 1,
                            'h4': 1,
                            'h5': 1,
                            'h6': 1,
                            'h7': 1,
                            'i': 1,
                            'img': 1,
                            'kbd': 1,
                            'li': 1,
                            'link': 1,
                            'meta': 1,
                            'ol': 1,
                            'p': 1,
                            'pre': 1,
                            'q': 1,
                            'samp': 1,
                            'script': 1,
                            'span': 1,
                            'strong': 1,
                            'style': 1,
                            'sub': 1,
                            'sup': 1,
                            'table': 1,
                            'tbody': 1,
                            'td': 1,
                            'tfoot': 1,
                            'th': 1,
                            'thead': 1,
                            'tr': 1,
                            'ul': 1,
                            'u': 1,
                            'var': 1,

                            // even though they're deprecated we should leave
                            // font tags as they are, since Kupu sometimes
                            // produces them itself.
                            'font': 1,
                            'center': 0
                            };
    };
                        
    this.initialize = function(editor) {
        /* init */
        this.editor = editor;
    };

    this.filter = function(ownerdoc, htmlnode) {
        return this._filterHelper(ownerdoc, htmlnode);
    };

    this._filterHelper = function(ownerdoc, node) {
        /* filter unwanted elements */
        if (node.nodeType == 3) {
            return ownerdoc.createTextNode(node.nodeValue);
        } else if (node.nodeType == 4) {
            return ownerdoc.createCDATASection(node.nodeValue);
        };
        // create a new node to place the result into
        // XXX this can be severely optimized by doing stuff inline rather 
        // than on creating new elements all the time!
        var newnode = ownerdoc.createElement(node.nodeName);
        // copy the attributes
        for (var i=0; i < node.attributes.length; i++) {
            var attr = node.attributes[i];
            newnode.setAttribute(attr.nodeName, attr.nodeValue);
        };
        for (var i=0; i < node.childNodes.length; i++) {
            var child = node.childNodes[i];
            var nodeType = child.nodeType;
            var nodeName = child.nodeName.toLowerCase();
            if (nodeType == 3 || nodeType == 4) {
                newnode.appendChild(this._filterHelper(ownerdoc, child));
            };
            if (nodeName in this.filterdata && this.filterdata[nodeName]) {
                newnode.appendChild(this._filterHelper(ownerdoc, child));
            } else if (nodeName in this.filterdata) {
                for (var j=0; j < child.childNodes.length; j++) {
                    newnode.appendChild(this._filterHelper(ownerdoc, child.childNodes[j]));
                };
            };
        };
        return newnode;
    };
};

