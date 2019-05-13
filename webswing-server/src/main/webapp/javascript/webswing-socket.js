const atmosphere = require('atmosphere.js');
const ProtoBuf = require('protobufjs');
import wsProto from './webswing.proto';

export default class SocketModule {
	    constructor() {
        let module = this;
        let api;
        module.injects = api = {
            cfg: 'webswing.config',
            processMessage: 'base.processMessage',
            showDialog: 'dialog.show',
            hideDialog: 'dialog.hide',
            currentDialog: 'dialog.current',
            stoppedDialog: 'dialog.content.stoppedDialog',
            disconnectedDialog: 'dialog.content.disconnectedDialog',
            connectionErrorDialog: 'dialog.content.connectionErrorDialog',
            initializingDialog: 'dialog.content.initializingDialog',
            continueSession: 'base.continueSession',
            fireCallBack : 'webswing.fireCallBack'
        };
        module.provides = {
            connect: connect,
            send: send,
            uuid: getuuid,
            awaitResponse: awaitResponse,
            dispose: dispose
        };
        module.ready = function () {
            binary = api.cfg.typedArraysSupported && api.cfg.binarySocket;
            document.addEventListener('visibilitychange', focusToActive);
        };

        function focusToActive() {
            if (document.visibilityState === 'visible' && !api.cfg.canPaint && !api.cfg.mirrorMode && api.currentDialog() !== api.stoppedDialog) {
                api.continueSession();
            }
        }


        let socket, uuid, binary;
        let responseHandlers = {};

        function connect() {
            let request = {
                url: api.cfg.connectionUrl + 'async/swing',
                contentType: "application/json",
                // logLevel : 'debug',
                transport: 'websocket',
                trackMessageLength: true,
                reconnectInterval: 5000,
                fallbackTransport: 'long-polling',
                enableXDR: true,
                headers: {}
            };

            if (binary) {
                request.url = request.url + '-bin';
                request.headers['X-Atmosphere-Binary'] = true;
                request.enableProtocol = false;
                request.trackMessageLength = false;
                request.contentType = 'application/octet-stream';
                request.webSocketBinaryType = 'arraybuffer';
            }

            if (api.cfg.recordingPlayback) {
                request.url =  api.cfg.connectionUrl + 'async/swing-play';
                request.headers['file'] = api.cfg.recordingPlayback;
            }

            if (api.cfg.args != null) {
                request.headers['X-webswing-args'] = api.cfg.args;
            }
            if (api.cfg.recording != null) {
                request.headers['X-webswing-recording'] = api.cfg.recording;
            }
            if (api.cfg.debugPort != null) {
                request.headers['X-webswing-debugPort'] = api.cfg.debugPort;
            }

            request.onOpen = function (response) {
                if (response.transport !== 'websocket' && binary) {
                    console.error('Webswing: Binary encoding not supported for ' + response.transport + ' transport. Falling back to json encoding.');
                    api.cfg.binarySocket = false;
                    binary = false;
                    dispose();
                    connect();
                } else {
                    api.fireCallBack({type: 'webswingWebSocketOpened'});
                }
            };

            request.onReopen = function (response) {
                api.hideDialog();
                api.fireCallBack({type: 'webswingWebSocketReOpened'});
            };

            request.onMessage = function (response) {
                try {
                    let data = decodeResponse(response);
                    if (data.sessionId != null) {
                        uuid = data.sessionId;
                    }
                    // javascript2java response handling
                    if (data.javaResponse != null && data.javaResponse.correlationId != null) {
                        let correlationId = data.javaResponse.correlationId;
                        if (responseHandlers[correlationId] != null) {
                            let callback = responseHandlers[correlationId];
                            delete responseHandlers[correlationId];
                            callback(data.javaResponse);
                        }
                    }
                    api.processMessage(data);
                } catch (e) {
                    console.error(e);
                    return;
                }
            };

            request.onClose = function (response) {
                if (api.currentDialog() !== api.stoppedDialog) {
                    api.showDialog(api.disconnectedDialog);
                    api.fireCallBack({type: 'webswingWebSocketOnClose'});
                }
            };

            request.onError = function (response) {
                api.fireCallBack({type: 'webswingWebSocketOnError'});
                api.showDialog(api.connectionErrorDialog);
            };

            socket = atmosphere.subscribe(request);
        }

        function decodeResponse(response) {
            let message = response.responseBody;
            let data;
            if (binary) {
                if (message.byteLength === 1) {
                    return {};// ignore atmosphere heartbeat
                }
                data = SocketModule.AppFrameMsgOutProto.decode(message);
                explodeEnumNames(data);
            } else {
                data = atmosphere.util.parseJSON(message);
            }
            return data;
        }

        function dispose() {
            atmosphere.unsubscribe(socket);
            document.removeEventListener('visibilitychange', focusToActive);
            socket = null;
            uuid = null;
            if (awaitResponseTimeoutId) {
                clearTimeout(awaitResponseTimeoutId);
            }
            for (var key in responseHandlers) {
                if (responseHandlers.hasOwnProperty(key)) {
                    responseHandlers[key] = null;
                }
            }
            responseHandlers = null;
        }

        function send(message) {
            if (socket != null && socket.request.isOpen && !socket.request.closed) {
                if (typeof message === "object") {
                    if (binary) {
                        let msg = new SocketModule.InputEventsFrameMsgInProto(message);
                        socket.push(msg.encode().toArrayBuffer());
                    } else {
                        socket.push(atmosphere.util.stringifyJSON(message));
                    }
                } else {
                    console.log("message is not an object " + message);
                }
            }
        }

        let awaitResponseTimeoutId = null;

        function awaitResponse(callback, request, correlationId, timeout) {
            send(request);
            responseHandlers[correlationId] = callback;
            awaitResponseTimeoutId = setTimeout(function () {
                if (responseHandlers[correlationId] != null) {
                    delete responseHandlers[correlationId];
                    callback(new Error("Java call timed out after " + timeout + " ms."));
                }
            }, timeout);
        }

        function getuuid() {
            return uuid;
        }

        function explodeEnumNames(data) {
            if (data != null) {
                if (Array.isArray(data)) {
                    data.forEach(function (d) {
                        explodeEnumNames(d);
                    });
                } else {
                    data.$type._fields.forEach(function (field) {
                        if (field.resolvedType != null) {
                            if (field.resolvedType.className === "Enum") {
                                let enm = field.resolvedType.object;
                                for (let key in enm) {
                                    if (enm[key] === data[field.name]) {
                                        data[field.name] = key;
                                    }
                                }
                            } else if (field.resolvedType.className === "Message") {
                                explodeEnumNames(data[field.name]);
                            }
                        }
                    });
                }
            }
        }
    }
}

SocketModule.proto = ProtoBuf.loadProto(wsProto, "webswing.proto");
SocketModule.InputEventsFrameMsgInProto = SocketModule.proto.build("org.webswing.server.model.proto.InputEventsFrameMsgInProto");
SocketModule.AppFrameMsgOutProto = SocketModule.proto.build("org.webswing.server.model.proto.AppFrameMsgOutProto");
