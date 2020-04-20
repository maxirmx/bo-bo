package org.webswing.toolkit.api;

import java.awt.Container;

import javax.swing.JComponent;

import org.webswing.toolkit.api.action.WebActionListener;
import org.webswing.toolkit.api.action.WebWindow;
import org.webswing.toolkit.api.component.HtmlPanel;

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
	 * Compositing window manager must be enabled in config.
	 * @throws IllegalArgumentException if isCompositingWindowManager() returns false
	 */
	public HtmlPanel createHtmlPanel();
	
	/**
	 * Creates an HtmlPanel component which is a part of given component and has a container parent.
	 * Compositing window manager must be enabled in config.
	 * @throws IllegalArgumentException if isCompositingWindowManager() returns false
	 */
	public HtmlPanel createHtmlPanelForComponent(Container container, JComponent component);
	
	/**
	 * Registers given container to be a parent web container and all of its child components will be rendered into separate canvases.
	 * Compositing window manager must be enabled in config.
	 * @throws IllegalArgumentException if isCompositingWindowManager() returns false
	 */
	public void registerWebContainer(Container container);
	
	/**
	 * Is compositing window manager enabled?
	 */
	public boolean isCompositingWindowManager();

}
