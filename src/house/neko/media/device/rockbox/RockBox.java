package house.neko.media.device.rockbox;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.MimeType;
import house.neko.media.common.ConfigurationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;


public class RockBox
{
	private Log log;
	private HierarchicalConfiguration config;
	private MediaLibrary library;
	private File rockboxMusicFolder;
	
	public RockBox(MediaLibrary library, HierarchicalConfiguration config)
	{
		this.log = ConfigurationManager.getLog(getClass());
		this.library = library;
		this.config = config;
		try
		{
			rockboxMusicFolder = new File(config.getString("MountPointMusicDir"));
		} catch(Exception e) {
			log.error("Unable to open Rockbox location '" + rockboxMusicFolder.getAbsolutePath() + "'", e);
		}
	}
	
	public void syncRandom(int count)
		throws Exception
	{
		Media[] m = library.getAllMedia();
		Random r = new Random(System.currentTimeMillis());
		if(count > m.length)
		{	count = m.length; }
		int i = 0;
		for(int c = 0; c < count; c++)
		{
			boolean found = false;
			do
			{
				i = r.nextInt(m.length);
				if(m[i] != null)
				{
					if(log.isTraceEnabled())
					{	log.trace("Syncing track " + m[i]); }
					copyTo(m[i]);
					m[i] = null;
					found = true;
				}
			} while(!found);
		}
	}
	
	public void syncToFull()
		throws Exception
	{
		Media[] m = library.getAllMedia();
		Random r = new Random(System.currentTimeMillis());
		int abortSyncAfterFailedCount = config.getInt("AbortSyncAfterFailedCount", 20);
		int abortCount = 0;
		int abortRecoverCount = 0;
		int songsSyncd = 0;
		int i = 0;
		int lastChecked = 0;
		int missedcount = 0;
		long freeSpace = getAvailableSpaceForTracks();
		SYNCLOOP:while(abortSyncAfterFailedCount > abortCount)
		{
			boolean found = false;
			if(lastChecked++ > 50)
			{	freeSpace = getAvailableSpaceForTracks(); lastChecked = 0; }
			int tryUntil = 1000;  // we should give up at somepoint, this problem should be made more correct later
			do
			{
				i = r.nextInt(m.length);
				if(m[i] != null)
				{
					MediaLocation l = m[i].getLocation();
					if(l.getSize() != null && l.getSize() >= freeSpace)
					{	abortCount++; continue; } // file is too big, lets try again
					if(log.isTraceEnabled())
					{	log.trace("Syncing track " + m[i]); }
					File copiedFile = copyTo(m[i]);
					if(copiedFile != null)
					{
						freeSpace -= copiedFile.length(); 
						songsSyncd++;
						if(abortCount > 0 && ++abortRecoverCount > abortSyncAfterFailedCount)
						{	abortCount = abortRecoverCount = 0; }
					} else {
						abortCount++; 
					}
					m[i] = null;
					found = true;
				} else {
					if(missedcount++ > 300)
					{
						if(log.isTraceEnabled())
						{	log.trace("Compacting list, to many misses"); }
						Vector<Media> templist = new Vector<Media>(m.length);
						for(Media tmpmedia : m)
						{
							if(tmpmedia != null)
							{	templist.add(tmpmedia); }
						}
						if(templist.size() < 1)
						{	// exit, no more valid tracks left
							if(log.isTraceEnabled())
							{	log.trace("No valid tracks left, stopping sync"); }
							break SYNCLOOP;
						}
						m = templist.toArray(new Media[templist.size()]);
						missedcount = 0;
					}
				}
			} while(!found && tryUntil-- > 0);
		}
	}
	
	private File copyTo(Media m)
		throws Exception
	{
		MediaLocation l = m.getLocation();
		if(l == null)
		{
			if(log.isTraceEnabled())
			{	log.trace("Skipping '" + m + "', no location available!?"); }
			return null;
		}
		if(l.getMimeType() == null)
		{
			if(log.isTraceEnabled())
			{	log.trace("Skipping '" + m + "', mime type not known!"); }
			return null;
		}
		File df = getDeviceFile(m, l);
		if(df == null)
		{
			if(log.isTraceEnabled())
			{	log.trace("Skipping '" + m + "', cannot exist!"); }
			return null;
		}
		if(df.exists())
		{
			if(log.isTraceEnabled())
			{	log.trace("Skipping '" + m + "', already exists!"); }
			return null;
		}
		if(log.isTraceEnabled())
		{	log.trace(" copying '" + m + "' as " + df.getAbsolutePath()); }
		df.getParentFile().mkdirs();
		FileOutputStream fos;
		InputStream is;
		try
		{
			fos = new FileOutputStream(df);
			is = l.getInputStream();
		} catch(IOException ioe) {
			log.error("Unable to sync media track " + m, ioe);
			return null;
		}
		byte[] buff = new byte[1024];
		int b = 0;
		while((b = is.read(buff)) > 0)
		{	fos.write(buff, 0, b); }
		is.close();
		fos.close();
		COPYARTWORK:try
		{
			
		} catch(Exception e) {
			log.error("unable to copy artwork for " + m, e);
		}
		return df;
	}
	
	public void deleteAll()
		throws IOException
	{
		if(log.isTraceEnabled())
		{	log.trace("Free space is " + getFreeSpace() + " before deleting all"); }
		deleteAll(rockboxMusicFolder, true);
		if(log.isTraceEnabled())
		{	log.trace("Free space is " + getFreeSpace() + " after deleting all"); }
	}
	
	private void deleteAll(File file, boolean atTop)
		throws IOException
	{
		if(file != null && file.exists())
		{
			if(file.isDirectory())
			{
				DIRLOOP:for(File f : file.listFiles())
				{
					if(".".equals(f.getName()) || "..".equals(f.getName()))
					{	continue DIRLOOP; }
					if(log.isTraceEnabled())
					{	log.trace("Deleting " + f.getAbsolutePath()); }
					deleteAll(f, false);
				}
			}
			if(!atTop)
			{
				if(log.isTraceEnabled())
				{	log.trace("Deleting " + file.getAbsolutePath()); }
				file.delete();
			}
		}
	}
	
	public File getDeviceFile(Media m, MediaLocation l)
		throws Exception
	{
		File f = new File(rockboxMusicFolder, m.getArtist());
		String album = m.getAlbum();
		if(album != null)
		{	f = new File(f, sanitizePathPart(album)); }
		f = new File(f, sanitizePathPart(m.getName()) + "." + l.getMimeType().getFileExtension());
		return f;
	}
	
	private String sanitizePathPart(String p)
	{	return p == null ? "" : p.replaceAll("[.\"/\\*?<>|:]", "_"); }
	
	private long getFreeSpace()
	{
		long freeSpace = rockboxMusicFolder.getUsableSpace();
		if(log.isTraceEnabled())
		{	log.trace("Free space left on device is " + freeSpace + " bytes"); }
		return freeSpace;
	}
	
	private long getAvailableSpaceForTracks()
	{
		long freeSpace = getFreeSpace() - config.getLong("ReservedSpace", 0L);
		if(log.isTraceEnabled())
		{	log.trace("Avaliable free space left on device is " + freeSpace + " bytes"); }
		return freeSpace;
	}
}