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
		this.config = config.configurationAt("RockBox(0)");
		//ConfigurationManager.dumpConfig(this.config);
		try
		{
			deviceMusicFolder = new File(this.config.getString("MountPointMusicDir"));
			if(log.isTraceEnabled()) { log.trace("Found " + deviceMusicFolder.getAbsolutePath()); }
			device = new CowonD3(this, this.config);
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
		shuffle(m);
		if(m == null)
		{	return; }
		int songsSyncd = 0;
		int lastChecked = 0;
		long freeSpace = device.getAvailableSpaceForTracks();
		SYNCLOOP:for(int i = 0; i < m.length; i++)
		{
			boolean found = false;
			MediaLocation l = m[i].getLocation();
			if(l == null || (l.getSize() != null && l.getSize() >= freeSpace))
			{	continue; } // file is too big, lets try again
			if(log.isTraceEnabled()) { log.trace("Syncing track " + m[i]); }
			File copiedFile = device.copyTo(m[i]);
			if(copiedFile != null)
			{
				freeSpace -= copiedFile.length(); 
				songsSyncd++;
				
			}
			found = true;
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
	
	private void shuffle(Media[] m)
	{
		if(m == null)
		{	return; }
		Random r = new Random(System.currentTimeMillis());
		Media t;
		for(int i = 0; i< m.length; i++)
		{
			int j = r.nextInt(m.length);
			t = m[j];
			m[j] = m[i];
			m[i] = t;
		}
	}
}