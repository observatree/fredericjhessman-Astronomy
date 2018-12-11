// Aperture_.java

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import astroj.*;

/**
 * Simple circular aperture tool using a circular background annulus.
 * Results are entered in a MeasurementTable as well as in a dialogue
 * pop-up window.
 * 
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2006-Feb-15
 * @changes Based on original ApertureTool but made it one-shot to permit use in a macro tool a la HOU
 *
 * @version 1.1
 * @date 2006-Apr-25
 * @changes Aperture defined by 3 radii (1x star, 2xsky) (FVH)
 *
 * @version 1.2
 * @date 2006-Dec-12
 * @changes  Added support for OverlayCanvas so that aperture radii and results displayed;
 * @changes  Added static method "a2rect" to make ROI's centered on best integer pixel.
 *
 * @version 1.3
 * @date 2007-Apr-11
 * @changes  Added "forgiving" reporting of errors, display of values from FITS header.
 *
 * @version 1.4
 * @date 2007-May-01
 * @changes  Added "slice" column to insure that the number of columns in a saved MeasurementTable file remains constant.
 *
 * @version 1.5
 * @date 2007-Aug-21
 * @changes Added temporary overlay possibility (to prevent future manipulations from occuring only with a ROI).
 *
 * @version 1.6
 * @date 2007-Sep-17
 * @changes Switched from FITSUtilities to FitsJ (support for separate FITS headers in stack images).
 *
 * @version 1.7
 * @date 2008-02-07
 * @changes Added support for WCS.
 *
 * @version 1.8
 * @date 2008-06-02
 * @changes Added support for ApertureRoi
 *
 * @version 1.9
 * @date 2009-01-10
 * @changes Added retry option.
 *
 * @version 1.10
 * @date 2009-07-05
 * @changes Added variance option.
 *
 * @version 1.11
 * @dave 2010-03-18
 * @author Karen Collins (Univ. Louisville/KY)
 * @changes
 * 1) Changes to the measurements table management code to support the new options mentioned in the
 *	Set_Aperture section.
 * 2) Changes to the preferences management code to support the new options mentioned in the Set_Aperture section.
 *
 * @version 1.12
 * @date 2010-Nov-24
 * @author Karen Collins (Univ. Louisville/KY)
 * @changes Added support for removal of stars from background region (>3 sigma from mean)
 *
 * @version 1.13
 * @date 2011-03-27
 * @author Karen Collins (Univ. Louisville/KY)
 * @changes Merged fix from F.V. Hessman - now uses the centers of pixels
 * as a measure of position when the user has turned off the automatic centering.
 */
public class Aperture_ implements PlugInFilter
	{
	ImagePlus img;
	ImageProcessor imp;

	ImageCanvas canvas;
	OverlayCanvas ocanvas;

	double xCenter, yCenter, radius, rBack1, rBack2, back, source, serror;
	double xWidth, yWidth, saturationWarningLevel, vradius, vrBack1, vrBack2;
	double angle, round, variance;
	double mjd = Double.NaN;

	double[] fitsVals = null;
	String fitsKeywords = "TEL-FOCU";

	int count=0;
	boolean oneTable = true;

	boolean backIsPlane = false;
	boolean reposition  = true;
	boolean forgiving   = false;
	boolean retry       = false;
	boolean removeBackStars = true;
	boolean useVariableAp = false;

	boolean showPosition = true;
	boolean showPhotometry = true;
	boolean showPeak = false;
	boolean showSaturationWarning = false;
	boolean showWidths = false;
	boolean showRadii = false;
	boolean showTimes = false;
	boolean showRaw = false;
	boolean showMeanWidth = false;
	boolean showAngle = false;
	boolean showRoundness = false;
	boolean showVariance = false;
	boolean showErrors = false;
	boolean showSNR = false;
	boolean showFits = false;
	boolean showRADEC = false;

	boolean starOverlay = true;
	boolean skyOverlay = false;
	boolean valueOverlay = false;
	boolean tempOverlay = false;
	boolean clearOverlay = false;

	String filename;
	int stackSize, slice;
	long sourceCount, backCount;
	Rectangle rct;
	boolean isCalibrated;

	Centroid center;
	GFormat g;
	MeasurementTable table = null;
	Photometer photom;
	WCS wcs = null;

	double ccdGain = 1.0;	// e-/count
	double ccdNoise = 0.0;	// e-
	double ccdDark = 0.0;	// e- AND NOT e-/sec!!!
	String darkKeyword = new String("");

	boolean temporary = false;
	boolean isFITS = false;
	boolean debug = false;

	double[] raDec = null;

	public static String AP_IMAGE = new String("Label");
	public static String AP_SLICE = new String("slice");

	public static String AP_XCENTER = new String("X");
	public static String AP_YCENTER = new String("Y");
	public static String AP_SOURCE = new String("Source-Sky");
	public static String AP_BACK = new String("Sky/Pixel");
	public static String AP_RSOURCE  = new String("Source_Radius");
	public static String AP_RBACK1  = new String("Sky_Radius(min)");
	public static String AP_RBACK2  = new String("Sky_Radius(max)");
	public static String AP_RAWSOURCE = new String("RawSource-RawSky");
	public static String AP_RAWBACK = new String("RawSky/Pixel");
	public static String AP_MJD = new String("J.D.-2400000");
	public static String AP_XWIDTH = new String("X-Width");
	public static String AP_YWIDTH = new String("Y-Width");
	public static String AP_MEANWIDTH = new String("Width");
	public static String AP_ANGLE = new String("Angle");
	public static String AP_ROUNDNESS = new String ("Roundness");
	public static String AP_VARIANCE = new String("Variance");
	public static String AP_SOURCE_ERROR = new String("Source_Error");
	public static String AP_SOURCE_SNR = new String("Source_SNR");
	public static String AP_RA = new String("R.A.[h]");
	public static String AP_DEC = new String("Dec.[d]");
	public static String AP_PEAK = new String ("Peak");
	public static String AP_WARNING = new String ("Saturated");

	public static String AP_PREFS_RADIUS = new String ("aperture.radius");
	public static String AP_PREFS_RBACK1 = new String ("aperture.rback1");
	public static String AP_PREFS_RBACK2 = new String ("aperture.rback2");
	public static String AP_PREFS_SHOWPEAK = new String ("aperture.showpeak");
	public static String AP_PREFS_SHOWSATWARNING = new String ("aperture.showsaturationwarning");
	public static String AP_PREFS_SATWARNLEVEL = new String ("aperture.saturationwarninglevel");

	public static String AP_PREFS_ONETABLE = new String ("aperture.onetable");

	// THESE ARE IN MultiAperture_ !!!!
	// public static String AP_PREFS_WIDETABLE = new String ("aperture.widetable");
	// public static String AP_PREFS_FOLLOW = new String ("aperture.follow");
	// public static String AP_PREFS_SHOWRATIO = new String ("aperture.showratio");
	// public static String AP_PREFS_NAPERTURESDEFAULT = new String ("aperture.naperturesdefault");

	public static String AP_PREFS_BACKPLANE = new String ("aperture.backplane");
	public static String AP_PREFS_CCDGAIN = new String ("aperture.ccdgain");
	public static String AP_PREFS_CCDNOISE = new String ("aperture.ccdnoise");
	public static String AP_PREFS_CCDDARK = new String ("aperture.ccddark");
	public static String AP_PREFS_DARKKEYWORD = new String ("aperture.darkkeyword");

	public static String AP_PREFS_REPOSITION = new String ("aperture.reposition");
	public static String AP_PREFS_FORGIVING = new String ("aperture.forgiving");
	public static String AP_PREFS_FITSKEYWORDS = new String ("aperture.fitskeywords");
	public static String AP_PREFS_RETRY = new String ("aperture.retry");
	public static String AP_PREFS_REMOVEBACKSTARS = new String ("aperture.removebackstars");

	public static String AP_PREFS_SHOWPOSITION = new String ("aperture.showposition");
	public static String AP_PREFS_SHOWPHOTOMETRY = new String ("aperture.showphotometry");
	public static String AP_PREFS_SHOWWIDTHS = new String ("aperture.showwidths");
	public static String AP_PREFS_SHOWRADII = new String ("aperture.showradii");
	public static String AP_PREFS_SHOWTIMES = new String ("aperture.showtimes");
	public static String AP_PREFS_SHOWRAW = new String ("aperture.showraw");
	public static String AP_PREFS_SHOWMEANWIDTH = new String ("aperture.showmeanwidth");
	public static String AP_PREFS_SHOWANGLE = new String ("aperture.showangle");
	public static String AP_PREFS_SHOWROUNDNESS = new String ("aperture.showroundness");
	public static String AP_PREFS_SHOWVARIANCE = new String ("aperture.showvariance");
	public static String AP_PREFS_SHOWERRORS = new String ("aperture.showerrors");
	public static String AP_PREFS_SHOWSNR = new String ("aperture.showsnr");
	public static String AP_PREFS_SHOWRATIOERROR = new String ("aperture.showratioerror");
	public static String AP_PREFS_SHOWRATIOSNR = new String ("aperture.showratiosnr");
	public static String AP_PREFS_SHOWFITS = new String ("aperture.showfits");
	public static String AP_PREFS_SHOWRADEC = new String ("aperture.showradec");

	public static String AP_PREFS_STAROVERLAY = new String ("aperture.staroverlay");
	public static String AP_PREFS_SKYOVERLAY = new String ("aperture.skyoverlay");
	public static String AP_PREFS_VALUEOVERLAY = new String ("aperture.valueoverlay");
	public static String AP_PREFS_TEMPOVERLAY = new String ("aperture.tempoverlay");
	public static String AP_PREFS_CLEAROVERLAY = new String ("aperture.clearoverlay");


	/*
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	 */
	public int setup (String arg, ImagePlus img)
		{
		// System.err.println("arg="+arg);

		this.img = img;
		if (img == null) return DONE;

		// GET VARIOUS MEASUREMENT PARAMETERS FROM PREFERENCES

		getMeasurementPrefs ();

		// GET OVERLAY CANVAS

		canvas = img.getCanvas();
		ocanvas = null;
		if (starOverlay || skyOverlay || valueOverlay)
			{
			ocanvas = OverlayCanvas.getOverlayCanvas (img); 
			canvas = ocanvas;
			}

		stackSize=img.getStackSize();
		slice = img.getCurrentSlice();

		if (stackSize <= 1)
			filename = img.getTitle();	// getShortTitle()?
		else	{
			filename = img.getImageStack().getSliceLabel(slice); // getShortSliceLabel()?
			if (filename == null)
				filename = img.getTitle();
			}

		// GET VARIOUS MEASUREMENT PARAMETERS FROM PREFERENCES

		getMeasurementPrefs ();

		// HANDY FORMATING

		g = new GFormat("4.3");

		// OUTPUT MEASUREMENT TABLE

		table = null;

		// CALIBRATION OBJECT FOR TRANSFORMING INTENSITIES IF NECESSARY

		Calibration cal = img.getCalibration();
		if (cal != null && cal.calibrated())
			isCalibrated = true;
		else
			isCalibrated = false;

		// PREVENT UNTIMELY GARBAGE-COLLECTION

		IJ.register(Aperture_.class);

		// SETUP COMPLETED

		return DOES_ALL+NO_UNDO+NO_CHANGES;
		}

	/**
	 * Measures centroid and aperture brightness of object, stores results in a MeasurementTable, and exits.
	 */
	public void run (ImageProcessor ip)
		{
		imp = ip;							// NOTE IMAGE PROCESSOR FOR LATER USE

		// if (IJ.escapePressed()) { shutDown(); return; }
		getCrudeCenter();
		if (measureAperture())
			storeResults();
		// shutDown();
		}

	/**
	 * Gets crude estimate of object position from ROI created by mouse click.
	 */
	protected void getCrudeCenter()
		{
		rct = imp.getRoi();
		xCenter=(double)rct.x+0.5*(double)rct.width +Centroid.PIXELCENTER;
		yCenter=(double)rct.y+0.5*(double)rct.height+Centroid.PIXELCENTER;
		if (debug) IJ.log("Aperture_.getCrudeCenter rct="+rct.x+","+rct.y+","+rct.width+","+rct.height+" => "+xCenter+","+yCenter);
		}

	/**
	 * Adjusts ROI to current Aperture_ size.
	 */
	protected void adjustRoi ()
		{
		Rectangle rect = Aperture_.a2rect (xCenter,yCenter,radius);
		imp.setRoi (rect.x, rect.y, rect.width, rect.height);
		IJ.makeOval (rect.x, rect.y, rect.width, rect.height);
		if (debug) IJ.log("Aperture_.adjustRoi: a2rect("+xCenter+","+yCenter+","+radius+
				")=("+rect.x+","+rect.y+","+rect.width+","+rect.height+")");
		}

	/**
	 * Allows multi-aperture to utilize variable size apertures
	 */
	protected void setVariableAperture(boolean useVar, double vrad)
		{
		useVariableAp = useVar;
		vradius = vrad;
		vrBack1 = rBack1 + vradius - radius;
		vrBack2 = rBack2 + vradius - radius;
		}

	protected void setVariableAperture(boolean useVar)
		{
		useVariableAp = useVar;
		}


	/**
	 * Finishes one-shot measurement by making the default tool a rectangular ROI.
	 */
	protected void shutDown()
		{
		IJ.setTool(0);
		showApertureStatus ();
		img.unlock();
		}

	/**
	 * Performs exact measurement of object position and integrated brightness.
	 */
	protected boolean measureAperture ()
		{
		if (!adjustAperture()) return false;

		// GET FITS HEADER AND WCS

		String[] hdr = FitsJ.getHeader (img);
		if (hdr != null)
			{
			isFITS = true;
			wcs = new WCS(hdr);
			if (!darkKeyword.trim().equals(""))
				{
				double dark = ccdDark;
				try	{
					dark = FitsJ.findDoubleValue(darkKeyword,hdr);
					}
				catch (NumberFormatException e)
					{
					dark = ccdDark;
					}
				ccdDark = dark;
				}
			}

		// DO APERTURE PHOTOMETRY (NOT SENSITIVE TO PLANAR BACKGROUNDS!)

		photom = new Photometer (img.getCalibration());
		photom.setSourceApertureRadius (radius);
		photom.setBackgroundApertureRadii (rBack1,rBack2);
		photom.setCCD (ccdGain, ccdNoise, ccdDark);
		photom.setRemoveBackStars(removeBackStars);

		photom.measure (imp,xCenter,yCenter);

		back = photom.backgroundBrightness();
		source = photom.sourceBrightness();
		serror = photom.sourceError();

		// GET MJD

		if (isFITS && showTimes)
			{
			mjd = FitsJ.getMeanMJD (hdr);
			if (Double.isNaN(mjd))
				mjd = FitsJ.getMJD (hdr);		// FITSUtilities.getMJD (img);
			}

		// GET FITS KEYWORD VALUES

		if (isFITS && showFits)
			{
			if (fitsKeywords != null && !fitsKeywords.equals(""))
				{
				String sFits = null;
				String[] sarr = fitsKeywords.split(",");
				fitsVals = new double[sarr.length];
				for (int l=0; l < sarr.length; l++)
					{
					try	{
						fitsVals[l] = FitsJ.findDoubleValue (sarr[l],hdr);
						}
					catch (NumberFormatException e) {}
					}
				}
			}

		// GET RA AND DEC (IN DEGREES) USING WCS

		if (isFITS && showRADEC && wcs.hasRaDec())
			{
			raDec = new double[] { xCenter,yCenter };
			raDec = wcs.pixels2wcs(raDec);
			}

		// SHOW RESULTS IN OVERLAY

		drawOverlay ();

		// SHOW RESULTS IN ImageJ TOOLBAR

		showApertureStatus ();

		if (tempOverlay)
			img.killRoi();
		return true;
		}


	protected boolean adjustAperture()
		{
		imp = img.getProcessor();
		if (stackSize > 1)
			{
			ImageStack stack = img.getImageStack();
			filename = stack.getShortSliceLabel(img.getCurrentSlice());
			}

		// GET MEASURMENT PARAMETERS AGAIN IN CASE THEY HAVE CHANGED

		getMeasurementPrefs ();

		if (useVariableAp)
			{
			radius = vradius;
			rBack1 = vrBack1;
			rBack2 = vrBack2;
			}

		// ADJUST ROI TO CORRECT SIZE

		adjustRoi();

		// GET CENTROID OBJECT FOR MEASURING

		center = new Centroid(backIsPlane);
		center.setPositioning (reposition);
		center.setPosition (xCenter,yCenter);
		center.forgiving = forgiving;

		boolean ok;
		if (reposition)				// FIND BETTER POSITION
			{
			if (debug) IJ.log("Aperture_ : repositioning using current ROI ...");
			ok = center.measureROI (imp);	// USE PRESENT ROI
			}
		else	{				// USE PRESENT POSITION
			if (debug) IJ.log("Aperture_ : use position "+xCenter+","+yCenter+","+radius);
			ok = center.measureXYR (imp, xCenter,yCenter,radius);
			}
		if (!ok) return false;

		xCenter = center.x();
		yCenter = center.y();
		xWidth = center.width();
		yWidth = center.height();
		angle = center.orientation();
		if (angle < 0.0) angle += 360.0;
		round = center.roundness();
		variance = center.variance();

		if (reposition) centerROI();

		count++;
		return true;
		}


	/**
	 * Shows results in the image overlay channel.
	 */
	protected void drawOverlay ()
		{
		if (ocanvas != null && clearOverlay)
			ocanvas.clearRois();
		if (starOverlay || skyOverlay || valueOverlay)
			{
			addApertureRoi ();
			canvas.repaint();
			}
		}

	/**
	 * Adds an OvalRoi to the overlay.
	 */
	protected void addOvalRoi (double x, double y, double r)
		{
		Rectangle rect = Aperture_.a2rect (x,y,r);
		OvalRoi roi = new OvalRoi (rect.x,rect.y,rect.width,rect.height);
		if (debug) IJ.log("Aperture_.addOvalRoi: a2rect("+x+","+y+","+r+
				")=("+rect.x+","+rect.y+","+rect.width+","+rect.height+")");
		roi.setImage (img);
		if (starOverlay || skyOverlay || valueOverlay)
			ocanvas.add (roi);
		}

	/**
	 * Adds an ApertureRoi to the overlay
	 */
	protected void addApertureRoi ()
		{
		if (starOverlay || skyOverlay || valueOverlay)
			{
			ApertureRoi roi = new ApertureRoi (xCenter,yCenter,radius,rBack1,rBack2,source);
			roi.setAppearance (starOverlay,skyOverlay,valueOverlay,g.format(source));
			roi.setImage (img);
			ocanvas.add (roi);
			}
		}

	/**
	 * Adds text to the overlay.
	 */
	protected void addStringRoi (double x, double y, String text)
		{
		int xc = (int)(x+0.5-Centroid.PIXELCENTER);
		int yc = (int)(y+0.5-Centroid.PIXELCENTER);
		StringRoi roi = new StringRoi (xc,yc,text);
		roi.setImage (img);
		if (starOverlay || skyOverlay || valueOverlay)
			ocanvas.add (roi);
		}

	/**
	 * Shows status of aperture measurement in the ImageJ toolbar.
	 */
	void showApertureStatus ()
		{
		if (wcs != null && showRADEC && wcs.hasRaDec() && raDec != null)
			IJ.showStatus (""+slice+": R.A.="+g.format(raDec[0]/15.0)+", Dec.="+g.format(raDec[1])+", src="
						+g.format(photom.sourceBrightness()));
		else
			IJ.showStatus (""+slice+": x="+g.format(xCenter)+", y="+g.format(yCenter)+", src="
						+g.format(photom.sourceBrightness()));
		}

	/**
	 * Sets a ROI with the correct radius at the aperture position.
	 */
	protected void centerROI()
		{
		Rectangle rect = Aperture_.a2rect (xCenter,yCenter,radius);
		imp.setRoi (rect.x, rect.y, rect.width, rect.height);
		IJ.makeOval (rect.x, rect.y, rect.width, rect.height);
		if (debug) IJ.log("Aperture_.centerROI: a2rect("+xCenter+","+yCenter+","+radius+
				")=("+rect.x+","+rect.y+","+rect.width+","+rect.height+")");
		}

	/**
	 * Displays the centroiding & photometry results in the table.
	 */
	protected void storeResults ()
		{
		// IF PROBLEMS WITH TABLE, FORGET IT

		if (! checkResultsTable()) return;

		// CREATE ROW FOR NEXT ENTRY

		table.incrementCounter();
		table.addLabel (AP_IMAGE,filename);

		// NOTE SLICE

		if (stackSize == 1)
			table.addValue (AP_SLICE, 0, 0);
		else
			table.addValue (AP_SLICE, slice, 0);

		// NOTE VALUES IN NEW TABLE ROW

		if (showPosition)
			{
			table.addValue (AP_XCENTER, xCenter, 6);
			table.addValue (AP_YCENTER, yCenter, 6);
			}
		if (showPhotometry)
			{
			table.addValue (AP_SOURCE, source, 6);
			if (showPeak)
				table.addValue (AP_PEAK, photom.peakBrightness(), 6);
			if (showSaturationWarning && photom.peakBrightness() > saturationWarningLevel)
				table.addValue (AP_WARNING, photom.peakBrightness(), 6);
			if (showErrors)
				table.addValue (AP_SOURCE_ERROR, serror, 6);
			if (showSNR)
				table.addValue (AP_SOURCE_SNR, source/serror, 6);
			table.addValue (AP_BACK, back, 6);
			if (showRadii)
				{
				table.addValue (AP_RSOURCE, radius, 6);
				table.addValue (AP_RBACK1, rBack1, 6); 
				table.addValue (AP_RBACK2, rBack2, 6);
				}
			}
		if (showTimes && !Double.isNaN(mjd))
			table.addValue (AP_MJD, mjd, 6);
		if (showRADEC && wcs.hasRaDec() && raDec != null)
			{
			table.addValue (AP_RA, raDec[0]/15.0, 6);
			table.addValue (AP_DEC, raDec[1], 6);
			}
		if (showFits && fitsVals != null)
			{
			String[] sarr = fitsKeywords.split(",");
			for (int l=0; l < fitsVals.length; l++)
				{
				if (fitsVals[l] != Double.NaN)
					table.addValue (sarr[l], fitsVals[l], 6);
				}
			}
		if (showWidths)
			{
			table.addValue (AP_XWIDTH, xWidth, 6);
			table.addValue (AP_YWIDTH, yWidth, 6);
			}
		if (showMeanWidth)
			table.addValue (AP_MEANWIDTH, 0.5*(xWidth+yWidth), 6);
		if (showAngle)
			table.addValue (AP_ANGLE, angle, 6);
		if (showRoundness)
			table.addValue (AP_ROUNDNESS, round, 6);
		if (showVariance)
			table.addValue (AP_VARIANCE, variance, 6);
		if (showRaw)
			{
			table.addValue (AP_RSOURCE, photom.getApertureRadius(0), 6);
			table.addValue (AP_RBACK1,   photom.getApertureRadius(1), 6);
			table.addValue (AP_RBACK2,   photom.getApertureRadius(2), 6);
			if (isCalibrated)
				{
				table.addValue (AP_RAWSOURCE, photom.rawSourceBrightness(), 6);
				table.addValue (AP_RAWBACK,     photom.rawBackgroundBrightness(), 6);
				}
			}

		// SHOW NEW ROW

		table.show("Measurements");
		}

	/**
	 * Identifies the results table, creating one if necessary.  If the results table
	 * was previously in a different format, it erases the present contents. Bug or feature?
	 */
	protected boolean checkResultsTable()
		{
		// ALREADY HAVE TABLE
		if (table != null) return true;

		if (oneTable)
			table = MeasurementTable.getTable (null);
		else
			table = MeasurementTable.getTable (filename);

		if (table == null)
			{
			IJ.error ("Unable to open measurement table.");
			return false;
			}


		// CHECK TO SEE IF Aperture_ ENTRIES ALREADY THERE

		int i=0;
		if (table.getColumnIndex(AP_SLICE) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_SLICE);
		if (showPosition)
			{
			if (table.getColumnIndex(AP_XCENTER) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_XCENTER);
			if (table.getColumnIndex(AP_YCENTER) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_YCENTER);
			}
		if (showPhotometry)
			{
			if (table.getColumnIndex(AP_SOURCE) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_SOURCE);
			if (showPeak && table.getColumnIndex(AP_PEAK) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_PEAK);
			if (showSaturationWarning && table.getColumnIndex(AP_WARNING) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_WARNING); 
			if (showErrors && table.getColumnIndex(AP_SOURCE_ERROR) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_SOURCE_ERROR);
			if (showSNR && table.getColumnIndex(AP_SOURCE_SNR) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_SOURCE_SNR);
			if (table.getColumnIndex(AP_BACK) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_BACK);
			}
		if (showTimes && table.getColumnIndex(AP_MJD) == ResultsTable.COLUMN_NOT_FOUND)
			{
			i=table.getFreeColumn (AP_MJD);
			}
		if (showFits && fitsKeywords != null)
			{
			String[] sarr = fitsKeywords.split(",");
			for (int l=0; l < sarr.length; l++)
				{
				if (!sarr[l].equals("") &&
						table.getColumnIndex(sarr[l]) == ResultsTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (sarr[l]);
				}
			}
		if (showRADEC)
			{
			if (table.getColumnIndex(AP_RA) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RA);
			if (table.getColumnIndex(AP_DEC) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_DEC);
			}
		if (showWidths)
			{
			if (table.getColumnIndex(AP_XWIDTH) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_XWIDTH);
			if (table.getColumnIndex(AP_YWIDTH) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_YWIDTH);
			}
		if (showMeanWidth && table.getColumnIndex(AP_MEANWIDTH) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_MEANWIDTH);
		if (showAngle && table.getColumnIndex(AP_ANGLE) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_ANGLE);
		if (showRoundness && table.getColumnIndex(AP_ROUNDNESS) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_ROUNDNESS);
		if (showVariance && table.getColumnIndex(AP_VARIANCE) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_ROUNDNESS);
		if (showRaw)
			{
			if (table.getColumnIndex(AP_RSOURCE) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RSOURCE);
			if (table.getColumnIndex(AP_RBACK1) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RBACK1);
			if (table.getColumnIndex(AP_RBACK2) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RBACK2);
			if (isCalibrated)
				{
				if (table.getColumnIndex(AP_RAWSOURCE) == ResultsTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (AP_RAWSOURCE);
				if (table.getColumnIndex(AP_RAWBACK) == ResultsTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (AP_RAWBACK);
				}
			}

		table.show("Measurements");
		return true;
		}

	/**
	 * Gets all the aperture measurement parameters needed from the preferences.
	 */
	protected void getMeasurementPrefs ()
		{
		debug = Prefs.get ("astroj.debug",false);

		radius = Prefs.get (AP_PREFS_RADIUS,11.0);
		rBack1 = Prefs.get (AP_PREFS_RBACK1,radius+3.0);
		rBack2 = Prefs.get (AP_PREFS_RBACK2,radius+8.0);

		oneTable = Prefs.get (AP_PREFS_ONETABLE, false);

		// THESE ARE IN MultiAperture_ !!!
		// wideTable = Prefs.get (AP_PREFS_WIDETABLE, true);
		// follow = Prefs.get (AP_PREFS_FOLLOW, false);
		// showRatio = Prefs.get (AP_PREFS_SHOWRATIO, true);
		// nAperturesDefault = (int) Prefs.get (AP_PREFS_NAPERTURESDEFAULT, nAperturesDefault);
		// showRatioError = Prefs.get (AP_PREFS_SHOWRATIOERROR, showRatioError);
		// showRatioSNR = Prefs.get (AP_PREFS_SHOWRATIOSNR, showRatioSNR);

		backIsPlane = Prefs.get (AP_PREFS_BACKPLANE, true);
		reposition = Prefs.get (AP_PREFS_REPOSITION, true);
		forgiving = Prefs.get (AP_PREFS_FORGIVING, false);
		retry = Prefs.get (AP_PREFS_RETRY, false);
		removeBackStars = Prefs.get(AP_PREFS_REMOVEBACKSTARS, true);
		
		showFits = Prefs.get (AP_PREFS_SHOWFITS, showFits);
		fitsKeywords = Prefs.get (AP_PREFS_FITSKEYWORDS, fitsKeywords);

		ccdGain = Prefs.get (AP_PREFS_CCDGAIN, ccdGain);
		ccdNoise = Prefs.get (AP_PREFS_CCDNOISE, ccdNoise);
		ccdDark = Prefs.get (AP_PREFS_CCDDARK, ccdDark);
		darkKeyword = Prefs.get (AP_PREFS_DARKKEYWORD, darkKeyword);

		showPosition = Prefs.get (AP_PREFS_SHOWPOSITION, showPosition);
		showPhotometry = Prefs.get (AP_PREFS_SHOWPHOTOMETRY, showPhotometry);

		showPeak = Prefs.get (AP_PREFS_SHOWPEAK, showPeak);
		showSaturationWarning = Prefs.get (AP_PREFS_SHOWSATWARNING, showSaturationWarning);
		saturationWarningLevel = Prefs.get (AP_PREFS_SATWARNLEVEL, saturationWarningLevel);
		showWidths = Prefs.get (AP_PREFS_SHOWWIDTHS, showWidths);
		showRadii = Prefs.get (AP_PREFS_SHOWRADII, showRadii);
		showTimes = Prefs.get (AP_PREFS_SHOWTIMES, showTimes);
		showRaw = Prefs.get (AP_PREFS_SHOWRAW, showRaw);
		showMeanWidth = Prefs.get (AP_PREFS_SHOWMEANWIDTH, showMeanWidth);
		showAngle = Prefs.get (AP_PREFS_SHOWANGLE, showAngle);
		showRoundness = Prefs.get (AP_PREFS_SHOWROUNDNESS, showRoundness);
		showVariance = Prefs.get (AP_PREFS_SHOWVARIANCE, showVariance);
		showErrors = Prefs.get (AP_PREFS_SHOWERRORS, showErrors);
		showSNR = Prefs.get (AP_PREFS_SHOWSNR, showSNR);
		showRADEC = Prefs.get (AP_PREFS_SHOWRADEC, showRADEC);

		starOverlay = Prefs.get (AP_PREFS_STAROVERLAY, starOverlay);
		skyOverlay = Prefs.get (AP_PREFS_SKYOVERLAY, skyOverlay);
		valueOverlay = Prefs.get (AP_PREFS_VALUEOVERLAY, valueOverlay);
		tempOverlay = Prefs.get (AP_PREFS_TEMPOVERLAY, tempOverlay);
		clearOverlay = Prefs.get (AP_PREFS_CLEAROVERLAY, clearOverlay);
		}

	/**
	 * Help routine to convert real pixel position + aperture radius into
	 * centered pixel rectangle.
	 */
	public static Rectangle a2rect (double x, double y, double r)
		{
		Rectangle rect = new Rectangle();

		rect.width = (int)(2.0*r);
		rect.height = rect.width+1-(rect.width%2); // SHOULD BE ODD
		rect.width = rect.height;
		rect.x = (int)(x+0.5-Centroid.PIXELCENTER) - rect.width/2;
		rect.y = (int)(y+0.5-Centroid.PIXELCENTER) - rect.height/2;
		return rect;
		}
	}

/*
			addOvalRoi (xCenter,yCenter,radius);
			if (skyOverlay)
				{
				addOvalRoi (xCenter,yCenter,rBack1);
				addOvalRoi (xCenter,yCenter,rBack2);
				}
			int off = (int)radius;
			if (skyOverlay) off = (int)rBack2;
			addStringRoi (xCenter+off,yCenter,"   "+g.format(source));
*/


