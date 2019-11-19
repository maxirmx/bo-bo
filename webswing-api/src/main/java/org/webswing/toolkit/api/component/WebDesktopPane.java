package org.webswing.toolkit.api.component;

import javax.swing.JDesktopPane;
import javax.swing.JPanel;

public abstract class WebDesktopPane extends JPanel {

	private static final long serialVersionUID = 1257129822488402149L;

	public abstract JDesktopPane getOriginal();

	public String getId() {
		return System.identityHashCode(this) + "";
	}
	
}
