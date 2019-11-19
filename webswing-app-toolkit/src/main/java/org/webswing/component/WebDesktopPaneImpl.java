package org.webswing.component;

import java.awt.BorderLayout;

import javax.swing.JDesktopPane;

import org.webswing.toolkit.api.component.WebDesktopPane;

public class WebDesktopPaneImpl extends WebDesktopPane {
	
	private static final long serialVersionUID = 2426526718212641945L;
	
	private final JDesktopPane original;
	
	public WebDesktopPaneImpl(JDesktopPane original) {
		this.original = original;
		setName(original.getName());
		setLayout(new BorderLayout());
		add(this.original, BorderLayout.CENTER);
	}

	@Override
	public JDesktopPane getOriginal() {
		return original;
	}
	
}
