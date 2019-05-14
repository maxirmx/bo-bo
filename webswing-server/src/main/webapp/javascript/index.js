import Webswing from './webswing.js';
(function(){
  let webswingScriptInitializationSuccessfull = false;
  
  try {
    new Webswing();
	webswingScriptInitializationSuccessfull = true;
  } catch(e) {
	console.error(e);
	if(!webswingScriptInitializationSuccessfull){
		function getAllElementsWithAttribute(attribute) {
			let matchingElements = [];
			let allElements = document.getElementsByTagName('*');
			for ( let i = 0, n = allElements.length; i < n; i++) {
				if (allElements[i].getAttribute(attribute) !== null) {
					// Element exists with attribute. Add to array.
					matchingElements.push(allElements[i]);
				}
			}
			return matchingElements;
		}
		
		let elements = getAllElementsWithAttribute('data-webswing-instance');
		for ( let i = 0; i < elements.length; i++) {
			elements[i].innerHTML = '<h1>Your Browser is no longer supported.</h1>\r\n<p>Please upgrade your browser to one of following.<p>\r\n<table width="650px" cellpadding="5">\r\n\t<tbody>\r\n\t\t<tr>\r\n\t\t\t<td align="center" valign="center" width="120px"><a href="https://www.google.com/chrome/?hl=en_GB"><img src="//ssl.gstatic.com/images/icons/product/chrome-128.png" width="128px" height="128px"></a></td>\r\n\t\t\t<td align="center" valign="center" width="120px"><a href="//www.microsoft.com/windows/internet-explorer/default.aspx"><img src="//ssl.gstatic.com/s2/profiles/images/ie.gif" width="120px" height="115px"></a></td>\r\n\t\t\t<td align="center" valign="center" width="120px"><a href="//www.mozilla.com/firefox/"><img src="//ssl.gstatic.com/s2/profiles/images/firefox.gif" width="120px" height="118px"></a></td>\r\n\t\t\t<td align="center" valign="center" width="120px"><a href="//www.apple.com/safari/download/"><img src="//ssl.gstatic.com/s2/profiles/images/safari.png" width="120px" height="120px"></a></td>\r\n\t\t</tr>\r\n\t\t<tr>\r\n\t\t\t<td align="center" valign="top"><a href="https://www.google.com/chrome/?hl=en_GB">Download Chrome now</a></td>\r\n\t\t\t<td align="center" valign="top"><a href="//www.microsoft.com/windows/internet-explorer/default.aspx">Download Internet Explorer (version 9+)</a></td>\r\n\t\t\t<td align="center" valign="top"><a href="//www.mozilla.com/firefox/">Download Firefox</a></td>\r\n\t\t\t<td align="center" valign="top"><a href="//www.apple.com/safari/download/">Download Safari</a></td>\r\n\t\t</tr>\r\n\t</tbody>\r\n</table>\r\n';
		}
	}
	
    throw Error("Error while starting webswing, Reason: " + e);
  }
}());