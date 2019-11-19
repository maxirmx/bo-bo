package org.webswing.component;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JWindow;

public class WebJInternalFrame extends JWindow {

	private static final long serialVersionUID = 464839246236488334L;

	private JDesktopPane ownerPane;
	private Component target;

	public WebJInternalFrame(JDesktopPane ownerPane, Component target) {
		super();

		this.target = target;
		this.ownerPane = ownerPane;

		init();
	}

	private void init() {
		setName(target.getName());
		setSize(target.getSize());

		updateBounds();
		
		target.addHierarchyListener(new HierarchyListener() {
			@Override
			public void hierarchyChanged(HierarchyEvent e) {
				if (!target.isShowing()) {
					updateVisible(false);
				} else {
					updateVisible(true);
					updateBounds();
				}
			}
		});
		target.addHierarchyBoundsListener(new HierarchyBoundsListener() {
			@Override
			public void ancestorResized(HierarchyEvent e) {
				updateBounds();
			}

			@Override
			public void ancestorMoved(HierarchyEvent e) {
				updateBounds();
			}
		});
		target.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
				updateVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				updateBounds();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				updateBounds();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				updateVisible(false);
			}
		});

		ownerPane.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
				updateVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				updateBounds();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				updateBounds();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				updateVisible(false);
			}
		});
	}

	@Override
	public void paint(Graphics g) {
		target.paint(g);
	}

	public Component getTarget() {
		return target;
	}

	public JDesktopPane getOwnerPane() {
		return ownerPane;
	}

	public void updateVisible(boolean visible) {
		if (visible && (getWidth() == 0 || getHeight() == 0)) {
			return;
		}

		setVisible(visible);
	}

	public void updateBounds() {
		if (!target.isShowing() || !ownerPane.isShowing() || ownerPane.getWidth() == 0 || ownerPane.getHeight() == 0) {
			return;
		}
		
		updateVisible(true);

		Point targetLocation = target.getLocationOnScreen();
		Dimension targetSize = target.getSize();

		Point ownerLocation = ownerPane.getLocationOnScreen();
		Dimension ownerSize = ownerPane.getSize();

		setLocation(targetLocation);

		setSize(Math.max(Math.min(ownerLocation.x + ownerSize.width, targetLocation.x + targetSize.width) - targetLocation.x, 0), Math.max(Math.min(ownerLocation.y + ownerSize.height, targetLocation.y + targetSize.height) - targetLocation.y, 0));
	}

}