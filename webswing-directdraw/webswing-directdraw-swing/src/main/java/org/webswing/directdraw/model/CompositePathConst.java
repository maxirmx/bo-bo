package org.webswing.directdraw.model;

import org.webswing.directdraw.DirectDraw;
import org.webswing.directdraw.proto.Directdraw;

public class CompositePathConst extends ImmutableDrawConstantHolder<SegmentedPathConst.CompositePath> {

	public CompositePathConst(DirectDraw context, SegmentedPathConst.CompositePath value) {
		super(context, value);
	}

	@Override
	public String getFieldName() {
		return "path";
	}

	@Override
	public Object toMessage() {
		Directdraw.CompositePathProto.Builder model = Directdraw.CompositePathProto.newBuilder();
		model.setWindingOdd(getValue().isWindingOdd());
		model.addAllXOffsets(getValue().getxOffsets());
		model.addAllYOffsets(getValue().getyOffsets());
		return model.build();
	}


}
