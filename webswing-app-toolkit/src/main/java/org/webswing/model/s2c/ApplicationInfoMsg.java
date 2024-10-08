package org.webswing.model.s2c;

import org.webswing.model.Msg;

public class ApplicationInfoMsg implements Msg, Comparable<ApplicationInfoMsg> {
	private static final long serialVersionUID = -7176092462203716782L;
	private String name;
	private byte[] base64Icon;
	private boolean applet;
	private boolean alwaysRestart;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getBase64Icon() {
		return base64Icon;
	}

	public void setBase64Icon(byte[] base64Icon) {
		this.base64Icon = base64Icon;
	}

	public boolean isApplet() {
		return applet;
	}

	public void setApplet(boolean applet) {
		this.applet = applet;
	}

	@Override
	public int compareTo(ApplicationInfoMsg o) {
		if (name != null && o != null && o.getName() != null) {
			return name.toLowerCase().compareTo(o.getName().toLowerCase());
		}
		return 0;
	}

	public void setAlwaysRestart(boolean b) {
		alwaysRestart = b;
	}

	public boolean isAlwaysRestart() {
		return alwaysRestart;
	}

}
