package org.webswing.toolkit;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.webswing.component.HtmlPanelImpl;
import org.webswing.component.HtmlPanelImpl.HtmlWindow;
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
					
					if (w != null && w instanceof WebWindow) {
						if (action.getEventType() == ActionEventType.init) {
							((WebWindow) w).handleWindowInitialized();
						} else {
							((WebWindow) w).handleWebActionEvent(new WebActionEvent(action.getActionName(), action.getData(), action.getBinaryData()));
						}
					} else if (w != null && w instanceof HtmlWindow) {
						if (action.getEventType() == ActionEventType.init) {
							((HtmlWindow) w).getTarget().handleWindowInitialized();
						} else {
							((HtmlWindow) w).getTarget().handleWebActionEvent(new WebActionEvent(action.getActionName(), action.getData(), action.getBinaryData()));
						}
					} else {
						// fire the general listeners
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
		
		WebWindowPeer peer = null;
		if (webWindow instanceof HtmlPanel) {
			peer = Util.findWindowPeerByHtmlPanel((HtmlPanel) webWindow);
		}
		if (webWindow instanceof Window) {
			peer = (WebWindowPeer) WebToolkit.targetToPeer(webWindow);
		}
		
		if (peer == null) {
			sendActionEvent(actionName, data, binaryData);
			return;
		}
		
		sendActionEvent(peer.getGuid(), actionName, data, binaryData);
	}
	
	private void sendActionEvent(String windowId, String actionName, String data, byte[] binaryData) {
		WebPaintDispatcher paintDispatcher = Util.getWebToolkit().getPaintDispatcher();
		if (paintDispatcher != null) {
			paintDispatcher.notifyActionEvent(windowId, actionName, data, binaryData);
		}
	}
	
	@Override
	public HtmlPanel createHtmlPanel() {
		return new HtmlPanelImpl();
	}
	
}
