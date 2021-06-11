import $ from 'jquery';
import html from './templates/clipboard.html';
import css from './templates/clipboard.css';
//import Util from './webswing-util';
import i18n from './webswing-i18n';

export default class ClipboardModule {	
    constructor() {
		let style = $("<style></style>", {
            type : "text/css"
        });
        style.text(css);
        $("head").prepend(style);
        let module = this;
        let api;
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

        let copyBar;
        let isPasteDataToClient = false;

        function cut(event) {
            copy(event, true);
        }

        function copy(event, cut) {
            if (copyBar == null || copyBar.minimized === true) {
                api.send({
                    copy : {
                        type : cut === true ? 'cut' : 'copy'
                    }
                });
            } else {
                let data = copyBar.wsEventData;
                if (api.cfg.ieVersion) {
                    // handling of copy events only for IE
                    let ieClipboardDiv = copyBar.find('div[data-id="ie-clipboard"]');
                    let clipboardData = window.clipboardData;
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
            let range = document.createRange();
            range.selectNodeContents((ieClipboardDiv.get(0)));
            let selection = window.getSelection();
            selection.removeAllRanges();
            selection.addRange(range);
        }

        function paste(event) {
            if (api.cfg.hasControl) {
                if (useLocalClipboard()) {
                    let text = '';
                    let html = '';
                    if (api.cfg.ieVersion) {
                        text = window.clipboardData.getData('Text');
                        html = text;
                    } else {
                        let data = event.clipboardData || event.originalEvent.clipboardData;
                        text = data.getData('text/plain');
                        html = data.getData('text/html');
                        if (data.items != null) {
                            for ( let i = 0; i < data.items.length; i++) {
                                if (data.items[i].type.indexOf('image') === 0) {
                                    let img = data.items[i];
                                    let reader = new FileReader();
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
                } else {
                    sendPasteEvent();
                }
            }
        }

        function sendPasteEvent(text, html, img) {
            let pasteObj = {};
            if (text != null) {
                pasteObj.text = text;
            }
            if (html != null) {
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

        	let onlyOtherData=false;
            if (copyBar != null) {
                close();
            }
            if (data.html == null && data.text == null && data.img == null && (data.files == null || data.files.length === 0)) {
                if(!data.other){
                	return;
                }else{
                	onlyOtherData=true;
                }
            }

            if(data.text && navigator.clipboard && navigator.clipboard.writeText){
                navigator.clipboard.writeText(data.text).then(function() {
                   return;
                }, function() {
                    showCopyBar(data);
                });
            }else {
                showCopyBar(data);
            }

        }

        function showCopyBar(data){
            api.cfg.rootElement.append(html);
            copyBar = api.cfg.rootElement.find('div[data-id="copyBar"]');
            copyBar.on('click', function(event) {
                clearTimeout(minimizer);
                api.getInput().focus({preventScroll: true});
            });
            copyBar.wsEventData = data;
            copyBar.minimized = false;
            let closeBtn = copyBar.find('button[data-id="closeBtn"]');
            closeBtn.click(function() {
                close();
            });
            copyBar.show("fast");
            /* TEXT TAB */
            if (data.text == null || data.text.length === 0) {
                copyBar.find('#text').remove();
                copyBar.find('#textTab').remove();
            } else {
                let textarea = copyBar.find('textarea[data-id="textarea"]');
                textarea.val(data.text);
                copyBar.find('span[data-id="plaintext"]').removeClass("webswing-copy-content-inactive").addClass("webswing-copy-content-active");
            }
            /* HTML TAB */
            if (data.html == null || data.html.length === 0) {
                copyBar.find('#html').remove();
                copyBar.find('#htmlTab').remove();
            } else {
                let htmlarea = copyBar.find('textarea[data-id="htmlarea"]');
                htmlarea.val(data.html);
                copyBar.find('span[data-id="html"]').removeClass("webswing-copy-content-inactive").addClass("webswing-copy-content-active");
            }
            /* IMAGE TAB */
            if (data.img == null) {
                copyBar.find('#image').remove();
                copyBar.find('#imageTab').remove();
            } else {
                copyBar.find('#image>div').append('<img src="' + util.getImageString(data.img) + '" id="wsCopyImage" class="img-thumbnail">');
                copyBar.find('span[data-id="image"]').removeClass("webswing-copy-content-inactive").addClass("webswing-copy-content-active");
            }
            /* FILES TAB */
            if (data.files == null || data.files.length === 0) {
                copyBar.find('#files').remove();
                copyBar.find('#filesTab').remove();
            } else {
                let fileListElement = copyBar.find('#wsFileList');
                for ( let i = 0; i < data.files.length; i++) {
                    let fileName = data.files[i];
                    let link = $('<a>');
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
                copyBar.find('span[data-id="files"]').removeClass("webswing-copy-content-inactive").addClass("webswing-copy-content-active");
            }
            /* OTHER TAB */
            if (!data.other) {
                copyBar.find('#other').remove();
                copyBar.find('#otherTab').remove();
            } else {
                copyBar.find('span[data-id="other"]').removeClass("webswing-copy-content-inactive").addClass("webswing-copy-content-active");
            }

            /* TAB Activation */
            let tabs = copyBar.find('.nav-tabs>li');
            tabs.first().addClass('active');
            copyBar.find('.tab-pane').first().addClass('active');
            tabs.on('click', function(event) {
                tabs.removeClass('active');
                copyBar.find('.tab-pane').removeClass('active');
                $(event.currentTarget).addClass('active');
                copyBar.find('#' + $(event.currentTarget).data('tab')).addClass('active');
            });

            let infoBar = copyBar.find('div[data-id="minimizedInfoBar"]');
            infoBar.on('click', function(event) {// maximize
                maximize();
            });

            let minimizeBtn = copyBar.find('.webswing-minimize-symbol');
            minimizeBtn.on('click', function(event) {// minimize
                minimize();
            });


            if (onlyOtherData) {
                copyBar.find('div[data-id="contentBar"]').hide();
                copyBar.find('div[data-id="minimizedInfoBar"]').show();
                copyBar.minimized = true;
            }else{
                let minimizer = setTimeout(function() {
                    minimize();
                }, 2000);
            }
        }

        function minimize() {
            if (copyBar != null) {
                copyBar.find('div[data-id="contentBar"]').slideUp('fast');
                copyBar.find('div[data-id="minimizedInfoBar"]').fadeIn('fast');
                copyBar.minimized = true;
            }
        }

        function maximize() {
            if (copyBar != null) {
                copyBar.find('div[data-id="contentBar"]').slideDown('fast');
                copyBar.find('div[data-id="minimizedInfoBar"]').fadeOut('fast');
                copyBar.minimized = false;
            }
        }

        function close() {
            if (copyBar != null) {
                copyBar.hide("fast");
                copyBar.remove();
                copyBar = null;
            }
        }

        function useLocalClipboard() {
            if (copyBar == null) {
                return true;
            }
        }

    }
}
