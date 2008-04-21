// Load Dojo's code relating to the Button widget
dojo.require("dojo.parser");
dojo.require("dijit.layout.LayoutContainer");
dojo.require("dijit.layout.TabContainer");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit.form.Button");
dojo.require("dijit.Menu");
dojo.require("dijit.Toolbar");
dojo.require("dijit.form.CheckBox");
dojo.require("dijit.form.Slider");
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.Textarea");
dojo.require("dijit.Dialog");
dojo.require("dojox.string.sprintf");
if (webCLIServlet==null){
	var webCLIServlet='../../servlets/MCRWebCLIServlet';
	console.debug("webCLIServlet is not set trying: "+webCLIServlet);
}
var test;
var webcli={
	"failedData"	:	null,
	"failedIoArgs"	:	null,
	"doRefresh"		:	true,
	"debug"			:	true,
	"logDebug"		:	function(/*String*/ str){
		if (webcli.debug==true){
			console.debug(str);
		}
	},
	"servletError"	:	function(data, ioArgs) {
		webcli.failedData=data;
		webcli.failedIoArgs=ioArgs;
		webcli.stopRefresh();
		title = ioArgs.xhr.status+": "+ioArgs.xhr.statusText;
		dialogText = ioArgs.xhr.responseText;
		if (ioArgs.xhr.status==0){
			title = "Server is not responding";
			dialogText = "Received bad return code '0' which mostly means that the server is down."
		}
		var errorDialog= new dijit.Dialog({
			title: title
		});
		errorDialog.setContent(dialogText);
		errorDialog.layout();
		errorDialog.show();
	},
	"refreshPos"	:   4,
	"playButton"	:	null,
	"pauseButton"	:	null,
	"toggleRefresh"	:	function(){
		webcli.logDebug("toggle...");
		if (webcli.doRefresh==true){
			webcli.stopRefresh();
		} else {
			webcli.startRefresh();
		}
	},
	"stopRefresh"	:	function(){
		webcli.logDebug("toggle off");
		webcli.doRefresh=false;
		toolbar=dijit.byId("toolbar");
		dojo.forEach(toolbar.getChildren(), function(widget){
			if (widget==webcli.pauseButton){
					webcli.logDebug("remove pausebutton");
					toolbar.removeChild(webcli.pauseButton);
					webcli.logDebug("add playbutton");
					toolbar.addChild(webcli.playButton, webcli.refreshPos);
			}
		});
		window.clearInterval(webcli.commands.refreshHandler);
		window.clearInterval(webcli.logs.refreshHandler);
	},
	"startRefresh"	:	function(){
		webcli.logDebug("toggle on");
		webcli.doRefresh=true;
		var toolbar=dijit.registry.byId("toolbar");
		dojo.forEach(toolbar.getChildren(), function(widget){
			if (widget==webcli.playButton){
				webcli.logDebug("remove playbutton");
				toolbar.removeChild(webcli.playButton);
				webcli.logDebug("add pausebutton");
				toolbar.addChild(webcli.pauseButton, webcli.refreshPos);
			}
		});
		webcli.commands.refreshHandler = window.setInterval("webcli.commands.refreshQueue()",  webcli.commands.refreshRate);
		webcli.logs.refreshHandler = window.setInterval("webcli.logs.refresh()", webcli.logs.refreshRate);
	},
	"commands"	:	{
		"refreshRate"					:	5000,
		"knownCommands"					:	null,
		"refreshReady"					:	true,
		"_callbackKnownCommands"		:	function(data, ioArgs) {
			webcli.commands.knownCommands= dojo.fromJson(data).commands;
			webcli.gui.createCommandsMenu();
		},
		"selectCommand"					:	function(/*String*/ evt) {
			dijit.byId("command").setValue(evt.target.textContent);
		},
		"_callbackRun"					:	function(data, ioArgs) {
			webcli.logDebug("command appended");
		},
		"executeCommand"				:	function(/*String*/ cmd) {
			webcli.logDebug("Will execute:\n"+cmd);
			dojo.xhrGet({
				url: webCLIServlet,
				load: webcli.commands._callbackRun,
				error: webcli.servletError,
				content: {run: cmd}
			});
		},
		"captureCommand"				:	function(/*Event*/ evt) {
			if (evt.keyCode == 13){
				webcli.commands.executeCommand(dojo.byId("command").value);
			}
		},
		"commandQueue"					:	null,
		"_callbackCommandQueue"			:	function(data, ioArgs) {
			commandQueue=	dojo.fromJson(data).commandQueue;
			if ((webcli.commands.commandQueue!=null) && 
				(commandQueue.length==webcli.commands.commandQueue.length)){
				if ((commandQueue.length==0) || 
					((commandQueue.length>0) && (commandQueue[0]==webcli.commands.commandQueue[0]))){
					webcli.logDebug("_callbackCommandQueue: nothing todo current queue is the same as server reported queue");
				}
			}
			webcli.commands.commandQueue=dojo.fromJson(data).commandQueue;
			var list = document.createElement("ol");
			dojo.forEach(webcli.commands.commandQueue, function(command){
				var newLink=document.createElement("li");
				newLink.appendChild(document.createTextNode(command));
				list.appendChild(newLink);
			});
			var cEl=dijit.byId("commandQueue");
			cEl.setContent(list);
			webcli.commands.refreshReady=true;
		},
		"refreshHandler"			: null,
		"refreshQueue"				: function(){
			if (webcli.doRefresh==true){
				if (webcli.commands.refreshReady==true){
					webcli.commands.refreshReady=false;
					dojo.xhrGet({
						url: webCLIServlet,
						load: webcli.commands._callbackCommandQueue,
						error: webcli.servletError,
						content: {request: "getCommandQueue"}
					});
				}
			} else {
				//some error occured on previous server request, stop automatic refreshing
				webcli.logDebug("refreshQueue: 'webcli.doRefresh!=true' abort automatic refresh");
				window.clearInterval(webcli.commands.refreshHandler);
			}
		},
		"getRefreshRate"	:	function(){
			return webcli.commands.refreshRate;
		},
		"setRefreshRate"	:	function(/*int*/ rate){
			if (webcli.commands.refreshRate!=rate){
				webcli.logDebug("setting command queue refresh rate: "+rate);
				webcli.commands.refreshRate=rate;
				window.clearInterval(webcli.commands.refreshHandler);
				webcli.commands.refreshHandler=window.setInterval("webcli.commands.refreshQueue()", rate);
			}
		}
	},
	"gui"		:	{
		"createCommandsMenu"	:	function(){
			var menu = dijit.byId("commandMenu");
			dojo.forEach(webcli.commands.knownCommands, function(commandGroup){
				var submenu= new dijit.Menu({});
				var popup=new dijit.PopupMenuItem({
					label: commandGroup.name,
					popup: submenu
				});
				menu.addChild(popup);
				dojo.forEach(commandGroup.commands,function(command){
					var subMenuItem=new dijit.MenuItem({
						label: command,
						onClick: webcli.commands.selectCommand
					});
					submenu.addChild(subMenuItem);
				});
			});
			var params= {
				label: "Command",
				dropDown: menu
			};
			menu.startup();
		},
		"processSettings"		:	function(dialogFields){
			webcli.logs.setRefreshRate(dialogFields.logRefreshSetting);
			webcli.commands.setRefreshRate(dialogFields.queueRefreshSetting);
		},
		"handleSlider"			:	function(/*String*/ id, value){
			element = dojo.byId(id);
			element.value = value;
			label = dojo.byId(id+".label").getElementsByTagName("div")[0].getElementsByTagName("span")[0];
			label.firstChild.nodeValue = document.createTextNode(dojox.string.sprintf("%.2f\u00A0s",(value/1000))).nodeValue;
		},
		"showSettings"			:	function(){
			dijit.byId('settingsDialog').show();
			dijit.byId('logRefreshSetting').setValue(webcli.logs.getRefreshRate(), true);
			dijit.byId('queueRefreshSetting').setValue(webcli.commands.getRefreshRate(), true);
		}
	},
	"logs"	:	{
		"refreshRate"		:	1000,
		"autoScroll"		:	true,
		"refreshReady"		:	true,
		"_callback"			:	function(data, ioArgs) {
			logs= dojo.fromJson(data).logs;
			dojo.forEach(logs, function(line){
				webcli.logs.writeln(line);
			});
			webcli.logs.refreshReady=true;
		},
		"toggleAutoScroll"	:	function(){
			if (webcli.logs.autoScroll==true){
				webcli.logDebug("Switching autoscroll off");
				webcli.logs.autoScroll=false;
			} else {
				webcli.logDebug("Switching autoscroll on");
				webcli.logs.autoScroll=true;
			}
		},
		"getRefreshRate"	:	function(){
			return webcli.logs.refreshRate;
		},
		"setRefreshRate"	:	function(/*int*/ rate){
			if (webcli.logs.refreshRate!=rate){
				webcli.logDebug("setting command queue refresh rate: "+rate);
				webcli.logs.refreshRate=rate;
				window.clearInterval(webcli.logs.refreshHandler);
				webcli.logs.refreshHandler=window.setInterval("webcli.logs.refresh()", rate);
			}
		},
		"writeln"			:	function(/*String*/ str){
			webcli.logDebug(str);
			test=dojo.byId("logs");
			text=document.createTextNode(str.logLevel+": "+str.message+"\n");
			testm=dojo.byId("logs").getElementsByTagName("pre")[0]
			if (testm.firstChild==null){
				//<pre> is empty
				testm.appendChild(text);
			} else {
				testm.firstChild.appendData(text.nodeValue);
			}
			if (webcli.logs.autoScroll==true){
				testm.scrollTop = testm.scrollHeight - testm.offsetHeight + 100;
			}
		},
		"clear"			:	function(){
			dojo.byId("logs").getElementsByTagName("pre")[0].firstChild.nodeValue="";
		},
		"refreshHandler"	:	null,
		"refresh"			:	function(){
			if (webcli.doRefresh==true){
				if (webcli.logs.refreshReady==true){
					webcli.logs.refreshReady=false;
					dojo.xhrGet({
						url: webCLIServlet,
						load: webcli.logs._callback,
						error: webcli.servletError,
						content: {request: "getLogs"}
					});
				}
			} else {
				//some error occured on previous server request, stop automatic refreshing
				webcli.logDebug("logs.refresh: 'webcli.doRefresh!=true' abort automatic refresh");
				window.clearInterval(webcli.logs.refreshHandler);
			}
		}
		
	}
};

dojo.addOnLoad(function(){ 
	dojo.xhrGet({
		url: webCLIServlet,
		load: webcli.commands._callbackKnownCommands,
		error: webcli.servletError,
		content: {request: "getKnownCommands"}
	});
	webcli.logDebug("adding playButton");
	webcli.playButton = new dijit.form.Button({
		label: "Refresh",
		onClick: webcli.toggleRefresh,
		iconClass: "webcliEditorIcon webcliEditorIconPlay"
	});
	webcli.logDebug("adding pauseButton");
	webcli.pauseButton = new dijit.form.Button({
		label: "Stop Refresh",
		onClick: webcli.toggleRefresh,
		iconClass: "webcliEditorIcon webcliEditorIconPause"
	});
	webcli.logDebug("adding pauseButton to toolbar");
	dijit.byId("toolbar").addChild(webcli.pauseButton, webcli.refreshPos);
	webcli.startRefresh();
});

