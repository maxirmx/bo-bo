package org.webswing.directdraw.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.webswing.directdraw.model.DrawConstant;
import org.webswing.directdraw.model.FontFaceConst;
import org.webswing.directdraw.model.ImageConst;
import org.webswing.directdraw.proto.Directdraw.DrawConstantProto;

public class DrawConstantPool {

	private static int IMG_CACHE_SIZE = Integer.getInteger("webswing.ddImageCaceSize",128);

	private LRUDrawConstantPoolCache pool;
	private LRUDrawConstantPoolCache imgPool;
	private Set<String> registeredFonts = new HashSet<String>();
	private Map<String, FontFaceConst> requestedFonts = new HashMap<String, FontFaceConst>();

	public DrawConstantPool(int size) {
		imgPool = new LRUDrawConstantPoolCache(IMG_CACHE_SIZE,0);
		pool = new LRUDrawConstantPoolCache(size - IMG_CACHE_SIZE,IMG_CACHE_SIZE);
	}

	public int addToCache(List<DrawConstantProto> protos, DrawConstant<?> cons) {
		if (cons instanceof ImageConst) {
			return addToCache(imgPool, protos, cons);
		} else {
			return addToCache(pool, protos, cons);
		}
	}

	private int addToCache(LRUDrawConstantPoolCache cache, List<DrawConstantProto> protos, DrawConstant<?> cons) {
		int thisId;
		if (!cache.contains(cons)) {
			DrawConstant<?> cacheEntry = cons.toCacheEntry();
			cache.getOrAdd(cacheEntry);
			DrawConstantProto.Builder proto = DrawConstantProto.newBuilder();
			if (cons.getFieldName() != null) {
				proto.setField(DrawConstantProto.Builder.getDescriptor().findFieldByName(cons.getFieldName()), cons.toMessage());
			}
			proto.setId(cacheEntry.getId());
			protos.add(proto.build());
			thisId = cacheEntry.getId();
		} else {
			thisId = cache.getOrAdd(cons).getId();
		}
		return thisId;
	}

	public synchronized boolean isFontRegistered(String file) {
		if (requestedFonts.containsKey(file) || registeredFonts.contains(file)) {
			return true;
		}
		return false;
	}

	public synchronized void requestFont(String file, FontFaceConst fontFaceConst) {
		requestedFonts.put(file, fontFaceConst);
	}

	public synchronized Collection<FontFaceConst> registerRequestedFonts() {
		if (requestedFonts.size() > 0) {
			Collection<FontFaceConst> result = new ArrayList<FontFaceConst>(requestedFonts.values());
			registeredFonts.addAll(requestedFonts.keySet());
			requestedFonts.clear();
			return result;
		}
		return Collections.emptyList();
	}

}
