package org.webswing.toolkit.util;

import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.webswing.Constants;
import org.webswing.component.HtmlPanelImpl;
import org.webswing.dispatch.WebPaintDispatcher;
import org.webswing.model.c2s.KeyboardEventMsgIn;
import org.webswing.model.c2s.KeyboardEventMsgIn.KeyEventType;
import org.webswing.model.c2s.MouseEventMsgIn;
import org.webswing.model.s2c.AppFrameMsgOut;
import org.webswing.model.s2c.WindowMsg;
import org.webswing.model.s2c.WindowMsg.WindowType;
import org.webswing.model.s2c.WindowPartialContentMsg;
import org.webswing.toolkit.WebComponentPeer;
import org.webswing.toolkit.WebToolkit;
import org.webswing.toolkit.WebWindowPeer;
import org.webswing.toolkit.api.component.HtmlPanel;
import org.webswing.toolkit.extra.WindowManager;

public class Util {

	private static List<Integer> NO_CHAR_KEY_CODES = Arrays.asList(KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4, KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8, KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12, KeyEvent.VK_PRINTSCREEN, KeyEvent.VK_SCROLL_LOCK, KeyEvent.VK_PAUSE, KeyEvent.VK_INSERT,
			KeyEvent.VK_HOME, KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_END, KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_WINDOWS, KeyEvent.VK_ALT_GRAPH);

	private static final double MAX_SLICE_SIZE = Integer.getInteger("webswing.maxSliceSize", 550); /*optimal slice size will split he full screen to N parts, where N is the number of cpu cores. 550 is optimal for fullHD with 8cpu cores */

	public static int getMouseButtonsAWTFlag(int button) {
		switch (button) {
		case 1:
			return MouseEvent.BUTTON1;
		case 2:
			return MouseEvent.BUTTON2;
		case 3:
			return MouseEvent.BUTTON3;
		case 0:
			return MouseEvent.NOBUTTON;
		}
		return 0;
	}

	public static int getMouseModifiersAWTFlag(MouseEventMsgIn evt) {
		int result = 0;
		switch (evt.getButton()) {
		case 1:
			result = MouseEvent.BUTTON1_DOWN_MASK;
			break;
		case 2:
			result = MouseEvent.BUTTON2_DOWN_MASK;
			break;
		case 3:
			result = MouseEvent.BUTTON3_DOWN_MASK;
			break;
		}
		if (evt.isCtrl()) {
			result = result | MouseEvent.CTRL_DOWN_MASK;
		}
		if (evt.isAlt()) {
			result = result | MouseEvent.ALT_DOWN_MASK;
		}
		if (evt.isShift()) {
			result = result | MouseEvent.SHIFT_DOWN_MASK;
		}
		if (evt.isMeta()) {
			result = result | MouseEvent.META_DOWN_MASK;
		}
		return result;
	}

	public static void savePngImage(BufferedImage imageContent, String name) {
		try {
			OutputStream os = new FileOutputStream(new File(name));
			ImageOutputStream ios = ImageIO.createImageOutputStream(os);
			ImageIO.write(imageContent, "png", ios);
			ios.close();
			os.close();
		} catch (IOException e) {
			Logger.error("Util:savePngImage", e);
		}
	}

	public static int getKeyModifiersAWTFlag(KeyboardEventMsgIn event) {
		int modifiers = 0;
		if (event.isAlt()) {
			modifiers = modifiers | KeyEvent.ALT_MASK;
		}
		if (event.isCtrl()) {
			modifiers = modifiers | KeyEvent.CTRL_MASK;
		}
		if (event.isShift()) {
			modifiers = modifiers | KeyEvent.SHIFT_MASK;
		}
		if (event.isAltgr()) {
			modifiers = modifiers | KeyEvent.ALT_GRAPH_MASK;
		}
		if (event.isMeta()) {
			modifiers = modifiers | KeyEvent.META_MASK;
		}
		return modifiers;
	}

	public static int getKeyType(KeyEventType type) {
		switch (type) {
		case keydown:
			return KeyEvent.KEY_PRESSED;
		case keypress:
			return KeyEvent.KEY_TYPED;
		case keyup:
			return KeyEvent.KEY_RELEASED;
		}
		return 0;
	}

	public static char getKeyCharacter(KeyboardEventMsgIn event) {
		if (NO_CHAR_KEY_CODES.contains(event.getKeycode())) {
			return KeyEvent.CHAR_UNDEFINED;
		} else {
			return (char) event.getCharacter();
		}
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static Rectangle[] concatRectangleArrays(Rectangle[] A, Rectangle[] B) {
		int aLen = A.length;
		int bLen = B.length;
		Rectangle[] C = new Rectangle[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	public static WebToolkit getWebToolkit() {
		return ((WebToolkit) Toolkit.getDefaultToolkit());
	}

	public static WebWindowPeer findWindowPeerById(String id) {
		for (Window w : Window.getWindows()) {
			Object peer = WebToolkit.targetToPeer(w);
			if (peer != null && peer instanceof WebWindowPeer) {
				if (((WebWindowPeer) peer).getGuid().equals(id)) {
					return (WebWindowPeer) peer;
				}

			}
		}
		return null;
	}

	public static Map<String, Map<Integer, BufferedImage>> extractWindowImages(AppFrameMsgOut json, Map<String, Map<Integer, BufferedImage>> windowImages) {
		for (WindowMsg window : json.getWindows()) {
			WebWindowPeer w = findWindowPeerById(window.getId());
			if (window.getId().equals(WebToolkit.BACKGROUND_WINDOW_ID)) {
				windowImages.put(window.getId(), new HashMap<Integer, BufferedImage>());// background
																						// image
																						// is
																						// handled
																						// on
																						// client
			} else {
				Map<Integer, BufferedImage> imageMap = new HashMap<Integer, BufferedImage>();
				for (int i = 0; i < window.getContent().size(); i++) {
					WindowPartialContentMsg wpc = window.getContent().get(i);
					Rectangle subRec = new Rectangle(wpc.getPositionX(), wpc.getPositionY(), wpc.getWidth(), wpc.getHeight());
					Rectangle parentRec = new Rectangle(window.getPosX(), window.getPosY(), window.getWidth(), window.getHeight());
					imageMap.put(i, w.extractBufferedImage(subRec, parentRec));
				}
				windowImages.put(window.getId(), imageMap);
			}
		}
		return windowImages;
	}

	public static Map<String, Image> extractWindowWebImages(Set<String> updatedWindows, Map<String, Image> webImages) {
		for (String windowId : updatedWindows) {
			if (windowId.equals(WebToolkit.BACKGROUND_WINDOW_ID)) {
				continue;
			}
			WebWindowPeer w = findWindowPeerById(windowId);
			if (w != null) {
				Image webimage = w.extractWebImage();
				webImages.put(windowId, webimage);
			}
		}
		return webImages;
	}

	public static void encodeWindowImages(Map<String, Map<Integer, BufferedImage>> windowImages, AppFrameMsgOut json) {
        long start = System.currentTimeMillis();
        long size = 0, count = 0;
        Map<Future<byte[]>, WindowPartialContentMsg> joinMap = new HashMap<Future<byte[]>, WindowPartialContentMsg>();
		for (WindowMsg window : json.getWindows()) {
			if (!window.getId().equals(WebToolkit.BACKGROUND_WINDOW_ID)) {
				Map<Integer, BufferedImage> imageMap = windowImages.get(window.getId());
				for (int i = 0; i < window.getContent().size(); i++) {
					WindowPartialContentMsg c = window.getContent().get(i);
					if (imageMap.containsKey(i)) {
                        BufferedImage image = imageMap.get(i);
                        size += image.getWidth() * image.getHeight();
                        count += 1;
                        Future<byte[]> future = Services.getImageService().getPngImageAsync(image);
                        joinMap.put(future, c);
                    }
					}
				}
			}
        for (Future<byte[]> j : joinMap.keySet()) {
            try {
                joinMap.get(j).setBase64Content(j.get());
            } catch (InterruptedException e) {
                Logger.error("Util.encodeWindowImages:Writing image interrupted:" + e.getMessage(), e);
            } catch (ExecutionException e) {
                Logger.error("Util.encodeWindowImages:Writing image failed:" + e.getMessage(), e);
            }
		}
	}

	public static void encodeWindowWebImages(Map<String, Image> windowWebImages, AppFrameMsgOut json) {
		for (WindowMsg window : json.getWindows()) {
			Image wi = windowWebImages.get(window.getId());
			if (wi != null) {
				window.setDirectDraw(Services.getDirectDrawService().buildWebImage(wi));
			}
		}
	}

	@SuppressWarnings("restriction")
	public static AppFrameMsgOut fillWithWindowsData(Map<String, Set<Rectangle>> currentAreasToUpdate) {
		Map<String, List<Rectangle>> windowNonVisibleAreas = getWebToolkit().getWindowManager().extractNonVisibleAreas();
		
		AppFrameMsgOut json = new AppFrameMsgOut();
		for (String windowId : currentAreasToUpdate.keySet()) {
			WebWindowPeer ww = Util.findWindowPeerById(windowId);
			if (ww != null || windowId.equals(WebToolkit.BACKGROUND_WINDOW_ID)) {
				WindowMsg window = json.getOrCreateWindowById(windowId);
				if (windowId.equals(WebToolkit.BACKGROUND_WINDOW_ID)) {
					window.setPosX(0);
					window.setPosY(0);
					window.setWidth(getWebToolkit().getScreenSize().width);
					window.setHeight(getWebToolkit().getScreenSize().height);
				} else {
					Point location = ww.getLocationOnScreen();
					window.setPosX(location.x);
					window.setPosY(location.y);
					window.setWidth(ww.getBounds().width);
					window.setHeight(ww.getBounds().height);
					if (ww.getTarget() instanceof Frame) {
						window.setTitle(((Frame) ww.getTarget()).getTitle());
					}
					if (ww.getTarget() instanceof Component) {
						window.setName(((Component) ww.getTarget()).getName());
					}
				}
				List<Rectangle> toPaint = joinRectangles(getGrid(new ArrayList<Rectangle>(currentAreasToUpdate.get(windowId)), windowNonVisibleAreas.get(windowId)));
				List<WindowPartialContentMsg> partialContentList = new ArrayList<WindowPartialContentMsg>();
				for (Rectangle r : toPaint) {
					if (r.x < window.getWidth() && r.y < window.getHeight()) {
                        int w = Math.min(r.width, window.getWidth() - r.x);
                        int h = Math.min(r.height, window.getHeight() - r.y);
                        //if area too large, we slice it up to allow paralled png encoding
                        int cols = (int) Math.max(1, Math.ceil(w / MAX_SLICE_SIZE));
                        int colSize = (int) Math.ceil(w / (double) cols);
                        int rows = (int) Math.max(1, Math.ceil(h / MAX_SLICE_SIZE));
                        int rowSize = (int) Math.ceil(h / (double) rows);
                        for (int col = 0; col < cols; col++) {
                            for (int row = 0; row < rows; row++) {
								WindowPartialContentMsg content = new WindowPartialContentMsg();
		                                content.setPositionX(r.x + col * colSize);
		                                content.setPositionY(r.y + row * rowSize);
		                                content.setWidth(Math.min(colSize, w - col * colSize));
		                                content.setHeight(Math.min(rowSize, h - row * rowSize));
								partialContentList.add(content);
							}
						}
                    }
                }
				window.setContent(partialContentList);
			}
		}
		return json;
	}
	
	public static AppFrameMsgOut fillWithCompositingWindowsData(Map<String, Set<Rectangle>> currentAreasToUpdate) {
		AppFrameMsgOut frame = new AppFrameMsgOut();
		List<String> zOrder = getWebToolkit().getWindowManager().getZOrder();
		Map<Window, List<Container>> webContainers = getWebToolkit().getPaintDispatcher().getRegisteredWebContainers();
		Map<Window, List<HtmlPanel>> htmlPanels = getWebToolkit().getPaintDispatcher().getRegisteredHtmlPanelsAsMap();
		Map<Container, Map<JComponent, WindowMsg>> htmlWebComponentsMap = new HashMap<>();
		
		for (String windowId : zOrder) {
			WebWindowPeer ww = findWindowPeerById(windowId);
			if (ww == null) {
				continue;
			}

			WindowMsg window = new WindowMsg();
			window.setId(windowId);
			
			Point location = ww.getLocationOnScreen();
			window.setBounds(location.x, location.y, ww.getBounds().width, ww.getBounds().height);
			if (ww.getTarget() instanceof Frame) {
				window.setTitle(((Frame) ww.getTarget()).getTitle());
				window.setState(((Frame) ww.getTarget()).getExtendedState());
			}
			if (ww.getTarget() instanceof Component) {
				window.setName(((Component) ww.getTarget()).getName());
			}
			
			boolean modalBlocked = ww.getTarget() instanceof Window && getWebToolkit().getWindowManager().isBlockedByModality((Window) ww.getTarget(), false);
			window.setModalBlocked(modalBlocked);
			
			if (!isDD()) {
				Set<Rectangle> rects = currentAreasToUpdate.get(window.getId());
				window.setContent(Collections.emptyList());
				
				if (rects != null) {
					List<Rectangle> toPaint = joinRectangles(getGrid(new ArrayList<Rectangle>(rects), null));
					List<WindowPartialContentMsg> partialContentList = new ArrayList<WindowPartialContentMsg>();
					for (Rectangle r : toPaint) {
						if (r.x < window.getWidth() && r.y < window.getHeight()) {
							WindowPartialContentMsg content = new WindowPartialContentMsg(r.x, r.y, Math.min(r.width, window.getWidth() - r.x), Math.min(r.height, window.getHeight() - r.y));
							partialContentList.add(content);
						}
					}
					window.setContent(partialContentList);
				}
			}
			
			if (ww.getTarget() instanceof Window) {
				window.setOwnerId(findWindowOwner(((Window) ww.getTarget()).getOwner(), zOrder));
			}
			
			if (htmlPanels.containsKey(ww.getTarget())) {
				handleHtmlPanels(htmlPanels, ww, window, frame, htmlWebComponentsMap);
			}
			
			if (webContainers.containsKey(ww.getTarget())) {
				handleWebContainers(webContainers.get(ww.getTarget()), window, frame, htmlWebComponentsMap);
			}
			
			frame.getWindows().add(window);
		}
		return frame;
	}
	
	private static void handleHtmlPanels(Map<Window, List<HtmlPanel>> htmlPanels, WebWindowPeer ww, WindowMsg window, AppFrameMsgOut frame, Map<Container, Map<JComponent, WindowMsg>> htmlWebComponentsMap) {
		for (HtmlPanel htmlPanel : htmlPanels.get(ww.getTarget())) {
			if (!htmlPanel.isShowing()) {
				continue;
			}
			
			WindowMsg htmlWin = new WindowMsg(System.identityHashCode(htmlPanel) + "", htmlPanel.getName(), htmlPanel.getLocationOnScreen(), 
					htmlPanel.getBounds().width, htmlPanel.getBounds().height, WindowType.html, window.isModalBlocked(), window.getId());

			if (!isDD()) {
				htmlWin.setContent(Collections.emptyList());
			}
			
			if (htmlPanel instanceof HtmlPanelImpl) {
				Container container = ((HtmlPanelImpl) htmlPanel).getWebContainer();
				JComponent component = ((HtmlPanelImpl) htmlPanel).getWebComponent();
				if (container != null && component != null) {
					// if HtmlPanel has a registered container, process later when web containers are processed (see handleWebContainers method)
					if (!htmlWebComponentsMap.containsKey(container)) {
						htmlWebComponentsMap.put(container, new HashMap<>());
					}
					htmlWebComponentsMap.get(container).put(component, htmlWin);
					continue;
				}
			}
			
			frame.getWindows().add(htmlWin);
		}
	}

	private static void handleWebContainers(List<Container> containers, WindowMsg window, AppFrameMsgOut frame, Map<Container, Map<JComponent, WindowMsg>> htmlWebComponentsMap) {
		// sort according to hierarchy
		Collections.sort(containers, (o1, o2) -> {
			return o1.isAncestorOf(o2) ? -1 : (o2.isAncestorOf(o1) ? 1 : 0);
		});
		
		for (Container container : containers) {
			if (!container.isShowing()) {
				continue;
			}
			
			// handle wrapper
			WindowMsg containerWin = new WindowMsg(System.identityHashCode(container) + "", container.getName(), container.getLocationOnScreen(), 
					container.getBounds().width, container.getBounds().height, WindowType.internalWrapper, window.isModalBlocked(), window.getId());
			if (!isDD()) {
				containerWin.setContent(Collections.emptyList());
			}
			
			if (window.getInternalWindows() == null) {
				window.setInternalWindows(new ArrayList<>());
			}
			window.getInternalWindows().add(containerWin);
			
			// handle child components
			Map<JComponent, WindowMsg> htmlComponents = htmlWebComponentsMap.get(container);
			for (Component c : container.getComponents()) {
				if (htmlComponents != null && htmlComponents.containsKey(c)) {
					// if component contains a registered HtmlPanel
					WindowMsg htmlWin = htmlComponents.get(c);
					htmlWin.setOwnerId(containerWin.getId());
					htmlWin.setType(WindowType.internalHtml);
					window.getInternalWindows().add(htmlWin);
				}
				
				WindowMsg componentWin = new WindowMsg(System.identityHashCode(c) + "", c.getName(), c.getLocationOnScreen(), 
						c.getBounds().width, c.getBounds().height, WindowType.internal, containerWin.isModalBlocked(), containerWin.getId());
				
				if (!isDD()) {
					componentWin.setContent(Collections.emptyList());
				}
				
				window.getInternalWindows().add(componentWin);
			}
		}
	}
	
	private static String findWindowOwner(Window owner, List<String> zOrder) {
		if (owner == null) {
			return null;
		}
		
		WebWindowPeer peer = (WebWindowPeer) WebToolkit.targetToPeer(owner);
		
		if (peer != null && zOrder.contains(peer.getGuid())) {
			// must be from current z-order windows, otherwise it could be SwingUtilities$SharedOwnerFrame
			return peer.getGuid();
		}
		
		return null;
	}

	public static Map<String, Set<Rectangle>> postponeNonShowingAreas(Map<String, Set<Rectangle>> currentAreasToUpdate, Map<String, Boolean> windowRendered) {
		Map<String, Set<Rectangle>> forLaterProcessing = new HashMap<String, Set<Rectangle>>();
		for (String windowId : currentAreasToUpdate.keySet()) {
			if (WebToolkit.BACKGROUND_WINDOW_ID.equals(windowId)) {
				continue;
			}
			if (Boolean.TRUE.equals(windowRendered.get(windowId))) {
				WebWindowPeer ww = Util.findWindowPeerById(windowId);
				if (ww != null) {
					if (!((Window) ww.getTarget()).isShowing()) {
						forLaterProcessing.put(windowId, currentAreasToUpdate.get(windowId));
					}
				}
			}
			// 本窗体还没有被绘制，延迟等到窗体绘制完成之后再将图像数据生成图片发给前台
			else {
				forLaterProcessing.put(windowId, currentAreasToUpdate.get(windowId));
			}
		}
		for (String later : forLaterProcessing.keySet()) {
			currentAreasToUpdate.remove(later);
		}
		return forLaterProcessing;
	}
	
	public static boolean isCompositingWM() {
		return Boolean.valueOf(System.getProperty(Constants.SWING_START_SYS_PROP_COMPOSITING_WM, "false"));
	}

	public static boolean isWindowDecorationEvent(Window w, AWTEvent e) {
		if (e instanceof MouseEvent && MouseEvent.MOUSE_WHEEL != e.getID()) {
			return isWindowDecorationPosition((Window) w, new Point(((MouseEvent) e).getXOnScreen(), ((MouseEvent) e).getYOnScreen()));
		}
		return false;
	}

	public static boolean isWindowDecorationPosition(Window w, Point locationOnScreen) {
		if (w != null && locationOnScreen != null && !isWindowUndecorated(w)) {
			Rectangle inner = w.getBounds();
			Insets i = w.getInsets();
			inner.x = i.left;
			inner.y = i.top;
			inner.width -= i.left + i.right;
			inner.height -= i.top + i.bottom;
			boolean isInInnerWindow = SwingUtilities.isRectangleContainingRectangle(inner, new Rectangle(locationOnScreen.x - w.getX(), locationOnScreen.y - w.getY(), 0, 0));
			boolean isInWindow = SwingUtilities.isRectangleContainingRectangle(w.getBounds(), new Rectangle(locationOnScreen.x, locationOnScreen.y, 0, 0));
			return !isInInnerWindow && isInWindow;
		} else {
			return false;
		}
	}

	public static Set<Rectangle> getGrid(List<Rectangle> dirtyAreas, List<Rectangle> topWindows) {
		Set<Rectangle> result = new HashSet<Rectangle>();
		Set<Integer> xLines = new TreeSet<Integer>();
		Set<Integer> yLines = new TreeSet<Integer>();
		for (Rectangle r : dirtyAreas) {
			xLines.add(r.x);
			xLines.add(r.x + r.width);
			yLines.add(r.y);
			yLines.add(r.y + r.height);
		}
		if (topWindows != null) {
			for (Rectangle r : topWindows) {
				xLines.add(r.x);
				xLines.add(r.x + r.width);
				yLines.add(r.y);
				yLines.add(r.y + r.height);
			}
		}
		Integer[] y = yLines.toArray(new Integer[yLines.size()]);
		Integer[] x = xLines.toArray(new Integer[xLines.size()]);
		for (int row = 0; row < y.length - 1; row++) {
			for (int col = 0; col < x.length - 1; col++) {
				Rectangle potential = new Rectangle(x[col], y[row], x[col + 1] - x[col], y[row + 1] - y[row]);
				// filter
				boolean insideDirtyAreas = false;
				for (Rectangle da : dirtyAreas) {
					if (SwingUtilities.isRectangleContainingRectangle(da, potential)) {
						insideDirtyAreas = true;
						break;
					}
				}
				if (insideDirtyAreas) {
					boolean insideTopWindow = false;
					if (topWindows != null) {
						for (Rectangle tw : topWindows) {
							if (SwingUtilities.isRectangleContainingRectangle(tw, potential)) {
								insideTopWindow = true;
								break;
							}
						}
					}
					if (!insideTopWindow) {
						result.add(potential);
					}
				}
			}
		}
		return result;
	}

	public static List<Rectangle> joinRectangles(Set<Rectangle> grid) {
		List<Rectangle> result = new ArrayList<Rectangle>();
		List<Rectangle> gridList = new ArrayList<Rectangle>(grid);
		// join by rows
		List<Rectangle> joinedRows = new ArrayList<Rectangle>();
		Collections.sort(gridList, new Comparator<Rectangle>() {

			@Override
			public int compare(Rectangle o1, Rectangle o2) {
				if (o1.y > o2.y) {
					return 1;
				} else if (o1.y == o2.y) {
					if (o1.x > o2.x) {
						return 1;
					} else {
						return -1;
					}
				} else {
					return -1;
				}
			}
		});
		Rectangle current = null;
		for (Rectangle r : gridList) {
			if (current == null) {
				current = r;
			} else {
				if (current.y == r.y && current.height == r.height && current.x + current.width == r.x) {// is
																												// joinable
																											// on
																											// row
					current.width += r.width;
				} else {
					joinedRows.add(current);
					current = r;
				}
			}
		}
		if (current != null) {
			joinedRows.add(current);
		}
		// join by cols
		Collections.sort(joinedRows, new Comparator<Rectangle>() {

			@Override
			public int compare(Rectangle o1, Rectangle o2) {
				if (o1.x > o2.x) {
					return 1;
				} else if (o1.x == o2.x) {
					if (o1.y > o2.y) {
						return 1;
					} else {
						return -1;
					}
				} else {
					return -1;
				}
			}
		});
		Rectangle currentX = null;
		for (Rectangle r : joinedRows) {
			if (currentX == null) {
				currentX = r;
			} else {
				if (currentX.x == r.x && currentX.width == r.width && currentX.y + currentX.height == r.y) {// is
																												// joinable
																											// on
																											// row
					currentX.height += r.height;
				} else {
					result.add(currentX);
					currentX = r;
				}
			}
		}
		if (currentX != null) {
			result.add(currentX);
		}
		return result;
	}

	public static void resetWindowsGC(int width, int height) {
		for (Window w : Window.getWindows()) {
			try {
				Class<?> windowClazz = w.getClass();
				while (windowClazz != Window.class && windowClazz != null) {
					windowClazz = windowClazz.getSuperclass();
				}
				if (windowClazz != null) {
					try {
						Method m = windowClazz.getDeclaredMethod("resetGC");
						m.setAccessible(true);
						m.invoke(w);
					} catch (Exception e) {
						// do nothing (java7 don't have this method)
					}
				}
				RepaintManager.currentManager(w).setDoubleBufferMaximumSize(new Dimension(width, height));
			} catch (Exception e) {
				Logger.error("Util:resetWindowsGC", e);
			}
		}
	}

	public static JFileChooser discoverFileChooser(WebWindowPeer windowPeer) {
		Window w = (Window) windowPeer.getTarget();
		if (w instanceof JDialog) {
			Component[] coms = ((JDialog) w).getContentPane().getComponents();
			if (coms != null && coms.length > 0 && coms[0] instanceof JFileChooser) {
				return (JFileChooser) coms[0];
			}
		}
		return null;
	}

	public static String resolveFilename(File currentDir, String fileName) {
		if (!existsFilename(currentDir, fileName)) {
			return fileName;
		} else {
			int i = fileName.lastIndexOf('.');
			String base = i > 0 ? fileName.substring(0, i) : fileName;
			String ext = i > 0 ? fileName.substring(i) : null;
			int next = 1;
			while (true) {
				String nextFN = base + " " + next + ext;
				if (!existsFilename(currentDir, nextFN)) {
					return nextFN;
				}
				next++;
			}
		}
	}

	public static boolean existsFilename(File currentDir, String fileName) {
		if (currentDir != null && currentDir.exists() && currentDir.isDirectory()) {
			for (File f : currentDir.listFiles()) {
				if (f.getName().equals(fileName)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isDD() {
		boolean startDD = Boolean.valueOf(System.getProperty(Constants.SWING_START_SYS_PROP_DIRECTDRAW, "false"));
		boolean supportedDD = Boolean.valueOf(System.getProperty(Constants.SWING_START_SYS_PROP_DIRECTDRAW_SUPPORTED, "true"));
		return startDD && supportedDD;
	}

	public static void repaintAllWindow() {
		synchronized (WebPaintDispatcher.webPaintLock) {
			for (Window w : Window.getWindows()) {
				if (w.isShowing()) {
					final Object peer = WebToolkit.targetToPeer(w);
					if (peer != null && peer instanceof WebWindowPeer) {
						((WebWindowPeer) peer).updateWindowDecorationImage();
						Util.getWebToolkit().getPaintDispatcher().notifyWindowRepaint(w);
						RepaintManager.currentManager(null).addDirtyRegion(w, 0, 0, w.getWidth(), w.getHeight());
					}
				}
			}
		}
	}

	public static Panel findHwComponentParent(JComponent c) {
		for (Container p = c.getParent(); p != null; p = p.getParent()) {
			if (p instanceof Panel && !(p instanceof Applet)) {
				return (Panel) p;
			}
		}
		return null;
	}

	public static AWTEvent createKeyEvent(Component src, int type, long when, int modifiers, int keycode, char character, int keyLocationStandard) {
		KeyEvent e = new KeyEvent(src, type, when, modifiers, keycode, character, KeyEvent.KEY_LOCATION_STANDARD);
		try {
			java.lang.reflect.Field f=KeyEvent.class.getDeclaredField("extendedKeyCode");
			f.setAccessible(true);
			f.set(e, keycode);
		} catch (Exception e1) {
			Logger.error("Failed to update extendedKeyCode of KeyEvent", e);
		}
		return e;
	}

	private static boolean isWindowUndecorated(Window w) {
		if (w instanceof Frame) {
			return ((Frame) w).isUndecorated();
		} else if (w instanceof Dialog) {
			return ((Dialog) w).isUndecorated();
		}
		return false;
	}
	
	public static Window findWindowById(String id) {
		for (Window w : Window.getWindows()) {
			Object peer = WebToolkit.targetToPeer(w);
			if (peer != null && peer instanceof WebWindowPeer) {
				if (((WebWindowPeer) peer).getGuid().equals(id)) {
					return w;
				}
			
			}
		}
		return null;
	}
	
	public static HtmlPanel findHtmlPanelById(String id) {
		for (HtmlPanel hp : getWebToolkit().getPaintDispatcher().getRegisteredHtmlPanels()) {
			if (id.equals(System.identityHashCode(hp) + "")) {
				return hp;
			}
		}
		return null;
	}

	public static WebComponentPeer getPeerForTarget(Object paramObject) {
		WebComponentPeer localWObjectPeer = (WebComponentPeer) WebToolkit.targetToPeer(paramObject);
		return localWObjectPeer;
	}

	public static String getCurrentCursor(String winId) {
		if (isCompositingWM()) {
			WebWindowPeer peer = findWindowPeerById(winId);
			return peer == null ? null : peer.getCurrentCursor();
		} else {
			return WindowManager.getInstance().getCurrentCursor();
		}
	}

	public static void setCurrentCursor(String winId, String cursor) {
		if (isCompositingWM()) {
			WebWindowPeer peer = findWindowPeerById(winId);
			if (peer != null) {
				peer.setCurrentCursor(cursor);
			}
		} else {
			WindowManager.getInstance().setCurrentCursor(cursor);
		}
	}
		
}
