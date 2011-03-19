package house.neko.media.itunes;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.MimeType;
import house.neko.media.common.ConfigurationManager;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.SQLException;

import org.apache.commons.logging.Log;

import java.net.URI;
import java.io.File;

import java.util.List;
import java.util.Iterator;

/**
 *
 * @author andy
 */
public class ITunesMediaLibraryXMLFile extends DefaultHandler
{
	final static private int TRACKID = 1;
	final static private int NAME = 2;
	final static private int ARTIST = 3;
	final static private int COMPOSER = 4;
	final static private int ALBUM = 5;
	final static private int GENRE = 6;
	final static private int KIND = 7;
	final static private int SIZE = 8;
	final static private int TOTALTIME = 9;
	final static private int DISCNUMBER = 10;
	final static private int DISCCOUNT = 11;
	final static private int TRACKNUMBER = 12;
	final static private int TRACKCOUNT = 13;
	final static private int YEAR = 14;
	final static private int DATEMODIFIED = 15;
	final static private int DATEADDED = 16;
	final static private int BITRATE = 17;
	final static private int SAMPLERATE = 18;
	final static private int PERSISTENTID = 19;
	final static private int TRACKTYPE = 20;
	final static private int FILETYPE = 21;
	final static private int FILECREATOR = 22;
	final static private int LOCATION = 23;
	final static private int FILEFOLDERCOUNT = 24;
	final static private int LIBRARYFOLDERCOUNT = 25;

	private Log log;
	private MediaLibrary library;

	private String trackid;
	private String name;
	private String artist;
	private String composer;
	private String album;
	private String genre;
	private String kind;
	private long size;
	private long totaltime;
	private int discnumber;
	private int disccount;
	private int tracknumber;
	private int trackcount;
	private int year;
	private long datemodified;
	private long dateadded;
	private int bitrate;
	private int samplerate;
	private String persistentid;
	private String tracktype;
	private String filetype;
	private String filecreator;
	private String location;
	private int filefoldercount;
	private int libraryfoldercount;

	private String tempValue;
	private int tempKey;
	
	private boolean inTracks = false;
	private int dictLevel = 0;

	private StringBuilder level;

	//private IWebdavStorage debugger;
	
	

	/**
	 *
	 */
	public ITunesMediaLibraryXMLFile(MediaLibrary library)
	{
		this.log = ConfigurationManager.getLog(getClass());
		this.library = library;
		tempValue = null;
		tempKey = 0;
		//debugger = debuglogger;
		level = new StringBuilder();
		reset();
	}
	

	private void reset()
	{
		trackid = "";
		name = "";
		artist = "";
		composer = "";
		album = "";
		genre = "";
		kind = "";
		size = 0L;
		totaltime = 0L;
		discnumber = 0;
		disccount = 0;
		tracknumber = 0;
		trackcount = 0;
		year = 0;
		datemodified = 0L;
		dateadded = 0L;
		bitrate = 0;
		samplerate = 0;
		persistentid = "";
		tracktype = "";
		filetype = "";
		filecreator = "";
		location = "";
		filefoldercount = 0;
		libraryfoldercount = 0;
	}

	//Event Handlers
	/**
	 *
	 * @param uri
	 * @param localName
	 * @param qName
	 * @param attributes
	 * @throws org.xml.sax.SAXException
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		//reset
		tempValue = null;
		//tempKey = 0;
		if(log.isTraceEnabled())
		{
			log.trace(level.toString() + "Starting element '" + qName + "'"); 
			level.append("    ");
		}
		if(qName.equalsIgnoreCase("dict"))
		{	dictLevel++; }
	}

	/**
	 *
	 * @param ch
	 * @param start
	 * @param length
	 * @throws org.xml.sax.SAXException
	 */
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		
		String t = new String(ch,start,length);
		tempValue = tempValue == null ? t : tempValue + t;
		if(log.isTraceEnabled())
		{	log.trace(level.toString() + "Found value from " + start + " to " + length + " -> '" + tempValue + "'"); }
	}

	private void saveMedia()
		throws Exception
	{
		//mimetypeid = getMimeTypeID(location.replaceAll("^.*\\.([^.]+)$", "$1").toLowerCase());
		Media m = new Media();
		m.setID(persistentid);
		m.setName(name);
		m.setArtist(artist);
		m.setAuthor(composer);
		m.setAlbum(album);
		m.setModifiedDateMS(datemodified);
		m.setAddedDateMS(dateadded);
		MediaLocation mediaLocation = new MediaLocation();
		mediaLocation.setLocationURLString(location);
		if(size > 0L)
		{	mediaLocation.setSize(size); }
		GETTYPE:{
			MimeType mt = getMimeTypeFromFileName(location);
			if(mt != null)
			{	mediaLocation.setMimeType(mt); }
		}
		m.setLocalLocation(mediaLocation);
		GETARTWORK:try
		{
			if(location == null)
			{	break GETARTWORK; }
			org.jaudiotagger.audio.AudioFile f = org.jaudiotagger.audio.AudioFileIO.read(new File(new URI(location)));
			org.jaudiotagger.tag.Tag tag = f.getTag();
			if(tag == null)
			{	break GETARTWORK; }
			List<org.jaudiotagger.tag.datatype.Artwork> list = tag.getArtworkList();
			if(list == null)
			{	break GETARTWORK; }
			int index = 1;
			for(Iterator<org.jaudiotagger.tag.datatype.Artwork> i = list.iterator(); i.hasNext();)
			{
				org.jaudiotagger.tag.datatype.Artwork artwork = i.next();
				library.setArtwork(m, artwork.getMimeType(), artwork.getImage(), index);
				//System.exit(1);
			}
		} catch(Exception e) {
			log.error("Unable to save artwork for " + m + " at '" + location + "'", e);
			//System.exit(1);
		}
		library.add(m);
		if(log.isDebugEnabled())
		{	log.debug("Found media '" + m +"'"); }
	}

	/**
	 *
	 * @param uri
	 * @param localName
	 * @param qName
	 * @throws org.xml.sax.SAXException
	 */
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(log.isTraceEnabled())
		{
			if(level.length() > 3)
			{	level.delete(0, 4); }
			log.debug(level.toString() + "ending element '" + qName + "' with value of '" + tempValue + "'");
		}
		if(qName.equalsIgnoreCase("dict"))
		{
			//add it to the list
			if(inTracks && name != null && name.trim().length() > 0)
			{
				try
				{
					//insertMediaTrack();
					saveMedia();
				} catch(Exception e) {
					log.warn("for '" + location + "'", e);
				}
				reset();
			}
			if(--dictLevel < 1 && inTracks)
			{	inTracks = false; }
		} else if (qName.equalsIgnoreCase("key")) {
			if("tracks".equalsIgnoreCase(tempValue))
			{
				inTracks = true;
			} else if("track id".equalsIgnoreCase(tempValue)) {
				tempKey = TRACKID;
			} else if("name".equalsIgnoreCase(tempValue)) {
				tempKey = NAME;
			} else if("artist".equalsIgnoreCase(tempValue)) {
				tempKey = ARTIST;
			} else if("composer".equalsIgnoreCase(tempValue)) {
				tempKey = COMPOSER;
			} else if("album".equalsIgnoreCase(tempValue)) {
				tempKey = ALBUM;
			} else if("genre".equalsIgnoreCase(tempValue)) {
				tempKey = GENRE;
			} else if("kind".equalsIgnoreCase(tempValue)) {
				tempKey = KIND;
			} else if("size".equalsIgnoreCase(tempValue)) {
				tempKey = SIZE;
			} else if("total time".equalsIgnoreCase(tempValue)) {
				tempKey = TOTALTIME;
			} else if("disc number".equalsIgnoreCase(tempValue)) {
				tempKey = DISCNUMBER;
			} else if("disc count".equalsIgnoreCase(tempValue)) {
				tempKey = DISCCOUNT;
			} else if("track number".equalsIgnoreCase(tempValue)) {
				tempKey = TRACKNUMBER;
			} else if("track count".equalsIgnoreCase(tempValue)) {
				tempKey = TRACKCOUNT;
			} else if("year".equalsIgnoreCase(tempValue)) {
				tempKey = YEAR;
			} else if("date modified".equalsIgnoreCase(tempValue)) {
				tempKey = DATEMODIFIED;
			} else if("date added".equalsIgnoreCase(tempValue)) {
				tempKey = DATEADDED;
			} else if("bit rate".equalsIgnoreCase(tempValue)) {
				tempKey = BITRATE;
			} else if("sample rate".equalsIgnoreCase(tempValue)) {
				tempKey = SAMPLERATE;
			} else if("persistent id".equalsIgnoreCase(tempValue)) {
				tempKey = PERSISTENTID;
			} else if("track type".equalsIgnoreCase(tempValue)) {
				tempKey = TRACKTYPE;
			} else if("file type".equalsIgnoreCase(tempValue)) {
				tempKey = FILETYPE;
			} else if("file creator".equalsIgnoreCase(tempValue)) {
				tempKey = FILECREATOR;
			} else if("location".equalsIgnoreCase(tempValue)) {
				tempKey = LOCATION;
			} else if("file folder count".equalsIgnoreCase(tempValue)) {
				tempKey = FILEFOLDERCOUNT;
			} else if("library folder count".equalsIgnoreCase(tempValue)) {
				tempKey = LIBRARYFOLDERCOUNT;
			}
		} else if (qName.equalsIgnoreCase("string")) {
			if(log.isTraceEnabled())
			{	log.trace(level.toString() + "found string '" + tempValue + "' of type " + tempKey); }
			switch(tempKey)
			{
				case NAME:
					name = tempValue == null ? tempValue : tempValue.trim();
				break;
				case ARTIST:
					artist = tempValue == null ? tempValue : tempValue.trim();
				break;
				case COMPOSER:
					composer = tempValue == null ? tempValue : tempValue.trim();
				break;
				case ALBUM:
					album = tempValue == null ? tempValue : tempValue.trim();
				break;
				case GENRE:
					genre = tempValue == null ? tempValue : tempValue.trim();
				break;
				case KIND:
					kind = tempValue == null ? tempValue : tempValue.trim();
				break;
				case DATEMODIFIED:
					try
					{	datemodified = Long.parseLong(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case DATEADDED:
					try
					{	dateadded = Long.parseLong(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case PERSISTENTID:
					persistentid = tempValue;
				break;
				case TRACKTYPE:
					tracktype = tempValue;
				break;
				case FILETYPE:
					filetype = tempValue;
				break;
				case FILECREATOR:
					filecreator = tempValue;
				break;
				case LOCATION:
					//location = tempValue.replaceAll(";", "#3B");
					// normalize iTunes weird URLs
					location = convertURL(tempValue);
					if(log.isWarnEnabled() && !location.startsWith("file:///"))
					{	log.warn("unable to get correct URL?!  '" + tempValue + "' converts to '" + location + "'");  System.exit(1); }
				break;
			}
		} else if (qName.equalsIgnoreCase("integer")) {
			if(log.isTraceEnabled())
			{	log.trace(level.toString() + "found integer '" + tempValue + "' of type " + tempKey); }
			switch(tempKey)
			{
				case TRACKID:
					trackid = tempValue;
				break;
				case TRACKCOUNT:
					try
					{	trackcount = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case SIZE:
					try
					{	size = Long.parseLong(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case TOTALTIME:
					try
					{	totaltime = Long.parseLong(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case DISCNUMBER:
					try
					{	discnumber = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case SAMPLERATE:
					try
					{	samplerate = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case DISCCOUNT:
					try
					{	disccount = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case TRACKNUMBER:
					try
					{	tracknumber = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case YEAR:
					try
					{	year = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case BITRATE:
					try
					{	bitrate = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case FILEFOLDERCOUNT:
					try
					{	filefoldercount = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				case LIBRARYFOLDERCOUNT:
					try
					{	libraryfoldercount = Integer.parseInt(tempValue); }
					catch(NumberFormatException nfe) { }
				break;
				}
		} else if (qName.equalsIgnoreCase("date")) {
			if(log.isTraceEnabled())
			{	log.trace(level.toString() + "found date '" + tempValue + "' of type " + tempKey); }
		}

	}
	
	protected String convertURL(String url)
	{	return url.replaceAll("^file://localhost/", "file:///"); }
	
	protected MimeType getMimeTypeFromFileName(String fileName)
	{
		if(fileName == null || fileName.length() < 3)
		{	return null; }
		MimeType mt = library.getMimeTypeByFileExtension(fileName.replaceAll("^.*\\.([^.]+)$", "$1").toLowerCase());
		return mt;
	}
	
	/**
	 *
	 * @param doc
	 */
	public void parseInputStream(java.io.InputStream doc)
	{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try
		{
			//connection = DatabaseHelper.getDatabaseConnection();
			//get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			//parse the file and also register this class for call backs
			sp.parse(doc, this);

		} catch(SAXException se) {
			log.error("Unable to parse", se);
		} catch(ParserConfigurationException pce) {
			log.error("Unable to parse", pce);
		} catch (java.io.IOException ie) {
			log.error("Unable to parse", ie);
		} catch (Exception sqle) {
			log.error("Unable to parse", sqle);
		}
	}
	
}
