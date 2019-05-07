define([], function () {
    'use strict';
    var getCookie = function (name) {
        var value = "; " + document.cookie;
        var parts = value.split("; " + name + "=");
        if (parts.length === 2) {
            return parts.pop().split(";").shift();
        }
        return 'en-us';
    };

    var get = function () {
        var temp = getCookie('locale');
        var language = temp ? temp : 'en-us';
        var labelSplit = arguments[0].split('.'),
            lastWord = [labelSplit[labelSplit.length - 1]];
        var _translate = javaClientLocale[language][lastWord];
        if (arguments[1] && arguments[1].pop && arguments[1].length === 2) {
            return _translate + ' ' + arguments[1][0] + '~' + arguments[1][1];
        }
        return _translate;
    }

    var javaClientLocale = {
        "en-us": {
            "uploadfile": "Upload files",
            "downloadSelected": "Download",
            "draganddropfiles": "Drag and drop files here",
            "cancel": "Cancel",
            "import": "Import",
            "close": "Close",
            "ready": "Webswing ready...",
            "initializing": "Initializing...",
            "startApp": "Starting app...",
            "connecting": "Connecting",
            "runningInOtherBrowser": "Application is already running in other browser window...",
            "sessionDisconnected": "The application is opened in another browser window. Click Activate to activate the application again.",
            "disconnected": "Disconnected...",
            "connectionError": "Connection error...",
            "toManyConnections": "Too many connections. Please try again later...",
            "stopped": "Application stopped...",
            "keepSession": "Continue existing session?",
            "continue": "Yes, continue.",
            "noStartNewSession": "No, start new session.",
            "startNewSession": "Start new session",
            "tryAgain": "Try again",
            "active": "Activate",
            "logout": "Logout",
            "retryAfterCheckService": "Please click the \"Start\" icon on the operating system, find and start \"WebProxy\" program, then try again.",
            "systemNoticeAlert": "Information",
            "closeCopy": "Cancel",
            "copy": "Copy",
            "copyInfo": "Please press Ctrl+C again or click Copy to copy the text.",
            "copySuccess": "Copy success",
            "copyFailed": "Copy failed",
            "webswingError": "Failed to connect to the Webswing server.",
            "retry": "Retry",
        },
        "zh-cn": {
            "uploadfile": "上传文件",
            "downloadSelected": "下载选中文件",
            "draganddropfiles": "拖动文件至此处上传",
            "cancel": "取消上传",
            "import": "导入",
            "close": "关闭",
            "ready": "Webswing 已准备好...",
            "initializing": "初始化中...",
            "startApp": "启动应用中...",
            "connecting": "连接中",
            "runningInOtherBrowser": "应用已经在其他浏览器窗口中运行...",
            "sessionDisconnected": "应用已经在其他浏览器窗口中打开。请点击激活按钮重新激活应用。",
            "disconnected": "断开连接...",
            "connectionError": "连接错误...",
            "toManyConnections": "连接过多。请稍后重试...",
            "keepSession": "继续存在的会话？",
            "stopped": "应用已经停止运行...",
            "continue": "是，继续。",
            "noStartNewSession": "不，启动新会话。",
            "startNewSession": "启动新会话",
            "tryAgain": "重试",
            "active": "激活",
            "logout": "退出",
            "retryAfterCheckService": "选择\"开始 > 所有程序\"，在\"启动\"中查找并点击\"WebProxy\"程序后重试。",
            "systemNoticeAlert": "提示信息",
            "closeCopy": "取消",
            "copy": "拷贝",
            "copyInfo": "请再次按Ctrl+C键进行拷贝或点击拷贝按钮。",
            "copySuccess": "拷贝成功",
            "copyFailed": "拷贝失败",
            "webswingError": "连接Webswing服务器失败。",
            "retry": "重试",
        }
    };
    return {get: get}

});
