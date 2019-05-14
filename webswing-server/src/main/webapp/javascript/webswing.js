import $ from 'jquery';
import css from './templates/base.css';
import Util from './webswing-util';
import Polyfill from './webswing-polyfill';
import Base from './webswing-base';
import Socket from './webswing-socket';
import Files from './webswing-files';
import Dialog from './webswing-dialog';
import Selector from './webswing-selector';
import Login from './webswing-login';
import Canvas from './webswing-canvas';
import Identity from './webswing-identity';
import JsLink from './webswing-jslink';
import Clipboard from './webswing-clipboard';
import Playback from './webswing-playback';
import Input from './webswing-input';
import Touch from './webswing-touch';
import Injector from './webswing-inject';

export default class Webswing {
	constructor() {          
            let style = $("<style></style>", {
                type : "text/css"
            });
            style.text(css);
            $("head").prepend(style);

            let globalName = $('[data-webswing-global-var]');
            let global = {};
            let typedArraysSupported = Polyfill.polyfill();
            if (globalName != null && globalName.length !== 0) {
                let name = globalName.data('webswingGlobalVar');
                global = window[name] = {
                    scan : scanForInstances,
                    bootstrap : bootstrap
                };
                scanForInstances(window[name]);
            } else {
                scanForInstances(window);
            }

            function scanForInstances(root) {
                root = root != null ? root : global;
                let result = {};
                let instances = $('[data-webswing-instance]');
                instances.each(function(index, instance) {
                    let id = $(instance).data('webswingInstance');
                    let active = $(instance).data('webswingActive');
                    if (!active) {
                        let wsInstance = bootstrap($(instance));
                        $(instance).attr('data-webswing-active', 'true');
                        if (id != null) {
                            result[id] = wsInstance;
                        }
                    }
                });
                for ( let exportName in result) {
                    root[exportName] = result[exportName];
                }
            }

            function bootstrap(element, customization) {
                var injector = new Injector();
                injector.module('webswing', new WebswingInstance(element));
                injector.module('dialog', new Dialog());
                injector.module('canvas', new Canvas());
                injector.module('base', new Base());
                injector.module('input', new Input());
                injector.module('touch', new Touch());
                injector.module('socket', new Socket());
                injector.module('files', new Files());
                injector.module('selector', new Selector());
                injector.module('login', new Login());
                injector.module('identity', new Identity());
                injector.module('jslink', new JsLink());
                injector.module('clipboard', new Clipboard());
                injector.module('playback', new Playback());
                let externalObj = {
                    start : 'webswing.start',
                    disconnect : 'webswing.disconnect',
                    configure : 'webswing.configure',
                    kill : 'base.kill',
                    setControl : 'webswing.setControl'
                };
                injector.module('external', {
                    provides : externalObj,
                    injects : externalObj
                });

                if (customization != null) {
                    customization(injector);
                }
                injector.injectAndVerify();

                return externalObj;
            }

            function WebswingInstance(rootElement) {
                let module = this;
                let api;
                let clientCallbackHandler = false;
                module.injects = api = {
                    cfg : 'webswing.config',
                    start : 'webswing.start',
                    disconnect : 'webswing.disconnect',
                    login : 'login.login',
                    connect : 'socket.connect',
                    showDialog : 'dialog.show',
                    readyDialog : 'dialog.content.readyDialog',
                    initializingDialog : 'dialog.content.initializingDialog',
                    disposeIdentity : 'identity.dispose',
                    disposeBase : 'base.dispose',
                    disposeCanvas : 'canvas.dispose',
                    disposeSocket : 'socket.dispose',
                    disposeInput : 'input.dispose',
                    disposeTouch : 'touch.dispose',
                    disposeFileDialog : 'files.close',
                    disposeCopyBar : 'clipboard.dispose',
                    disposeJslink: 'jslink.dispose',
                    showPlaybackControls : 'playback.showControls'
                };
                module.provides = {
                    config : defaultCtxConfig(),
                    start : start,
                    newSession : newSession,
                    reTrySession : reTrySession,
                    disconnect : disconnect,
                    setControl : setControl,
                    configure : configure,
                    setCallBack: setCallBack,
                    fireCallBack: fireCallBack
                };
                module.ready = function() {
                    configure();
                    if (api.cfg.autoStart) {
                        api.start();
                    } else {
                        api.showDialog(api.readyDialog);
                    }
                };

                function defaultCtxConfig() {
                    return {
                        rootElement : setupRootElement(rootElement),
                        autoStart : false,
                        applicationName : null,
                        args : null,
                        anonym : false,
                        connectionUrl : location.origin + location.pathname,
                        mirror : false,
                        recordingPlayback : false,
                        typedArraysSupported : typedArraysSupported,
                        binarySocket : true,
                        recording : false,
                        debugPort : null,
                        javaCallTimeout : 3000,
                        documentBase : document.location.origin + document.location.pathname,
                        ieVersion : Util.detectIE(),
                        /* webswing instance context */
                        clientId : null,
                        viewId : null,
                        appName : null,
                        hasControl : false,
                        mirrorMode : false,
                        canPaint : false,
                        applet : false,
                        virtualKB :false
                    };
                }

                function setupRootElement(rootElement) {
                   return rootElement.addClass('webswing-root');
                }

                function setCallBack(callback) {
                    clientCallbackHandler = callback;
                }

                function fireCallBack(event) {
                    if (clientCallbackHandler) {
                        return clientCallbackHandler(event);
                    } else {
                        console.info('webswing callback is false, fire event is', event);
                    }
                }

                function start() {
                    api.login(function() {
                        api.showDialog(api.initializingDialog);
                        api.connect();
                    }, clientCallbackHandler);
                }

                function newSession() {
                    api.disconnect();
                    api.disposeIdentity();
                    api.start();
                }
                
                function reTrySession() {
                    api.disconnect();
                    api.start();
                }

                function disconnect() {
                    api.disposeFileDialog();
                    api.disposeBase();
            api.disposeInput();
            api.disposeTouch();
                    api.disposeCanvas();
                    api.disposeSocket();
                    api.disposeCopyBar();
            api.disposeJslink();
                }

                function configure(options, appletParams) {
                    let cfg = api.cfg;
                    options = options != null ? options : readOptions(cfg.rootElement);
                    if (options != null) {
                        cfg.autoStart = options.autoStart != null ? JSON.parse(options.autoStart) : cfg.autoStart;
                        cfg.applicationName = options.applicationName != null ? options.applicationName : cfg.applicationName;
                        cfg.args = options.args != null ? options.args : cfg.args;
                        cfg.anonym = options.anonym != null ? JSON.parse(options.anonym) : cfg.anonym;
                        cfg.binarySocket = options.binarySocket != null ? JSON.parse(options.binarySocket) : cfg.binarySocket;
                        cfg.recording = options.recording != null ? JSON.parse(options.recording) : cfg.recording;
                        cfg.clientId = options.clientId != null ? options.clientId : cfg.clientId;
                        cfg.mirror = options.mirrorMode != null ? JSON.parse(options.mirrorMode) : cfg.mirror;
                        cfg.connectionUrl = options.connectionUrl != null ? options.connectionUrl : cfg.connectionUrl;
                        cfg.debugPort = options.debugPort != null ? options.debugPort : cfg.debugPort;
                        cfg.javaCallTimeout = options.javaCallTimeout != null ? parseInt(options.javaCallTimeout, 10) : cfg.javaCallTimeout;
                        if (cfg.connectionUrl.substr(cfg.connectionUrl.length - 1) !== '/') {
                            cfg.connectionUrl = cfg.connectionUrl + '/';
                        }
                        if (options.recordingPlayback != null) {
                            cfg.recordingPlayback = cfg.applicationName = options.recordingPlayback;
                            api.showPlaybackControls();
                        }
                    }
                    appletParams = appletParams != null ? appletParams : readAppletParams(cfg.rootElement);
                    if (appletParams != null) {
                        cfg.params = [];
                        for ( let prop in appletParams) {
                            if (typeof appletParams[prop] === 'string') {
                                cfg.params.push({
                                    name : prop,
                                    value : appletParams[prop]
                                });
                            }
                        }
                    }
                }

                function setControl(value) {
                    api.cfg.hasControl = value ? true : false;
                }

                function readOptions(element) {
                    let options = element.data('webswingOptions');
                    if (typeof options !== 'object') {
                        try {
                            options = eval("(function(){return " + options + ";})()");
                        } catch (e) {
                            throw Error("Configuration of webswing instance is not a valid json object. " + options);
                        }
                    }
                    return options;
                }

                function readAppletParams(element) {
                    let result = {};
                    let params = $(element).find('webswing-param');
                    for ( let i = 0; i < params.length; i++) {
                        let name = params[i].getAttribute('name');
                        let val = params[i].getAttribute('value');
                        if (name != null) {
                            result[name] = val;
                        }
                    }
                    return result;
                }

            }
        }
}
