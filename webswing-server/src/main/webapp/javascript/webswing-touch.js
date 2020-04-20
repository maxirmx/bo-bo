import $ from 'jquery';
import html from './templates/touch.html';
import css from './templates/touch.css';
import Util from './webswing-util';

export default class TouchModule {
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
            getInput : 'canvas.getInput',
            getCanvas : 'canvas.get'
        };
        module.provides = {
            register : register,
            dispose : dispose
        };
        module.ready = function() {
        };

        let touchBar;
        let compositionText = "";
        let composition=false;
        var tapStarted = 0;
        var longPressTimeout = null;
        var tapDelayThreshold = 750;

        function register() {
            // prevent ghost mouse events to be fired
            Util.preventGhosts(api.cfg.rootElement);

            document.addEventListener("touchstart", function(evt) {
            	if (!evt.target || !evt.target.matches("canvas")) {
            		return;
            	}
            	handleStart(evt);
            }, {passive: false});
            document.addEventListener("touchend", function(evt) {
            	if (!evt.target || !evt.target.matches("canvas")) {
            		return;
            	}
            	handleEnd(evt);
            }, {passive: false});
            document.addEventListener("touchcancel", function(evt) {
            	if (!evt.target || !evt.target.matches("canvas")) {
            		return;
            	}
            	handleCancel(evt);
            }, {passive: false});
            document.addEventListener("touchleave", function(evt) {
            	if (!evt.target || !evt.target.matches("canvas")) {
            		return;
            	}
            	handleEnd(evt);
            }, {passive: false});
        }
        
        function handleStart(evt) {
        	if (longPressTimeout != null) {
        		clearTimeout(longPressTimeout);
        		longPressTimeout = null;
        	}
        	
        	var touches = evt.changedTouches;
        	   
        	if (evt.touches.length == 1) {
        		tapStarted = evt.timeStamp;
        		var tx = touches[0].clientX;
        		var ty = touches[0].clientY;
        		
        		longPressTimeout = setTimeout(function() {
        			// handle long press
        			longPressTimeout = null;
        			tapStarted = 0;
        			
        			var eventMsg = [];
        			eventMsg.push(createMouseEvent(evt.target, 'mousedown', tx, ty, 3));
        			eventMsg.push(createMouseEvent(evt.target, 'mouseup', tx, ty, 3));
        			
        			api.send({events: eventMsg});
        			evt.target.focus({preventScroll: true});
        		}, tapDelayThreshold);
        	} else {
        		// cancel tap and long press
        		tapStarted = 0;
        		
        		if (longPressTimeout != null) {
            		clearTimeout(longPressTimeout);
            		longPressTimeout = null;
            	}
        	}
        }
        
        function handleEnd(evt) {
        	if (longPressTimeout != null) {
        		clearTimeout(longPressTimeout);
        		longPressTimeout = null;
        	}
        	
        	var touches = evt.changedTouches;
        	
        	if (tapStarted > 0) {
        		var duration = evt.timeStamp - tapStarted;
        		
        		if (duration <= tapDelayThreshold) {
        			var x = touches[0].clientX;
        			var y = touches[0].clientY;
        			
        			// tap
        			var eventMsg = [];
        			eventMsg.push(createMouseEvent(evt.target, 'mousedown', x, y, 1));
        			eventMsg.push(createMouseEvent(evt.target, 'mouseup', x, y, 1));
        			
        			api.send({events: eventMsg});
        			
        			display();
        		} else {
        			// long press, already handled by timer
        		}
        	}
        }

        function handleCancel(evt) {
        	if (longPressTimeout != null) {
        		clearTimeout(longPressTimeout);
        		longPressTimeout = null;
        	}
        }
        
        function createMouseEvent(relatedCanvas, type, x, y, button) {
        	var rect = relatedCanvas.getBoundingClientRect();
        	
            return {
                mouse: {
                    x: Math.round(x - rect.left),
                    y: Math.round(y - rect.top),
                    type: type,
                    button: button
                }
            }
        }
        
        function focusInput(input) {
            // In order to ensure that the browser will fire clipboard events, we always need to have something selected
            input.value = ' ';
            input.focus();
            input.select();
        }

        function getTouchPos(canvas, evt, button) {
            let rect = canvas.getBoundingClientRect();
            // return relative mouse position
            let mouseX = Math.round(evt.center.x - rect.left);
            let mouseY = Math.round(evt.center.y - rect.top);

            return {
                events : [ mouseEvent('mousedown'), mouseEvent('mouseup') ]
            };

            function mouseEvent(type) {
                return {
                    mouse : {
                        x : mouseX,
                        y : mouseY,
                        type : type,
                        button : button,
                    }
                };
            }
        }

        function display() {
            if (touchBar != null) {
                close();
            }
            api.cfg.rootElement.append(html);
            touchBar = api.cfg.rootElement.find('div[data-id="touchBar"]');
            touchBar.find('button[data-id="kbdBtn"]').on('click', function(evt) {
                api.cfg.virtualKB = true;
                focusInput(api.getInput());
                close();
                $(api.getInput()).on('input compositionstart compositionupdate compositionend', processCompositionEvent);
                $(api.getInput()).on('blur', function(evt) {
                    api.cfg.virtualKB = false;
                    $(api.getInput()).off('compositionstart compositionupdate compositionend');
                });
            });
            touchBar.show("fast");
        }

        function processCompositionEvent(evt) {
            if (evt.type === 'compositionstart') {
                compositionText = evt.originalEvent.data;
                composition=true;
            }
            if (evt.type === 'compositionupdate' || evt.type === 'compositionend') {
                let newText = evt.originalEvent.data;
                if (newText.indexOf(compositionText) == 0) {
                    sendString(newText.substring(compositionText.length));
                } else {
                    sendBackspace(compositionText.length);
                    sendString(newText);
                }
                compositionText = newText;
            }
            if (evt.type === 'compositionend') {
                compositionText = "";
                setTimeout(function() {
                    evt.currentTarget.value = "";
                    composition=false;
                }, 0)
            }
            if(evt.type === 'input'){
                if(!composition){
                    let newText =evt.originalEvent.target.value;
                    sendString(newText);
                    evt.currentTarget.value = "";
                }
            }
        }

        function close() {
            if (touchBar != null) {
                touchBar.hide("fast");
                touchBar.remove();
                touchBar = null;
            }
        }

        function sendString(s) {
            for ( let i = 0, len = s.length; i < len; i++) {
                let char = 0;
                char = s.charCodeAt(i);
                api.send({
                    events : [ keyEvent('keydown', char), keyEvent('keypress', char), keyEvent('keyup', char) ]
                });
            }
        }

        function sendBackspace(no) {
            let evts = [];
            for ( let i = 0; i < no; i++) {
                evts.push(keyEvent("keydown", 8, 8));
                evts.push(keyEvent("keyup", 8, 8));
            }
            api.send({
                events : evts
            });

        }

        function keyEvent(type, char, kkode) {
            return {
                key : {
                    type : type,
                    character : char,
                    keycode : kkode != null ? kkode : 0,
                    alt : false,
                    ctrl : false,
                    shift : false,
                    meta : false
                }
            };
        }

        function dispose() {
            close();
        }
    }
}