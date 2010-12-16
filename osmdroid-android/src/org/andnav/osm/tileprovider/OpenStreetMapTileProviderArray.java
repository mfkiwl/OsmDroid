package org.andnav.osm.tileprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.andnav.osm.tileprovider.modules.OpenStreetMapTileDownloader;
import org.andnav.osm.tileprovider.modules.OpenStreetMapTileModuleProviderBase;
import org.andnav.osm.tileprovider.renderer.IOpenStreetMapRendererInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.Drawable;

/**
 * This top-level tile provider allows a consumer to provide an array of modular
 * asynchronous tile providers to be used to obtain map tiles. When a tile is
 * requested, the ArrayProvider first checks the MapTileCache (synchronously)
 * and returns the tile if available. If not, then the ArrayProvider returns
 * null and sends the tile request through the asynchronous tile request chain.
 * Each asynchronous tile provider returns success/failure to the ArrayProvider.
 * If successful, the ArrayProvider passes the result to the base class. If
 * failed, then the next asynchronous tile provider is called in the chain. If
 * there are no more asynchronous tile providers in the chain, then the failure
 * result is passed to the base class. The ArrayProvider provides a mechanism so
 * that only one unique tile-request can be in the map tile request chain at a
 * time.
 * 
 * @author Marc Kurtz
 * 
 */
public class OpenStreetMapTileProviderArray extends
		OpenStreetMapTileProviderBase implements
		IOpenStreetMapTileProviderCallback {

	private final ConcurrentHashMap<OpenStreetMapTileRequestState, OpenStreetMapTile> mWorking;

	private static final Logger logger = LoggerFactory
			.getLogger(OpenStreetMapTileProviderArray.class);

	protected final List<OpenStreetMapTileModuleProviderBase> mTileProviderList;

	/**
	 * Creates an OpenStreetMapTileProviderArray with no tile providers.
	 * 
	 * @param aRegisterReceiver
	 *            a RegisterReceiver
	 */
	protected OpenStreetMapTileProviderArray(
			final IRegisterReceiver aRegisterReceiver) {
		this(aRegisterReceiver, new OpenStreetMapTileModuleProviderBase[0]);
	}

	/**
	 * Creates an OpenStreetMapTileProviderArray with the specified tile
	 * providers.
	 * 
	 * @param aRegisterReceiver
	 *            a RegisterReceiver
	 * @param tileProviderArray
	 *            an array of OpenStreetMapTileModuleProviderBase
	 */
	public OpenStreetMapTileProviderArray(
			final IRegisterReceiver aRegisterReceiver,
			final OpenStreetMapTileModuleProviderBase[] tileProviderArray) {
		super();

		mWorking = new ConcurrentHashMap<OpenStreetMapTileRequestState, OpenStreetMapTile>();

		mTileProviderList = new ArrayList<OpenStreetMapTileModuleProviderBase>();
		Collections.addAll(mTileProviderList, tileProviderArray);
	}

	@Override
	public void detach() {
		synchronized (mTileProviderList) {
			for (OpenStreetMapTileModuleProviderBase tileProvider : mTileProviderList) {
				tileProvider.detach();
			}
		}
	}

	@Override
	public Drawable getMapTile(final OpenStreetMapTile pTile) {
		if (mTileCache.containsTile(pTile)) {
			if (DEBUGMODE)
				logger.debug("MapTileCache succeeded for: " + pTile);
			return mTileCache.getMapTile(pTile);
		} else {
			boolean alreadyInProgress = false;
			synchronized (mWorking) {
				alreadyInProgress = mWorking.containsValue(pTile);
			}

			if (!alreadyInProgress) {
				if (DEBUGMODE)
					logger.debug("Cache failed, trying from async providers: "
							+ pTile);

				OpenStreetMapTileRequestState state;
				synchronized (mTileProviderList) {
					OpenStreetMapTileModuleProviderBase[] providerArray = new OpenStreetMapTileModuleProviderBase[mTileProviderList
							.size()];
					state = new OpenStreetMapTileRequestState(pTile,
							mTileProviderList.toArray(providerArray), this);
				}

				synchronized (mWorking) {
					// Check again
					alreadyInProgress = mWorking.containsValue(pTile);
					if (alreadyInProgress)
						return null;

					mWorking.put(state, pTile);
				}

				OpenStreetMapTileModuleProviderBase provider = findNextAppropriateProvider(state);
				if (provider != null)
					provider.loadMapTileAsync(state);
				else
					mapTileRequestFailed(state);
			}
			return null;
		}
	}

	@Override
	public void mapTileRequestCompleted(OpenStreetMapTileRequestState aState,
			final Drawable aDrawable) {
		synchronized (mWorking) {
			mWorking.remove(aState);
		}
		super.mapTileRequestCompleted(aState, aDrawable);
	}

	@Override
	public void mapTileRequestFailed(final OpenStreetMapTileRequestState aState) {
		OpenStreetMapTileModuleProviderBase nextProvider = findNextAppropriateProvider(aState);
		if (nextProvider != null) {
			nextProvider.loadMapTileAsync(aState);
		} else {
			synchronized (mWorking) {
				mWorking.remove(aState);
			}
			super.mapTileRequestFailed(aState);
		}
	}

	private OpenStreetMapTileModuleProviderBase findNextAppropriateProvider(
			final OpenStreetMapTileRequestState aState) {
		OpenStreetMapTileModuleProviderBase provider = null;
		// The logic of the while statement is
		// "Keep looping until you get null, or a provider that has a data connection if it needs one"
		do {
			provider = aState.getNextProvider();
		} while ((provider != null)
				&& (!useDataConnection() && provider.getUsesDataConnection()));
		return provider;
	}

	@Override
	public int getMinimumZoomLevel() {
		int result = Integer.MAX_VALUE;
		synchronized (mTileProviderList) {
			for (OpenStreetMapTileModuleProviderBase tileProvider : mTileProviderList) {
				if (tileProvider.getMinimumZoomLevel() < result)
					result = tileProvider.getMinimumZoomLevel();
			}
		}
		return result;
	}

	@Override
	public int getMaximumZoomLevel() {
		int result = Integer.MIN_VALUE;
		synchronized (mTileProviderList) {
			for (OpenStreetMapTileModuleProviderBase tileProvider : mTileProviderList) {
				if (tileProvider.getMaximumZoomLevel() > result)
					result = tileProvider.getMaximumZoomLevel();
			}
		}
		return result;
	}

	@Override
	public void setRenderer(IOpenStreetMapRendererInfo aRenderer) {
		for (OpenStreetMapTileModuleProviderBase tileProvider : mTileProviderList) {
			// We could identify tile providers by an Interface if we wanted to
			// make this extensible.
			if (tileProvider instanceof OpenStreetMapTileDownloader) {
				((OpenStreetMapTileDownloader) tileProvider)
						.setRenderer(aRenderer);
				clearTileCache();
			}
		}
	}
}
