package org.webswing.services.impl;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.webswing.Constants;
import org.webswing.common.WindowDecoratorTheme;
import org.webswing.ext.services.ImageService;
import org.webswing.toolkit.util.DeamonThreadFactory;
import org.webswing.toolkit.util.Logger;
import org.webswing.toolkit.util.Util;

import com.objectplanet.image.PngEncoder;

public class ImageServiceImpl implements ImageService {

	private static ImageServiceImpl impl;
	private ThreadLocal<Map<Integer, PngEncoder>> encoders = new ThreadLocal<Map<Integer, PngEncoder>>();
	private WindowDecoratorTheme windowDecorationTheme;
	private ExecutorService encoderPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), DeamonThreadFactory.getInstance());
	
	private static final String DEFAULT_THEME = "Murrine";
	private String theme = System.getProperty(Constants.SWING_START_SYS_PROP_THEME, DEFAULT_THEME);
	
	public static ImageServiceImpl getInstance() {
		if (impl == null) {
			impl = new ImageServiceImpl();
		}
		return impl;
	}

	public ImageServiceImpl() {
		try {
			ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
			Thread.currentThread().setContextClassLoader(currentContextClassLoader);
		} catch (Exception e) {
			Logger.warn("ImageService:Library for fast image encoding not found. Download the library from http://objectplanet.com/pngencoder/");
		}
	}

	public byte[] getPngImage(BufferedImage image) {
		try {
			return getPngImageAsync(image).get();
		} catch (InterruptedException e) {
			Logger.error("ImageService:Writing image interrupted:" + e.getMessage(), e);
		} catch (ExecutionException e) {
			Logger.error("ImageService:Writing image failed:" + e.getMessage(), e);
		}
		return null;
	}

	public Future<byte[]> getPngImageAsync(final BufferedImage image) {
		return encoderPool.submit(new Callable<byte[]>() {
			@Override public byte[] call() throws Exception {
				try {
					long start = System.currentTimeMillis();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PngEncoder encoder = getEncoder(image);
					if (encoder != null) {
						encoder.encode(image, baos);
					} else {
						ImageIO.write(image, "png", baos);
					}
					Logger.trace("ImageService: " + image.getWidth() * image.getHeight() + "pixels encoded in " + (System.currentTimeMillis() - start) + "ms.");
					return baos.toByteArray();
				} catch (IOException e) {
					Logger.error("ImageService:Writing image interrupted:" + e.getMessage(), e);
				}
				return null;
			}
		});
	}

	public PngEncoder getEncoder(BufferedImage image) {
		int type;
		switch (image.getType()) {
		case BufferedImage.TYPE_BYTE_BINARY:
		case BufferedImage.TYPE_BYTE_INDEXED:
			type = image.getColorModel().hasAlpha() ? PngEncoder.COLOR_INDEXED_ALPHA : PngEncoder.COLOR_INDEXED;
			break;

		case BufferedImage.TYPE_INT_RGB:
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE:
		case BufferedImage.TYPE_3BYTE_BGR:
		case BufferedImage.TYPE_4BYTE_ABGR:
		case BufferedImage.TYPE_4BYTE_ABGR_PRE:
			type = image.getColorModel().hasAlpha() ? PngEncoder.COLOR_TRUECOLOR_ALPHA : PngEncoder.COLOR_TRUECOLOR;
			break;

		default:
			return null;
		}
		if (encoders.get() == null) {
			encoders.set(new HashMap<Integer, PngEncoder>());
		}
		Map<Integer, PngEncoder> encodersMap = encoders.get();
		PngEncoder encoder = encodersMap.get(type);
		if (encoder == null) {
			encodersMap.put(type, encoder = new PngEncoder(type, PngEncoder.BEST_SPEED));
		}
		return encoder;
	}

	public WindowDecoratorTheme getWindowDecorationTheme() {
	    String curTheme = System.getProperty(Constants.SWING_START_SYS_PROP_THEME, DEFAULT_THEME);
	    if (!curTheme.equals(theme))
	    {
	        windowDecorationTheme = null;
	        theme = curTheme;
	    }
	    
		if (windowDecorationTheme == null) {
			String implClassName = System.getProperty(WindowDecoratorTheme.DECORATION_THEME_IMPL_PROP, WindowDecoratorTheme.DECORATION_THEME_IMPL_DEFAULT);
			Class<?> implclass = null;
			try {
				implclass = ImageServiceImpl.class.getClassLoader().loadClass(implClassName);
			} catch (ClassNotFoundException e) {
				Logger.error("ImageService: WindowDecoratorTheme class not found", e);
				try {
					implclass = ImageServiceImpl.class.getClassLoader().loadClass(WindowDecoratorTheme.DECORATION_THEME_IMPL_DEFAULT);
				} catch (ClassNotFoundException e1) {
					Logger.fatal("ImageService: Fatal error:Default decoration theme not found.");
					Util.getWebToolkit().exitSwing(1);
				}
			}
			if (WindowDecoratorTheme.class.isAssignableFrom(implclass)) {
				try {
					this.windowDecorationTheme = (WindowDecoratorTheme) implclass.newInstance();
				} catch (Exception e) {
					Logger.fatal("ImageService: exception when creating instance of " + implclass.getCanonicalName(), e);
					Util.getWebToolkit().exitSwing(1);
				}
			} else {
				Logger.fatal("ImageService: Fatal error: Decoration theme not instance of WindowDecoratorThemeIfc:" + implclass.getCanonicalName());
				Util.getWebToolkit().exitSwing(1);
			}
		}
		return windowDecorationTheme;
	}

	@Override
	public void moveFile(File srcFile, File destFile) throws IOException {
		FileUtils.moveFile(srcFile, destFile);
	}

	@Override
	public Image readFromDataUrl(String dataUrl) {
		String encodingPrefix = "base64,";
		int contentStartIndex = dataUrl.indexOf(encodingPrefix) + encodingPrefix.length();
		byte[] imageData = Base64.decodeBase64(dataUrl.substring(contentStartIndex));

		// create BufferedImage from byteArray
		BufferedImage inputImage = null;
		try {
			inputImage = ImageIO.read(new ByteArrayInputStream(imageData));
		} catch (IOException e) {
			Logger.error("ImageService: reading image from dataUrl failed", e);
		}
		return inputImage;
	}
}
