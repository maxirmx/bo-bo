package org.webswing.toolkit.extra;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.webswing.toolkit.util.Util;

public class WebRepaintManager extends RepaintManager {

	private RepaintManager delegate;
	private Map<Container, Rectangle> dirty = new HashMap<Container, Rectangle>();

	public WebRepaintManager(RepaintManager delegate) {
		if (delegate != null) {
			this.delegate = delegate;
		}
	}

	public void setDelegate(RepaintManager delegate) {
		if (delegate != null) {
			this.delegate = delegate;
		}
	}

	@Override
	public void addDirtyRegion(final JComponent c, final int x, final int y, final int w, final int h) {
		addDirtyRegionPrivate(c, x, y, w, h);
	}

	@Override
	public void addDirtyRegion(Window window, int x, int y, int w, int h) {
		addDirtyRegionPrivate(window, x, y, w, h);
	}

	@Override
	public void addDirtyRegion(Applet applet, int x, int y, int w, int h) {
		addDirtyRegionPrivate(applet, x, y, w, h);
	}

	private void addDirtyRegionPrivate(Container c, int x, int y, int w, int h) {
		synchronized (delegate) {
			Rectangle r = dirty.get(c);
			if (r != null) {
				SwingUtilities.computeUnion(x, y, w, h, r);
			} else {
				dirty.put(c, new Rectangle(x, y, w, h));
			}
		}
	}

	@Override
	public Rectangle getDirtyRegion(JComponent aComponent) {
		Rectangle r;
		synchronized (delegate) {
			r = dirty.get(aComponent);
		}
		if (r == null)
			return new Rectangle(0, 0, 0, 0);
		else
			return new Rectangle(r);
	}

	@Override
	public void markCompletelyClean(JComponent component) {
		synchronized (delegate) {
			dirty.remove(component);
		}
	}

	public void process() {
		synchronized (delegate) {
			if (!dirty.isEmpty()) {
				// 将当前积累的脏区域交由delegate进行合并,并启动绘制
				// 注意一定要将脏区域提交放到EDT里，因为一个事件（比如鼠标点击菜单并会触发菜单小时以及对话框弹出）会提交很多脏区域，而addDirtyRegion是在EDT里
				// 但是process方法是在33ms的另外一个线程，两个线程并发，如果提交到delegate脏区域只包含部分脏区域，导致多出一张图片的问题
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						addDirtyRegionToDelegate();
					}
				});
			}
		}
	}

	private void addDirtyRegionToDelegate() {
		synchronized (delegate) {
			for (Container c : dirty.keySet()) {
				Rectangle r = dirty.get(c);
				if (r == null) {
					r = new Rectangle(0, 0, 0, 0);
				}
				if (c instanceof JComponent) {
					Panel p = Util.findHwComponentParent((JComponent) c);
					if (p != null) {
						for (Component chld : p.getComponents()) {
							delegate.addDirtyRegion((JComponent) chld, 0, 0, chld.getWidth(), chld.getHeight());
						}
					} else {
						if (r != null) {
							delegate.addDirtyRegion((JComponent) c, r.x, r.y, r.width, r.height);
						}
					}
				} else if (c instanceof Window) {
					if (r != null) {
						delegate.addDirtyRegion((Window) c, r.x, r.y, r.width, r.height);
					}
				} 
			}
			dirty.clear();
		}
	}

	@Override
	public void addInvalidComponent(JComponent invalidComponent) {
		delegate.addInvalidComponent(invalidComponent);
	}

	@Override
	public void removeInvalidComponent(JComponent component) {
		delegate.removeInvalidComponent(component);
	}

	/**
	 * {@inheritDoc}
	 */
	public Dimension getDoubleBufferMaximumSize() {
		return delegate.getDoubleBufferMaximumSize();
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
		return delegate.getOffscreenBuffer(c, proposedWidth, proposedHeight);
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getVolatileOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
		return delegate.getVolatileOffscreenBuffer(c, proposedWidth, proposedHeight);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isCompletelyDirty(JComponent component) {
		return delegate.isCompletelyDirty(component);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDoubleBufferingEnabled() {
		return delegate.isDoubleBufferingEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	public void markCompletelyDirty(JComponent component) {
		delegate.markCompletelyDirty(component);
	}

	/**
	 * {@inheritDoc}
	 */
	public void paintDirtyRegions() {
		delegate.paintDirtyRegions();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDoubleBufferingEnabled(boolean flag) {
		delegate.setDoubleBufferingEnabled(flag);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDoubleBufferMaximumSize(Dimension d) {
		delegate.setDoubleBufferMaximumSize(d);
	}

	/**
	 * {@inheritDoc}
	 */
	public void validateInvalidComponents() {
		delegate.validateInvalidComponents();
	}

}
