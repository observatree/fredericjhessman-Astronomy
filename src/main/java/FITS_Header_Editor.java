// FITS_Header_Editor.java

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import astroj.*;

public class FITS_Header_Editor implements PlugInFilter, ListSelectionListener, ActionListener
	{
	ImagePlus imp;
	String[][] header = null;
	String[][] rows = null;

	JTable table = null;
	JFrame frame = null;
	JCheckBox keywordLock;

	public static String OK     = "OK";
	public static String CANCEL = "CANCEL";
	public static String CUT    = "CUT";
	public static String PASTE  = "PASTE";
	public static String NEW    = "NEW";
	public static String VERSION = "2008-08-03";

	boolean editKeys = false;
	boolean changed = false;

	public int setup(String arg, ImagePlus imp)
		{
		this.imp = imp;
		return DOES_ALL;
		}

	public void run(ImageProcessor ip)
		{
		// PLACE FITS HEADER IN A STRING ARRAY

		String[] hdr = FitsJ.getHeader(imp);
		int l = hdr.length;
		header = new String[l][5];
		for (int i=0; i < l; i++)
			{
			String card = hdr[i];
			String ctype = FitsJ.getCardType(card);

			header[i][0] = ""+(i+1);
			if (ctype == "C")	// COMMENT
				{
				header[i][1] = "COMMENT";
				header[i][2] = FitsJ.getCardValue (card);
				header[i][3] = null;
				}
			else if (ctype == "H")	// HISTORY
				{
				header[i][1] = "HISTORY";
				header[i][2] = FitsJ.getCardValue (card);
				header[i][3] = null;
				}
			else if (ctype == "S")
				{
				header[i][1] = FitsJ.getCardKey(card);
				header[i][2] = "\'"+FitsJ.getCardValue (card)+"\'";
				header[i][3] = FitsJ.getCardComment (card);
				}
			else	{
				header[i][1] = FitsJ.getCardKey(card);
				header[i][2] = FitsJ.getCardValue (card);
				header[i][3] = FitsJ.getCardComment (card);
				}
			header[i][4] = ctype;
			}

		// CREATE GUI

		frame = new JFrame ("FITS Header Editor");
		JPanel panel = new JPanel(new BorderLayout());
		frame.add (new JLabel("Image: "+imp.getShortTitle()));

		// CREATE TABLE

		table = new JTable (new FITSTableModel());
		// table.setFillsViewportHeight(true);		Java 1.6
		table.setShowGrid(true);
		table.doLayout();

		ListSelectionModel model = table.getSelectionModel();
		model.addListSelectionListener(this);
		FontMetrics metrics = table.getFontMetrics(table.getFont());

		TableColumn col = table.getColumnModel().getColumn(0);
		int w = metrics.stringWidth("M");
		col.setMinWidth (5*w);
		col.setMaxWidth (5*w);
		col = table.getColumnModel().getColumn(1);
		col.setMinWidth (9*w);
		col.setMaxWidth (9*w);

		col = table.getColumnModel().getColumn(4);
		col.setMinWidth (5*w);
		col.setMaxWidth (5*w);
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setToolTipText("B=boolean, \nC=comment, \nH=history, \nI=integer, \nS=string, \n?=unknown");
		col.setCellRenderer(renderer);

		// PUT TABLE IN SCROLL PANE

		JScrollPane scrollPane = new JScrollPane (table);
		panel.add (scrollPane, BorderLayout.CENTER);

		// ADD BUTTONS

		JPanel gui = new JPanel();

		JButton ok = new JButton (OK);
		ok.addActionListener (this);
		gui.add (ok);

		JButton cancel = new JButton (CANCEL);
		cancel.addActionListener (this);
		gui.add (cancel);

		// JButton cutcol = new JButton (CUT);
		// cutcol.addActionListener (this);
		// gui.add (cutcol);

		// JButton pastecol = new JButton (PASTE);
		// pastecol.addActionListener (this);
		// gui.add (pastecol);

		// JButton newcol = new JButton (NEW);
		// newcol.addActionListener (this);
		// gui.add (newcol);

		keywordLock = new JCheckBox("Lock keyword values",!editKeys);
		gui.add (keywordLock);

		panel.add (gui, BorderLayout.SOUTH);

		// FINISH

		frame.add (panel);
		frame.pack ();
		// frame.setResizable (false);
		frame.setVisible (true);
		}

	// ActionListener METHODS

	public void actionPerformed (ActionEvent e)
		{
		String cmd = e.getActionCommand();
		if (cmd.equals(CANCEL))
			{
			frame.setVisible (false);
			frame = null;
			return;
			}
		else if (cmd.equals(OK))
			{
			if (IJ.showMessageWithCancel ("Saving Modified FITS Header","The header was changed: are you sure you want to save the results?"))
				{
				IJ.log("Extracting header....");
				String[] hdr = extractHeader();
				IJ.log("Saving header...");
				if (hdr == null)
					{
					IJ.log("Unable to save header!");
					IJ.showMessage ("Unable to save header!");
					return;
					}
				IJ.log("New header ...");
                                int l=hdr.length;
                                for (int i=0; i < l; i++)
                                	IJ.log(hdr[i]);
				FitsJ.putHeader(imp,hdr);
				}
			else
				IJ.showMessage("Keeping old header!");
			frame.setVisible (false);
			frame = null;
			return;
			}
		else if (cmd.equals(CUT))
			{
			int[] cols = table.getSelectedColumns();
			if (cols.length > 0)
				cut(cols);
			}
		else if (cmd.equals(PASTE))
			{
			int col = table.getSelectedColumn();
			if (col >= 0 && rows != null)
				paste(col);
			}
		else if (cmd.equals(NEW))
			{
			int col = table.getSelectedColumn();
			if (col < 0) col = table.getRowCount()+1;
			}
		}

	protected String[] extractHeader()
		{
		int len;
		int n=0;
		int l = header.length;
		String[] hdr = new String[l];
		String s,key,val,comment,blanks;

		// COPY BASIC HEADER, CHECKING FOR 80 CHARACTER LIMIT AND FOR EMPTY FIELDS

		for (int i=0; i < l; i++)
			{
			if (header[i][1] != null && header[i][2] != null)
				{
				key = header[i][1].trim();
				while (key.length() < 8) key += " ";
				val = header[i][2].trim();
				len = key.length()+1+val.length();
				Boolean normal = !key.equals("COMMENT") && !key.equals("HISTORY");
				if (normal && !val.equals(""))
					{
					// AKEYWORD+=+VALUE+/+COMMENT
					if (len > 80)
						{
						IJ.showMessage("Entry #"+i+" is too long: length <= 80 characters!");
						table.changeSelection(i,1,false,false);
						return null;
						}
					comment = "";
					if (header[i][3] != null)
						{
						s = " / "+header[i][3].trim();
						if (len+s.length() > 80)
 							comment = s.substring(0,80-len);
						else
							comment = s;
						while (len+comment.length() < 80)
							comment += " ";
						}
					else	{
						while (len < 80) val += " ";
						}
					hdr[n++] = key+"="+val+comment;
					}
				else if (! normal)
					{
					while (len < 80) val += " ";
					hdr[n++] = key+" "+val;
					}
				}
			}

		// ADD A HISTORY ENTRY

		return FitsJ.addHistory ("Header modified by ImageJ FITS editor, Version "+VERSION,hdr);
		}

	protected void cut (int[] cols)
		{
		}

	protected void paste (int col)
		{
		}

	// ListListener METHODS

	public void valueChanged (ListSelectionEvent e)
		{
		int i1 = e.getFirstIndex();
		int i2 = e.getLastIndex();
		}

	/**
	 * A private TableModel for FITS data
	 */
	class FITSTableModel extends AbstractTableModel
		{
		private static final long serialVersionUID = 1L;	// MINOR VERSION NUMBER
		private String[] columnNames = { "#","Keyword","Value","Comment","Type" };

		public int getColumnCount() { return 5; }
		public int getRowCount() { return header.length; }
		public String getColumnName(int col) { return columnNames[col]; }
		public Object getValueAt(int row, int col) { return header[row][col]; }

		/*
		 * The 1st and last columns aren't editable, and the 2nd also if ....
		 */
		public boolean isCellEditable(int row, int col)
			{
			editKeys = !keywordLock.isSelected();
			if (col == 0 || col == 4)
				return false;
			else if (col == 1 && !editKeys)
				return false;
			return true;
			}

		public void setValueAt(Object value, int row, int col)
			{
			if (value instanceof String)
				{
				String s = ((String)value).trim();
				IJ.log("changing entry at row="+row+", col="+col+" from "+header[row][col]);
				if (row == 2 && header[row][4].equals("S"))
					{
					String q1 = s.substring(0,1);
					if ((q1.equals("'") || q1.equals("\"")) && !s.endsWith(q1))
						s += q1;
					else	{
						int l = s.length();
						q1 = s.substring(l-1,l);
						if ((q1.equals("'") || q1.equals("\"")) && !s.startsWith(q1))
							s = q1+s;
						}
					}
				IJ.log("\tto ["+s+"]");
				header[row][col] = s;
				changed = true;
				}
			fireTableCellUpdated(row, col);
			}
		}

	}
