define(['webswing-util'], function amdFactory(util) {
    "use strict";
    return function CanvasModule() {
        var module = this;
        var api;
        module.injects = api = {
            cfg: 'webswing.config',
            sendHandshake: 'base.handshake'
        };
        module.provides = {
            dispose: dispose,
            get: get,
            getInput: getInput
        };

        var canvas;
        var inputHandler;
        var resizeCheck;

        function create() {
            if (canvas == null) {
                var dpr = util.dpr();
                api.cfg.rootElement.append('<canvas data-id="canvas" style="display:block; width:' + width() + 'px;height:' + height() + 'px;" width="' + width() * dpr + '" height="' + height() * dpr + '" tabindex="-1"/>');
                api.cfg.rootElement.append('<input data-id="input-handler" class="input-hidden" type="text" value="" />');
                canvas = api.cfg.rootElement.find('canvas[data-id="canvas"]');
                inputHandler = api.cfg.rootElement.find('input[data-id="input-handler"]');
            }
            if (resizeCheck == null) {
                resizeCheck = setInterval(function () {
                	dpr = util.dpr();
                    // 只有当当前页是激活态且非mirror模式下，才发送handshake告知尺寸变化，否则会导致非激活态的tab页变成激活态，而激活态的tab变成stolen
                    if (api.cfg.canPaint && !api.cfg.mirror && (canvas.width() !== width() || canvas.height() !== height())) {
                        var theCanvas = canvas[0];
                        var snapshot = theCanvas.getContext("2d").getImageData(0, 0, theCanvas.width, theCanvas.height);
                        var widthTmp = width();
                        var heightTmp = height();
                        theCanvas.width = widthTmp * dpr;
                        theCanvas.height = heightTmp * dpr;
                        theCanvas.style.width = widthTmp + 'px';
                        theCanvas.style.height = heightTmp + 'px';
                        theCanvas.getContext("2d").putImageData(snapshot, 0, 0);
                        api.sendHandshake();
                    }
                }, 500);
            }
        }

        function dispose() {
            if (canvas != null) {
                canvas.remove();
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
    };

});