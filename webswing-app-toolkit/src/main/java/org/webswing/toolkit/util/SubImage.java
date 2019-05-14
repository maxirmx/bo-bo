package org.webswing.toolkit.util;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class SubImage {

	BufferedImage image;
	Rectangle coordinates;

	public SubImage(BufferedImage image, Rectangle coordinates) {
		super();
		this.image = image;
		this.coordinates = coordinates;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	public Rectangle getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Rectangle coordinates) {
		this.coordinates = coordinates;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
		result = prime * result + ((image == null) ? 0 : image.hashCode());
		return result;
	}

}
