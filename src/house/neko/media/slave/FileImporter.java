package house.neko.media.slave;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.MimeType;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.ConfigurationManager;
import house.neko.media.common.MediaMetaDataReader;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JFileChooser;

import java.awt.event.ActionListener;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Container;
import java.awt.Component;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;

public class FileImporter implements ActionListener
{
	private MediaLibrary library;
	private Log log;
	private HierarchicalConfiguration config;
	private MimeType flacMimeType;
	private Component parent;
	
	public FileImporter(MediaLibrary library, HierarchicalConfiguration config, Component parent)
	{
		this.library = library;
		this.log = ConfigurationManager.getLog(getClass());
		this.config = config;
		this.parent = parent;
		flacMimeType = new MimeType();
		flacMimeType.setFileExtension("flac");
		flacMimeType.setMimeType("audio/flac");
	}
	
	public void actionPerformed(ActionEvent e)
	{	importFiles(); }
	
	public void importFiles()
	{
		//Create a file chooser
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setMultiSelectionEnabled(true);
		fc.setDialogTitle("Pick a file or directory to import");
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		String lastDir = config.getString("LastDir");
		if(lastDir != null && lastDir.length() > 0)
		{	fc.setCurrentDirectory(new File(lastDir)); }
		int r = fc.showDialog(parent, "Import");
		if(r == JFileChooser.APPROVE_OPTION)
		{	// try to import now
			File[] selected = explodeSelected(fc.getSelectedFiles());
			if(selected == null)
			{	return; }
			config.setProperty("LastDir", fc.getCurrentDirectory().getAbsolutePath());
			Media[] m = getMetaDataForFiles(selected);
		}
	}
	
	private Media[] getMetaDataForFiles(File[] selected)
	{
		java.util.Arrays.sort(selected);
		JPanel p = new JPanel();
		MediaMetaDataReader metaReader = new MediaMetaDataReader();
		Media[] m = new Media[selected.length];
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 100;
		gc.weighty = 100;
		p.setLayout(gridbag);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.ipadx = 7;
		int y = 0;
		JCheckBox[] checked = new JCheckBox[selected.length];
		JTextField[] title = new JTextField[selected.length];
		JTextField[] artist = new JTextField[selected.length];
		JTextField[] album = new JTextField[selected.length];
		JTextField[] track = new JTextField[selected.length];
		addLabel(p, new JLabel("Album"), gridbag, gc, 2, y);
		addLabel(p, new JLabel("Artist"), gridbag, gc, 4, y);
		addLabel(p, new JLabel("Title"), gridbag, gc, 6, y);
		addLabel(p, new JLabel("Track"), gridbag, gc, 8, y++);
		READLOOP:for(int k = 0; k < selected.length; k++)
		{
			log.info("Checking " + selected[k]);
			try
			{
				m[k] = metaReader.getMediaFromFile(selected[k]);
			} catch(IOException ioe) {
				log.error(ioe);
				continue READLOOP;
			}
			checked[k] = new JCheckBox("Import", true);
			title[k] = new JTextField(20);
			artist[k] = new JTextField(20);
			album[k] = new JTextField(20);
			track[k] = new JTextField(3);
			title[k].setText(m[k].getName());
			artist[k].setText(m[k].getArtist());
			album[k].setText(m[k].getAlbum());
			track[k].setText("" + m[k].getTrackNumber());
			
			gc.gridx = 0;
			gc.gridy = ++y;
			gc.gridheight = 1;
			gc.gridwidth = GridBagConstraints.REMAINDER;
			JLabel filelabel = new JLabel("File " + (k+1) + ": " + selected[k].getAbsolutePath());
			gridbag.setConstraints(filelabel, gc);
			p.add(filelabel);
			gc.gridwidth = 1;
			addField(p, checked[k], gridbag, gc, 0, ++y);
			addField(p, album[k], gridbag, gc, 2, y);
			addField(p, artist[k], gridbag, gc, 4, y);
			addField(p, title[k], gridbag, gc, 6, y);
			addField(p, track[k], gridbag, gc, 8, y);
		}
		if(JOptionPane.showConfirmDialog(null, p, "Pick Disc", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			for(int i = 0; i < m.length; i++)
			{
				if(m[i] != null && checked[i] != null && checked[i].isSelected())
				{	library.add(m[i]); }
			}
		} else {
			// pop all off
			log.info("Canceled picking, clearing list");
			//discs.clear();
		}
		return null;
	}
	
	private File[] explodeSelected(File[] in)
	{
		if(in == null || in.length < 1)
		{	return new File[0]; }
		Vector<File> v = new Vector<File>();
		for(File f : in)
		{
			if(f.isDirectory())
			{
				for(File lsf : explodeSelected(f.listFiles()))
				{	v.add(lsf); }
			} else {
				v.add(f);
			}
		}
		return v.toArray(new File[v.size()]);
	}
	
	private void addComponent(Container p, Component c, GridBagLayout gridbag, GridBagConstraints gc, int x, int y)
	{
		gc.gridx = x;
		gc.gridy = y;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gridbag.setConstraints(c, gc);
		p.add(c);
	}
	
	private void addLabel(Container p, JLabel label, GridBagLayout gridbag, GridBagConstraints gc, int x, int y)
	{
		//gc.gridwidth = GridBagConstraints.RELATIVE;
		addComponent(p, label, gridbag, gc, x, y);
	}
	
	private void addField(Container p, JTextField field, GridBagLayout gridbag, GridBagConstraints gc, int x, int y)
	{
		//gc.gridwidth = GridBagConstraints.RELATIVE;
		gc.fill = GridBagConstraints.HORIZONTAL;
		addComponent(p, field, gridbag, gc, x, y);
	}
	
	private void addField(Container p, JCheckBox box, GridBagLayout gridbag, GridBagConstraints gc, int x, int y)
	{
		//gc.gridwidth = GridBagConstraints.RELATIVE;
		gc.fill = GridBagConstraints.HORIZONTAL;
		addComponent(p, box, gridbag, gc, x, y);
	}
}