import $ from 'jquery';
import Util from './webswing-util';

export default class InputModule {
	constructor() {
		let prepos;
        let module = this;
        let api;
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
            focusInput : focusInput,
            dispose : dispose
        };

        module.ready = function() {
        };

        let registered = false;
        let latestMouseMoveEvent = null;
        let latestMouseWheelEvent = null;
        let latestWindowResizeEvent = null;
        let mouseDown = 0;
        var mouseDownCanvas = null;
        let inputEvtQueue = [];
        let compositionInput = false;

        function sendInput(message) {
            enqueueInputEvent();
            let evts = inputEvtQueue;
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
            resetMouseDownCanvas();
            inputEvtQueue = [];
        }

        function dispose() {
            registered = false;
            resetInput();
            for (let key in canvasEventHandlerMap) {
                if (canvasEventHandlerMap.hasOwnProperty(key)) {
                    document.removeEventListener(key, canvasEventHandlerMap[key]);
                }
            }
            canvasEventHandlerMap = null;
            for (let key in inputEventHandlerMap) {
                if (inputEventHandlerMap.hasOwnProperty(key)) {
                    api.getInput().removeEventListener(key, inputEventHandlerMap[key]);
                }
            }
            inputEventHandlerMap = null;
            document.removeEventListener('mouseout', mouseOutEventHandler);
			//api.getCanvas().removeEventListener('mouseover', mouseOverEventHandler);
        }

        let canvasEventHandlerMap = {};
        let inputEventHandlerMap = {};

        function register() {
            if (registered) {
                return;
            }
            let canvas = api.getCanvas();
            let input = api.getInput();
            resetInput();
            focusInput();

            let canvasMousedownEventHandler = function (evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
                mouseDownEventHandler(evt);
                
                let mousePos = getMousePos(canvas, evt, 'mousedown', evt.target);
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput();
                sendInput();
                
                evt.preventDefault();
                evt.stopPropagation();
                
                return false;
            };
            canvasEventHandlerMap['mousedown'] = canvasMousedownEventHandler;
            Util.bindEvent(document, 'mousedown', canvasMousedownEventHandler, false);

            let canvasDblclickEventHandler = function (evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
                var mousePos = getMousePos(canvas, evt, 'dblclick', evt.target);
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput();
                sendInput();
                
                evt.preventDefault();
                evt.stopPropagation();
                
                return false;
            };
            canvasEventHandlerMap['dblclick'] = canvasDblclickEventHandler;
            Util.bindEvent(document, 'dblclick', canvasDblclickEventHandler, false);

            let canvasMousemoveEventHandler = function (evt) {
            	if (isNotValidCanvasTarget(evt) && mouseDownCanvas == null) {
            		return;
            	}
            	
                var mousePos = getMousePos(canvas, evt, 'mousemove', mouseDownCanvas != null ? mouseDownCanvas : evt.target);
                if (prepos && prepos.mouse.x == mousePos.mouse.x && prepos.mouse.y == mousePos.mouse.y) {
                    return false;
                }

                prepos = mousePos;
                mousePos.mouse.button = mouseDown;
                latestMouseMoveEvent = mousePos;
                
                if (isNotValidCanvasTarget(evt) && mouseDownCanvas != null) {
                	// prevent firing mouse move events on underlying html components if dragging webswing component and mouse gets out of canvas bounds
                	// this can happen when you quickly move a webswing dialog window over an html element (using compositing window manager)
            		evt.preventDefault();
            		evt.stopPropagation();
            	}
                
                return false;
            };
            canvasEventHandlerMap['mousemove'] = canvasMousemoveEventHandler;
            Util.bindEvent(document, 'mousemove', canvasMousemoveEventHandler, false);

            let canvasMouseupEventHandler = function (evt) {
                if(mouseDown === 0 ){ // ignore mouseup events if the mousedown did not originate on canvas
                    return;
                }
                // do this for the whole document, not only canvas

                var mousePos = getMousePos(canvas, evt, 'mouseup', evt.target);
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                
                if (evt.target && evt.target.matches && evt.target.matches("canvas.webswing-canvas") && mouseDownCanvas != null) {
                	// focus input only in case mouse was pressed and released over canvas
                	focusInput();
            	}
                
                sendInput();
                
                mouseDown = 0;
                resetMouseDownCanvas();
                
                return false;
            };
            canvasEventHandlerMap['mouseup'] = canvasMouseupEventHandler;
            Util.bindEvent(document, 'mouseup', canvasMouseupEventHandler, false);

            // IE9, Chrome, Safari, Opera
            let canvasMouseWheelEventHandler = function (evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
                let mousePos = getMousePos(canvas, evt, 'mousewheel', evt.target);
                latestMouseMoveEvent = null;
                if (latestMouseWheelEvent != null) {
                    mousePos.mouse.wheelDelta += latestMouseWheelEvent.mouse.wheelDelta;
                }
                latestMouseWheelEvent = mousePos;
                return false;
            };
            canvasEventHandlerMap['wheel'] = canvasMouseWheelEventHandler;
            Util.bindEvent(document, "wheel", canvasMouseWheelEventHandler, false);

            // firefox
            let canvasDOMMouseScrollEventHandler = function (evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
                let mousePos = getMousePos(canvas, evt, 'mousewheel', evt.target);
                latestMouseMoveEvent = null;
                if (latestMouseWheelEvent != null) {
                    mousePos.mouse.wheelDelta += latestMouseWheelEvent.mouse.wheelDelta;
                }
                latestMouseWheelEvent = mousePos;
                return false;
            };
            canvasEventHandlerMap['DOMMouseScroll'] = canvasDOMMouseScrollEventHandler;
            Util.bindEvent(document, "DOMMouseScroll", canvasDOMMouseScrollEventHandler, false);

            let canvasContextmenuEventHandler = function (evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
                event.preventDefault();
                event.stopPropagation();
                return false;
            };
            canvasEventHandlerMap['contextmenu'] = canvasContextmenuEventHandler;
            Util.bindEvent(document, 'contextmenu', canvasContextmenuEventHandler);

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

            let keydownHandler = function(event) {
                let functionKeys=[9/*tab*/, 12/*Numpad5*/, 16/*Shift*/, 17/*ctrl*/, 18/*alt*/, 19/*pause*/, 20/*CapsLock*/, 27/*esc*/,
                                  32/*space*/, 33/*pgup*/, 34/*pgdown*/, 35/*end*/, 36/*home*/, 37/*left*/, 38/*up*/, 39/*right*/, 40/*down*/, 44/*prtscr*/,
                                  45/*insert*/, 46/*Delete*/, 91/*OSLeft*/, 92/*OSRight*/, 93/*Context*/, 145/*scrlck*/, 225/*altGraph(Linux)*/,
                                  112/*F1*/, 113/*F2*/, 114/*F3*/, 115/*F4*/, 116/*F5*/, 117/*F6*/, 118/*F7*/, 119/*F8*/, 120/*F9*/,
                                  121/*F10*/, 122/*F11*/, 123/*F12*/, 124/*F13*/, 125/*F14*/, 126/*F15*/, 127/*F16*/, 128/*F17*/, 129/*F18*/, 130/*F19*/, 131/*F20*/,
                                  132/*F21*/, 133/*F22*/, 134/*F23*/, 135/*F24*/];

            	let kc = event.keyCode;
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
                let keyevt = getKBKey('keydown', canvas, event);
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
            };
            var handleKeyDown = function(evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
            	keydownHandler(evt);
            }
            
            inputEventHandlerMap['keydown'] = keydownHandler;
            Util.bindEvent(input, 'keydown', keydownHandler, false);
            canvasEventHandlerMap['keydown'] = handleKeyDown;
			Util.bindEvent(document, 'keydown', handleKeyDown, false);
			
            let keypressHandler = function(event) {
                let keyevt = getKBKey('keypress', canvas, event);
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
            };
            var handleKeyPress = function(evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
            	keypressHandler(evt);
            }
            
            inputEventHandlerMap['keypress'] = keypressHandler;
			Util.bindEvent(input, 'keypress', keypressHandler, false);
            canvasEventHandlerMap['keypress'] = handleKeyPress;
			Util.bindEvent(document, 'keypress', handleKeyPress, false);
			
             let keyupHandler = function(event) {
                let keyevt = getKBKey('keyup', canvas, event);
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
            };
            var handleKeyUp = function(evt) {
            	if (isNotValidCanvasTarget(evt)) {
            		return;
            	}
            	
            	keyupHandler(evt);
            }
            
            inputEventHandlerMap['keyup'] = keyupHandler;
			Util.bindEvent(input, 'keyup', keyupHandler, false);
            canvasEventHandlerMap['keyup'] = handleKeyUp;
			Util.bindEvent(document, 'keyup', handleKeyUp, false);

            let DEFAULT_FONT = '14px sans-serif';
            // 中文输入法（如搜狗，智能ABC）等最终可见字符需要一连串键盘输入才能形成的，当用户开始输入字符的时候，
            // 需要将承载用户输入的input设置成可见（Z-INDEX不为-1等），以便input能回显一连串输入过程中的文字
            let inputCompositionstartEventHandler = function (event) {
                compositionInput = true;
                let input=api.getInput();
                $(input).addClass('input-ime');
                $(input).removeClass('input-hidden');
                input.style.font = DEFAULT_FONT;
            };
            inputEventHandlerMap['compositionstart'] = inputCompositionstartEventHandler;
            Util.bindEvent(input, 'compositionstart', inputCompositionstartEventHandler, false);

            // 中文输入法（如搜狗，智能ABC）等最终可见字符需要一连串键盘输入才能形成的，通过compositionend来获取一连串输入之后的最终可见字符
            let inputCompositionendEventHandler = function (event) {
                let input = api.getInput();
                $(input).addClass('input-hidden');
                $(input).removeClass('input-ime');
                input.style.width = '1px';
                // IE 11以下版本不能从event.data里获取输入的值，需要从input控件里获取用户输入的值
                let isIE = api.cfg.ieVersion && api.cfg.ieVersion <= 11;
                sentWordsUsingKeypressEvent(isIE ? event.target.value : event.data);
                compositionInput = false;
                focusInput();
            };
            inputEventHandlerMap['compositionend'] = inputCompositionendEventHandler;
            Util.bindEvent(input, 'compositionend', inputCompositionendEventHandler, false);

            // 对于IE,CHROME标点符号不能显示问题修复
            // IE,CHROME标点符号输入不会触发compositionStart compositionEnd事件，导致这些浏览器标点符号输入无法被webswing捕获到
            let inputInputEventHandler = function (event) {
                let input = api.getInput();
                // 中文输入法输入汉字的时候夹带标点符号也会触发本事件，但由compositionEnd来完成用户文本录入，
                // 通过设置compositionInput 为true，将将该过程中触发的事件忽略掉
                // input的值为空的时候（比如汉字输入为了兼容所有浏览器，统一使用compositionend事件来处理，compositionend事件处理调用了focusInput导致input为空）不需要向后台发送
                if (compositionInput || !input.value) {
                    return;
                }
                // chrome
                if (((!event.isComposing && event.inputType == 'insertText' && event.data != null)
                    // IE
                    || (api.cfg.ieVersion && event.type === "input"))
                    // IE focusInput新增空格并选中会触发input事件并走到这个分支，无效输入要过滤
                    && input.value != " "
                ) {
                    sentWordsUsingKeypressEvent(input.value)
                    focusInput();
                }
            };
            inputEventHandlerMap['input'] = inputInputEventHandler;
            Util.bindEvent(input, 'input', inputInputEventHandler, false);

            // 用户输入过程中的回显用input承载，需要用及时调整宽度
            let inputCompositionupdateEventHandler = function (event) {
                let input = api.getInput();
                let text = event.data;
                input.style.width = getTextWidth(text, DEFAULT_FONT) + 'px';
            };
            inputEventHandlerMap['compositionupdate'] = inputCompositionupdateEventHandler;
            Util.bindEvent(input, 'compositionupdate', inputCompositionupdateEventHandler, false);

            let inputCutEventHandler = function (event) {
                event.preventDefault();
                event.stopPropagation();
                api.cut(event);
                return false;
            };
            inputEventHandlerMap['cut'] = inputCutEventHandler;
            Util.bindEvent(input, 'cut', inputCutEventHandler, false);

            let inputCopyEventHandler = function (event) {
                event.preventDefault();
                event.stopPropagation();
                api.copy(event);
                return false;
            };
            inputEventHandlerMap['copy'] = inputCopyEventHandler;
            Util.bindEvent(input, 'copy', inputCopyEventHandler, false);

            let inputPasteEventHandler = function (event) {
                event.preventDefault();
                event.stopPropagation();
                api.paste(event);
                return false;
            };
            inputEventHandlerMap['paste'] = inputPasteEventHandler;
            Util.bindEvent(input, 'paste', inputPasteEventHandler, false);

            Util.bindEvent(document, 'mouseout', mouseOutEventHandler);
			//Util.bindEvent(document, 'mouseover', mouseOverEventHandler);

            registered = true;
        }

        function isWebswingCanvas(e){
            return e && e.matches && e.matches("canvas.webswing-canvas")
        }

        function setMouseDownCanvas(evt){
            mouseDownCanvas = isWebswingCanvas(evt.target) ? evt.target : null;
            if(isWebswingCanvas(evt.target)){
                $('.webswing-html-canvas iframe').addClass('webswing-iframe-muted-while-dragging');
            }
        }

        function resetMouseDownCanvas(){
            mouseDownCanvas = null;
            $('.webswing-html-canvas iframe').removeClass('webswing-iframe-muted-while-dragging');
        }

        // 获取文本宽度
	   function getTextWidth(text, font) {
            let canvas = api.getCanvas();
            let ctx = canvas.getContext("2d");
            ctx.save();
            ctx.font = font;
            let metrics = ctx.measureText(text);
            ctx.restore();
            return Math.ceil(metrics.width)+5;
        }
	   	
	   	function mouseDownEventHandler(evt) {
	   		mouseDown = mouseDown | Math.pow(2, evt.which);
	   		if (evt.which == 1) {
	   			mouseDown = 1;
	   		}
            setMouseDownCanvas(evt);
	   	}
	   
        function mouseOutEventHandler(evt) {
        	if (isWebswingCanvas(evt.target) || isWebswingCanvas(evt.relatedTarget) || mouseDownCanvas != null) {
        		return;
        	}
        	
        	mouseOutEventHandlerImpl(evt);
        }
        
        function mouseOutEventHandlerImpl(evt) {
            // canvas会铺满整个界面
            // 当鼠标移除document范围后，鼠标松开的事件需要监听在document之上。
            // 中文输入的时候，承载中文输入的input空间也会触发本事件，需忽略
            if (api.cfg.hasControl && api.cfg.canPaint && !api.cfg.mirrorMode && !compositionInput
					&& document.activeElement.getAttribute('data-id') === "input-handler") {
                let mousePos = getMousePos(api.getCanvas(), evt, 'mouseup', evt.target);
                //when an new web page pops after user click, mouseup will send twice
                mousePos.mouse.x = -1;
                mousePos.mouse.y = -1;
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput();
                sendInput();
            }
            mouseDown = 0;
            resetMouseDownCanvas();
        }
		
		/*function mouseOverEventHandler(evt) {
			if (api.cfg.hasControl && api.cfg.canPaint && !api.cfg.mirrorMode && !compositionInput) {
				var mousePos = getMousePos(api.getCanvas(), evt, 'mouseup');
                latestMouseMoveEvent = null;
                enqueueInputEvent(mousePos);
                focusInput();
                sendInput();
			}
			mouseDown = 0;
        }*/

        function isNotValidCanvasTarget(evt) {
        	return !isWebswingCanvas(evt.target)
        }
        
        function focusInput() {
        	var input = api.getInput();
            // In order to ensure that the browser will fire clipboard events, we always need to have something selected
            // scrollX , scrollY attributes on IE gives undefined, so changed to compatible pageXOffset,pageYOffset
            let sx = window.pageXOffset, sy = window.pageYOffset;
            input.value = ' ';
            // set the style attributes as the focus/select cannot work well in IE
            var temppos = input.style.position;
            var templeft = input.style.left;
            var temptop = input.style.top;
            if(Util.detectIE() && Util.detectIE() <= 11){
                input.style.position = 'fixed';
                input.style.left = '0px';
                input.style.top = '0px'
            }
            input.focus({preventScroll: true});
            input.select();
            if(Util.detectIE() && Util.detectIE() <= 11) {
                input.style.position = temppos;
                input.style.left = templeft;
                input.style.top = temptop;
            }
        }

        // 当快速输入多个中文标点的时候，一个标点发送一次拷贝事件，但是webswing里应用程序EDT处理时因为只有一个剪切板，而粘贴处理是先将内容放到剪切板里，
        // 然后出发拷贝事件，如果事件没有及时处理，导致后输入的标点在剪切板里将前面的标点给覆盖掉，所以需要用keypress事件来发送文本
        function sentWordsUsingKeypressEvent(data) {
            for (let i = 0, length = data.length; i < length ;i++) {
                inputEvtQueue.push({
                    key : {
                        type : 'keypress',
                        character : data.charCodeAt(i),
                        keycode : 0,
                    }
                });
            }
        }

        function getMousePos(canvas, evt, type, targetElement) {
        	var rect;
        	if (targetElement && targetElement != null && targetElement.parentNode && targetElement.parentNode != null && targetElement.parentNode.getBoundingClientRect) {
                if ($(targetElement).is(".internal")) {
                	rect = targetElement.parentNode.parentNode.getBoundingClientRect();
                } else {
                	rect = targetElement.parentNode.getBoundingClientRect();
                }
        	} else {
        		rect = api.getCanvas().getBoundingClientRect();
        	}         
        	            
        	var winId;
        	if (targetElement && targetElement.matches("canvas.webswing-canvas") && $(targetElement).data("id") && $(targetElement).data("id") != "canvas") {
        		// for a composition canvas window send winId
            	if ($(targetElement).is(".internal")) {
            		// internal window must use its parent as mouse events target
            		if ($(targetElement.parentNode).data("ownerid")) {
            			winId = $(targetElement.parentNode).data("ownerid");
            		}
            	} else {
            		winId = $(targetElement).data("id");
            	}
        	}
        	
            let mouseX = 0;
            let mouseY = 0;

            mouseX = Math.round(evt.clientX - rect.left);
            mouseY = Math.round(evt.clientY - rect.top);

            let delta = 0;
            if (type == 'mousewheel') {
                if (Util.detectFF()) {
                    delta = -Math.max(-1, Math.min(1, (-evt.deltaY * 100)));
                } else if (Util.detectIE() <= 11) {
                    delta = -Math.max(-1, Math.min(1, (-evt.deltaY)));
                } else {
                    delta = -Math.max(-1, Math.min(1, (evt.wheelDelta || -evt.detail)));
                }
            }
            
            if (type == 'mouseup' && (!targetElement || !targetElement.matches || !targetElement.matches("canvas.webswing-canvas"))) {
            	// fix for detached composition canvas windows that might overlay same coordinates space, when clicked outside a canvas
            	mouseX = -1;
            	mouseY = -1;
            }
            
            if (type == 'mouseup' && targetElement && targetElement.matches && !targetElement.matches("canvas.webswing-canvas") && targetElement.closest(".webswing-html-canvas") != null) {
            	// fix for mouseup over an HtmlWindow div content
            	rect = targetElement.closest(".webswing-html-canvas").parentNode.getBoundingClientRect();
            	            	
            	mouseX = Math.round(evt.clientX - rect.left);
            	mouseY = Math.round(evt.clientY - rect.top);
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
                    meta : evt.metaKey,
                    winId: winId || ""
                }
            };
        }

        function getKBKey(type, canvas, evt) {
            let char = evt.which;
            if (char == 0 && evt.key != null) {
                char = evt.key.charCodeAt(0);
            }
            let kk = evt.keyCode;
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

    }
}
