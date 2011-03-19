package house.neko.media.itunes;


import house.neko.media.common.ConfigurationManager;
import house.neko.media.common.MimeType;
import house.neko.media.common.MediaLibrary;

import java.util.Random;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.URL;

public class ITunesMediaLibraryXMLFileTest extends TestCase
{
	static private String lock = "lock";
	static ITunesMediaLibraryXMLFile itunes = null;
	static MediaLibrary library = null;
	
	public void setUp()
	{
		synchronized(lock)
		{
			if(library == null)
			{
				ConfigurationManager.init(new String[0]);
				library = new MediaLibrary(ConfigurationManager.getConfiguration("Slave(0).MediaLibrary(0)"));
				itunes = new ITunesMediaLibraryXMLFile(library);
			}
		}
	}
	
	@org.junit.Test
	public void testURLConversion()
		throws Exception
	{
		String[] rawURLS = 
			{
				"file://localhost/Music/Compilations/Garage%20Inc.%20%5BDisc%201%5D/1-04%20Turn%20The%20Page.m4a",
				"file://localhost/Music/Brent%20Unkbelt/Unknown%20Album/World%20War.mp3"
			};
		for(String rawURL : rawURLS)
		{
			String convertedURL = itunes.convertURL(rawURL);
			System.out.println("Converting '" + rawURL + "' to '" + convertedURL + "'");
			URL u = new URL(convertedURL);
		}
	}
	
	@org.junit.Test
	public void testMimeTypeGetter()
		throws Exception
	{
		String[] rawURLS = 
			{
				"file://localhost/Music/Compilations/Garage%20Inc.%20%5BDisc%201%5D/1-04%20Turn%20The%20Page.m4a",
				"file://localhost/Music/Brent%20Unkbelt/Unknown%20Album/World%20War.mp3"
			};
		for(String rawURL : rawURLS)
		{
			MimeType mt = itunes.getMimeTypeFromFileName(rawURL);
			System.out.println("Got mime type '" + mt + "' from url '" + rawURL + "'");
			if(mt == null)
			{	fail("Unable to get mime type for '" + rawURL + "'"); }
		}
	}
}