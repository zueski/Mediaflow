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

public class FileCheckAction extends AbstractAction implements Runnable
{
	private HierarchicalConfiguration config = null;
	final DatabaseDataStore store;
	final static private String NAME = "Check files in library";
	private Log log = null;
	
	public FileCheckAction(DatabaseDataStore store, HierarchicalConfiguration config)
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
		if(log.isDebugEnabled()) { log.trace("queuing Library File Check"); }
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
		if(log.isDebugEnabled()) { log.debug("FileCheck  - Started"); }
		Connection c = null;
		FILECHECK:try
		{
			for(Media m : store.getAllMedia())
			{
				MediaLocation l = m.getLocation();
				if(l == null)
				{	continue; }
				File f = l.getFile();
				if(f == null)
				{	continue; }
				m.setLocationValid(l, f.exists());
				if(log.isDebugEnabled()) { log.debug("FileCheck - " + l.isLocationValid() + "<-" + f.getAbsolutePath()); }
			}
		} catch(Exception e) {
			log.error("FileCheck - Unable to complete", e);
		} finally {
			
		}
		if(log.isDebugEnabled()) { log.debug("FileCheck  - Complete"); }
	}
}