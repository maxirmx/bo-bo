package org.webswing.toolkit;

import java.awt.*;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.lang.reflect.Method;

import org.webswing.model.s2c.FocusEventMsg;
import org.webswing.toolkit.extra.WindowManager;
import org.webswing.toolkit.util.Logger;

import org.webswing.toolkit.util.Util;
import sun.awt.CausedFocusEvent;
import sun.awt.SunToolkit;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

@SuppressWarnings("restriction")
public class WebKeyboardFocusManagerPeer implements KeyboardFocusManagerPeer {

	private static CaretListener caretListener = new CaretListener() {
		@Override
		public void caretUpdate(CaretEvent e) {
			Util.getWebToolkit().getPaintDispatcher().notifyFocusEvent(getFocusEvent());
		}
	};

	@Override
	public void clearGlobalFocusOwner(Window activeWindow) {
	}

	@Override
	public Component getCurrentFocusOwner() {
		return WindowManager.getInstance().getActiveWindow().getFocusOwner();
	}

	@Override
	public Window getCurrentFocusedWindow() {
		return WindowManager.getInstance().getActiveWindow();
	}

	@Override
	public void setCurrentFocusOwner(Component comp) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Component o = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				if (o instanceof JTextComponent) {
					JTextComponent tc = (JTextComponent) o;
					tc.removeCaretListener(caretListener);
					tc.addCaretListener(caretListener);
				}
				Util.getWebToolkit().getPaintDispatcher().notifyFocusEvent(getFocusEvent());
			}
		});
	}

	private static FocusEventMsg getFocusEvent(){
		Component o = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		FocusEventMsg msg = new FocusEventMsg();
		if (o != null && o.isShowing()) {
			msg.setType(FocusEventMsg.FocusEventType.focusGained);
			Point l = o.getLocationOnScreen();
			msg.setX(l.x);
			msg.setY(l.y);
			Rectangle b = o.getBounds();
			msg.setW(b.width);
			msg.setH(b.height);
			if (o instanceof JTextComponent) {
				JTextComponent tc = (JTextComponent) o;
				int position = tc.getCaretPosition();
				try {
					Rectangle location = tc.modelToView(position);
					if (location != null) {
						msg.setType(FocusEventMsg.FocusEventType.focusWithCarretGained);
						msg.setCaretX(location.x);
						msg.setCaretY(location.y);
						msg.setCaretH(location.height);
					}
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		} else {
			msg.setType(FocusEventMsg.FocusEventType.focusLost);
		}
		return msg;
	}

	@Override
	public void setCurrentFocusedWindow(Window win) {

	}

	public static int shouldNativelyFocusHeavyweight(Window heavyweight, Component descendant, boolean temporary, boolean focusedWindowChangeAllowed, long time, CausedFocusEvent.Cause cause) {
		try {
			Method m2 = KeyboardFocusManager.class.getDeclaredMethod("shouldNativelyFocusHeavyweight", Component.class, Component.class, Boolean.TYPE, Boolean.TYPE, Long.TYPE, CausedFocusEvent.Cause.class);
			m2.setAccessible(true);
			Integer result2 = (Integer) m2.invoke(null, heavyweight, descendant, temporary, focusedWindowChangeAllowed, time, cause);
			return result2;
		} catch (Exception e) {
			Logger.debug("Failed to invoke processSynchronousLightweightTransfer on KeyboardFocusManager. Check your java version.", e);
			return 0;
		}
	}

	@SuppressWarnings("deprecation")
	public static boolean deliverFocus(Component heavyweight, Component descendant, boolean temporary, boolean focusedWindowChangeAllowed, long time, CausedFocusEvent.Cause cause) {
		if (heavyweight == null) {
			heavyweight = descendant;
		}

		Component c = WindowManager.getInstance().getActiveWindow().getFocusOwner();
		CausedFocusEvent focusEvent;
		if ((c != null) && (c.getPeer() == null)) {
			c = null;
		}
		if (c != null) {
			focusEvent = new CausedFocusEvent(c, CausedFocusEvent.FOCUS_LOST, false, heavyweight, cause);

			SunToolkit.postEvent(SunToolkit.targetToAppContext(c), focusEvent);
		}

		focusEvent = new CausedFocusEvent(heavyweight, CausedFocusEvent.FOCUS_GAINED, false, c, cause);

		SunToolkit.postEvent(SunToolkit.targetToAppContext(heavyweight), focusEvent);
		return true;
	}

}
