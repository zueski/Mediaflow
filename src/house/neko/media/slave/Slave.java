package house.neko.media.slave;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.MediaConverter;
import house.neko.media.common.MediaPlayer;
import house.neko.media.common.ConfigurationManager;

import house.neko.media.itunes.ITunesMediaLibraryXMLFile;

import house.neko.media.device.Exporter;

import javax.swing.Action;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.BorderLayout;
import java.awt.Container;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;

import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 */
public class Slave extends JFrame implements ActionListener, WindowListener
{
	private static final long serialVersionUID = 5L;
	
	private int CONTROLCOMMAND_MASK = InputEvent.CTRL_MASK;
	private int ALTOPTIONDOWN_MASK = InputEvent.ALT_DOWN_MASK;

	private Log log;
	private HierarchicalConfiguration config;
	private MediaLibrary library;
	private LibraryViewPane view;
	private LibrarySearchPane search;
	private MediaPlayer player;
	private MediaPlayerControls controls;
	private EventMapper eventMapper;
	
	private JMenuBar menubar;
	private JMenu fileMenu;
	private JMenu libraryMenu;
	private JMenu trackMenu;
	private JMenu viewMenu;
	private JMenu deviceMenu;

	private boolean isMac = false;
	
	/**
	 *
	 */
	public Slave()
	{
		super("Media Master Slave");
		ConfigurationManager.init(new String[0]);
		this.log = ConfigurationManager.getLog(getClass());
		if(log.isTraceEnabled())
		{	log.trace("Initializing"); }
		
		//figure out if we are on OS X
		isMac = System.getProperty("mrj.version") != null;
		if(isMac)
		{
			if(log.isTraceEnabled())
			{	log.trace("This is OS X! found version " + System.getProperty("mrj.version")); }
			
			CONTROLCOMMAND_MASK = InputEvent.META_MASK;
			//ALTOPTIONDOWN_MASK = InputEvent.
			//try { javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); } catch(Exception e) { log.error("Unable to reset UIManager", e); }
		}
		
		// setup the logic
		config = ConfigurationManager.getConfiguration("Slave(0)");
		library = new MediaLibrary(ConfigurationManager.getConfiguration("Slave(0).MediaLibrary(0)"));
		
		if(!library.isSetup())
		{
			LibrarySetupDialog dialog = new LibrarySetupDialog(this, true);
			dialog.setLibrary(library);
			dialog.setVisible(true);
			try { dialog.wait(); } catch(InterruptedException ie) { }
		}
		
		house.neko.media.common.LibraryView libraryview = library.getNewView();
		view = new LibraryViewPane(this, ConfigurationManager.getConfiguration("Slave(0).LibraryView(0)"), libraryview);
		search = new LibrarySearchPane(ConfigurationManager.getConfiguration("Slave(0).LibrarySearch(0)"), libraryview);
		player = new MediaPlayer();
		controls = new MediaPlayerControls(player, eventMapper);
		eventMapper = new EventMapper(player);

		//setup graphics
		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		c.add(search, BorderLayout.NORTH);
		
		c.add(view, BorderLayout.CENTER);

		c.add(controls, BorderLayout.SOUTH);

		setSize(config.getInt("Window.Width", 400), config.getInt("Window.Height", 600));
		setLocation(config.getInt("Window.PositionX", 50), config.getInt("Window.PositionY", 50));
		
		//setup window decorations
		setIconImage((new ImageIcon(Slave.class.getResource("/house/neko/mediamaster/slave/windowIcon.png"))).getImage());
		createMenus();
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		
		log.trace("Done initializing");
		setVisible(true);
	}
	
	private void createMenus()
	{
		menubar = new JMenuBar();
		
		fileMenu = new JMenu("File");
		
		FileImporter fi = new FileImporter(library, ConfigurationManager.getConfiguration("Slave(0).Import(0)"), this);
		JMenuItem importFiles = new JMenuItem("Import from Files", 'O');
		importFiles.setMnemonic('I');
		importFiles.setAccelerator(KeyStroke.getKeyStroke('O', CONTROLCOMMAND_MASK));
		importFiles.addActionListener(fi);
		fileMenu.add(importFiles);
		
		JMenuItem importCD = new JMenuItem("Import from CD", 'D');
		importCD.setMnemonic('I');
		importCD.setAccelerator(KeyStroke.getKeyStroke('D', CONTROLCOMMAND_MASK | InputEvent.SHIFT_DOWN_MASK));
		importCD.addActionListener(this);
		fileMenu.add(importCD);
		
		JMenuItem importITunesXML = new JMenuItem("Import iTunes library", 'I');
		importITunesXML.setMnemonic('I');
		importITunesXML.setAccelerator(KeyStroke.getKeyStroke('I', CONTROLCOMMAND_MASK | InputEvent.SHIFT_DOWN_MASK));
		importITunesXML.addActionListener(this);
		fileMenu.add(importITunesXML);
		
		JMenuItem saveLibrary = new JMenuItem("Save Library", 'S');
		saveLibrary.setMnemonic('S');
		saveLibrary.setAccelerator(KeyStroke.getKeyStroke('S', CONTROLCOMMAND_MASK));
		saveLibrary.addActionListener(this);
		fileMenu.add(saveLibrary);
		
		fileMenu.addSeparator();
		JMenuItem dump = new JMenuItem("Dump library", 'D');
		dump.setAccelerator(KeyStroke.getKeyStroke('D', CONTROLCOMMAND_MASK));
		dump.setMnemonic('D');
		dump.addActionListener(this);
		fileMenu.add(dump);
		
		if(!isMac)
		{
			fileMenu.addSeparator();
			JMenuItem quit = new JMenuItem("Quit",'Q');
			quit.setMnemonic('Q');
			quit.setAccelerator(KeyStroke.getKeyStroke('Q', CONTROLCOMMAND_MASK));
			quit.addActionListener(this);
			fileMenu.add(quit);
		} else {
			//MacApplicationHandler mac = new MacApplicationHandler(this);
		}
		
		menubar.add(fileMenu);
		
		viewMenu = new JMenu("View");
		for(Action a : view.getActions())
		{	viewMenu.add(new JCheckBoxMenuItem(a)); }
		
		menubar.add(viewMenu);
		
		libraryMenu = new JMenu("Library");
		for(Action a : library.getActions())
		{	libraryMenu.add(new JMenuItem(a)); }
		menubar.add(libraryMenu);
		
		trackMenu = new JMenu("Track");
		JMenuItem getTrackInfo = new JMenuItem("Get track info", 'I');
		getTrackInfo.setMnemonic('I');
		getTrackInfo.setAccelerator(KeyStroke.getKeyStroke('I', CONTROLCOMMAND_MASK));
		getTrackInfo.addActionListener(this);
		trackMenu.add(getTrackInfo);
		
		JMenuItem convertALACToFlac = new JMenuItem("Convert ALAC track to FLAC", 'L');
		convertALACToFlac.setMnemonic('R');
		convertALACToFlac.addActionListener(this);
		trackMenu.add(convertALACToFlac);
		
		menubar.add(trackMenu);
		
		List<HierarchicalConfiguration> devices = ConfigurationManager.configurationsAt("Slave.Device");
		if(devices != null)
		{
			deviceMenu = new JMenu("Device");
			for(int i = 0; i < devices.size(); i++)
			{
				if(i > 0)
				{	deviceMenu.	addSeparator(); }
				Exporter exporter = new Exporter(library, devices.get(i));
				for(Action a : exporter.getActions(view))
				{	deviceMenu.add(new JMenuItem(a)); }
			}
			menubar.add(deviceMenu);
		}
		
		setJMenuBar(menubar);
	}

	/**
	 *
	 * @param is
	 */
	/*public void play(InputStream is)
	{
		try
		{
			Thread.currentThread().setName("MediaMasterSlave");
			player.stop();
			//if(log.isTraceEnabled())
			//{	log.trace("Setting URL to " + url); }
			player.0(is);
			log.trace("Player should be playing about now");
		} catch(Exception e) {
			log.error(e);
		}
	}*/
	public void play(Media m)
	{
		try
		{
			Thread.currentThread().setName("MediaMasterSlave");
			//player.stop();
			//if(log.isTraceEnabled())
			//{	log.trace("Setting URL to " + url); }
			player.setMediaLocation(m.getLocation());
			log.trace("Player should be playing about now");
		} catch(Exception e) {
			log.error(e);
		}
	}

	/**
	 *
	 * @param e
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(log.isDebugEnabled())
		{	log.debug("Action " + e.getActionCommand()); }
		switch(e.getActionCommand())
		{
			case "Import iTunes library":
				JFileChooser fc = new JFileChooser();
				String lastFile = config.getString("iTunes.lastImported");
				if(lastFile != null && lastFile.length() > 3)
				{	try { fc.setSelectedFile(new File(lastFile)); } finally { } }
				int returnVal = fc.showOpenDialog(this);
				if (returnVal == JFileChooser.APPROVE_OPTION) 
				{
					File file = fc.getSelectedFile();
					try
					{
						//This is where a real application would open the file.
						log.trace("importing iTunes libary: " + file.getAbsolutePath());
						ITunesMediaLibraryXMLFile importer = new ITunesMediaLibraryXMLFile(library);
						importer.parseInputStream(new FileInputStream(file));
						config.setProperty("iTunes.lastImported", file.getAbsolutePath());
					} catch(Exception ex) {
						log.error("Unable to import file " + file.getAbsolutePath(), ex); 
					}
				} else {
					log.trace("Open command cancelled by user.");
				}
				break;
			case "Import from CD":
				SlaveCDImporter scdi = new SlaveCDImporter(library);
				scdi.importCD();
				break;
			case "Save Library":
				library.saveAllDirty();
				break;
			case "Dump library":
				log.fatal("Dumping library to stdout!");
				library.dump();
				break;
			case "Convert ALAC track to FLAC":
				log.trace("Convert ALAC track to FLAC");
				MediaConverter converter = new MediaConverter(library);
				converter.convertToFLAC(library.getAllMedia());
				break;
			case "Get track info":
				log.trace("Getting track info for selected track");
				view.openInfoForSelected();
				break;
			case "Quit":
				if(log.isDebugEnabled())
				{	log.debug("Quitting from menu, saving settings and library"); }
				if(library.isDirty())
				{
					int option = JOptionPane.showConfirmDialog(null, "Library has unsaved changes; should they be saved?");
					switch(option)
					{
						case JOptionPane.OK_OPTION: library.saveAllDirty(); break;
						case JOptionPane.CANCEL_OPTION: return;
					}
				}
				shutdown();
		}
	}
	
	/*
	 * Class handles MRJ's special hooks to make the app more mac like
	 *
	public static class MacApplicationHandler extends com.apple.eawt.Application implements com.apple.eawt.ApplicationListener
	{
		private Slave slave;
		public MacApplicationHandler(Slave slave) 
		{
			this.slave = slave;
			addApplicationListener(this);
		}
		public void handleAbout(com.apple.eawt.ApplicationEvent event) { }
		public void handleOpenApplication(com.apple.eawt.ApplicationEvent event) { }
		public void handleOpenFile(com.apple.eawt.ApplicationEvent event) { }
		public void handlePreferences(com.apple.eawt.ApplicationEvent event) { }
		public void handlePrintFile(com.apple.eawt.ApplicationEvent event) { }
		public void handleReOpenApplication(com.apple.eawt.ApplicationEvent event) { }
		public void handleQuit(com.apple.eawt.ApplicationEvent event)
		{
			if(slave.log.isDebugEnabled())
			{	slave.log.debug("Quitting from MacApplicationHandler"); }
			slave.shutdown(); 
		}
	}*/

	
	// WindowListener
	public void windowOpened(WindowEvent e) { }
	public void windowClosing(WindowEvent e)
	{
		if(log.isDebugEnabled())
		{	log.debug("Window closed, saving settings and library"); }
		shutdown();
	}
	public void windowIconified(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowActivated(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }
	
	public void windowClosed(WindowEvent e) { }
	// end WindowListener
	
	public void refreshView()
	{	view.refreshView(); }
	
	public void shutdown()
	{
		setVisible(false); 
		saveSettings();
		library.shutdown();
		System.exit(0);
	}
	
	public void saveSettings()
	{
		config.setProperty("Window.Width", getWidth());
		config.setProperty("Window.Height", getHeight());
		config.setProperty("Window.PositionX", getX());
		config.setProperty("Window.PositionY", getY());
		ConfigurationManager.save();
	}
}