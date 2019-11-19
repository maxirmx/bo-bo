package org.webswing.toolkit.api;

import javax.swing.JDesktopPane;

import org.webswing.toolkit.api.action.WebActionListener;
import org.webswing.toolkit.api.action.WebWindow;
import org.webswing.toolkit.api.component.HtmlPanel;
import org.webswing.toolkit.api.component.WebDesktopPane;

/**
 * Webswing API used by Swing application for easy integration.  
 */
public interface WebswingApi {

	/**
	 * Adds a WebswingBrowserActionListener to listen to javascript browser initiated events.
	 */
	public void addBrowserActionListener(WebActionListener listener);
	
	/**
	 * Removed a WebswingBrowserActionListener.
	 */
	public void removeBrowserActionListener(WebActionListener listener);
	
	/**
	 * Sends an action event with optional data to the browser.
	 */
	public void sendActionEvent(String actionName, String data, byte[] binaryData);
	
	/**
	 * Sends an action event to a WebWindow, with optional data to the browser.
	 */
	public void sendActionEvent(WebWindow webWindow, String actionName, String data, byte[] binaryData);

	/**
	 * Creates an HtmlPanel component.
	 */
	public HtmlPanel createHtmlPanel();
	
	/**
	 * Creates an WebDesktopPane component.
	 */
	public WebDesktopPane createWebDesktopPane(JDesktopPane original);
	
}
