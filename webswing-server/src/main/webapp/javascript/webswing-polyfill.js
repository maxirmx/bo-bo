const typedarray = require('typedarray');
const es6promise = require('es6-promise').Promise;

export default class Polyfill {
    static polyfill() {
        var typedArraysSupported = false;
        if (isArrayBufferSupported()) {
            typedArraysSupported = true;
        }
        if (!isPromisesSupported()) {
            es6promise.polyfill();
        }
        
        if (!Element.prototype.matches) {
        	// fix for IE matches selector
        	Element.prototype.matches = Element.prototype.msMatchesSelector;
        }

        return typedArraysSupported;

        function isPromisesSupported() {
            if (typeof Promise !== "undefined" && Promise.toString().indexOf("[native code]") !== -1) {
                return true;
            }
            return false;
        }

        function isArrayBufferSupported() {
            if ('ArrayBuffer' in window && ArrayBuffer.toString().indexOf("[native code]") !== -1) {
                return true;
            }
            return false;
        }
    }
}
