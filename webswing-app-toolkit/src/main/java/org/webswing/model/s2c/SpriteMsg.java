package org.webswing.model.s2c;

import org.webswing.model.Msg;

public class SpriteMsg implements Msg {
	
	private static final long serialVersionUID = 1L;
	
	private Integer spriteX;
	private Integer spriteY;
	private Integer positionX;
	private Integer positionY;
	private Integer width;
	private Integer height;
	
	public Integer getSpriteX() {
		return spriteX;
	}
	public void setSpriteX(Integer spriteX) {
		this.spriteX = spriteX;
	}
	public Integer getSpriteY() {
		return spriteY;
	}
	public void setSpriteY(Integer spriteY) {
		this.spriteY = spriteY;
	}
	
	public Integer getPositionX() {
		return positionX;
	}

	public void setPositionX(Integer positionX) {
		this.positionX = positionX;
	}

	public Integer getPositionY() {
		return positionY;
	}

	public void setPositionY(Integer positionY) {
		this.positionY = positionY;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

}
