package org.webswing.model.s2c;

import org.webswing.model.MsgOut;

public class FocusEventMsg implements MsgOut{
	public enum FocusEventType {
		focusLost,
		focusGained,
		focusWithCarretGained,
		focusPasswordGained,
		htmlPanelFocused;
	}

	FocusEventType type;
	int x, y, w, h, caretX, caretY, caretH;
	String windowId;

	public FocusEventType getType() {
		return type;
	}

	public void setType(FocusEventType type) {
		this.type = type;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getW() {
		return w;
	}

	public void setW(int w) {
		this.w = w;
	}

	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
	}

	public int getCaretX() {
		return caretX;
	}

	public void setCaretX(int caretX) {
		this.caretX = caretX;
	}

	public int getCaretY() {
		return caretY;
	}

	public void setCaretY(int caretY) {
		this.caretY = caretY;
	}

	public int getCaretH() {
		return caretH;
	}

	public void setCaretH(int caretH) {
		this.caretH = caretH;
	}

	public String getWindowId() {
		return windowId;
	}

	public void setWindowId(String windowId) {
		this.windowId = windowId;
	}
	
}
