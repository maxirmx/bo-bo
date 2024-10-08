package org.webswing.dispatch;

import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.webswing.Constants;
import org.webswing.model.MsgIn;
import org.webswing.model.c2s.ActionEventMsgIn;
import org.webswing.model.c2s.ConnectionHandshakeMsgIn;
import org.webswing.model.c2s.CopyEventMsgIn;
import org.webswing.model.c2s.KeyboardEventMsgIn;
import org.webswing.model.c2s.MouseEventMsgIn;
import org.webswing.model.c2s.MouseEventMsgIn.MouseEventType;
import org.webswing.model.c2s.PasteEventMsgIn;
import org.webswing.model.c2s.SimpleEventMsgIn;
import org.webswing.model.c2s.UploadEventMsgIn;
import org.webswing.model.c2s.UploadedEventMsgIn;
import org.webswing.model.c2s.WindowEventMsgIn;
import org.webswing.model.internal.OpenFileResultMsgInternal;
import org.webswing.model.jslink.JSObjectMsg;
import org.webswing.toolkit.WebClipboard;
import org.webswing.toolkit.WebClipboardTransferable;
import org.webswing.toolkit.WebDragSourceContextPeer;
import org.webswing.toolkit.WebWindowPeer;
import org.webswing.toolkit.extra.DndEventHandler;
import org.webswing.toolkit.extra.WindowManager;
import org.webswing.toolkit.jslink.WebJSObject;
import org.webswing.toolkit.util.DeamonThreadFactory;
import org.webswing.toolkit.util.Logger;
import org.webswing.toolkit.util.Services;
import org.webswing.toolkit.util.Util;

import netscape.javascript.JSObject;
import sun.awt.CausedFocusEvent;
import sun.awt.UngrabEvent;

@SuppressWarnings("restriction")
public class WebEventDispatcher {

	private MouseEvent lastMouseEvent;
	private MouseEvent lastMousePressEvent;
	private long lastTime = 0;
	private long intervalTime = 0;
	private Point lastMousePosition = new Point();
	private Component lastEnteredWindow;
	private static final DndEventHandler dndHandler = new DndEventHandler();
	private HashMap<String, String> uploadMap = new HashMap<String, String>();
	private ExecutorService eventDispatcher = Executors.newSingleThreadExecutor(DeamonThreadFactory.getInstance());

	private static final Map<Integer, Integer> convertedKeyCodes = new HashMap<Integer, Integer>();
	static {
		convertedKeyCodes.put(45, 155);//	Insert
		convertedKeyCodes.put(46, 127);//	Delete
		convertedKeyCodes.put(189, 45);//	Minus
		convertedKeyCodes.put(187, 61);//	Equals
		convertedKeyCodes.put(219, 91);//	Open Bracket
		convertedKeyCodes.put(221, 93);//	Close Bracket
		convertedKeyCodes.put(186, 59);//	Semicolon
		convertedKeyCodes.put(220, 92);//	Back Slash
		convertedKeyCodes.put(226, 92);//	Back Slash
		convertedKeyCodes.put(188, 44);//	Comma
		convertedKeyCodes.put(190, 46);//	Period
		convertedKeyCodes.put(191, 47);//	Slash
	}

	//these keycodes are assigned to different keys in browser
	private static final List<Integer> nonStandardKeyCodes = Arrays.asList(KeyEvent.VK_KP_DOWN, KeyEvent.VK_KP_UP, KeyEvent.VK_KP_RIGHT, KeyEvent.VK_KP_LEFT);

	public void dispatchEvent(final MsgIn event) {
		Logger.debug("WebEventDispatcher.dispatchEvent:", event);
		eventDispatcher.submit(new Runnable() {

			@Override
			public void run() {
				if (event instanceof MouseEventMsgIn) {
					dispatchMouseEvent((MouseEventMsgIn) event);
				}
				if (event instanceof KeyboardEventMsgIn) {
					dispatchKeyboardEvent((KeyboardEventMsgIn) event);
				}
				if (event instanceof ConnectionHandshakeMsgIn) {
					final ConnectionHandshakeMsgIn handshake = (ConnectionHandshakeMsgIn) event;
					Util.getWebToolkit().initSize(handshake.getDesktopWidth(), handshake.getDesktopHeight());
					if (handshake.isApplet()) {
						// resize and refresh the applet object exposed in javascript in case of page reload/session continue
						Applet a = (Applet) WebJSObject.getJavaReference(System.getProperty(Constants.SWING_START_SYS_PROP_APPLET_CLASS));
						a.resize(handshake.getDesktopWidth(), handshake.getDesktopHeight());
						JSObject root = new WebJSObject(new JSObjectMsg("instanceObject"));
						root.setMember("applet", a);
					}
				}
				if (event instanceof SimpleEventMsgIn) {
					SimpleEventMsgIn msg = (SimpleEventMsgIn) event;
					dispatchMessage(msg);
				}
				if (event instanceof PasteEventMsgIn) {
					PasteEventMsgIn paste = (PasteEventMsgIn) event;
					handlePasteEvent(paste);
				}
				if (event instanceof CopyEventMsgIn) {
					CopyEventMsgIn copy = (CopyEventMsgIn) event;
					handleCopyEvent(copy);
				}
				if (event instanceof UploadedEventMsgIn) {
					handleUploadedEvent((UploadedEventMsgIn) event);
				}
				if (event instanceof UploadEventMsgIn) {
					UploadEventMsgIn upload = (UploadEventMsgIn) event;
					JFileChooser dialog = Util.getWebToolkit().getPaintDispatcher().getFileChooserDialog();
					if (dialog != null) {
						File currentDir = dialog.getCurrentDirectory();
						File tempFile = new File(upload.getTempFileLocation());
						String validfilename = Util.resolveFilename(currentDir, upload.getFileName());
						if (currentDir.canWrite() && tempFile.exists()) {
							try {
								Services.getImageService().moveFile(tempFile, new File(currentDir, validfilename));
								uploadMap.put(upload.getFileName(), validfilename);
							} catch (IOException e) {
								Logger.error("Error while moving uploaded file to target folder: ", e);
							}
						}
					}
				}
				if (event instanceof WindowEventMsgIn) {
					handleWindowEvent((WindowEventMsgIn) event);
				}
				if (event instanceof ActionEventMsgIn) {
					Util.getWebToolkit().processApiEvent(event);
				}
			}
		});
	}

	private void dispatchMessage(SimpleEventMsgIn message) {
		Logger.debug("WebEventDispatcher.dispatchMessage SimpleEventMsgIn type: ", message.getType());
		switch (message.getType()) {
		case killSwing:
			Logger.info("Received kill signal. Swing application shutting down.");
			Util.getWebToolkit().exitSwing(0);
			break;
		case deleteFile:
			Util.getWebToolkit().getPaintDispatcher().notifyDeleteSelectedFile();
			break;
		case downloadFile:
			Util.getWebToolkit().getPaintDispatcher().notifyDownloadSelectedFile();
			break;
		case paintAck:
			Util.getWebToolkit().getPaintDispatcher().clientReadyToReceive();
			break;
		case repaint:
			if (Util.isDD()) {
				Util.getWebToolkit().getPaintDispatcher().notifyBackgroundRepainted(new Rectangle(Util.getWebToolkit().getScreenSize()));
				Services.getDirectDrawService().resetCache();
				Util.repaintAllWindow();
			} else {
				Util.getWebToolkit().getPaintDispatcher().notifyWindowRepaintAll();
			}
			break;
		case unload:
			boolean instantExit = Integer.parseInt(System.getProperty(Constants.SWING_SESSION_TIMEOUT_SEC, "300")) == 0;
			if (instantExit) {
				Logger.warn("Exiting swing application. Client has disconnected from web session. (swingSessionTimeout setting is 0 or less)");
				Util.getWebToolkit().exitSwing(1);
			}
			break;
		case hb:
			break;
		}
	}

	private void dispatchKeyboardEvent(KeyboardEventMsgIn event) {
		Window w = (Window) WindowManager.getInstance().getActiveWindow();
		if (w != null) {
			long when = System.currentTimeMillis();
			int modifiers = Util.getKeyModifiersAWTFlag(event);
			int type = Util.getKeyType(event.getType());
			char character = Util.getKeyCharacter(event);
			Component src = w.getFocusOwner() == null ? w : w.getFocusOwner();
			// 此处为webswing的web转swing的特殊字符，整体上还缺很多，暂时规避对应关系如下:
			// https://docs.oracle.com/javase/7/docs/api/constant-values.html#java.awt.event.KeyEvent.VK_DECIMAL
			// https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/keyCode
			if (event.getKeycode() == 13) {// enter keycode
				event.setKeycode(10);
				character = 10;
			} else if (convertedKeyCodes.containsKey(event.getKeycode()) && type != KeyEvent.KEY_TYPED) {
				int converted = convertedKeyCodes.get(event.getKeycode());
				event.setKeycode(converted);
				character = (char) converted;
			} else if (nonStandardKeyCodes.contains(event.getKeycode())) {
				event.setKeycode(0);
			}
			if (type == KeyEvent.KEY_TYPED) {
				AWTEvent e = new KeyEvent(src, KeyEvent.KEY_TYPED, when, 0, 0, (char) event.getCharacter());
				dispatchEventInSwing(w, e);
			} else {
				AWTEvent e = Util.createKeyEvent(src, type, when, modifiers, event.getKeycode(), character, KeyEvent.KEY_LOCATION_STANDARD);
				dispatchEventInSwing(w, e);
				if (event.getKeycode() == 32 && event.getType() == KeyboardEventMsgIn.KeyEventType.keydown) {// space keycode handle press
					event.setType(KeyboardEventMsgIn.KeyEventType.keypress);
					dispatchKeyboardEvent(event);
				}
			}
		}
	}

	private void dispatchMouseEvent(MouseEventMsgIn event) {
		Component c = null;
		boolean relatedToLastEvent = false;
		if (WindowManager.getInstance().isLockedToWindowDecorationHandler()) {
			c = WindowManager.getInstance().getLockedToWindow();
			if(c != null && c.isShowing() == false)
			{
				WindowManager.getInstance().resetEventHandlerLock(false);
				c = null;
			}
		} else {
			c = WindowManager.getInstance().getVisibleComponentOnPosition(event.getX(), event.getY(), event.getWinId());
			if (lastMouseEvent != null && (lastMouseEvent.getID() == MouseEvent.MOUSE_DRAGGED || lastMouseEvent.getID() == MouseEvent.MOUSE_PRESSED) && ((event.getType() == MouseEventType.mousemove && event.getButton() == 1) || (event.getType() == MouseEventType.mouseup))) {
				c = (Component) lastMouseEvent.getSource();
				relatedToLastEvent=true;
			}
		}
		if (c == null) {
			if (Util.getWebToolkit().getPaintDispatcher() != null) {
				Util.getWebToolkit().getPaintDispatcher().notifyCursorUpdate(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR),null);
			}
			return;
		}
		if (c != null && c.isShowing()) {
			MouseEvent e = null;
			int x = (int) (event.getX() - c.getLocationOnScreen().getX());
			int y = (int) (event.getY() - c.getLocationOnScreen().getY());
			lastMousePosition.x = event.getX();
			lastMousePosition.y = event.getY();
			long when = System.currentTimeMillis();
			int modifiers = Util.getMouseModifiersAWTFlag(event);
			int id = 0;
			int clickcount = 0;
			int buttons = Util.getMouseButtonsAWTFlag(event.getButton());
			if (buttons != 0 && event.getType() == MouseEventType.mousedown) {
				Window w = (Window) (c instanceof Window ? c : SwingUtilities.windowForComponent(c));
				WindowManager.getInstance().activateWindow(w, null, x, y, false, true, CausedFocusEvent.Cause.MOUSE_EVENT);
				WindowManager.getInstance().flashTopModalDialog(w);
			}
			switch (event.getType()) {
			case mousemove:
				id = event.getButton() == 1 ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED;
				e = new MouseEvent(c, id, when, modifiers, x, y, event.getX(), event.getY(), clickcount, false, buttons);
				lastMouseEvent = e;
				dispatchEventInSwing(c, e,relatedToLastEvent);
				break;
			case mouseup:
				Logger.debug("webswing event : receive mouse up event");
				id = MouseEvent.MOUSE_RELEASED;
				boolean popupTrigger = (buttons == 3) ? true : false;
				clickcount = computeClickCount(x, y, buttons, false);
				modifiers = modifiers & (((1 << 6) - 1) | (~((1 << 14) - 1)) | MouseEvent.CTRL_DOWN_MASK | MouseEvent.ALT_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.META_DOWN_MASK);
				e = new MouseEvent(c, id, when, modifiers, x, y, event.getX(), event.getY(), clickcount, popupTrigger, buttons);
				if(event.getButton() == 0 && event.getX() == -1 && event.getY() == -1 && event.getType() == MouseEventType.mouseup)
				{
					Logger.debug("Event handling for mouse release event");
					dispatchEventInSwing(c, e,relatedToLastEvent);
					break;
				}
				dispatchEventInSwing(c, e);
				if (lastMousePressEvent != null && lastMousePressEvent.getX() == x && lastMousePressEvent.getY() == y) {
					e = new MouseEvent(c, MouseEvent.MOUSE_CLICKED, when, modifiers, x, y, event.getX(), event.getY(), clickcount, popupTrigger, buttons);
					dispatchEventInSwing(c, e);
					lastMouseEvent = e;
					lastMousePressEvent = e;
				} else {
					lastMouseEvent = e;
					lastMousePressEvent = e;
				}
				break;
			case mousedown:
				Logger.debug("webswing event : receive mouse down event");
				id = MouseEvent.MOUSE_PRESSED;
				clickcount = computeClickCount(x, y, buttons, true);
				e = new MouseEvent(c, id, when, modifiers, x, y, event.getX(), event.getY(), clickcount, false, buttons);
				dispatchEventInSwing(c, e);
				lastMousePressEvent = e;
				lastMouseEvent = e;
				break;
			case mousewheel:
				id = MouseEvent.MOUSE_WHEEL;
				buttons = 0;
				e = new MouseWheelEvent(c, id, when, modifiers, x, y, clickcount, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, event.getWheelDelta());
				dispatchEventInSwing(c, e);
				break;
			case dblclick:
			default:
				break;
			}

		}
	}

	private int computeClickCount(int x, int y, int buttons, boolean isPressed) {
		if (isPressed) {
			long currentTime = System.currentTimeMillis();
			intervalTime = currentTime - lastTime;
			if(intervalTime>500){
				lastTime=currentTime;
				return 1;
			}
			lastTime=currentTime;
			if (lastMousePressEvent != null && lastMousePressEvent.getID() == MouseEvent.MOUSE_CLICKED && lastMousePressEvent.getButton() == buttons && lastMousePressEvent.getX() == x && lastMousePressEvent.getY() == y) {
				return lastMousePressEvent.getClickCount() + 1;
			}
		} else {
			if (lastMousePressEvent != null && lastMousePressEvent.getID() == MouseEvent.MOUSE_PRESSED && lastMousePressEvent.getButton() == buttons) {
				return lastMousePressEvent.getClickCount();
			}
		}
		return 1;
	}

	private void handleCopyEvent(final CopyEventMsgIn copy) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (copy.getType() != null) {
					switch (copy.getType()) {
					case copy:
						dispatchCopyEvent();
						break;
					case cut:
						dispatchCutEvent();
						break;
					case getFileFromClipboard:
						handleClipboardFileDownload(copy);
					}
				}
			}
		});
	}

	private void handleClipboardFileDownload(CopyEventMsgIn copy) {
		if (Boolean.getBoolean(Constants.SWING_START_SYS_PROP_ALLOW_DOWNLOAD)) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
				try {
					List<?> files = (List<?>) clipboard.getData(DataFlavor.javaFileListFlavor);
					for (Object o : files) {
						File file = (File) o;
						if (file.getAbsolutePath().equals(copy.getFile())) {
							if (file != null && file.exists() && !file.isDirectory() && file.canRead()) {
								OpenFileResultMsgInternal f = new OpenFileResultMsgInternal();
								f.setClientId(System.getProperty(Constants.SWING_START_SYS_PROP_CLIENT_ID));
								f.setFile(file);
								Util.getWebToolkit().getPaintDispatcher().sendObject(f);
							} else {
								Logger.error("Failed to download file " + copy.getFile() + " from clipboard. File is not accessible or is a directory");
							}
						}
					}
				} catch (Exception e) {
					Logger.error("Failed to download file " + copy.getFile() + " from clipboard.", e);
				}
			}
		}
	}

	private void handlePasteEvent(final PasteEventMsgIn paste) {
		Logger.debug("WebEventDispatcher.handlePasteEvent", paste);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				WebClipboardTransferable transferable = new WebClipboardTransferable(paste);
				if (!transferable.isEmpty()) {
					WebClipboard wc = (WebClipboard) Util.getWebToolkit().getSystemClipboard();
					wc.setContents(transferable);
				}
				WebEventDispatcher.this.dispatchPasteEvent();
			}
		});
	}

	private void dispatchPasteEvent() {
		KeyboardEventMsgIn event = new KeyboardEventMsgIn();
		event.setType(KeyboardEventMsgIn.KeyEventType.keydown);
		event.setCharacter(KeyEvent.VK_V);
		event.setKeycode(KeyEvent.VK_V);// 'v'
		event.setCtrl(true);
		dispatchKeyboardEvent(event);
		event.setType(KeyboardEventMsgIn.KeyEventType.keyup);
		dispatchKeyboardEvent(event);
	}

	private void dispatchCopyEvent() {
		KeyboardEventMsgIn event = new KeyboardEventMsgIn();
		event.setType(KeyboardEventMsgIn.KeyEventType.keydown);
		event.setCharacter(KeyEvent.VK_C); // 'c'
		event.setKeycode(KeyEvent.VK_C);
		event.setCtrl(true);
		dispatchKeyboardEvent(event);
		event.setType(KeyboardEventMsgIn.KeyEventType.keyup);
		dispatchKeyboardEvent(event);
	}

	private void dispatchCutEvent() {
		KeyboardEventMsgIn event = new KeyboardEventMsgIn();
		event.setType(KeyboardEventMsgIn.KeyEventType.keydown);
		event.setCharacter(KeyEvent.VK_X);
		event.setKeycode(KeyEvent.VK_X);// 'x'
		event.setCtrl(true);
		dispatchKeyboardEvent(event);
		event.setType(KeyboardEventMsgIn.KeyEventType.keyup);
		dispatchKeyboardEvent(event);
	}

	public Point getLastMousePosition() {
		return lastMousePosition;
	}
	
	protected Component getLastEnteredWindow() {
		return lastEnteredWindow;
	}

	protected void setLastEnteredWindow(Component lastEnteredWindow) {
		this.lastEnteredWindow = lastEnteredWindow;
	}

	public void dragStart(WebDragSourceContextPeer peer, Transferable transferable, int actions, long[] formats) {
		dndHandler.dragStart(peer, transferable, actions, formats);
	}

	public void dispatchEventInSwing(final Component c, final AWTEvent e) {
		dispatchEventInSwing(c, e, false);
	}

	public void dispatchEventInSwing(final Component c, final AWTEvent e, boolean relatedToLastEvent) {
		Window w = (Window) (c instanceof Window ? c : SwingUtilities.windowForComponent(c));
		if(w.isEnabled()) {

			if (e instanceof MouseEvent) {
				w.setCursor(w.getCursor());// force cursor update
			}
			if (((!relatedToLastEvent && Util.isWindowDecorationEvent(w, e)) || WindowManager.getInstance().isLockedToWindowDecorationHandler()) && e instanceof MouseEvent) {
				Logger.debug("WebEventDispatcher.dispatchEventInSwing:windowManagerHandle", e);
				if(e.getID()==MouseEvent.MOUSE_PRESSED) {
					Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new UngrabEvent(c));
				}
				WindowManager.getInstance().handleWindowDecorationEvent(w, (MouseEvent) e);
			} else if (dndHandler.isDndInProgress() && (e instanceof MouseEvent || e instanceof KeyEvent)) {
				dndHandler.processMouseEvent(w, e);
			} else {
				Logger.debug("WebEventDispatcher.dispatchEventInSwing:postSystemQueue", e);
				dispatchEnterExitEvents(w, e);
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
			}
		}
	}
	
	private void dispatchEnterExitEvents(Window w, AWTEvent e) {
		if (e instanceof MouseEvent && getLastEnteredWindow() != w) {
			MouseEvent oe = (MouseEvent) e;
			dispatchExitEvent(oe.getWhen(), oe.getModifiersEx() | oe.getModifiers(), oe.getX(), oe.getY(), oe.getXOnScreen(), oe.getYOnScreen());
			if (w != null) {
				MouseEvent enterEvent = new MouseEvent(w, MouseEvent.MOUSE_ENTERED, oe.getWhen(), oe.getModifiersEx() | oe.getModifiers(), oe.getX(), oe.getY(), oe.getXOnScreen(), oe.getYOnScreen(), oe.getClickCount(), oe.isPopupTrigger(), oe.getButton());
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(enterEvent);
			}
			setLastEnteredWindow(w);
		}
	}

	protected void dispatchExitEvent(long when, int mod, int x, int y, int absX, int absY) {
		if (getLastEnteredWindow() != null && getLastEnteredWindow().isShowing()) {
			MouseEvent exitEvent = new MouseEvent(getLastEnteredWindow(), MouseEvent.MOUSE_EXITED, when, mod, x, y, absX, absY, 0, false, 0);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(exitEvent);
			setLastEnteredWindow(null);
		}
	}

	private void handleUploadedEvent(UploadedEventMsgIn e) {
		if (Boolean.getBoolean(Constants.SWING_START_SYS_PROP_ALLOW_UPLOAD)) {
			JFileChooser fc = Util.getWebToolkit().getPaintDispatcher().getFileChooserDialog();
			UploadedEventMsgIn event = (UploadedEventMsgIn) e;
			if (fc != null) {
				fc.rescanCurrentDirectory();
				if (event.getFiles().size() > 0) {
					if (fc.isMultiSelectionEnabled()) {
						List<File> arr = new ArrayList<File>();
						for (int i = 0; i < event.getFiles().size(); i++) {
							if (uploadMap.get(event.getFiles().get(i)) != null) {
								arr.add(new File(fc.getCurrentDirectory(), uploadMap.get(event.getFiles().get(i))));
							}
						}
						fc.setSelectedFiles(arr.toArray(new File[arr.size()]));
					} else {
						if (uploadMap.get(event.getFiles().get(0)) != null) {
							File f = new File(fc.getCurrentDirectory(), uploadMap.get(event.getFiles().get(0)));
							fc.setSelectedFile(f);
						}
					}
					// fc.approveSelection();
				} else {
					fc.cancelSelection();
				}
			}
			uploadMap.clear();
		}
	}

	public void handleWindowEvent(WindowEventMsgIn windowUpdate) {
		WebWindowPeer winPeer = Util.findWindowPeerById(windowUpdate.getId());

		if (winPeer == null) {
			return;
		}
		
		synchronized (Util.getWebToolkit().getTreeLock()) {
			synchronized (WebPaintDispatcher.webPaintLock) {
				final Window win = ((Window) winPeer.getTarget());
				if (windowUpdate.isClose()) {
					if (winPeer.getTarget() instanceof Window) {
						win.setVisible(false);
					}
				} else {
					SwingUtilities.invokeLater(()->{
						win.setBounds(windowUpdate.getX(), windowUpdate.getY(), windowUpdate.getWidth(), windowUpdate.getHeight());
						WindowManager.getInstance().requestRepaintAfterMove(win, new Rectangle(windowUpdate.getX(), windowUpdate.getY(), windowUpdate.getWidth(), windowUpdate.getY()));
					});
				}
			}
		}
	}
	
	public static boolean isDndInProgress() {
		return dndHandler.isDndInProgress();
	}
}
