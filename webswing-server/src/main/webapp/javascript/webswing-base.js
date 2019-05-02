define([ 'webswing-dd', 'webswing-util' ], function amdFactory(WebswingDirectDraw, util) {
    "use strict";
    return function BaseModule() {
        var module = this;
        var api;
        module.injects = api = {
            cfg : 'webswing.config',
            disconnect : 'webswing.disconnect',
            fireCallBack : 'webswing.fireCallBack',
            getSocketId : 'socket.uuid',
            getCanvas : 'canvas.get',
            getInput: 'canvas.getInput',
            registerInput : 'input.register',
            sendInput : 'input.sendInput',
            disposeInput : 'input.dispose',
            registerTouch : 'touch.register',
            disposeTouch : 'touch.dispose',
            getUser : 'login.user',
            login : 'login.login',
            getIdentity : 'identity.get',
            disposeIdentity : 'identity.dispose',
            getLocale : 'identity.getLocale',
            showDialog : 'dialog.show',
            hideDialog : 'dialog.hide',
            startingDialog : 'dialog.content.startingDialog',
            stoppedDialog : 'dialog.content.stoppedDialog',
            applicationAlreadyRunning : 'dialog.content.applicationAlreadyRunning',
            sessionStolenNotification : 'dialog.content.sessionStolenNotification',
            tooManyClientsNotification : 'dialog.content.tooManyClientsNotification',
            continueOldSessionDialog : 'dialog.content.continueOldSessionDialog',
            showSelector : 'selector.show',
            openFileDialog : 'files.open',
            closeFileDialog : 'files.close',
            openLink : 'files.link',
            print : 'files.print',
            download : 'files.download',
            displayCopyBar : 'clipboard.displayCopyBar',
            processJsLink : 'jslink.process',
            playbackInfo : 'playback.playbackInfo'
        };
        module.provides = {
            startApplication : startApplication,
            startMirrorView : startMirrorView,
            continueSession : continueSession,
            kill : kill,
            handshake : handshake,
            processMessage : processMessage,
            dispose : dispose
        };

        var timer1, timer2, timer3;
        var drawingLock;
        var drawingQ = [];

        var windowImageHolders = {};
        var directDraw = new WebswingDirectDraw({logDebug:api.cfg.debugLog, ieVersion:api.cfg.ieVersion, dpr: util.dpr});

        function startApplication(name, applet, alwaysReset) {
            if (alwaysReset===true){
                api.disposeIdentity();
            }
            initialize(api.getUser() + api.getIdentity() + name, name, applet, false);
        }

        function startMirrorView(clientId, appName) {
            initialize(clientId, appName, null, true)
        }

        function initialize(clientId, name, applet, isMirror) {
            api.registerInput();
            api.registerTouch();
            window.addEventListener('beforeunload', beforeUnloadEventHandler);
            resetState();
            api.cfg.clientId = clientId;
            api.cfg.appName = name;
            api.cfg.canPaint = true;
            api.cfg.hasControl = !isMirror;
            api.cfg.mirrorMode = isMirror;
            api.cfg.applet = applet != null ? applet : api.cfg.applet;
            handshake();
            if (isMirror) {
                repaint();
            }
            //api.showDialog(api.startingDialog);
            api.fireCallBack({type: 'webswingInitialied'});
        }

        function beforeUnloadEventHandler(evt) {
            dispose();
        }

        function continueSession() {
            api.hideDialog();
            api.cfg.canPaint = true;
            handshake();
            repaint();
            ack();
            api.fireCallBack({type: 'continueSession'});
        }

        function resetState() {
            api.cfg.clientId = '';
            api.cfg.viewId = util.GUID(); 
            api.cfg.appName = null;
            api.cfg.hasControl = false;
            api.cfg.mirrorMode = false;
            api.cfg.canPaint = false;
            clearInterval(timer1);
            clearInterval(timer2);
            clearInterval(timer3);
            timer1 = setInterval(api.sendInput, 5);
            timer2 = setInterval(heartbeat, 10000);
            timer3 = setInterval(servletHeartbeat, 100000);
            windowImageHolders = {};
            directDraw.dispose();
            directDraw = new WebswingDirectDraw({logDebug:api.cfg.debugLog, ieVersion:api.cfg.ieVersion, dpr: util.dpr});
        }

        function sendMessageEvent(message) {
            api.sendInput({
                event : {
                    type : message
                }
            });
        }

        function heartbeat() {
            sendMessageEvent('hb');
        }

        function servletHeartbeat() {
            //touch servlet session to avoid timeout
            api.login(function(){},function(){});
        }

        function repaint() {
            sendMessageEvent('repaint');
        }

        function ack() {
            sendMessageEvent('paintAck');
        }

        function kill() {
            if (api.cfg.hasControl) {
                sendMessageEvent('killSwing');
            }
        }

        function unload() {
            if(!api.cfg.mirrorMode){
                sendMessageEvent('unload');
            }
        }

        function handshake(continueOldSession) {
            api.sendInput(getHandShake(api.getCanvas(), continueOldSession));
        }

        function dispose() {
            clearInterval(timer1);
            clearInterval(timer2);
            clearInterval(timer3);
            unload();
            api.sendInput();
            drawingQ.length = 0
            drawingQ = null;
            drawingLock = null;
            api.cfg.clientId = null;
            api.cfg.viewId = null;
            api.cfg.appName = null;
            api.cfg.hasControl = false;
            api.cfg.mirrorMode = false;
            api.cfg.canPaint = false;
            for (var key in windowImageHolders) {
                if (windowImageHolders.hasOwnProperty(key)) {
                    windowImageHolders[key] = null;
                }
            }
            windowImageHolders = null;
            api.disposeInput();
            api.disposeTouch();
            window.removeEventListener('beforeunload', beforeUnloadEventHandler);
            directDraw.dispose();
            directDraw = null;
            api.fireCallBack({type: 'webswingDispose'});
        }

        function processMessage(data) {
            if(data.javaResponse && data.javaResponse.value && data.javaResponse.value.javaObject &&  data.javaResponse.value.javaObject.id != null){
                api.fireCallBack({type: "deleteIdforTitle"});
            }
            if (data.playback != null) {
                api.playbackInfo(data);
            }
            if (data.applications != null && data.applications.length != 0) {
                api.showSelector(data.applications);
            }
            if (data.event != null && !api.cfg.recordingPlayback) {
                if (data.event == "shutDownNotification") {
                    api.fireCallBack({type: 'clientShutDown'});
                    api.showDialog(api.stoppedDialog);
                    api.disconnect();
                } else if (data.event == "applicationAlreadyRunning") {
                    api.showDialog(api.applicationAlreadyRunning);
                }
                else if (data.event == "tooManyClientsNotification") {
                    api.showDialog(api.tooManyClientsNotification);
                } else if (data.event == "continueOldSession") {
                    api.cfg.canPaint = false;
                    api.showDialog(api.continueOldSessionDialog);
                } else if (data.event == "continueOldSessionAutomatic") {
                    api.fireCallBack({type: "deleteIdforTitle"});
                    continueSession();
                } else if (data.event == "sessionStolenNotification") {
                    api.fireCallBack({type: 'stopSession'});
                    api.cfg.canPaint = false;
                    if (document.visibilityState === 'visible') {
                        api.showDialog(api.sessionStolenNotification);
                    }
                }
                return;
            }
            if (data.jsRequest != null && api.cfg.mirrorMode == false && !api.cfg.recordingPlayback) {
                api.processJsLink(data.jsRequest);
                return;
            }
            if (api.cfg.canPaint) {
                queuePaintingRequest(data);
            }
        }

        function queuePaintingRequest(data) {
            drawingQ.push(data);
            if (!drawingLock) {
                processNextQueuedFrame();
            }
        }

        function processNextQueuedFrame() {
            drawingLock = null;
            if (drawingQ.length > 0) {
                drawingLock = drawingQ.shift();
                try {
                    processRequest(api.getCanvas(), drawingLock);
                } catch (error) {
                    drawingLock = null;
                    throw error;
                }
            }
        }

        function processRequest(canvas, data) {
            api.hideDialog();
            var context = canvas.getContext("2d");
            if (data.linkAction != null && !api.cfg.recordingPlayback) {
                if (data.linkAction.action == 'url') {
                    api.openLink(data.linkAction.src);
                } else if (data.linkAction.action == 'print') {
                    api.print(encodeURIComponent(location.pathname + 'file?id=' + data.linkAction.src));
                } else if (data.linkAction.action == 'file') {
                    api.download('file?id=' + data.linkAction.src);
                }
            }
            if (data.moveAction != null) {
                copy(data.moveAction.sx, data.moveAction.sy, data.moveAction.dx, data.moveAction.dy, data.moveAction.width, data.moveAction.height,context);
            }
            if (data.focusEvent != null) {
                var input=api.getInput();
                if(data.focusEvent.type === 'focusWithCarretGained'){
                    input.style.top = (data.focusEvent.y+data.focusEvent.caretY)+'px';
                    input.style.left = (data.focusEvent.x+data.focusEvent.caretX)+'px';
                    input.style.height = data.focusEvent.caretH+'px';
                }else{
                    input.style.top = null;
                    input.style.left = null;
                    input.style.height = null;
                }
            }
            if (data.cursorChange != null && api.cfg.hasControl && !api.cfg.recordingPlayback) {
                canvas.style.cursor = getCursorStyle(data.cursorChange);
            }
            if (data.copyEvent != null && api.cfg.hasControl && !api.cfg.recordingPlayback) {
                api.displayCopyBar(data.copyEvent);
            }
            if (data.fileDialogEvent != null && api.cfg.hasControl && !api.cfg.recordingPlayback) {
                if (data.fileDialogEvent.eventType === 'Open') {
                    api.openFileDialog(data.fileDialogEvent, api.cfg.clientId);
                } else if (data.fileDialogEvent.eventType === 'Close') {
                    api.closeFileDialog();
                }
            }
            if (data.closedWindow != null) {
                windowImageHolders[data.closedWindow] = null;
                delete windowImageHolders[data.closedWindow];
            }
            // first is always the background
            for ( var i in data.windows) {
                var win = data.windows[i];
                if (win.id == 'BG') {
                    if (api.cfg.mirrorMode || api.cfg.recordingPlayback) {
                        adjustCanvasSize(canvas, win.width, win.height);
                    }
                    for ( var x in win.content) {
                        var winContent = win.content[x];
                        if (winContent != null) {
                            clear(win.posX + winContent.positionX, win.posY + winContent.positionY, winContent.width, winContent.height, context);
                        }
                    }
                    data.windows.splice(i, 1);
                    break;
                }
            }
            // regular windows (background removed)
            if (data.windows != null) {


                data.windows.reduce(function(sequence, window) {
                    return sequence.then(function() {
                        return renderWindow(window, context);
                    }, errorHandler);
                }, Promise.resolve()).then(function() {
                    ack();
                    processNextQueuedFrame();
                }, errorHandler);
            } else {
                processNextQueuedFrame();
            }
        }

        function renderWindow(win, context) {
            return new Promise(function(resolved, rejected) {
                var rPromise;
            if (win.directDraw != null) {
                    rPromise =  renderDirectDrawWindow(win, context);//windowImageHolders[win.id]);
            } else {
                    rPromise =  renderPngDrawWindow(win, context);
            }
                rPromise.then(function(resultImage) {
                    resolved();
                }, function(error) {
                    rejected(error);
                });
            });
        }

        function renderPngDrawWindow(win, context) {
            var decodedImages = [];
            return win.content.reduce(function(sequence, winContent) {
                return sequence.then(function(decodedImages) {
                    return new Promise(function(resolved, rejected) {
                        if (winContent != null) {
                            var imageObj = new Image();
                            var onloadFunction = function() {
                                //context.drawImage(imageObj, win.posX + winContent.positionX, win.posY + winContent.positionY);
                                decodedImages.push({image:imageObj,winContent:winContent})
                                resolved(decodedImages);
                                
                            };
                            imageObj.onload = function() {
                                // fix for ie - onload is fired before the image is ready for rendering to canvas.
                                // This is an ugly quickfix
                                if (api.cfg.ieVersion && api.cfg.ieVersion <= 10) {
                                    window.setTimeout(onloadFunction, 20);
                                } else {
                                    onloadFunction();
                                }
                            };
                            imageObj.src = util.getImageString(winContent.base64Content);
                        }
                    });
                }, errorHandler);
            }, Promise.resolve(decodedImages)).then(function(decodedImages){
                decodedImages.forEach(function(image, idx){

                    if(win.sprites.length != 0){
                        for(var i = 36 * idx; i < 36 * (idx+1) && i < win.sprites.length; i++ ){
                            var sprite = win.sprites[i];
                            context.drawImage(image.image, sprite.spriteX, sprite.spriteY, sprite.width, sprite.height, win.posX + sprite.positionX, win.posY + sprite.positionY, sprite.width, sprite.height);
                        }
                    } else {
                        context.drawImage(image.image, win.posX + image.winContent.positionX, win.posY + image.winContent.positionY);
                    }
                    image.image.onload = null;
                    image.image.src = '';
                    if (image.image.clearAttributes != null) {
                        image.image.clearAttributes();
                    }
                })
            });
        }

        function renderDirectDrawWindow(win, context) {
            return new Promise(function(resolved, rejected) {
                var ddPromise;
                if (typeof win.directDraw === 'string') {
                    ddPromise = directDraw.draw64(win.directDraw, windowImageHolders[win.id]);
                } else {
                    ddPromise = directDraw.drawBin(win.directDraw, windowImageHolders[win.id]);
                }
                ddPromise.then(function(resultImage) {
                    windowImageHolders[win.id] = resultImage;
                                var dpr = util.dpr();
                    for ( var x in win.content) {
                        var winContent = win.content[x];
                        if (winContent != null) {
                                       var sx = Math.min(resultImage.width, Math.max(0, winContent.positionX * dpr));
                                       var sy = Math.min(resultImage.height, Math.max(0, winContent.positionY * dpr));
                                       var sw = Math.min(resultImage.width - sx, winContent.width * dpr - (sx - winContent.positionX * dpr));
                                       var sh = Math.min(resultImage.height - sy, winContent.height * dpr - (sy - winContent.positionY * dpr));

                                       var dx = win.posX * dpr + sx;
                                       var dy = win.posY * dpr + sy;
                                        var dw = sw;
                                        var dh = sh;

                                        if (dx <= context.canvas.width && dy <= context.canvas.height && dx + dw > 0 && dy + dh > 0) {
                                            context.drawImage(resultImage, sx, sy, sw, sh, dx, dy, dw, dh);
                                        }
                        }
                    }
                    resolved();
                }, function(error) {
                    rejected(error);
                });
            });
        }

        function adjustCanvasSize(canvas, width, height) {
        	var dpr = util.dpr();
            if (canvas.width != width * dpr || canvas.height != height * dpr) {
                var ctx = canvas.getContext("2d");
                var snapshot = ctx.getImageData(0, 0, canvas.width, canvas.height);
                canvas.width = width * dpr;
                canvas.height = height * dpr;
                ctx.putImageData(snapshot, 0, 0);
                if (typeof(CollectGarbage) == "function") {
                    CollectGarbage();
                }
            }
        }

        function clear(x, y, w, h, context) {
            var dpr = util.dpr();
            context.clearRect(x * dpr, y * dpr, w * dpr, h * dpr);
        }

        function copy(sx, sy, dx, dy, w, h, context) {
            var dpr = util.dpr();
            var copy = context.getImageData(sx * dpr, sy * dpr, w * dpr, h * dpr);
            context.putImageData(copy, dx * dpr, dy * dpr);
            if (typeof(CollectGarbage) == "function") {
                CollectGarbage();
            }
        }

        function getCursorStyle(cursorMsg) {
            if (cursorMsg.b64img == null) {
                return cursorMsg.cursor;
            } else {
                var data = util.getImageString(cursorMsg.b64img);
                return 'url(' + data + ') ' + cursorMsg.x + ' ' + cursorMsg.y + ' , auto';
            }
        }

        function getHandShake(canvas, continueOldSession) {
            var handshake = {
                applicationName : api.cfg.appName,
                clientId : api.cfg.clientId,
                viewId : api.cfg.viewId,
                sessionId : api.getSocketId(),
                locale : api.getLocale(),
                mirrored : api.cfg.mirrorMode,
                directDrawSupported : api.cfg.typedArraysSupported  && !(api.cfg.ieVersion && api.cfg.ieVersion <= 10),
                continueSession : continueOldSession || false
            };

            if (!api.cfg.mirrorMode) {
                handshake.applet = api.cfg.applet;
                handshake.documentBase = api.cfg.documentBase;
                handshake.params = api.cfg.params;
                handshake.desktopWidth = canvas.offsetWidth;
                handshake.desktopHeight = canvas.offsetHeight;
            }
            return {
                handshake : handshake
            };
        }

        function errorHandler(error) {
            drawingLock = null;
            throw error;
        }
    };

});