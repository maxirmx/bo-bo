package org.webswing.model.internal;

import org.webswing.model.MsgInternal;

public class JvmStatsMsgInternal implements MsgInternal {
	private static final long serialVersionUID = 4505813670370834105L;
	private double heapSize;
	private double heapSizeUsed;

	public double getHeapSize() {
		return heapSize;
	}

	public void setHeapSize(double heapSize) {
		this.heapSize = heapSize;
	}

	public double getHeapSizeUsed() {
		return heapSizeUsed;
	}

	public void setHeapSizeUsed(double heapSizeUsed) {
		this.heapSizeUsed = heapSizeUsed;
	}

}
