// Set_MultiAperture.java 

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;

import astroj.*;

/**
 * Setup plug-in for MultiAperture_
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2011-Dec-07
 */
public class Set_MultiAperture implements PlugIn
	{
	boolean showOtherPanel = true;

	// STANDARD MultiAperture PREFERENCES
	boolean wideTable = true;
	boolean showRatio = true;
	boolean showRatioError = false;
	boolean singleStep = false;
	boolean previous = false;

	// SPECIAL MultiAperture_ PREFERENCES
	int nAperturesMax = 100;
	boolean showRatioSNR = false;
	boolean showCompTot = false;
	boolean forgiving = false;
	boolean useMacroImage = false;
	String macroImageName = null;
	boolean useVarSizeAp = false;
	double apFWHMFactor = 4.0;
	boolean enableDoubleClicks = false;

	// NON-INTERACTIVE PREFERENCES
	boolean multiApertureRunning = false;
	boolean autoMode = false;

	/**
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	 */
	public void run (String arg)
		{
		showOtherPanel = Prefs.get("setmultiaperture.showother", showOtherPanel);

		// GET STANDARD MultiAperture PREFERENCES
		showRatio      = Prefs.get (MultiAperture_.PREFS_SHOWRATIO, showRatio);
		wideTable      = Prefs.get (MultiAperture_.PREFS_WIDETABLE, wideTable);
		singleStep     = Prefs.get (MultiAperture_.PREFS_SINGLESTEP, singleStep);
		previous       = Prefs.get (MultiAperture_.PREFS_PREVIOUS, previous);

		// GET SPECIAL MultiAperture_ PREFERENCES
		showRatioError = Prefs.get (MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
		showRatioSNR   = Prefs.get (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		showCompTot    = Prefs.get (MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
		forgiving      = Prefs.get (Aperture_.AP_PREFS_FORGIVING, forgiving);
		useMacroImage  = Prefs.get (MultiAperture_.PREFS_USEMACROIMAGE, useMacroImage);
		macroImageName = Prefs.get (MultiAperture_.PREFS_MACROIMAGENAME, macroImageName);
		useVarSizeAp   = Prefs.get (MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
		apFWHMFactor   = Prefs.get (MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
		nAperturesMax  = (int) Prefs.get (MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
	        enableDoubleClicks   = Prefs.get (MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);

		// GET NON-INTERACTIVE PREFERENCES
		multiApertureRunning = Prefs.get (MultiAperture_.PREFS_MULTIAPERTURERUNNING, multiApertureRunning);
		autoMode       = Prefs.get (MultiAperture_.PREFS_AUTOMODE, autoMode);

		// DO DIALOGUES
		mainPanel();
		if (showOtherPanel)
			otherPanel();
		Prefs.set("setmultiaperture.showother", showOtherPanel);

		// SAVE Set_MultiAperture PREFERENCES

		Prefs.set (MultiAperture_.PREFS_WIDETABLE, wideTable);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO, showRatio);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
		Prefs.set (MultiAperture_.PREFS_PREVIOUS, previous);
		Prefs.set (MultiAperture_.PREFS_SINGLESTEP, singleStep);

		// SAVE MultiAperture_ PREFERENCES
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		Prefs.set (Aperture_.AP_PREFS_FORGIVING, forgiving);
		Prefs.set (MultiAperture_.PREFS_USEMACROIMAGE, useMacroImage);
		Prefs.set (MultiAperture_.PREFS_MACROIMAGENAME, macroImageName);
		Prefs.set (MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		Prefs.set (MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
		Prefs.set (MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
		Prefs.set (MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
	        Prefs.set (MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);

		// SAVE NON-INTERACTIVE PREFERENCES
		Prefs.set (MultiAperture_.PREFS_AUTOMODE, autoMode);
		Prefs.set (MultiAperture_.PREFS_MULTIAPERTURERUNNING, multiApertureRunning);
		}

	public void mainPanel ()
		{
		GenericDialog gd = new GenericDialog ("Multi-Aperture Photometry Parameters");
		gd.addCheckbox ("Show other configuration panel",showOtherPanel);
		gd.addMessage (" ");

		gd.addCheckbox ("All measurements from one image on the same line.", wideTable);
		gd.addCheckbox ("Compute ratio of 1st aperture to others (only if on same line).",showRatio);
		gd.addCheckbox ("Show error of the ratio (only if you check \"Compute ratio\" above).",showRatioError);
		gd.addCheckbox ("Use single step mode (right click to exit)",singleStep);
		gd.addCheckbox ("Use previous apertures (1-click for first aperture).",previous);

		if (Centroid.PIXELCENTER < 0.49)
			gd.addMessage ("Warning: following standard astronomical convention,\n"
					+"pixel centers are assumed to be at whole number positions!");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		showOtherPanel = gd.getNextBoolean();

		wideTable      = gd.getNextBoolean();
		showRatio      = gd.getNextBoolean();
		showRatioError = gd.getNextBoolean();
		singleStep     = gd.getNextBoolean();
		previous       = gd.getNextBoolean();
		}

	public void otherPanel ()
		{
		GenericDialog gd = new GenericDialog ("Set Other MultiAperture Photometry Parameters");
		gd.addCheckbox ("Show this panel of other aperture photometry parameters",showOtherPanel);
		gd.addMessage (" ");

		gd.addCheckbox ("Show error of the ratio (only if you check \"Compute ratio\" in Set Aperture).",
					showRatioError);
		gd.addCheckbox ("Show signal-to-noise of multi-aperture ratio (*)", showRatioSNR);
		gd.addCheckbox ("Show total comparison star counts (from apertures 2 to n).",showCompTot);
		gd.addCheckbox ("Halt measurement sequence if error.", !forgiving);
		gd.addCheckbox ("Use macro image", useMacroImage);
		gd.addStringField (".... called",macroImageName);
		gd.addCheckbox ("Vary photometer aperture radii based on FWHM.",useVarSizeAp);
		gd.addNumericField ("FWHM multiplication factor :", apFWHMFactor,2);
		gd.addNumericField ("Maximum number of apertures :",nAperturesMax,0);
		gd.addMessage (" ");
		gd.addCheckbox ("Allow left/right double click fast zoom-in/out (adds slight delay to aperture placement).",
					enableDoubleClicks);

		if (Centroid.PIXELCENTER < 0.49)
			gd.addMessage ("Warning: following standard astronomical convention,\n"
					+"pixel centers are assumed to be at whole number positions!");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		showOtherPanel = gd.getNextBoolean();

		showRatioError = gd.getNextBoolean();
		showRatioSNR = gd.getNextBoolean();
		showCompTot = gd.getNextBoolean();
		forgiving = ! gd.getNextBoolean();
		useMacroImage = gd.getNextBoolean();
		macroImageName = gd.getNextString();
		useVarSizeAp = gd.getNextBoolean();
		apFWHMFactor = gd.getNextNumber();
		nAperturesMax = (int)gd.getNextNumber();
		enableDoubleClicks = gd.getNextBoolean();
		}
	}
