package house.neko.media.common;

import java.io.File;

import java.awt.image.BufferedImage;

import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public class MediaLibrary
{
	private Log log;
	private HierarchicalConfiguration config;
	private Vector<DataStore> datastores;
	private java.util.Map<String,Media> cache;
	private DataStore primaryStore;
	private LibraryView libraryView = null;
	
	private ViewUpdater updater = null;

	/**
	 *
	 * @param config
	 */
	public MediaLibrary(HierarchicalConfiguration config)
	{
		this.config = config;
		this.log = ConfigurationManager.getLog(MediaLibrary.class);
		cache = java.util.Collections.synchronizedMap(new java.util.HashMap<String,Media>());
		
		datastores = new Vector<DataStore>();
		int maxIndex = config.getMaxIndex("DataStore");
		if(log.isDebugEnabled())
		{	log.debug("Found " + maxIndex + " DataStore(s)"); }
		// load the data stores
		Thread[] startDataStores = new Thread[maxIndex+1];
		for(int j = 0; j < startDataStores.length; j++)
		{
			try
			{
				final HierarchicalConfiguration dsconfig = config.configurationAt("DataStore(" + j + ")");
				final Class dsclass = Class.forName(dsconfig.getString("Type"));
				final DataStore localstore = (DataStore) dsclass.newInstance();
				final MediaLibrary myself = this;
				startDataStores[j] = new Thread(new Runnable() 
				{
					public void run() 
					{
						try
						{
							localstore.init(myself, dsconfig); 
							datastores.add(localstore);
						} catch(Exception e) {
							log.error("Unable to setup datastore!", e);
						}
					}
				});
				startDataStores[j].start();
				if(dsconfig.getBoolean("Primary", false))
				{	primaryStore = localstore; }
			} catch(Exception e) {
				log.error("Unable to setup datastore!", e);
			}
		}
	}
	
	public File getArtworkBasePath()
	{
		try
		{
			return new File(config.getString("Artwork.BasePath"));
		} catch(Exception e) {
			log.error("Unable to get artwork base path", e);
			return null;
		}
	}
	
	public void setArtwork(Media m, String mimeType, BufferedImage image, int index)
	{	primaryStore.setArtwork(m, mimeType, image, index); }
	
	public File getDefaultMediaFile(Media m)
	{	return primaryStore.getDefaultMediaFile(m); }
	
	/**
	 * 
	 * @param m
	 */
	public void addMedia(Media m)
	{	add(m); }
	/**
	 *
	 * @param m
	 */
	public void add(Media m)
	{
		if(m != null)
		{	cache.put(m.getID(), m); }
		synchronized(this)
		{
			if(updater == null)
			{
				updater = new ViewUpdater(this);
				updater.updateView = true;
				updater.sleepTime = 500L;
				updater.start();
			} else {
				updater.updateView = true;
			}
		}
	}
	
	public void saveAllDirty()
	{
		if(primaryStore == null)
		{	log.warn("Unable to save library, no data store marked Primary"); return; }
		if(log.isDebugEnabled())
		{	log.debug("Saving library"); }
		java.util.Iterator<Media> i = cache.values().iterator();
		while(i.hasNext())
		{
			Media m = i.next();
			if(m.getID() == null)
			{	m.setID(m.generateID()); }
			if(m.isDirty())
			{	primaryStore.putMedia(m); }
		}
		if(log.isDebugEnabled())
		{	log.debug("Done saving library"); }
	}
	
	public boolean isDirty()
	{
		if(primaryStore == null)
		{	log.warn("Unable to save library, no data store marked Primary"); return false; }
		if(log.isDebugEnabled())
		{	log.debug("checking for dirt in library"); }
		java.util.Iterator<Media> i = cache.values().iterator();
		while(i.hasNext())
		{
			Media m = i.next();
			if(m.getID() == null)
			{	m.setID(m.generateID()); }
			if(m.isDirty())
			{
				if(log.isDebugEnabled())
				{	log.debug("library was dirty: " + m); }
				return true;
			}
		}
		if(log.isDebugEnabled())
		{	log.debug("library was clean"); }
		return false;
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public Media getMedia(String id)
	{	return cache.get(id); }
	
	public Media getMediaByFile(File f)
	{
		for(Media m : getAllMedia())
		{
			MediaLocation l = m.getLocalLocation();
			try
			{
				File tf = l.getFile();
				if(tf != null && tf.compareTo(f) == 0)
				{	return m; }
			} catch(Exception e) { }
		}
		return null;
	}
	
	public Media[] getAllMedia()
	{	return cache.values().toArray(new Media[cache.size()]); }
	
	/**
	 *
	 */
	public void dump()
	{
		java.util.Iterator<Media> i = cache.values().iterator();
		while(i.hasNext())
		{	System.out.println(i.next()); }
	}
	
	/**
	 *
	 * @param v
	 */
	public void setViewDefault(LibraryView v)
	{
		if(log.isDebugEnabled())
		{	log.debug("Setting default view"); }
		libraryView = v;
		java.util.Vector<Media> t = new java.util.Vector<Media>(cache.values());
		Object[][] o = new Object[t.size()][3];
		String[] cn = { "Title", "Artist" };
		Class[] ct = { "".getClass(), "".getClass() };
		for(int i = 0; i < o.length; i++)
		{
			Media m = t.elementAt(i);
			o[i][0] = m.getName();
			o[i][1] = m.getArtist();
			o[i][2] = m;
		}
		LibrarySearchResult r = new LibrarySearchResult(cn, ct, o);
		v.setResult(r);
		if(log.isDebugEnabled())
		{	log.debug("Set default view with " + o.length + " tracks"); }
	}
	
	public MimeType getMimeTypeByFileExtension(String extension)
	{	return primaryStore.getMimeTypeByFileExtension(extension); }
	
	private class ViewUpdater extends Thread
	{
		public boolean updateView = false;
		public long sleepTime = 1L;
		private Object parent;
		
		public ViewUpdater(Object parent)
		{	this.parent = parent; }
		
		@Override
		public void run()
		{
			if(updateView)
			{
				do
				{
					try
					{
						sleep(sleepTime);
					} catch(InterruptedException ie) {
						log.error(ie); 
					}
					synchronized(parent)
					{
						updateView = false;
						updater = null; 
					}
				} while(updateView == true);
				log.trace("Updating view");
				setViewDefault(libraryView); 
			}
		}
	}
	
	public boolean isSetup()
	{	return primaryStore != null; }
	
	public void shutdown()
	{
		
	}
}