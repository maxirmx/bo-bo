package org.webswing.dispatch;

import org.webswing.Constants;
import org.webswing.model.internal.ExitMsgInternal;
import org.webswing.model.internal.OpenFileResultMsgInternal;
import org.webswing.model.s2c.AppFrameMsgOut;
import org.webswing.model.s2c.CopyEventMsg;
import org.webswing.model.s2c.CursorChangeEventMsg;
import org.webswing.model.s2c.FileDialogEventMsg;
import org.webswing.model.s2c.FileDialogEventMsg.FileDialogEventType;
import org.webswing.model.s2c.FocusEventMsg;
import org.webswing.model.s2c.LinkActionMsg;
import org.webswing.model.s2c.LinkActionMsg.LinkActionType;
import org.webswing.model.s2c.WindowMoveActionMsg;
import org.webswing.model.s2c.WindowMsg;
import org.webswing.toolkit.WebCursor;
import org.webswing.toolkit.WebToolkit;
import org.webswing.toolkit.WebWindowPeer;
import org.webswing.toolkit.extra.WebRepaintManager;
import org.webswing.toolkit.extra.WindowManager;
import org.webswing.toolkit.util.DeamonThreadFactory;
import org.webswing.toolkit.util.Logger;
import org.webswing.toolkit.util.Services;
import org.webswing.toolkit.util.Util;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebPaintDispatcher {

	public static final Object webPaintLock = new Object();

	private volatile Map<String, Set<Rectangle>> areasToUpdate = new HashMap<String, Set<Rectangle>>();

	private volatile Map<String, Boolean> windowRendered = new HashMap<String, Boolean>();
	private volatile WindowMoveActionMsg moveAction;
	private volatile boolean clientReadyToReceive = true;
	private volatile FocusEventMsg focusEvent;
	private long lastReadyStateTime;
	private JFileChooser fileChooserDialog;

	private ScheduledExecutorService contentSender = Executors.newScheduledThreadPool(1,DeamonThreadFactory.getInstance());

	private Map<String, BufferedImage> previousWindowImages = new HashMap<String, BufferedImage>();

	public WebPaintDispatcher() {
		Runnable sendUpdate = new Runnable() {

			public void run() {
				try {
					AppFrameMsgOut json;
					Map<String, Map<Integer, BufferedImage>> windowImages = null;
					Map<String, Image> windowWebImages = null;
					Map<String, List<Rectangle>> windowNonVisibleAreas;
					Map<String, Set<Rectangle>> currentAreasToUpdate = null;
					long start= new Date().getTime();
					synchronized (Util.getWebToolkit().getTreeLock()) {
						synchronized (webPaintLock) {
							if (clientReadyToReceive) {
								if (RepaintManager.currentManager(null) instanceof WebRepaintManager) {
									((WebRepaintManager) RepaintManager.currentManager(null)).process();
								}
								lastReadyStateTime = System.currentTimeMillis();
							}
							if ((areasToUpdate.size() == 0 && moveAction == null) || !clientReadyToReceive) {
								if (!clientReadyToReceive && (System.currentTimeMillis() - lastReadyStateTime) > 2000) {
									Logger.info("contentSender.readyToReceive re-enabled after timeout");
									if (Util.isDD()) {
										Services.getDirectDrawService().resetCache();
									}
									clientReadyToReceive = true;
								}
								return;
							}
							currentAreasToUpdate = areasToUpdate;
							areasToUpdate = Util.postponeNonShowingAreas(currentAreasToUpdate,windowRendered);
							if (currentAreasToUpdate.size() == 0 && moveAction == null) {
								return;
							}
							windowNonVisibleAreas = WindowManager.getInstance().extractNonVisibleAreas();
							json = Util.fillJsonWithWindowsData(currentAreasToUpdate, windowNonVisibleAreas);
							if (Util.isDD()) {
								windowWebImages = new HashMap<String, Image>();
								windowWebImages = Util.extractWindowWebImages(json, windowWebImages);
							} else {
								windowImages = new HashMap<String, Map<Integer, BufferedImage>>();
								windowImages = Util.extractWindowImages(json, windowImages);
							}
							if (moveAction != null) {
								json.setMoveAction(moveAction);
								moveAction = null;
							}
							if (focusEvent!= null) {
								json.setFocusEvent(focusEvent);
								focusEvent = null;
							}
							clientReadyToReceive = false;
						}
					}
					if (Util.isDD()) {
						Util.encodeWindowWebImages(windowWebImages, json);
					} else {
						Logger.trace("contentSender:pngEncodingStart", json.hashCode());
						Util.encodeWindowImages(windowImages, json);
						Logger.trace("contentSender:pngEncodingDone", json.hashCode());
					}
					Services.getConnectionService().sendObject(json);
					Logger.trace("frame processed "+ (new Date().getTime()-start)+"ms");

				} catch (Exception e) {
					Logger.error("contentSender:error", e);
				}
			}

		};
		contentSender.scheduleWithFixedDelay(sendUpdate, 33, 33, TimeUnit.MILLISECONDS);
	}

	public void clientReadyToReceive() {
		synchronized (webPaintLock) {
			clientReadyToReceive = true;
			Logger.trace("clientReadyToReceive after "+ (new Date().getTime()-lastReadyStateTime)+"ms");
		}
	}

	public void sendObject(Serializable object) {
		Logger.info("WebPaintDispatcher:sendJsonObject", object);
		Services.getConnectionService().sendObject(object);
	}

	public void notifyWindowAreaRepaintedForced(String guid, Rectangle r) {
		BufferedImage previous = previousWindowImages.get(guid);
		if(previous!=null) {
			Graphics2D g = previous.createGraphics();
			g.clearRect(r.x, r.y, r.width, r.height);
			g.dispose();
		}
		notifyWindowAreaRepainted(guid, r);
	}

	
	public void notifyWindowAreaRepainted(String guid, Rectangle repaintedArea) {
		synchronized (webPaintLock) {
			if (validBounds(repaintedArea)) {
				if (areasToUpdate.containsKey(guid)) {
					Set<Rectangle> rset = areasToUpdate.get(guid);
					rset.add(repaintedArea);
				} else {
					Set<Rectangle> rset = new HashSet<Rectangle>();
					rset.add(repaintedArea);
					areasToUpdate.put(guid, rset);
				}
				Logger.trace("WebPaintDispatcher:notifyWindowAreaRepainted", guid, repaintedArea);
			}
		}
	}

	public void notifyWindowBoundsChanged(String guid, Rectangle newBounds) {
		synchronized (webPaintLock) {
			if (validBounds(newBounds)) {
				Set<Rectangle> rset;
				if (areasToUpdate.containsKey(guid)) {
					rset = areasToUpdate.get(guid);
					rset.clear();
				} else {
					rset = new HashSet<Rectangle>();
					areasToUpdate.put(guid, rset);
				}
				rset.add(newBounds);
				Logger.trace("WebPaintDispatcher:notifyWindowBoundsChanged", guid, newBounds);
			}
		}
	}

	private boolean validBounds(Rectangle newBounds) {
		if (newBounds.width > 0 && newBounds.height > 0) {
			return true;
		} else {
			return false;
		}
	}

	public void notifyWindowClosed(String guid) {
		synchronized (webPaintLock) {
			areasToUpdate.remove(guid);
		}
		previousWindowImages.remove(guid);
		AppFrameMsgOut f = new AppFrameMsgOut();
		WindowMsg fdEvent = new WindowMsg();
		fdEvent.setId(guid);
		f.setClosedWindow(fdEvent);
		Logger.info("WebPaintDispatcher:notifyWindowClosed", guid);
		Services.getConnectionService().sendObject(f);
		
	}

	public void notifyWindowRepaint(Window w) {
		Rectangle bounds = w.getBounds();
		WebWindowPeer peer = (WebWindowPeer) WebToolkit.targetToPeer(w);
		notifyWindowAreaRepainted(peer.getGuid(), new Rectangle(0, 0, bounds.width, bounds.height));
		previousWindowImages.remove(peer.getGuid());
	}

	@SuppressWarnings("restriction")
	public void notifyWindowRepaintAll() {
		notifyBackgroundRepainted( new Rectangle(Util.getWebToolkit().getScreenSize()));
		for (Window w : Window.getWindows()) {
			if (w.isShowing()) {
				notifyWindowRepaint(w);
			}
		}
	}

	public void notifyBackgroundRepainted(Rectangle toRepaint) {
		notifyWindowAreaRepainted(WebToolkit.BACKGROUND_WINDOW_ID, toRepaint);
	}

	public void notifyOpenLinkAction(URI uri) {
		AppFrameMsgOut f = new AppFrameMsgOut();
		LinkActionMsg linkAction = new LinkActionMsg(LinkActionType.url, uri.toString());
		f.setLinkAction(linkAction);
		Logger.info("WebPaintDispatcher:notifyOpenLinkAction", uri);
		Services.getConnectionService().sendObject(f);
	}

	@SuppressWarnings("restriction")
	public void resetWindowsPosition(int oldWidht, int oldHeight) {
		for (Window w : Window.getWindows()) {
			WebWindowPeer peer = (WebWindowPeer) WebToolkit.targetToPeer(w);
			if (peer != null) {
				Rectangle b = w.getBounds();
				Dimension current = Util.getWebToolkit().getScreenSize();

				if (peer.getTarget() instanceof JFrame) {
					JFrame frame = (JFrame) peer.getTarget();
					//maximized window - auto resize
					if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
						w.setLocation(0, 0);
						w.setBounds(0, 0, current.width, current.height);
					}
				} else {
					// move the window position to same position relative to
					// previous size;
					if (oldWidht != 0 && oldHeight != 0) {
						int xCenterWinPoint = b.x + (b.width / 2);
						int yCenterWinPoint = b.y + (b.height / 2);
						boolean xCenterValid = b.width < oldWidht;
						boolean yCenterValid = b.height < oldHeight;
						double xrelative = (double) xCenterWinPoint / (double) oldWidht;
						double yrelative = (double) yCenterWinPoint / (double) oldHeight;
						int xCenterCurrent = (int) (current.width * xrelative);
						int yCenterCurrent = (int) (current.height * yrelative);
						int newx = xCenterCurrent - (b.width / 2);
						int newy = yCenterCurrent - (b.height / 2);
						if (xCenterValid || newx < b.x) {
							b.x = newx >= 0 ? newx : 0;
						}
						if (yCenterValid || newy < b.y) {
							b.y = newy >= 0 ? newy : 0;
						}
						w.setLocation(b.x, b.y);
					}
					peer.setBounds(b.x, b.y, b.width, b.height, 0);
				}
			}
		}
	}

	public void notifyWindowMoved(Window w, Rectangle from, Rectangle to) {
		synchronized (webPaintLock) {
			if (moveAction == null) {
				moveAction = new WindowMoveActionMsg(from.x, from.y, to.x, to.y, from.width, from.height);
				notifyRepaintOffScreenAreas(w, moveAction);
			} else if (moveAction.getDx() == from.x && moveAction.getDy() == from.y && moveAction.getWidth() == from.width && moveAction.getHeight() == from.height) {
				moveAction.setDx(to.x);
				moveAction.setDy(to.y);
				notifyRepaintOffScreenAreas(w, moveAction);
			} else {
				notifyWindowRepaint(w);
			}
		}
	}

	@SuppressWarnings("restriction")
	private void notifyRepaintOffScreenAreas(Window w, WindowMoveActionMsg m) {
		Rectangle screen = new Rectangle(Util.getWebToolkit().getScreenSize());
		Rectangle before = new Rectangle(m.getSx(), m.getSy(), m.getWidth(), m.getHeight());
		Rectangle after = new Rectangle(m.getDx(), m.getDy(), m.getWidth(), m.getHeight());
		int xdiff = m.getSx() - m.getDx();
		int ydiff = m.getSy() - m.getDy();
		Rectangle[] invisibleBefore = SwingUtilities.computeDifference(before, screen);
		if (invisibleBefore.length != 0) {
			for (Rectangle r : invisibleBefore) {
				r.setLocation(r.x - xdiff, r.y - ydiff);
			}
			Rectangle[] invisibleAfter = SwingUtilities.computeDifference(after, screen);
			List<Rectangle> toRepaint = Util.joinRectangles(Util.getGrid(Arrays.asList(invisibleBefore), Arrays.asList(invisibleAfter)));
			WebWindowPeer peer = (WebWindowPeer) WebToolkit.targetToPeer(w);
			for (Rectangle r : toRepaint) {
				r.setLocation(r.x - w.getX(), r.y - w.getY());
				notifyWindowAreaRepainted(peer.getGuid(), r);
			}
			previousWindowImages.remove(peer.getGuid());
		}
	}

	public void notifyCursorUpdate(Cursor cursor) {
		notifyCursorUpdate(cursor, null);
	}

	public void notifyCursorUpdate(Cursor cursor, Cursor overridenCursorName) {
		String webcursorName = null;
		Cursor webcursor = null;
		if (overridenCursorName == null) {
			switch (cursor.getType()) {
			case Cursor.DEFAULT_CURSOR:
				webcursorName = CursorChangeEventMsg.DEFAULT_CURSOR;
				break;
			case Cursor.HAND_CURSOR:
				webcursorName = CursorChangeEventMsg.HAND_CURSOR;
				break;
			case Cursor.CROSSHAIR_CURSOR:
				webcursorName = CursorChangeEventMsg.CROSSHAIR_CURSOR;
				break;
			case Cursor.MOVE_CURSOR:
				webcursorName = CursorChangeEventMsg.MOVE_CURSOR;
				break;
			case Cursor.TEXT_CURSOR:
				webcursorName = CursorChangeEventMsg.TEXT_CURSOR;
				break;
			case Cursor.WAIT_CURSOR:
				webcursorName = CursorChangeEventMsg.WAIT_CURSOR;
				break;
			case Cursor.E_RESIZE_CURSOR:
			case Cursor.W_RESIZE_CURSOR:
				webcursorName = CursorChangeEventMsg.EW_RESIZE_CURSOR;
				break;
			case Cursor.N_RESIZE_CURSOR:
			case Cursor.S_RESIZE_CURSOR:
				webcursorName = CursorChangeEventMsg.NS_RESIZE_CURSOR;
				break;
			case Cursor.NW_RESIZE_CURSOR:
			case Cursor.SE_RESIZE_CURSOR:
				webcursorName = CursorChangeEventMsg.BACKSLASH_RESIZE_CURSOR;
				break;
			case Cursor.NE_RESIZE_CURSOR:
			case Cursor.SW_RESIZE_CURSOR:
				webcursorName = CursorChangeEventMsg.SLASH_RESIZE_CURSOR;
				break;
			case Cursor.CUSTOM_CURSOR:
				webcursorName = cursor.getName();
				break;
			default:
				webcursorName = CursorChangeEventMsg.DEFAULT_CURSOR;
			}
			webcursor = cursor;
		} else {
			webcursor = overridenCursorName;
			webcursorName = overridenCursorName.getName();
		}
		if (!WindowManager.getInstance().getCurrentCursor().equals(webcursorName)) {
			AppFrameMsgOut f = new AppFrameMsgOut();
			CursorChangeEventMsg cursorChange = new CursorChangeEventMsg(webcursorName);
			if (webcursor instanceof WebCursor) {
				WebCursor c = (WebCursor) webcursor;
				BufferedImage img = c.getImage();
				cursorChange.setB64img(Services.getImageService().getPngImage(img));
				cursorChange.setX(c.getHotSpot() != null ? c.getHotSpot().x : 0);
				cursorChange.setY(c.getHotSpot() != null ? c.getHotSpot().y : 0);
			}
			f.setCursorChange(cursorChange);
			WindowManager.getInstance().setCurrentCursor(webcursorName);
			Logger.debug("WebPaintDispatcher:notifyCursorUpdate", f);
			Services.getConnectionService().sendObject(f);
		}
	}

	public void notifyCopyEvent(String content, String html, byte[] img, List<String> files, boolean other) {
		AppFrameMsgOut f = new AppFrameMsgOut();
		CopyEventMsg copyEvent;
		copyEvent = new CopyEventMsg(content, html, img, files, other);
		f.setCopyEvent(copyEvent);
		Logger.debug("WebPaintDispatcher:notifyCopyEvent", f);
		Services.getConnectionService().sendObject(f);
	}

	public void notifyFileDialogActive(WebWindowPeer webWindowPeer) {
		AppFrameMsgOut f = new AppFrameMsgOut();
		FileDialogEventMsg fdEvent = new FileDialogEventMsg();
		fdEvent.setEventType(FileDialogEventType.Open);
		f.setFileDialogEvent(fdEvent);
		Logger.info("WebPaintDispatcher:notifyFileTransferBarActive", f);
		fileChooserDialog = Util.discoverFileChooser(webWindowPeer);
		fdEvent.addFilter(fileChooserDialog.getChoosableFileFilters());
		fdEvent.setMultiSelection(fileChooserDialog.isMultiSelectionEnabled());
		Services.getConnectionService().sendObject(f);
	}

	public void notifyFileDialogHidden(WebWindowPeer webWindowPeer) {
		AppFrameMsgOut f = new AppFrameMsgOut();
		FileDialogEventMsg fdEvent = new FileDialogEventMsg();
		fdEvent.setEventType(FileDialogEventType.Close);
		f.setFileDialogEvent(fdEvent);
		Logger.info("WebPaintDispatcher:notifyFileTransferBarActive", f);

		if (Boolean.getBoolean(Constants.SWING_START_SYS_PROP_ALLOW_AUTO_DOWNLOAD)) {
			if (fileChooserDialog != null && fileChooserDialog.getDialogType() == JFileChooser.SAVE_DIALOG) {
				try {
					Field resultValueField = JFileChooser.class.getDeclaredField("returnValue");
					resultValueField.setAccessible(true);
					if (resultValueField.get(fileChooserDialog).equals(JFileChooser.APPROVE_OPTION)) {
						File saveFile = fileChooserDialog.getSelectedFile();
						if (saveFile != null) {
							OpenFileResultMsgInternal msg = new OpenFileResultMsgInternal();
							msg.setClientId(System.getProperty(Constants.SWING_START_SYS_PROP_CLIENT_ID));
							msg.setFile(saveFile);
							msg.setWaitForFile(true);
							if (saveFile.exists()) {
								msg.setOverwriteDetails(saveFile.length() + "|" + saveFile.lastModified());
							}
							Util.getWebToolkit().getPaintDispatcher().sendObject(msg);
						}
					}
				} catch (Exception e) {
					Logger.warn("Save file dialog's file monitoring failed: " + e.getMessage());
				}
			}
		}
		fileChooserDialog = null;
		Services.getConnectionService().sendObject(f);
	}

	public JFileChooser getFileChooserDialog() {
		return fileChooserDialog;
	}

	public void notifyDownloadSelectedFile() {
		if (fileChooserDialog != null && Boolean.getBoolean(Constants.SWING_START_SYS_PROP_ALLOW_DOWNLOAD)) {
			File file = fileChooserDialog.getSelectedFile();
			if (file != null && file.exists() && !file.isDirectory() && file.canRead()) {
				OpenFileResultMsgInternal f = new OpenFileResultMsgInternal();
				f.setClientId(System.getProperty(Constants.SWING_START_SYS_PROP_CLIENT_ID));
				f.setFile(file);
				Util.getWebToolkit().getPaintDispatcher().sendObject(f);
			}
		}
	}

	public void notifyDeleteSelectedFile() {
		if (fileChooserDialog != null && Boolean.getBoolean(Constants.SWING_START_SYS_PROP_ALLOW_DELETE)) {
			File[] selected = fileChooserDialog.getSelectedFiles();
			if ((selected == null || selected.length == 0) && fileChooserDialog.getSelectedFile() != null) {
				selected = new File[] { fileChooserDialog.getSelectedFile() };
			}
			for (File f : selected) {
				if (f.exists() && f.canWrite()) {
					f.delete();
				}
			}
			fileChooserDialog.rescanCurrentDirectory();
		}
	}

	public void notifyApplicationExiting() {
		ExitMsgInternal f=new ExitMsgInternal();
		f.setWaitForExit(Integer.getInteger(Constants.SWING_START_SYS_PROP_WAIT_FOR_EXIT,60000));
		Services.getConnectionService().sendObject(f);
		contentSender.shutdownNow();
	}

	public void notifyFocusEvent(FocusEventMsg msg) {
		focusEvent=msg;
	}

	public void notifyWindowReset(String guid) {
		windowRendered.put(guid,false);
	}

	public void notifyWindowDisposed(String guid) {
		windowRendered.remove(guid);
	}

	public void notifyWindowRendered(String guid) {
		windowRendered.put(guid,true);
	}
}
