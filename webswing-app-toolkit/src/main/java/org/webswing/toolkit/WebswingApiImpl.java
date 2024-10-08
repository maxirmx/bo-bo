package org.webswing.toolkit;

import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JComponent;

import org.webswing.component.HtmlPanelImpl;
import org.webswing.dispatch.WebPaintDispatcher;
import org.webswing.model.Msg;
import org.webswing.model.c2s.ActionEventMsgIn;
import org.webswing.model.c2s.ActionEventMsgIn.ActionEventType;
import org.webswing.toolkit.api.WebswingApi;
import org.webswing.toolkit.api.action.WebActionEvent;
import org.webswing.toolkit.api.action.WebActionListener;
import org.webswing.toolkit.api.action.WebWindow;
import org.webswing.toolkit.api.component.HtmlPanel;
import org.webswing.toolkit.util.DeamonThreadFactory;
import org.webswing.toolkit.util.Logger;
import org.webswing.toolkit.util.Util;

public class WebswingApiImpl implements WebswingApi {
	private final List<WebActionListener> browserActionListeners = Collections.synchronizedList(new ArrayList<WebActionListener>());
	private ExecutorService apiProcessor = Executors.newSingleThreadExecutor(DeamonThreadFactory.getInstance());

	void processEvent(final Msg msg) {
		if (msg instanceof ActionEventMsgIn) {
			ActionEventMsgIn action = (ActionEventMsgIn) msg;
			if (action != null) {
				if (action.getWindowId() != null) {
					Window w = Util.findWindowById(action.getWindowId());
					HtmlPanel hp = Util.findHtmlPanelById(action.getWindowId());
					
					if (w != null && w instanceof WebWindow) {
						if (action.getEventType() == ActionEventType.init) {
							((WebWindow) w).handleWindowInitialized();
						} else {
							((WebWindow) w).handleWebActionEvent(new WebActionEvent(action.getActionName(), action.getData(), action.getBinaryData()));
						}
					} else if (hp != null) {
						if (action.getEventType() == ActionEventType.init) {
							hp.handleWindowInitialized();
						} else {
							hp.handleWebActionEvent(new WebActionEvent(action.getActionName(), action.getData(), action.getBinaryData()));
						}
					} else if (action.getEventType() != ActionEventType.init) {
						// fire the general listeners
						// don't fire for init event type
						fireBrowserActionListener(new WebActionEvent(action.getActionName(), action.getData(), action.getBinaryData()));
					}
				} else {
					fireBrowserActionListener(new WebActionEvent(action.getActionName(), action.getData(), action.getBinaryData()));
				}
			}
		}
	}

	private void fireBrowserActionListener(WebActionEvent actionEvent) {
		synchronized (browserActionListeners) {
			for (WebActionListener l : browserActionListeners) {
				try {
					l.actionPerformed(actionEvent);
				} catch (Exception e) {
					Logger.error("Browser action listener failed.", e);
				}
			}
		}
	}

	@Override
	public void addBrowserActionListener(WebActionListener listener) {
		if (listener != null) {
			browserActionListeners.add(listener);
		}
	}

	@Override
	public void removeBrowserActionListener(WebActionListener listener) {
		if (listener != null) {
			browserActionListeners.remove(listener);
		}
	}
	
	@Override
	public void sendActionEvent(String actionName, String data, byte[] binaryData) {
		sendActionEvent((String) null, actionName, data, binaryData);
	}
	
	@Override
	public void sendActionEvent(WebWindow webWindow, String actionName, String data, byte[] binaryData) {
		if (webWindow == null || !(webWindow instanceof Window || webWindow instanceof HtmlPanel)) {
			sendActionEvent(actionName, data, binaryData);
			return;
		}
		
		String windowId = null;
		if (webWindow instanceof HtmlPanel) {
			windowId = System.identityHashCode(webWindow) + "";
		}
		if (webWindow instanceof Window) {
			windowId = ((WebWindowPeer) WebToolkit.targetToPeer(webWindow)).getGuid();
		}

		if (windowId == null) {
			sendActionEvent(actionName, data, binaryData);
			return;
		}

		sendActionEvent(windowId, actionName, data, binaryData);
	}
	
	private void sendActionEvent(String windowId, String actionName, String data, byte[] binaryData) {
		WebPaintDispatcher paintDispatcher = Util.getWebToolkit().getPaintDispatcher();
		if (paintDispatcher != null) {
			paintDispatcher.notifyActionEvent(windowId, actionName, data, binaryData);
		}
	}
	
	@Override
	public HtmlPanel createHtmlPanel() {
		if (!isCompositingWindowManager()) {
			throw new IllegalArgumentException("Not allowed to create HtmlPanel! Enable compositing window manager.");
		}
		HtmlPanel htmlPanel = new HtmlPanelImpl();
		Util.getWebToolkit().getPaintDispatcher().registerHtmlPanel(htmlPanel);
		return htmlPanel;
	}
	
	@Override
	public HtmlPanel createHtmlPanelForComponent(Container container, JComponent component) {
		if (!isCompositingWindowManager()) {
			throw new IllegalArgumentException("Not allowed to create HtmlPanel! Enable compositing window manager.");
		}
		HtmlPanel htmlPanel = new HtmlPanelImpl(container, component);
		Util.getWebToolkit().getPaintDispatcher().registerHtmlPanel(htmlPanel);
		return htmlPanel;
	}
	
	@Override
	public void registerWebContainer(Container container) {
		if (!isCompositingWindowManager()) {
			throw new IllegalArgumentException("Not allowed to create web container! Enable compositing window manager.");
		}
		Util.getWebToolkit().getPaintDispatcher().registerWebContainer(container);
	}
	
	@Override
	public boolean isCompositingWindowManager() {
		return Util.isCompositingWM();
	}
	
}
