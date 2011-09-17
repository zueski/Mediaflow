package house.neko.media.device;

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


public class RockBox implements house.neko.media.device.Device
{
	private Log log;
	private HierarchicalConfiguration config;
	private Exporter exporter;
	private File rockboxMusicFolder;
	
	private final java.util.TreeSet<String> _valid_sub_mime_types;
	
	public RockBox(Exporter exporter, HierarchicalConfiguration config)
	{
		this.log = ConfigurationManager.getLog(getClass());
		this.config = config;
		this.exporter = exporter;
		this._valid_sub_mime_types = new java.util.TreeSet<String>();
		this._valid_sub_mime_types.add("mp3");
		this._valid_sub_mime_types.add("m4a");
		this._valid_sub_mime_types.add("flac");
		try
		{
			rockboxMusicFolder = new File(config.getString("MountPointMusicDir"));
		} catch(Exception e) {
			log.error("Unable to open Rockbox location '" + rockboxMusicFolder.getAbsolutePath() + "'", e);
		}
	}
	
	public File copyTo(Media m)
		throws IOException
	{
		MediaLocation l = m.getLocation();
		if(l == null)
		{
			if(log.isTraceEnabled()) { log.trace("Skipping '" + m + "', no location available!?"); }
			return null;
		}
		MimeType mimeType = l.getMimeType();
		if(mimeType == null)
		{
			if(log.isTraceEnabled()) { log.trace("Skipping '" + m + "', mime type not known!"); }
			return null;
		}
		if(!"audio".equalsIgnoreCase(mimeType.getMimeType()))
		{
			if(log.isTraceEnabled()) { log.trace("Skipping '" + m + "', mime not audio!"); }
			return null;
		}
		boolean convert = !_valid_sub_mime_types.contains(mimeType.getMimeSubType());
		File df = convert ? getDeviceFile(m, l) : getConvertedDeviceFile(m, l);
		if(df == null)
		{
			if(log.isTraceEnabled()) { log.trace("Skipping '" + m + "', cannot exist!"); }
			return null;
		}
		if(df.exists())
		{
			if(log.isTraceEnabled()) { log.trace("Skipping '" + m + "', already exists!"); }
			return null;
		}
		if(log.isTraceEnabled()) { log.trace(" copying '" + m + "' as " + df.getAbsolutePath()); }
		df.getParentFile().mkdirs();
		FileOutputStream fos;
		try
		{
			fos = new FileOutputStream(df);
		} catch(IOException ioe) {
			log.error("Unable to open output stream '" + df.getAbsolutePath() + "' for " + m, ioe);
			return null;
		}
		InputStream is;
		if(convert)
		{
			if(log.isTraceEnabled()) { log.trace("Need to convert mime type " + mimeType.getMimeSubType() + " for '" + m + "'"); }
			try
			{
				is = l.getInputStream();
			} catch(IOException ioe) {
				log.error("Unable to convert input stream for " + m, ioe);
				return null;
			}
		} else {
			try
			{
				is = l.getInputStream();
			} catch(IOException ioe) {
				log.error("Unable to open input stream for " + m, ioe);
				return null;
			}
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
		if(log.isTraceEnabled()) { log.trace("Free space is " + getFreeSpace() + " before deleting all"); }
		deleteAll(rockboxMusicFolder, true);
		if(log.isTraceEnabled()) { log.trace("Free space is " + getFreeSpace() + " after deleting all"); }
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
					if(log.isTraceEnabled()) { log.trace("Deleting " + f.getAbsolutePath()); }
					deleteAll(f, false);
				}
			}
			if(!atTop)
			{
				if(log.isTraceEnabled()) { log.trace("Deleting " + file.getAbsolutePath()); }
				file.delete();
			}
		}
	}
	
	public File getDeviceFile(Media m, MediaLocation l)
		throws IOException
	{
		File f = new File(rockboxMusicFolder, m.getArtist());
		String album = m.getAlbum();
		if(album != null)
		{	f = new File(f, sanitizePathPart(album)); }
		f = new File(f, sanitizePathPart(m.getName()) + "." + l.getMimeType().getFileExtension());
		return f;
	}
	
	public File getConvertedDeviceFile(Media m, MediaLocation l)
		throws IOException
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
	
	public long getFreeSpace()
	{
		long freeSpace = rockboxMusicFolder.getUsableSpace();
		if(log.isTraceEnabled()) { log.trace("Free space left on device is " + freeSpace + " bytes"); }
		return freeSpace;
	}
	
	public long getAvailableSpaceForTracks()
	{
		long freeSpace = getFreeSpace() - config.getLong("ReservedSpace", 0L);
		if(log.isTraceEnabled()) {log.trace("Avaliable free space left on device is " + freeSpace + " bytes"); }
		return freeSpace;
	}
}