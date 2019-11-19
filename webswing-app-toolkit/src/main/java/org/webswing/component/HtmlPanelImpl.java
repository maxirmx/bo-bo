package org.webswing.component;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JInternalFrame;

import org.webswing.toolkit.api.action.WebActionEvent;
import org.webswing.toolkit.api.action.WebWindowActionListener;
import org.webswing.toolkit.api.component.HtmlPanel;
import org.webswing.toolkit.api.component.WebDesktopPane;

public class HtmlPanelImpl extends HtmlPanel {

	private static final long serialVersionUID = 3974466420979353544L;

	private List<WebWindowActionListener> webActionListeners = new ArrayList<>();
	
	/**
	 * WebDesktopPane parent in case the HtmlPane is included in a JInternalFrame.
	 */
	private WebDesktopPane webDesktopPane;
	/**
	 * JInternalFrame parent in case the HtmlPane is included in it.
	 */
	private JInternalFrame jInternalFrame;
	
	public HtmlPanelImpl() {
	}
	
	public HtmlPanelImpl(WebDesktopPane webDesktopPane, JInternalFrame jInternalFrame) {
		this.webDesktopPane = webDesktopPane;
		this.jInternalFrame = jInternalFrame;
	}
	
	@Override
	public final void handleWebActionEvent(WebActionEvent webActionEvent) {
		for (WebWindowActionListener listener : webActionListeners) {
			listener.actionPerformed(webActionEvent);
		}
	}
	
	@Override
	public void handleWindowInitialized() {
		for (WebWindowActionListener listener : webActionListeners) {
			listener.windowInitialized();
		}
	}
	
	public void addWebWindowActionListener(WebWindowActionListener listener) {
		webActionListeners.add(listener);
	}
	
	public void removeWebWindowActionListener(WebWindowActionListener listener) {
		webActionListeners.remove(listener);
	}
	
	public WebDesktopPane getWebDesktopPane() {
		return webDesktopPane;
	}
	
	public JInternalFrame getjInternalFrame() {
		return jInternalFrame;
	}
	
}
