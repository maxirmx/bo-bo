package org.webswing.directdraw.model;

import java.awt.geom.PathIterator;

import org.webswing.directdraw.DirectDraw;
import org.webswing.directdraw.proto.Directdraw;
import org.webswing.directdraw.proto.Directdraw.PartialPathProto;
import org.webswing.directdraw.proto.Directdraw.PartialPathProto.SegmentTypeProto;

public class PartialPathConst extends ImmutableDrawConstantHolder<SegmentedPathConst.PartialPath> {

	public PartialPathConst(DirectDraw context, SegmentedPathConst.PartialPath value) {
		super(context, value);
	}

	@Override
	public String getFieldName() {
		return "partialPath";
	}

	@Override
	public Object toMessage() {
		Directdraw.PartialPathProto.Builder model = PartialPathProto.newBuilder();
		model.addAllType(getValue().getTypes());
		model.addAllPoints(getValue().getPoints());
		return model.build();
	}


}
