// MeasurementTable.java

package astroj;

import ij.*;
import ij.measure.*;
import ij.text.*;

import java.awt.Frame;
import java.io.*;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Like an ImageJ ResultsTable but with more functionality.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.4
 * @date 2006-Sep-11
 *
 * @version 1.5
 * @date 2012-Sep-09
 * @changes Better parsing in getTableFromFile()
 *
 * @version 1.6
 * @date 2017-Aug-14 (FVH)
 * @changes Added getSelectedRow()
 */
public class MeasurementTable extends ResultsTable
	{
	public static String PREFIX = new String("Measurements");
	public static String RESULTS = new String("Results");
	protected String shortName = null;
	public static int DEFAULT_DECIMALS = 6;

	/**
	 * Creates an empty default MeasurementTable.
	 */
	public MeasurementTable ()
		{
		setPrecision(DEFAULT_DECIMALS);
		shortName = null;
		}

	/**
	 * Creates an empty MeasurementTable with a given long name.
	 */
	public MeasurementTable (String tableName)
		{
		setPrecision (DEFAULT_DECIMALS);
		shortName = MeasurementTable.shorterName (tableName);
		}

	/**
	 * Creates a MeasurementTable object from a TextWindow.
	 */
	public MeasurementTable (TextWindow w)
		{
		super();
		setPrecision (DEFAULT_DECIMALS);
		TextPanel panel = w.getTextPanel();
		shortName = MeasurementTable.shorterName (w.getTitle());
		int count = panel.getLineCount();
		// IJ.showMessage(panel.getText());
		}

	/**
	 * Transfers the last entry from a MeasurementTable to the standard Results table.
	 */
	public static void transferLastRow (String tableName)
		{
		MeasurementTable t = new MeasurementTable(tableName);
		ResultsTable r = ResultsTable.getResultsTable();
		if (t == null || r == null)
			{
			IJ.error("Unable to read measurement table or Results table!");
			return;
			}
		int row = t.getCounter();
		int ncols = t.getLastColumn();
		// IJ.log("row="+row+", ncols="+ncols);
		for (int col=0; col < ncols; col++)
			{
			String heading = t.getColumnHeading(col);
			double val = t.getValueAsDouble(col,row);
			// IJ.log(heading+" "+val);
			r.addValue(heading,val);
			}
		return;
		}

	/* AVAILABLE IN 1.5
	public void setLabel (String column, String label)
		{
		addValue (column, getCounter(), label);
		}
	*/

	/**
	 * Stores a number using a temporary different number of decimal places.
	 */
	public void addValue (String column, double value, int places)
		{
		setPrecision(places);
		super.addValue (column,value);
		// setPrecision (DEFAULT_DECIMALS);
		// PRESENT ResultsTable DOESN'T KEEP TRACK OF INDIVIDUAL PRECISIONS!!!
		}  

	/**
	 * Displays/Refreshes a MeasurementTable.
	 */
	public void show()
		{
		super.show (MeasurementTable.longerName(shortName));
		}

	/**
	 * ResultTable method to be overridden: a ResultTable's shortTitle is a MeasurementTable's longName.
	 */
	public String shortTitle()
		{
		return MeasurementTable.longerName(shortName);
		}

	/**
	 * Returns the ImagePlus from which the measurements in the MeasurementTable
	 * were obtained.  The image must still be displayed for the WindowManager to find it.
	 */
	public ImagePlus getImage()
		{
		if (shortName == null)
			return null;
		int ids[] = WindowManager.getIDList();
		int n = WindowManager.getWindowCount();
		for (int i=0; i < n; i++)
			{
			ImagePlus img = WindowManager.getImage(ids[i]);
			if (shortName.equals(img.getTitle()))	// getShortTitle()?
				return img;
			}
		return null;
		}

	/**
	 * Class method for extracting a TextPanel from a MeasurementTable with a given name.
	 */
	public static TextPanel getTextPanel (String tableName)
		{
		Frame frame = WindowManager.getFrame(tableName);
		if (frame == null || !(frame instanceof TextWindow))
			{
			// IJ.error ("Unable to access "+tableName+"!");
			return null;
			}
		else	{
			TextWindow win = (TextWindow)frame;
			return win.getTextPanel();
			}
		}

	/**
	 * Indicates whether a MeasurementTable exists with the given name.
	 */
	public static boolean exists (String tableName)
		{
		TextPanel tp = getTextPanel (tableName);
		if (tp == null)
			return false;
		else
			return true;
		}

	/**
	 * Returns a MeasurementTable reconstructed from a text file produced by ImageJ from a MeasurementTable/ResultsTable.
	 */
	public static MeasurementTable getTableFromFile (String filename)
		{
		BufferedReader in = null;
		MeasurementTable table = null;

		try	{
			in = new BufferedReader(new FileReader(filename));

			// READ HEADER LINE

			String line = in.readLine();
			if (line == null)
				{
				IJ.error("MeasurementTable: cannot read header line!");
				in.close();
				return null;
				}
			// IJ.log("header:"+line);

			// OPEN NEW MeasurementTable

			table = new MeasurementTable();	// SHOULD USE filename); BUT CAN'T BECAUSE OF PRIVATE WindowManager.getNonImageWindows() METHOD

			// CREATE COLUMNS

			String[] labels = line.split("\t");
			int n = labels.length;
			if (labels[n-1].trim().equals("")) n -= 1;
			int nstart=0;
			if (labels[0].trim().equals("")) nstart=1;

	String nix = "";
	for (int l=nstart; l < n; l++)
		{
		// table.setHeading (l,labels[l]);
		nix += labels[l]+",";
		}
	// IJ.log("labels: "+nix);


			// READ DATA

			int row = 0;
			double d = 0.0;
			boolean usesText = false;
			while ((line = in.readLine()) != null)
				{
				// IJ.log(""+row+": "+line);
				table.incrementCounter();

				String[] words = line.trim().split("\t");
				int m = words.length;
				// if (m != n) IJ.log("n="+n+", m="+m);

				if (row == 0)
					{
					try	{
						d = Double.parseDouble(words[0]);
						d = Double.parseDouble(words[1]);
						}
					catch (NumberFormatException e)
						{
						usesText = true;
						// IJ.log("usesText : "+words[0]+" "+words[1]);
						}
					}
				for (int col=nstart; col < m; col++)
					{
					if (col == nstart && usesText)
						{
						// IJ.log("adding label "+words[col]);
						table.setLabel(words[nstart],row);	// labels[nstart].trim(),words[nstart]);
						}
					else	{
						if (words[col] == null)
							d = 0.0;	// Double.NaN;
						else if (words[col].trim().equals("") || words[col].trim().equals("-"))
							d = 0.0;	// Double.NaN;
						else if (isHMS(words[col]))
							d = hms(words[col]);
						else
							d = Double.parseDouble(words[col]);
						if (nstart+col <= labels.length)
							{
							// IJ.log("adding value "+d+" to "+labels[col]);
							table.addValue(labels[col].trim(),d);
							}
						else	{
							table.addValue("?",d);
							}
						table.setValue(col,row,d);
						}
					}
				row++;
				}
			in.close();
			}
		catch (IOException e)
			{
			// IJ.log ("MeasurementTable IOException: "+e.getMessage());
			IJ.error("MeasurementTable: "+e.getMessage());
			table = null;
			}
		catch (NumberFormatException nfe)
			{
			// IJ.log ("MeasurementTable NumberFormatException: "+nfe.getMessage());
			IJ.error("MeasurementTable: "+nfe.getMessage());
			table = null;
			}
		try { in.close(); } catch(Exception exc) { IJ.error("Cannot close file?"); }
		// IJ.log("sucessful!");
		return table;
		}

	/**
	 * Returns an existing MeasurementTable reconstructed from the TextWindow with the appropriate name.
	 */
	public static MeasurementTable getTable (String tableName)
		{
		int n;
		String str="";

		// IJ.log("Reading "+tableName);

		String name = MeasurementTable.longerName (tableName);

		// CREATE EMPTY TABLE

		MeasurementTable table = new MeasurementTable (name);

		// IF NO PREVIOUS TABLE EXISTS, RETURN EMPTY TABLE

		if (! exists(name))
			return table;

		// GET CONTENTS OF EXISTING MeasurementTable

		TextPanel panel = getTextPanel (name);

		String[] headings = panel.getColumnHeadings().split("\t");
		// for (int i=0; i < headings.length; i++)
		//	IJ.log((i+1)+": heading=["+headings[i]+"]");

		StringTokenizer parser = new StringTokenizer ("","\t");

		n = panel.getLineCount();
		for (int row=0; row < n; row++)
			{

			parser = new StringTokenizer (panel.getLine(row),"\t");
			str = parser.nextToken();		// IGNORE MEASUREMENT NUMBER

			table.incrementCounter();
			for (int column = 1; column < headings.length; column++)
				{
				if (parser.hasMoreTokens() && (str = parser.nextToken()) != null)
					{
					try	{
						double d = Double.parseDouble(str);
						table.addValue(headings[column],d);
						}
					catch (NumberFormatException e)
						{
						table.setLabel(str,row);	// headings[1],str);
						}
					}
				else	{
					table.addValue(headings[column],0.0);
					}
				}
			}

		return table;
		}

	/**
	 * Returns a MeasurementTable with the path pathname.  If already exists, the table in reconstructed.
	 */
	public static MeasurementTable readTable (String pathname)
		{
		// FILTER pathname HERE ONE OF THESE DAYS!!!!

		// CREATE EMPTY TABLE

		MeasurementTable table = new MeasurementTable (pathname);

		try	{
			int column=-1;
			int row = -1;
			FileInputStream stream = new FileInputStream(pathname);
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

			StreamTokenizer parser = new StreamTokenizer(reader);
			parser.eolIsSignificant(true);
			parser.whitespaceChars((int)'\t',(int)'\t') ;     // JUST HTABS
			parser.wordChars((int)'!',(int)'~');

			int ttype=0;
			while (parser.nextToken() != StreamTokenizer.TT_EOF)
				{
				ttype = parser.ttype;
				// START OF NEXT LINE
				if (ttype == StreamTokenizer.TT_EOL)
					{
					column = -1;
					row++;
					}
				// START OF FIRST LINE: STRING?
				else if (row == -1)
					{
					if ( ttype == StreamTokenizer.TT_WORD)
						// table.setHeading(++column, parser.sval);
						table.addLabel (parser.sval);
					column++;
					}
				// ... OR NUMBER?
				else if (ttype == StreamTokenizer.TT_NUMBER)
					{
					if (column == -1)
						table.incrementCounter();
					else
						table.setValue(column,row,parser.nval);
					column++;
					}
				}
			reader.close();
			stream.close();
			}
		catch (FileNotFoundException e)
			{
			IJ.error("Error: "+e.getMessage());
			}
		catch (IOException e)
			{
			IJ.error("Error: "+e.getMessage());
			}
		return table;
		}

	/**
	 * Retrieves a double array column.
	 */
	public double[] getDoubleColumn (int col)
		{
		int cntr = this.getCounter();
		double[] d = new double[cntr];
		for (int r=0; r < cntr; r++)
			{
			double dd = this.getValueAsDouble(col,r);
			// IJ.log("arr["+col+"]["+r+"]="+dd);
			d[r] = dd;
			}

/* OLD VERSION DOESN'T WORK IF TABLE CONTAINS NaN!!!
		float[] f = this.getColumn(col);
		if (f == null) return null;

		double[] d = new double[f.length];
		for (int i=0; i < f.length; i++)
			d[i] = f[i];
*/
		return d;
		}

	/**
	 * Inserts a double array column.
	 */
	public boolean putDoubleColumn (String title, double[] arr)
		{
		int col = getFreeColumn (title);
		if (col == COLUMN_IN_USE || col == TABLE_FULL)
			return false;

		for (int i=0; i < arr.length; i++)
			setValue (col, i, arr[i]);
		return true;
		}

	/**
	 * Returns the full name of a MeasurementTable, including the standard prefix if not already given.
	 */
	public static String longerName (String name)
		{
		if (name == null)
			return new String (MeasurementTable.PREFIX);
		else if (name.equals(MeasurementTable.RESULTS))
			return name;
		else if (name.equals(MeasurementTable.PREFIX))
			return name;
		else if (name.startsWith(MeasurementTable.PREFIX+" in "))
			return name;
		else if (name.indexOf(".txt") >= 0)
			return name;
		else
			return new String (MeasurementTable.PREFIX+" in "+name);
		}

	/**
	 * Returns the part of the MeasurementTable name without the standard prefix.
	 */
	public static String shorterName (String tableName)
		{
		if (tableName == null)
			return null;
		else if (tableName.equals (MeasurementTable.PREFIX))
			return null;
		else if (tableName.equals (MeasurementTable.RESULTS))
			return null;
		else if (tableName.startsWith(MeasurementTable.PREFIX+" in "))
			return tableName.substring(tableName.indexOf(" in ")+4);
		else
			return tableName;
		}

	/**
	 * This method desperately attempts to replace the private functionality of WindowManager.getNonImageWindows() (before ImageJ Version 1.38q)
	 */
	public static String[] getMeasurementTableNames()
		{
		Vector<String> frames = new Vector<String>();

		// FIND MEASUREMENT TABLES ASSOCIATED WITH IMAGES

		if (IJ.versionLessThan("1.40"))
			{
			Frame std = WindowManager.getFrame ("Results");
			if (std != null)
				frames.addElement ("Results");
			std = WindowManager.getFrame (PREFIX);
			if (std != null)
				frames.addElement (PREFIX);
			String[] openImages = IJU.listOfOpenImages (null);
			if (openImages != null && openImages.length > 0)
				{
				for (int i=0; i < openImages.length; i++)
					{
					String longName = longerName (openImages[i]);
					if (WindowManager.getFrame (longName) != null)
						frames.addElement (longName);
					}
				}
			}
		else	{
			Frame[] windows = WindowManager.getNonImageWindows();
			if (windows != null && windows.length > 0)
				{
				Frame std = WindowManager.getFrame ("Results");
				if (std != null)
					frames.addElement ("Results");
				for (int i=0; i < windows.length; i++)
					{
					String title = windows[i].getTitle();
					// if (title.startsWith("Measurement"))
						frames.addElement (title);
					}
				}
			}

		// ANYTHING AT ALL?

		int n = frames.size();
		if (n == 0) return null;

		// THEN GATHER INTO STRING ARRAY

		String[] result = new String [n];
		for (int i=0; i < n; i++)
			result[i] = new String (frames.elementAt (i));
		return result;
		}

	/**
	 * Checks to see if a string contains a hh:mm:ss.sss representation of an angle/time.
	 */
	static public boolean isHMS (String s)
		{
		String[] arr = s.split(":");
		if (arr.length > 1)
			return true;
		else
			return false;
		}

	/**
	 * Converts hh:mm:ss.sss to a number.
	 */
	static public double hms (String s) 
		{
		double d[] = new double[] {0.0,0.0,0.0};
		String[] arr = s.split(":");
		int n = (arr.length > 3)? 3 : arr.length;
		double sgn = 1.0;
		for (int i=0; i < n; i++)
			{
			d[i] = Double.parseDouble(arr[i]);
			if (arr[i].trim().startsWith("-")) sgn = -1.0;
			}
		return sgn*(Math.abs(d[0])+Math.abs(d[1])/60.0+Math.abs(d[2])/3600.0);
		}

	/**
	 * Returns the selected row as a string
	 */
	static public String getSelectedRow (String tableName)
		{
		if (! exists (tableName))
			{
			IJ.error ("Cannot find table "+tableName+"!");
			return null;
			}
		TextPanel textPanel = getTextPanel(tableName);
		int row = textPanel.getSelectionStart();
		String s = textPanel.getLine (row);
		return s;
		}

	/**
	 * Returns the selected value as a string
	 */
	static public String getSelectedStringValue (String tableName, String columnName)
		{
		if (! exists (tableName))
			{
			IJ.error ("Cannot find table "+tableName+"!");
			return null;
			}
		TextPanel textPanel = getTextPanel(tableName);
		int row = textPanel.getSelectionStart();
		String s = textPanel.getLine (row);
		String[] vals = s.split("\t");
		for (int i=0; i < vals.length; i++)
			IJ.log("i="+i+", val="+vals[i]);
		String headings = textPanel.getColumnHeadings ();
		String[] labels = headings.split("\t");
		for (int i=0; i < labels.length; i++)
			IJ.log("i="+i+", label="+labels[i]);
		return s;
		}
	}
