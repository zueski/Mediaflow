package house.neko.media.slave;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.MediaList;
import house.neko.media.common.MediaListEntry;
import house.neko.media.common.MimeType;
import house.neko.media.common.MediaLibrary;
import house.neko.media.common.ConfigurationManager;

import java.io.File;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Container;
import java.awt.Component;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.apache.commons.logging.Log;

public class SlaveCDImporter
{
	private MediaLibrary library;
	private Log log;
	private HierarchicalConfiguration config;
	private MimeType flacMimeType;
	
	private Vector<JTextField> discTitles = new Vector<JTextField>();
	private Vector<JTextField> discArtists = new Vector<JTextField>();
	private Vector<JTextField> discYears = new Vector<JTextField>();
	private Vector<String> discIDs = new Vector<String>();
	
	private Vector<Vector<JLabel>> trackNumbers = new Vector<Vector<JLabel>>();
	private Vector<Vector<JTextField>> trackTitles = new Vector<Vector<JTextField>>();
	private Vector<Vector<JTextField>> trackArtists = new Vector<Vector<JTextField>>();
	
	
	public SlaveCDImporter(MediaLibrary library)
	{
		this.library = library;
		this.log = ConfigurationManager.getLog(getClass());
		flacMimeType = MimeType.getInstanceFromType(MimeType.TYPE_FLAC);
	}

	public void importCD()
	{
		if(log.isDebugEnabled())
		{	log.debug("Starting CD import"); }
		try
		{
			if(log.isDebugEnabled()) { log.debug("getting CDDB info"); }
			Process cddbExec = Runtime.getRuntime().exec("cd-info --no-cddb-cache");
			java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(cddbExec.getInputStream()));
			READLOOP:
			{
				String discYear = "";
				String discArtist = "";
				String discTitle = "";
				String discID = "";
				String trackCount = "";
				String trackNumber = "";
				String trackLength = "";
				String frameOffset = "";
				String trackArtist = "";
				String trackTitle = "";
				String line = r.readLine();
				while(line != null)
				{
					line = line.trim();
					if(line.startsWith("Year:"))
					{
						discYear = line.substring(6);
						if(log.isTraceEnabled()) { log.trace("CD year: " + discYear); }
					} else if(line.startsWith("Artist:")) {
						discArtist = line.substring(8).replaceAll("^'(.*)'$", "$1");
						if(log.isTraceEnabled()) { log.trace("Artist: " + discArtist); }
					} else if(line.startsWith("Title:")) {
						discTitle = line.substring(7).replaceAll("^'(.*)'$", "$1");
						if(log.isTraceEnabled()) { log.trace("Disc title: " + discTitle); }
					} else if(line.startsWith("Number of tracks:")) {
						if(discTitles.size() > 0)
						{	// update last disc
							trackNumbers.lastElement().add(new JLabel(trackNumber));
							trackTitles.lastElement().add(new JTextField(trackTitle));
							trackArtists.lastElement().add(new JTextField(trackArtist));
						}
						// start of new disc
						trackCount = line.substring(18).replaceAll("^'(.*)'$", "$1");
						discTitles.add(new JTextField(discTitle));
						discArtists.add(new JTextField(discArtist));
						discYears.add(new JTextField(discYear));
						discIDs.add(discID);
						trackNumbers.add(new Vector<JLabel>());
						trackTitles.add(new Vector<JTextField>());
						trackArtists.add(new Vector<JTextField>());
						if(log.isTraceEnabled()) { log.trace("Track count: " + trackCount); }
					} else if(line.startsWith("number:")) {
						trackNumber = line.substring(8);
						if(log.isTraceEnabled()) { log.trace("Track number: " + trackNumber); }
					} else if(line.startsWith("frame offset:")) {
						frameOffset = line.substring(14);
						if(log.isTraceEnabled()) { log.trace("Frame offset: " + frameOffset); }
					} else if(line.startsWith("length:")) {
						trackLength = line.substring(8).replaceAll("^(.*) seconds$", "$1");
						if(log.isTraceEnabled()) { log.trace("length: " + trackLength); }
					} else if(line.startsWith("artist:")) {
						trackArtist = line.substring(8).replaceAll("^'(.*)'$", "$1");
						if(log.isTraceEnabled()) { log.trace("Track artist: " + trackArtist); }
					} else if(line.startsWith("title:")) {
						trackTitle = line.substring(7).replaceAll("^'(.*)'$", "$1");
						if(log.isTraceEnabled()) { log.trace("Track title: " + trackTitle); }
					} else if(line.matches("^Track +\\d+$") && trackTitle.length() > 0) {
						// found next, add current track 
						trackNumbers.lastElement().add(new JLabel(trackNumber));
						trackTitles.lastElement().add(new JTextField(trackTitle));
						trackArtists.lastElement().add(new JTextField(trackArtist));
					} else if(line.startsWith("Disc ID: ") && trackTitle.length() > 0) {
						discID = line.substring(9);
					}
					line = r.readLine();
				}
				if(trackTitle.length() > 0)
				{
					trackNumbers.lastElement().add(new JLabel(trackNumber));
					trackTitles.lastElement().add(new JTextField(trackTitle));
					trackArtists.lastElement().add(new JTextField(trackArtist));
				}
				if(log.isInfoEnabled()) { log.info("Found " + discTitles.size() + " possible matches"); }
				MediaList ml = pickDisc();
				if(ml != null)
				{
					if(log.isInfoEnabled()) { log.info("importing disc with " + ml.getTrackCount() + " tracks"); }
					MediaListEntry[] mlislt = ml.getTrackList();
					for(MediaListEntry mt : mlislt)
					{
						Media m = mt.media;
						if(log.isInfoEnabled()) { log.info("importing track " + m.getTrackNumber() + ": " + m); }
						MediaLocation l = new MediaLocation();
						l.setMimeType(flacMimeType);
						m.setLocalLocation(l);
						File encodedFile = library.getDefaultMediaFile(m);
						l.setLocationURLString(encodedFile.toURI().toURL().toString());
						if(encodedFile.exists())
						{
							Media fm = library.getMediaByFile(encodedFile);
							if(fm != null)
							{
								if(log.isInfoEnabled()) { log.info("media already exists, skipping import, " + encodedFile.getAbsolutePath()); }
								continue;
							}
							// else add
							l.setSize(encodedFile.length());
							m.setID(Media.generateID());
							if(log.isDebugEnabled()) { log.debug("adding existing track " + m.getTrackNumber() + ": " + m); }
							library.add(m);
							continue;
						}
						File tmpFile = File.createTempFile("tmpMediaFlow_", "_" + m.getTrackNumber() + ".wav");
						if(log.isDebugEnabled()) { log.debug("ripping track to " + tmpFile.getAbsolutePath()); }
						String[] cmdRip = { "cdparanoia", "-q", Integer.toString(m.getTrackNumber()), tmpFile.getAbsolutePath() };
						Process execRip = Runtime.getRuntime().exec(cmdRip);
						if(execRip.waitFor() == 0)
						{	// encode
							if(log.isDebugEnabled()) { log.debug("encoding track to " + encodedFile.getAbsolutePath()); }
							if(!encodedFile.getParentFile().mkdirs())
							{	if(log.isDebugEnabled()) { log.debug("unable to create directory  " + encodedFile.getParentFile().getAbsolutePath()); } }
							String[] cmdEncode = 
								{
									"flac", 
									"--totally-silent",
									"-8",
									"--tag=ALBUM=" + m.getAlbum(),
									"--tag=TITLE=" + m.getName(),
									"--tag=ARTIST=" + m.getArtist(),
									"--tag=TRACKNUMBER=" + m.getTrackNumber(),
									"-o", encodedFile.getAbsolutePath(),
									tmpFile.getAbsolutePath() 
								};
							Process execEncode = Runtime.getRuntime().exec(cmdEncode);
							if(execEncode.waitFor() == 0)
							{	// add to library
								l.setSize(encodedFile.length());
								m.setID(Media.generateID());
								if(log.isDebugEnabled()) { log.debug("adding track " + m.getTrackNumber() + ": " + m); }
								library.add(m);
							}
						}
						tmpFile.delete();
					}
					// add list
					library.add(ml);
				}
			}
			if(cddbExec.waitFor() != 0)
			{	log.error("Unable to query CDDB"); }
			
		} catch(Exception e) {
			log.error("unable to finish CD import", e);
		}
		if(log.isDebugEnabled())
		{	log.debug("Done with CD import"); }
	}
	
	private MediaList pickDisc()
	{
		MediaList ml = null;
		try
		{
			JTabbedPane tp = new JTabbedPane();
			for(int k = 0; k < discTitles.size(); k++)
			{
				JPanel p = new JPanel();
				GridBagLayout gridbag = new GridBagLayout();
				GridBagConstraints gc = new GridBagConstraints();
				gc.weightx = 100;
				gc.weighty = 100;
				p.setLayout(gridbag);
				gc.anchor = GridBagConstraints.NORTHWEST;
				gc.ipadx = 7;
				int y = 0;
				addLabel(p, new JLabel("Album Name"), gridbag, gc, 0, y);
				addField(p, discTitles.elementAt(k), gridbag, gc, 4, y);
				addLabel(p, new JLabel("Album Artist"), gridbag, gc, 0, ++y);
				addField(p, discArtists.elementAt(k), gridbag, gc, 4, y);
				addLabel(p, new JLabel("Year Released"), gridbag, gc, 0, ++y);
				addField(p, discYears.elementAt(k), gridbag, gc, 2, y);
				addLabel(p, new JLabel("#"), gridbag, gc, 0, ++y);
				addLabel(p, new JLabel("Artist"), gridbag, gc, 2, y);
				addLabel(p, new JLabel("Title"), gridbag, gc, 4, y);
				for(int i = 0; i < trackNumbers.elementAt(k).size(); i++)
				{
					addLabel(p, trackNumbers.elementAt(k).elementAt(i), gridbag, gc, 0, ++y);
					addField(p, trackArtists.elementAt(k).elementAt(i), gridbag, gc, 2, y);
					addField(p, trackTitles.elementAt(k).elementAt(i), gridbag, gc, 4, y);
				}
				tp.addTab(discArtists.elementAt(k).getText() + " - " + discTitles.elementAt(k).getText(), p);
			}
			if(JOptionPane.showConfirmDialog(null, tp, "Pick Disc", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			{	// pop all off except choice
				int j = tp.getSelectedIndex();
				log.info("Picked disc #" + j);
				ml = new MediaList();
				ml.setID(discIDs.elementAt(j));
				ml.setName(discTitles.elementAt(j).getText());
				ml.setArtist(discArtists.elementAt(j).getText());
				//try { ml.setPublishedDate(new Integer.parseInt(discYears.elementAt(j).trim())); } catch(Exception e) { }
				MediaListEntry[] tracks = new MediaListEntry[trackNumbers.elementAt(j).size()];
				for(int i = 0; i < tracks.length; i++)
				{
					Media m = new Media();
					m.setName(trackTitles.elementAt(j).elementAt(i).getText());
					m.setArtist(trackArtists.elementAt(j).elementAt(i).getText());
					m.setAlbum(discTitles.elementAt(j).getText());
					try { m.setTrackNumber(Integer.parseInt(trackNumbers.elementAt(j).elementAt(i).getText())); } catch(NumberFormatException nfe) { }
					//m.getPublishedDateMS(ml.getPublishedDate());
					tracks[i] = new MediaListEntry(m, i+1);
				}
				ml.setTrackList(tracks);
			} else {
				// pop all off
				log.info("Canceled picking");
			}
		} catch(Exception e) {
			log.error("unable to pick titles for CD import", e);
		}
		return ml;
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
}