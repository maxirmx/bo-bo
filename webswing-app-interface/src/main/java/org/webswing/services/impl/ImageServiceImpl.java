package org.webswing.services.impl;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.webswing.common.WindowDecoratorTheme;
import org.webswing.ext.services.ImageService;
import org.webswing.toolkit.util.Logger;
import org.webswing.toolkit.util.Util;

import com.objectplanet.image.PngEncoder;

public class ImageServiceImpl implements ImageService {

	private static ImageServiceImpl impl;
	private PngEncoder encoder;
	private WindowDecoratorTheme windowDecorationTheme;

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

			encoder = new PngEncoder(PngEncoder.COLOR_TRUECOLOR_ALPHA, PngEncoder.BEST_SPEED);
		} catch (Exception e) {
			Logger.warn("ImageService:Library for fast image encoding not found. Download the library from http://objectplanet.com/pngencoder/");
		}
	}

	public byte[] getPngImage(BufferedImage imageContent) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (encoder != null) {
				encoder.encode(imageContent, baos);
			} else {
				ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
				ImageIO.write(imageContent, "png", ios);
			}
			byte[] result = baos.toByteArray();
			baos.close();
			return result;
		} catch (IOException e) {
			Logger.error("ImageService:Writing image interupted:" + e.getMessage(), e);
		}
		return null;
	}

	public WindowDecoratorTheme getWindowDecorationTheme() {
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
					WindowDecoratorTheme theme = (WindowDecoratorTheme) implclass.newInstance();
					this.windowDecorationTheme = theme;
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
