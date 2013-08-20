package house.neko.media.common;

import javax.swing.Action;
import java.awt.event.ActionListener;

import java.awt.image.BufferedImage;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 *
 * DataStores should never reset the dirty bits!
 *
 */
public abstract interface DataStore 
//extends ActionListener
{
	/**
	 *
	 * @param library
	 * @param config
	 * @throws java.lang.Exception
	 */
	public abstract void init(MediaLibrary library, HierarchicalConfiguration config) throws Exception;
	/**
	 *
	 * @param id
	 * @return
	 */
	public abstract Media getMedia(String id);
	/**
	 *
	 * @return
	 */
	// TODO: rename this to loadAllMedia()
	public abstract Media[] getAllMedia();
	
	public abstract Action[] getActions();
	/**
	 *
	 * @param m
	 */
	public abstract void putMedia(Media m);
	
	public abstract void putMediaList(MediaList l);
	
	public abstract MimeType getMimeTypeByFileExtension(String extension);
	
	public abstract DataStoreConfigurationHelper getConfigurationHelper();
	
	public abstract void setArtwork(Media m, String mimeType, BufferedImage image, int index);
	
	public abstract java.io.File getDefaultMediaFile(Media m);
	
	static class DataStoreConfigurationHelper
	{
		private String[] keys;
		private String[] defaultValues;
		private String[] descriptions;
		public String[] values;
		
		public DataStoreConfigurationHelper(String[] keys, String[] defaultValues, String[] descriptions)
		{
			this.keys = keys;
			this.defaultValues = defaultValues;
			this.descriptions = descriptions;
			this.values = new String[keys.length];
		}
		
		public String[] getConfigurationKeys()
		{	return keys; }
		
		public String getDefaultValue(String key)
		{	return findValue(defaultValues, key); }
		
		public String getDescription(String key)
		{	return findValue(descriptions, key); }
		
		private String findValue(String[] haystack, String needle)
		{
			for(int i = 0; i < keys.length; i++)
			{
				if(keys[i] == needle || keys[i].equals(needle))
				{	 return haystack[i]; }
			}
			return "";
		}
	}
}