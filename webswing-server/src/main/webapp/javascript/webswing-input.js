define([ 'jquery', 'webswing-util' ], function amdFactory($, util) {
    "use strict";

    return function InputModule() {
        var module = this;
        var api;
        module.injects = api = {
            cfg : 'webswing.config',
            send : 'socket.send',
            getInput : 'canvas.getInput',
            getCanvas : 'canvas.get',
            cut : 'clipboard.cut',
            copy : 'clipboard.copy',
            paste : 'clipboard.paste'
        };
        module.provides = {
            register : register,
            sendInput : sendInput,
            dispose : dispose
        };

        module.ready = function() {
        };

        var registered = false;
        var latestMouseMoveEvent = null;
        var latestMouseWheelEvent = null;
        var latestWindowResizeEvent = null;
        var mouseDown = 0;
        var inputEvtQueue = [];
        var compositionInput = false;

        function sendInput(message) {
            enqueueInputEvent();
            var evts = inputEvtQueue;
            inputEvtQueue = [];
            if (message != null) {
                evts.push(message);
            }
            if (evts.length > 0) {
                api.send({
                    events : evts
                });
            }
        }

        function enqueueInputEvent(message) {
            if (api.cfg.hasControl) {
                if (latestMouseMoveEvent != null) {
                    inputEvtQueue.push(latestMouseMoveEvent);
                    latestMouseMoveEvent = null;
                }
                if (latestMouseWheelEvent != null) {
                    inputEvtQueue.push(latestMouseWheelEvent);
                    latestMouseWheelEvent = null;
                }
                if (latestWindowResizeEvent != null) {
                    inputEvtQueue.push(latestWindowResizeEvent);
                    latestWindowResizeEvent = null;
                }
                if (message != null) {
                    if (JSON.stringify(inputEvtQueue[inputEvtQueue.length - 1]) !== JSON.stringify(message)) {
                        inputEvtQueue.push(message);
                    }
                }
            }
        }

        function resetInput() {
            latestMouseMoveEvent = null;
            latestMouseWheelEvent = null;
            latestWindowResizeEvent = null;
            mouseDown = 0;
            inputEvtQueue = [];
        }

        function dispose() {
            registered = false;
            resetInput();
            document.removeEventListener('mousedown', mouseDownEventHandler);
            document.removeEventListener('mouseout', mouseOutEventHandler);
            document.removeEventListener('mouseup', mouseUpEventHandler);
        }

        function register() {
            if (registered) {
                return;
            }
            var canvas = api.getCanvas();
            var input = api.getInput();
            resetInput();
            focusInput(input);
            util.bindEvent(canvas, 'mousedown', function(evt) {
                var mousePos = getMousePos(canvas, evt, 'mousedown');
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput(input);
                sendInput();
                return false;
            }, false);
            util.bindEvent(canvas, 'dblclick', function(evt) {
                var mousePos = getMousePos(canvas, evt, 'dblclick');
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput(input);
                sendInput();
                return false;
            }, false);
            util.bindEvent(canvas, 'mousemove', function(evt) {
                var mousePos = getMousePos(canvas, evt, 'mousemove');
                mousePos.mouse.button = mouseDown;
                latestMouseMoveEvent = mousePos;
                return false;
            }, false);
            util.bindEvent(canvas, 'mouseup', function(evt) {
                var mousePos = getMousePos(canvas, evt, 'mouseup');
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput(input);
                sendInput();
                return false;
            }, false);
            // IE9, Chrome, Safari, Opera
            util.bindEvent(canvas, "mousewheel", function(evt) {
                var mousePos = getMousePos(canvas, evt, 'mousewheel');
                latestMouseMoveEvent = null;
                if (latestMouseWheelEvent != null) {
                    mousePos.mouse.wheelDelta += latestMouseWheelEvent.mouse.wheelDelta;
                }
                latestMouseWheelEvent = mousePos;
                return false;
            }, false);
            // firefox
            util.bindEvent(canvas, "DOMMouseScroll", function(evt) {
                var mousePos = getMousePos(canvas, evt, 'mousewheel');
                latestMouseMoveEvent = null;
                if (latestMouseWheelEvent != null) {
                    mousePos.mouse.wheelDelta += latestMouseWheelEvent.mouse.wheelDelta;
                }
                latestMouseWheelEvent = mousePos;
                return false;
            }, false);
            util.bindEvent(canvas, 'contextmenu', function(event) {
                event.preventDefault();
                event.stopPropagation();
                return false;
            });
            function isCharatersPress(kc){
                return (kc>=97&&kc<=122)||(kc>=65&&kc<=90);

            }
            function isHotKeyBehavoir(keyevt){
                return isCharatersPress(keyevt.key.keycode)&&keyevt.key.alt&&keyevt.key.shift&&keyevt.key.ctrl;

            }
            function convertHotKey(keyevt){
                keyevt.key.shift = false;
                keyevt.key.ctrl = false;
            }

            util.bindEvent(input, 'keydown', function(event) {
                var functionKeys=[9/*tab*/, 12/*Numpad5*/, 16/*Shift*/, 17/*ctrl*/, 18/*alt*/, 19/*pause*/, 20/*CapsLock*/, 27/*esc*/, 
                                  32/*space*/, 33/*pgup*/, 34/*pgdown*/, 35/*end*/, 36/*home*/, 37/*left*/, 38/*up*/, 39/*right*/, 40/*down*/, 44/*prtscr*/, 
                                  45/*insert*/, 46/*Delete*/, 91/*OSLeft*/, 92/*OSRight*/, 93/*Context*/, 145/*scrlck*/, 225/*altGraph(Linux)*/,
                                  112/*F1*/, 113/*F2*/, 114/*F3*/, 115/*F4*/, 116/*F5*/, 117/*F6*/, 118/*F7*/, 119/*F8*/, 120/*F9*/,
                                  121/*F10*/, 122/*F11*/, 123/*F12*/, 124/*F13*/, 125/*F14*/, 126/*F15*/, 127/*F16*/, 128/*F17*/, 129/*F18*/, 130/*F19*/, 131/*F20*/,
                                  132/*F21*/, 133/*F22*/, 134/*F23*/, 135/*F24*/]; 
            	
            	var kc = event.keyCode;
                //mute F1 help
                if(kc === 112) {
                    return false;
                }
                if (functionKeys.indexOf(kc)!=-1) {
                    if(!api.cfg.virtualKB){
                        event.preventDefault();
                        event.stopPropagation();
                    }
                }
                var keyevt = getKBKey('keydown', canvas, event);
                // hanle paste event
                if (!(keyevt.key.ctrl && (keyevt.key.character == 88 || keyevt.key.character == 67 || keyevt.key.character == 86))) { // cut copy
                    // default action prevented
                    if (keyevt.key.ctrl && !keyevt.key.alt && !keyevt.key.altgr) {
                        event.preventDefault();
                    }
                    if(isHotKeyBehavoir(keyevt)){
                        convertHotKey(keyevt);
                    }
                    enqueueInputEvent(keyevt);
                }
                return false;
            }, false);
            util.bindEvent(input, 'keypress', function(event) {
                var keyevt = getKBKey('keypress', canvas, event);
                if (!(keyevt.key.ctrl && (keyevt.key.character == 120 || keyevt.key.character == 24 || keyevt.key.character == 99
                        || keyevt.key.character == 118 || keyevt.key.character == 22))) { // cut copy paste handled separately
                    event.preventDefault();
                    event.stopPropagation();
                    if(isHotKeyBehavoir(keyevt)){
                        convertHotKey(keyevt);
                    }
                    enqueueInputEvent(keyevt);
                }
                return false;
            }, false);
            util.bindEvent(input, 'keyup', function(event) {
                var keyevt = getKBKey('keyup', canvas, event);
                if (!(keyevt.key.ctrl && (keyevt.key.character == 88 || keyevt.key.character == 67 || keyevt.key.character == 86))) { // cut copy
                    event.preventDefault();
                    event.stopPropagation();
                    if(isHotKeyBehavoir(keyevt)){
                        convertHotKey(keyevt);
                    }
                    enqueueInputEvent(keyevt);
                    sendInput();
                }
                return false;
            }, false);

            var DEFAULT_FONT = '14px sans-serif';
            // 中文输入法（如搜狗，智能ABC）等最终可见字符需要一连串键盘输入才能形成的，当用户开始输入字符的时候，
            // 需要将承载用户输入的input设置成可见（Z-INDEX不为-1等），以便input能回显一连串输入过程中的文字
            util.bindEvent(input, 'compositionstart', function(event) {
                compositionInput = true;
                var input=api.getInput();
                $(input).addClass('input-ime');
                $(input).removeClass('input-hidden');
                input.style.font = DEFAULT_FONT;
            }, false);
            // 中文输入法（如搜狗，智能ABC）等最终可见字符需要一连串键盘输入才能形成的，通过compositionend来获取一连串输入之后的最终可见字符
            util.bindEvent(input, 'compositionend', function (event) {
                var input = api.getInput();
                $(input).addClass('input-hidden');
                $(input).removeClass('input-ime');
                input.style.width = '1px';
                // IE 11以下版本不能从event.data里获取输入的值，需要从input控件里获取用户输入的值
                var isIE = api.cfg.ieVersion && api.cfg.ieVersion <= 11;
                sentWordsUsingKeypressEvent(isIE ? event.target.value : event.data);
                compositionInput = false;
                focusInput(input);
            }, false);
            // 对于IE,CHROME标点符号不能显示问题修复
            // IE,CHROME标点符号输入不会触发compositionStart compositionEnd事件，导致这些浏览器标点符号输入无法被webswing捕获到
            util.bindEvent(input, 'input', function(event) {
                var input = api.getInput();
                // 中文输入法输入汉字的时候夹带标点符号也会触发本事件，但由compositionEnd来完成用户文本录入，
                // 通过设置compositionInput 为true，将将该过程中触发的事件忽略掉
                // input的值为空的时候（比如汉字输入为了兼容所有浏览器，统一使用compositionend事件来处理，compositionend事件处理调用了focusInput导致input为空）不需要向后台发送
                if(compositionInput || !input.value){
                    return;
                }
                // chrome
                if (((!event.isComposing && event.inputType =='insertText' && event.data!=null)
                    // IE
                    || (api.cfg.ieVersion && event.type ==="input"))
                    // IE focusInput新增空格并选中会触发input事件并走到这个分支，无效输入要过滤
                    && input.value != " "
                ) {
                    sentWordsUsingKeypressEvent(input.value)
                    focusInput(input);
                }
            }, false);

            // 用户输入过程中的回显用input承载，需要用及时调整宽度
            util.bindEvent(input, 'compositionupdate', function(event) {
                var input=api.getInput();
                var text = event.data;
                input.style.width = getTextWidth(text, DEFAULT_FONT)+'px';
            }, false);

            util.bindEvent(input, 'cut', function(event) {
                event.preventDefault();
                event.stopPropagation();
                api.cut(event);
                return false;
            }, false);
            util.bindEvent(input, 'copy', function(event) {
                event.preventDefault();
                event.stopPropagation();
                api.copy(event);
                return false;
            }, false);
            util.bindEvent(input, 'paste', function(event) {
                event.preventDefault();
                event.stopPropagation();
                api.paste(event);
                return false;
            }, false);
            util.bindEvent(document, 'mousedown', mouseDownEventHandler);
            util.bindEvent(document, 'mouseout', mouseOutEventHandler);
            util.bindEvent(document, 'mouseup', mouseUpEventHandler);

            registered = true;
        }

        // 获取文本宽度
        function getTextWidth(text, font) {
            var canvas = api.getCanvas();
            var ctx = canvas.getContext("2d");
            ctx.save();
            ctx.font = font;
            var metrics = ctx.measureText(text);
            ctx.restore();
            return Math.ceil(metrics.width)+5;
        }

        function mouseDownEventHandler(evt) {
            if (evt.which == 1) {
                mouseDown = 1;
            }
        }

        function mouseOutEventHandler(evt) {
            // canvas会铺满整个界面
            // 当鼠标移除document范围后，鼠标松开的事件需要监听在document之上。
            // 中文输入的时候，承载中文输入的input空间也会触发本事件，需忽略
            if (api.cfg.hasControl && api.cfg.canPaint && !api.cfg.mirrorMode && !compositionInput) {
                var mousePos = getMousePos(api.getCanvas(), evt, 'mouseup');
                //when an new web page pops after user click, mouseup will send twice
                mousePos.mouse.x = -1;
                mousePos.mouse.y = -1;
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput(api.getInput());
                sendInput();
            }
            mouseDown = 0;
        }

        function mouseUpEventHandler(evt) {
            if (evt.which == 1) {
                mouseDown = 0;
            }
        }

        function focusInput(input) {
            // In order to ensure that the browser will fire clipboard events, we always need to have something selected
            // scrollX , scrollY attributes on IE gives undefined, so changed to compatible pageXOffset,pageYOffset
            var sx = window.pageXOffset, sy = window.pageYOffset;
            input.value = ' ';
            // set the style attributes as the focus/select cannot work well in IE
            input.style.top = sy +'px';
            input.style.left = sx +'px';
            $(input).focus(function(){preventScroll:true});
            input.select();
            window.scrollTo(sx,sy);
        }

        // 当快速输入多个中文标点的时候，一个标点发送一次拷贝事件，但是webswing里应用程序EDT处理时因为只有一个剪切板，而粘贴处理是先将内容放到剪切板里，
        // 然后出发拷贝事件，如果事件没有及时处理，导致后输入的标点在剪切板里将前面的标点给覆盖掉，所以需要用keypress事件来发送文本
        function sentWordsUsingKeypressEvent(data) {
            for (var i = 0, length = data.length; i < length ;i++) {
                inputEvtQueue.push({
                    key : {
                        type : 'keypress',
                        character : data.charCodeAt(i),
                        keycode : 0,
                    }
                });
            }
        }

        function getMousePos(canvas, evt, type) {
            var rect = canvas.getBoundingClientRect();
            var scaleX = 1;
            var scaleY = 1;

            //for lesser zoom %
            if(window.innerWidth > canvas.width)
            {
                scaleX = (window.innerWidth-canvas.width)/canvas.width + 1;
            }
            if(window.innerHeight > Math.round(canvas.height + rect.top))
            {
                scaleY = (window.innerHeight-canvas.height - rect.top)/canvas.height + 1;
            }
            var mouseX = 0;
            var mouseY = 0;
            var translateX = (window.innerWidth-canvas.width)/2;
            var translateY = (window.innerHeight-canvas.height - rect.top)/2;

            // return relative mouse position
            if(scaleX > 1 && scaleY > 1)
            {
                mouseX = Math.round(window.innerWidth/2 - translateX - rect.left - (window.innerWidth/2 - evt.clientX)/scaleX);
                mouseY = Math.round((window.innerHeight - rect.top)/2 - translateY - ((window.innerHeight + rect.top)/2 - evt.clientY)/scaleY);
            }
            else if(scaleX > 1)
            {
                mouseX = Math.round(window.innerWidth/2 - translateX - rect.left - (window.innerWidth/2 - evt.clientX)/scaleX);
                mouseY = Math.round(evt.clientY - rect.top);
            }
            else if(scaleY > 1)
            {
                mouseX = Math.round(evt.clientX - rect.left);
                mouseY = Math.round((window.innerHeight - rect.top)/2 - translateY - ((window.innerHeight + rect.top)/2 - evt.clientY)/scaleY);
            }
            else
            {
                mouseX = Math.round(evt.clientX - rect.left);
                mouseY = Math.round(evt.clientY - rect.top);
            }
            var delta = 0;
            if (type == 'mousewheel') {
                delta = -Math.max(-1, Math.min(1, (evt.wheelDelta || -evt.detail)));
            }
            return {
                mouse : {
                    x : mouseX,
                    y : mouseY,
                    type : type,
                    wheelDelta : delta,
                    button : evt.which,
                    ctrl : evt.ctrlKey,
                    alt : evt.altKey,
                    shift : evt.shiftKey,
                    meta : evt.metaKey
                }
            };
        }

        function getKBKey(type, canvas, evt) {
            var char = evt.which;
            if (char == 0 && evt.key != null) {
                char = evt.key.charCodeAt(0);
            }
            var kk = evt.keyCode;
            if (kk == 0) {
                kk = char;
            }
            return {
                key : {
                    type : type,
                    character : char,
                    keycode : kk,
                    alt : evt.altKey,
                    ctrl : evt.ctrlKey,
                    shift : evt.shiftKey,
                    meta : evt.metaKey
                }
            };
        }

    };

});