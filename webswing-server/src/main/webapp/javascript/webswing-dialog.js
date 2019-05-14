import $ from 'jquery';
import html from './templates/dialog.html';
import css from './templates/dialog.css';
import i18n from './webswing-i18n';
import cssBootstrap from './templates/bootstrap.css';

export default class DialogModule {
    constructor() {
	    let style = $("<style></style>", {
        type: "text/css"
    });
    style.text(css);
     let style0 = $("<style></style>", {
            type: "text/css"
        });
        style0.text(cssBootstrap);
        $("head").prepend(style0);
    $("head").prepend(style);

        let module = this;
        let api;
        module.injects = api = {
            cfg: 'webswing.config',
            continueSession: 'base.continueSession',
            kill: 'base.kill',
            newSession: 'webswing.newSession',
            reTrySession: 'webswing.reTrySession',
            fireCallBack: 'webswing.fireCallBack',
            logout: 'login.logout',
        };
        module.provides = {
            show: show,
            hide: hide,
            current: current,
            content: configuration()
        };

        let currentContent;
        let dialog, content, header, backdrop;

        function configuration() {
            return {
                readyDialog: {
                    content: '<p>' + i18n.get('ready') + '</p>'
                },
                initializingDialog: {
                    content: '<p>' + i18n.get('initializing') + '</p>'
                },
                startingDialog: {
                    content: '<p>' + i18n.get('startApp') + '</p>'
                },
                connectingDialog: {
                    content: '<p>' + i18n.get('connecting') + '</p>'
                },
                applicationAlreadyRunning: activeCurrentDialog(i18n.get('runningInOtherBrowser')),
                sessionStolenNotification: activeCurrentDialog(i18n.get('sessionDisconnected')),
                disconnectedDialog: retryMessageDialog(i18n.get('disconnected')),
                connectionErrorDialog: retryMessageDialog(i18n.get('connectionError')),
                tooManyClientsNotification: retryMessageDialog(i18n.get('toManyConnections')),
                stoppedDialog: finalMessageDialog(i18n.get('stopped')),
                continueOldSessionDialog: {
                    content: '<p>' + i18n.get('keepSession') + '</p>'
                        + '<button data-id="continue" class="btn btn-primary">' + i18n.get('continue') + '</button><span> </span><button data-id="newsession" class="btn btn-default" >' + i18n.get('noStartNewSession') + '</button>',
                    events: {
                        continue_click: function () {
                            api.continueSession();
                        },
                        newsession_click: function () {
                            api.kill();
                            api.newSession();
                        }
                    }
                }
            };
        }

        function finalMessageDialog(msg) {
            return {
                content: '<p>'
                        + msg
                        + '</p><button data-id="newsession" class="btn btn-primary">' + i18n.get('startNewSession') + '</button> <span> </span><button data-id="logout" class="btn btn-default">' + i18n.get('logout') + '</button>',
                events: {
                    newsession_click: function () {
                        api.fireCallBack({type: 'reloadSession'});
                        api.newSession();
                    },
                    logout_click: function () {
                        api.logout();
                    }
                }
            };
        }

        function activeCurrentDialog(msg) {
            return {
                content: '<p>' + msg + '</p>'
                    + '<button data-id="retrysession" class="btn btn-primary">' + i18n.get('active') + '</button> <span> </span>',
                events: {
                    retrysession_click: function () {
                        api.continueSession();
                    }
                }
            };
        }
        
        function retryMessageDialog(msg) {
            return {
                content: '<p>'
                        + msg
                        + '</p><button data-id="retrysession" class="btn btn-primary">' + i18n.get('tryAgain') + '</button> <span> </span><button data-id="logout" class="btn btn-default">' + i18n.get('logout') + '</button>',
                events: {
                    retrysession_click: function () {
                        api.fireCallBack({type: 'reloadSession'});
                        api.reTrySession();
                    },
                    logout_click: function () {
                        api.logout();
                    }
                }
            };
        }

        function setup() {
            api.cfg.rootElement.append(html);
            backdrop = api.cfg.rootElement.find('div[data-id="commonDialogBackDrop"]');
            dialog = api.cfg.rootElement.find('div[data-id="commonDialog"]');
            content = dialog.find('div[data-id="content"]');
            header = dialog.find('div[data-id="header"]');
        }

        function show(msg) {
            if (dialog == null) {
                setup();
            }
            currentContent = msg;
            if (dialog.is(":visible")) {
                header.hide();
                content.hide();
            }
            if (msg.header != null) {
                header.html(msg.header);
                if (dialog.is(":visible")) {
                    header.fadeIn('fast');
                } else {
                    header.show();
                }
            } else {
                header.hide();
                header.html('');
            }
            content.html(msg.content);
            for (let e in msg.events) {
                let element = dialog.find('*[data-id="' + e.substring(0, e.lastIndexOf('_')) + '"]');
                element.bind(e.substring(e.lastIndexOf('_') + 1), msg.events[e]);
            }
            if (dialog.is(":visible")) {
                content.fadeIn('fast');
            }
            backdrop.show();
            dialog.slideDown('fast');
        }

        function hide() {
            currentContent = null;
            content.html('');
            dialog.fadeOut('fast');
            backdrop.fadeOut('fast');
        }

        function current() {
            return currentContent;
        }
    }
}
