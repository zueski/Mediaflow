package house.neko.media.common;

import house.neko.media.common.datastore.FileReorgAction;
import house.neko.media.common.datastore.FileCheckAction;

import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.TreeMap;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.concurrent.RunnableFuture;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.image.BufferedImage;
import java.io.File;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.DatabaseMetaData;

import org.apache.commons.logging.Log;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 */
public class DatabaseDataStore implements DataStore
{
	//private
	private HierarchicalConfiguration config = null;
	private Log log = null;
	private MediaLibrary library = null;
	private File artworkBasePath;
	
	private static final String REORG_FILES_ACTION = "C";
	
	private String usernm = "UNKNOWN";
	private int userid = 0;
	private Connection _conn = null;
	// statements
	private PreparedStatement insertMediaTrackStatement = null;
	private PreparedStatement getArtistIDStatement = null;
	private PreparedStatement insertArtistAliasStatement = null;
	private PreparedStatement getArtistIdentityStatement = null;
	private PreparedStatement insertArtistStatement = null;
	private PreparedStatement getMediaTrackIdentityStatement = null;
	private PreparedStatement getMediaLocationUpdateableStatement = null;
	private PreparedStatement getMediaTrackByPersistantID = null;

	// caches
	private boolean _is_loaded = false;
	private TreeMap<String,MimeType> cacheFileExtenstionMimeType = new TreeMap<String,MimeType>();
	private TreeMap<Integer,MimeType> cacheMimeTypeID = new TreeMap<Integer,MimeType>();
	private TreeMap<String,Integer> cacheArtistID = new TreeMap<String,Integer>();
	private TreeMap<String,Integer> cacheArtistAliasID = new TreeMap<String,Integer>();

	/**
	 *
	 * @param library
	 * @param config
	 * @throws java.lang.Exception
	 */
	public void init(MediaLibrary library, HierarchicalConfiguration config)
		throws Exception
	{
		this.log = ConfigurationManager.getLog(getClass());
		if(log.isTraceEnabled())
		{	log.trace("Initializing database store"); }
		this.config = config;
			
		try 
		{
			Class c = Class.forName(config.getString("Driver"));
		} catch(Exception e) { 
			log.error(e.toString(), e); 
		}
		try
		{
			artworkBasePath = new File(config.getString("Artwork.BasePath"));
			if(log.isDebugEnabled())
			{	log.debug("Using artwork base path " + artworkBasePath); }
		} catch(Exception e) {
			log.error("Unable to get artwork base path", e);
		}
		
		this.library = library;
		try
		{
			usernm = config.getString("User");
			_conn = getConnection();
			_conn.setAutoCommit(false);
			if(log.isInfoEnabled())
			{
				DatabaseMetaData metadata = _conn.getMetaData();
				log.info("Using driver " + metadata.getDriverName() + " version " + metadata.getDriverVersion());
				log.info("Connected to " + metadata.getDatabaseProductName() + " version " + metadata.getDatabaseProductVersion());
				
			}
			PreparedStatement s = _conn.prepareStatement("select user_id from public.user where user_nm=?");
			s.setString(1, usernm);
			ResultSet rs = s.executeQuery();
			if(!rs.next())
			{
				log.error("Unable to connect to datastore as user '" + usernm + "'!"); 
			} else {
				userid = rs.getInt(1);
			}
			rs.close();
			s.close();
			_conn.commit();
		} catch(SQLException e) {
			log.error(e.toString(), e);
		}
		DatabaseStore$LoadMedia loader = new DatabaseStore$LoadMedia();
		new Thread(loader).start();
		if(log.isTraceEnabled())
		{	log.trace("Initialion of database store complete"); }
	}

	private Connection getConnection()
		throws java.sql.SQLException
	{
		if(_conn == null)
		{
			log.trace("Getting connection");
			String url = config.getString("URL");
			log.trace("URL = " + url);
			String user = config.getString("Username");
			log.trace("USER = " + user);
			String pass = config.getString("Password");
			log.trace("PASSWORD = " + pass);
			_conn = DriverManager.getConnection(url, user, pass);
			if(log.isTraceEnabled())
			{	log.trace("Created connection to " + url + " with user " + user); }
		}
		return _conn;
	}
	
	private void returnConnection(Connection c)
		throws SQLException
	{
		c.commit();
		return; 
	}
	
	private Long getLong(ResultSet rs, int position)
		throws SQLException
	{
		Long l = rs.getLong(position);
		if(rs.wasNull())
		{	l = null; }
		return l;
	}

	private MediaList mapResultSetToMediaList(ResultSet rs)
		throws java.sql.SQLException
	{
		return null;
	}
	
	private Media mapResultSetToMedia(ResultSet rs)
		throws java.sql.SQLException
	{
		Media m = new Media();
		m.setLocalID(rs.getLong(SQL_SELECT_TRACK_ID_POS));
		m.setID(rs.getString(SQL_SELECT_TRACK_PERSISTENT_ID_POS));
		m.setModifiedDateMS(rs.getDate(SQL_SELECT_TRACK_AUDIT_TIMESTAMP_POS).getTime());
		m.setAddedDateMS(rs.getDate(SQL_SELECT_TRACK_ADD_TIMESTAMP_POS).getTime());
		m.setName(rs.getString(SQL_SELECT_TRACK_NAME_POS));
		m.setArtist(rs.getString(SQL_SELECT_TRACK_ARTIST_NAME_POS));
		m.setAlbum(rs.getString(SQL_SELECT_TRACK_ALBUM));
		int t = rs.getInt(SQL_SELECT_TRACK_ALBUM_TRACK_NO_POS);
		if(!rs.wasNull())
		{	m.setTrackNumber(t); }
		SETLOCATIONS:{
			String localURL = rs.getString(SQL_SELECT_TRACK_URL_LOCAL_POS);
			if(localURL != null)
			{
				if(log.isTraceEnabled())
				{	log.trace("Media " + m.getID() + " has local URl!"); }
				MediaLocation localLocation = new MediaLocation();
				localLocation.setLocationURLString(localURL);
				localLocation.setMimeType(getMimeTypeByID(rs.getInt(SQL_SELECT_TRACK_MIME_TYPE_ID_LOCAL_POS), rs.getStatement().getConnection()));
				localLocation.setLocationValid("T".equals(rs.getString(SQL_SELECT_TRACK_IS_VALID_LOCAL)));
				long size = rs.getLong(SQL_SELECT_TRACK_SIZE_LOCAL_POS);
				if(!rs.wasNull())
				{	localLocation.setSize(size); }
				m.setLocalLocation(localLocation);
			}
			String remoteURL = rs.getString(SQL_SELECT_TRACK_URL_REMOTE_POS);
			if(remoteURL != null)
			{
				if(log.isTraceEnabled())
				{	log.trace("Media " + m.getID() + " has local URl!"); }
				MediaLocation remoteLocation = new MediaLocation();
				remoteLocation.setLocationURLString(remoteURL);
				remoteLocation.setMimeType(getMimeTypeByID(rs.getInt(SQL_SELECT_TRACK_MIME_TYPE_ID_REMOTE_POS), rs.getStatement().getConnection()));
				remoteLocation.setLocationValid("T".equals(rs.getString(SQL_SELECT_TRACK_IS_VALID_REMOTE)));
				long size = rs.getLong(SQL_SELECT_TRACK_SIZE_REMOTE_POS);
				if(!rs.wasNull())
				{	remoteLocation.setSize(size); }
				m.setRemoteLocation(remoteLocation);
			}
		}
		m.resetDirty();
		if(log.isTraceEnabled())
		{	log.trace("Mapped media -> " + m); }
		return m;
	}
	
	/**
	 *
	 * @param id
	 * @return
	 */
	public Media getMedia(String id)
	{
		if(_conn == null)
		{	log.error("DatabaseDatastore not connected!");  return null; }
		PreparedStatement s = null;
		ResultSet rs = null;
		try
		{
			s = _conn.prepareStatement(SQL_SELECTMEDIA + " WHERE track_persistent_id = ?");
			s.setString(1, id);
			rs = s.executeQuery();
			if(!rs.next())
			{
				rs.close();
				s.close();
				return null;
			}

			Media m = mapResultSetToMedia(rs);

			rs.close();
			s.close();

			return m;
		} catch(Exception e) {
			log.error(e.toString(), e);
			return null;
		}
	}

	/**
	 *
	 * @return
	 */
	public Media[] getAllMedia()
	{
		while(!_is_loaded)
		{	try { Thread.sleep(500L); } catch(Exception ie) { } }
		return library.getAllMedia();
	}
	
	
	public Media[] loadAllMedia()
	{
		if(_conn == null)
		{	log.error("DatabaseDatastore not connected!");  return new Media[0]; }
		Media[] list = null;
		PreparedStatement s = null;
		ResultSet rs = null;
		try
		{
			long startTimeMS = 0L;
			if(log.isDebugEnabled())
			{	log.debug("Getting all media from database store"); startTimeMS = System.currentTimeMillis(); }
			// get IDs from db
			
			s = _conn.prepareStatement(SQL_SELECTMEDIA);
			rs = s.executeQuery();
			
			Vector<Media> mlist = new Vector<Media>();
			while(rs.next())
			{   mlist.add(mapResultSetToMedia(rs)); }
			rs.close();
			s.close();
			
			if(log.isTraceEnabled())
			{	log.trace("Found " + mlist.size() + " tracks, loading"); }
			list = mlist.toArray(new Media[mlist.size()]);
			for(int k = 0; k < list.length; k++)
			{	library.addMedia(list[k]); }
			if(log.isDebugEnabled())
			{	log.debug("Loaded " + list.length + " tracks in " + (System.currentTimeMillis() - startTimeMS) + " millisecons"); }
		} catch(Exception e) {
			log.error(e.toString(), e);
		}
		return list;
	}
	
	public MediaList[] loadAllMediaList()
	{
		if(_conn == null)
		{	log.error("DatabaseDatastore not connected!");  return new MediaList[0]; }
		MediaList[] list = null;
		PreparedStatement s = null;
		ResultSet rs = null;
		try
		{
			long startTimeMS = 0L;
			if(log.isDebugEnabled())
			{	log.debug("Getting all media list from database store"); startTimeMS = System.currentTimeMillis(); }
			// get IDs from db
			
			s = _conn.prepareStatement(SQL_SELECTMEDIALIST);
			rs = s.executeQuery();
			
			Vector<MediaList> mlist = new Vector<MediaList>();
			long lastMediaListID = -1L;
			MediaList currList = null;
			Vector<MediaListEntry> tracks = new Vector<MediaListEntry>(30);
			while(rs.next())
			{
				long mediaListID = rs.getLong(SQL_SELECTMEDIALIST_LIST_ID_POS);
				if(lastMediaListID != mediaListID)
				{
					if(lastMediaListID > -1L)
					{
						if(log.isTraceEnabled())
						{	log.trace("MediaListLoad - adding " + currList.getName() + " (" + currList.getLocalID() + ")"); }
						currList.setTrackList(tracks.toArray(new MediaListEntry[tracks.size()]));
						currList.resetDirty();
						mlist.add(currList);
						tracks = new Vector<MediaListEntry>(30);
					}
					currList = new MediaList();
					currList.setLocalID(mediaListID);
					currList.setID(Long.toString(mediaListID));
					currList.setName(rs.getString(SQL_SELECTMEDIALIST_LIST_NAME_POS));
				}
				tracks.add(new MediaListEntry(library.getMedia(rs.getString(SQL_SELECTMEDIALIST_TRACK_PERSISTENT_ID_POS)), rs.getInt(SQL_SELECTMEDIALIST_SEQ_NBR_POS)));
				lastMediaListID = mediaListID;
			}
			rs.close();
			s.close();
			
			if(log.isTraceEnabled())
			{	log.trace("Found " + mlist.size() + " lists, loading"); }
			list = mlist.toArray(new MediaList[mlist.size()]);
			for(int k = 0; k < list.length; k++)
			{	library.addMediaList(list[k]); }
			if(log.isDebugEnabled())
			{	log.debug("Loaded " + list.length + " lists in " + (System.currentTimeMillis() - startTimeMS) + " millisecons"); }
		} catch(Exception e) {
			log.error(e.toString(), e);
		}
		return list;
	}

	/**
	 *
	 * @param m
	 */
	public void putMedia(Media m)
	{
		if(m == null)
		{	return; }
		if(_conn == null)
		{	log.error("DatabaseDatastore not connected!");  return; }
		if(log.isTraceEnabled())
		{	log.trace("Writing Media " + m.toString() + " to database"); }
		try
		{
			try
			{
				long trackid = getLocalID(m, _conn);
				if(m.isBaseDirty())
				{
					if(log.isTraceEnabled())
					{	log.trace("Need to update base " + m.getID() + " to database"); }
					PreparedStatement s = _conn.prepareStatement("update media_track set track_name=?,track_artist_id=?,track_artist_alias_id=?,track_album=?,track_album_num=?,track_audit_timestamp=CURRENT_TIMESTAMP where track_id = ?");
					s.setString(1, m.getName());
					s.setInt(2, getArtistID(m, _conn));
					s.setInt(3, getArtistAliasID(m, _conn));
					s.setString(4, m.getAlbum());
					if(m.getTrackNumber() != null)
					{
						s.setInt(5, m.getTrackNumber());
					} else {
						s.setNull(5, java.sql.Types.INTEGER);
					}
					s.setLong(6, trackid);
					s.executeUpdate();
					s.close();
				}
				if(m.isUserDirty())
				{
					if(log.isTraceEnabled())
					{	log.trace("Need to update user " + m.getID() + " to database"); }
					PreparedStatement s = _conn.prepareStatement("update media_track_rating set rating=?, play_count=? where track_id=? and user_id=?");
					s.setFloat(1, m.getRating());
					s.setLong(2, m.getPlayCount());
					s.setLong(3, trackid);
					s.setLong(4, userid);
					if(s.executeUpdate() != 1)
					{
						s.close();
					}
					s.close();
				}
				if(m.isContentDirty())
				{	updateLocation(m, _conn); }
				_conn.commit();
				m.resetDirty();
				if(log.isTraceEnabled())
				{	log.trace("Committed " + m.getID() + " to database"); }
			} catch(Exception e) {
				log.error(e.toString(), e);
				_conn.rollback();
			}
		} catch(Exception e) {
			log.error(e.toString(), e);
		}
	}
	
	public void putMediaList(MediaList l)
	{
		
	}
	
	public void setArtwork(Media m, String mimeType, BufferedImage image, int index)
	{
		try
		{
			if(mimeType == null || image== null || m == null)
			{	log.error("Unable to save image '" + image + " of type '" + mimeType + "' for " + m); return; }
			// create filename
			File imageFile = new File(artworkBasePath, sanitizeFilenamePart(m.getArtist()));
			if(m.getAlbum() != null && m.getAlbum().length() > 0)
			{	imageFile = new File(imageFile, sanitizeFilenamePart(m.getAlbum())); }
			String fileExtension = mimeType.replaceAll("^(?:[^/]*?/)?(.*)", "$1");
			imageFile = new File(imageFile, sanitizeFilenamePart(m.getName() + "-" + index +"." + fileExtension));
			// check if in DB already
			
			Iterator<javax.imageio.ImageWriter> wi = javax.imageio.ImageIO.getImageWritersByMIMEType(mimeType);
			if(!wi.hasNext())
			{	log.error("Unable to get image writer for '" + mimeType + "', will not save artwork for " + m); return; }
			
			
			if(imageFile.getParentFile().mkdirs() && log.isDebugEnabled())
			{	log.debug("Created directory '" + imageFile.getParentFile().getAbsolutePath() + "'"); }
			javax.imageio.ImageWriter imageWriter = wi.next();
			javax.imageio.stream.FileImageOutputStream outputStream = new javax.imageio.stream.FileImageOutputStream(imageFile);
			imageWriter.setOutput(outputStream);
			imageWriter.write(image);
			imageWriter.dispose();
			outputStream.close();
			
			if(log.isDebugEnabled())
			{	log.debug("Saving artwork type '" + fileExtension + "' for " + m + " to '" + imageFile.getAbsolutePath() + "'"); }
		} catch(Exception e) {
			log.error("Unable to set artwork for " + m, e);
		}
	}
	
	private void updateLocation(Media m, Connection c)
		throws SQLException
	{
		if(m.isContentDirty())
		{	// avoid most db calls
			PreparedStatement s = c.prepareStatement("update media_track_location set location_url=?,mime_id=?,location_size=?,is_valid=? where track_id=? and location_type=?");
			boolean updateLocal = m.getLocalLocation() != null;
			boolean updateRemote = m.getRemoteLocation() != null;
			if(updateLocal)
			{
				MimeType mt = m.getLocalLocation().getMimeType();
				if(mt == null)
				{	mt = getMimeTypeByFileExtension(m.getLocalLocation().getLocationURLString()); }
				if(log.isDebugEnabled())
				{	log.debug("Updating local location to (" +  m.getLocalLocation().isLocationValid() + ") " + mt + " with URL " + m.getLocalLocation().getLocationURLString()); }
				s.setString(1, m.getLocalLocation().getLocationURLString());
				if(mt == null || mt.getLocalID() == null)
				{
					s.setNull(2, Types.INTEGER);
				} else {
					s.setInt(2, mt.getLocalID());
				}
				if(m.getLocalLocation().getSize() != null)
				{
					s.setLong(3, m.getLocalLocation().getSize());
				} else {
					s.setNull(3, Types.BIGINT);
				}
				s.setString(4, m.getLocalLocation().isLocationValid() ? "T" : "F");
				s.setLong(5, m.getLocalID());
				s.setString(6, URL_LOCATION_TYPE_LOCAL);
				s.executeUpdate();
				updateLocal = false;
			} 
			if(updateRemote)
			{
				MimeType mt = m.getRemoteLocation().getMimeType();
				if(mt == null)
				{	mt = getMimeTypeByFileExtension(m.getRemoteLocation().getLocationURLString()); }
				if(log.isTraceEnabled())
				{	log.trace("Updating remote location to (" +  m.getRemoteLocation().isLocationValid() + ") " + mt + " with URL " + m.getRemoteLocation().getLocationURLString()); }
				s.setString(1, m.getRemoteLocation().getLocationURLString());
				if(mt == null || mt.getLocalID() == null)
				{
					s.setNull(2, Types.INTEGER);
				} else {
					s.setInt(2, mt.getLocalID());
				}
				if(m.getRemoteLocation().getSize() != null)
				{
					s.setLong(3, m.getRemoteLocation().getSize());
				} else {
					s.setNull(3, Types.BIGINT);
				}
				s.setString(4, m.getRemoteLocation().isLocationValid() ? "T" : "F");
				s.setLong(4, m.getLocalID());
				s.setString(5, URL_LOCATION_TYPE_REMOTE);
				s.executeUpdate();
				updateRemote = false;
			}
			s.close();
			if(updateLocal || updateRemote)
			{
				s = c.prepareStatement("insert into media_track_location (track_id,location_type,mime_id,location_url,location_size) values (?,?,?,?,?)");
				if(updateLocal)
				{
					s.setLong(1, m.getLocalID());
					s.setString(2, URL_LOCATION_TYPE_LOCAL);
					if(m.getLocalLocation() != null)
					{
						if(m.getLocalLocation().getMimeType() != null)
						{
							MimeType type = getMimeTypeByFileExtension(m.getLocalLocation().getMimeType().getFileExtension());
							if(type == null)
							{	type = getMimeTypeByFileExtension(m.getLocalLocation().getLocationURLString()); }
							if(type != null)
							{
								setIntegerForStatement(s, 3, type.getLocalID());
							} else {
								s.setNull(3, Types.INTEGER);
							}
						} else {
							s.setNull(3, Types.INTEGER);
						}
						s.setString(4, m.getLocalLocation().getLocationURLString());
						if(m.getLocalLocation().getSize() != null)
						{
							s.setLong(5, m.getLocalLocation().getSize());
						} else {
							s.setNull(5, Types.BIGINT);
						}
					} else {
						s.setNull(3, Types.INTEGER);
						s.setNull(4, Types.VARCHAR);
						s.setNull(5, Types.BIGINT);
					}
					s.addBatch();
				}
				if(updateRemote)
				{
					s.setLong(1, m.getLocalID());
					s.setString(2, URL_LOCATION_TYPE_REMOTE);
					if(m.getRemoteLocation() != null)
					{
						if(m.getRemoteLocation().getMimeType() != null)
						{
							setIntegerForStatement(s, 3, getMimeTypeByFileExtension(m.getLocalLocation().getMimeType().getFileExtension()).getLocalID());
						} else {
							s.setNull(3, Types.INTEGER);
						}
						s.setString(4, m.getRemoteLocation().getLocationURLString());
						if(m.getRemoteLocation().getSize() != null)
						{
							s.setLong(5, m.getRemoteLocation().getSize());
						} else {
							s.setNull(5, Types.BIGINT);
						}
					} else {
						s.setNull(3, Types.INTEGER);
						s.setNull(4, Types.VARCHAR);
						s.setNull(5, Types.BIGINT);
					}
					s.addBatch();
				}
				s.executeBatch();
				s.close();
			}
		}
	}
	
	public MimeType getMimeTypeByURL(String url)
	{
		if(url == null)
		{	return null; }
		Pattern p = Pattern.compile("^.*\\.([^.]+)$");
		Matcher m = p.matcher(url);
		if(m.matches())
		{
			return getMimeTypeByFileExtension(m.group(1));
		} else {
			if(log.isTraceEnabled())
			{	log.trace("Unable to interpret URL '" + url+ "' to get extension"); }
		}
		return null;
	}
	
	public MimeType getMimeTypeByFileExtension(String extension)
	{
		MimeType type = cacheFileExtenstionMimeType.get(extension);
		if(type == null)
		{
			if(log.isTraceEnabled())
			{	log.trace("Mime cache miss for '" + extension + "'"); }
			try
			{
				Connection c = getConnection();
				try
				{
					type = getMimeTypeByFileExtension(extension, c);
				} catch(SQLException sqle) {
					log.error("Unable to get mime type for file extension '" + extension + "'", sqle);
				}
				returnConnection(c);
			} catch(SQLException sqle) {
				log.error("Unable to get connection to get mime type for file extension '" + extension + "'", sqle);
			}
		}
		return type;
	}
	
	private MimeType getMimeTypeByFileExtension(String extension, Connection c)
		throws SQLException
	{
		if(extension == null || extension.length() < 1)
		{	return null; }
		extension = extension.toLowerCase();
		MimeType type = cacheFileExtenstionMimeType.get(extension);
		if(type == null)
		{
			PreparedStatement s = c.prepareStatement("select mime_id,mime_type, mime_sub_type, mime_file_extension, mime_file_type,mime_file_creator from mime_type where mime_file_extension = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			s.setString(1, extension);
			ResultSet rs = s.executeQuery();
			if(rs.next())
			{
				type = new MimeType();
				type.setLocalID(rs.getInt(1));
				type.setMimeType(rs.getString(2));
				type.setMimeSubType(rs.getString(3));
				type.setFileExtension(rs.getString(4));
				type.setFileCreatorID(getLong(rs, 5));
				type.setFileTypeID(getLong(rs, 6));
				cacheFileExtenstionMimeType.put(extension, type);
				if(log.isTraceEnabled())
				{	log.trace("Got type for getMimeTypeByFileExtension(\"" + extension + "\") -> " + type + " with local ID " + rs.getInt(1)); }
			} else {
				if(log.isTraceEnabled())
				{	log.trace("Type for getMimeTypeByFileExtension(\"" + extension + "\") NOT FOUND!"); }
			}
			rs.close();
			s.close();
		}
		return type;
	}
	
	private MimeType getMimeTypeByID(Integer id, Connection c)
		throws SQLException
	{
		MimeType type = cacheMimeTypeID.get(id);
		if(type == null)
		{
			PreparedStatement s = c.prepareStatement("select mime_type, mime_sub_type, mime_file_extension, mime_file_type,mime_file_creator from mime_type where mime_id = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			s.setInt(1, id);
			ResultSet rs = s.executeQuery();
			if(rs.next())
			{
				type = new MimeType();
				type.setLocalID(id);
				type.setFileExtension(rs.getString(3));
				type.setMimeType(rs.getString(1));
				type.setMimeSubType(rs.getString(2));
				type.setFileCreatorID(getLong(rs, 5));
				type.setFileTypeID(getLong(rs, 4));
				cacheMimeTypeID.put(id, type);
				if(log.isTraceEnabled())
				{	log.trace("Got type for getMimeTypeByID(\"" + id + "\") -> " + type); }
			} else {
				if(log.isTraceEnabled())
				{	log.trace("Type for getMimeTypeByID(\"" + id + "\") NOT FOUND!"); }
			}
			rs.close();
			s.close();
		}
		return type;
	}
	
	private Long getLocalID(Media m, Connection c)
		throws SQLException
	{
		Long localID = m.getLocalID();
		if(localID == null)
		{
			if(getMediaTrackByPersistantID == null)
			{	getMediaTrackByPersistantID = _conn.prepareStatement("select track_id from media_track where track_persistent_id = ?"); }
			getMediaTrackByPersistantID.setString(1, m.getID());
			ResultSet rs = getMediaTrackByPersistantID.executeQuery();
			if(rs != null && rs.next())
			{
				localID = rs.getLong(1);
				m.setLocalID(localID);
				rs.close();
			} else {
				rs.close();
				if(log.isTraceEnabled())
				{	log.trace("Need to insert " + m.getID() + " to database"); }
				int artist = getArtistID(m, _conn);
				if(insertMediaTrackStatement == null)
				{	insertMediaTrackStatement = _conn.prepareStatement("insert into media_track(track_name,track_artist_id,track_artist_alias_id,track_album,track_album_num,track_audit_user,track_audit_timestamp,track_add_timestamp,track_length_ms,track_persistent_id) values (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?)", Statement.RETURN_GENERATED_KEYS); }
				insertMediaTrackStatement.setString(1, m.getName());
				insertMediaTrackStatement.setInt(2, artist);
				insertMediaTrackStatement.setInt(3, getArtistAliasID(m, _conn));
				insertMediaTrackStatement.setString(4, m.getAlbum());
				if(m.getTrackNumber() != null && m.getTrackNumber() > 0)
				{
					insertMediaTrackStatement.setInt(5, m.getTrackNumber());
				} else {
					insertMediaTrackStatement.setNull(5, java.sql.Types.INTEGER);
				}
				insertMediaTrackStatement.setInt(6, userid);
				insertMediaTrackStatement.setLong(7, m.getLength());
				insertMediaTrackStatement.setString(8, m.getID());
				insertMediaTrackStatement.executeUpdate();
				if(log.isTraceEnabled())
				{	log.trace("inserting track " + m.getName() + "' with artist '" + m.getArtist() + "' (" +artist + ")"); }
				rs = insertMediaTrackStatement.getGeneratedKeys();
				if(rs.next())
				{
					localID = rs.getLong(1);
					m.setLocalID(localID);
					rs.close();
				} else {
					rs.close();
					throw new SQLException("Unable to get track_id key from insert for " + m);
				}
			}
		}
		return localID;
	}
	
	private Integer getArtistID(Media m, Connection c)
		throws SQLException
	{
		if(m.getArtist() == null)
		{	return null; }
		Integer artistID = cacheArtistID.get(m.getArtist());
		if(artistID != null)
		{	return artistID; }
		if(getArtistIDStatement == null)
		{	getArtistIDStatement = c.prepareStatement("SELECT artist_id FROM artist WHERE artist_name=?"); }
		getArtistIDStatement.setString(1, m.getArtist());
		ResultSet rs = getArtistIDStatement.executeQuery();
		if(rs.next())
		{
			artistID = rs.getInt(1);
			rs.close();
			cacheArtistID.put(m.getArtist(), artistID);
		} else {
			rs.close();
			if(insertArtistStatement == null)
			{	insertArtistStatement = c.prepareStatement("INSERT INTO artist(artist_name,artist_audit_user_id,artist_create_timestamp,artist_modify_timestamp) VALUES(?,?,current_timestamp,current_timestamp)", Statement.RETURN_GENERATED_KEYS); }
			insertArtistStatement.setString(1, m.getArtist());
			insertArtistStatement.setInt(2, userid);
			insertArtistStatement.executeUpdate();
			rs = insertArtistStatement.getGeneratedKeys();
			if(rs.next())
			{
				artistID = rs.getInt(1);
				rs.close();
			} else {
				rs.close();
				throw new SQLException("Unable to get artist_id key from insert for " + m);
			}
			if(log.isDebugEnabled())
			{	log.debug("Adding artist '" + m.getArtist() + "' with local ID of " + artistID); }
		}
		return artistID;
	}
	
	private Integer getArtistAliasID(Media m, Connection c)
		throws SQLException
	{
		return getArtistID(m, c);
		/*
		// try the cache first
		Integer artistAliasID = cacheArtistID.get(m.getArtist());
		if(artistAliasID != null)
		{	return artistAliasID; }
		// try the DB next
		Integer artistID = getArtistID(m, c);
		PreparedStatement s = c.prepareStatement("select artist_alias_id from artist_alias where artist_id = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		s.setInt(1, artistID);
		ResultSet rs = getArtistIDStatement.executeQuery();
		if(rs.next())
		{
			// use the found aliasID
			artistAliasID = rs.getInt(1);
			rs.close();
		} else {
			rs.close();
			// need to insert as last resort, assume that alias is the artist
			artistAliasID = artistID;
			if(insertArtistAliasStatement == null)
			{	insertArtistAliasStatement = c.prepareStatement("INSERT INTO artist_alias(artist_id, artist_alias_id, artist_alias_audit_user_id, artist_alias_create_timestamp) VALUES(?, ?, ?, current_timestamp)"); }
			insertArtistAliasStatement.setInt(1, artistID);
			insertArtistAliasStatement.setInt(2, artistAliasID);
			insertArtistAliasStatement.setInt(3, userid);
			insertArtistAliasStatement.executeUpdate();
		}
		// was not found in cache, saving before returning
		cacheArtistID.put(m.getArtist(), artistAliasID);
		return artistAliasID;*/
	}
	
	private void setIntegerForStatement(PreparedStatement s, int position, Integer i)
		throws SQLException
	{
		if(i != null)
		{
			s.setInt(position, i);
		} else {
			s.setNull(position, Types.INTEGER);
		}
	}
	
	public void submitTask(Runnable r)
	{	library.submitTask(r); }
	
	public File getDefaultMediaFile(File base, Media m)
	{
		try
		{
			File f = (base != null ? base : new File(config.getString("MusicBasePath")));
			f = new File(f, sanitizeFilenamePart(m.getArtist()));
			if(m.getAlbum() != null)
			{	f = new File(f, sanitizeFilenamePart(m.getAlbum())); }
			Integer t = m.getTrackNumber();
			String track = "";
			if(t != null)
			{
				if(t < 10)
				{
					track = "0" + t; 
				} else {
					track = t.toString();
				}
			}
			f = new File(f, sanitizeFilenamePart(track + m.getName()) + "." + m.getLocation().getMimeType().getFileExtension());
			return f;
		} catch(Exception e) {
			log.error("Unable to get media path for " + m, e);
			return null;
		}
	}
	public File getDefaultMediaFile(Media m)
	{
		try
		{
			File f = new File(config.getString("MusicBasePath"));
			return getDefaultMediaFile(f, m);
		} catch(Exception e) {
			log.error("Unable to get media path", e);
			return null;
		}
	}
	
	private String sanitizeFilenamePart(String namePart)
	{	return namePart == null ? "" : namePart.replaceAll("['\"/\\*!?%&^$#@]", "_"); }
	
	private class DatabaseStore$LoadMedia implements Runnable
	{
		public void run()
		{
			loadAllMedia();
			loadAllMediaList();
			_is_loaded = true;
			library.forceUpdate();
		}
	}
	
	public DataStoreConfigurationHelper getConfigurationHelper()
	{
		String[] keys = 
			{
				"User",
				"Type",
				"Driver",
				"URL",
				"Username",
				"Primary"
			};
		String[] defaultValues = 
			{
				System.getProperty("user.name"),
				this.getClass().getName(),
				"org.postgresql.Driver",
				"jdbc:postgresql://localhost:5432/music?logUnclosedConnections=true?loginTimeout=10",
				"music",
				"music",
				"true"
			};
		String[] descriptions = 
			{
				"Your name",
				"DataStore class, leave this alone",
				"JDBC Driver class",
				"JDBC Connection URL",
				"JDBC Connection username",
				"JDBC Connection password",
				"Flag to indicate primary datastore (where new media gets stored)",
			};
		return new DataStore.DataStoreConfigurationHelper(keys, defaultValues, descriptions);
	}
	
	public Action[] getActions()
	{
		Action[] a = new Action[2];
		a[0] = new FileReorgAction(this, config);
		a[1] = new FileCheckAction(this, config);
		return a;
	}
	
	
	
	
	/**
	 *
	 */
	@Override
	protected final void finalize()
	{
		try
		{
			if(_conn != null && !_conn.isClosed())
			{
				_conn.close();
			}
		} catch(SQLException e) {
			log.error(e.toString(), e);
		}	
	}
	
	private final String URL_LOCATION_TYPE_LOCAL = "L";
	private final String URL_LOCATION_TYPE_REMOTE = "R";
	private final String SQL_SELECTMEDIALIST = "SELECT " + 
										"ml.list_id," +					//  1
										"ml.list_name," +				//  2
										"ml.list_artist_id," +			//  3
										"ml.list_release_date," +		//  4
										"ml.list_audit_user_id," +		//  5
										"ml.list_modify_timestamp," +	//  6
										"ml.list_create_timestamp," +	//  7
										"mtl.seq_nbr," +				//  8
										"mt.track_id," +				//  9
										"mt.track_persistent_id " +		// 10
										"from media_list ml " +
										"inner join media_track_list mtl on (ml.list_id=mtl.list_id) " +
										"inner join media_track mt on (mtl.track_id=mt.track_id) " +
										"order by ml.list_id,mtl.seq_nbr,mt.track_id";
	private final int SQL_SELECTMEDIALIST_LIST_ID_POS = 1;
	private final int SQL_SELECTMEDIALIST_LIST_NAME_POS = 2;
	private final int SQL_SELECTMEDIALIST_LIST_ARTIST_ID_POS = 3;
	private final int SQL_SELECTMEDIALIST_LIST_RELEASE_DATE_POS = 4;
	private final int SQL_SELECTMEDIALIST_LIST_AUDIT_USER_ID_DATE_POS = 5;
	private final int SQL_SELECTMEDIALIST_LIST_MODIFY_TIMESTAMP_POS = 6;
	private final int SQL_SELECTMEDIALIST_LIST_CREATE_TIMESTAMP_POS = 7;
	private final int SQL_SELECTMEDIALIST_SEQ_NBR_POS = 8;
	private final int SQL_SELECTMEDIALIST_TRACK_ID_POS = 9;
	private final int SQL_SELECTMEDIALIST_TRACK_PERSISTENT_ID_POS = 10;
	private final String SQL_SELECTMEDIA = "SELECT " + 
										"t.track_id, " + 					//  1
										"t.track_name, " +					//  2
										"t.track_artist_id, "+				//  3
										"t.track_artist_alias_id, "+		//  4
										"t.track_audit_user, "+				//  5
										"t.track_audit_timestamp, "+"" +	//  6
										"t.track_add_timestamp, "+			//  7
										"t.track_length_ms, "+				//  8
										"t.track_persistent_id, " +			//  9
										"a.artist_name," +					// 10
										"ll.location_url," +				// 11
										"ll.mime_id," +						// 12
										"rl.location_url," +				// 13
										"rl.mime_id, " +					// 14
										"rl.location_size, " + 				// 15
										"ll.location_size," + 				// 16
										"t.track_album_num," + 				// 17
										"t.TRACK_ADD_TIMESTAMP," + 			// 18
										"t.TRACK_AUDIT_TIMESTAMP," + 		// 19
										"t.track_album," + 					// 20
										"ll.is_valid," +					// 21
										"rl.is_valid " +					// 22
										"FROM media_track t " +
										"inner join artist a on t.track_artist_id = a.artist_id " +
										"left join media_track_location ll on (t.track_id = ll.track_id and ll.location_type = '" + URL_LOCATION_TYPE_LOCAL + "') " +
										"left join media_track_location rl on (t.track_id = rl.track_id and rl.location_type = '" + URL_LOCATION_TYPE_REMOTE + "') ";
	private final int SQL_SELECT_TRACK_ID_POS = 1;
	private final int SQL_SELECT_TRACK_NAME_POS = 2;
	private final int SQL_SELECT_TRACK_ARTIST_ID_POS = 3;
	private final int SQL_SELECT_TRACK_ARTIST_NAME_POS = 10;
	private final int SQL_SELECT_TRACK_ARTIST_ALIAS_ID_POS = 4;
	private final int SQL_SELECT_TRACK_ARTIST_ALIAS_NAME_POS = 0;
	private final int SQL_SELECT_TRACK_ADD_TIMESTAMP_POS = 18;
	private final int SQL_SELECT_TRACK_AUDIT_TIMESTAMP_POS = 19;
	private final int SQL_SELECT_TRACK_LENGTH_MS_POS = 8;
	private final int SQL_SELECT_TRACK_PERSISTENT_ID_POS = 9;
	private final int SQL_SELECT_TRACK_URL_LOCAL_POS = 11;
	private final int SQL_SELECT_TRACK_SIZE_LOCAL_POS = 16;
	private final int SQL_SELECT_TRACK_SIZE_REMOTE_POS = 15;
	private final int SQL_SELECT_TRACK_MIME_TYPE_ID_LOCAL_POS = 12;
	private final int SQL_SELECT_TRACK_URL_REMOTE_POS = 13;
	private final int SQL_SELECT_TRACK_MIME_TYPE_ID_REMOTE_POS = 14;
	private final int SQL_SELECT_TRACK_ALBUM_TRACK_NO_POS = 17;
	private final int SQL_SELECT_TRACK_ALBUM = 20;
	private final int SQL_SELECT_TRACK_IS_VALID_LOCAL = 21;
	private final int SQL_SELECT_TRACK_IS_VALID_REMOTE = 22;
}