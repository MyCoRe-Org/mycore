/*
 * Author: Rodney Rehm
 * Web: http://rodneyrehm.de
 * Get tabManager at http://rodneyrehm.de/tools/tabmanager
 *
 * Date / Version: 13. July 2006
 *
 * Published under the Open Source BSD-License
 * http://www.opensource.org/licenses/bsd-license.php
 */


/**
 * generic interface to create easy tabbed-panels
 * @param panelID id of div-element which will be used as tabPanel, also used as identifier. leave empty (or set null) to have a new <div> created
 * @param autoimport true if divs in panel should be importet automatically, defaults to false
 * @param tabBarBottom if true tabBar is displayed below panels, defaults to false (tabBar displayed above panels)
 **/


function tabManager( panelID, autoimport, tabBarBottom )
{
	// general panel IDs
	var firstID = null;				/* ID of first tabpanel */
	var lastID = null;				/* ID of last tabpanel */
	var nextID = null;				/* ID of next tabpanel */
	var previousID = null;			/* ID of previous tabpanel */
	var currentID = null;			/* ID of currently visible tabpanel */

	// dom-objects
	var basePanel;					/* the tabPanel-object */
	var panels = new Array();		/* contains all panel-objects */
	var tabBar;						/* the tabBar-object */
	var tabs = new Array();			/* contains all tab-objects */
	var tabBarDiv;					/* tabBar-container */


	var widthCache;					/* cache the width of a panel - regarding safari-bug */

	// object placement
	if(!tabBarBottom) tabBarBottom = false;	// just in case...

	// curent object, for use with onclick
	var myThis = this;

	/**
	 * add a new panel, creates tabs and navigation automatically
	 * @param panel DOM-object or element-id of div-element to add
	 * @param name name of the tab-link (defaults to Tab <index>)
	 * @param innerHTML html to place inside tab-link (defaults to name)
	 * @param title title of the tab-link, defaults to 'Tab <id>'
	 * @param accesskey accesskey of the tab-link, defaults to ''
	 * @param alreadyAppended set to true if div-element already resides within tabManager
	 * @returns ID of new tabpanel, or -1 on failure
	 **/
	this.addPanel = function(panel, name, innerHTML, title, accesskey, alreadyAppended)
	{
		// panel might be the id of an element, instead of the element itself
		if( typeof(panel) == 'string' )
			panel = document.getElementById(panel);

		// panel might not be usable
		if(!panel)
		{
			if(DEBUGGINGMODE) alert( 'adding\nPanel could not be added!'+
									 '\npanel is not an element' );
			return -1;
		}

		// remove panel from DOM an reassign it to tabPane
		if(!alreadyAppended)
		{
			try
			{
				// don't remember if all this try-catch stuff is
				// neccessary... but who cares?
				panel.parentNode.removeChild(panel);
				basePanel.appendChild(panel);
			}
			catch(x)
			{
				// we don't want to catch anything...
			}
		}

		// get ID of the new panel
		var newTabID = panels.length;

		// add panel-object to panels-list
		panels[newTabID] = panel;
		panels[newTabID].className = 'panel';
		panels[newTabID].style.display = 'none';

		// create link for tab
		var newLink = document.createElement('a');
		newLink.href = '#';
		newLink.myTabID = newTabID;
		newLink.onclick = function(){ myThis.switchTo( this.myTabID ); return false; };
		if(title)
			newLink.title = title;

		if(!name)
			name = 'Tab ' + newTabID;

		newLink.name = name;

		if(innerHTML)
			name = innerHTML;

		newLink.innerHTML = name;

		if(accesskey)
			newLink.accesskey = accesskey;

		// create new tab for panel
		tabs[newTabID] = document.createElement('li');
		tabs[newTabID].disabled = 0;
		tabs[newTabID].initialTabID = newTabID;
		tabs[newTabID].appendChild(newLink);
		tabBar.appendChild( tabs[newTabID] );

		// add eventhandler-dummies
		tabs[newTabID].onSwitchTo = function(){ return true; };
		tabs[newTabID].onSwitchFrom = function(){ return true; };

		// add panel to tabManager
		if(!alreadyAppended)
		{
			if(tabBarBottom)
				basePanel.insertBefore( panels[newTabID], tabBarDiv );
			else
				basePanel.appendChild( panels[newTabID] );
		}

		updateIDs();			/* get those first-last IDs right */
		this.updateTmLinks();	/* get the navigational-links within the panels right */

		// jump to first panel
		if(tabs.length == 1)
			this.switchTo(0);

		return newTabID;
	};

	/**
	 * remove a tabpanel
	 * @param tabID the tabManager ID of the tabpanel
	 * @returns removed panel
	 **/
	this.removePanel = function( tabID )
	{
		// tabpanel might not exist
		if( !panels[tabID] )
		{
			if(DEBUGGINGMODE) alert( 'removing\nTab ' + tabID + ' could not be found!'+
									 '\nThere\'s no registered panel with that ID' );
			return false;
		}

		// remove objects from DOM
		panels[tabID].parentNode.removeChild( panels[tabID] );
		tabs[tabID].parentNode.removeChild( tabs[tabID] );

		// get internal list right again
		if(tabID < lastID)
		{
			for(var i=tabID; i < lastID; i++)
			{
				panels[i] = panels[i+1];
				tabs[i] = tabs[i+1];
				tabs[i].getElementsByTagName('a')[0].myTabID = i;
			}
		}
		var removedPanel = panels.pop();	/* remove last panel from internal list */
		tabs.pop();							/* remove last tab from internal list */

		updateIDs();			/* get those first-last IDs right */
		this.updateTmLinks();	/* get the navigational-links within the panels right */

		// switch to first panel, if the panel we just removed was active
		if(currentID == tabID)
			this.switchTo(0);
		return removedPanel;
	};

	/**
	 * move a tabpanel infront of another one
	 * @param tabID the tabManager ID of the tabpanel to move
	 * @param moveBeforeTabID the tabManager ID of the tabpanel to move to
	 **/
	this.movePanel = function(tabID, moveBeforeTabID)
	{
		// just in case there are any trolls around...
		if(tabID == moveBeforeTabID) return;

		// move tab (that visual thing)
		tabBar.removeChild( tabs[tabID] );
		tabBar.insertBefore( tabs[tabID], tabs[moveBeforeTabID] );

		// we're going to move everything into a new array
		var newTabs = new Array();
		var newPanels = new Array();

		// pop the tabpanel
		var moveTab = tabs[tabID];
		tabs[tabID] = null;
		var movePanel = panels[tabID];
		panels[tabID] = null;

		for(var i=0; i < panels.length; i++)
		{
			// the previously pop-ed tabpane, skip this number
			if(panels[i] == null)
				continue;

			// the tabpanel we move in front of
			var n = newPanels.length;
			if( (moveBeforeTabID < tabID && n == moveBeforeTabID)
				|| (moveBeforeTabID > tabID && n == moveBeforeTabID-1) )
			{
				newPanels[n] = movePanel;
				newTabs[n] = moveTab;
				newTabs[n].getElementsByTagName('a')[0].myTabID = n;
				var n = newPanels.length;
			}

			// every other panel
			newPanels[n] = panels[i];
			newTabs[n] = tabs[i];
			newTabs[n].getElementsByTagName('a')[0].myTabID = n;
		}

		// 'save' the changes
		tabs = newTabs;
		panels = newPanels;

		// if we moved to the left, we need to decrement ;)
		if(tabID < moveBeforeTabID)
			moveBeforeTabID--;

		currentID = moveBeforeTabID; /* avoid onSwitchFrom-event to be executed */
		this.switchTo(moveBeforeTabID);

		updateIDs();			/* get those first-last IDs right */
		this.updateTmLinks();	/* get the navigational-links within the panels right */
	};

	/**
	 * display a tabpanel, hide all others
	 * @param tabID the tabManager ID of the tabpanel
	 **/
	this.switchTo = function( tabID )
	{
		if(!tabs.length) return;

		// strings are no integers - experienced some problems without this
		tabID = parseInt(tabID);

		// tabpanel to switch to might not exist
		if( !panels[tabID] )
		{
			if(DEBUGGINGMODE) alert( 'switching\nTab ' + tabID + ' could not be found!'+
									 '\nThere\'s no registered panel with that ID' );
			return;
		}

		// do nothin if panel is disabled
		if( this.isDisabled(tabID) ) return;

		// no need to execute switchFrom if handling the same tab
		if(currentID != tabID)
		{
			// execute onSwitchFrom handler
			// associated with the current tabpanel
			if( tabs[currentID] && typeof(tabs[currentID].onSwitchFrom) == 'function' )
				if(!tabs[currentID].onSwitchFrom()) return;
		}
		// execute onSwitchTo handler
		// associated with the next tabpanel
		if( typeof(tabs[tabID].onSwitchTo) == 'function' )
			if(!tabs[tabID].onSwitchTo()) return;

		// activate panel
		panels[tabID].style.display = '';
		tabs[tabID].className = 'active';

		if(tabID != currentID)
		{
			/* set the width of a panel - regarding safari-bug */
			if(!widthCache) widthCache = getStyle(panels[currentID], 'width');
			panels[tabID].style.width =  widthCache;

			// hide previously active panel
			if(panels[currentID]) panels[currentID].style.display = 'none';
			if(tabs[currentID]) tabs[currentID].className = '';
		}

		currentID = tabID;
		updateIDs();		/* get those first-last IDs right */
	};



	/**
	 * scan panels for navigational-links an update the onclicks
	 **/
	this.updateTmLinks = function()
	{
		for(var x = 0; x < panels.length; x++)
		{
			var paneLinks = panels[x].getElementsByTagName('a');
			for(var i=0; i < paneLinks.length; i++)
			{
				var index = paneLinks[i].href.indexOf('#');
				if(index > -1)
					var anchor = paneLinks[i].href.substr( index );

				if(!anchor || anchor.length == 1)
					continue;

				switch( anchor )
				{
					case '#tmFirst':
						paneLinks[i].myTabID = firstID;
					break;

					case '#tmLast':
						paneLinks[i].myTabID = lastID;
					break;

					case '#tmNext':
						paneLinks[i].myTabID = (x < lastID)? (x + 1): x;
					break;

					case '#tmPrevious':
						paneLinks[i].myTabID = (x > firstID)? (x - 1): x;
					break;

					default:
						if(anchor.indexOf('#tmTab') < 0)
							continue;

						// walk through tabs an check if tabID exists somewhere
						// use initialTabID, since moving tabs around can and
						// will screw up taborder of the beginning
						var tabName = anchor.substr(6);
						var tabID = parseInt( tabName );
						for(var t=0; t < tabs.length; t++)
						{
							if(!isNaN(tabName) && tabs[t].initialTabID == tabID )
							{
								// check for tabIDs
								paneLinks[i].myTabID = tabID;
								break;
							}
							else if(tabName && tabs[t].getElementsByTagName('a')[0].name == tabName)
							{
								// check for tabNames
								paneLinks[i].myTabID = tabs[t].initialTabID;
								break;
							}
						}


					break;
				}
				if( paneLinks[i].myTabID || paneLinks[i].myTabID === 0 )
				{
					paneLinks[i].onclick = function(){ myThis.switchTo( this.myTabID ); return false; };
				}
			}
		}
	}

	/**
	 * update IDs of first, last, next, previous and current
	 **/
	function updateIDs()
	{
		if(!panels.length)
		{
			firstID = null;
			lastID = null;
			nextID = null;
			previousID = null;
			currentID = null;
		}
		else
		{
			if(currentID == null) currentID = 0;
			firstID = 0;
			lastID = panels.length - 1;
			if(currentID < lastID)
				nextID = currentID + 1;
			else
				nextID = currentID;
			if(currentID > firstID)
				previousID = currentID - 1;
			else
				previousID = currentID;
		}
	}

	/**
	 * get the computed value of a style-attribute
	 * @see http://www.robertnyman.com/2006/04/24/get-the-rendered-style-of-an-element/
	 * @param elem element to get computed style from
	 * @param cssAttribute compute style for attribute
	 * @return (string) value of the selected attribute
	 **/
	function getStyle(elem, cssAttribute)
	{
		var strValue = '';
		if(document.defaultView && document.defaultView.getComputedStyle)
		{
			strValue = document.defaultView.getComputedStyle(elem, '').getPropertyValue(cssAttribute);
		}
		else if(elem.currentStyle)
		{
			try
			{
				cssAttribute = cssAttribute.replace(/\-(\w)/g, function (strMatch, p1)
				{
					return p1.toUpperCase();
				});
				strValue = elem.currentStyle[cssAttribute];
			}
			catch(e){ /* Used to prevent an error in IE 5.0 */ }
		}
		return strValue;
	}

	/**
	 * initialize tabManager,
	 * make tabPanel out of div-element identified by panelID,
	 * import nested div-elements
	 **/
	this.init = function()
	{
		// get panel-object
		if( typeof(panelID) == 'string' )
			basePanel = document.getElementById( panelID );
		else
		{
			basePanel = document.createElement('div');
			autoimport = false;
		}

		if(!basePanel)
		{
			if(DEBUGGINGMODE) alert( 'tabPanel could not be found!'+
									 '\nThere\'s no Div-Element with the ID ' + panelID );
			return;
		}

		// create tabbar-object
		tabBar = document.createElement('ul');
		tabBar.className = 'tabBar';

		if(autoimport)
		{
			// scan basePanel for divs and add the found (first-level) panels
			// deeper divs have to be left alone!
			var foundDIVs = basePanel.getElementsByTagName('div');
			for(var i=0; i < foundDIVs.length; i++)
			{
				if( foundDIVs[i].parentNode == basePanel )

					var tabname = false;
					var tabinnerHTML = false;
					var tabtitle = false;
					var tabaccesskey = false;

					var dls = foundDIVs[i].getElementsByTagName('dl');
					for(var dli=0; dli < dls.length; dli++)
					{
						if(dls[dli].title == 'tabinfo')
						{
							var dds = foundDIVs[i].getElementsByTagName('dd');
							for(var ddi=0; ddi < dds.length; ddi++)
							{
								switch( dds[ddi].title )
								{
									case 'name':
										tabname = dds[ddi].innerHTML;
									break;

									case 'innerHTML':
										tabinnerHTML = dds[ddi].innerHTML;
									break;

									case 'title':
										tabtitle = dds[ddi].innerHTML;
									break;

									case 'accesskey':
										tabaccesskey = dds[ddi].innerHTML;
									break;
								}
							}
							dls[dli].parentNode.removeChild(dls[dli]);
							break;
						}
					}


					this.addPanel(  foundDIVs[i],
									tabname,
									tabinnerHTML,
									tabtitle,
									tabaccesskey,
									true );
			}
		}

		// tabbar needs to reside inside a div (style-thing)
		tabBarDiv = document.createElement('div');
		tabBarDiv.className = 'tabBar';
		tabBarDiv.appendChild( tabBar );

		// add tabBar as first child
		if( basePanel.hasChildNodes() && !tabBarBottom )
			basePanel.insertBefore( tabBarDiv, basePanel.firstChild );
		else
			basePanel.appendChild( tabBarDiv );


		// check if one of the importet tabs is called
		var index = window.location.href.indexOf('#');
		if(index > -1)
		{
			var anchor = window.location.href.substr( index );
			if( anchor.length > 1 )
			{
				currentID = this.getTabIDbyElementID( anchor.substr(1) );
				if(currentID == -1) currentID = 0;
			}
		}

		// show current tab
		if( tabs.length > 0 && currentID != null)
			this.switchTo(currentID);
	};

	/**
	 * get tabManager ID of the first tabpanel
	 * @return tabID
	 */
	this.getFirstID = function()
	{
		return firstID;
	};

	/**
	 * get tabManager ID of the last tabpanel
	 * @return tabID
	 */
	this.getLastID = function()
	{
		return lastID;
	};

	/**
	 * get tabManager ID of the next tabpanel
	 * @return tabID
	 */
	this.getNextID = function()
	{
		return nextID;
	};

	/**
	 * get tabManager ID of the previous tabpanel
	 * @return tabID
	 */
	this.getPreviousID = function()
	{
		return previousID;
	};

	/**
	 * get tabManager ID of the current tabpanel
	 * @return tabID
	 */
	this.getCurrentID = function()
	{
		return currentID;
	};

	/**
	 * disable a tabpanel
	 * @param tabID (initial) tabManager ID of tabpanel to disable
	 */
	this.disablePanel = function( tabID )
	{
		for(var t=0; t < tabs.length; t++)
		{
			if(tabs[t].initialTabID == tabID )
			{
				tabs[t].disabled = 1;
				tabs[t].className = 'disabled';
				break;
			}
		}
	};

	/**
	 * enable a tabpanel
	 * @param tabID (initial) tabManager ID of tabpanel to enable
	 */
	this.enablePanel = function( tabID )
	{
		for(var t=0; t < tabs.length; t++)
		{
			if(tabs[t].initialTabID == tabID )
			{
				tabs[t].disabled = 0;
				tabs[t].className = '';
				break;
			}
		}
	};

	/**
	 * check if a tabpanel is disabled
	 * @param tabID (current) tabManager ID of tabpanel to check
	 * @returns boolean status
	 */
	this.isDisabled = function( tabID )
	{
		return (tabs[tabID].disabled == 1)? true : false;
	};

	/**
	 * add onSwitchFrom eventHandler to a tabpanel,
	 * eventHandler has to return true, if switchTo should continue,
	 * or false if switchTo should cancel
	 * @param tabID tabManager ID of tabpanel to add eventHandler to
	 * @param eventHandler function to execute on switching from tabpanel
	 */
	this.addOnSwitchFrom = function( tabID, eventHandler )
	{
		tabs[tabID].onSwitchFrom = eventHandler ;
	};

	/**
	 * add onSwitchTo eventHandler to a tabpanel,
	 * eventHandler has to return true, if switchTo should continue,
	 * or false if switchTo should cancel
	 * @param tabID tabManager ID of tabpanel to add eventHandler to
	 * @param eventHandler function to execute on switching to another tabpanel
	 */
	this.addOnSwitchTo = function( tabID, eventHandler )
	{
		tabs[tabID].onSwitchTo = eventHandler;
	};

	/**
	 * set the name of a tab
	 * @param tabID tabManager ID of tabpanel
	 * @param name new name to set (may be html)
	 */
	this.setTabName = function( tabID, name )
	{
		tabs[tabID].getElementsByTagName('a')[0].name = name;
	};

	/**
	 * set the innerHTML of a tab(-link)
	 * @param tabID tabManager ID of tabpanel
	 * @param innerHTML html to place inside <a>
	 */
	this.setTabLinkInnerHTML = function( tabID, innerHTML )
	{
		tabs[tabID].getElementsByTagName('a')[0].innerHTML = innerHTML;
	};

	/**
	 * set the title of a tab(-link)
	 * @param tabID tabManager ID of tabpanel
	 * @param title new title of link (only text)
	 */
	this.setTabLinkTitle = function( tabID, title )
	{
		tabs[tabID].getElementsByTagName('a')[0].title = title;
	};

	/**
	 * set the accesskey of a tab(-link)
	 * @param tabID tabManager ID of tabpanel
	 * @param accesskey new access of link (one letter)
	 */
	this.setTabLinkAccesskey = function( tabID, accesskey )
	{
		tabs[tabID].getElementsByTagName('a')[0].accesskey = accesskey;
	};

	/**
	 * get a list of all registered tabs
	 * @returns array containing tabnames
	 */
	this.getTabNames = function()
	{
		var tmp = new Array();
		for(var i=0; i<tabs.length; i++)
		{
			tmp[tmp.length] = tabs[i].getElementsByTagName('a')[0].name;
		}
		return tmp;
	};

	/**
	 * find tabID to given element-ID
	 * @param elemID DOM-element-id of tabpanel
	 * @return tabID
	 */
	this.getTabIDbyElementID = function( elemID )
	{
		for(var i=0; i < panels.length; i++)
		{
			if(panels[i].id == elemID) return tabs[i].getElementsByTagName('a')[0].myTabID;
		}
		return -1;
	};

	/**
	 * get DOM-object of tab-element
	 * @param tabID tabManager ID of tabpanel
	 * @return DOM-object, or null on error
	 */
	this.getTabElement = function( tabID )
	{
		if( !tabs[tabID] )
			return null;

		return tabs[tabID];
	};

	/**
	 * get DOM-object of panel-element
	 * @param tabID tabManager ID of tabpanel
	 * @return DOM-object, or null on error
	 */
	this.getPanelElement = function( tabID )
	{
		if( !panels[tabID] )
			return null;

		return panels[tabID];
	};

	/**
	 * get DOM-object of basePanel-element
	 * @return DOM-object, or null on error
	 */
	this.getBasePanelElement = function( )
	{
		if( !basePanel )
			return null;

		return basePanel;
	};

	/**
	 * set className of basePanel-element
	 * @param className class to set for basePanel
	 */
	this.setBasePanelClassName = function( className )
	{
		if( !basePanel )
			return;

		basePanel.className = className;
	};

	// ok, start that thing!
	this.init();
}
