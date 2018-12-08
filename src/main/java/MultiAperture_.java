// MultiAperture_.java

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.Toolbar.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.Canvas.*;
import java.util.*;

import astroj.*;


/**
 * Based on Aperture_.java, but with pre-selection of multiple apertures and processing of stacks.
 * 
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2006-Feb-15
 *
 * @version 1.1
 * @date 2006-Nov-29
 * @changes Slight re-organization so that it's easier to use MultiAperture_ as a parent class, e.g. for Stack_Aligner_.
 *
 * @version 1.2
 * @date 2006-Dec-6
 * @changes Corrected problem with non-instantiated xOld for the case of images and not stacks;
 *	added thread to display current ROIs.
 *
 * @version 1.3
 * @date 2006-Dec-11
 * @changes Complicated thread solution replaced with simple overlay solution inherited from Aperture_
 *
 * @version 1.4
 * @date 2007-Mar-14
 * @changes Added <ESC> to stop processing of large stacks which have gone awry.
 *
 * @version 1.5
 * @date 2007-May-01
 * @changes Added output of ratio
 *
 * @version 1.6
 * @date 2010-Mar-18
 * @author Karen Collins (Univ. Louisville/KY)
 * @changes
 * 1) On my machine, the number of apertures and the 4 check box option values do not get stored between runs.
 *	I don't know if this is a problem with my computing environment, or if it is a problem in general.
 *	To make it work on my end, I had to implement the MultiAperture_ preferences retrieval from the Aperture_
 *	preferences management code. I was able to implement the save (set) from MultiAperture_ without problems.
 *	I could never work out how to do both from MultiAperture_, so my solution is probably not ideal, but it seems to work now.
 * 2) Implemented the Ratio Error functionality. The code I am currently using is:
 *		ratio*Math.sqrt(targetVariance/(target*target)+
 *		othersVariance/(others*others))        {equation 2}
 *	where
 *		ratio = target counts/total of all comparison counts  (all adjusted for sky background)
 *		targetVariance = (source error)^2 from the 1st of N apertures per image (as calculated in Photometer above)
 *		target = total source counts less sky background from the 1st of N apertures
 *		othersVariance = (ap#2 error)^2 + (ap#3 error)^2 + ...  + (ap#N error)^2
 *		others = total source counts less sky background from the 2nd through Nth apertures
 * 3) Implemented the Ratio SNR functionality. It simply reports (target counts)/{equation 2}, which is accomplished by
 *	not multiplying by "ratio".
 * 4) Measurements table code to support the ratio error and ratioSNR reporting.
 *
 * @version 1.7
 * @date 2011-Mar-29
 * @author F. Hessman (Goettingen)
 * @changes added dialog() to make it easier to sub-class (e.g. Stack_Aligner)
 *
 * @version 1.8
 * @date 2011-Dec-07
 * @author F. Hessman
 * @changes Corrected for definition of centers of pixels:  always add the Centroid.PIXELCENTER to the raw image coordinates!
 *
 * @version 1.9
 * @date 2012-Oct-06
 * @author F. Hessman
 * @changes Added measurements from current overlay ROIs
 *
 * @version 1.10
 * @date 2013-Oct-20
 * @author F. Hessman
 * @changes Added edit aperture option.
 */
public class MultiAperture_ extends Aperture_ implements MouseListener, KeyListener
	{
	boolean cancelled=false;
	boolean verbose=true;
	boolean blocked=false;
	boolean previous=false;
	boolean doStack=false;
	boolean processingStack=false;
	int firstSlice=1;
	int lastSlice=1;

	int astronomyToolId = 0;
	int currentToolId = 0;
	Toolbar toolbar;

	double[] xOld;
	double[] yOld;

	String infoMessage = new String("");

	protected int ngot=0;
	protected int aperture=0;
	protected int nAperturesMax=200;
	protected int nApertures=2;
	protected int nAperturesStored=0;
	protected int startDragScreenX;
	protected int startDragScreenY;

	protected double[] xPos;	// ImageJ PIXELS
	protected double[] yPos;
	protected double[] aPos;	// WCS coords
	protected double[] dPos;

	double others = 0.0;		// SUM OF OTHER APERTURES
	double peak = 0.0;		// MAX PIXEL VALUE IN APERTURE
	double target = 0.0;
	double targetVariance = 0.0;
	double othersVariance = 0.0;

	double apFWHMFactor = 2.0;
	double meanFWHM = 0.0;

	double ratio = 0.0;		// FIRST APERTURE
	double ratioError = 0.0;
	double ratioSNR = 0.0;
	public static String RATIO = new String ("ratio1");
	public static String OTHERS = new String ("tot_comp_cnts");
	public static String RATIOERROR = new String ("ratio1_error");
	public static String RATIOSNR = new String ("ratio1_SNR");

	protected boolean autoMode = false;
	protected boolean singleStep = false;
	protected boolean simulatedLeftClick = false;
	protected boolean aperturesInitialized = false;
	protected boolean enableDoubleClicks = false;
	protected boolean multiApertureRunning = false;

	protected boolean useVarSizeAp= false;
	protected boolean wideTable=true;

	protected boolean showRatio=false;
	protected boolean showCompTot=true;
	protected boolean showRatioError=false;
	protected boolean showRatioSNR=false;
	protected boolean useMacroImage=false;

	protected String macroImageName=null;
	ImagePlus openImage;

	protected boolean frameAdvance=false;

	String xOldApertures,yOldApertures;

	MouseEvent dummyClick;
	MouseEvent ee;

	protected int screenX;
	protected int screenY;
	protected int modis;

	TimerTask stackTask = null;
	java.util.Timer stackTaskTimer = null;

	boolean doubleClick = false;
	TimerTask doubleClickTask = null;
	java.util.Timer doubleClickTaskTimer = null;
	
	protected static String PREFS_AUTOMODE        = new String ("multiaperture.automode");  //0 click - for use with macros
	protected static String PREFS_FINISHED        = new String ("multiaperture.finished");  //signals finished to macros
	protected static String PREFS_PREVIOUS        = new String ("multiaperture.previous");
	protected static String PREFS_SINGLESTEP      = new String ("multiaperture.singlestep");
	protected static String PREFS_USEVARSIZEAP    = new String ("multiaperture.usevarsizeap");
	protected static String PREFS_APFWHMFACTOR    = new String ("multiaperture.apfwhmfactor");
	protected static String PREFS_WIDETABLE       = new String ("multiaperture.widetable");
	protected static String PREFS_SHOWRATIO       = new String ("multiaperture.showratio");
	protected static String PREFS_SHOWCOMPTOT     = new String ("multiaperture.showcomptot");
	protected static String PREFS_SHOWRATIO_ERROR = new String ("multiaperture.showratioerror");
	protected static String PREFS_SHOWRATIO_SNR   = new String ("multiaperture.showratiosnr");
	protected static String PREFS_NAPERTURESMAX   = new String ("multiaperture.naperturesmax");
	protected static String PREFS_XAPERTURES      = new String ("multiaperture.xapertures");
	protected static String PREFS_YAPERTURES      = new String ("multiaperture.yapertures");
	protected static String PREFS_USEMACROIMAGE   = new String ("multiaperture.useMacroImage");
	protected static String PREFS_MACROIMAGENAME  = new String ("multiaperture.macroImageName");
	protected static String PREFS_ENABLEDOUBLECLICKS  = new String ("multiaperture.enableDoubleClicks");
	protected static String PREFS_MULTIAPERTURERUNNING  = new String ("multiaperture.multiApertureRunning");

	public static double RETRY_RADIUS = 3.0;


	protected boolean editApertures = false;

	/**
	 * Standard ImageJ PluginFilter setup routine.
	 */
	public int setup (String arg, ImagePlus im)
		{
		this.getMeasurementPrefs();
		if (multiApertureRunning)
			{
			if (!IJ.showMessageWithCancel("MultiAperture Conflict",
				"MultiAperture appears to be running already.\n\rPress OK to continue or Cancel to abort the new instance." ))
			return DONE;
			}
		Prefs.set (MultiAperture_.PREFS_MULTIAPERTURERUNNING, true);
		if (useMacroImage)
			{
			openImage = WindowManager.getImage(macroImageName);
			if (openImage != null) im = openImage;
			}
		if (im == null) return DONE;

		// TO MAKE SURE THAT THE NEXT CLICK WILL WORK
		toolbar = Toolbar.getInstance();
		astronomyToolId = toolbar.getToolId("Astronomy_Tool");
		currentToolId = Toolbar.getToolId();
		if (currentToolId != astronomyToolId)
			{
			if (astronomyToolId != -1)
				IJ.setTool(astronomyToolId);
			else
				IJ.setTool(0);
			}
		IJ.register(MultiAperture_.class);
		return super.setup(arg,im);
		}

	/**
	 * Standard ImageJ Plug-in method which runs the plug-in, notes whether the image is a stack,
	 * and registers the routine for mouse clicks.
	 */
	public void run (ImageProcessor ip)
		{
		this.getMeasurementPrefs();
		if (!autoMode)
			{
			String[] apsX = xOldApertures.split(",");
			String[] apsY = yOldApertures.split(",");
			if (apsX.length != apsY.length)
				nAperturesStored = 0;
			else
				nAperturesStored = apsX.length;
			}
		if (useMacroImage)
			{
			openImage = WindowManager.getImage(macroImageName);
			if (!(openImage == null))
				{
				img = openImage;
				imp = openImage.getProcessor();
				}
			}

		// GET HOW MANY APERTURES WILL BE MEASURED WITH WHAT RADII

		if ( !setUpApertures() || nApertures == 0 || !prepare () )
			{
			Prefs.set (MultiAperture_.PREFS_MULTIAPERTURERUNNING, "false");
			img.unlock();
			return;
			}

		// START ESCAPE ABORTION POSSIBILITY
		IJ.resetEscape();

		// REGISTER FOR MOUSE CLICKS IF NOT AUTOMATIC
		if ( !autoMode )
			{
			canvas.addMouseListener(this);
			canvas.addKeyListener(this);
			}

		if (starOverlay || skyOverlay)
			ocanvas.clearRois();
		if (previous)
			{
			infoMessage = "Please select first aperture (right click to finalize) ...";
			IJ.showStatus (infoMessage);
			}
		setApertureColor(Color.green);
		if ( autoMode )
			mouseReleased( dummyClick );
		}

	/**
	 * Get all preferences.
	 */
	protected void getMeasurementPrefs()
		{
		super.getMeasurementPrefs();

		autoMode       = Prefs.get (MultiAperture_.PREFS_AUTOMODE, autoMode);
		useMacroImage  = Prefs.get (MultiAperture_.PREFS_USEMACROIMAGE, useMacroImage);
		macroImageName = Prefs.get (MultiAperture_.PREFS_MACROIMAGENAME, macroImageName);
		previous       = Prefs.get (MultiAperture_.PREFS_PREVIOUS, previous);
		singleStep     = Prefs.get (MultiAperture_.PREFS_SINGLESTEP, singleStep);
		wideTable      = Prefs.get (MultiAperture_.PREFS_WIDETABLE, wideTable);
		showRatio      = Prefs.get (MultiAperture_.PREFS_SHOWRATIO, showRatio);
		showCompTot    = Prefs.get (MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
		showRatioError = Prefs.get (MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
		showRatioSNR   = Prefs.get (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		useVarSizeAp   = Prefs.get (MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
		apFWHMFactor   = Prefs.get (MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
		nAperturesMax  = (int) Prefs.get (MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
		xOldApertures  = Prefs.get (MultiAperture_.PREFS_XAPERTURES,"");
		yOldApertures  = Prefs.get (MultiAperture_.PREFS_YAPERTURES,"");
	        enableDoubleClicks   = Prefs.get (MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);
		multiApertureRunning = Prefs.get (MultiAperture_.PREFS_MULTIAPERTURERUNNING, multiApertureRunning);
		}

	/**
	 * Initializes variables etc.
	 */
	protected boolean prepare ()
		{
		//removed to allow dynamic number of apertures using right click to terminate aperture selections:
		// if (!checkResultsTable ()) return false;

		// LOAD PREVIOUS APERTURES IF DESIRED

		if ( previous || autoMode )
			{
			String[] aps = xOldApertures.split(",");
			nApertures = aps.length;
			xPos = extract(aps);
			aps = yOldApertures.split(",");
			if (nApertures != aps.length)
				{
				IJ.error("The stored apertures are not consistent: "+nApertures+"!="+aps.length);
				return false;
				}
			aperturesInitialized = true;
			yPos = extract(aps);
			}
		else	{
			xPos = new double[nApertures];
			yPos = new double[nApertures];
			}
		if (xPos == null || yPos == null)
			{
			IJ.error("Null aperture arrays???");
			return false;
			}

		img.setSlice(firstSlice);
		imp = img.getProcessor();
		img.killRoi();
		return true;
		}

	/**
	 * Extracts a double array from a string array.
	 */
	protected double[] extract (String[] s)
		{
		double[] arr = new double[s.length];
		try	{
			for (int i=0; i < arr.length; i++)
				arr[i] = Double.parseDouble(s[i]);
			}
		catch (NumberFormatException e)
			{
			arr = null;
			}
		return arr;
		}

	/**
	 * Stops reception of mouse and keyboard clicks.
	 */
	protected void noMoreInput()
		{
		canvas.removeMouseListener(this);
		canvas.removeKeyListener(this);
		}

	/**
	 * Finishes whole process.
	 */
	protected void shutDown()
		{
		noMoreInput();
		super.shutDown();
		cancelled=true;
		processingStack=false;
		stackTask = null;
		stackTaskTimer = null;
		doubleClickTask = null;
		doubleClickTaskTimer = null;
		Prefs.set (MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
		Prefs.set (MultiAperture_.PREFS_PREVIOUS, previous);
		Prefs.set (MultiAperture_.PREFS_SINGLESTEP, singleStep);
		Prefs.set (MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
		Prefs.set (MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
		Prefs.set (MultiAperture_.PREFS_WIDETABLE, wideTable);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO, showRatio);
		Prefs.set (MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		Prefs.set (MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);
		Prefs.set (MultiAperture_.PREFS_MULTIAPERTURERUNNING, false);
		Prefs.set (MultiAperture_.PREFS_FINISHED, "true");
		Prefs.set (MultiAperture_.PREFS_USEMACROIMAGE, "false");
		}

	//
	// MouseListener METHODS
	//

	/**
	 * Main MouseListener method used: process all mouse clicks.
	 */
	public void mouseReleased(MouseEvent e)
		{
		ee = e;
		if (autoMode || !enableDoubleClicks)
			{
			processSingleClick(ee);
			}
		else	{
			if (e.getClickCount() == 1)
				{
				doubleClick = false;
				try	{
					doubleClickTask = new TimerTask ()
						{
						public void run ()
							{
							if (!doubleClick) processSingleClick(ee);
							doubleClickTask = null;
							doubleClickTaskTimer = null;
							}
						};
					doubleClickTaskTimer = new java.util.Timer();
					if ((modis & InputEvent.BUTTON1_MASK) != 0)
						doubleClickTaskTimer.schedule (doubleClickTask, 300);
					else
						doubleClickTaskTimer.schedule (doubleClickTask, 600);
					}
				catch (Exception eee)
					{
					IJ.showMessage ("Error starting double click timer task : "+eee.getMessage());
					}
				}
			else	{
				doubleClick = true;
				return;
				}
			}
		}

	void processSingleClick(MouseEvent e)
		{
		// IJ.log("click count = "+e.getClickCount()+"    doubleClick = "+doubleClick);

		// NORMAL MOUSE CLICK (NO autoMode)
		if (!autoMode)
			{
			screenX = e.getX();
			screenY = e.getY();
			modis = e.getModifiers();
			}

		// Right mouse click finalizes aperture selection or terminates single step mode
		if ((Math.abs(screenX-startDragScreenX) + Math.abs(screenY-startDragScreenY) < 4.0 ) &&
						(modis & InputEvent.BUTTON3_MASK) != 0)
			{
			if (!aperturesInitialized)
				{
				nApertures = ngot;
				simulatedLeftClick = true;
				aperturesInitialized = true;
				}
			else if (singleStep)
				{
				IJ.beep();
				shutDown();
				return;
				}
			}

		// Do nothing unless automode or left mouse is clicked with no modifier keys pressed
		if (autoMode	|| simulatedLeftClick
				|| ((Math.abs(screenX-startDragScreenX) + Math.abs(screenY-startDragScreenY) < 4.0 ) &&
					(modis & InputEvent.BUTTON1_MASK) != 0 && 
					!e.isControlDown() && !e.isAltDown() && !e.isMetaDown() && !e.isShiftDown())
				)
			{
			simulatedLeftClick = false;
			if (ngot > 0) setApertureColor(Color.red);

			// TEST FOR REASONABLE SLICE
			if ( !autoMode && firstSlice > stackSize)
				{
				IJ.beep();
				shutDown();
				return;
				}
			// NEXT SLICE
			else if (frameAdvance)
				{
				firstSlice += 1;
				lastSlice = firstSlice;
				img.setSlice(firstSlice);
				img.updateImage();
				if (starOverlay || skyOverlay)
					{
					ocanvas = OverlayCanvas.getOverlayCanvas (img);
					canvas = ocanvas;
					ocanvas.clearRois();
					}
				imp = img.getProcessor();
				}
			// OR CURRENT SLICE
			else	{
				slice = img.getCurrentSlice();	// INHERITED FROM Aperture_
				}
	
			// INITIAL CENTER FROM MOUSE CLICK OR PREVIOUS APERTURE LOCATION
			if ( autoMode )
				{
				xCenter = xPos[0];
				yCenter = yPos[0];
				}
			else	{
				xCenter = (double)canvas.offScreenX(e.getX())+Centroid.PIXELCENTER;
				yCenter = (double)canvas.offScreenY(e.getY())+Centroid.PIXELCENTER;
				}

			// MEASURE ALL APERTURES
			if (ngot < nApertures && !measureAperture())
				{
				img.unlock();
				return;
				}

			// ADD APERTURE TO LIST OR SHIFT OLD APERTURE POSITIONS
			if (previous || autoMode )
				{
				double dx = xCenter-xPos[0];
				double dy = yCenter-yPos[0];
				for (int i=0; i < nApertures; i++)
					{
					xPos[i] += dx;
					yPos[i] += dy;
					}
				ngot = nApertures;
				aperture = ngot;
				}
			else if (ngot < nApertures)
				addAperture ();

			if (frameAdvance)
				{
				if (firstSlice > stackSize)
				    IJ.showStatus ("Finished processing stack. Click to exit.");
				else
				    IJ.showStatus ("Click to select first aperture (right click to exit).");
				frameAdvance = false;
				}
			else if (singleStep && ngot >= nApertures)
				{
				//PROCESS ONE SLICE AT A TIME WHILE IN SINGLE STEP MODE
				xOld = xPos.clone();
				yOld = yPos.clone();
				processStack();
				frameAdvance = true;
				if (!previous)
					{
					saveNewApertures ();
					previous = true;
					Prefs.set (MultiAperture_.PREFS_PREVIOUS, previous);
					}
				IJ.showStatus ("Click to advance slice (right click to exit).");
				}

			// GOT ALL APERTURES?
			else if (ngot < nApertures)
				{
				infoMessage = "Click to select aperture #"+(ngot+1)+" (<ESC> to abort).";
				IJ.showStatus (infoMessage);
				}
			// PROCESS ALL SLICES WHEN NOT IN SINGLE STEP MODE
			else	{
				if (editApertures)
					{
					reposition=false;
					center.setPositioning (reposition);
					}
				noMoreInput ();
				xOld = xPos.clone();
				yOld = yPos.clone();
				saveNewApertures ();
				if (!singleStep)
					{
					if (stackSize > 1 && doStack)
						{
						IJ.showStatus ("Processing stack...");
						processingStack = true;
						startProcessStack();
						}
					else	{
						IJ.showStatus ("Processing image...");
						processImage();
						}
					}
				if (!processingStack && !autoMode && (firstSlice < lastSlice)) IJ.beep();
				if (!processingStack) shutDown();
				}
			}
		}

        /**
	 * Saves new aperture locations to preferences.
	 */
	protected void saveNewApertures ()
		{
		String xpos = "";
		String ypos = "";
		for (int i=0; i < nApertures; i++)
			{
			if (i == 0)
				{
				xpos += (float)xPos[i];
				ypos += (float)yPos[i];
				}
			else	{
				xpos += ","+(float)xPos[i];
				ypos += ","+(float)yPos[i];
				}
			}
		if (aperturesInitialized)
			{
			Prefs.set (MultiAperture_.PREFS_XAPERTURES, xpos);
			Prefs.set (MultiAperture_.PREFS_YAPERTURES, ypos);
			}
		}

        /**
	 * Adds the aperture parameters to the list of apertures.
	 */
	protected void addAperture ()
		{
		if (editApertures)
			{
			GenericDialog g = new GenericDialog(" Edit aperture #"+ngot+" ");
			g.addNumericField ("x-position :",xCenter,3);
			g.addNumericField ("y-position :",yCenter,3);
			g.showDialog();
			if (g.wasOKed())
				{
				xCenter = g.getNextNumber();
				yCenter = g.getNextNumber();
				}
			}
		xPos[ngot] = xCenter;
		yPos[ngot] = yCenter;
		ngot++;
		aperture = ngot;
		}

	/**
	 * Misc mouse functions.
	 */
	public void mousePressed(MouseEvent e)
		{
		if (!autoMode)
			{
			startDragScreenX = e.getX();
			startDragScreenY = e.getY();
			}
		}
	public void mouseClicked(MouseEvent e) {}
	public void mouseExited(MouseEvent e)
		{
		IJ.showStatus (infoMessage);
		}
	public void mouseEntered(MouseEvent e) {}
	public void keyTyped(KeyEvent e) { }
	public void keyPressed(KeyEvent e)
		{
		if (IJ.escapePressed())
			{
			IJ.log("escape pressed!");
			IJ.beep();
			shutDown();
			}
		}

	/** Handle the key-released event from the image canvas. */
	public void keyReleased(KeyEvent e) { }


	//
	// MultiAperture_ METHODS
	//

	/**
	 * Define the apertures and decide on the sub-stack if appropriate.
	 */
	protected boolean setUpApertures ()
		{
		// CHECK SLICES
		firstSlice= img.getCurrentSlice();
		lastSlice = stackSize;
		if (singleStep)
			lastSlice = firstSlice;
		if (!autoMode)
			{
			GenericDialog gd = dialog();
			gd.showDialog();
			if (!gd.wasOKed())
				{
				Prefs.set (MultiAperture_.PREFS_MULTIAPERTURERUNNING, false);
				cancelled = true;
				img.unlock();
				return false;
				}

			// GET UPDATED STANDARD PARAMETERS FROM REQUIRED DIALOG FIELDS:
			//	nApertures,firstSlice,lastSlice,previous,singleStep,oneTable,wideTable

			// NOTE: ONLY THE GENERIC MultiAperture_ FIELDS BELONG HERE !!!!!!!!!!!!!

			nAperturesMax = (int)gd.getNextNumber();
			nApertures = nAperturesMax;
			if (gd.invalidNumber() || nApertures <= 0)
				{
				IJ.beep();
				IJ.error ("Invalid number of apertures: "+nApertures);
				shutDown();
				return false;
				}
			Prefs.set (MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
			if (stackSize > 1)
				{
				firstSlice=(int)gd.getNextNumber();
				if (gd.invalidNumber() || firstSlice < 1)
					firstSlice=1;
				lastSlice=(int)gd.getNextNumber();
				if (gd.invalidNumber() || lastSlice > stackSize)
					lastSlice= stackSize;
				if (firstSlice != lastSlice)
					{
					if (firstSlice > lastSlice)
					        {
					        int i=firstSlice;
					        firstSlice=lastSlice;
					        lastSlice=i;
					        }
					doStack=true;
					}
		        	}
			else	{
				firstSlice=1;
				lastSlice=1;
				}
			previous = gd.getNextBoolean();
			singleStep = gd.getNextBoolean();
			if (singleStep)
				lastSlice = firstSlice;
			oneTable = !gd.getNextBoolean();
			wideTable = gd.getNextBoolean();

			Prefs.set (MultiAperture_.PREFS_PREVIOUS, previous);
			Prefs.set (MultiAperture_.PREFS_SINGLESTEP, singleStep);
			Prefs.set (MultiAperture_.PREFS_WIDETABLE, wideTable);

			// GET NON-STANDARD PARAMETERS AND CLEAN UP
			return finishFancyDialog (gd);
			}
		return true;
		}


	/**
	 * Process a stack for processing.
	 */
	protected void startProcessStack ()
		{
		try	{
			stackTask = new TimerTask ()
				{
				public void run ()
					{
					processStack();
					stackTask = null;
					stackTaskTimer = null;
					}
				};
			stackTaskTimer = new java.util.Timer();
			stackTaskTimer.schedule (stackTask,0);
			}
		catch (Exception e)
			{
			IJ.showMessage ("Error starting process stack task : "+e.getMessage());
			}
		}


	/**
	 * Perform photometry on each image of selected sub-stack.
	 */
	protected void processStack ()
		{
		verbose=false;
		canvas = img.getCanvas();
		ocanvas = null;

		for (int i=firstSlice; i <= lastSlice; i++)
			{
			slice=i;
			img.setSlice(i);
			img.updateImage();
			if (starOverlay || skyOverlay)
				{
				ocanvas = OverlayCanvas.getOverlayCanvas (img);
				canvas = ocanvas;
				ocanvas.clearRois();
				}
			imp = img.getProcessor();
			processImage();
			if (cancelled || IJ.escapePressed())
				{
				IJ.beep();
				shutDown();
				return;
				}
			}
		if (processingStack)
			{
			IJ.beep();
			shutDown();
			return;
			}
		}

	/**
	 * Perform photometry on each aperture of current image.
	 */
	protected void processImage ()
		{
		double dx = 0.0;		// CHANGE
		double dy = 0.0;

		double src = 0.0;		// MEAN SOURCE BRIGHTNESSES AND BACKGROUNDS
		double bck = 0.0;

		ratio = 0.0;
		ratioError = 0.0;
		ratioSNR = 0.0;
		others = 0.0;
		target = 0.0;
		targetVariance = 0.0;
		othersVariance = 0.0;
		meanFWHM = 0.0;

        	if (useVarSizeAp)
			{
			for (int ap=0;  ap < nApertures; ap++)
				{
				aperture = ap;
				// GET POSITION ESTIMATE
				if (ap == 0) setApertureColor(Color.green);
				else setApertureColor(Color.red);
				xCenter = xPos[ap];
				yCenter = yPos[ap];

				// MEASURE NEW POSITION
				centerROI();
				if (!adjustAperture())
					{
					IJ.beep();
					shutDown();
		    			return;
					}
				meanFWHM += 0.5*(xWidth+yWidth);
				}
			meanFWHM /= (double)nApertures;
			setVariableAperture(true, meanFWHM*apFWHMFactor);
			OverlayCanvas.getOverlayCanvas(img).clearRois();
			}
        
		for (int ap=0;  ap < nApertures; ap++)
			{
			aperture = ap;
			if (ap == 0)
				setApertureColor(Color.green);
			else
				setApertureColor(Color.red);

			// GET POSITION ESTIMATE
			xCenter = xPos[ap];
			yCenter = yPos[ap];

			// MEASURE NEW POSITION
			centerROI();
			if (!measureAperture())
				{
				setVariableAperture(false);
				shutDown();
				return;
				}

			// STORE RESULTS
			if (ap == 0 || !wideTable)
			 	storeResults();
			else
				storeAdditionalResults (ap);

            		// FOLLOW MOTION FROM FRAME TO FRAME
			dx += xCenter-xPos[ap];
			dy += yCenter-yPos[ap];
			xOld[ap] = xPos[ap];
			yOld[ap] = yPos[ap];
			xPos[ap]=xCenter;		// STORE POSITION IN CASE IT DRIFTS WITHIN A STACK
			yPos[ap]=yCenter;

			src += source;
			bck += back;

			if (ap == 0)
				{
				ratio = source;
				target = source;
				targetVariance = serror*serror;
 				}
			else	{
				others += source;
				othersVariance += serror*serror;
				}

			// FOR DAUGHTER CLASSES....
			noteOtherApertureProperty (ap);
			}

		setVariableAperture(false);

		// COMPUTE APERTURE RATIO AND ERRORS AND UPDATE TABLE
		if (showRatio && nApertures > 1)
			{
			ratio /= others;
			table.addValue (RATIO, ratio, 6);
			if (showRatioError)
				table.addValue (RATIOERROR, ratio*Math.sqrt(targetVariance/(target*target)+ othersVariance/(others*others)), 8);
			if (showRatioSNR)
				table.addValue (RATIOSNR, 1/Math.sqrt(targetVariance/(target*target)+ othersVariance/(others*others)), 6);
			}
		if (showCompTot && nApertures > 1)
			table.addValue (OTHERS, others, 6);

		// CALCULATE MEAN SHIFT, BRIGHTNESS, AND BACKGROUND
		xCenter = dx/nApertures;
		yCenter = dy/nApertures;
		source = src/nApertures;
		back = bck/nApertures;

		// UPDATE TABLE
		table.show();
		}

	/**
	 * Notes anything else which might be interesting about an aperture measurement.
	 */
	protected void noteOtherApertureProperty  (int ap)
		{
		}

	/**
	 * Set up extended table format.
	 */
	protected boolean checkResultsTable ()
		{

		if (!super.checkResultsTable ()) return false;
		int i=0;
   
		if (wideTable && nApertures > 1)
			{
			if (showRatio)
				{
				if (table.getColumnIndex(RATIO) == MeasurementTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (RATIO);
				if (showRatioError)
					{
					if (table.getColumnIndex(RATIOERROR) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn (RATIOERROR);
					}
				if (showRatioSNR)
					{
					if (table.getColumnIndex(RATIOSNR) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn (RATIOSNR);
					}
				}
			if (showCompTot)
				{
				if (table.getColumnIndex(OTHERS) == MeasurementTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (OTHERS);
				}
			for (int ap=1; ap < nApertures; ap++)
				{
				String header = "_#"+(ap+1);
				if (showPosition)
					{
					if (table.getColumnIndex(      AP_XCENTER+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_XCENTER+header);
					if (table.getColumnIndex(      AP_YCENTER+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_YCENTER+header);
					}
				if (showPhotometry)
					{
					if (table.getColumnIndex(     AP_SOURCE+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_SOURCE+header);
					if (showPeak && table.getColumnIndex(     AP_PEAK+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_PEAK+header);
					if (showErrors && table.getColumnIndex(AP_SOURCE_ERROR+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_SOURCE_ERROR+header);
					if (showSNR && table.getColumnIndex(AP_SOURCE_SNR+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_SOURCE_SNR+header);
					if (table.getColumnIndex(      AP_BACK+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_BACK+header);
					}
				if (showWidths)
					{
					if (table.getColumnIndex(      AP_XWIDTH+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_XWIDTH+header);
					if (table.getColumnIndex(      AP_YWIDTH+header) == MeasurementTable.COLUMN_NOT_FOUND)
						i=table.getFreeColumn ( AP_YWIDTH+header);
					}
				if (showMeanWidth && table.getColumnIndex ( AP_MEANWIDTH) == MeasurementTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (AP_MEANWIDTH+header);
				}
			}
		table.show();
		return true;
		}

	/**
	 * Stores results for additional apertures.
	 */
	void storeAdditionalResults (int ap)
		{
		if (ap <= 0) return;

		String header = "_#"+(ap+1);
		if (showPosition)
			{
			table.addValue (AP_XCENTER+header, xCenter, 6);
			table.addValue (AP_YCENTER+header, yCenter, 6);
			}
		if (showPhotometry)
			{
			table.addValue (AP_SOURCE+header,   photom.sourceBrightness(), 6);
			if (showErrors)
				table.addValue (AP_SOURCE_ERROR+header,   photom.sourceError(), 6);
			if (showSNR)
				table.addValue (AP_SOURCE_SNR+header,   photom.sourceBrightness()/photom.sourceError(), 6);
			table.addValue (AP_BACK+header, photom.backgroundBrightness());
		  	if (showPeak)
				table.addValue (AP_PEAK+header, photom.peakBrightness(), 6);
			if (showSaturationWarning && (photom.peakBrightness() > saturationWarningLevel))
				table.addValue (AP_WARNING, photom.peakBrightness(), 6);
			}
		if (showWidths)
			{
			table.addValue (AP_XWIDTH+header,   xWidth, 6);
			table.addValue (AP_YWIDTH+header,   yWidth, 6);
			}
		if (showMeanWidth)
			table.addValue (AP_MEANWIDTH+header, 0.5*(xWidth+yWidth), 6);
		table.show();
		}

	/**
	 * Add aperture number to overlay display.
	 */
	protected void addStringRoi (double x, double y, String text)
		{
		super.addStringRoi (x,y,"  #"+(aperture+1)+":  "+text.trim());
		}

	/**
	 * Standard preferences dialog for MultiAperture_
	 */
	protected GenericDialog dialog()
		{
		GenericDialog gd = new GenericDialog("Multi-Aperture Measurements");
		gd.addMessage ("Aperture radii should have been set with the \"Set Aperture\" tool (double-click icon).");

		// REQUIRED DIALOG FIELDS:
		//	nApertures,firstSlice,lastSlice,previous,singleStep,oneTable,wideTable
		// NON-REQUIRED DIALOGUE FIELDS:
		//	showRatio,showRatioError,showRatioSNR,useVarSizeAp,apFWHMFactor

		gd.addNumericField ("   Maximum number of apertures per image :",
					nAperturesMax,0,6,"  (right click to finalize)");
		if (stackSize > 1)
			{
			gd.addNumericField ("           First slice :", firstSlice,0);
			gd.addNumericField ("           Last slice :",  lastSlice, 0);
	        }
		gd.addCheckbox ("Use previous "+nAperturesStored+" apertures (1-click for first aperture).",
					previous && nAperturesStored > 0);
		gd.addCheckbox ("Use single step mode (right click to exit)",singleStep);
		gd.addCheckbox ("Put results in image's own measurements table.", !oneTable);
		gd.addCheckbox ("All measurements from one image on the same line.", wideTable);
		gd.addMessage (" ");

		// HERE ARE THE THINGS WHICH AREN'T ABSOLUTELY NECESSARY
		addFancyDialog (gd);
		gd.addMessage (" ");
		gd.addMessage ("PLEASE SELECT OBJECTS (to abort aperture selection or processing, press <ESC>) :");
		return gd;
		}

	/**
	 * Adds options to MultiAperture_ dialog() which aren't absolutely necessary.
	 * Sub-classes of MultiAperture_ may choose to replace or extend this functionality if they use the original dialog().
	 */
	protected void addFancyDialog (GenericDialog gd)
		{
		// GET NON-REQUIRED DIALOGUE FIELDS:
		//	showRatio,showRatioError,showRatioSNR,useVarSizeAp,apFWHMFactor

		gd.addCheckbox ("Compute ratio of 1st aperture to others (only if on same line).",showRatio);
		gd.addCheckbox ("Show total comparison star counts (from apertures 2 to n).",showCompTot);
		gd.addCheckbox ("Show error of the ratio (only if you check \"Compute ratio\" above).",showRatioError);
		gd.addCheckbox ("Show signal-to-noise of ratio (only if you check \"Compute ratio\" above).",showRatioSNR);
		gd.addCheckbox ("Vary photometer aperture radii based on FWHM.",useVarSizeAp);
		gd.addNumericField ("FWHM multiplication factor :", apFWHMFactor,4);
		gd.addMessage (" ");
		gd.addCheckbox ("Allow left/right double click fast zoom-in/out (adds slight delay to aperture placement).", enableDoubleClicks);
		gd.addCheckbox ("Edit apertures as they are input.", editApertures);
		}

	/**
	 * Last part of non-required dialog created by addFancyDialog().
	 * Sub-classes not using the original dialog() will need a dummy version of this method!
	 */
	protected boolean finishFancyDialog (GenericDialog gd)
		{
		// GET NON-REQUIRED DIALOGUE FIELDS:
		//	showRatio,showRatioError,showRatioSNR,useVarSizeAp,apFWHMFactor

		showRatio      = gd.getNextBoolean();
		showCompTot    = gd.getNextBoolean();
		showRatioError = gd.getNextBoolean();
		showRatioSNR   = gd.getNextBoolean();
		useVarSizeAp   = gd.getNextBoolean();
		apFWHMFactor   = gd.getNextNumber();
		if (gd.invalidNumber())
			{
			IJ.beep();
			IJ.error ("Invalid aperture FWHM factor");
			shutDown();
			return false;
			}
		enableDoubleClicks = gd.getNextBoolean();
		editApertures = gd.getNextBoolean();

		Prefs.set (MultiAperture_.PREFS_SHOWRATIO, showRatio);
		Prefs.set (MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
		Prefs.set (MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
		Prefs.set (MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
		Prefs.set (MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
		Prefs.set (MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);
		return true;
		}

	/**
	 * Copies the ROI positions into the aperture positions for remeasuring (TENTATIVE).
	 */
	protected boolean extractROIs ()
		{
		Roi[] rois = OverlayCanvas.getOverlayCanvas(img).getRois();
		nApertures = rois.length;
		if (nApertures < 1)
			{
			IJ.beep();
			IJ.error ("No ROI's to remeasure!");
			shutDown();
			return false;
			}
		xPos = new double[nApertures];
		yPos = new double[nApertures];
		aPos = new double[nApertures];
		dPos = new double[nApertures];
		for (int i=0; i < nApertures; i++)
			{
			Roi roi = rois[i];
			if (roi instanceof ApertureRoi)
				{
				ApertureRoi aroi = (ApertureRoi)roi;
				double[] xy = aroi.getCenter();
				xPos[i] = xy[0];
				yPos[i] = xy[1];
				xy = aroi.getWCS();
				aPos[i] = xy[0];
				dPos[i] = xy[1];
				}
			else	{
				Rectangle rect = roi.getBounds();
				xPos[i] = (double)rect.x+0.5*(double)rect.width;
				yPos[i] = (double)rect.y+0.5*(double)rect.height;
				}
			}
		return true;
		}

	}

