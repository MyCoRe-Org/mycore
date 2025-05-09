/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

var WCMS2FileBrowser = function() {
  let currentPath = "";
  let type = "files";
  let baseHref = "";
  let qpara = [], hash;
  let i18nKeys = [];
  let uploadProgress = [];
  const dropArea = document.getElementById("drop-area");
  const progressBar = document.getElementById("upload-progress-bar");

  function showLightbox(elm) {
    loadImage($(elm).attr("data-url"));
  }

  function loadImage(imgSrc) {
    const image = new Image();
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

  function updateFileView(path) {
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

  function uploadFile(file, i) {
    const url = 'filebrowser'
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    xhr.open('POST', url, true);

    xhr.upload.addEventListener("progress", function(e) {
      updateProgress(i, (e.loaded * 100.0 / e.total) || 100);
    });
    xhr.addEventListener('readystatechange', function(e) {
      if (xhr.readyState === 4 && xhr.status === 200) {
        updateFileView(currentPath);
      } else if (xhr.readyState === 4 && xhr.status !== 200) {
        alert(file + " couldn't be uploaded.");
      }
    });
    formData.append('file', file);
    formData.append('path', currentPath);
    xhr.send(formData);
  }

  function initializeProgress(numFiles) {
    progressBar.value = 0;
    progressBar.style.opacity = "1";
    uploadProgress = [];
    for (let i = numFiles; i > 0; i--) {
      uploadProgress.push(0)
    }
  }

  function updateProgress(fileNumber, percent) {
    uploadProgress[fileNumber] = percent;
    progressBar.value = uploadProgress.reduce((tot, curr) => tot + curr, 0) / uploadProgress.length;
    if (progressBar.value >= 100) {
      progressBar.style.opacity = "0";
    }
  }

  function deleteFile(path) {
    $.ajax({
      url: "filebrowser?path=" + path,
      type: "DELETE",
      success: function(data) {
        updateFileView(currentPath);
      },
      error: function(error) {
        alert(error);
      }
    });
  }

  function addFolder(path, node) {
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

  function deleteFolder(path, node) {
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
    const li = $("<li class='folder'><div class='folder-name'>" + data.name + "</div></li>");
    if (data.type == "folder") {
      $(li).prepend("<span class='glyphicon glyphicon-folder-close icon'></span>");
    }
    if (parentPath == "start") {
      $(li).data("path", "");
    } else {
      $(li).data("path", parentPath + "/" + data.name);
    }
    if (data.allowed == false) {
      $(li).find("span.icon").addClass("faded");
      $(li).data("allowed", false);
    }
    if (data.allowed == true) {
      $(li).data("allowed", true);
    }
    if (data.children != undefined && data.children.length > 0) {
      $(li).prepend("<span class='glyphicon glyphicon-plus button button-expand'></span>");
      const ul = $("<ul class='hide-folder children'></ul>");
      $(li).append(ul);
      $.each(data.children, function(i, node) {
        if (node.type == "folder") {
          createFolder(ul, node, $(li).data("path"));
        }
      });
    } else {
      $(li).prepend("<div class='no-button'>&nbsp;</div>");
    }
    $(parent).append(li);
  }

  function createFiles(parent, data, path) {
    $(parent).html("");
    const fileTemp = $("<div class='file'><div class='file-image-cotainer'></div><h5 class='file-title'></h5></div>");
    const noFiles = $("<div id='noFiles'>" + geti18n("component.wcms.navigation.fileBrowser.noFiles") + "</div>");
    if (data.length == 0 && $(".aktiv").data("allowed")) {
      $(parent).html(noFiles);
    }
    $.each(data, function(i, node) {
      if (node.type != "folder") {
        var file = fileTemp.clone();
        if (node.type == "image") {
          $(file).find("div.file-image-cotainer").append("<img class='file-image'></img>");
          $(file).find("img.file-image").attr("src", node.path);
          $(file).find("div.file-image-cotainer").data("path", path);
          $(file).append("<span class='glyphicon glyphicon-remove button button-remove wcms2-image-button hidden'></span>");
          $(file).append("<span class='glyphicon glyphicon-fullscreen wcms2-lightbox wcms2-image-button hidden' aria-hidden='true'></span>");
          $(file).find("span.wcms2-lightbox").attr("data-url", node.path);
        } else {
          $(file).find("div.file-image-cotainer").append("<span class='glyphicon glyphicon-file file-file'></span>");
          $(file).find("div.file-image-cotainer").data("path", path);
        }
        $(file).find("h5.file-title").html(node.name);
        $(file).find("h5.file-title").attr("title", node.name);
        $(parent).append(file);
      }
    });
  }

  function removeFolder(node) {
    const parent = node.parents(".folder")[0];
    node.remove();
    if ($(parent).children("ul.children").length && $(parent).children("ul.children").html() == "") {
      $(parent).children("ul.children").remove();
      $(parent).children(".button").remove();
      $(parent).prepend("<div class='no-button'>&nbsp;</div>");
      $(parent).children(".icon").removeClass("glyphicon-folder-open");
      $(parent).children(".icon").addClass("glyphicon-folder-close");
    }
  }

  function expandFolder(node) {
    const button = $(node).children(".button");
    if (button.hasClass("button-expand")) {
      button.siblings("ul.children").removeClass("hide-folder");
      button.siblings(".icon").removeClass("glyphicon-folder-close");
      button.siblings(".icon").addClass("glyphicon-folder-open");
      button.removeClass("button-expand glyphicon-plus");
      button.addClass("button-contract glyphicon-minus");
    }
  }

  function readQueryParameter() {
    let q = document.URL.split(/\?(.+)?/)[1];
    if (q != undefined) {
      q = q.split('&');
      for (var i = 0; i < q.length; i++) {
        hash = q[i].split(/=(.+)?/);
        qpara.push(hash[1]);
        qpara[hash[0]] = hash[1];
      }
    }
  }

  function changeFolder(folderPath) {
    const folder = $("li").filter(function() {
      return $(this).data("path") == folderPath;
    });
    $(folder).parents(".folder").each(function() {
      expandFolder($(this));
    });
    updateFileView(folder.data("path"));
    currentPath = folder.data("path");
    $(".aktiv").removeClass("aktiv");
    folder.addClass("aktiv");
  }

  function geti18n(key) {
    let string = i18nKeys[key];
    if (string != undefined) {
      for (let i = 0; i < arguments.length - 1; i++) {
        string = string.replace(new RegExp('\\{' + i + '\\}', "g"), arguments[i + 1]);
      }
      return string;
    }
    return "";
  }

  function getRelativePath(path) {
    const pathArray = path.split("/");
    const basePathArray = qpara["href"].split("/");
    let relativePath = "";
    let i = -1;
    while ((basePathArray.length + i) > 0 && pathArray.indexOf(basePathArray[basePathArray.length + i]) === -1) {
      console.log(basePathArray[basePathArray.length + i]);
      relativePath = relativePath + "../";
      i--;
    }
    i = pathArray.indexOf(basePathArray[basePathArray.length + i]) + 1;
    while (i < pathArray.length) {
      relativePath = relativePath + pathArray[i] + "/";
      i++;
    }
    return relativePath;
  }

  return {
    init: function() {
      const $body = $("body");
      const $aktiv = $(".aktiv");

      $body.on("click", ".button-expand", function() {
        expandFolder($(this).closest(".folder"));
      });

      $body.on("click", ".button-contract", function() {
        $(this).siblings("ul.children").addClass("hide-folder");
        $(this).siblings(".icon").removeClass("glyphicon-folder-open");
        $(this).siblings(".icon").addClass("glyphicon-folder-close");
        $(this).removeClass("button-contract glyphicon-minus");
        $(this).addClass("button-expand glyphicon-plus");

      });

      $body.on("click", ".folder-name, .folder > span.icon", function() {
        if (!$(this).parent().hasClass("edit")) {
          changeFolder($(this).parent().data("path"));
        }
      });

      $body.on("click", ".file-image-cotainer", function() {
        if (type === "images") {
          window.callback(getRelativePath($(this).data("path")) + $(this).siblings("h5.file-title").html());
        } else {
          window.callback(baseHref + getRelativePath($(this).data("path")) + $(this).siblings("h5.file-title").html());
        }
        window.close();
      });

      ["dragenter", "dragover", "dragleave", "drop"].forEach(eventName => {
        dropArea.addEventListener(eventName, (e) => {
          e.preventDefault()
          e.stopPropagation()
        }, false)
      });
      ["dragenter", "dragover"].forEach(eventName => {
        dropArea.addEventListener(eventName, () => {
          dropArea.classList.add("highlight")
        }, false)
      });
      ["dragleave", "drop"].forEach(eventName => {
        dropArea.addEventListener(eventName, () => {
          dropArea.classList.remove("highlight")
        }, false)
      });
      dropArea.addEventListener("drop", (e) => {
        let dt = e.dataTransfer;
        let files = dt.files;
        this.uploadFiles(files);
      }, false)

      $body.on("mouseenter", ".file", function() {
        $(this).find(".wcms2-image-button").removeClass("hidden");
      });

      $body.on("mouseleave", ".file", function() {
        $(this).find(".wcms2-image-button").addClass("hidden");
      });

      $body.on("click", ".button-remove", function() {
        const parent = $(this).parent();
        if (confirm(geti18n("component.wcms.navigation.fileBrowser.confirmDeleteFile", parent.find("h5").html()))) {
          deleteFile(currentPath + "/" + parent.find(".file-title").html());
        }
      });

      $body.on("click", "#add-folder", function() {
        if ($aktiv.data("allowed")) {
          expandFolder($aktiv);
          if (!$aktiv.children("ul.children").length) {
            $aktiv.append("<ul class='children'></ul>");
            $aktiv.children(".no-button").remove();
            $aktiv.prepend("<span class='glyphicon glyphicon-minus button button-contract'></span>");
            $aktiv.children(".icon").addClass("glyphicon-folder-open");
            $aktiv.children(".icon").removeClass("glyphicon-folder-close");
          }
          const li = $("<li class='folder edit'><div class='folder-name'><input class='add-folder-input' type='text'></div></li>");
          $(li).prepend("<span class='glyphicon glyphicon-folder-close icon'></span>");
          $(li).prepend("<div class='no-button'>&nbsp;</div>");
          $(li).data("allowed", true);
          $aktiv.children("ul.children").append(li);
          $(li).find("input.add-folder-input").focus();
        } else {
          alert(geti18n("component.wcms.navigation.fileBrowser.invalidCreateFolder"))
        }
      });

      $body.on("keydown", ".add-folder-input", function(key) {
        if (key.which === 13 && $(this).val() !== "") {
          addFolder(currentPath + "/" + $(this).val(), $(this).parents(".folder")[0]);
        }
        if (key.which === 27) {
          removeFolder($(this).closest(".folder"));
        }
      });

      $body.on("click", "#delete-folder", function() {
        if ($aktiv.data("allowed")) {
          if (confirm(geti18n("component.wcms.navigation.fileBrowser.confirmDeleteFolder", $aktiv.find(".folder-name").html()))) {
            deleteFolder(currentPath, $aktiv);
          }
        } else {
          alert(geti18n("component.wcms.navigation.fileBrowser.invalidDeleteFolder"));
        }
      });

      $body.on("click", ".wcms2-lightbox", function(evt) {
        evt.preventDefault();
        showLightbox($(this));
      });

      $body.on("click", "#wcms2-lightbox-close", function() {
        hideLightbox();
      });

      readQueryParameter();
      currentPath = qpara["href"] != undefined ? qpara["href"] : "";
      type = qpara["type"] != undefined ? qpara["type"] : "files";
      baseHref = qpara["basehref"] != undefined ? qpara["basehref"] : "";
      jQuery.getJSON("../../rsc/locale/translate/" + qpara["langCode"] + "/component.wcms.navigation.fileBrowser.*", function(data) {
        i18nKeys = data;
        $("#folder-label").html(geti18n("component.wcms.navigation.fileBrowser.folder"));
        $("#drag-and-drop-info").append(geti18n("component.wcms.navigation.fileBrowser.dragDropInfo"));
        $("#add-folder").append(geti18n("component.wcms.navigation.fileBrowser.addFolder"));
        $("#delete-folder").append(geti18n("component.wcms.navigation.fileBrowser.deleteFolder"));
        getFolders();
      });
    },

    uploadFiles: function(files) {
      files = [...files];
      initializeProgress(files.length);
      files.forEach(uploadFile);
    }

  }

}

$(document).ready(function() {
  window.wcms2FileBrowser = new WCMS2FileBrowser();
  window.wcms2FileBrowser.init();
});
