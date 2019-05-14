import $ from 'jquery';

export default class Util {
	
    //recalculateDpr
    static dpr() {
    	var dpr = Math.ceil(window.devicePixelRatio) || 1;
    	return dpr;
    }	

    static isTouchDevice() {
        return !!('ontouchstart' in window);
    }

    static getImageString(data) {
        if (typeof data === 'object') {
            let binary = '';
            let bytes = new Uint8Array(data.buffer, data.offset, data.limit - data.offset);
            for ( let i = 0, l = bytes.byteLength; i < l; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
            data = window.btoa(binary);
        }
        return 'data:image/png;base64,' + data;	
    }

    static bindEvent(el, eventName, eventHandler) {
        if (el.addEventListener != null) {
            el.addEventListener(eventName, eventHandler);
        }
    }

    static detectIE() {
        let ua = window.navigator.userAgent;

        let msie = ua.indexOf('MSIE ');
        if (msie > 0) {
            // IE 10 or older => return version number
            return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
        }

        let trident = ua.indexOf('Trident/');
        if (trident > 0) {
            // IE 11 => return version number
            let rv = ua.indexOf('rv:');
            return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
        }

        let edge = ua.indexOf('Edge/');
        if (edge > 0) {
            // IE 12 => return version number
            return parseInt(ua.substring(edge + 5, ua.indexOf('.', edge)), 10);
        }
        // other browser
        return false;
    }

    static preventGhosts(element) {
        let ANTI_GHOST_DELAY = 2000;
        let POINTER_TYPE = {
            MOUSE : 0,
            TOUCH : 1
        };
        let latestInteractionType, latestInteractionTime;

        function handleTap(type, e) {
            // console.log('got tap ' + e.type + ' of pointer ' + type);

            let now = Date.now();

            if (type !== latestInteractionType) {

                if (now - latestInteractionTime <= ANTI_GHOST_DELAY) {
                    // console.log('!prevented!');
                    e.preventDefault();
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                    return false;
                }

                latestInteractionType = type;
            }

            latestInteractionTime = now;
        }

        function attachEvents(eventList, interactionType) {
            eventList.forEach(function(eventName) {
                element[0].addEventListener(eventName, handleTap.bind(null, interactionType), true);
            });
        }

        let mouseEvents = [ 'mousedown', 'mouseup', 'mousemove' ];
        let touchEvents = [ 'touchstart', 'touchend' ];

        attachEvents(mouseEvents, POINTER_TYPE.MOUSE);
        attachEvents(touchEvents, POINTER_TYPE.TOUCH);
    }

    static GUID() {
        let S4 = function () {
            return Math.floor(Math.random() * 0x100000000).toString(16);
        };
        return (S4() + S4() + S4() + S4());
    }
}
