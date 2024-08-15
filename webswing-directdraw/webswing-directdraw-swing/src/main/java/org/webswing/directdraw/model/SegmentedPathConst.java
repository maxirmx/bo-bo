package org.webswing.directdraw.model;

import org.webswing.directdraw.DirectDraw;
import org.webswing.directdraw.util.DrawConstantPool;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.lang.ref.WeakReference;
import java.util.*;

import static org.webswing.directdraw.proto.Directdraw.*;
import static org.webswing.directdraw.proto.Directdraw.PartialPathProto.*;
import static org.webswing.directdraw.proto.Directdraw.PartialPathProto.SegmentTypeProto.*;

public class SegmentedPathConst extends CompositeDrawConstantHolder<Shape> {

	private boolean windingOdd;
	List<PartialPath> segmentSequence = new ArrayList<>();
	List<Float> xOffsets = new ArrayList<>();
	List<Float> yOffsets = new ArrayList<>();
	private static WeakHashMap<PartialPath, WeakReference<PartialPath>> segmentCache = new WeakHashMap<>();

	public SegmentedPathConst(DirectDraw context, Shape value) {
		super(context);
		double[] points = new double[6];
		PathIterator iterator = value.getPathIterator(null);
		if (iterator != null) {
			this.windingOdd = iterator.getWindingRule() == PathIterator.WIND_EVEN_ODD;
			PartialPath current = null;
			Point2D.Float currentOffset = null;
			while (!iterator.isDone()) {
				int type = iterator.currentSegment(points);
				if (type == PathIterator.SEG_CLOSE) {
					if (current != null) {
						current.addCloseStep();
						addPartialPath(current, currentOffset);
						current = null;
						currentOffset = null;
					}
				} else {
					if (current == null) {
						current = new PartialPath();
						currentOffset = new Point2D.Float((float) points[0], (float) points[1]);
					}
					current.addStep(type, points, currentOffset);
				}
				iterator.next();
			}
			if (current != null) {
				addPartialPath(current, currentOffset);
			}
		}

		//		System.out.println("sequence:" + segmentSequence.size());
		//		System.out.println("cache:" + segmentCache.size());

	}

	private void addPartialPath(PartialPath current, Point2D.Float currentOffset) {
		PartialPath cached = null;
		WeakReference<PartialPath> cacheref = segmentCache.get(current);
		if (cacheref != null) {
			cached = cacheref.get();
		}

		if (cached != null) {
			segmentSequence.add(cached);
		} else {
			segmentCache.put(current, new WeakReference<>(current));
			segmentSequence.add(current);
		}
		xOffsets.add(currentOffset.x);
		yOffsets.add(currentOffset.y);
	}

	@Override
	public Shape getValue() {
		Path2D.Float path = new Path2D.Float(this.windingOdd ? PathIterator.WIND_EVEN_ODD : PathIterator.WIND_NON_ZERO);
		for (int i = 0; i < segmentSequence.size(); i++) {
			PartialPath segment = segmentSequence.get(i);
			Point2D.Double shift = new Point2D.Double(xOffsets.get(i), yOffsets.get(i));
			int offset = 0;
			for (int j = 0; j < segment.types.size(); j++) {
				int pointCount = 0;
				switch (segment.types.get(j)) {
				case MOVE:
					path.moveTo(segment.p.get(offset) + shift.x, segment.p.get(offset + 1) + shift.y);
					pointCount = 2;
					break;
				case LINE:
					path.lineTo(segment.p.get(offset) + shift.x, segment.p.get(offset + 1) + shift.y);
					pointCount = 2;
					break;
				case QUAD:
					path.quadTo(segment.p.get(offset + 2) + shift.x, segment.p.get(offset + 3) + shift.y, segment.p.get(offset) + shift.x, segment.p.get(offset + 1) + shift.y);
					pointCount = 4;
					break;
				case CUBIC:
					path.curveTo(segment.p.get(offset + 4) + shift.x, segment.p.get(offset + 5) + shift.y, segment.p.get(offset + 2) + shift.x, segment.p.get(offset + 3) + shift.y, segment.p.get(offset) + shift.x, segment.p.get(offset + 1) + shift.y);
					pointCount = 6;
					break;
				case CLOSE:
					path.closePath();
				}
				offset += pointCount;
			}
		}
		return path;
	}

	@Override
	public void expandAndCacheConstants(List<DrawConstantProto> protos, DrawConstantPool cache) {
		int[] ids = new int[segmentSequence.size() + 1];

		CompositePath path = new CompositePath(this.windingOdd, xOffsets, yOffsets);
		CompositePathConst pathConst = new CompositePathConst(getContext(), path);
		int id = cache.addToCache(protos, pathConst);
		ids[0] = id;

		for (int i = 0; i < segmentSequence.size(); i++) {
			PartialPath ppath = segmentSequence.get(i);
			PartialPathConst ppathConst = new PartialPathConst(getContext(), ppath);
			int ppathId = cache.addToCache(protos, ppathConst);
			ids[i + 1] = ppathId;
		}

		//create combined constant and set address
		CombinedConst constIds = new CombinedConst(getContext(), ids);
		int thisId = cache.addToCache(protos, constIds);
		this.setId(thisId);
	}

	@Override
	public int getExpandedConstantCount() {
		return 0;
	}

	public static class PartialPath {

		private ArrayList<SegmentTypeProto> types = new ArrayList<>();
		private ArrayList<Float> p = new ArrayList<>();

		public void addStep(int type, double[] points, Point2D.Float offset) {
			this.types.add(valueOf(type));
			switch (type) {
			case PathIterator.SEG_CUBICTO:
				p.add((float) (points[4] - offset.x));
				p.add((float) (points[5] - offset.y));
			case PathIterator.SEG_QUADTO:
				p.add((float) (points[2] - offset.x));
				p.add((float) (points[3] - offset.y));
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				p.add((float) (points[0] - offset.x));
				p.add((float) (points[1] - offset.y));
			default:
			}
		}

		public void addCloseStep() {
			this.types.add(valueOf(PathIterator.SEG_CLOSE));
		}

		public ArrayList<SegmentTypeProto> getTypes() {
			return types;
		}

		public ArrayList<Float> getPoints() {
			return p;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			PartialPath that = (PartialPath) o;

			if (!types.equals(that.types))
				return false;
			int s = p.size();
			for (int i = 0; i < s; i++) {
				if (Math.abs(p.get(i) - that.p.get(i)) > .001) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			int result = types.hashCode();
			return result;
		}

	}

	public static class CompositePath {
		private boolean windingOdd;
		private List<Float> xOffsets;
		private List<Float> yOffsets;

		public CompositePath(boolean windingOdd, List<Float> xOffsets, List<Float> yOffsets) {
			this.windingOdd = windingOdd;
			this.xOffsets = xOffsets;
			this.yOffsets = yOffsets;
		}

		public boolean isWindingOdd() {
			return windingOdd;
		}

		public List<Float> getxOffsets() {
			return xOffsets;
		}

		public List<Float> getyOffsets() {
			return yOffsets;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			CompositePath that = (CompositePath) o;

			if (windingOdd != that.windingOdd)
				return false;
			if (!xOffsets.equals(that.xOffsets))
				return false;
			return yOffsets.equals(that.yOffsets);
		}

		@Override
		public int hashCode() {
			int result = (windingOdd ? 1 : 0);
			result = 31 * result + xOffsets.hashCode();
			result = 31 * result + yOffsets.hashCode();
			return result;
		}
	}
}
