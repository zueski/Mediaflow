package house.neko.media.common;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Date;

/**
 *
 * @author andy
 */
public class Media implements java.io.Serializable
{
	private static final long serialVersionUID = 3L;
	// IDs 
	private String id;
	private String parent_id;
	private Long local_id;

	// library specific values
	private long modified_date;    // seconds from epoc, eg 1145205862
	private long added_date;       // seconds from epoc
	private MediaLocation localLocation;
	private MediaLocation remoteLocation;
	private MediaLocation imageLocation;
	
	// user specific values
	private Long last_played_date; // seconds from epoc
	private long play_count;
	private float rating;          // 0 to 5, -1 for unset

	// song 
	private String name;
	private String author;
	private String artist;
	private String artistAlias;
	private String album;
	private Long publish_date;     // seconds from epoc
	private Integer track;             // track number
	
	private long length;           // milliseconds
	
	private boolean isUserDirty;
	private boolean isContentDirty;
	private boolean isBaseDirty;

	/**
	 *
	 */
	public Media()
	{
		id = null;
		parent_id = null;
		local_id = null;
		modified_date = 0L;
		added_date = 0L;
		last_played_date = null;
		play_count = 0L;
		rating = 0F;
		name = null;
		author = null;
		artist = null;
		artistAlias = null;
		album = null;
		publish_date = 0L;
		track = null;
		length = 0;
		localLocation = null;
		remoteLocation = null;
		imageLocation = null;
		isUserDirty = false;
		isContentDirty = false;
		isBaseDirty = false;
	}


	/**
	 * 
	 * @return
	 */
	public String getID()
	{	return id; }
	/**
	 * 
	 * @param i
	 */
	public void setID(String i)
	{
		makeBaseDirty(id, i);
		id = i;
	}

	/**
	 * 
	 * @return
	 */
	public String getParentID()
	{	return parent_id; }
	/**
	 * 
	 * @param i
	 */
	public void setParentID(String i)
	{
		makeBaseDirty(parent_id, i);
		parent_id = i;
	}

	public Long getLocalID()
	{	return local_id; }
	public void setLocalID(Long local_id)
	{
		makeBaseDirty(this.local_id, local_id);
		this.local_id = local_id;
	}

	/**
	 * 
	 * @return
	 */
	public long getModifiedDateMS()
	{	return modified_date; }
	/**
	 * 
	 * @param ms
	 */
	public void setModifiedDateMS(long ms)
	{
		makeBaseDirty(modified_date, ms);
		modified_date = ms;
	}

	/**
	 * 
	 * @return
	 */
	public long getAddedDateMS()
	{	return added_date; }
	
	public Date getAddedDate()
	{	return new Date(added_date); }
	/**
	 * 
	 * @param ms
	 */
	public void setAddedDateMS(long ms)
	{
		makeBaseDirty(added_date, ms);
		added_date = ms; 
	}

	/**
	 * 
	 * @return
	 */
	public Long getLastPlayedDateMS()
	{	return last_played_date; }
	/**
	 * 
	 * @param ms
	 */
	public void setLastPlayedDate(Long ms)
	{
		makeUserDirty(last_played_date, ms);
		last_played_date = ms;
	}

	/**
	 * 
	 * @return
	 */
	public long getPlayCount()
	{	return play_count; }
	/**
	 * 
	 * @param c
	 */
	public void setPlayCount(long c)
	{
		makeUserDirty(play_count, c);
		play_count = c;
	}

	/**
	 * 
	 * @return
	 */
	public float getRating()
	{	return rating; }
	/**
	 * 
	 * @param r
	 */
	public void setRating(float r)
	{
		makeUserDirty(rating, r);
		rating = r;
	}

	/**
	 * 
	 * @return
	 */
	public String getName()
	{	return name; }
	/**
	 * 
	 * @param n
	 */
	public void setName(String n)
	{
		makeBaseDirty(name, n);
		name = n;
	}

	/**
	 * 
	 * @return
	 */
	public String getAuthor()
	{	return author; }
	/**
	 * 
	 * @param a
	 */
	public void setAuthor(String a)
	{
		makeBaseDirty(author, a);
		author = a; 
	}
	
	/**
	 * 
	 * @param a
	 */
	public void setArtist(String a)
	{
		makeBaseDirty(artist, a);
		artist = a;
	}
	public String getArtist()
	{	return artist; }
	
	public void setArtistAlias(String a)
	{
		makeBaseDirty(artistAlias, a);
		artistAlias = a;
	}
	public String getArtistAlias()
	{	return artistAlias; }
	
	/**
	 * 
	 * @param a
	 */
	public void setAlbum(String a)
	{
		makeBaseDirty(album, a);
		album = a; 
	}
	/**
	 * 
	 * @return
	 */
	public String getAlbum()
	{	return album; }
	
	/**
	 * 
	 * @return
	 */
	public Long getPublishedDateMS()
	{	return publish_date; }
	/**
	 * 
	 * @param ms
	 */
	public void setPublishedDate(Long ms)
	{
		makeBaseDirty(publish_date, ms);
		publish_date = ms; 
	}

	/**
	 * 
	 * @return
	 */
	public Integer getTrackNumber()
	{	return track; }
	/**
	 * 
	 * @param t
	 */
	public void setTrackNumber(int t)
	{
		makeBaseDirty(track, t);
		track = t; 
	}

	public long getLength()
	{	return length; }
	public void setLength(long length)
	{	this.length = length; }
	
	/**
	 *
	 * @return
	 * @throws java.io.IOException
	 */
	public InputStream getInputStream()
		throws IOException
	{
		try
		{
			MediaLocation l = getLocation();
			if(l != null)
			{	return l.getInputStream(); }
			return null;
		} catch(Exception e) {
			IOException ioe = new IOException("Exception getting input stream for '" + toString() + "'");
			ioe.initCause(e);
			throw ioe;
		}
	}

	public MediaLocation getLocation()
	{
		if(localLocation != null)
		{	return localLocation; }
		if(remoteLocation != null)
		{	return remoteLocation; }
		return null;
	}
	
	public void setLocalLocation(MediaLocation localLocation)
	{
		makeContentDirty(this.localLocation, localLocation);
		this.localLocation = localLocation; 
	}
	public MediaLocation getLocalLocation()
	{	return localLocation; }

	public void setRemoteLocation(MediaLocation remoteLocation)
	{
		makeContentDirty(this.remoteLocation, remoteLocation);
		this.remoteLocation = remoteLocation; 
	}
	public MediaLocation getRemoteLocation()
	{	return remoteLocation; }

	public void setImageLocation(MediaLocation imageLocation)
	{
		makeContentDirty(this.imageLocation, imageLocation);
		this.imageLocation = imageLocation; 
	}
	public MediaLocation getImageLocation()
	{	return imageLocation; }
	
	/**
	 *
	 * @return
	 */
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		String delim = "|";
		sb.append(id);
		sb.append(delim);
		sb.append(local_id);
		sb.append(delim);
		sb.append(name);
		sb.append(delim);
		sb.append(author);
		sb.append(delim);
		sb.append(artist);
		sb.append(delim);
		sb.append(getLocation());
		sb.append(delim);
		
		return sb.toString();
	}
	
	/**
	 *  This method will (try to) generate a unique ID based on time and hostname
	 * @return
	 */
	public static String generateID()
	{
		StringBuilder sb = new StringBuilder(32);
		sb.append(compressLong(System.currentTimeMillis()));
		try
		{
			String hostname = java.net.InetAddress.getLocalHost().getHostName();
			if(hostname.length() > 24)
			{	hostname = hostname.substring(0, 24); }
			sb.append(hostname);
		} catch (Throwable t) { }
		while(sb.length() < 32)
		{	sb.append(" "); }
		return sb.toString();
	}

	/* This method takes a long (timestamp) and encodes it to a short string for use when creating the unique persistant ID. */
	private static String compressLong(long l)
	{
		StringBuilder sb = new StringBuilder(8);
		long base = (long) compressedBase.length;
		long currbase = base;
		long lastbase = 1;
		System.out.println("Using base " + base);
		long pos = 1;
		while(l > 0)
		{
			currbase = lastbase * base;
			long tmp = l % currbase;
			sb.append(compressedBase[(int) (tmp / lastbase)]);
			//System.out.println("Converting " + (tmp / lastbase) + " " + tmp + ") to " + compressedBase[(int) (tmp / lastbase)] + " l is " + l + " " + (base * pos));
			l -= tmp;
			pos++;
			lastbase = currbase;
		}
		while(pos++ < 9)
		{	sb.append(" "); }
		return sb.toString();
	}

	/* This method takes an encoded string and decodes it into a long (timestamp). */
	private static long uncompressLong(String s)
	{
		s = s.trim();
		long v = 0L;
		long base = (long) compressedBase.length;
		long currbase = 1;
		int loc = 0;
		for(int i = 0; i < s.length(); i++)
		{
			char currchar = s.charAt(i);
			loc = 0;
			while(currchar != compressedBase[loc] && loc++ < base);
			//System.out.println("Converting " + currchar + " (" + compressedBase[loc] + ") to " + (currbase * ((long) loc)));
			v += currbase * ((long) loc);
			currbase *= base;
		}
		return v;
	}
	
	private void makeUserDirty(Comparable _old, Comparable _new)
	{
		if(_old == null)
		{
			if(_new != null)
			{	isUserDirty = true; }
		} else if(_new == null || _old.equals(_new)) {
			isUserDirty = true;
		}
	}
	
	private void makeContentDirty(Object _old, Object _new)
	{
		if(_old == null)
		{
			if(_new != null)
			{	isContentDirty = true; }
		} else if(_new == null || _old.equals(_new)) {
			isContentDirty = true;
		}
	}

	private void makeBaseDirty(Object _old, Object _new)
	{
		if(_old == null)
		{
			if(_new != null)
			{	isBaseDirty = true; }
		} else if(_new == null || _old.equals(_new)) {
			isBaseDirty = true;
		}
	}

	
	public boolean isDirty()
	{	return isUserDirty || isContentDirty || isBaseDirty; }
	
	public void resetDirty()
	{	isUserDirty = isContentDirty = isBaseDirty = false; }

	public boolean isUserDirty()
	{	return isUserDirty; }
	public void resetUserDirty()
	{	isUserDirty = false; }

	public boolean isContentDirty()
	{	return isContentDirty; }
	public void resetContentDirty()
	{	isContentDirty = false; }
	
	public boolean isBaseDirty()
	{	return isBaseDirty; }
	public void resetBaseDirty()
	{	isBaseDirty = false; }

	private static final char compressedBase[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','=','+','*','~','@','#','%','&'};

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("ID: '" + generateID() + "'");
	}
	
	public static final String NAME = "Title";
	public static final String ARTIST = "Artist";
	public static final String ALBUM = "Album";
	public static final String DATEADDED = "Date Added";
	public static final String[] COLUMNS = { NAME, ARTIST, ALBUM, DATEADDED };
}