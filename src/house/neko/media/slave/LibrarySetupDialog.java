package house.neko.media.slave;

import house.neko.media.common.ConfigurationManager;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.DatabaseDataStore;
import house.neko.media.common.DataStore.DataStoreConfigurationHelper;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;

import org.apache.commons.configuration.HierarchicalConfiguration;

public class LibrarySetupDialog extends JDialog implements ActionListener
{

	private static final long serialVersionUID = 5L;
	private Log log;
	private HierarchicalConfiguration config;
	private MediaLibrary library;
	
	private JTabbedPane tabs;
	private DataStoreConfigurationHelper[] helper;

	public LibrarySetupDialog(JFrame parent, boolean isModal)
	{
		super(parent, "MediaFlow Library Setup", isModal);
		ConfigurationManager.init(new String[0]);
		this.log = ConfigurationManager.getLog(getClass());
		if(log.isTraceEnabled())
		{	log.trace("Initializing"); }
		/*
		helper = new DataStoreConfigurationHelper[1];
		helper[0] = (new DatabaseDataStore()).getConfigurationHelper();
		for(String key : helper.getConfigurationKeys())
		{
			
		}
		*/
		setMinimumSize(new Dimension(400, 400));
		setSize(new Dimension(600, 500));
	}
	
	public void setLibrary(MediaLibrary library)
	{	this.library = library; }

	public void actionPerformed(ActionEvent e)
	{
		if(log.isDebugEnabled())
		{	log.debug("Action " + e.getActionCommand()); }
		notifyAll();
	}

}