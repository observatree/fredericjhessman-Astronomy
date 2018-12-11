// FITSUtilities.java

import ij.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;

import java.io.*;
import java.util.*;

/**
 * This set of static functions enables one to integrate the FITS header information
 * into the normal ImagePlus functionality.
 *
 * @author F.V. Hessman, IAG, Georg-August-Universitaet Goettingen
 * @date 2006-02-24
 * @version 0.1
 * @description Implementation of DateTime functions.
 */
public class FITSUtilities
	{

	/**
	 * Extracts the FITS header info from either the "Info" properties of normal ImagePlus images
	 * or from the slice label strings of ImageStacks.
	 */ 
	public static Properties getFITSProperties (ImagePlus img)
		{
		if (img == null)
			{
			IJ.error("No image!");
			return null;
			}
		int ndim = 0;
		if (img.getWidth() > 0) ndim++;
		if (img.getHeight() > 0) ndim++;

		Properties oldProps = img.getProperties();

		// CHECK TO SEE IF THE FITS PROPERTIES HAVE ALREADY BEEN EXTRACTED

		if (oldProps != null)
			{
			String test1 = oldProps.getProperty("SIMPLE");
			String test2 = oldProps.getProperty("BITPIX");
			if (test1 != null && test2 != null)
				return oldProps;
			}

		// IF ORIGINAL IMAGE WAS IN FITS FORMAT, GET HEADER FROM "Info" PROPERTY

		FileInfo info = img.getOriginalFileInfo();
		Properties newProps = new Properties();
		int depth = img.getStackSize();

		String header="";
		if (depth == 1 && info != null && info.fileFormat == FileInfo.FITS)
			{
			header = (String)img.getProperty ("Info");
			if (header == null)
				{
				System.err.println ("Image does not contain FITS headers in Property \"Info\" although FITS fileFormat!");
				return null;
				}
			}

		// IF NOT, PERHAPS THE SLICES IN A STACK ONCE WHERE IN FITS FORMAT

		else if (depth > 1)
			{
			int slice = img.getCurrentSlice();
			ImageStack stack = img.getStack();
			header = stack.getSliceLabel(slice);
			if (header == null)
				{
				// IJ.showStatus ("Stack does not contain FITS headers in slice labels! ("+slice+")");
				return null;
				}
			}
/*
		// ELSE, SOMETHING'S WRONG AND EXIT

		else	{
			IJ.showStatus ("Image was not originally FITS format or a stack of FITS images!");
			return null;
			}
*/
		// READ HEADER INTO TEMPORARY PROPERTIES

		try	{
			StringBufferInputStream stream = new StringBufferInputStream (header);
			newProps.load(stream);
			}
		catch (IOException e)
			{
			IJ.error ("Unable to extract FITS header from image Properties \"Info\" or stack labels: "+e.getMessage());
			return null;
			}

		// MAKE SURE THE BASIC FITS KEYWORDS ARE STILL CORRECT

		newProps.setProperty ("NAXIS",""+ndim);
		newProps.setProperty ("NAXIS1",""+img.getWidth());
		newProps.setProperty ("NAXIS2",""+img.getHeight());

		// RETURN NEW IMAGE PROPERTIES IF NO OLD

		if (oldProps == null || depth > 1)
			return newProps;

		// OR TRANSFER PROPERTIES TO OLD IMAGE PROPERTIES

		Enumeration e = newProps.propertyNames();
		while (e.hasMoreElements())
			{
			String key = (String)e.nextElement();
			String val = newProps.getProperty(key);
System.err.println("FITS: key=["+key+"], val=["+val+"]");
			if (oldProps.getProperty(key) == null)
				oldProps.setProperty(key,val);
			}

		return oldProps;
		}

	/**
	 * Copies the FITS header entries from one image to another.
	 */
	public static Properties copyFITSProperties (ImagePlus fromImage, ImagePlus toImage)
		{
		Properties fromProps = getFITSProperties(fromImage);
		if (fromProps == null) return null;

		Properties props = getFITSProperties (toImage);

		Enumeration e = fromProps.propertyNames();
		while (e.hasMoreElements())
			{
			String key = (String)e.nextElement();
			String val = fromProps.getProperty(key);
			if (fromProps.getProperty(key) == null)
				props.setProperty(key,val);
			}

		// MAKE SURE THE BASIC FITS KEYWORDS ARE CORRECT

		props.setProperty ("NAXIS","2");
		props.setProperty ("NAXIS1",""+toImage.getWidth());
		props.setProperty ("NAXIS2",""+toImage.getHeight());

		// WRITE FITS HEADER TO ImageJ Info PROPERTY

		returnPropertiesToInfo (props);
		return props;
		}

	/**
	 * Transfers the FITS header stored in the image Properties object back into the ImagePlus Info string whence they came.
	 */
	public static void returnPropertiesToInfo (Properties props)
		{
		StringBuffer buffer = new StringBuffer(props.size());
		buffer.append ("SIMPLE  = "+props.getProperty("SIMPLE")+"\n");
		buffer.append ("NAXIS   = "+props.getProperty("NAXIS")+"\n");
		buffer.append ("NAXIS1 = "+props.getProperty("NAXIS1")+"\n");
		buffer.append ("NAXIS2 = "+props.getProperty("NAXIS2")+"\n");
		Enumeration e = props.propertyNames();
		while (e.hasMoreElements())
			{
			String key = (String)e.nextElement();
			if (		!key.equals("SIMPLE")	&&
					!key.equals("NAXIS")	&&
					!key.equals("NAXIS1")	&&
					!key.equals("NAXIS2")	&&
					!key.equals("END"))
				{
				String val = props.getProperty(key);
				buffer.append (key+" = "+val+"\n");
				}
			}
		buffer.append ("END");
		props.setProperty ("Info",buffer.toString());
		}

	/**
	 * Returns FITS header info given a FITS keyword from an ImagePlus, removing enclosing quotes and comments if present.
	 */
	public static String getFITSProperty (ImagePlus img, String key)
		{
		if (img == null || key == null) return null;
		Properties props = getFITSProperties (img);
		return getFITSProperty (props,key);
		}

	/**
	 * Returns FITS header info given a FITS keyword from a Properties list, removing enclosing quotes and comments if present.
	 */
	public static String getFITSProperty (Properties props, String key)
		{
		return filter(props.getProperty(key));
		}


/********************************** DATE, TIME, JD ROUTINES ***********************************************/


	/**
	 * Extracts explicit DateTime string from the FITS "DATE-OBS" entry using an ImagePlus.
	 */
	public static String getDateTime (ImagePlus img)
		{
		if (img == null) return null;
		Properties props = getFITSProperties (img);
		return getDateTime (props);
		}

	/**
	 * Extracts a DateTime string either from an explict DateTime entry or builds one from
	 * separate date and time entries.
	 */
	public static String getDateTime (Properties props)
		{
		String dt = getExplicitDateTime (props);
		if (dt != null) return dt;

		String date = getDate (props);
		if (date == null) return null;
		String time = getTime (props);
		if (time == null) return null;
		dt = date+"T"+time;
		return dt;
		}

	/**
	 * Extracts explicit DateTime string from the FITS "DATE-OBS" entry using a Properties list.
	 */
	public static String getExplicitDateTime (Properties props)
		{
		if (props == null) return null;

		// TRY "DATE-OBS"

		String datum = getFITSProperty (props, "DATE-OBS");

		// TRY "DATEOBS"

		if (datum == null)
			datum = getFITSProperty (props, "DATEOBS");

		// TRY "DATE_OBS"

		if (datum == null)
			datum = getFITSProperty (props, "DATE_OBS");

		if (datum == null) return null;

		// MAKE SURE IT'S REALLY AN ISO DATETIME WITH yyyy-{m}m-{d}dT{hh:mm:ss}

		int i = datum.indexOf("T");
		int j = datum.indexOf("-");

		if (i > 7 && j == 4)
			return datum;
		return null;
		}

	/**
	 * Extracts calendar date from the FITS header using an ImagePlus.
	 */
	public static String getDate (ImagePlus img)
		{
		if (img == null) return null;
		Properties props = getFITSProperties (img);
		return getDate (props);
		}

	/**
	 * Extracts calendar date from the FITS header using a Properties list.
	 */
	public static String getDate (Properties props)
		{
		if (props == null) return null;
		String datum = null;

		// OR EXTRACT FROM "DATE-OBS"?

		datum = getFITSProperty (props, "DATE-OBS");

		// OR EXTRACT FROM "DATEOBS"?

		if (datum == null)
			datum = getFITSProperty (props, "DATEOBS");

		// OR EXTRACT FROM "DATE_OBS"?

		if (datum == null)
			datum = getFITSProperty (props, "DATE_OBS");

		if (datum == null) return null;

		// RE-ARRANGE INTO ISO FORMAT

		String dt="";

		// CHECK FOR dd/mm/yy

		if (datum.length() == 8 && datum.charAt(2) == '/' && datum.charAt(5) == '/')
			dt = new String("19"+datum.substring(6,8)+"-"+datum.substring(3,5)+"-"+datum.substring(1,3));

		// CHECK FOR dd/mm/yyyy

		else if (datum.length() == 10 && datum.charAt(2) == '/' && datum.charAt(5) == '/')
			dt = new String(datum.substring(6,10)+"-"+datum.substring(3,5)+"-"+datum.substring(1,3));

		// CHECK FOR yyyy-mm-dd

		else if (datum.length() == 10 && datum.charAt(4) == '-' && datum.charAt(7) == '-')
			dt = new String(datum.substring(0,4)+"-"+datum.substring(5,7)+"-"+datum.substring(8,10));

		// CHECK FOR yy-mm-dd

		else if (datum.length() == 8 && datum.charAt(2) == '-' && datum.charAt(5) == '-')
			dt = new String("19"+datum.substring(0,2)+"-"+datum.substring(3,5)+"-"+datum.substring(6,8));

		// OR GIVE UP

		else
			{
			IJ.error("Unable to parse date "+datum);
			return null;
			}
		return dt;
		}

	/**
	 * Extracts UT Time from the FITS header using an ImagePlus.
	 */
	public static String getTime (ImagePlus img)
		{
		if (img == null) return null;
		Properties props = getFITSProperties (img);
		return getTime (props);
		}

	/**
	 * Extracts UT Time in format hh:mm:ss from the FITS header using a Properties list.
	 */
	public static String getTime (Properties props)
		{
		if (props == null) return null;
		String datum = null;

		// OR EXTRACT FROM "TIME-OBS"

		datum = getFITSProperty (props, "TIME-OBS");

		// OR EXTRACT FROM "TIMEOBS"

		if (datum == null)
			datum = getFITSProperty (props, "TIMEOBS");

		// OR EXTRACT FROM "TIME_OBS"

		if (datum == null)
			datum = getFITSProperty (props, "TIME_OBS");

		// OR EXTRACT FROM "TM-START"

		if (datum == null)
			datum = getFITSProperty (props, "TM-START");

		// OR EXTRACT FROM "TM_START"

		if (datum == null)
			datum = getFITSProperty (props, "TM_START");

		// OR EXTRACT FROM "UT"

		if (datum == null)
			datum = getFITSProperty (props, "UT");

		// OR EXTRACT FROM "UTSTART"

		if (datum == null)
			datum = getFITSProperty (props, "UTSTART");

		// OR EXTRACT FROM "UT-START"

		if (datum == null)
			datum = getFITSProperty (props, "UT-START");

		// OR EXTRACT FROM "UT_START"

		if (datum == null)
			datum = getFITSProperty (props, "UT_START");

		if (datum == null) return null;
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
				IJ.error("Unable to parse time "+datum);
				return null;
				}
			}
		return dt;
		}

	/**
	 * Returns time of day in seconds.
	 */
	public static double getDecimalTime (Properties props)
		{
		double t = Double.NaN;
		String time = getTime(props);
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
			IJ.error ("Unable to parse time "+time);
			}
		return  t;
		}

	/**
	 * Extracts exposure time in seconds from the FITS header using an ImagePlus.
	 */
	public static double getExposureTime (ImagePlus img)
		{
		if (img == null) return Double.NaN;
		Properties props = getFITSProperties (img);
		return getExposureTime (props);
		}

	/**
	 * Extracts exposure time from the FITS header using a Properties list.
	 */
	public static double getExposureTime (Properties props)
		{
		if (props == null) return Double.NaN;

		double ts=0.0;
		double te=0.0;

		try	{
			// CHECK FOR STANDARD KEYWORD "EXPTIME" (SECS)

			String texp = getFITSProperty(props, "EXPTIME");
			if (texp != null)
				{
				ts = Double.parseDouble(texp);
				return ts;
				}

			// CHECK FOR KEYWORD "EXPOSURE" (e.g. Mount Stromlo)

			texp = getFITSProperty(props, "EXPOSURE");
			if (texp != null)
				{
				ts = Double.parseDouble(texp);
				return ts;
				}

			// OR CHECK FOR 'TM-START' AND 'TM-END' (SECS)

			String tstart = getFITSProperty (props,"TM-START");
			String tend   = getFITSProperty (props,"TM-END");
		
			// OR CHECK FOR 'TM_START' AND 'TM_END' (SECS)

			if (tstart == null || tend == null)
				{
				tstart = getFITSProperty (props,"TM_START");
				tend   = getFITSProperty (props,"TM_END");
				}

			// OR CHECK FOR 'UT-START' AND 'UT-END' (SECS)

			if (tstart == null || tend == null)
				{
				tstart = getFITSProperty (props,"UT-START");
				tend   = getFITSProperty (props,"UT-END");
				}

			if (tstart == null || tend == null) return Double.NaN;

			ts = Double.parseDouble (tstart);
			te = Double.parseDouble (tend);
			}
		catch (NumberFormatException e)
			{
			IJ.error ("Unable to extract exposure time from FITS header: "+e.getMessage());
			return Double.NaN;
			}

			// WATCH OUT FOR CHANGE OF DAYS

		if (te < ts) te += 3600*24.0;

		// RETURN DIFFERENCE BETWEEN START AND END TIMES

		return (te-ts);
		}

	/**
	 * Returns mid-exposure dateTime using an ImagePlus.
	 */
	public static String getMeanDateTime (ImagePlus img)
		{
		if (img == null) return null;
		Properties props = getFITSProperties (img);
		return getMeanDateTime (props);
		}

	/**
	 * Returns mid-exposure dateTime using a Properties list.
	 */
	public static String getMeanDateTime (Properties props)
		{
		if (props == null) return null;

		String dt = getDateTime (props);
		double t=getExposureTime (props);
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
			IJ.error(e.getMessage());
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
			IJ.error ("Unable to add half of exposure time = "+t+" to datetime "+dt);
			return null;
			}
		}


/**************************************** JD METHODS **************************************************/


	/**
	 * Returns JD either from FITS header or from dateTime using an ImagePlus.
	 */
	public static double getJD (ImagePlus img)
		{
		if (img == null) return Double.NaN;
		Properties props = getFITSProperties (img);
		return getJD (props);
		}

	/**
	 * Returns JD either from FITS header or from dateTime using a Properties list.
	 */
	public static double getJD (Properties props)
		{
		if (props == null) return Double.NaN;

		boolean modified = false;
		double julian = Double.NaN;

		// TRY TO GET JD FROM FITS HEADER

		String jd = getFITSProperty (props, "JD-OBS");
		if (jd == null)
			jd = getFITSProperty (props, "JD");
		if (jd == null)
			{
			jd = getFITSProperty (props, "MJD-OBS");
			if (jd != null) modified = true;
			}

		// OTHERWISE DERIVE FROM DATETIME

		if (jd == null)
			{
			String dt = getDateTime (props);
			if (dt == null) return Double.NaN;
			julian = JulianDate.JD (dt);
			}
		else	{
			try	{
				julian = Double.parseDouble(jd);
				}
			catch (NumberFormatException e)
				{
				return Double.NaN;
				}
			}
		if (Double.isNaN(julian)) return Double.NaN;
		
		if (modified) julian += 2400000.0;
		return julian;
		}

	/**
	 * Returns mid-exposure Julian Date using an ImagePlus.
	 */
	public static double getMeanJD (ImagePlus img)
		{
		if (img == null) return Double.NaN;
		Properties props = getFITSProperties (img);
		return getMeanJD (props);
		}

	/**
	 * Returns mid-exposure Julian Date using a Properties list.
	 */
	public static double getMeanJD (Properties props)
		{
		if (props == null) return Double.NaN;

		double jd = getJD (props);
		double texp = getExposureTime (props);
		if (Double.isNaN(jd) || Double.isNaN(texp))
			return Double.NaN;
		else
			return jd+0.5*(texp/3600.0)/24.0;
		}

	/**
	 * Returns mid-exposure MJD using an ImagePlus.
	 */
	public static double getMeanMJD (ImagePlus img)
		{
		if (img == null) return Double.NaN;
		Properties props = getFITSProperties (img);
		return getMeanMJD (props);
		}

	/**
	 * Returns mid-exposure MJD using a Properties list.
	 */
	public static double getMeanMJD (Properties props)
		{
		double jd = getMeanJD(props);
		if (!Double.isNaN(jd)) jd -= 2400000.0;
		return jd;
		}

	/**
	 * Returns MJD using an ImagePlus.
	 */
	public static double getMJD (ImagePlus img)
		{
		if (img == null) return Double.NaN;
		Properties props = getFITSProperties (img);
		return getMJD (props);
		}

	/**
	 * Returns MJD from a Properties list.
	 */
	public static double getMJD (Properties props)
		{
		double jd = getJD (props);
		if (!Double.isNaN(jd)) jd -= 2400000.0;
		return jd;
		}


/*************************************** MISC METHODS **********************************************/


	/**
	 * Removes all of the unwanted stuff around a FITS header entry.
	 */
	public static String filter (String s)
		{
		if (s == null) return null;

		// REMOVE LEADING AND TRAILING SPACES

		String t = s.trim();
		int i1=0;
		int i2=t.length();

		// IS CONTENT IN QUOTES?

		if (t.charAt(i1) == '\'')
			{
			i1++;
			i2=t.indexOf("'",i1);
			return t.substring(i1,i2);
			}

		// OTHERWISE REMOVE COMMENT DEFINED BY FIRST SLASH

		int i=t.indexOf("/",i1);
		if (i >i1) i2=i;
		return t.substring(i1,i2).trim();
		}
	}
