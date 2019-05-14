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
