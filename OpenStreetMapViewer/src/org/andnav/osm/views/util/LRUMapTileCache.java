// Created by plusminus on 22:13:10 - 28.09.2008
package org.andnav.osm.views.util;

import java.util.HashMap;
import java.util.LinkedList;

import org.andnav.osm.services.util.OpenStreetMapTile;

import android.graphics.Bitmap;

/**
 * Simple LRU cache for any type of object. Implemented as an extended
 * <code>HashMap</code> with a maximum size and an aggregated <code>List</code>
 * as LRU queue.
 * @author Nicolas Gramlich
 *
 */
public class LRUMapTileCache extends HashMap<OpenStreetMapTile, Bitmap> {

	// ===========================================================
	// Constants
	// ===========================================================
	
	private static final long serialVersionUID = 3345124753192560741L;

	// ===========================================================
	// Fields
	// ===========================================================
	
	/** Maximum cache size. */
	private final int maxCacheSize;
	/** LRU list. */
	private final LinkedList<Object> list;

	// ===========================================================
	// Constructors
	// ===========================================================
	
	/**
	 * Constructs a new LRU cache instance.
	 * 
	 * @param maxCacheSize the maximum number of entries in this cache before entries are aged off.
	 */
	public LRUMapTileCache(final int maxCacheSize) {
		super(maxCacheSize);
		this.maxCacheSize = Math.max(1, maxCacheSize);
		this.list = new LinkedList<Object>();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================
	
	/**
	 * Overrides clear() to also clear the LRU list.
	 */
	@Override
	public synchronized void clear() {
		super.clear();
		list.clear();
	}

	/**
	 * Overrides <code>put()</code> so that it also updates the LRU list.
	 * 
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the key
	 * @return previous value associated with key or <code>null</code> if there
	 *         was no mapping for key; a <code>null</code> return can also
	 *         indicate that the cache previously associated <code>null</code>
	 *         with the specified key
	 */
	@Override
	public synchronized Bitmap put(final OpenStreetMapTile key, final Bitmap value) {

		// if the key isn't in the cache and the cache is full...
		if (!super.containsKey(key) && !list.isEmpty() && list.size() + 1 > maxCacheSize) {
			final Object deadKey = list.removeLast();
			Bitmap bm = super.remove(deadKey);
			if (bm != null) {
				bm.recycle();
			}
		}

		updateKey(key);
		return super.put(key, value);
	}

	/**
	 * Overrides <code>get()</code> so that it also updates the LRU list.
	 * 
	 * @param key
	 *            key with which the expected value is associated
	 * @return the value to which the cache maps the specified key, or
	 *         <code>null</code> if the map contains no mapping for this key
	 */
	@Override
	public synchronized Bitmap get(final Object key) {
		final Bitmap value = super.get(key);
		if (value != null) {
			updateKey(key);
		}
		return value;
	}

	@Override
	public synchronized Bitmap remove(final Object key) {
		list.remove(key);
		return super.remove(key);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Moves the specified value to the top of the LRU list (the bottom of the
	 * list is where least recently used items live).
	 * 
	 * @param key of the value to move to the top of the list
	 */
	private void updateKey(final Object key) {
		list.remove(key);
		list.addFirst(key);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
