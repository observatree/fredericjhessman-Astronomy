// TableFit.java

import java.io.*;
import java.util.*;

/**
 * Sub-class of LLS which fits tabular data.
 */
public class TableFit extends LLS
	{
	public double[] yData = null;
	public double[] yErrs = null;
	public double[][] modelData = null;
	public int nCols=0;
	public int nRows=0;
	public String separ = null;

	protected Vector lines = null;

	/**
	 * Construct from a file.
	 */
	public TableFit (String filename)
		{
		lines = new Vector();
		getData (filename);
		}

	/**
	 * Reads file given on command line and fits tabular data.
	 */
	public static void main (String[] args)
		{
		int[] xcols=null;
		int[] ycols=null;

		if (args.length != 3)
			{
			System.out.println ("Syntax:\n\tjava TableFit {file} {ycol{,yerr}} {col1,col2,col3,...}");
			return;
			}

		// PARSE ARGUMENTS

		String[] s = args[1].split(",");
		if (s.length < 1)
			{
			System.err.println ("Not enough ycolumns : "+args[1]);
			return;
			}
		try	{
			ycols = new int[s.length];
			for (int i=0; i < s.length; i++)
				ycols[i] = Integer.parseInt(s[i]);
			}
		catch (NumberFormatException e)
			{
			System.err.println ("Unable to parse columns \""+args[1]+"\"");
			return;
			}

		s = args[2].split(",");
		try	{
			xcols = new int[s.length];
			for (int i=0; i < s.length; i++)
				xcols[i] = Integer.parseInt(s[i]);
			}
		catch (NumberFormatException e)
			{
			System.err.println ("Unable to parse columns \""+args[2]+"\"");
			return;
			}

		// READ DATA FROM THE FILE

		TableFit fitter = new TableFit (args[0]);
		fitter.extractModelData (xcols);
		fitter.extractData (ycols);
		fitter.setFunctionType (LLS.OTHER_FUNCTION,null,null);

		// PERFORM FIT

		fitter.fit (fitter.yData, fitter.yErrs);
		fitter.printLastFit (fitter.yData, fitter.yErrs);
		}

	/**
	 * Reads data from a file.  Comment lines starting with a "#" are ignored.
	 */
	protected void getData (String filename)
		{
		String line;

		// READ DATA IN THE FORM OF STRINGS, ONE PER LINE

		try	{
			BufferedReader in = new BufferedReader (new FileReader(filename));
			while ((line = in.readLine()) != null)
				{
				if (!line.startsWith("#"))
					{
					lines.addElement (line);
					getSeparator(line);
					}
				}
			in.close();
			}
		catch (IOException e)
			{
			System.err.println(e.getMessage());
			System.exit(1);
			}
		nRows = lines.size();
		if (nRows == 0)
			{
			System.err.println("No data");
			System.exit(1);
			}
		}

	public void extractData (int[] cols)
		{
		yData = extract (cols[0]);
		if (cols.length == 2)
			yErrs = extract (cols[1]);
		}

	public void extractModelData (int[] cols)
		{
		nCols = cols.length;
		modelData = extract (cols);
		}

	public double[] extract (int col)
		{
		String line;
		double[] datex = new double[nRows];

		for (int l=0; l < nRows; l++)
			{
			line = (String)lines.get(l);
			String[] words = line.split(separ);
			if (col > words.length)
				{
				System.err.println ("Cannot parse numbers: only sensed "+words.length+" columns");
				System.exit(1);
				}
			try	{
				datex[l] = parseData (words[col-1]);
				}
			catch (NumberFormatException e)
				{
				System.err.println ("Cannot parse numbers");
				System.exit(1);
				}
			}
		return datex;
		}

	public double[][] extract (int[] cols)
		{
		int n = cols.length;

		String line;
		double[][] datex = new double[nRows][n];

		for (int l=0; l < nRows; l++)
			{
			line = (String)lines.get(l);
			String[] words = line.split(separ);
			int c=0;
			try	{
				for (int k=0; k < n; k++)
					{
					c = cols[k];
					if (c > words.length)
						{
						System.err.println ("Cannot parse numbers: sensed "+words.length+" but want "+c+" columns");
						System.exit(1);
						}
					datex[l][k] = parseData (words[c-1]);
					}
				}
			catch (NumberFormatException e)
				{
				System.err.println ("Cannot parse column "+c+" of "+line+" : ["+words[c-1]+"]");
				System.exit(1);
				}
			}
		return datex;
		}


	/**
	 * Simple parsing of tabular data (could be refined by sub-class).
	 */
	protected double parseData (String s) throws NumberFormatException
		{
		return Double.parseDouble (s);
		}

	/**
	 * Intelligently estimates the number of columns.
	 */
	protected void getSeparator (String s)
		{
		String[] commas = s.split(",");
		String[] semicolons = s.split(";");
		String[] spaces = s.split(" ");
		String[] tabs = s.split("\t");
		int nCommas = commas.length;
		int nSemicolons = semicolons.length;
		int nSpaces = spaces.length;
		int nTabs = tabs.length;
		separ = ",";
		int n = nCommas;
		if (nSemicolons > n)
			{
			n = nSemicolons;
			separ = ";";
			}
		if (nTabs >= n)
			{
			n = nTabs;
			separ = "\t";
			}
		if (n <= 2)
			separ = " ";
		}

	public double[] fit (double[] y, double[] yerr)
		{
		return super.fit (null, y, yerr, this);
		}

	public void printLastFit (double[] y, double[] yerr)
		{
		System.out.println (super.results (null, y, yerr, this, null));
		printTable (y,yerr);
		}

	public void printTable (double[] y, double[] yerr)
		{
		System.out.println ("\nSummary of Table Fit\n"+bar+"\nRow #\ty\t\terr\t\tyfit\t\tO-C\t\ttable\n"+bar);
		int n=y.length;
		for (int i=0; i < n; i++)
			{
			double yfit = model (i,y[i],coef);
			double omc = y[i]-yfit;
			double err = Double.NaN;
			if (yerr != null) err = yerr[i];
			System.out.print (""+(i+1)+"\t"+format.format(y[i])+"\t"+format.format(err)+"\t"+format.format(yfit)+"\t"+format.format(omc));
			for (int j=0; j < nCols; j++)
				System.out.print ("\t"+format.format(modelData[i][j]));
			System.out.println ("");
			}
		}

	// LinearFunction METHODS

	/**
	 * The table function ignores the values of x and only uses the table row indice "indx".
	 */
	public double[][] modelFunctions (int indx, double x, double y, double wgt)
		{
		double[][] r = new double[nCols][nCols+1];
		for (int j=0; j < nCols; j++)
			{
			for (int i=0; i < nCols; i++)
				r[j][i] = modelData[indx][j]*modelData[indx][i]*wgt;
			r[j][nCols] = modelData[indx][j]*y*wgt;
			}
		return r;
		}

	public String modelName()
		{
		return new String ("table of "+nCols+" model functions");
		}

	/**
	 * Returns the size of the model function array (e.g. ncol=1 for a table).
	 */
	public int numberOfParameters()
		{
		return nCols;
		}
	}

