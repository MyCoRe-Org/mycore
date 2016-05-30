var WCMS2FileBrowser = function(){
	var currentPath = "";
	var type = "files";
	var baseHref = "";
	var qpara = [], hash;
	var i18nKeys =[];

	
	return {
		init: function() {
			$("body").on("click", ".button-expand", function() {
				expandFolder($(this).closest(".folder"));
			});
			
			$("body").on("click", ".button-contract", function() {
				$(this).siblings("ul.children").addClass("hide-folder");
				$(this).siblings(".icon").removeClass("glyphicon-folder-open");
				$(this).siblings(".icon").addClass("glyphicon-folder-close");
				$(this).removeClass("button-contract glyphicon-minus");
				$(this).addClass("button-expand glyphicon-plus");
				
			});
			
			$("body").on("click", ".folder-name, .folder > span.icon", function() {
				if(!$(this).parent().hasClass("edit")){
					changeFolder($(this).parent().data("path"));
				}
			});
			
			$("body").on("click", ".file-image-cotainer", function(e) {
				var funcNum = qpara["CKEditorFuncNum"];
				if (funcNum == undefined){
					funcNum = 1;
				}
				if (type == "images") {
					window.opener.CKEDITOR.tools.callFunction(funcNum, getRelativePath($(this).data("path")) + $(this).siblings("h5.file-title").html());					
				}
				else{
					window.opener.CKEDITOR.tools.callFunction(funcNum, baseHref + getRelativePath($(this).data("path")) + $(this).siblings("h5.file-title").html());
				}
				window.close();
			});
			
			$("body").on("drop", "#file-view", function(event) {
				event.preventDefault();
				event.stopPropagation();
				if($(".aktiv").data("allowed")){
					var files = event.originalEvent.dataTransfer.files;
					$.each(files, function(i, file) {
						if (file.type != "" && file.type != "text/xml"){
						    var data = new FormData();
						    data.append('path', currentPath);
						    data.append('file', file);
							uploadFile(data, createStatusBar($("#uploadbar"), file));
						}
						else{
							alert(geti18n("component.wcms.navigation.fileBrowser.invalidFileType"))
						}
					});
				}
				else{
					alert(geti18n("component.wcms.navigation.fileBrowser.invalidUploadFolder"));
				}
			});
			
			$("body").on("mouseenter", ".file", function() {
				$(this).find(".wcms2-image-button").removeClass("hidden");
			});
			
			$("body").on("mouseleave", ".file", function() {
				$(this).find(".wcms2-image-button").addClass("hidden");
			});
			
			$("body").on("click", ".button-remove", function() {
				var parent = $(this).parent();
				if( confirm(geti18n("component.wcms.navigation.fileBrowser.confirmDeleteFile", parent.find("h5").html()))){
					deleteFile(currentPath + "/" + parent.find(".file-title").html());
				}
			});
			
			$("body").on("click", "#add-folder", function() {
				if($(".aktiv").data("allowed")){
					expandFolder($(".aktiv"));
					if(!$(".aktiv").children("ul.children").length){
						$(".aktiv").append("<ul class='children'></ul>");
						$(".aktiv").children(".no-button").remove();
						$(".aktiv").prepend("<span class='glyphicon glyphicon-minus button button-contract'></span>");
						$(".aktiv").children(".icon").addClass("glyphicon-folder-open");
						$(".aktiv").children(".icon").removeClass("glyphicon-folder-close");
					}
					var li = $("<li class='folder edit'><div class='folder-name'><input class='add-folder-input' type='text'></div></li>");
					$(li).prepend("<span class='glyphicon glyphicon-folder-close icon'></span>");
					$(li).prepend("<div class='no-button'>&nbsp;</div>");
					$(li).data("allowed", true);
					$(".aktiv").children("ul.children").append(li);
					$(li).find("input.add-folder-input").focus();
				}
				else{
					alert(geti18n("component.wcms.navigation.fileBrowser.invalidCreateFolder"))
				}
			});
			
			$("body").on("keydown", ".add-folder-input", function(key) {
				if(key.which == 13 && $(this).val() != "") {
					addFolder(currentPath + "/" + $(this).val(), $(this).parents(".folder")[0]);
				}
				if(key.which == 27) {
					removeFolder($(this).closest(".folder"));
				}
			});
			
			$("body").on("click", "#delete-folder", function() {
				if($(".aktiv").data("allowed")){
					if( confirm(geti18n("component.wcms.navigation.fileBrowser.confirmDeleteFolder", $(".aktiv").find(".folder-name").html()))){
						deleteFolder(currentPath, $(".aktiv"));
					}
				}
				else{
					alert(geti18n("component.wcms.navigation.fileBrowser.invalidDeleteFolder"));
				}
			});
			
			$("body").on("click", "#hide-drag-drop-info", function() {
				$("#drag-and-drop-info").hide();
			});
			
			$("body").on("click", ".wcms2-lightbox", function(evt) {
			    evt.preventDefault();
			    showLightbox($(this));
			});
			
			$("body").on("click", "#wcms2-lightbox-close", function() {
			    hideLightbox();
			});
						
			readQueryParameter();
			currentPath = qpara["href"] != undefined ? qpara["href"] : "";
			type = qpara["type"] != undefined ? qpara["type"] : "files";
			baseHref = qpara["basehref"] != undefined ? qpara["basehref"] : "";
			jQuery.getJSON("../../servlets/MCRLocaleServlet/" + qpara["langCode"] + "/component.wcms.navigation.fileBrowser.*", function(data) { 
				i18nKeys = data;
				$("#folder-label").html(geti18n("component.wcms.navigation.fileBrowser.folder"));
				$("#drag-and-drop-info").append(geti18n("component.wcms.navigation.fileBrowser.dragDropInfo"));
				$("#add-folder").append(geti18n("component.wcms.navigation.fileBrowser.addFolder"));
				$("#delete-folder").append(geti18n("component.wcms.navigation.fileBrowser.deleteFolder"));
				getFolders();
			});
		}
	}
		
	function showLightbox(elm) {
	    loadImage($(elm).attr("data-url"));
	}
	
	function loadImage(imgSrc) {
	    var image = new Image();
	    image.onload = function() {
	        $("#wcms2-lightbox-img").remove();
	        $(image).attr("id", "wcms2-lightbox-img");
	        $("#wcms2-lightbox-content").prepend(image);
	        $("#wcms2-lightbox").css("display", "block");
	        $("html").css("overflow", "hidden");
	    }
	    image.src = imgSrc;
	}
	
	function hideLightbox() {
	    $("#wcms2-lightbox").css("display", "none");
	    $("html").css("overflow", "auto");
	}
	  
	function getFolders() {
		$.ajax({
			url: "filebrowser/folder",
			type: "GET",
			dataType: "json",
			success: function(data) {
						createFolder($("#folder-list-ul"), data.folders, "start");
						$("#folder-list-ul > li").addClass("aktiv");
						changeFolder(currentPath);
					},
			error: function(error) {
						alert(error);
					}
		});
	}	
	
	function getFiles(path) {
		$.ajax({
			url: "filebrowser/files?path=" + path + "&type=" + type,
			type: "GET",
			dataType: "json",
			success: function(data) {
						createFiles($("#files"), data.files, path)
					},
			error: function(error) {
						alert(error);
					}
		});
	}
	
	function uploadFile(file, statusbar){
		$.ajax({
			url: "filebrowser",
			type: "POST",
			processData: false, 
			contentType: false,
			data: file,
			xhr: function() {
				var xhr = new window.XMLHttpRequest();
				xhr.upload.addEventListener("progress", function(evt) {
					if (evt.lengthComputable){
						var percentComplete = evt.loaded / evt.total;
						$(statusbar).find(".statusbar-progress-status").width(percentComplete + '%');
			            $(statusbar).find(".statusbar-progress-status").html(percentComplete + '%');
					}
				}, false);
				return xhr;
			},
			success: function(data) {
						$(statusbar).find(".statusbar-progress-status").width('100%');
						$(statusbar).find(".statusbar-progress-status").html('100%');
						$(statusbar).fadeOut(3000, function() {
							$(statusbar).remove();
						});
						getFiles(currentPath);
					},
			error: function(error) {
						alert(error);
					}
		});
	}
	
	function deleteFile(path) {
		$.ajax({
			url: "filebrowser?path=" + path,
			type: "DELETE",
			success: function(data) {
						getFiles(currentPath);
					},
			error: function(error) {
						alert(error);
					}
		});
	}
	
	function addFolder(path, node){
		$.ajax({
			url: "filebrowser/folder?path=" + path,
			type: "POST",
			success: function(data) {
						var name = $(node).find("input").val();
						$(node).find("div.folder-name").html(name);
						var parent = $(node).parents(".folder")[0];
						$(node).data("path", $(parent).data("path") + "/" + name);
						$("li.edit").removeClass("edit");
					},
			error: function(error) {
						alert(error);
					}
		});
	}
	
	function deleteFolder(path, node){
		$.ajax({
			url: "filebrowser/folder?path=" + path,
			type: "DELETE",
			statusCode: {
				200: function() {
					removeFolder(node);
				},
				409: function() {
					alert(geti18n("component.wcms.navigation.fileBrowser.errorDelete"));
				},
				403: function() {
					alert(geti18n("component.wcms.navigation.fileBrowser.folderNotEmpty"));
				},
				500: function(error) {
					alert(error);
				}
			}
		});
	}
		
	function createFolder(parent, data, parentPath) {
		var li = $("<li class='folder'><div class='folder-name'>" + data.name + "</div></li>");
		if (data.type == "folder"){
			$(li).prepend("<span class='glyphicon glyphicon-folder-close icon'></span>");
		}
		if (parentPath == "start"){
			$(li).data("path", "");
		}
		else{
			$(li).data("path", parentPath + "/"  +  data.name);
		}
		if (data.allowed == false){
			$(li).find("span.icon").addClass("faded");
			$(li).data("allowed", false);
		}
		if (data.allowed == true){
			$(li).data("allowed", true);
		}
		if(data.children != undefined && data.children.length > 0 ){
			$(li).prepend("<span class='glyphicon glyphicon-plus button button-expand'></span>");
			var ul = $("<ul class='hide-folder children'></ul>");
			$(li).append(ul);
			$.each(data.children, function(i, node) {
				if (node.type == "folder"){
					createFolder(ul, node, $(li).data("path"));
				}
			});
		}
		else{
			$(li).prepend("<div class='no-button'>&nbsp;</div>");
		}
		$(parent).append(li);
	}
	
	function createStatusBar(parent, file){
		$("#drag-and-drop-info").hide();
		var status = $("<div class='statusbar'><div class='statusbar-image-cotainer'><img class='statusbar-image'></img></div><div class='statusbar-progress-container'><h5 class='statusbar-title'></h5><div class='statusbar-progress'><div class='statusbar-progress-status'></div></div></div></div>");
		if (file.type.match(/image.*/)){
			readImg(file, $(status).find("img.statusbar-image"));
		}
		$(status).find("h5.statusbar-title").html(file.name);
		$(status).find("h5.statusbar-title").attr("title",file.name);
		$(parent).append(status);
		return status;
	};
	
	function readImg(file, display) {
		var reader = new FileReader();
		reader.onload =  function(e) {
			display.attr("src", reader.result);
		}
		reader.readAsDataURL(file);
	}
	
	function createFiles(parent, data, path) {
		$(parent).html("");
		var fileTemp = $("<div class='file'><div class='file-image-cotainer'></div><h5 class='file-title'></h5></div>");
		var noFiles = $("<div id='noFiles'>" + geti18n("component.wcms.navigation.fileBrowser.noFiles") + "</div>");
		if(data.length == 0 && $(".aktiv").data("allowed")){
			$(parent).html(noFiles);
		}
		$.each(data, function(i, node) {
			if(node.type != "folder"){
				var file = fileTemp.clone();
				if(node.type == "image"){
					$(file).find("div.file-image-cotainer").append("<img class='file-image'></img>");
					$(file).find("img.file-image").attr("src",node.path);
					$(file).find("div.file-image-cotainer").data("path", path);
					$(file).append("<span class='glyphicon glyphicon-remove button button-remove wcms2-image-button hidden'></span>");
					$(file).append("<span class='glyphicon glyphicon-fullscreen wcms2-lightbox wcms2-image-button hidden' aria-hidden='true'></span>");
					$(file).find("span.wcms2-lightbox").attr("data-url", node.path);
				}
				else{
					$(file).find("div.file-image-cotainer").append("<span class='glyphicon glyphicon-file file-file'></span>");
					$(file).find("div.file-image-cotainer").data("path", path);
				}
				$(file).find("h5.file-title").html(node.name);
				$(file).find("h5.file-title").attr("title",node.name);
				$(parent).append(file);
			}
		});
	}
	
	function removeFolder(node) {
		var parent = node.parents(".folder")[0];
		node.remove();
		if($(parent).children("ul.children").length && $(parent).children("ul.children").html() == ""){
			$(parent).children("ul.children").remove();
			$(parent).children(".button").remove();
			$(parent).prepend("<div class='no-button'>&nbsp;</div>");
			$(parent).children(".icon").removeClass("glyphicon-folder-open");
			$(parent).children(".icon").addClass("glyphicon-folder-close");
		}
	}
		
	function expandFolder(node) {
		var button = $(node).children(".button");
		if(button.hasClass("button-expand")){
			button.siblings("ul.children").removeClass("hide-folder");
			button.siblings(".icon").removeClass("glyphicon-folder-close");
			button.siblings(".icon").addClass("glyphicon-folder-open");
			button.removeClass("button-expand glyphicon-plus");
			button.addClass("button-contract glyphicon-minus");
		}
	}
	
	function readQueryParameter() {
		var q = document.URL.split(/\?(.+)?/)[1];
		if(q != undefined){
	        q = q.split('&');
	        for(var i = 0; i < q.length; i++){
	            hash = q[i].split(/=(.+)?/);
	            qpara.push(hash[1]);
	            qpara[hash[0]] = hash[1];
	        }
		}
	}
	
	function changeFolder(folderPath) {
		var folder = $("li").filter(function() {
			return $(this).data("path") == folderPath;
		});
		$(folder).parents(".folder").each(function() {
			expandFolder($(this));
		});
		getFiles(folder.data("path"));
		currentPath = folder.data("path");
		$(".aktiv").removeClass("aktiv");
		folder.addClass("aktiv");
	}
	
	function geti18n(key) {
		var string = i18nKeys[key];
		if (string != undefined){
			for (i = 0; i < arguments.length-1; i++){
				string = string.replace(new RegExp('\\{' + i + '\\}', "g"), arguments[i+1]);
			}
			return string;
		}
		else{
			return "";
		}
	}
	
	function getRelativePath(path) {
		var pathArray = path.split("/"); 
		var basePathArray = qpara["href"].split("/");
		var relativePath = "";
		var i = -1;
		while((basePathArray.length + i) > 0 && pathArray.indexOf(basePathArray[basePathArray.length+i]) == -1){
			console.log(basePathArray[basePathArray.length+i]);
			relativePath = relativePath + "../";
			i--;
		}
		i = pathArray.indexOf(basePathArray[basePathArray.length+i]) + 1;
		while(i < pathArray.length){
			relativePath = relativePath + pathArray[i] + "/";
			i++;
		}
		return relativePath;
	}
}


$(document).ready(function() {
	var wcms2FileBrowserInstance = new WCMS2FileBrowser();
	wcms2FileBrowserInstance.init();
});