package house.neko.media.slave;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;
import house.neko.media.common.ConfigurationManager;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Container;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import javax.swing.JLabel;


import org.apache.commons.logging.Log;

public class MediaTracksInfoDialog extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 60L;
	
	private static final String UPDATE_ACTION = "U";
	private static final String CANCEL_ACTION = "C";

	private Slave slave;
	private Media[] media;
	
	private Log log = null;
	
	private JTextField[] nameField;
	private JTextField[] authorField;
	private JTextField[] artistField;
	private JTextField[] artistAliasField;
	private JTextField[] albumField;
	private JTextField[] trackField;
	private JButton cancelButton;
	private JButton okayButton;
	
	public MediaTracksInfoDialog(Slave slave, Media[] media)
	{
		super("Media Tracks");
		this.log = ConfigurationManager.getLog(getClass());
		this.slave = slave;
		this.media = media;
		setMinimumSize(new Dimension(150, 100));
		//setSize(new Dimension(650, 300));

		JPanel c = new JPanel();
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		c.setLayout(gridbag);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.ipadx = 7;
		
		int y = 0;
		int row = 0;
		
		nameField = new JTextField[media.length];
		authorField = new JTextField[media.length];
		artistField = new JTextField[media.length];
		artistAliasField = new JTextField[media.length];
		albumField = new JTextField[media.length];
		nameField = new JTextField[media.length];
		trackField = new JTextField[media.length];
		
		addLabel(c, new JLabel("Artist"), gridbag, gc, 0, row);
		addLabel(c, new JLabel("Artist Alias"), gridbag, gc, 1, row);
		addLabel(c, new JLabel("Title"), gridbag, gc, 2, row);
		addLabel(c, new JLabel("Album"), gridbag, gc, 3, row);
		addLabel(c, new JLabel("Track"), gridbag, gc, 4, row);
		addLabel(c, new JLabel("Author"), gridbag, gc, 5, row);
		
		for(int i = 0; i < media.length; i++)
		{
			row++;
			if(log.isTraceEnabled())
			{	log.trace("Adding (" + row + ") to dialog: " + media[i]); }
			
			artistField[i] = new JTextField(media[i].getArtist());
			addField(c, artistField[i], gridbag, gc, 0, row);
			
			artistAliasField[i] = new JTextField(media[i].getArtistAlias());
			addField(c, artistAliasField[i], gridbag, gc, 1, row);
			
			nameField[i] = new JTextField(media[i].getName());
			addField(c, nameField[i], gridbag, gc, 2, row);
			
			albumField[i] = new JTextField(media[i].getAlbum());
			addField(c, albumField[i], gridbag, gc, 3, row);

			Integer track = media[i].getTrackNumber();
			trackField[i] = new JTextField(track != null ? track.toString() : "");
			addField(c, trackField[i], gridbag, gc, 4, row);
			
			authorField[i] = new JTextField(media[i].getAuthor());
			addField(c, authorField[i], gridbag, gc, 5, row);
		}
		
		okayButton = new JButton("  OK  ");
		okayButton.setActionCommand(UPDATE_ACTION);
		okayButton.addActionListener(this);
		addComponent(c, okayButton, gridbag, gc, 2, ++row);
		
		cancelButton= new JButton("  Cancel  ");
		cancelButton.setActionCommand(CANCEL_ACTION);
		cancelButton.addActionListener(this);
		addComponent(c, cancelButton, gridbag, gc, 3, row);
		
		
		getContentPane().add(new JScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
		pack();
	}
	
	private void addComponent(Container p, Component c, GridBagLayout gridbag, GridBagConstraints gc, int x, int y)
	{
		gc.gridx = x;
		gc.gridy = y;
		gridbag.setConstraints(c, gc);
		p.add(c);
	}
	
	private void addLabel(Container p, JLabel label, GridBagLayout gridbag, GridBagConstraints gc, int x, int y)
	{
		gc.gridx = x;
		gc.gridy = y;
		addComponent(p, label, gridbag, gc, x, y);
	}
	
	private void addField(Container p, JTextField field, GridBagLayout gridbag, GridBagConstraints gc, int x, int y)
	{
		gc.fill = GridBagConstraints.HORIZONTAL;
		addComponent(p, field, gridbag, gc, x, y);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(log.isTraceEnabled())
		{	log.trace("Action " + e.getActionCommand()); }
		if(UPDATE_ACTION.equals(e.getActionCommand()))
		{
			for(int i = 0; i < media.length; i++)
			{
				media[i].setName(nameField[i].getText());
				media[i].setAuthor(authorField[i].getText());
				media[i].setArtist(artistField[i].getText());
				media[i].setArtistAlias(artistAliasField[i].getText());
				media[i].setAlbum(albumField[i].getText());
				try
				{
					media[i].setTrackNumber(Integer.parseInt(trackField[i].getText()));
				} catch(NumberFormatException nfe) { }
			}
			setVisible(false);
			slave.refreshView();
		} else if(CANCEL_ACTION.equals(e.getActionCommand())) {
			setVisible(false);
		}
	}
}