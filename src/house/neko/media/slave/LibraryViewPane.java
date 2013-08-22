package house.neko.media.slave;

import house.neko.util.TableSorter;
import house.neko.media.common.Media;
import house.neko.media.common.LibrarySearchResult;
import house.neko.media.common.LibraryView;
import house.neko.media.common.ConfigurationManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableColumnModel;

import javax.swing.event.TableModelListener;


import org.apache.commons.logging.Log;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 */
public class LibraryViewPane extends JScrollPane implements TableModel, ListSelectionListener, java.util.Observer, ActionListener
{
	private static final long serialVersionUID = 6L;

	private Slave slave;
	private house.neko.media.common.LibraryView libraryView;
	
	private Log log = null;
	private HierarchicalConfiguration config;

	private LibrarySearchResult result;
	private TableModelListener listeners[];
	private boolean listnerLock = true;

	private JTable table;
	private String[] columnHeaders = { };
	//private Class[] columnClasses = { };
	private TableColumn[] columns = { };
	private TableSorter tableSorter;
	private DefaultTableColumnModel colModel;

	/**
	 *
	 * @param mms
	 */
	public LibraryViewPane(Slave mms, HierarchicalConfiguration config, house.neko.media.common.LibraryView libraryView)
	{
		super();
		slave = mms;
		this.config = config;
		this.libraryView = libraryView;
		
		this.log = ConfigurationManager.getLog(getClass());
		
		setViewportView(table);

		
		
		colModel = new DefaultTableColumnModel();
		
		columnHeaders = libraryView.getColumnHeaders();
		//columnClasses = new Class[columnHeaders.length];
		columns = new TableColumn[columnHeaders.length];
		for(int i = 0; i < columns.length; i++)
		{
			//columnClasses[i] = columnHeaders[i].getClass();
			columns[i] = new TableColumn(i);
			columns[i].setIdentifier(columnHeaders[i]);
			columns[i].setHeaderValue(columnHeaders[i]);
			//table.addColumn(columns[i]);
			colModel.addColumn(columns[i]);
		}
		
		result = libraryView.getVisibleMedia();
		libraryView.addObserver(this);
		
		tableSorter = new TableSorter(this);  // sets model
		table = new JTable(tableSorter, colModel);
		//table.setModel(tableSorter);
		//table.setColumnModel(colModel);
		
		listeners = new TableModelListener[0];
		
		tableSorter.setTableHeader(table.getTableHeader());
		
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(this);
		
        /*String javaVersion = System.getProperty("java.version");
		if(javaVersion != null && javaVersion.matches("^1\\.6.*$"))
		{	// this is 1.6 only 
			table.setAutoCreateRowSorter(true);
		}*/
		
		
		// assign mouse listener
		addMouseListener(new LibraryMouseListener(this));
		
		super.setViewportView(table);
		
		if(log.isTraceEnabled())
        {	log.trace("Library view created"); }
	}

	// house.neko.media.common.LibraryView
	/**
	 *
	 * @param r
	 */
	synchronized public void setResult(LibrarySearchResult r)
	{
		if(log.isTraceEnabled())
		{	log.trace("Setting LibrarySearchResults: " + r); }
		ListSelectionModel lsm = table.getSelectionModel();
		lsm.clearSelection();
		result = r;
		table.tableChanged(new TableModelEvent(this, 0, result.results.length, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		if(log.isTraceEnabled())
		{	log.trace("Updating results to " + r.results.length); }
	}
	
	public LibraryView getView()
	{	return libraryView; }

	// TableModel
	/**
	 *
	 * @param l
	 */
	synchronized public void addTableModelListener(TableModelListener l)
	{
		if(log.isTraceEnabled())
		{	log.trace("Adding TableModelListener"); }
		if(listeners == null || listeners.length < 1)
		{
			listeners = new TableModelListener[1];
			listeners[0] = l;
		} else {
			TableModelListener[] t = new TableModelListener[listeners.length + 1];
			System.arraycopy(listeners, 0, t, 0, listeners.length);
			t[listeners.length] = l;
			listeners = t;
		}
	}

	// TableModel
	/**
	 *
	 * @param columnIndex
	 * @return
	 */
	public Class getColumnClass(int columnIndex)
	{
		return colModel.getColumn(columnIndex).getHeaderValue().getClass();
		//return columnClasses[columnIndex]; 
	}

	// TableModel
	/**
	 *
	 * @return
	 */
	public int getColumnCount()
	{	return table.getColumnCount(); }

	// TableModel
	/**
	 *
	 * @param columnIndex
	 * @return
	 */
	public String getColumnName(int columnIndex)
	{
		//try { Thread.sleep(900000L); } catch(Exception e) { } 
		return table.getColumnName(columnIndex); 
	}

	// TableModel
	/**
	 *
	 * @return
	 */
	public int getRowCount()
	{
		return result.results.length;
	}

	// TableModel
	/**
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public Object getValueAt(int rowIndex, int columnIndex)
	{	return result.results[rowIndex][columnIndex]; }

	// TableModel
	/**
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{	return false; }

	// TableModel
	/**
	 *
	 * @param l
	 */
	public void removeTableModelListener(TableModelListener l)
	{
		if(log.isTraceEnabled())
		{	log.trace("Removing TableModelListener"); }
		synchronized(this)
		{
			int index = -1;
			for(int i = 0; i < listeners.length; i++)
			{	if(l == listeners[index]) {index = i; break; } }
			if(index > -1)
			{
				if(listeners.length > 1)
				{
					TableModelListener[] t = new TableModelListener[listeners.length - 1];
					System.arraycopy(listeners, 0, t, 0, index);
					if(index < t.length)
					{	System.arraycopy(listeners, index + 1, t, index, listeners.length - index); }
					listeners = t;
				} else {
					listeners = new TableModelListener[0];
				}
			}
		}
	}

	// TableModel
	/**
	 *
	 * @param aValue
	 * @param rowIndex
	 * @param columnIndex
	 */
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{	result.results[rowIndex][columnIndex] = aValue; }

	//ListSelectionListener
	/**
	 *
	 * @param e
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		//Ignore extra messages.
		if(e.getValueIsAdjusting()) { return; }

		ListSelectionModel lsm = (ListSelectionModel) e.getSource();
		if(lsm.isSelectionEmpty())
		{
			//no rows are selected
			if(log.isTraceEnabled())
			{	log.trace("Selection cleared"); }
		} else {
			//selectedRow is selected
			int selectedRow = lsm.getMinSelectionIndex();
			Media m = (Media) result.results[tableSorter.modelIndex(selectedRow)][result.mediaIndex];
			if(log.isTraceEnabled())
			{	log.trace("Selection changed to row " + selectedRow + ", is " + m); }
			slave.play(m);
		}
	}
	
	public void refreshView()
	{	libraryView.refresh(); }
	
	public void openInfoForSelected()
	{
		if(table.getSelectedRowCount() > 1)
		{
			int[] selectedRows = table.getSelectedRows();
			Media[] m = new Media[selectedRows.length];
			for(int i = 0; i < selectedRows.length; i++)
			{	m[i] = (Media) result.results[tableSorter.modelIndex(selectedRows[i])][result.mediaIndex]; }
			MediaTracksInfoDialog d = new MediaTracksInfoDialog(slave, m);
			d.setVisible(true);
		} else {
			int selectedRow = table.getSelectedRow();
			if(selectedRow < 0)
			{
				if(log.isTraceEnabled())
				{	log.trace("Skipping opening info for selected, nothing selected?!"); }
				return; 
			}
			Media m = (Media) result.results[tableSorter.modelIndex(selectedRow)][result.mediaIndex];
			if(log.isTraceEnabled())
			{	log.trace("Opening info dialog for track " + m); }
			MediaTrackInfoDialog d = new MediaTrackInfoDialog(slave, m);
			d.setVisible(true);
		}
	}
	
	public Media[] getSelectedMedia()
	{
		int selectedRow = table.getSelectedRow();
		if(selectedRow < 0)
		{
			if(log.isTraceEnabled())
			{	log.trace("Skipping syncing for selected, nothing selected?!"); }
			return null; 
		}
		Media[] m = new Media[1];
		m[0] = (Media) result.results[tableSorter.modelIndex(selectedRow)][result.mediaIndex];
		return m;
	}
	
	public Action[] getActions()
	{
		Action[] allActions = new Action[Media.COLUMNS.length+1];
		allActions[Media.COLUMNS.length] = new RefreshAction(this);
		for(int i = 0; i < Media.COLUMNS.length; i++)
		{
			boolean selected = false;
			for(int j = 0; j < columnHeaders.length; j++)
			{
				if(Media.COLUMNS[i].equals(columnHeaders[j]))
				{	selected = true; }
			}
			allActions[i] = new ToggleColumnAction(this, Media.COLUMNS[i], selected);
		}
		return allActions;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(log.isDebugEnabled())
		{	log.debug("Action " + e.getActionCommand()); }
		
	}
	
	public static class LibraryMouseListener extends MouseAdapter
	{
		private LibraryViewPane view;
		
		public LibraryMouseListener(LibraryViewPane view)
		{	this.view = view; }
		
		public void mouseClicked(MouseEvent e)
		{
			if(e.getClickCount() > 1)
			{
				if(view.log.isDebugEnabled())
				{	view.log.debug("was double-clicked!"); }
				view.openInfoForSelected();
			}
		}
	}
	
	public class RefreshAction extends AbstractAction
	{
		house.neko.media.slave.LibraryViewPane viewPane;
		final static private String NAME = "Refresh";
		
		public RefreshAction(house.neko.media.slave.LibraryViewPane viewPane)
		{
			super(NAME);
			putValue(super.LONG_DESCRIPTION, NAME);
			putValue(super.SHORT_DESCRIPTION, NAME);
			this.viewPane = viewPane;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			AbstractButton aButton = (AbstractButton) e.getSource();
			boolean selected = aButton.getModel().isSelected();
			if(log.isTraceEnabled()) { log.trace("refreshing LibraryViewPane"); }
			refreshView();
		}
	}
	
	public class ToggleColumnAction extends AbstractAction
	{
		house.neko.media.slave.LibraryViewPane viewPane;
		String mediaName;
		
		public ToggleColumnAction(house.neko.media.slave.LibraryViewPane viewPane, String mediaName, boolean checked)
		{
			super(mediaName);
			putValue(super.LONG_DESCRIPTION, mediaName);
			putValue(super.SHORT_DESCRIPTION, mediaName);
			putValue(super.SELECTED_KEY, checked);
			this.viewPane = viewPane;
			this.mediaName = mediaName;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			AbstractButton aButton = (AbstractButton) e.getSource();
			boolean selected = aButton.getModel().isSelected();
			if(log.isTraceEnabled()) { log.trace("change state for " + mediaName + " -> " + selected); }
			if(!selected)
			{
				TableColumn column = table.getColumn(mediaName);
				//colModel.removeColumn(column);
				table.removeColumn(column);
			} else {
				
			}
		}
	}
	
	public void update(java.util.Observable o, Object arg)
	{
		System.out.println("LibraryViewPane was updated : " + o + " -> " + arg);
		setResult(libraryView.getVisibleMedia());
		//table.tableChanged(new TableModelEvent(this, 0, result.results.length, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		if(log.isTraceEnabled())
		{	log.trace("Forcing update results from " + o); }
	}
}