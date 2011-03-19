package house.neko.media.common;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

import java.awt.image.BufferedImage;

import org.apache.commons.logging.Log;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 *
 *  2009-07-06 andy: Abandoning this class for the moment until the interface stabilizes
 */
public class FileDataStore implements DataStore
{
	private HierarchicalConfiguration config = null;
	private Log log = null;
	private MediaLibrary library = null;
	
	private java.io.File filestore;
	private java.util.Map<String, Integer> line_number_map;
	private java.io.LineNumberReader filereader;
	private Object lock = new Object();

	private static final String FILE_DELIMETER = "\t";
	private static final int STORE_ID = 0;
	private static final int STORE_PARENT_ID = 1;
	private static final int STORE_MODIFIED_DATE = 2;
	private static final int STORE_ADDED_DATE = 3;
	private static final int STORE_LAST_PLAYED_DATE = 4;
	private static final int STORE_PLAY_COUNT = 5;
	private static final int STORE_RATING = 6;
	private static final int STORE_NAME = 7;
	private static final int STORE_AUTHOR = 8;
	private static final int STORE_PUBLISH_DATE = 9;
	private static final int STORE_TRACK_NUMBER = 10;
	private static final int STORE_LENGTH = 11;
	private static final int STORE_REMOTE_LOCATION_URL = 12;
	private static final int STORE_LOCAL_LOCATION_URL = 13;
	private static final int STORE_IMAGE_URL = 14;

	/**
	 *
	 * @param library
	 * @param config
	 */
	public void init(MediaLibrary library, HierarchicalConfiguration config)
	{
		this.log = ConfigurationManager.getLog(FileDataStore.class);
		this.config = config;
		this.library = library;
		
		filestore = new java.io.File(config.getString("File"));
		FileDataStore$LoadMedia loader = new FileDataStore$LoadMedia();
		new Thread(loader).start();
		log.trace("FileDataSTore initialized with file " + filestore.getAbsolutePath());
	}

	/* MUST SYNCHRONIZE OUTSIDE OF THIS FUNCTION */
	private void cleanThyself()
	{
		try
		{
			if(filestore == null)
			{
				if(log.isWarnEnabled())
				{	log.warn("File database not set for store!"); }
				return; 
			}
			if(!filestore.exists())
			{
				if(log.isWarnEnabled())
				{	log.warn("File database not found for store '" + filestore.getAbsolutePath() + "'"); }
				return; 
			}
			filereader = new java.io.LineNumberReader(new java.io.FileReader(filestore));
			line_number_map = java.util.Collections.synchronizedMap(new java.util.HashMap<String, Integer>());
			String line = null;
			for(int i = 0;(line = filereader.readLine()) != null; i++)
			{
				int pos = line.indexOf(FILE_DELIMETER);
				if(pos > 0)
				{	line_number_map.put(line.substring(0, pos), Integer.valueOf(i)); }
			}
			if(log.isTraceEnabled())
			{   log.trace("Read " + line_number_map.size() + " lines from " + filestore.getAbsolutePath()); }
		} catch(Exception e) {
			log.error(e.toString(), e);
		}
		getAllMedia();
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public Media getMedia(String id)
	{	return readMedia(id); }

	/**
	 *
	 * @return
	 */
	public Media[] getAllMedia()
	{
		Media[] m = new Media[0];
		synchronized(lock)
		{
			try
			{
				java.util.Vector<Media> v = new java.util.Vector<Media>(line_number_map.size());
				filereader = new java.io.LineNumberReader(new java.io.FileReader(filestore));
				String line = null;
				for(int i = 0;(line = filereader.readLine()) != null; i++)
				{
					v.add(convertLineToMedia(line));
				}
				m = v.toArray(m);
				log.trace("Found " + m.length + " tracks, loading");
				for(int k = 0; k < m.length; k++)
				{   library.addMedia(m[k]); }
				log.trace("Tracks loaded");
			} catch(Exception e) { 
				log.error(e.toString(), e);
			}
		}
		return m;
	}

	/**
	 *
	 * @param m
	 */
	public void putMedia(Media m)
	{
		try
		{
			writeMedia(m);
		} catch(Exception e) {
			log.error(e.toString(), e);
		}
	}
	
	public void setArtwork(Media m, String mimeType, BufferedImage image, int index) { }
	
	public MimeType getMimeTypeByFileExtension(String extension)
	{	return null; }

	public DataStoreConfigurationHelper getConfigurationHelper()
	{
		String[] keys = 
			{
				"User",
				"Type",
				"File",
				"Primary"
			};
		String[] defaultValues = 
			{
				System.getProperty("user.name"),
				this.getClass().getName(),
				"MediaFlow.dat",
				"true"
			};
		String[] descriptions = 
			{
				"Your name",
				"FileDataStore class, leave this alone",
				"Filename",
				"Flag to indicate primary datastore (where new media gets stored)",
			};
		return new DataStore.DataStoreConfigurationHelper(keys, defaultValues, descriptions);
	}
	
	private Media readMedia(String id)
	{
		Media m = null;
		synchronized(lock)
		{
			Integer linenumber = line_number_map.get(id);
			if(linenumber != null)
			{	m = convertLineToMedia(getLine(linenumber.intValue())); }
		}
		return m;
	}

	private void writeMedia(Media m)
		throws Exception
	{
		synchronized(lock)
		{
			boolean isNew = false;
			if(m.getID() == null)
			{	m.setID(Media.generateID()); }

			BufferedWriter writer = new BufferedWriter(new FileWriter(filestore, isNew));

			line_number_map.get(m.getID());
			if(isNew)
			{
				// append
				String line = convertMediaToLine(m);
				writer.append(line, 0, line.length());
			} else {
				// write out all
				Media[] allm = getAllMedia();
				for(int i = 0; i < allm.length; i++)
				{
					String line = convertMediaToLine(allm[i]);
					writer.append(line, 0, line.length());
				}
			}
			writer.close();
			cleanThyself();
		}
	}

	/* MUST SYNCHRONIZE OUTSIDE OF THIS FUNCTION */
	private String getLine(int linenumber)
	{
		String line = "";
		try
		{
			filereader.setLineNumber(linenumber);
			line = filereader.readLine();
		} catch(Exception e) {
			log.error(e.toString(), e);
		}
		return line;
	}

	private Media convertLineToMedia(String line)
	{
		Media m = new Media();
		java.util.StringTokenizer st = new java.util.StringTokenizer(line, FILE_DELIMETER, false);
		for(int currPos = 0; st.hasMoreTokens(); currPos++)
		{
			switch(currPos)
			{
				case STORE_ID: m.setID(st.nextToken()); break;
				case STORE_PARENT_ID: m.setParentID(st.nextToken()); break;
				case STORE_MODIFIED_DATE: m.setModifiedDateMS(Long.parseLong(st.nextToken())); break;
				case STORE_ADDED_DATE: m.setAddedDateMS(Long.parseLong(st.nextToken())); break;
				case STORE_LAST_PLAYED_DATE: m.setLastPlayedDate(Long.parseLong(st.nextToken())); break;
				case STORE_PLAY_COUNT: m.setPlayCount(Integer.parseInt(st.nextToken())); break;
				case STORE_RATING: m.setRating(Float.parseFloat(st.nextToken())); break;
				case STORE_NAME: m.setName(st.nextToken()); break;
				case STORE_AUTHOR: m.setAuthor(st.nextToken()); break;
				case STORE_PUBLISH_DATE: m.setPublishedDate(Long.parseLong(st.nextToken())); break;
				case STORE_TRACK_NUMBER: m.setTrackNumber(Integer.parseInt(st.nextToken())); break;
				case STORE_LENGTH: m.setPublishedDate(Long.parseLong(st.nextToken())); break;
				//case STORE_REMOTE_LOCATION_URL: m.setRemoteLocationURLString(st.nextToken()); break;
				//case STORE_LOCAL_LOCATION_URL: m.setLocalLocationURLString(st.nextToken()); break;
				//case STORE_IMAGE_URL: m.setImageLocationURLString(st.nextToken()); break;
			}
		}
		return m;
	}
	
	public File getDefaultMediaFile(Media m) { return null; }

	private String convertMediaToLine(Media m)
	{
		StringBuffer sb = new StringBuffer();
		int maxPos = STORE_IMAGE_URL;
		for(int currPos = 0; currPos <= maxPos; currPos++)
		{
			switch(currPos)
			{
				case STORE_ID: sb.append(m.getID()); break;
				case STORE_PARENT_ID: sb.append(m.getParentID()); break;
				case STORE_MODIFIED_DATE: sb.append(Long.toString(m.getModifiedDateMS())); break;
				case STORE_ADDED_DATE: sb.append(Long.toString(m.getAddedDateMS())); break;
				case STORE_LAST_PLAYED_DATE: sb.append(Long.toString(m.getLastPlayedDateMS())); break;
				case STORE_PLAY_COUNT: sb.append(Long.toString(m.getPlayCount())); break;
				case STORE_RATING: sb.append(Float.toString(m.getRating())); break;
				case STORE_NAME: sb.append(m.getName()); break;
				case STORE_AUTHOR: sb.append(m.getAuthor()); break;
				case STORE_PUBLISH_DATE: sb.append(Long.toString(m.getPublishedDateMS())); break;
				case STORE_TRACK_NUMBER: sb.append(m.getTrackNumber()); break;
				case STORE_LENGTH: sb.append(Long.toString(m.getPublishedDateMS())); break;
				//case STORE_REMOTE_LOCATION_URL: sb.append(m.getRemoteLocationURLString()); break;
				//case STORE_LOCAL_LOCATION_URL: sb.append(m.getLocalLocationURLString()); break;
				//case STORE_IMAGE_URL: sb.append(m.getImageURLString()); break;
			}
			if(currPos < maxPos)
			{	sb.append(FILE_DELIMETER); }
		}
		return sb.toString();
	}
	
	private class FileDataStore$LoadMedia implements Runnable
	{
		public void run()
		{
			cleanThyself();
		}
	}
}