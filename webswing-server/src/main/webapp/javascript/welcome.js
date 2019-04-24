var enTranslations={
  "tip-content-title":"The management plug-in can be properly accessed.",
  "tip-content-detail": "Return to the main page and continue."
};
var cnTranslations={
  "tip-content-title":"管理插件访问正常",
  "tip-content-detail": "请返回网管页面继续操作"
};

function setContentBasedOnLocale(localeContent) {
  document.getElementById("tip-content-title").textContent = localeContent['tip-content-title'];
  document.getElementById("tip-content-detail").textContent = localeContent['tip-content-detail'];
}

function getLanguage(){
  var language = navigator.language;   //判断除IE外其他浏览器使用语言
  if(!language){//判断IE浏览器使用语言
    language = navigator.browserLanguage;
  }
  return language;
}
function setHtmlContent(){
  var urlParams = getRouterParams();
  var locale = urlParams && urlParams.locale ? urlParams.locale : getLanguage();
  locale = locale.toLowerCase();
  var localeContent = (locale === 'zh_cn' || locale === 'zh-cn') ? cnTranslations : enTranslations;
  setContentBasedOnLocale(localeContent);
}

function getRouterParams() {
  var hashDetail = window
    .location
    .href
    .split('?');
  if(hashDetail.length > 1){
    var params = hashDetail[1]
      ? hashDetail[1].split('&')
      : [];
    var query = {};
    for(var i = 0 ; i < params.length; i++){
      var temp = params[i].split('=');
      query[temp[0]] = temp[1];
    }
    return query;
  }
  return null;
}

setHtmlContent();
