// Set_Aperture.java

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;

import astroj.*;

/**
 * Setup plug-in for Aperture_ which sets the following characteristics:
 *	- aperture radii (object=AP_PREFS_RADIUS and background=AP_PREFS_RBACK1,2)
 *	- output to image-specific table or not (AP_PREFS_ONETABLE)
 *	- show centroid position (AP_PREFS_SHOWPOSITION)
 *	- show photometry (AP_PREFS_SHOWPHOTOMETRY)
 *	- show x- and y-widths (AP_PREFS_SHOWWIDTHS)
 *	- show mean width (AP_PREFS_SHOWMEANWIDTH)
 *	- show aperture radii (AP_PREFS_SHOWRADII)
 *	- show JD times (AP_PREFS_SHOWTIMES)
 *	- show object orientation angle (AP_PREFS_SHOWANGLE)
 *	- show object roundedness (AP_PREFS_SHOWROUNDNESS)
 *	- show object roundedness (AP_PREFS_SHOWVARIANCE)
 *	- show raw photometry (AP_PREFS_SHOWRAW)
 *	- use non-constant background (AP_PREFS_CONST_BACK)
 *	- display aperture radii in overlay (AP_PREFS_SHOWSKYANNULAE)
 *	- set CCD parameters gain [e-/ADU] and RON [e-]
 *	- show photometric errors (AP_PREFS_SHOWERRORS), do/don't reposition aperture (AP_PREFS_REPOSITION)
 *	- show RA, DEC (AP_PREFS_SHOWRADEC)
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2006-Mar-07
 *
 * @version 1.1
 * @date 2006-May-02
 * @changes Added support or planar background.
 *
 * @version 1.2
 * @date 2006-Dec-12
 * @changes Added AP_PREFS_STAROVERLAY & SKYOVERLAY support.
 *
 * @version 1.3
 * @date 2006-Dec-20
 * @changes Added orientation angle and roundness support from Centroid
 *
 * @version 1.4
 * @date 2007-Jan-29
 * @changes Added CCD parameters, showing errors, do/don't  reposition, FITS keyword display.
 *
 * @version 1.5
 * @date 2007-Aug-21
 * @changes Added possibility of making ROIs disappear after use (to prevent future
 *	manipulations from occuring only within the ROI).
 *
 * @ version 1.6
 * @date 2008-Feb-07
 * @changes Added showRADEC.
 *
 * @ version 1.7
 * @date 2008-Jul-03
 * @changes Panel was too long for small screens so packed little-used features into a separate configuration panel.
 *
 * @ version 1.8
 * @date 2009-01-10
 * @changes Support retry option.
 *
 * @version 1.9
 * @date 2010-03-18
 * @author Karen Collins (Univ. Louisville)
 * @changes 
 * 1) Reversed the order of display of the "Aperture Photometry Parameters" * and "Other Photometry
 * 	Parameters" panels. "The Aperture Photometry * Parameters" panel now displays first.
 * 2) Added a check box option to list an aperture peak value column in the measurements table.
 * 3) Added a check box option to list a saturation warning column in the measurements table.
 *	This option displays the peak value that exceeds a defined limit. Otherwise a "0" is listed
 *	in the column. This helps in quickly identifying overly saturated images.
 * 4) Added a numeric entry box for the "Saturation warning level".
 * 5) Added an option to list the error in the "ratio" for multi-aperture differential photometry.
 *	This column is only added to the table if both ratio calculation is selected in multi-aperture
 *	and the "error in the ratio" option is selected here.
 * 6) Added an option to list the multi-aperture ratio SNR. Again, this column only gets added to the
 *	table if both ratio calculation is selected in multi-aperture and the "ratio SNR" option is selected here.
 * 7) Added a "CCD dark current" numeric entry box that is used in the enhanced source error calculation.
 *
 * @version 1.10
 * @dave 2010-03-18
 * @author FVH
 * @changes Slight modification: put some specialized entries in the "Other Photometry Parameters" panel.
 *
 * @version 1.11
 * @date 2010-Nov-24
 * @author Karen Collins (Univ. Louisvill/KY)
 * @changes Added support for removal of stars from background region (>3 sigma from mean)
 */
public class Set_Aperture implements PlugIn
	{
	double radius=17.0;
	double rBack1=19.0;
	double rBack2 = 23.0;
	double oldradius;
	double oldrBack1;
	double oldrBack2;
	double saturationWarningLevel = 55000.0;
	double gain = 1.0;
	double noise = 0.0;
	double dark = 0.0;
	String darkKeyword = new String("");

	boolean finished = false;

	boolean oneTable=true;
	boolean wideTable = true;

	boolean backPlane  = false;
	boolean reposition = true;
	boolean forgiving  = true;			// STOP IF ERROR
	boolean retry      = false;
	boolean removeBackStars = true;			// REMOVE STARS > 3 SIGMA FROM MEAN FROM BACKGROUND CALCULATION

	boolean showFileName = true;        //LIST THE FILENAME AS THE ROW LABEL
	boolean showSliceNumber = true;     // LIST THE SLICE NUMBER
	boolean showPosition=true;			// LIST MEASURED CENTROID POSITION
	boolean showPhotometry=true;			// LIST MEASURED APERTURE BRIGHTNESS MINUS BACKGROUND
	boolean showBack=true;                  //LIST MEASURED SKY BRIGHTNESS PER PIXEL
	boolean showSaturationWarning=true;		// LIST WARNING IF PEAK IS OVER SATURATION LIMIT
	boolean showPeak=true;				// LIST THE PEAK VALUE WITHIN THE SOURCE APERTURE
	boolean showWidths=false;			// LIST MEASURED MOMENT WIDTHS
	boolean showMeanWidth=false;			// LIST MEAN OF MEASURE MOMENT WIDTHS
	boolean showRadii=true;			// LIST APERTURE RADII USED
	boolean showTimes=true;			// LIST JD IF AVAILABLE
	boolean showRaw = false;			// LIST RAW PHOTOMETRY NUMBERS
	boolean showAngle = false;			// LIST ORIENTATION ANGLE
	boolean showRoundness = false;			// LIST ROUNDEDNESS
	boolean showVariance = false;			// LIST VARIANCE
	boolean showErrors = false;			// LIST SOURCE STAR COUNT ERROR
	boolean showSNR = false;			// LIST SOURCE STAR SIGNAL-TO-NOISE

	boolean showFits = true;			// LIST FITS VALUE READ FROM HEADER (e.g. FOCUS)
	String fitsKeywords = "JD_MOBS,HJD_MOBS,ALT_OBJ,AIRMASS";		// KEYWORD

	boolean showRADEC = false;			// LIST RA, DEC IF PRESENT IN WCS

	boolean starOverlay = true;			// SHOW STAR ANNULUS IN OVERLAY
	boolean skyOverlay = true;			// SHOW SKY ANNULAE IN OVERLAY
	boolean valueOverlay = false;			// SHOW VALUE AS LABEL IN OVERLAY
	boolean tempOverlay = true;			// OVERLAY IS TEMPORARY
	boolean clearOverlay = false;			// CLEAR OVERLAY BEFORE MEASUREMENT

	// MultiAperture_ PREFERENCES

	// boolean follow = false;				// TRY TO GUESS THE TRAJECTORY
	boolean showRatio = true;			// LIST THE RATIO OF FIRST TO OTHER STARS
	boolean showRatioError = false;			// LIST THE RATIO ERROR IN MULTI-APERTURE MODE
	boolean showRatioSNR = false;			// LIST THE RATIO SIGNAL TO NOISE RATIO
	boolean autoMode =false;			// SETS MULTI-APERTURE AUTO MODE FOR USE WITH DIRECTORY WATCHER

	// Set_Aperture PREFERENCES

	boolean showOtherPanel = true;
	boolean apertureChanged = false;    // LETS OTHER PLUGINS KNOW THE APERTURE SIZE HAS CHANGED

	/**
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	 */
	public void run (String arg)
		{
		// GET MEASURMENT PARAMETERS: OUTER RADII OF STAR AND SKY APERTURES

		radius     = Prefs.get (Aperture_.AP_PREFS_RADIUS, radius);
		oldradius = radius;
		rBack1     = Prefs.get (Aperture_.AP_PREFS_RBACK1, rBack1);
		oldrBack1 = rBack1;
		rBack2     = Prefs.get (Aperture_.AP_PREFS_RBACK2, rBack2);
		oldrBack2 = rBack2;
		gain       = Prefs.get (Aperture_.AP_PREFS_CCDGAIN, gain);
		noise      = Prefs.get (Aperture_.AP_PREFS_CCDNOISE, noise);
		dark       = Prefs.get (Aperture_.AP_PREFS_CCDDARK, dark);
		darkKeyword= Prefs.get (Aperture_.AP_PREFS_DARKKEYWORD, darkKeyword);
		showPeak   = Prefs.get (Aperture_.AP_PREFS_SHOWPEAK, showPeak);

		oneTable   = Prefs.get (Aperture_.AP_PREFS_ONETABLE, oneTable);

		backPlane  = Prefs.get (Aperture_.AP_PREFS_BACKPLANE, backPlane);
		reposition = Prefs.get (Aperture_.AP_PREFS_REPOSITION, reposition);
		forgiving  = Prefs.get (Aperture_.AP_PREFS_FORGIVING, forgiving);
		retry      = Prefs.get (Aperture_.AP_PREFS_RETRY, retry);
		removeBackStars = Prefs.get (Aperture_.AP_PREFS_REMOVEBACKSTARS, removeBackStars);

		showFileName   = Prefs.get (Aperture_.AP_PREFS_SHOWFILENAME, showFileName);
		showSliceNumber= Prefs.get (Aperture_.AP_PREFS_SHOWSLICENUMBER, showSliceNumber);
		showPosition   = Prefs.get (Aperture_.AP_PREFS_SHOWPOSITION, showPosition);
		showPhotometry = Prefs.get (Aperture_.AP_PREFS_SHOWPHOTOMETRY, showPhotometry);
		showBack       = Prefs.get (Aperture_.AP_PREFS_SHOWBACK, showBack);
		showWidths     = Prefs.get (Aperture_.AP_PREFS_SHOWWIDTHS, showWidths);
		showMeanWidth  = Prefs.get (Aperture_.AP_PREFS_SHOWMEANWIDTH, showMeanWidth);
		showRadii      = Prefs.get (Aperture_.AP_PREFS_SHOWRADII, showRadii);
		showTimes      = Prefs.get (Aperture_.AP_PREFS_SHOWTIMES, showTimes);
		showRaw        = Prefs.get (Aperture_.AP_PREFS_SHOWRAW, showRaw);
		showAngle      = Prefs.get (Aperture_.AP_PREFS_SHOWANGLE, showAngle);
		showRoundness  = Prefs.get (Aperture_.AP_PREFS_SHOWROUNDNESS, showRoundness);
		showVariance   = Prefs.get (Aperture_.AP_PREFS_SHOWVARIANCE, showVariance);
		showErrors     = Prefs.get (Aperture_.AP_PREFS_SHOWERRORS, showErrors);
		showSNR        = Prefs.get (Aperture_.AP_PREFS_SHOWSNR, showSNR);
		showSaturationWarning  = Prefs.get (Aperture_.AP_PREFS_SHOWSATWARNING, showSaturationWarning);
		saturationWarningLevel = Prefs.get (Aperture_.AP_PREFS_SATWARNLEVEL, saturationWarningLevel);

		showFits = Prefs.get (Aperture_.AP_PREFS_SHOWFITS, showFits);
		fitsKeywords = Prefs.get (Aperture_.AP_PREFS_FITSKEYWORDS, fitsKeywords);

		showRADEC = Prefs.get (Aperture_.AP_PREFS_SHOWRADEC, showRADEC);

		starOverlay = Prefs.get (Aperture_.AP_PREFS_STAROVERLAY, starOverlay);
		skyOverlay = Prefs.get (Aperture_.AP_PREFS_SKYOVERLAY, skyOverlay);
		valueOverlay = Prefs.get (Aperture_.AP_PREFS_VALUEOVERLAY, valueOverlay);
		tempOverlay = Prefs.get (Aperture_.AP_PREFS_TEMPOVERLAY, tempOverlay);
		clearOverlay = Prefs.get (Aperture_.AP_PREFS_CLEAROVERLAY, clearOverlay);

		showOtherPanel = Prefs.get("setaperture.showother", showOtherPanel);
		apertureChanged = Prefs.get("setaperture.aperturechanged", apertureChanged);

		// MultiAperture_ PREFERENCES

		// follow         = Prefs.get (MultiAperture_.PREFS_FOLLOW, follow);
		wideTable      = Prefs.get (MultiAperture_.PREFS_WIDETABLE, wideTable);
		showRatio      = Prefs.get (MultiAperture_.PREFS_SHOWRATIO, showRatio);
		showRatioError = Prefs.get (MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
		showRatioSNR   = Prefs.get (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		autoMode       = Prefs.get (MultiAperture_.PREFS_AUTOMODE, autoMode);

		// DO DIALOGUES
		
		mainPanel();
		if (showOtherPanel)
			otherPanel();

		// SAVE Aperture_ PREFERENCES

		Prefs.set (Aperture_.AP_PREFS_RADIUS, radius);
		Prefs.set (Aperture_.AP_PREFS_RBACK1, rBack1);
		Prefs.set (Aperture_.AP_PREFS_RBACK2, rBack2);
		Prefs.set (Aperture_.AP_PREFS_SHOWPEAK, showPeak);
		Prefs.set (Aperture_.AP_PREFS_SATWARNLEVEL, saturationWarningLevel);
		Prefs.set (Aperture_.AP_PREFS_SHOWSATWARNING, showSaturationWarning);
		Prefs.set (Aperture_.AP_PREFS_ONETABLE, oneTable);
		Prefs.set (Aperture_.AP_PREFS_BACKPLANE, backPlane);
		Prefs.set (Aperture_.AP_PREFS_REPOSITION, reposition);
		Prefs.set (Aperture_.AP_PREFS_FORGIVING, forgiving);
		Prefs.set (Aperture_.AP_PREFS_RETRY, retry);
		Prefs.set (Aperture_.AP_PREFS_REMOVEBACKSTARS, removeBackStars);

		Prefs.set (Aperture_.AP_PREFS_SHOWFILENAME, showFileName);
		Prefs.set (Aperture_.AP_PREFS_SHOWSLICENUMBER, showSliceNumber);
		Prefs.set (Aperture_.AP_PREFS_SHOWPOSITION, showPosition);
		Prefs.set (Aperture_.AP_PREFS_SHOWPHOTOMETRY, showPhotometry);
		Prefs.set (Aperture_.AP_PREFS_SHOWBACK, showBack);
		Prefs.set (Aperture_.AP_PREFS_SHOWWIDTHS, showWidths);
		Prefs.set (Aperture_.AP_PREFS_SHOWMEANWIDTH, showMeanWidth);
		Prefs.set (Aperture_.AP_PREFS_SHOWRADII, showRadii);
		Prefs.set (Aperture_.AP_PREFS_SHOWTIMES, showTimes);
		Prefs.set (Aperture_.AP_PREFS_SHOWRAW, showRaw);
		Prefs.set (Aperture_.AP_PREFS_SHOWANGLE, showAngle);
		Prefs.set (Aperture_.AP_PREFS_SHOWROUNDNESS, showRoundness);
		Prefs.set (Aperture_.AP_PREFS_SHOWVARIANCE, showVariance);

		Prefs.set (Aperture_.AP_PREFS_SHOWFITS, showFits);
		Prefs.set (Aperture_.AP_PREFS_FITSKEYWORDS, fitsKeywords);

		Prefs.set (Aperture_.AP_PREFS_SHOWRADEC, showRADEC);

		Prefs.set (Aperture_.AP_PREFS_STAROVERLAY, starOverlay);
		Prefs.set (Aperture_.AP_PREFS_SKYOVERLAY, skyOverlay);
		Prefs.set (Aperture_.AP_PREFS_VALUEOVERLAY, valueOverlay);
		Prefs.set (Aperture_.AP_PREFS_TEMPOVERLAY, tempOverlay);
		Prefs.set (Aperture_.AP_PREFS_CLEAROVERLAY, clearOverlay);

		Prefs.set (Aperture_.AP_PREFS_SHOWERRORS, showErrors);
		Prefs.set (Aperture_.AP_PREFS_SHOWSNR, showSNR);
		Prefs.set (Aperture_.AP_PREFS_CCDGAIN, gain);
		Prefs.set (Aperture_.AP_PREFS_CCDNOISE, noise);
		Prefs.set (Aperture_.AP_PREFS_CCDDARK, dark);
		Prefs.set (Aperture_.AP_PREFS_DARKKEYWORD, darkKeyword);

		// SAVE MultiAperture_ PREFERENCES

		// Prefs.set (MultiAperture_.PREFS_FOLLOW, follow);
		Prefs.set (MultiAperture_.PREFS_WIDETABLE, wideTable);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO, showRatio);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		Prefs.set (MultiAperture_.PREFS_AUTOMODE, autoMode);

		// SAVE Set_Aperture PREFERNCES

		Prefs.set("setaperture.showother", showOtherPanel);
		Prefs.set("setaperture.aperturechanged",apertureChanged);
		}

	public void mainPanel ()
		{
		GenericDialog gd = new GenericDialog ("Aperture Photometry Parameters");

		gd.addNumericField ("Radius of object aperture",radius,2);
		gd.addNumericField ("Inner radius of background annulus",rBack1,2);
		gd.addNumericField ("Outer radius of background annulus",rBack2,2);
		gd.addCheckbox ("Use the clicked position, do not reposition", !reposition);
		gd.addCheckbox ("Show filename", showFileName);
		gd.addCheckbox ("Show slice number", showSliceNumber);
		gd.addCheckbox ("Show centroid position", showPosition);
		gd.addCheckbox ("Show aperture integrated counts", showPhotometry);
		gd.addCheckbox ("Show sky brightness", showBack);
		gd.addCheckbox ("Show Julian Date of image (if available)", showTimes);
		gd.addCheckbox ("Show world coordinates (if available)", showRADEC);
		gd.addCheckbox ("Show mean moment width",showMeanWidth);
		gd.addCheckbox ("Display object aperture in overlay", starOverlay);
		gd.addCheckbox ("Display sky annulus in overlay", skyOverlay);
		gd.addCheckbox ("Display value as label in overlay",valueOverlay);
		gd.addCheckbox ("Clear aperture display after use", tempOverlay);
		gd.addCheckbox ("Clear aperture overlay before use", clearOverlay);
		gd.addCheckbox ("Show other configuration panel",showOtherPanel);

		if (Centroid.PIXELCENTER < 0.49)
			gd.addMessage ("Warning: following standard astronomical convention,\n"
						+"pixel centers are assumed to be at whole number positions!");
		gd.showDialog();
		if (gd.wasCanceled()) return;

		radius = gd.getNextNumber();
		if (oldradius != radius)
			apertureChanged = true;
		rBack1 = gd.getNextNumber();
		if (oldrBack1 != rBack1)
			apertureChanged = true;
		rBack2 = gd.getNextNumber();
		if (oldrBack2 != rBack2)
			apertureChanged = true;
		reposition = ! gd.getNextBoolean();
		showFileName = gd.getNextBoolean();
		showSliceNumber = gd.getNextBoolean();
		showPosition = gd.getNextBoolean();
		showPhotometry = gd.getNextBoolean();
		showBack = gd.getNextBoolean();
		showTimes = gd.getNextBoolean();
		showRADEC = gd.getNextBoolean();
		showMeanWidth = gd.getNextBoolean();
		starOverlay = gd.getNextBoolean();
		skyOverlay = gd.getNextBoolean();
		valueOverlay = gd.getNextBoolean();
		tempOverlay = gd.getNextBoolean();
		clearOverlay = gd.getNextBoolean();
		showOtherPanel = gd.getNextBoolean();
		}

	public void otherPanel ()
		{
		GenericDialog gd = new GenericDialog ("Set Other Aperture Photometry Parameters");

		gd.addCheckbox ("Show this panel of other aperture photometry parameters",showOtherPanel);
		gd.addMessage (" ");
		gd.addCheckbox ("List photometric errors (*)", showErrors);
		gd.addCheckbox ("List photometric signal-to-noise (*)", showSNR);
		gd.addCheckbox ("List error of multi-aperture ratio (*)", showRatioError);
		gd.addCheckbox ("List signal-to-noise of multi-aperture ratio (*)", showRatioSNR);
		gd.addNumericField ("CCD gain [e-/count]", gain, 2);
		gd.addNumericField ("CCD readout noise [e-]", noise, 2);
		gd.addNumericField ("CCD dark current [e-]", dark, 2);
		gd.addStringField ("FITS keyword for dark current [e-]", darkKeyword);
		gd.addMessage ("(*needs gain, readout noise, and dark current info above)");
		gd.addMessage (" ");
		gd.addCheckbox ("List moment x- and y-widths", showWidths);
		gd.addCheckbox ("List aperture radii", showRadii);
		gd.addCheckbox ("List raw numbers (e.g. if intensity-calibrated)", showRaw);
		gd.addCheckbox ("List orientation angle", showAngle);
		gd.addCheckbox ("List roundness (=0 if round, =1 if a line)", showRoundness);
		gd.addCheckbox ("List variance (square of std. dev.)", showVariance);
		gd.addCheckbox ("List aperture peak value", showPeak);
		gd.addCheckbox ("List a saturation warning ...", showSaturationWarning);
		gd.addNumericField ("    .... for levels higher than", saturationWarningLevel,0);
		gd.addCheckbox ("List the decimal values for the FITS keywords called...", showFits);
		gd.addStringField ("comma-separated keywords:",fitsKeywords,20);
		gd.addCheckbox ("Assume background is a plane",backPlane);
		gd.addCheckbox ("Remove stars from background region (>2 sigma from mean)", removeBackStars);
		gd.addCheckbox ("Measurement results in image-specific tables",!oneTable);
		gd.addCheckbox ("Halt measurement sequence if error.", !forgiving);
		gd.addCheckbox ("Retry bad measurement by using larger search aperture.", retry);

		if (Centroid.PIXELCENTER < 0.49)
			gd.addMessage ("Warning: following standard astronomical convention,\n"
					+"pixel centers are assumed to be at whole number positions!");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		showOtherPanel = gd.getNextBoolean();

		showErrors = gd.getNextBoolean();
		showSNR = gd.getNextBoolean();
		showRatioError = gd.getNextBoolean();
		showRatioSNR = gd.getNextBoolean();
		gain = gd.getNextNumber();
		noise = gd.getNextNumber();
		dark = gd.getNextNumber();
		darkKeyword = gd.getNextString();
		showWidths = gd.getNextBoolean();
		showRadii = gd.getNextBoolean();
		showRaw = gd.getNextBoolean();
		showAngle = gd.getNextBoolean();
		showRoundness = gd.getNextBoolean();
		showVariance = gd.getNextBoolean();
		showPeak = gd.getNextBoolean();
		showSaturationWarning = gd.getNextBoolean();
		saturationWarningLevel = gd.getNextNumber();
		showFits = gd.getNextBoolean();
		fitsKeywords = gd.getNextString();
		backPlane = gd.getNextBoolean();
		removeBackStars = gd.getNextBoolean();
		oneTable = ! gd.getNextBoolean();
		forgiving = ! gd.getNextBoolean();
		retry = gd.getNextBoolean();
		}
	}
