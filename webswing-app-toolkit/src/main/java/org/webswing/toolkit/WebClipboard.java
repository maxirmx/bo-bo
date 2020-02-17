package org.webswing.toolkit;

import org.webswing.Constants;
import org.webswing.toolkit.util.Logger;
import org.webswing.toolkit.util.Services;
import org.webswing.toolkit.util.Util;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebClipboard extends Clipboard {
	private static DataFlavor htmlDf;
	static {

		try {
			htmlDf = new DataFlavor("text/html;class=java.lang.String");
		} catch (ClassNotFoundException e) {
			Logger.error("initialization error:", e);
		}
	}
	public final static DataFlavor HTML_FLAVOR = htmlDf;

	private final boolean isSystemClipboard;
	public final ClipboardOwner owner = new ClipboardOwner() {

		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
		}
	};

	public WebClipboard(String name, boolean isSystemClipboard) {
		super(name);
		this.isSystemClipboard = isSystemClipboard;
	}

	public void setContents(Transferable contents) {
		super.setContents(contents, owner);
	}

	@Override
	public synchronized void setContents(Transferable contents, ClipboardOwner owner) {
		super.setContents(contents, owner);
		if (isSystemClipboard) {
			String html = null;
			String text = null;
			byte[] img = null;
			List<String> files = null;
			boolean other = false;

			if (contents.isDataFlavorSupported(HTML_FLAVOR)) {
				try {
					Object transferData = contents.getTransferData(HTML_FLAVOR);
					html = transferData.toString();
				} catch (Exception e) {
					Logger.error("WebClipboard:setContent:HTML", e);
				}
			}

			if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					text = (String) contents.getTransferData(DataFlavor.stringFlavor);
				} catch (Exception e) {
					Logger.error("WebClipboard:setContent:Plain", e);
				}
			}
			if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
				try {
					Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
					if (image != null) {
						BufferedImage result = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
						Graphics g = result.getGraphics();
						g.drawImage(image, 0, 0, null);
						g.dispose();
						img = Services.getImageService().getPngImage(result);
					}
				} catch (Exception e) {
					Logger.error("WebClipboard:setContent:Image", e);
				}
			}
			if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				try {
					List<?> fileList = (List<?>) contents.getTransferData(DataFlavor.javaFileListFlavor);
					if (fileList != null) {
						files = new ArrayList<String>();
						for (Object o : fileList) {
							if (o instanceof File) {
								File f = (File) o;
								if (Boolean.getBoolean(Constants.SWING_START_SYS_PROP_ALLOW_DOWNLOAD)) {
									if (f.exists() && f.canRead() && !f.isDirectory()) {
										files.add(f.getAbsolutePath());
									} else {
										files.add("#" + f.getAbsolutePath());
									}
								} else {
									files.add("#Downloading not allowed.");
									break;
								}
							}
						}
					}
				} catch (Exception e) {
					Logger.error("WebClipboard:setContent:Files", e);
				}
			}
			List<DataFlavor> flavors = new ArrayList<DataFlavor>(Arrays.asList(contents.getTransferDataFlavors()));
			flavors.removeAll(Arrays.asList(HTML_FLAVOR, DataFlavor.stringFlavor, DataFlavor.imageFlavor, DataFlavor.javaFileListFlavor));
			if (flavors.size() > 0) {
				other = true;
			}
			Util.getWebToolkit().getPaintDispatcher().notifyCopyEvent(text, html, img, files, other);
		}
	}
}
