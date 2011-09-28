package house.neko.media.device;

import house.neko.media.device.Device;
import house.neko.media.device.RockBox;
import house.neko.media.device.CowonD3;

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

public class Exporter
{
	private Log log;
	private HierarchicalConfiguration config;
	private MediaLibrary library;
	private File deviceMusicFolder;
	private Device device;
	
	public Exporter(MediaLibrary library, HierarchicalConfiguration config)
	{
		this.log = ConfigurationManager.getLog(getClass());
		this.library = library;
		this.config = config;
		try
		{
			deviceMusicFolder = new File(config.getString("MountPointMusicDir"));
			if(log.isTraceEnabled()) { log.trace("Found " + deviceMusicFolder.getAbsolutePath()); }
			device = new CowonD3(this, config);
		} catch(Exception e) {
			log.error("Unable to open device location '" + deviceMusicFolder.getAbsolutePath() + "'", e);
		}
	}
	
	public void syncRandom(int count)
		throws Exception
	{
		if(log.isDebugEnabled()) { log.debug("Starting to sync"); }
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
					device.copyTo(m[i]);
					m[i] = null;
					found = true;
				}
			} while(!found);
		}
		if(log.isDebugEnabled()) { log.debug("Done syncing"); }
	}
	
	public void syncToFull()
		throws Exception
	{
		if(log.isDebugEnabled()) { log.debug("Syncing to full"); }
		Media[] m = library.getAllMedia();
		Random r = new Random(System.currentTimeMillis());
		int abortSyncAfterFailedCount = config.getInt("AbortSyncAfterFailedCount", 20);
		int abortCount = 0;
		int abortRecoverCount = 0;
		int songsSyncd = 0;
		int i = 0;
		int lastChecked = 0;
		int missedcount = 0;
		long freeSpace = device.getAvailableSpaceForTracks();
		SYNCLOOP:while(abortSyncAfterFailedCount > abortCount)
		{
			boolean found = false;
			if(lastChecked++ > 50)
			{	freeSpace = device.getAvailableSpaceForTracks(); lastChecked = 0; }
			int tryUntil = 1000;  // we should give up at somepoint, this problem should be made more correct later
			do
			{
				i = r.nextInt(m.length);
				if(m[i] != null)
				{
					MediaLocation l = m[i].getLocation();
					if(l.getSize() != null && l.getSize() >= freeSpace)
					{	abortCount++; continue; } // file is too big, lets try again
					if(log.isTraceEnabled()) { log.trace("Syncing track " + m[i]); }
					File copiedFile = device.copyTo(m[i]);
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
						if(log.isTraceEnabled()) { log.trace("Compacting list, to many misses"); }
						Vector<Media> templist = new Vector<Media>(m.length);
						for(Media tmpmedia : m)
						{
							if(tmpmedia != null)
							{	templist.add(tmpmedia); }
						}
						if(templist.size() < 1)
						{	// exit, no more valid tracks left
							if(log.isTraceEnabled()) { log.trace("No valid tracks left, stopping sync"); }
							break SYNCLOOP;
						}
						m = templist.toArray(new Media[templist.size()]);
						missedcount = 0;
					}
				}
			} while(!found && tryUntil-- > 0);
		}
		if(log.isDebugEnabled()) { log.debug("Done syncing"); }
	}
	
	public void sync(Media[] m)
		throws Exception
	{
		if(log.isTraceEnabled()) { log.trace("Starting to sync tracks"); }
		if(m == null)
		{	return; }
		for(Media t : m)
		{
			if(log.isTraceEnabled()) { log.trace("Sync track: " + m); }
			device.copyTo(t); 
		}
	}
	
	public void clearDevice()
	{
		try
		{
			if(log.isTraceEnabled()) { log.trace("Clearing device"); }
			device.deleteAll();
			if(log.isTraceEnabled()) { log.trace("Cleared device"); }
		} catch(IOException ioe) {
			log.error("Unable to clear device " + device, ioe);
		}
	}
}