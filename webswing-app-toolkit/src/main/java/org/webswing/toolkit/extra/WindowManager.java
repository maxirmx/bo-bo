package org.webswing.toolkit.extra;

import org.webswing.common.WindowActionType;
import org.webswing.dispatch.WebEventDispatcher;
import org.webswing.dispatch.WebPaintDispatcher;
import org.webswing.model.s2c.CursorChangeEventMsg;
import org.webswing.toolkit.WebComponentPeer;
import org.webswing.toolkit.WebDialogPeer;
import org.webswing.toolkit.WebKeyboardFocusManagerPeer;
import org.webswing.toolkit.WebToolkit;
import org.webswing.toolkit.util.Services;
import org.webswing.toolkit.util.Util;
import sun.awt.CausedFocusEvent;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("restriction")
public class WindowManager {

	private static WindowManager singleton = null;
	private WindowHierarchyTree zorder = new WindowHierarchyTree();
	private Window activeWindow = null;
	private WindowEventHandler eventhandler = new WindowEventHandler();
	private String currentCursor = CursorChangeEventMsg.DEFAULT_CURSOR;

	private WindowManager() {
	}

	public static WindowManager getInstance() {
		if (singleton == null) {
			singleton = new WindowManager();
		}
		return singleton;
	}

	public void bringToFront(Window w) {
		synchronized (Util.getWebToolkit().getTreeLock()) {
			synchronized (WebPaintDispatcher.webPaintLock) {
				if (w != null && !w.isEnabled()) {
					return;
				}
				if ((w == null || w.isFocusableWindow()) && activeWindow != w) {
					Window oldActiveWindow = activeWindow;
					activeWindow = w;
					if (activeWindow != null) {
						WindowEvent gainedFocusWindowEvent = new WindowEvent(activeWindow, WindowEvent.WINDOW_GAINED_FOCUS, activeWindow, 0, 0);
						WebEventDispatcher.dispatchEventInSwing(activeWindow, gainedFocusWindowEvent);
						WebComponentPeer activeWindowPeer = (WebComponentPeer) WebToolkit.targetToPeer(activeWindow);
						activeWindowPeer.updateWindowDecorationImage();
						Util.getWebToolkit().getPaintDispatcher().notifyWindowRepaint(activeWindow);
					}
					if (oldActiveWindow != null) {
						WebComponentPeer oldActiveWindowPeer = (WebComponentPeer) WebToolkit.targetToPeer(oldActiveWindow);
						oldActiveWindowPeer.updateWindowDecorationImage();
						Util.getWebToolkit().getPaintDispatcher().notifyWindowRepaint(oldActiveWindow);
					}
				}
				if (w != null) {
					zorder.bringToFront(w);
				}
			}
		}
	}

	public void removeWindow(Window target) {
		synchronized (WebPaintDispatcher.webPaintLock) {
			zorder.removeWindow(target);
			if (target == activeWindow) {
				activeWindow = null;
			}
		}
	}

	public void bringToBack(Window w) {
		synchronized (WebPaintDispatcher.webPaintLock) {
			// w.setAlwaysOnTop(false);
			// removeWindow(w);
			// zorder.add(w);
			// w.repaint();
		}
	}

	public Window getActiveWindow() {
		return activeWindow;
	}

	public boolean isWindowActive(Window w) {
		if (activeWindow == w) {
			return true;
		} else {
			return false;
		}
	}

	public void activateWindow(Window w) {
		activateWindow(w, 0, 0);
	}

	public boolean activateWindow(Window w, Component newFocusOwner, int x, int y, boolean tmp, boolean focusedWindowChangeAllowed, CausedFocusEvent.Cause cause) {
		boolean success = false;
		boolean newWindow = false;
		if (!zorder.contains(w)) {
			if(w.getClass().getName().contains("JLightweightFrame")){
				return false;
			}
			zorder.addWindow(w);
			newWindow = true;
		}

		if (isBlockedByModality(w, newWindow)) {
			return false;
		}

		//dont allow activation of disabled windows
		if (!w.isEnabled()) {
			return false;
		}
		
		if (focusedWindowChangeAllowed || activeWindow == w) {

			if (newFocusOwner != null && newFocusOwner.isFocusable() && w.isFocusableWindow()) {
				int result = WebKeyboardFocusManagerPeer.shouldNativelyFocusHeavyweight(w, newFocusOwner, tmp, true, new Date().getTime(), cause);
				switch (result) {
				case 1:
					success = true;
					break;
				case 2:
					WebKeyboardFocusManagerPeer.deliverFocus(w, newFocusOwner, tmp, true, new Date().getTime(), cause);
					success = true;
					break;
				default:
					break;
				}
			}

			if (SwingUtilities.isRectangleContainingRectangle(new Rectangle(0, 0, w.getWidth(), w.getHeight()), new Rectangle(x, y, 0, 0))) {
				bringToFront(w);
			} else {
				bringToFront(null);
			}
		}
		return success;

	}
	
	public boolean isBlockedByModality(Window w, boolean newWindow) {
		//if active window is in modal branch and requested window is not modalExclude type 
		if (zorder.isInModalBranch(activeWindow) && !(w instanceof sun.awt.ModalExclude)) {
			// if fullModal (not document_modal) branch
			if (zorder.isInFullModalBranch(activeWindow)) {
				//don't allow activation outside modal dialog ancestor's tree 
				if (!(isModal(w) && newWindow) && !zorder.isInSameModalBranch(activeWindow, w)) {
					return true;
				}
			} else {//if in document_modal branch
				//don't allow activation in same window branch
				if (zorder.isParent(w, activeWindow)) {
					return true;
				}
			}
		}
		
		return false;
	}

    /**
     * 当点击非模态Dialog内的区域时，为Dialog窗体增加闪烁效果<br>
     * @author liuwen 00207214
     * @since  iManager U2000 MSO, 2018年1月12日
     * @param curSelectedWindow 当前选中对话框
     */
	public void flashTopModalDialog(Window curSelectedWindow)
	{
        // 当前顶部激活窗体为选中（鼠标点中）的窗体，不需要做进一步处理
	    if(activeWindow.equals(curSelectedWindow))
	    {
	       return;
	    }

	    Object activeWindowPeer = WebToolkit.targetToPeer(activeWindow);
	    if(activeWindowPeer instanceof WebDialogPeer)
	    {
	        ((WebDialogPeer) activeWindowPeer).flashWindow();
	    }
	}

	private boolean isModal(Window w) {
		return (w instanceof Dialog) && ((Dialog) w).isModal();
	}

    public void activateWindow(final Window w, final int x, final int y)
    {
        activateWindow(w, null, x, y, false, true, CausedFocusEvent.Cause.NATIVE_SYSTEM);
    }

	public Window getVisibleWindowOnPosition(int x, int y) {
		Window positionWin = zorder.getVisibleWindowOnPosition(x, y);
		if (positionWin == null) {
			positionWin = activeWindow;
		}
		return positionWin;
	}

	@SuppressWarnings("deprecation")
	public Component getVisibleComponentOnPosition(int x, int y, String winId) {
		Component result = activeWindow;
		Window positionWin;
		if (winId != null && winId.length() > 0) {
			positionWin = Util.findWindowById(winId);
		} else {
			positionWin = zorder.getVisibleWindowOnPosition(x, y);
		}
		if (positionWin != null) {
			result = ((WebComponentPeer) positionWin.getPeer()).getHwComponentAt(x, y);
		}
		return result;
	}

	public Map<String, List<Rectangle>> extractNonVisibleAreas() {
		return zorder.extractNonVisibleAreas();
	}
	
	public List<String> getZOrder() {
		return zorder.getZOrder();
	}

	public void requestRepaintAfterMove(Window w, Rectangle originalPosition) {
		zorder.requestRepaintAfterMove(w, originalPosition);
	}

	public void handleWindowDecorationEvent(Window w, MouseEvent e) {
		WindowActionType wat = Services.getImageService().getWindowDecorationTheme().getAction(w, new Point(e.getX(), e.getY()));
		eventhandler.handle(wat, e);
	}

	public boolean isLockedToWindowDecorationHandler() {
		return eventhandler.isEventHandlingLocked();
	}

	public Window getLockedToWindow() {
		return eventhandler.getLockedToWindow();
	}

	public String getCurrentCursor() {
		return currentCursor;
	}

	public void setCurrentCursor(String currentCursor) {
		this.currentCursor = currentCursor;
	}
	
	public void resetEventHandlerLock(boolean lockToBeReset) {
		eventhandler.setLockedToWindow(lockToBeReset);	
	}	

}
