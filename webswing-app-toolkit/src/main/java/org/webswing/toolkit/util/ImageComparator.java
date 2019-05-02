package org.webswing.toolkit.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.webswing.model.s2c.AppFrameMsgOut;
import org.webswing.model.s2c.SpriteMsg;
import org.webswing.model.s2c.WindowMsg;
import org.webswing.model.s2c.WindowPartialContentMsg;
import org.webswing.toolkit.WebToolkit;

public class ImageComparator {
	
	private static final int SQ = 100;
	private static final int TEX_EDGE = 6*SQ;
	private static final int threadCount = Runtime.getRuntime().availableProcessors();
	private static ExecutorService processorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), DeamonThreadFactory.getInstance());
	private static BufferedImage spriteImg = new BufferedImage(TEX_EDGE, TEX_EDGE, BufferedImage.TYPE_INT_ARGB);

	/**
	 * Split image into small areas and compare each area with previous frame, areas are processed in parallel 
	 * @param image input image to be checked
	 * @param previous merged image of previous frame
	 * @param parent_x position of current image within the previous frame
	 * @param parent_y position of current image within the previous frame
	 * @return areas of image that have changed
	 */
	public static Set<SubImage> findUpdateAreas(final BufferedImage image, final BufferedImage previous, final int parent_x, final int parent_y) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final List<Rectangle> toCompare = new ArrayList<Rectangle>();
		for (int c = 0; c <= width / SQ; c++) {
			for (int r = 0; r <= height / SQ; r++) {
				int x = Math.min(c * SQ, width - 1), y = Math.min(r * SQ, height - 1);
				int w = Math.min(SQ, width - x), h = Math.min(SQ, height-y);
				toCompare.add(new Rectangle(x, y, w, h));				
			}
		}
		List<Future<Set<SubImage>>> futures = new ArrayList<Future<Set<SubImage>>>();
		for (int i = 0; i < threadCount; i++) {
			final int currentoffset = i;
			futures.add(processorPool.submit(new Callable<Set<SubImage>>() {
				int offset = currentoffset;

				@Override
				public Set<SubImage> call() throws Exception {
					Set<SubImage> diff = new HashSet<SubImage>();

					int[] imgData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
					int[] prevData = ((DataBufferInt) previous.getRaster().getDataBuffer()).getData();
					for (int j = offset; j < toCompare.size(); j = j + threadCount) {
						Rectangle currentArea = toCompare.get(j);

						areaCompare:
						for (int x = (int) currentArea.getMinX(); x < currentArea.getMaxX(); x++) {
							for (int y = (int) currentArea.getMinY(); y < currentArea.getMaxY(); y++) {
								int offset = (y + parent_y) * previous.getWidth() + (x + parent_x);
								try {
									if (imgData[offset] != prevData[offset]) {										
										Rectangle r = new Rectangle(currentArea.x+parent_x, currentArea.y+parent_y, currentArea.width, currentArea.height);
										diff.add(new SubImage(image.getSubimage(r.x-parent_x, r.y-parent_y, r.width, r.height), r));
										break areaCompare;
									}
								} catch (Exception e) {
									Logger.error(e.toString());
									Logger.error("failed to compare: offset" + offset + "|" + x + "|" + y + "|" + width + "|" + height + "| size" + imgData.length);
								}
							}
						}
					}
					return diff;
				}
			}));
		}

		//collect results:
		Set<SubImage> completeResult = new HashSet<SubImage>();
		for (Future<Set<SubImage>> f : futures) {
			try {
				completeResult.addAll(f.get());
			} catch (Exception e) {
				Logger.error("failed to compare pixels", e);
			}
		}
		return completeResult;
	}



	/**
	 * Loop through images and if dimensions and positions match with previous, then compare with previous 
	 * @param images images to be redraw
	 * @param prevImage all images from previous window frame merged into one
	 * @param list dirty areas marked for redraw
	 * @return
	 */
	public static Set<SubImage> findUpdateAreas(Map<Integer, BufferedImage> images, BufferedImage prevImage, List<WindowPartialContentMsg> list) {

		Set<SubImage> updatedAreas = new HashSet<SubImage>();
		for (Integer imageId : images.keySet()) {

			BufferedImage currentImage = images.get(imageId);

			WindowPartialContentMsg wpcm = list.get(imageId);
			if (currentImage.getHeight() > prevImage.getHeight() || currentImage.getWidth() > prevImage.getWidth() || wpcm.getPositionX() < 0 || wpcm.getPositionY() < 0) {
				updatedAreas.add(new SubImage(currentImage, new Rectangle(wpcm.getPositionX(), wpcm.getPositionY(), currentImage.getWidth(), currentImage.getHeight())));
			} else {
				updatedAreas.addAll(findUpdateAreas(currentImage, prevImage, wpcm.getPositionX(), wpcm.getPositionY()));
			}
		}
		return updatedAreas;
	}
	
	public static Map<String, Set<SubImage>> updateWindowImages(Map<String, Map<Integer, BufferedImage>> windowImageMap, Map<String, BufferedImage> previousWindowImageMap, AppFrameMsgOut json) {
		Logger.info("updateWindowImages ", windowImageMap.size());
		Map<String, Set<SubImage>> subimages = new HashMap<String, Set<SubImage>>();
		
		// Loop all windows from the json
		for (WindowMsg window : json.getWindows()) {
						
			if (!window.getId().equals(WebToolkit.BACKGROUND_WINDOW_ID)) {
				// Create Set to hold subImages of current window
				subimages.put(window.getId(), new HashSet<SubImage>());
				// Create List for holding partials of current window
				List<WindowPartialContentMsg> partialContentList = new ArrayList<WindowPartialContentMsg>();
				List<SpriteMsg> spriteList = new ArrayList<SpriteMsg>();
				BufferedImage previousWindowImage = previousWindowImageMap.get(window.getId());
				boolean reset = false;
				boolean flush = false;
				if(previousWindowImage != null && previousWindowImage.getHeight() == window.getHeight() && previousWindowImage.getWidth() == window.getWidth()) {					
					
					Map<Integer, BufferedImage> currentWindowImages = windowImageMap.get(window.getId());				
					
					Set<SubImage> imageMap = ImageComparator.findUpdateAreas(currentWindowImages, previousWindowImage, window.getContent());
					if(imageMap.isEmpty()) {
						window.getContent().clear();
						continue; // no updates for current Window
					}
					
					int atlas_x = 0;
					int atlas_y = 0;
					
					Graphics2D g2d = spriteImg.createGraphics();
					g2d.setBackground(new Color(0,0,0,0));
					g2d.setComposite(AlphaComposite.SrcOver);
					g2d.clearRect(0, 0, TEX_EDGE, TEX_EDGE);
					Iterator<SubImage> itr = imageMap.iterator();
					while(itr.hasNext()) {
						SubImage subImg = itr.next();
						flush = true;
						Rectangle r = subImg.getCoordinates();
						if (r.width < window.getWidth() && r.height < window.getHeight()) {
							SpriteMsg content = new SpriteMsg();
							content.setPositionX(r.x);
							content.setPositionY(r.y);
							content.setSpriteX(atlas_x*SQ);
							content.setSpriteY(atlas_y*SQ);
							content.setWidth(r.width);
							content.setHeight(r.height);
							
							spriteList.add(content);
							
							g2d.drawImage(subImg.getImage(), atlas_x*SQ, atlas_y*SQ, null);
							if(itr.hasNext()) {
								if(atlas_x < 5) {
									atlas_x += 1;
								} else if (atlas_y < 5){
									atlas_x = 0;
									atlas_y += 1;
								} else {
									flush(partialContentList, atlas_y, atlas_x, spriteImg);
									flush = false;
									atlas_x = 0;
									atlas_y = 0;
								}
							}
						} else {
							reset = true;
							subimages.get(window.getId()).clear();
							break;
						}
						subimages.get(window.getId()).add(subImg);
					}
					g2d.dispose();
					if(!reset) {
						if(flush) flush(partialContentList, atlas_y, atlas_x, spriteImg);
						window.setSprites(spriteList);
						window.setContent(partialContentList);
					}
				} else {
					reset = true;
				}
				
				if(reset) {
					previousWindowImageMap.remove(window.getId());
					Map<Integer, BufferedImage> imageMap = windowImageMap.get(window.getId());
					
					for (int i = 0; i < window.getContent().size(); i++) {
						WindowPartialContentMsg c = window.getContent().get(i);
						if (imageMap.containsKey(i)) {
							subimages.get(window.getId()).add(new SubImage(imageMap.get(i),new Rectangle(c.getPositionX(), c.getPositionY(), c.getWidth(), c.getHeight())));
							c.setBase64Content(Services.getImageService().getPngImage(imageMap.get(i)));
						}
					}
		
				}
			}
		}
		return subimages;
	}

	/**
	 * Write partial changes to the buffer for next frame comparison
	 * 
	 * @param previousWindowImages
	 * @param windowImages
	 * @param json
	 */
	public static void mergePartials(Map<String, BufferedImage> previousWindowImages, Map<String, Set<SubImage>> windowImages, AppFrameMsgOut json) {

		for (WindowMsg window : json.getWindows()) {
			if (!window.getId().equals(WebToolkit.BACKGROUND_WINDOW_ID)) {
				BufferedImage image = previousWindowImages.get(window.getId());
				if(image == null || image.getWidth() != window.getWidth() || image.getHeight() != window.getHeight()) {
					image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_ARGB);
					previousWindowImages.put(window.getId(), image);
				}
				Graphics2D g2d = image.createGraphics();
				for(SubImage subimage : windowImages.get(window.getId())) {
					g2d.drawImage(subimage.getImage(), subimage.getCoordinates().x, subimage.getCoordinates().y, null);				
				}
	
				g2d.dispose();
			}
		}

	}
	
	private static void flush(List<WindowPartialContentMsg> target, int atlas_y, int atlas_x, BufferedImage image ) {
		int w = atlas_y < 1 ? (atlas_x + 1)  * SQ : TEX_EDGE;
		int h = (atlas_y + 1)  * SQ;
		
		WindowPartialContentMsg wpcm = new WindowPartialContentMsg();
		wpcm.setPositionX(0);
		wpcm.setPositionY(0);
		wpcm.setWidth(w);
		wpcm.setHeight(h);								
		wpcm.setBase64Content(Services.getImageService().getPngImage(image.getSubimage(0, 0, w, h)));
		target.add(wpcm);
	}
}
