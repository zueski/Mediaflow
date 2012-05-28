package house.neko.media.device;

import house.neko.media.device.Device;
import house.neko.media.device.RockBox;
import house.neko.media.device.CowonD3;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.MimeType;
import house.neko.media.common.ConfigurationManager;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;

public class Exporter implements ActionListener
{
	private Log log;
	private HierarchicalConfiguration config;
	private MediaLibrary library;
	private File deviceMusicFolder;
	private Device device;
	private String name;
	
	public final String ACTION_SYNC_TRACK = "T";
	
	public Exporter(MediaLibrary library, HierarchicalConfiguration config)
	{
		this.log = ConfigurationManager.getLog(getClass());
		this.library = library;
		this.config = config;
		//ConfigurationManager.dumpConfig(this.config);
		try
		{
			deviceMusicFolder = new File(this.config.getString("MountPointMusicDir"));
			name = config.getString("Name");
			if(log.isTraceEnabled()) { log.trace("Found " + name + " -> " + deviceMusicFolder.getAbsolutePath()); }
			if("CowonD3".equals(this.config.getString("Type")))
			{
				device = new CowonD3(this, this.config);
			} else {
				device = new RockBox(this, this.config);
			}
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
			device.copyTo(t, true); 
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
	
	public Action[] getActions(house.neko.media.slave.LibraryViewPane viewPane)
	{
		int actionCount = 3;
		Action[] deviceActions = device.getActions(viewPane);
		Action[] allActions = new Action[deviceActions.length + actionCount];
		allActions[0] = new DeviceClearAction(this);
		allActions[1] = new DeviceSyncTrackAction(this, viewPane);
		allActions[2] = new DeviceSyncAction(this);
		System.arraycopy(deviceActions, 0, allActions, actionCount, deviceActions.length);
		return allActions;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(log.isDebugEnabled())
		{	log.debug("Action " + e.getActionCommand()); }
		
	}
	
	public class DeviceClearAction extends AbstractAction
	{
		private Exporter exporter;
		public DeviceClearAction(Exporter e)
		{
			super("Clear " + name);
			this.exporter = e;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			log.warn("Syncing to device");
			try
			{
				exporter.clearDevice();
				log.trace("Done purging " + name);
			} catch(Exception ex) {
				log.error("Unable to purge " + name, ex);
			}
		}
	}
	
	public class DeviceSyncTrackAction extends AbstractAction
	{
		private house.neko.media.slave.LibraryViewPane viewPane;
		private Exporter exporter;
		public DeviceSyncTrackAction(Exporter e, house.neko.media.slave.LibraryViewPane viewPane)
		{
			super("Sync selected track(s) to " + name);
			putValue(super.LONG_DESCRIPTION, "Sync selected track(s) to " + name);
			putValue(super.SHORT_DESCRIPTION, "Sync track(s) to " + name);
			this.viewPane = viewPane;
			this.exporter = e;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			log.warn("Syncing track(s) to " + name);
			try
			{
				exporter.sync(viewPane.getSelectedMedia());
				log.trace("Done syncing to " + name);
			} catch(Exception ex) {
				log.error("Unable to sync to device", ex);
			}
		}
	}
	
	public class DeviceSyncAction extends AbstractAction
	{
		private Exporter exporter;
		public DeviceSyncAction(Exporter e)
		{
			super("Sync to " + name);
			this.exporter = e;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			log.warn("Syncing to " + name);
			try
			{
				exporter.syncToFull();
				log.trace("Done syncing to " + name);
			} catch(Exception ex) {
				log.error("Unable to sync to " + name, ex);
			}
		}
	}
}