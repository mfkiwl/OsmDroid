// Created by plusminus on 20:50:06 - 03.10.2008
package org.andnav.osm.views.overlay;

import java.util.List;

import org.andnav.osm.DefaultResourceProxyImpl;
import org.andnav.osm.ResourceProxy;
import org.andnav.osm.views.OpenStreetMapView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;


public class OpenStreetMapViewItemizedOverlayWithFocus<T extends OpenStreetMapViewOverlayItem> extends OpenStreetMapViewItemizedOverlay<T> {
	
	// ===========================================================
	// Constants
	// ===========================================================
	
	public static final int DESCRIPTION_BOX_PADDING = 3;
	public static final int DESCRIPTION_BOX_CORNERWIDTH = 3;
	
	public static final int DESCRIPTION_LINE_HEIGHT = 12;
	/** Additional to <code>DESCRIPTION_LINE_HEIGHT</code>. */
	public static final int DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = 2;
	

	protected static final Point DEFAULTMARKER_FOCUSED_HOTSPOT = new Point(10, 19);
	protected static final int DEFAULTMARKER_BACKGROUNDCOLOR = Color.rgb(101, 185, 74);
	
	protected static final int DESCRIPTION_MAXWIDTH = 200;

	// ===========================================================
	// Fields
	// ===========================================================
	
	protected final Point mMarkerFocusedHotSpot; 
	protected final Drawable mMarkerFocusedBase;
	protected final int mMarkerFocusedBackgroundColor;
	protected final int mMarkerFocusedWidth, mMarkerFocusedHeight; 
	protected final Paint mMarkerBackgroundPaint, mDescriptionPaint, mTitlePaint;
	
	protected int mFocusedItemIndex;
	protected boolean mFocusItemsOnTap;
	private Point mFocusedScreenCoords = new Point();
	
	private final String UNKNOWN;

	// ===========================================================
	// Constructors
	// ===========================================================
	
	public OpenStreetMapViewItemizedOverlayWithFocus(
			final Context ctx, 
			final List<T> aList, 
			final OnItemTapListener<T> aOnItemTapListener) {
		this(ctx, aList, aOnItemTapListener, new DefaultResourceProxyImpl(ctx));
	}
	
	public OpenStreetMapViewItemizedOverlayWithFocus(
			final Context ctx, 
			final List<T> aList, 
			final OnItemTapListener<T> aOnItemTapListener, 
			final ResourceProxy pResourceProxy) {
		this(ctx, aList, null, null, null, null, NOT_SET, aOnItemTapListener, pResourceProxy);
	}
	
	public OpenStreetMapViewItemizedOverlayWithFocus(
			final Context ctx, 
			final List<T> aList, 
			final Drawable pMarker, 
			final Point pMarkerHotspot, 
			final Drawable pMarkerFocusedBase, 
			final Point pMarkerFocusedHotSpot, 
			final int pFocusedBackgroundColor, 
			final OnItemTapListener<T> aOnItemTapListener, 
			final ResourceProxy pResourceProxy) {

		super(ctx, aList, pMarkerFocusedBase, pMarkerHotspot, aOnItemTapListener, pResourceProxy);
		
		UNKNOWN = mResourceProxy.getString(ResourceProxy.string.unknown);
		
		this.mMarkerFocusedBase = (pMarkerFocusedBase != null) ? pMarkerFocusedBase : mResourceProxy.getDrawable(ResourceProxy.drawable.marker_default_focused_base);
		
		this.mMarkerFocusedHotSpot = (pMarkerFocusedHotSpot != null) ? pMarkerFocusedHotSpot : DEFAULTMARKER_FOCUSED_HOTSPOT;
		
		if(pFocusedBackgroundColor != NOT_SET)
			this.mMarkerFocusedBackgroundColor = pFocusedBackgroundColor;
		else
			this.mMarkerFocusedBackgroundColor = DEFAULTMARKER_BACKGROUNDCOLOR;
		
		this.mMarkerBackgroundPaint = new Paint(); // Color is set in onDraw(...)
		
		this.mDescriptionPaint = new Paint();
		this.mDescriptionPaint.setAntiAlias(true);
		this.mTitlePaint = new Paint();
		this.mTitlePaint.setFakeBoldText(true);
		this.mTitlePaint.setAntiAlias(true);

		this.mMarkerFocusedWidth = this.mMarkerFocusedBase.getIntrinsicWidth();
		this.mMarkerFocusedHeight = this.mMarkerFocusedBase.getIntrinsicHeight();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public void setFocusedItem(final int pIndex){
		this.mFocusedItemIndex = pIndex;
	}
	
	public void unSetFocusedItem(){
		this.mFocusedItemIndex = NOT_SET;
	}
	
	public void setFocusedItem(final T pItem){
		final int indexFound = super.mItemList.indexOf(pItem);
		if(indexFound < 0)
			throw new IllegalArgumentException();
		
		this.setFocusedItem(indexFound);
	}
	
	public void setFocusItemsOnTap(final boolean doit) {
		this.mFocusItemsOnTap = doit;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================
	
	@Override
	protected boolean onTap(int pIndex) {
		if(this.mFocusItemsOnTap)
			this.mFocusedItemIndex = pIndex;
			
		return super.onTap(pIndex);
	}
	
	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		if(this.mFocusedItemIndex != NOT_SET){
			/* Calculate and set the bounds of the marker. */
			final int left = this.mFocusedScreenCoords.x - this.mMarkerFocusedHotSpot.x;
			final int right = left + this.mMarkerFocusedWidth;
			final int top = this.mFocusedScreenCoords.y - this.mMarkerFocusedHotSpot.y;
			final int bottom = top + this.mMarkerFocusedHeight;
			this.mMarkerFocusedBase.setBounds(left, top, right, bottom);
			
			/* Strings of the OverlayItem, we need. */
			final T focusedItem = super.mItemList.get(this.mFocusedItemIndex);
			final String itemTitle = (focusedItem.mTitle == null) ? UNKNOWN : focusedItem.mTitle;
			final String itemDescription = (focusedItem.mDescription == null) ? UNKNOWN : focusedItem.mDescription;
			
			/* Store the width needed for each char in the description to a float array. This is pretty efficient. */
			final float[] widths = new float[itemDescription.length()];
			this.mDescriptionPaint.getTextWidths(itemDescription, widths);
			
			final StringBuilder sb = new StringBuilder();
			int maxWidth = 0;
			int curLineWidth = 0;
			int lastStop = 0;
			int i;
			int lastwhitespace = 0;
			/* Loop through the charwidth array and harshly insert a linebreak, 
			 * when the width gets bigger than DESCRIPTION_MAXWIDTH. */
			for (i = 0; i < widths.length; i++) {
				if(!Character.isLetter(itemDescription.charAt(i)))
					lastwhitespace = i;
				
				float charwidth = widths[i];
				       
				if(curLineWidth + charwidth> DESCRIPTION_MAXWIDTH){
					if(lastStop == lastwhitespace)
						i--;
					else
						i = lastwhitespace;
					
					
					sb.append(itemDescription.subSequence(lastStop, i));
					sb.append('\n');
					
					lastStop = i;
					maxWidth = Math.max(maxWidth, curLineWidth);
					curLineWidth = 0;
				}
				
				curLineWidth += charwidth;
			}
			/* Add the last line to the rest to the buffer. */
			if(i != lastStop){
				final String rest = itemDescription.substring(lastStop, i);
				
				maxWidth = Math.max(maxWidth, (int)this.mDescriptionPaint.measureText(rest));

				sb.append(rest);
			}
			final String[] lines = sb.toString().split("\n");
			
			/* The title also needs to be taken into consideration for the width calculation. */
			final int titleWidth = (int)this.mDescriptionPaint.measureText(itemTitle);
			
			maxWidth = Math.max(maxWidth, titleWidth);
			final int descWidth = Math.min(maxWidth, DESCRIPTION_MAXWIDTH);
			
			/* Calculate the bounds of the Description box that needs to be drawn. */
			final int descBoxLeft = left - descWidth / 2 - DESCRIPTION_BOX_PADDING + this.mMarkerFocusedWidth / 2;
			final int descBoxRight = descBoxLeft + descWidth + 2 * DESCRIPTION_BOX_PADDING;
			final int descBoxBottom = top;
			final int descBoxTop = descBoxBottom 
						- DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT 
						- (lines.length + 1) * DESCRIPTION_LINE_HEIGHT /* +1 because of the title. */ 
						- 2 * DESCRIPTION_BOX_PADDING;
			
			/* Twice draw a RoundRect, once in black with 1px as a small border. */
			this.mMarkerBackgroundPaint.setColor(Color.BLACK);
			c.drawRoundRect(new RectF(descBoxLeft - 1, descBoxTop - 1, descBoxRight + 1, descBoxBottom + 1),
						DESCRIPTION_BOX_CORNERWIDTH, DESCRIPTION_BOX_CORNERWIDTH,
						this.mDescriptionPaint);
			this.mMarkerBackgroundPaint.setColor(this.mMarkerFocusedBackgroundColor);
			c.drawRoundRect(new RectF(descBoxLeft, descBoxTop, descBoxRight, descBoxBottom),
						DESCRIPTION_BOX_CORNERWIDTH, DESCRIPTION_BOX_CORNERWIDTH, 
						this.mMarkerBackgroundPaint);
			
			final int descLeft = descBoxLeft + DESCRIPTION_BOX_PADDING;
			int descTextLineBottom = descBoxBottom - DESCRIPTION_BOX_PADDING;
			
			/* Draw all the lines of the description. */
			for(int j = lines.length - 1; j >= 0; j--){
				c.drawText(lines[j].trim(), descLeft, descTextLineBottom, this.mDescriptionPaint);
				descTextLineBottom -= DESCRIPTION_LINE_HEIGHT;
			}
			/* Draw the title. */
			c.drawText(itemTitle, descLeft, descTextLineBottom - DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT, this.mTitlePaint);
			c.drawLine(descBoxLeft, descTextLineBottom, descBoxRight, descTextLineBottom, mDescriptionPaint);
			
			/* Finally draw the marker base. This is done in the end to make it look better. */
			this.mMarkerFocusedBase.draw(c);
		}
	}
	
	@Override
	protected void onDrawItem(final Canvas c, final int index, final Point screenCoords) {
		if(this.mFocusedItemIndex != NOT_SET && index == this.mFocusedItemIndex){
			/* Actual draw will take place in onDrawFinished. */
			/* Because we are reusing the screencoords apssed here, we cannot simply store the reference. */
			this.mFocusedScreenCoords.set(screenCoords.x, screenCoords.y);
		}else{
			super.onDrawItem(c, index, screenCoords);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
