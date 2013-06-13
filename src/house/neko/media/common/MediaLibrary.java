package house.neko.media.common;

import java.io.File;

import java.awt.image.BufferedImage;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;

/**
 *
 * @author andy
 */
public class MediaLibrary extends java.util.Observable 
{
	private Log log;
	private HierarchicalConfiguration config;
	private Vector<DataStore> datastores;
	private java.util.Map<String,Media> cache;
	private DataStore primaryStore;
	private Vector<LibraryView> libraryViews = new Vector<LibraryView>(1, 1);
	private TaskEngine taskEngine;
	
	/**
	 *
	 * @param config
	 */
	public MediaLibrary(HierarchicalConfiguration config)
	{
		this.config = config;
		this.log = ConfigurationManager.getLog(MediaLibrary.class);
		cache = java.util.Collections.synchronizedMap(new java.util.HashMap<String,Media>());
		taskEngine = new TaskEngine();
		taskEngine.addQueue("CD", 1);
		taskEngine.addQueue("cpu", Math.max(1, Runtime.getRuntime().availableProcessors()));
		
		datastores = new Vector<DataStore>();
		int maxIndex = config.getMaxIndex("DataStore");
		if(log.isDebugEnabled())
		{	log.debug("Found " + (maxIndex + 1) + " DataStore(s)"); }
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
	{
		add(m);
		setChanged();
	}
	/**
	 *
	 * @param m
	 */
	public void add(Media m)
	{
		if(m != null)
		{	cache.put(m.getID(), m); }
		setChanged();
	}
	
	public void saveAllDirty()
	{
		if(primaryStore == null)
		{	log.warn("Unable to save library, no data store marked Primary"); return; }
		if(log.isDebugEnabled())
		{	log.debug("Saving library"); }
		java.util.Iterator<Media> i = cache.values().iterator();
		int updatedCount = 0;
		while(i.hasNext())
		{
			Media m = i.next();
			if(m.getID() == null)
			{	m.setID(Media.generateID()); }
			if(m.isDirty())
			{
				if(log.isTraceEnabled())
				{	log.trace("Putting dirty track " + m); }
				primaryStore.putMedia(m);
				updatedCount++;
			}
		}
		if(log.isDebugEnabled())
		{	log.debug("Done saving " + updatedCount + " to library"); }
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
			{	m.setID(Media.generateID()); }
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
	
	
	public Media[] getAlbum(Media m)
	{	return m == null ? null : getAlbum(m.getAlbum()); }
	
	public Media[] getAlbum(String album)
	{
		if(album == null)
		{	return new Media[0]; }
		Vector<Media> v = new Vector<Media>();
		Iterator<Media> i = cache.values().iterator();
		while(i.hasNext())
		{
			Media m = i.next();
			if(album.equals(m.getAlbum()))
			{	v.add(m); }
		}
		return v.toArray(new Media[v.size()]);
	}
	
	/**
	 *
	 * @param v
	 */
	/*public void setViewDefault(LibraryView v)
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
	}*/
	
	public LibraryView getNewView()
	{
		LibraryView v = new LibraryView(this, config.configurationAt("View(0)"));
		libraryViews.add(v);
		addObserver(v);
		v.clearFilter();
		return v;
	}
	
	public void forceUpdate()
	{
		if(log.isTraceEnabled()) { log.trace("Update forced to " + countObservers() + " observers", new Exception()); }
		for(LibraryView v : libraryViews.toArray(new LibraryView[libraryViews.size()]))
		{	v.clearFilter(); }
	}
	
	public void submitTask(Runnable r)
	{	taskEngine.submit(taskEngine.DEFAULT, r); }
	
	public void submitTask(String q, Runnable r)
	{	taskEngine.submit(q, r); }
	
	public MimeType getMimeTypeByFileExtension(String extension)
	{	return primaryStore.getMimeTypeByFileExtension(extension); }
	
	public boolean isSetup()
	{	return primaryStore != null; }
	
	public void shutdown()
	{
		
	}
	
	public Action[] getActions()
	{
		Action[] dataStoreActions = primaryStore.getActions();
		return dataStoreActions;
	}
	
}