//
//  ITunesMediaLibraryXMLFile.java
//  musicdav
//
//  Created by Andrew Tomaszewski on 2007.03.24.
//  Copyright 2007 __MyCompanyName__. All rights reserved.
//
package house.neko.media.master.webdav;

import house.neko.media.common.Media;

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
	private long  datemodified;
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

	private StringBuilder level;

	//private IWebdavStorage debugger;
	
	
	//database stuff
	private Connection connection = null;
	private PreparedStatement insertMediaTrackStatement = null;
	private PreparedStatement getMimeTypeStatement = null;
	private PreparedStatement getArtistIDStatement = null;
	private PreparedStatement getArtistIdentityStatement = null;
	private PreparedStatement insertArtistStatement = null;

	/**
	 *
	 */
	public ITunesMediaLibraryXMLFile()
	{
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

	private int getArtistID(String artist)
		throws Exception
	{
		int id = -1;
		if(getArtistIDStatement == null)
		{
			getArtistIDStatement = connection.prepareStatement("SELECT artist_id FROM public.artist WHERE artist_name = ?");
		}
		getArtistIDStatement.setString(1, artist);
		ResultSet rs = getArtistIDStatement.executeQuery();
		if(rs.next())
		{
			id = rs.getInt(1);
			rs.close();
		} else {
			rs.close();

			// get identity for insert
			if(getArtistIdentityStatement == null)
			{
				getArtistIdentityStatement = connection.prepareStatement("select nextval('artist_artist_id_seq')");
			}
			rs = getArtistIdentityStatement.executeQuery();
			if(rs.next())
			{	id = rs.getInt(1); }
			rs.close();

			// insert
			if(insertArtistStatement == null)
			{
				insertArtistStatement = connection.prepareStatement("INSERT INTO public.artist(artist_id, artist_name, artist_audit_user_id, artist_create_timestamp, artist_modify_timestamp) VALUES(?, ?, ?, current_timestamp, current_timestamp)");
			}
			insertArtistStatement.setInt(1, id);
			insertArtistStatement.setString(2, artist);
			insertArtistStatement.setInt(3, 4); // user -> IMPORT
			insertArtistStatement.executeUpdate();
		}
		return id;
	}

	private int getMimeTypeID(String fileextension)
		throws Exception
	{
		int id = -1;
		if(getMimeTypeStatement == null)
		{
			getMimeTypeStatement = connection.prepareStatement("SELECT mime_id FROM public.mime_type WHERE mime_file_extension = ?");
		}
		getMimeTypeStatement.setString(1, fileextension);
		ResultSet rs = getMimeTypeStatement.executeQuery();
		if(rs.next())
		{
			id = rs.getInt(1);
			rs.close();
		} else {
			rs.close();
			return 0;
			//throw new Exception("Unable to find mime type for '" + fileextension + "'!");
		}
		return id;
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
		//debugger.debug(level.toString() + "Starting element '" + qName + "'");
		level.append("    ");
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
		tempValue = new String(ch,start,length);
	}

	private void saveMedia()
		throws Exception
	{
		int artistid = getArtistID(artist);
		int mimetypeid = getMimeTypeID(location.replaceAll("^.*\\.([^.]+)$", "$1").toLowerCase());
		Media m = new Media();
		m.setID(persistentid);
		m.setName(name);
		m.setArtist(artist);
		m.setAuthor(composer);
		m.setAlbum(album);
		m.setModifiedDateMS(datemodified);
		m.setAddedDateMS(dateadded);
		//library.add(m);
		System.out.println("Found media '" + m +"'");
	}
	
	private void insertMediaTrack()
		throws Exception
	{
		int artistid = getArtistID(artist);
		int mimetypeid = getMimeTypeID(location.replaceAll("^.*\\.([^.]+)$", "$1").toLowerCase());
		
		if(insertMediaTrackStatement == null)
		{
			insertMediaTrackStatement = connection.prepareStatement("INSERT INTO public.media_track(track_persistent_id, track_name, track_artist_id, track_artist_alias_id, track_audit_user, track_audit_timestamp, track_add_timestamp, track_length_ms, track_mime_id, track_location_url)  VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		}
		insertMediaTrackStatement.setString(1, persistentid);
		insertMediaTrackStatement.setString(2, name);
		insertMediaTrackStatement.setInt(3, artistid);
		insertMediaTrackStatement.setNull(4, java.sql.Types.INTEGER);
		insertMediaTrackStatement.setInt(5, 4); // userid - >IMPORT
		insertMediaTrackStatement.setTimestamp(6, new Timestamp(dateadded));
		insertMediaTrackStatement.setTimestamp(7, new Timestamp(datemodified));
		insertMediaTrackStatement.setLong(8, totaltime);
		insertMediaTrackStatement.setInt(9, mimetypeid);
		insertMediaTrackStatement.setString(10, location);
		insertMediaTrackStatement.execute();
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
		if(level.length() > 3)
		{
			level.delete(0, 4);
		}
		//debugger.debug(level.toString() + "ending element '" + qName + "' with value of '" + tempValue + "'");
		if(qName.equalsIgnoreCase("dict"))
		{
			//add it to the list
			if(name == null && name.trim().length() < 1)
			{	return; }
			try
			{
				insertMediaTrack();
			} catch(Exception e) {
				//debugger.debug("for '" + location + "' -> " + e.toString());
			}
			reset();
		} else if (qName.equalsIgnoreCase("key")) {
			if("track id".equalsIgnoreCase(tempValue))
			{
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
			//debugger.debug(level.toString() + "found string '" + tempValue + "' of type " + tempKey);
			switch(tempKey)
			{
				case NAME:
					name = tempValue;
				break;
				case ARTIST:
					artist = tempValue;
				break;
				case COMPOSER:
					composer = tempValue;
				break;
				case ALBUM:
					album = tempValue;
				break;
				case GENRE:
					genre = tempValue;
				break;
				case KIND:
					kind = tempValue;
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
					location = tempValue.replaceAll(";", "#3B");
				break;
			}
		} else if (qName.equalsIgnoreCase("integer")) {
			//debugger.debug(level.toString() + "found integer '" + tempValue + "' of type " + tempKey);
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
			//debugger.debug(level.toString() + "found date '" + tempValue + "' of type " + tempKey);
		}

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
			connection = DatabaseHelper.getDatabaseConnection();
			//get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			//parse the file and also register this class for call backs
			sp.parse(doc, this);
			
			connection = null;
			DatabaseHelper.closeConnection(connection);
			
		} catch(SAXException se) {
			//debugger.debug(se.toString());
		} catch(ParserConfigurationException pce) {
			//debugger.debug(pce.toString());
		} catch (java.io.IOException ie) {
			//debugger.debug(ie.toString());
		} catch (Exception sqle) {
			//debugger.debug(sqle.toString());
		}
	}
}
