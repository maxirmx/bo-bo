define(['webswing-util'], function amdFactory(util) {
    "use strict";
    return function JsLinkModule() {
        var module = this;
        module.provides = {
            get: get,
            getLocale: getLocale,
            dispose: dispose
        };

        var cookieName = 'webswingID';
        function get() {
            var id = readCookie(cookieName);
                if (id != null && id != "") {
                return id;
            } else {
                id = util.GUID();
                createCookie(cookieName, id);
                return id;
            }
        }

        function getLocale(){
            var lang = (navigator.language || navigator.browserLanguage).split('-')[0];
            return lang;
        }

        function dispose() {
            eraseCookie(cookieName);
        }

        function createCookie(name, value, days) {
            var expires;

            if (days) {
                var date = new Date();
                date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
                expires = "; expires=" + date.toGMTString();
            } else {
                expires = "";
            }
            document.cookie = escape(name) + "=" + escape(value) + expires + "; path=/";
        }

        function readCookie(name) {
            var nameEQ = escape(name) + "=";
            var ca = document.cookie.split(';');
            for (var i = 0; i < ca.length; i++) {
                var c = ca[i];
                while (c.charAt(0) === ' ')
                    c = c.substring(1, c.length);
                    if (c.indexOf(nameEQ) === 0){
                        var webswingCookie = unescape(c.substring(nameEQ.length, c.length));
                        if(webswingCookie == null || webswingCookie.length == 0){
                            console.log("find unexpect null webswingID");
                            continue;
                        }
                        return webswingCookie;
                    }
            }
            return null;
        }

        function eraseCookie(name) {
            createCookie(name, "", -1);
        }
    };
});