package house.neko.media.common;

/**
 *
 * @author andy
 */
public class MediaList implements java.io.Serializable
{
	private static final long serialVersionUID = 3L;
	// IDs 
	private String id;
	private Long local_id;
	
	private long modified_date;    // seconds from epoc, eg 1145205862
	private long added_date;       // seconds from epoc
	
	private String name;
	private String artist;
	private String artistAlias;
	private Long publish_date;     // seconds from epoc
	
	private boolean isUserDirty;
	private boolean isContentDirty;
	private boolean isBaseDirty;
	
	public MediaList()
	{
		id = null;
		local_id = null;
		modified_date = 0L;
		added_date = 0L;
		name = null;
		artist = null;
		artistAlias = null;
		publish_date = 0L;
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
}