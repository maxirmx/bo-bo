package org.webswing.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

import org.webswing.toolkit.api.component.WebDesktopPane;

public class WebDesktopPaneImpl extends WebDesktopPane {
	
	private static final long serialVersionUID = 2426526718212641945L;
	
	private final JDesktopPane original;
	private Map<Component, WebJInternalFrame> internalFrames = new HashMap<>();

	public WebDesktopPaneImpl(JDesktopPane original) {
		this.original = original;
		setLayout(new BorderLayout());
		add(this.original, BorderLayout.CENTER);
		this.original.addContainerListener(new ContainerListener() {
			@Override
			public void componentAdded(ContainerEvent e) {
				if (e.getChild() instanceof JInternalFrame) {
					final JInternalFrame frame = (JInternalFrame) e.getChild();
					SwingUtilities.invokeLater(() -> {
						WebJInternalFrame htmlJInternalFrame = new WebJInternalFrame(original, frame);
						internalFrames.put(e.getChild(), htmlJInternalFrame);
					});
				}
			}

			@Override
			public void componentRemoved(ContainerEvent e) {
				if (e.getChild() instanceof JInternalFrame) {
					final JInternalFrame frame = (JInternalFrame) e.getChild();
					if (internalFrames.containsKey(frame)) {
						internalFrames.get(frame).setVisible(false);
						internalFrames.remove(frame);
					}
				}
			}
		});
	}

	@Override
	public void paint(Graphics g) {
		Graphics gc = g.create();
		int flags = ComponentAccessor.getFlags(original);
		int flagsWithPrintingOn= flags|(1 << 11);
		try {
			ComponentAccessor.setFlags(original, flagsWithPrintingOn);
			ComponentAccessor.paintComponent(original, g);
			ComponentAccessor.paintBorder(original, g);
		} finally {
			gc.dispose();
			ComponentAccessor.setFlags(original, flags);
		}
	}

	static class ComponentAccessor {
		public static void paintComponent(JComponent c, Graphics g) {
			try {
				Method m = JComponent.class.getDeclaredMethod("paintComponent", Graphics.class);
				m.setAccessible(true);
				m.invoke(c, g);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static void paintBorder(JComponent c, Graphics g) {
			try {
				Method m = JComponent.class.getDeclaredMethod("paintBorder", Graphics.class);
				m.setAccessible(true);
				m.invoke(c, g);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static void paintChildren(JComponent c, Graphics g) {
			try {
				Method m = JComponent.class.getDeclaredMethod("paintChildren", Graphics.class);
				m.setAccessible(true);
				m.invoke(c, g);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static int getFlags(JComponent c) {
			try {
				Field f = JComponent.class.getDeclaredField("flags");
				f.setAccessible(true);
				return (int) f.get(c);
			} catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
		}

		public static void setFlags(JComponent c, int val) {
			try {
				Field f = JComponent.class.getDeclaredField("flags");
				f.setAccessible(true);
				f.set(c, val);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
