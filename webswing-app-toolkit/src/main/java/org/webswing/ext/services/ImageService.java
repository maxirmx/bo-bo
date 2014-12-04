package org.webswing.ext.services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.webswing.common.WindowDecoratorTheme;

public interface ImageService {

    String encodeImage(BufferedImage window);

    byte[] getPngImage(BufferedImage image);

    WindowDecoratorTheme getWindowDecorationTheme();

    void moveFile(File srcFile, File destFile) throws IOException;
}
