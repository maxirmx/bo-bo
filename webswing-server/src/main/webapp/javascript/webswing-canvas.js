import Util from './webswing-util';
export default class CanvasModule {
	constructor() {
        let module = this;
        let api;
        module.injects = api = {
            cfg: 'webswing.config',
            sendHandshake: 'base.handshake'
        };
        module.provides = {
            dispose: dispose,
            get: get,
            getInput: getInput,
            getDesktopSize: getDesktopSize,
            translateBrowserPoint: translateBrowserPoint, // translate point from browser coordinate system to desktop CS based on api.cfg.rootElement location representing [0,0]
            translateDesktopPoint: translateDesktopPoint // translate point from desktop coordinate system to browser CS based on document root location representing [0,0] and relative element
        };

        let canvas;
        let inputHandler;
        let resizeCheck;
        var lastRootWidth = 0;
        var lastRootHeight = 0;

        function create() {
            if (canvas == null) {
                var dpr = Util.dpr();
                api.cfg.rootElement.append('<canvas data-id="canvas" style="display:block; width:' + width() + 'px;height:' + height() + 'px;" width="' + width() * dpr + '" height="' + height() * dpr + '" tabindex="-1"/>');
                api.cfg.rootElement.append('<input data-id="input-handler" class="input-hidden" type="text" value="" />');
                canvas = api.cfg.rootElement.find('canvas[data-id="canvas"]');
                canvas.addClass("webswing-canvas");
                inputHandler = api.cfg.rootElement.find('input[data-id="input-handler"]');
                lastRootWidth = width();
                lastRootHeight = height();
            }
            if (resizeCheck == null) {
                resizeCheck = setInterval(function () {
                    dpr = Util.dpr();
                    // 只有当当前页是激活态且非mirror模式下，才发送handshake告知尺寸变化，否则会导致非激活态的tab页变成激活态，而激活态的tab变成stolen
                    if (api.cfg.canPaint && !api.cfg.mirror && (canvas.width() !== width() || canvas.height() !== height())) {
                    	if (api.cfg.rootElement.is(".composition")) {
                    		// when using compositing window manager, the root canvas has 0 size
                    		// we need to do a handshake only if the root element has changed size
                    		if (lastRootWidth !== width() || lastRootHeight !== height()) {
                    			lastRootWidth = width();
                    			lastRootHeight = height();
                    			api.sendHandshake();
                    		}
                    	} else {
	                        let theCanvas = canvas[0];
	                        let snapshot = theCanvas.getContext("2d").getImageData(0, 0, theCanvas.width, theCanvas.height);
	                        let widthTmp = width();
	                        let heightTmp = height();
	                        theCanvas.width = widthTmp * dpr;
	                        theCanvas.height = heightTmp * dpr;
	                        theCanvas.style.width = widthTmp + 'px';
	                        theCanvas.style.height = heightTmp + 'px';
	                        theCanvas.getContext("2d").putImageData(snapshot, 0, 0);
	                        api.sendHandshake();
	                        if (typeof(CollectGarbage) == "function") {
	                            CollectGarbage();
	                        }
                    	}
                    }
                }, 500);
            }
        }

        function dispose() {
            if (canvas != null) {
            	api.cfg.rootElement.find('canvas').remove();
            	$(".webswing-canvas, .webswing-html-canvas").remove();
                inputHandler.remove();
                canvas = null;
                inputHandler = null;
            }
            if (resizeCheck != null) {
                clearInterval(resizeCheck);
                resizeCheck = null;
            }
        }

        function width() {
            return api.cfg.rootElement.width();
        }

        function height() {
            return api.cfg.rootElement.height();
        }

        function get() {
            if (canvas == null || resizeCheck != null) {
                create();
            }
            return canvas[0];
        }

        function getInput() {
            if (inputHandler == null) {
                create();
            }
            return inputHandler[0];
        }
        
        function getDesktopSize() {
        	if (api.cfg.rootElement.is(".composition")) {
        		return {width: width(), height: height()};
        	}
        	return {width: get().offsetWidth, height: get().offsetHeight};
        }
        
        function translateBrowserPoint(x, y) {
        	var rect = api.cfg.rootElement[0].getBoundingClientRect();
        	
        	var result = {
        			x: x - rect.left, 
        			y: y - rect.top
        	};
        	
        	return result;
        }
        
        function translateDesktopPoint(x, y, relativeElement) {
        	var rectRoot = api.cfg.rootElement[0].getBoundingClientRect();
        	var rectRel = relativeElement.getBoundingClientRect();
        	
        	var result = {
        			x: (x + rectRoot.left + api.cfg.rootElement[0].scrollLeft) - (rectRel.left - relativeElement.scrollLeft), 
        			y: (y + rectRoot.top + api.cfg.rootElement[0].scrollTop) - (rectRel.top - relativeElement.scrollTop)
        	};
        	
        	return result;
        }
	
    }
}