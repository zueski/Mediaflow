package house.neko.media.common.datastore;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.DatabaseDataStore;
import house.neko.media.common.ConfigurationManager;

import java.io.File;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.DatabaseMetaData;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;

public class FileReorgAction extends AbstractAction implements Runnable
{
	private HierarchicalConfiguration config = null;
	final DatabaseDataStore store;
	final static private String NAME = "Reorganize Library";
	private Log log = null;
	
	public FileReorgAction(DatabaseDataStore store, HierarchicalConfiguration config)
	{
		super(NAME);
		this.log = ConfigurationManager.getLog(getClass());
		this.config = config;
		putValue(super.LONG_DESCRIPTION, NAME);
		putValue(super.SHORT_DESCRIPTION, NAME);
		this.store = store;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(log.isDebugEnabled()) { log.trace("queuing Library Reorg"); }
		store.submitTask(this);
	}
	/*
	This function will do the following actions:
		1.  create a new top level folder
		2.  move each file to this new folder
		3.  rename the old folder to the new
		4.  remove empty dirs from old folder (including old folder)
	*/
	public void run()
	{
		if(log.isDebugEnabled()) { log.debug("Reorg - Started"); }
		Connection c = null;
		REORG:try
		{
			java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("_yyyyMMdd_HHmmss");
			File oldBase = new File(config.getString("MusicBasePath"));
			File newBase = new File(oldBase.getParentFile(), oldBase.getName() + dateFormat.format(new java.util.Date()));
			if(log.isDebugEnabled()) { log.debug("Reorg - moving " + oldBase.getAbsolutePath() + " to " + newBase.getAbsolutePath()); }
			if(!newBase.mkdirs())
			{
				log.error("Unable to create new dir " + newBase.getAbsolutePath());
				break REORG;
			}
			for(Media m : store.getAllMedia())
			{
				MediaLocation l = m.getLocation();
				if(l == null)
				{	continue; }
				File of = l.getFile();
				if(of == null)
				{	continue; }
				File nf = store.getDefaultMediaFile(newBase, m);
				File nfol = store.getDefaultMediaFile(m);
				if(log.isDebugEnabled()) { log.debug("Reorg - would move " + of.getAbsolutePath() + " to " + nf.getAbsolutePath() + "(" + nfol.getAbsolutePath() + ")"); }
			}
		} catch(Exception e) {
			log.error("Reorg - Unable to complete", e);
		} finally {
			
		}
		if(log.isDebugEnabled()) { log.debug("Reorg - Complete"); }
	}
}