// FitsHeader.java

import java.awt.*;
import java.util.*;
import java.io.IOException;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.process.*;

/**
 * An object to read, manipulate, write, and query FITS-based images within ImageJ.
 * The header is manipulated as an array of Strings which can be read and written via the "Info" property
 * (a Properties object wouldn't keep the cards in the right order).
 */
public class FitsHeader
	{
	public static int 	NO_CARD = -1;		// TYPES OF FITS CARDS
	public static int 	STRING_CARD = 0;
	public static int 	INTEGER_CARD = 1;
	public static int	DOUBLE_CARD = 2;
	public static int	BOOLEAN_CARD = 3;
	public static int 	COMMENT_CARD = 4;
	public static int 	HISTORY_CARD = 5;

	public static int 	KEY_PART = 0;		// PARTS OF A CARD PARSE BY THE cardParts() METHOD
	public static int 	STRING_PART= 1;
	public static int 	DOUBLE_PART = 2;
	public static int 	INTEGER_PART = 3;
	public static int 	BOOLEAN_PART = 4;
	public static int 	COMMENT_PART = 5;
	public static int 	TYPE_PART = 6;

	protected String[] cards = null;
	protected ImagePlus img = null;
	protected int depth = 0;
	protected String filename = null;

	/**
	 * Extracts the original FITS header from the Properties object of the
	 * ImagePlus image (or from the slice label in the case of an ImageStack)
	 * and returns it as an array of String objects representing each card.
	 *
	 * @param img		The ImagePlus image which has the FITS header in it's "Info" property.
	 */
	public FitsHeader (ImagePlus im)
		{
		this.img = im;
		cards = null;

		depth = img.getStackSize();
		String content = null;

		if (depth == 1)
			content = getImagePlusInfo (img);
		else if (depth > 1)
			{
			content = getImageSliceInfo (img);
			if (content == null)
				content = getImagePlusInfo (img);
			}

		// THERE APPEARS TO BE A FITS HEADER

		if (content == null) return;

		// PARSE INTO LINES

		String[] lines = content.split("\n");

		// FIND "SIMPLE" AND "END" KEYWORDS

		int istart = 0;
		for (; istart < lines.length; istart++)
			{
			if (lines[istart].startsWith("SIMPLE") ) break;
			}

		// CAN'T FIND FITS HEADER

		if (istart == lines.length)
			{
			if (depth > 1)	// MAYBE BECAUSE THERE'S ONLY ONE GLOBAL HEADER
				{
				content = getImagePlusInfo (img);
				lines = content.split("\n");
				istart = 0;
				for (; istart < lines.length; istart++)
					{
					if (lines[istart].startsWith("SIMPLE") ) break;
					}
				if (istart == lines.length)
					return;
				}
			else
				 return;
			}

		int iend = istart+1;
		for (; iend < lines.length; iend++)
			{
			if ( lines[iend].startsWith ("END ") ) break;
			}
		if (iend >= lines.length) return;

		int l = iend-istart+1;
		String header = "";
		for (int i=0; i < l; i++)
			header += lines[istart+i]+"\n";
		cards = header.split("\n");
		}

	protected String getImagePlusInfo (ImagePlus img)
		{
		filename = img.getShortTitle();
		Properties props = img.getProperties();
		if (props == null) return null;
		String content = (String)props.getProperty ("Info");
		return content;
		}
	protected String getImageSliceInfo (ImagePlus img)
		{
		filename = null;
		int slice = img.getCurrentSlice();
		ImageStack stack = img.getStack();
		String content = stack.getSliceLabel(slice);
		if (content == null) return null;

		String[] sarr = content.split("\n");
		filename = sarr[0];
		return content;
		}

	/**
	 * Sets a FITS card with an integer keyword
	 *
	 * @param key			The name of the FITS keyword (e.g. "NAXIS3").
	 * @param property		The integer value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 */
	public void setCard (String key, int property, String comment)
		{
		if (cards == null) return;
		this.set (key,""+property,comment);
		}

	/**
	 * Sets a FITS card with a double keyword.
	 *
	 * @param key			The name of the FITS keyword (e.g. "JD").
	 * @param property		The double value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 */
	public void setCard (String key, double property, String comment)
		{
		if (cards == null) return;
		this.set (key,""+property,comment);
		}

	/**
	 * Sets a FITS card with a boolean keyword.
	 *
	 * @param key			The name of the FITS keyword (e.g. "EXTENDED").
	 * @param property		The boolean value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 */
	public void setCard (String key, boolean property, String comment)
		{
		if (cards == null) return;
		if (property)
			this.set (key,"T       ",comment);
		else
			this.set (key,"F       ",comment);
		}

	/**
	 * Sets a FITS card with a string keyword.
	 *
	 * @param key			The name of the FITS keyword (e.g. "DATE-OBS").
	 * @param property		The string value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 */
	public void setCard (String key, String property, String comment)
		{
		if (cards == null) return;
		this.set (key,"\""+property+"\"",comment);
		}

	/**
	 * Sets a FITS card in the form of a String to an ImagePlus image.
	 *
	 * @param key			The name of the FITS keyword.
	 * @param property		The value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 */
	protected void set (String key, String val, String comment)
		{
		if (cards == null) return;

		String k = key.trim();
		String card = null;

		// GET OLD VALUE AND COMMENT FROM CARD CONTAINING KEYWORD

		int icard = findCardWithKey (k);
		if (icard >= 0)
			card = cards[icard];
		String old = FitsHeader.getCardValue(card);
		String comm = FitsHeader.getCardComment (card);

		// IF THERE'S A NEW OR OLD COMMENT, USE IT

		String v = val;
		if (comment != null)
			v += " / "+comment.trim();
		else if (comm != null)
			v += " / "+comm;

		// SAVE NEW HEADER

		if (icard >= 0)
			cards[icard] = v;
		else
			addCard (v);
		}

	/**
	 * Returns the indx-th card of the header.
	 */
	public String get (int indx)
		{
		if (cards == null) return null;
		if (indx < cards.length)
			return new String(cards[indx]);
		else
			return null;
		}

	/**
	 * Saves a FITS header in an array of Strings back into an ImagePlus's "Info" property.
	 */
	public void putHeader (ImagePlus im, int slice)
		{
		if (cards == null) return;
		String s = FitsHeader.unsplit(cards,"\n");
		if (im.getStackSize() == 1)
			{
			Properties props = im.getProperties ();
			props.setProperty ("Info",s);
			}
		else	{
			ImageStack stack = im.getStack();
			String[] labels = stack.getSliceLabel(slice).split("\n");
			stack.setSliceLabel(labels[0]+"\n"+s,slice);
			}
		}

	/**
	 * Adds a FITS card to the FITS header stored in a String array.
	 *
	 * @param card		A FITS card image to be added to the FITS header String array.
	 */
	public void addCard (String card)
		{
		if (cards == null) return;
		int l = cards.length;
		String[] hdr = new String[l+1];
		for (int i=0; i < l; i++)
			hdr[i] = new String (cards[i]);
		if (cards[l-1].startsWith("END"))
			{
			hdr[l-1] = new String (card);
			hdr[l] = new String (cards[l-1]);
			}
		else
			hdr[l] = new String (card);
		cards = hdr;
		}

	/**
	 * Removes all FITS cards with the given key from the FITS header stored in a String array.
	 *
	 * @param key		A FITS card keyword for those cards to be removed from the FITS header String array.
	 */
	public void removeCards (String key)
		{
		if (cards == null) return;
		int l = cards.length;
		String[] hdr = new String[l+1];
		for (int i=0; i < l; i++)
			{
			if (! FitsHeader.getCardKey (cards[i]).equals(key))
				hdr[i] = new String (cards[i]);
			}
		cards = hdr;
		}

	/**
	 * Finds the location of a FITS card in a String array having the FITS keyword "key".
	 *
	 * @param key		A String containing the FITS keyword to be searched for.
	 */
	public int findCardWithKey (String key)
		{
		if (cards == null) return -1;
		int n=cards.length;
		String k = key.trim();
		for (int i=0; i < n; i++)
			{
			String l = cards[i].trim();
			if (l.startsWith(k)) return i;
			}
		return -1;
		}

	/**
	 * Adds a comment to the image's FITS header.
	 *
	 * @param comment		The FITS comment string.
	 */
	public void addComment (String comment)
		{
		if (cards == null) return;
		addCard ("COMMENT "+comment);
		}

	/**
	 * Adds a FITS history card to the image's FITS header.
	 *
	 * @param comment		The FITS history string.
	 */
	public void addHistory (String history)
		{
		if (cards == null) return;
		addCard ("HISTORY "+history);
		}

	/**
	 * Extracts the FITS keyword from a card.
	 *
	 * @param card		The FITS card image from which the comment should be extracted.
	 */
	public static String getCardKey (String card)
		{
		int equals = -1;
		equals = card.indexOf("=");
		if (equals < 0)
			return null;		// NO VALUE (e.g. COMMENT?)
		return card.substring(0,equals).trim();
		}

	/**
	 * Extracts the FITS value from a card
	 *
	 * @param card		The FITS card image from which the comment should be extracted.
	 */
	public static String getCardValue (String card)
		{
		if (card == null) return null;

		int q1 = -1;
		int q2 = -1;
		int dq1 = -1;
		int dq2 = -1;
		int slash = -1;
		int equals = -1;

		// FIND EQUALS SIGN

		equals = card.indexOf("=");
		if (equals < 0) return null;		// NO VALUE (e.g. COMMENT?)

		// LOOK FOR QUOTED VALUE

		q1 = card.indexOf("'");
		if (q1 >= 0)
			q2 = card.indexOf("'",q1+1);

		// LOOK FOR COMMENT

		slash = card.indexOf("/");

		// IS SLASH INSIDE QUOTED CONTENTS?

		if (q1 > 0 && q2 > 0 && slash > q1 && slash < q2)
			return card.substring (q1+1,q2).trim();

		// NO COMMENT PRESENT, RETURN EVERYTHING RIGHT OF '='

		else if (slash < 0)
			{
			if (q1 > 0 && q2 > 0)			// AS VALUE IN QUOTES
				return card.substring (q1+1,q2);
			else					// AS UNQUOTED VALUE
				return card.substring (equals+1).trim();
			}

		// COMMENT PRESENT, RETURN EVERYTHING IN-BETWEEN
		else	{
			if (q1 > 0 && q2 > 0)			// AS VALUE IN QUOTES
				return card.substring (q1+1,q2);
			else					// AS UNQUOTED VALUE
				return card.substring (equals+1,slash).trim();
			}
		}

	/**
	 * Extracts the FITS comment from a card, including something like    "DATE = '12/34/56' / A date."
	 *
	 * @param card		The FITS card image from which the comment should be extracted.
	 */
	public static String getCardComment (String card)
		{
		if (card == null) return null;

		int q1 = -1;
		int q2 = -1;
		int slash = -1;
		int equals = -1;

		// FIND EQUALS SIGN

		equals = card.indexOf("=");
		if (equals < 0) return null;		// NO VALUE (e.g. COMMENT?)

		// LOOK FOR SIMPLE QUOTE IN VALUE

		q1 = card.indexOf("'");
		if (q1 >= 0)
			q2 = card.indexOf("'",q1+1);

		// OR FOR A DOUBLE QUOTE IN VALUE

		if (q1 < 0)
			{
			q1 = card.indexOf("\"");
			if (q1 >= 0)
				q2 = card.indexOf("\"",q1+1);
			}

		// LOOK FOR COMMENT

		slash = card.indexOf("/");

		if (slash < 0)			// NO COMMENT PRESENT
			return null;
		else if (q2 < 0)		// NO MATCHING QUOTES PRESENT, RETURN STRING RIGHT OF SLASH AS COMMENT
			return card.substring (slash+1);
		else if (slash > q2)		// MATCHING QUOTES IN VALUE, RETURN STRING RIGHT OF SLASH AS COMMENT
			return card.substring (slash+1);

		slash = card.indexOf("/",q2+1);
		return card.substring (slash+1);
		}

	/**
	 * Extracts a double value from a FITS card.
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 */
	public static double getCardDoubleValue (String card)
		{
		double d = Double.NaN;
		String s = FitsHeader.getCardValue (card);
		try	{
			d = Double.parseDouble(s);
			}
		catch (NumberFormatException e)
			{
			}
		return d;
		}

	/**
	 * Extracts an int value from a FITS card.
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 */
	public static int getCardIntValue (String card)
		{
		int i = 0;	// Integer.NaN?????
		String s = FitsHeader.getCardValue (card);
		try	{
			i = Integer.parseInt(s);
			}
		catch (NumberFormatException e)
			{
			}
		return i;
		}

	/**
	 * Finds and extracts a double value from a FITS header stored in a String array.
	 *
	 * @param key		The FITS keyword that should be found and parsed.
	 */
	public double getDoubleValue (String key)
		{
		if (cards == null) return Double.NaN;
		double d = Double.NaN;
		int icard = findCardWithKey (key);
		if (icard < 0) return d;
		return FitsHeader.getCardDoubleValue (cards[icard]);
		}

	/**
	 * Finds and extracts an integer value.
	 *
	 * @param key		The FITS keyword that should be found and parsed.
	 */
	public int getIntValue (String key)
		{
		if (cards == null) return 0;
		int icard = findCardWithKey (key);
		if (icard < 0) return 0;	// SOMETHING LIKE Integer.NaN?
		return FitsHeader.getCardIntValue(cards[icard]);
		}

	/**
	 * Expands a Properties (key,val) pair into proper FITS format.
	 *
	 * @param key		The property key, which may contain a prefix showing that it is a FITS entry.
	 * @param val		The string value.
	 */
	protected static String expandKeyValue (String key, String val)
		{
		if (key == null || val == null) return null;

		int l = val.length();
		if (l > 70) l=70;
		String v = val.substring(0,l);

		l = key.length();
		String k = key;
		if (key.startsWith ("COMMENT"))
			return new String ("COMMENT "+v);
		else if (key.startsWith ("HISTORY"))
			return new String ("HISTORY "+v);
		while (l++ < 8) k += " ";
		return new String (k+"= "+v.trim());
		}

	/**
	 * Extracts the different parts of a FITS header card.
	 *
	 * @param card		A FITS card image.
	 */
	public static Object[] cardParts (String card)
		{
		String key = null;
		String val = null;
		String comment = null;
		double d = 0.0;
		int i = 0;
		boolean b = false;
		int typ = NO_CARD;

		// System.err.println("card="+card);

		String s = new String(card);

		// COMMENT

		if (card.startsWith ("COMMENT"))
			{
			key ="COMMENT";
			val = card.substring (7);
			comment = null;
			typ = COMMENT_CARD;
			}

		// HISTORY

		else if (card.startsWith ("HISTORY"))
			{
			key = "HISTORY";
			val = card.substring (7);
			comment = null;
			typ= HISTORY_CARD;
			}

		else
			{
			int eq = s.indexOf ("=");
			// System.err.println("eq="+eq);
			if (eq < 0) return null;
			key = s.substring (0,eq);
			// System.err.println("key="+key);
			if (key == null) return null;
			val = s.substring (eq+1);
			// System.err.println("val="+val);

			// COMMENT

			comment = FitsHeader.getCardComment (s.substring(eq+1));
			// System.err.println ("comment="+comment);
			if (comment != null && !comment.equals(""))
				{
				int slash = s.indexOf (comment);
				// System.err.println ("slash=["+s.substring(slash-1,slash+1)+"]");
				val = s.substring (eq+1,slash-1).trim();
				// System.err.println ("val=["+val+"]");
				}

			// STRING

			if (val.startsWith("\'") || val.startsWith("\""))
				{
				s = val;
				val = s.substring(1,s.length()-1);
				// System.err.println ("val=["+val+"]");
				typ = STRING_CARD;
				}

			// BOOLEAN

			else if (val.equals("T") || val.equals("F"))
				{
				b = val.equals("T");
				typ = BOOLEAN_CARD;
				}

			// INTEGER OR DOUBLE

			else
				{
				try	{
					i = Integer.parseInt(val);
					typ = INTEGER_CARD;
					}
				catch (NumberFormatException e)
					{
					try	{
						d = Double.parseDouble(val);
						typ = DOUBLE_CARD;
						}
					catch (NumberFormatException nfe)
						{
						typ = NO_CARD;
						}
					}
				}
			}

		Object[] arr = new Object[]
				{key, val, new Double(d), new Integer(i), new Boolean(b), comment, new Integer(typ)};
		return arr;
		}

	/**
	 * Copies a FITS header from one image to another.
	 */
	public void copyHeaderTo (ImagePlus imTo, int slice)
		{
		if (cards == null) return;
		addHistory ("Complete FITS header copied from image "+filename+", slice "+depth);
		if (slice < 1)
			putHeader (imTo,imTo.getCurrentSlice());
		else
			putHeader (imTo,slice);
		}

	/**
	 * Pads a string to a given total length.
	 *
	 * @param s		The input string.
	 * @param length	The length to which the string should be padded.
	 */
	public static String pad (String s, int length)
		{
		int l = s.length();
		String blanks="";
		while (l++ < length) blanks += " ";
		return s+blanks;
		}

	/**
	 * Unsplits a String.
	 *
	 * @param arr		A String array to be concatenated.
	 */
	public static String unsplit (String[] arr, String sep)
		{
		String s = arr[0];
		for (int i=1; i < arr.length; i++)
			s += sep+arr[i];
		return s;
		}


	/********************************** DATE, TIME, JD ROUTINES ***********************************************/


	/**
	 * Extracts a DateTime string either from an explict DateTime entry or builds one from
	 * separate date and time entries.
	 */
	public String getDateTime ()
		{
		String dt = getExplicitDateTime ();
		if (dt != null) return dt;

		String date = getDate ();
		if (date == null) return null;
		String time = getTime ();
		if (time == null) return null;
		dt = date+"T"+time;
		return dt;
		}

	/**
	 * Extracts explicit DateTime string from the FITS "DATE-OBS" entry.
	 */
	public String getExplicitDateTime ()
		{
		String datum = getDateObs();
		if (datum == null) return null;

		// MAKE SURE IT'S REALLY AN ISO DATETIME WITH yyyy-{m}m-{d}dT{hh:mm:ss}

		int i = datum.indexOf("T");
		int j = datum.indexOf("-");
		if (i > 7 && j == 4)
			return datum;
		return null;
		}

	/**
	 * Extracts calendar date from the FITS header stored in a String array.
	 */
	public String getDateObs ()
		{
		String dateobs = null;

		// TRY "DATE-OBS"

		int icard = findCardWithKey ("DATE-OBS");
		if (icard > 0)
			dateobs = FitsHeader.getCardValue (cards[icard]);

		// TRY "DATEOBS"

		if (dateobs == null)
			{
			icard = findCardWithKey ("DATEOBS");
			if (icard > 0)
				dateobs = FitsHeader.getCardValue (cards[icard]);
			}

		// TRY "DATE_OBS"

		if (dateobs == null)
			{
			icard = findCardWithKey ("DATE_OBS");
			if (icard > 0)
				dateobs = FitsHeader.getCardValue (cards[icard]);
			}
		return dateobs;
		}

	/**
	 * Extracts calendar date from the FITS header stored in a String array.
	 */
	public String getDate ()
		{
		String datum = getDateObs ();
		if (datum == null) return null;

		// RE-ARRANGE INTO ISO FORMAT

		String dt="";

		// CHECK FOR dd/mm/yy

		if (datum.length() == 8 && datum.charAt(2) == '/' && datum.charAt(5) == '/')
			dt = new String("19"+datum.substring(6,8)+"-"+datum.substring(3,5)+"-"+datum.substring(0,2));

		// CHECK FOR dd/mm/yyyy

		else if (datum.length() == 10 && datum.charAt(2) == '/' && datum.charAt(5) == '/')
			dt = new String(datum.substring(6,10)+"-"+datum.substring(3,5)+"-"+datum.substring(0,2));

		// CHECK FOR yyyy-mm-dd

		else if (datum.length() == 10 && datum.charAt(4) == '-' && datum.charAt(7) == '-')
			dt = new String(datum.substring(0,4)+"-"+datum.substring(5,7)+"-"+datum.substring(8,10));

		// CHECK FOR yy-mm-dd

		else if (datum.length() == 8 && datum.charAt(2) == '-' && datum.charAt(5) == '-')
			dt = new String("19"+datum.substring(0,2)+"-"+datum.substring(3,5)+"-"+datum.substring(6,8));

		// OR GIVE UP

		else
			{
			beeps();
			IJ.log ("Unable to parse date "+datum+" from image "+filename);
			return null;
			}
		return dt;
		}

	/**
	 * Extracts UT Time from a FITS header in the form of a String array.
	 */
	public String getTimeObs ()
		{
		String timeobs = null;

		// TRY "TIME-OBS"

		int icard = findCardWithKey ("TIME-OBS");
		if (icard > 0)
			timeobs = FitsHeader.getCardValue (cards[icard]);

		// TRY "TIMEOBS"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TIMEOBS");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		// TRY "TIME_OBS"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TIME_OBS");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		// OR EXTRACT FROM "TM-START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TM-START");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		// OR EXTRACT FROM "TM_START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TM_START");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		// OR EXTRACT FROM "UT"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UT");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		// OR EXTRACT FROM "UTSTART"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UTSTART");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		// OR EXTRACT FROM "UT-START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UT-START");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		// OR EXTRACT FROM "UT_START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UT_START");
			if (icard > 0)
				timeobs = FitsHeader.getCardValue (cards[icard]);
			}

		return timeobs;
		}

	/**
	 * Extracts UT Time in format hh:mm:ss from a FITS header in a String array.
	 */
	public String getTime ()
		{
		String datum = getTimeObs ();
		String dt="";

		// CHECK FOR hh:mm:ss.sss

		if (datum.indexOf(":") > 0)
			dt = datum;

		// OR CHECK FOR FLOATING POINT NUMBER

		else	{
			try	{
				double fp = Double.parseDouble(datum);
				int hh = (int)fp;
				int mm = (int)((fp-(double)hh)*60.0);
				double ss = (fp-(double)hh-(double)mm/60.0)/60.0;

				String sh=null;
				String sm=null;

				if (hh < 10)
					sh = "0"+hh;
				else
					sh = ""+hh;
				if (mm < 10)
					sm = "0"+mm;
				else
					sm = ""+mm;
				dt = sh+":"+sm+":"+ss;
				}
			catch (NumberFormatException e)
				{
				beeps();
				IJ.log("Unable to parse time "+datum);
				return null;
				}
			}
		return dt;
		}

	/**
	 * Returns time of day in seconds.
	 */
	public double getDecimalTime ()
		{
		double t = Double.NaN;
		String time = getTime();
		if (time == null) return Double.NaN;

		try	{
			int i = time.indexOf(":");
			double hh = Double.parseDouble (time.substring (0,i));
			double mm = Double.parseDouble (time.substring (i+1,i+3));
			double ss = Double.parseDouble (time.substring (i+4));
			t = 3600.0*hh+60.0*mm+ss;
			}
		catch (NumberFormatException e)
			{
			beeps();
			IJ.log ("Unable to parse time "+time);
			}
		return  t;
		}

	/**
	 * Extracts exposure time from the FITS header in a  String array.
	 */
	public double getExposureTime ()
		{
		double tstart=0.0;
		double tend=0.0;

		try	{
			// CHECK FOR STANDARD KEYWORD "EXPTIME" (SECS)

			tstart = getDoubleValue ("EXPTIME");
			if (! Double.isNaN(tstart))
				return tstart;

			// CHECK FOR KEYWORD "EXPOSURE" (e.g. Mount Stromlo)

			tstart = getDoubleValue ("EXPOSURE");
			if (! Double.isNaN(tstart))
				return tstart;

			// OR CHECK FOR 'TM-START' AND 'TM-END' (SECS)

			tstart = getDoubleValue ("TM-START");
			tend   = getDoubleValue ("TM-END");
		
			// OR CHECK FOR 'TM_START' AND 'TM_END' (SECS)

			if (Double.isNaN(tstart) || Double.isNaN(tend))
				{
				tstart = getDoubleValue ("TM_START");
				tend   = getDoubleValue ("TM_END");
				}

			// OR CHECK FOR 'UT-START' AND 'UT-END' (SECS)

			if (Double.isNaN(tstart) || Double.isNaN(tend))
				{
				tstart = getDoubleValue ("UT-START");
				tend   = getDoubleValue ("UT-END");
				}

			// OR CHECK FOR 'UT_START' AND 'UT_END' (SECS)

			if (Double.isNaN(tstart) || Double.isNaN(tend))
				{
				tstart = getDoubleValue ("UT-START");
				tend   = getDoubleValue ("UT-END");
				}

			if (Double.isNaN(tstart) || Double.isNaN(tend))
				return Double.NaN;
			}
		catch (NumberFormatException e)
			{
			beeps();
			IJ.log ("Unable to extract exposure time from FITS header: "+e.getMessage());
			return Double.NaN;
			}

		// WATCH OUT FOR CHANGE OF DAYS

		if (tend < tstart) tend += 3600*24.0;

		// RETURN DIFFERENCE BETWEEN START AND END TIMES

		return (tend-tstart);
		}

	/**
	 * Returns mid-exposure dateTime.
	 */
	public String getMeanDateTime ()
		{
		String dt = getDateTime ();
		double t = getExposureTime ();
		if (dt == null || Double.isNaN(t)) return null;

		t *= 0.5;

		Duration dur;
		Date date;
		try	{
			dur = new Duration("P"+t+"S");
			date = DateParser.parse (dt);
			}
		catch (InvalidDateException e)
			{
			IJ.log (e.getMessage());
			return null;
			}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime (date);
		try	{
			Calendar result = dur.addTo(cal);
			return DateParser.getIsoDate(result.getTime());
			}
		catch (InvalidDateException e)
			{
			IJ.log ("Unable to add half of exposure time = "+t+" to datetime "+dt);
			return null;
			}
		}


/**************************************** JD METHODS **************************************************/


	/**
	 * Returns JD from a FITS header stored in a String array.
	 */
	public double getJD ()
		{
		boolean modified = false;
		double julian = Double.NaN;

		// TRY TO GET JD FROM FITS HEADER

		julian = getDoubleValue ("JD-OBS");
		if (Double.isNaN(julian))
			julian = getDoubleValue ("JD");
		if (Double.isNaN(julian))
			{
			julian = getDoubleValue ("MJD-OBS");
			if (! Double.isNaN(julian))
				modified = true;
			}
		if (Double.isNaN(julian))
			{
			julian = getDoubleValue ("MJD");
			if (! Double.isNaN(julian))
				modified = true;
			}

		// OTHERWISE DERIVE FROM DATETIME

		if (Double.isNaN(julian))
			{
			String dt = getDateTime ();
			if (dt == null) return Double.NaN;
			julian = JulianDate.JD (dt);
			}
		if (Double.isNaN(julian))
			return Double.NaN;
		
		if (modified) julian += 2400000.0;
		return julian;
		}

	/**
	 * Returns mid-exposure Julian Date from a FITS header stored in a String array.
	 */
	public double getMeanJD ()
		{
		double jd = getJD ();
		double texp = getExposureTime ();
		if (Double.isNaN(jd) || Double.isNaN(texp))
			return Double.NaN;
		else
			return jd+0.5*(texp/3600.0)/24.0;
		}

	/**
	 * Returns MJD from a FITS heaader stored in a String array.
	 */
	public double getMJD ()
		{
		double jd = getJD ();
		if (!Double.isNaN(jd)) jd -= 2400000.0;
		return jd;
		}

	/**
	 * Returns mid-exposure MJD from a FITS header stored in a String array.
	 */
	public double getMeanMJD ()
		{
		double jd = getMeanJD();
		if (!Double.isNaN(jd)) jd -= 2400000.0;
		return jd;
		}

	/**
	 * Three beeps.
	 */
	protected void beeps ()
		{
		IJ.beep();
		IJ.beep();
		IJ.beep();
		}
	}
