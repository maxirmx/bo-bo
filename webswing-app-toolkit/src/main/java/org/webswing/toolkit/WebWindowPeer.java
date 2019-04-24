package org.webswing.toolkit;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.peer.WindowPeer;

import javax.swing.SwingUtilities;

import org.webswing.toolkit.extra.WindowManager;
import org.webswing.toolkit.util.Util;

public class WebWindowPeer extends WebContainerPeer implements WindowPeer {

	//////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////// WebWindowPeer Implementation//////////////////////////////////////////////////
	public WebWindowPeer(Window t) {
		super(t);
	}

	public void toFront() {
		WindowManager.getInstance().bringToFront((Window) target);
	}

	public void toBack() {
		WindowManager.getInstance().bringToBack((Window) target);
	}

	public void setAlwaysOnTop(boolean paramBoolean) {
		WindowManager.getInstance().bringToFront((Window) target);
	}

	public void updateFocusableWindowState() {
	}

	public boolean requestWindowFocus() {
		Util.getWebToolkit().getWindowManager().activateWindow((Window) target);
		return true;
	}

	public void setModalBlocked(Dialog paramDialog, boolean paramBoolean) {
	}

	public void updateMinimumSize() {
	}

	public void updateIconImages() {
	}

	public void setOpacity(float paramFloat) {
	}

	public void setOpaque(boolean paramBoolean) {
	}

	public void repositionSecurityWarning() {
	}

	public void updateWindow() {
	}

	public void show() {
		if (isJFileChooserDialog()) {
			Util.getWebToolkit().getPaintDispatcher().notifyFileDialogActive(this);
		}
		Util.getWebToolkit().getWindowManager().activateWindow((Window) target);
	}

	public void hide() {
		if (isJFileChooserDialog()) {
			Util.getWebToolkit().getPaintDispatcher().notifyFileDialogHidden(this);
		}
		Util.getWebToolkit().getWindowManager().removeWindow((Window) target);
		notifyWindowClosed();
	}

	public void setTitle(String title) {
		updateWindowDecorationImage();
		Util.getWebToolkit().getPaintDispatcher().notifyWindowAreaRepainted(this.getGuid(), new Rectangle(0, 0, getBounds().width, getInsets().top));
	}

	public void setResizable(boolean resizeable) {
	}

	protected Point validate(int x, int y, int w, int h) {
		Point result = new Point(x, y);
		if (w == 0 && h == 0) {
			return result;
		}
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = this.getInsets();
		if (y < 0) {
			result.y = 0;
		}
		if (y > (screen.height - insets.top)) {
			result.y = screen.height - insets.top;
		}
		if (x < ((w - 40) * (-1))) {
			result.x = (w - 40) * (-1);
		}
		if (x > (screen.width - 40)) {
			result.x = (screen.width - 40);
		}
		if ((target instanceof Frame) && ((Frame) target).getExtendedState() == Frame.MAXIMIZED_BOTH) {
			result.x = 0;
			result.y = 0;
		}
		if (result.x != x || result.y != y) {
			((Component) target).setLocation(result);
		}
		return result;
	}

	public void updateAlwaysOnTopState() {
	}

	public boolean isJFileChooserDialog() {
		return Util.discoverFileChooser(this) != null;
	}

}
