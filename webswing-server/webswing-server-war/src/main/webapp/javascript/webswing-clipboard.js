define([ 'jquery', 'text!templates/clipboard.html', 'text!templates/clipboard.css', 'webswing-util' ], function amdFactory($, html, css, util) {
	"use strict";
	var style = $("<style></style>", {
		type : "text/css"
	});
	style.text(css);
	$("head").prepend(style);

	return function ClipboardModule() {
		var module = this;
		var api;
		module.injects = api = {
			cfg : 'webswing.config',
			send : 'socket.send',
			getInput : 'canvas.getInput'
		};
		module.provides = {
			cut : cut,
			copy : copy,
			paste : paste,
			displayCopyBar : displayCopyBar,
			dispose : close
		};
		module.ready = function() {
			document.addEventListener("copy",copy);
		};

		var copyBar;

		function cut(event) {
			copy(event, true);
		}

		function copy(event, cut) {
			if (copyBar == null) {
				if (api.cfg.ieVersion) {
					window.clipboardData.setData('Text', '');
				} else {
					event.clipboardData.setData('text/plain', '');
				}
				api.send({
					copy : {
						type : cut === true ? 'cut' : 'copy'
					}
				});
			} else {
				var data = copyBar.wsEventData;
				if (api.cfg.ieVersion) {
					// handling of copy events only for IE
					var ieClipboardDiv = copyBar.find('div[data-id="ie-clipboard"]');
					var clipboardData = window.clipboardData;
					if (data.html != null) {
						ieClipboardDiv.html(data.html);
						focusIeClipboardDiv();
						setTimeout(function() {
							close();
						}, 0);
					} else {
						event.preventDefault();
						clipboardData.setData('Text', data.text);
						close();
					}

				} else {
					// handling of copy events for rest of browsers
					event = event.originalEvent || event;
					event.preventDefault();
					if (data.text != null) {
						event.clipboardData.setData('text/plain', data.text);
					}
					if (data.html != null) {
						event.clipboardData.setData('text/html', data.html);
					}
					close();
				}
			}
		}

		function focusIeClipboardDiv() {
			ieClipboardDiv.focus();
			var range = document.createRange();
			range.selectNodeContents((ieClipboardDiv.get(0)));
			var selection = window.getSelection();
			selection.removeAllRanges();
			selection.addRange(range);
		}

		function paste(event) {
			if (api.cfg.hasControl) {
				var text = '';
				var html = '';
				if (api.cfg.ieVersion) {
					text = window.clipboardData.getData('Text');
					html = text;
				} else {
					var data = event.clipboardData || event.originalEvent.clipboardData;
					text = data.getData('text/plain');
					html = data.getData('text/html');
					if (data.items != null) {
						for (var i = 0; i < data.items.length; i++) {
							if (data.items[i].type.indexOf('image') === 0) {
								var img = data.items[i];
								var reader = new FileReader();
								reader.onload = function(event) {
									sendPasteEvent(text, html, event.target.result);
								};
								reader.readAsDataURL(img.getAsFile());
								return;
							}
						}
					}
				}
				sendPasteEvent(text, html);
			}
		}

		function sendPasteEvent(text, html, img) {
			var pasteObj = {};
			if (text != null && text.length !== 0) {
				pasteObj.text = text;
			}
			if (html != null && html.length !== 0) {
				pasteObj.html = html;
			}
			if (img != null) {
				pasteObj.img = img;
			}

			api.send({
				paste : pasteObj
			});
		}

		function displayCopyBar(data) { // trigered by swing app
			if (copyBar != null) {
				close();
			}
			api.cfg.rootElement.append(html);
			copyBar = api.cfg.rootElement.find('div[data-id="copyBar"]');
			copyBar.on('mouseleave', function(event) {
				minimize();
			});
			copyBar.wsEventData = data;
			var closeBtn = copyBar.find('button[data-id="closeBtn"]');
			closeBtn.click(function() {
				close();
			});
			copyBar.show("fast");

			/* TEXT TAB */
			var copyBtn = copyBar.find('button[data-id="text"]');
			if ((data.text != null && data.text.length !== 0) || (data.html != null && data.html.length !== 0)) {
				var textarea = copyBar.find('div[data-id="textarea"]');
				if (data.text != null && data.text.length !== 0) {
					textarea.append($('<pre class="c-tab-content__text-pre"></pre>').text(data.text));
				} else {
					textarea.html('<iframe class="c-tab-content__text-iframe" src="data:text/html;charset=utf-8,' + encodeURIComponent(data.html) + '"></iframe>');
				}
				copyBtn.on('mouseenter', function() {
					showTab(filesTab, 'text');
					maximize();
				});
			}
			copyBtn.on('click', function(e) {
				document.execCommand("copy");
			});
			copyBtn.removeClass("c-minimized-tab--is-inactive").addClass("c-minimized-tab--is-active");
			showTab(copyBtn, 'text');

			/* More TAB */
			if ((data.files != null && data.files.length !== 0) || data.img != null) {
				if (data.files != null && data.files.length !== 0) {
					var fileListElement = copyBar.find('#wsFileList');
					for (var i = 0; i < data.files.length; i++) {
						var fileName = data.files[i];
						var link = $('<a>');
						if (fileName.indexOf("#") === 0) {
							link = $('<span>');
							link.html(data.files[i].substring(1));
						} else {
							link.html(data.files[i]);
							link.on('click', function(event) {
								api.send({
									copy : {
										type : 'getFileFromClipboard',
										file : $(event.currentTarget).html()
									}
								});
							});
						}
						fileListElement.append(link);
						fileListElement.append("<br/>");
					}
				} else {
					copyBar.find('div[data-id="files"]').remove();
				}
				if (data.img != null) {
					var clipImgDataUrl = util.getImageString(data.img);
					copyBar.find('div[data-id="image"]').append('<a target="_blank" download="clipboard.png" href="' + clipImgDataUrl + '"><img src="' + clipImgDataUrl + '" id="wsCopyImage" class="c-tab-content__img-thumb"></a>');
				} else {
					copyBar.find('div[data-id="image"]').remove();
				}

				var filesTab = copyBar.find('button[data-id="more"]');
				filesTab.on('mouseenter', function() {
					showTab(filesTab, 'more');
					maximize();
				});
			} else {
				var filesTab = copyBar.find('button[data-id="more"]');
				filesTab.remove();
			}

			function showTab(tab, type) {
				copyBar.find('.c-minimized-tab--is-selected').removeClass('c-minimized-tab--is-selected');
				copyBar.find('.c-tab-content__item').removeClass('c-tab-content__item--is-active');
				$(tab).addClass('c-minimized-tab--is-selected');
				copyBar.find('div[data-id="' + type + '"]').addClass('c-tab-content__item--is-active');
			}

			copyBar.find('div[data-id="contentBar"]').hide();
		}

		function minimize() {
			if (copyBar != null) {
				copyBar.find('.c-minimized-tab--is-selected').removeClass('c-minimized-tab--is-selected');
				copyBar.find('div[data-id="contentBar"]').slideUp('fast');
				copyBar.minimized = true;
			}
			api.getInput().focus();
		}

		function maximize() {
			if (copyBar != null) {
				copyBar.find('div[data-id="contentBar"]').slideDown('fast');
				copyBar.minimized = false;
			}
		}

		function close() {
			if (copyBar != null) {
				copyBar.hide("fast");
				copyBar.remove();
				copyBar = null;
			}
			api.getInput().focus();
		}

	};
});