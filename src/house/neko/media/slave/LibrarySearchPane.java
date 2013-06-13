package house.neko.media.slave;

import house.neko.util.TableSorter;
import house.neko.media.common.Media;
import house.neko.media.common.LibrarySearchResult;
import house.neko.media.common.LibraryView;
import house.neko.media.common.ConfigurationManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Container;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.AbstractAction;

import org.apache.commons.logging.Log;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 */
public class LibrarySearchPane extends JComponent implements KeyListener
{
	private static final long serialVersionUID = 6L;

	private house.neko.media.common.LibraryView libraryView;
	
	private Log log = null;
	private HierarchicalConfiguration config;
	
	private JTextField searchBox;
	
	public LibrarySearchPane(HierarchicalConfiguration config, house.neko.media.common.LibraryView libraryView)
	{
		super();
		this.config = config;
		this.libraryView = libraryView;
		
		this.log = ConfigurationManager.getLog(getClass());
		
		searchBox = new JTextField();
		searchBox.addKeyListener(this);
		
		setLayout(new BorderLayout());
		add(searchBox, BorderLayout.CENTER);
	}
	
	public void keyTyped(KeyEvent e) { }
	public void keyReleased(KeyEvent e) { }
	public void keyPressed(KeyEvent e)
	{
		if(e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			if(log.isDebugEnabled()) log.debug("applying search filter '" + searchBox.getText() + "'");
			libraryView.applySimpleFilter(searchBox.getText());
		}
	}
}