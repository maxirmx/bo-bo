export default class JsLinkModule {
	constructor() {
        let module = this;
        let api;
        module.injects = api = {
            cfg: 'webswing.config',
            external: 'external',
            send: 'socket.send',
            awaitResponse: 'socket.awaitResponse',
            fireCallBack : 'webswing.fireCallBack'
        };
        module.provides = {
            process: process,
            dispose: dispose
        };
        module.ready = function () {
            referenceCache['instanceObject'] = api.external;
        };

        let idMemberName = '__webswing_jslink_id';
        let referenceCache = {};

        function dispose() {
            for (let key in referenceCache) {
                if (referenceCache.hasOwnProperty(key)) {
                    referenceCache[key] = null;
                }
            }
            referenceCache = {};
            referenceCache['instanceObject'] = api.external;
            window['listener'] = null;
            delete window['listener'];
        }

        function process(jsRequest) {
            let response;
            try {
                if (jsRequest.type === 'eval') {
                    try {
                        let indirectEval = eval;
                        let result = indirectEval(jsRequest.evalString);
                        response = buildResponse(result, jsRequest.correlationId);
                    } catch(e) {
                        api.fireCallBack({type: 'sendFromJavaClient', value: jsRequest.evalString});
                        response = buildResponse(null, jsRequest.correlationId);
                    }
                } else if (jsRequest.type === 'call') {
                    let ref = jsRequest.thisObjectId != null ? referenceCache[jsRequest.thisObjectId] : window;
                    let args = decodeParams(jsRequest);
					if(ref[jsRequest.evalString] === undefined || ref[jsRequest.evalString] === null)
					{
					    response = buildResponse(null, jsRequest.correlationId);  	
					}
                    else 
					{						
						var result = ref[jsRequest.evalString].apply(ref, args);
						response = buildResponse(result, jsRequest.correlationId);
					}
                } else if (jsRequest.type === 'setMember' || jsRequest.type === 'setSlot') {
                    let ref = jsRequest.thisObjectId != null ? referenceCache[jsRequest.thisObjectId] : window;
                    let args = decodeParams(jsRequest);
                    ref[jsRequest.evalString] = args != null ? args[0] : null;
                    if (jsRequest.evalString === 'listener' && ref[jsRequest.evalString] != null) {
                        api.fireCallBack({type: 'javaClientObjectId', value: ref[jsRequest.evalString].id});
                    }
                    response = buildResponse(null, jsRequest.correlationId);
                } else if (jsRequest.type === 'getMember' || jsRequest.type === 'getSlot') {
                    let ref = jsRequest.thisObjectId != null ? referenceCache[jsRequest.thisObjectId] : window;
                    response = buildResponse(ref[jsRequest.evalString], jsRequest.correlationId);
                } else if (jsRequest.type === 'deleteMember') {
                    let ref = jsRequest.thisObjectId != null ? referenceCache[jsRequest.thisObjectId] : window;
                    delete ref[jsRequest.evalString];
                    response = buildResponse(null, jsRequest.correlationId);
                }
                if (jsRequest.garbageIds != null) {
                    jsRequest.garbageIds.forEach(function (id) {
                        delete referenceCache[id];
                    });
                }
                api.send(response);
            } catch (e) {
                response = buildResponse(e, jsRequest.correlationId);
                api.send(response);
                throw e;
            }
        }

        function buildResponse(obj, correlationId) {
            let result = {
                jsResponse: serializeObject(obj)
            };
            result.jsResponse.correlationId = correlationId;
            return result;
        }

        function serializeObject(object) {
            let jsResponse = {};
            if (object == null) {
                return jsResponse;
            } else if (Object.prototype.toString.call(object) === '[object Error]') {
                jsResponse.error = object.toString();
                return jsResponse;
            } else {
                jsResponse.value = {};
                if (typeof object === 'number' || typeof object === 'string' || typeof object === 'boolean') {
                    jsResponse.value.primitive = JSON.stringify(object);
                } else if (object instanceof JavaObjectRef) {
                    jsResponse.value.javaObject = {
                        id: object.id
                    };
                } else {
                    if (object[idMemberName] == null) {
                        object[idMemberName] = GUID();
                        referenceCache[object[idMemberName]] = object;
                    }
                    jsResponse.value.jsObject = {
                        id: object[idMemberName]
                    };
                }
                return jsResponse;
            }
        }

        function decodeParams(jsRequest) {
            let result = [];
            let args = jsRequest.params;
            if (args != null) {
                for (let i = 0; i < args.length; i++) {
                    result.push(decodeJsParam(args[i]));
                }
            }
            return result;
        }

        function decodeJsParam(param) {
            if (param.primitive != null) {
                return JSON.parse(param.primitive);
            } else if (param.jsObject != null) {
                return referenceCache[param.jsObject.id];
            } else if (param.javaObject != null) {
                return new JavaObjectRef(param.javaObject);
            } else if (param.array != null) {
                let array = [];
                for (let j = 0; j < param.array.length; j++) {
                    array.push(decodeJsParam(param.array[j]));
                }
                return array;
            }
            return null;
        }

        function GUID() {
            let S4 = function () {
                return Math.floor(Math.random() * 0x10000).toString(16);
            };
            return (S4() + S4() + S4());
        }

        function JavaObjectRef(javaRefMsg) {
            this.id = javaRefMsg.id;
            if (javaRefMsg.methods != null) {
                for (let i = 0; i < javaRefMsg.methods.length; i++) {
                    let methodName = javaRefMsg.methods[i];
                    this[methodName] = function (m) {
                        return function () {
                            let currentArguments = arguments;
                            return new Promise(function (resolve, reject) {
                                let jCorrelationId = GUID();
                                let params = [];
                                for (let i = 0; i < currentArguments.length; i++) {
                                    let serializedObject = serializeObject(currentArguments[i]);
                                    params[i] = serializedObject != null ? serializedObject.value : null;
                                }
                                let request = {
                                    javaRequest: {
                                        correlationId: jCorrelationId,
                                        objectId: javaRefMsg.id,
                                        method: m,
                                        params: params
                                    }
                                };

                                api.awaitResponse(function (result) {
                                    if (Object.prototype.toString.call(result) === '[object Error]') {
                                        reject(result);
                                    } else if (result.error != null) {
                                        reject(new Error(result.error));
                                    } else if (result.value != null) {
                                        resolve(decodeJsParam(result.value));
                                    }
                                }, request, jCorrelationId, api.cfg.javaCallTimeout);
                            });
                        };
                    }(methodName);
                }
            }
        }
    }
}
