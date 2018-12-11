// Create_Dark.java

import java.awt.*;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;

import astroj.*;

/**
 * Plugin that processes/calibrates CCD images.
 *
 * @date 2008-NOV-18
 * @name F.V. Hessman (Univ. Goettingen)
 * @version 1.0
 */
public class Process_Images implements PlugIn
	{
	ImagePlus rawImage;
	ImagePlus biasImage = null;
	ImagePlus resultImage = null;

	boolean biasCorrection,expCorrection;

	String raw  = null;
	String bias = null;
	String result = null;

	ImageProcessor rawp = null;
	ImageProcessor biasp = null;
	ImageProcessor resultp = null;

	int h,w;
	int slices;

	public static String PREFS_CCD_BIAS = "BIAS";
	public static String PREFS_CCD_DARK = "DARK";

	String[] hdr = null;

	float darktime = 1.0f;
	float[] rawtimes;

	/**
	 * Perform all the necessary steps
	 */
	public void run(String arg)
		{
		if (! getArgs(arg)) return;
		if (! doDialog()) return;
		if (! getImages()) return;
		if (! process()) return;
		resultImage.show();
		savePreferences();
		}

	/**
	 * Gets arguments from the PlugIn argument or from the ImageJ preferences.
	 */
	protected boolean getArgs (String arg)
		{
		getPreferences();
		if (arg == null || arg.trim().length() == 0) return true;

		// PARSE ARGUMENTS IN THE FORMAT "arg1=val1, arg2=val2, ..."

		String[] args = arg.trim().split(",");
		for (int i=0; i < args.length; i++)
			{
			String[] things = args[i].trim().split("=");
			if (things.length == 2)
				{
				if (things[0].startsWith("biascorr"))
					biasCorrection = (things[1] == "true")? true : false;
				else if (things[0] == "bias")
					bias = things[1];
				else if (things[0].startsWith("expcorr"))
					expCorrection = (things[1] == "true")? true : false;
				else if (things[0] == "raw")
					raw = things[1];
				else if (things[0] == "result")
					result = things[0];
				else if (things[0] == "new")
					newimage = (things[1] == "true")? true : false;
				else	{
					IJ.showMessage("Unknown Create_Dark argument: "+args[i]);
					return false;
					}
				}
			}
		return true;
		}

	/**
	 * Get preferences: names of files and options.
	 */
	protected void getPreferences()
		{
		biasCorrection = Prefs.get("ccd.biascorr",false);
		bias = Prefs.get("ccd.bias",PREFS_CCD_BIAS);
		result = Prefs.get ("ccd.dark",PREFS_CCD_DARK);
		expCorrection = Prefs.get("ccd.expcorr",false);
		}

	/**
	 * Dialogue that lets the user input options and image names.
	 */
	protected boolean doDialog()
		{
		String r,b,d,f;

		String[] images = IJU.listOfOpenImages("");
		if (raw == null || raw.trim().length() == 0)
			raw = WindowManager.getCurrentImage().getTitle();		

		GenericDialog gd = new GenericDialog("Create Master Dark Image");

		gd.addStringField ("Name of new dark image",result,20);			// 1
		if (contains(images,raw))
			gd.addChoice ("Stack to be processed",images,raw);		// 2
		else
			gd.addChoice ("Stack to be processed",images,"");
		gd.addCheckbox ("Subtract bias",biasCorrection);			// 4
		if (biasCorrection && contains(images,bias))
			gd.addChoice ("",images,bias);					// 5
		else
			gd.addChoice ("",images,"");
		gd.addMessage ("");
		gd.addCheckbox ("Correct for exposure times",expCorrection);		// 6

		gd.showDialog();
		if (gd.wasCanceled()) return false;

		result = gd.getNextString();						// 1
		r = gd.getNextChoice(); if (r.trim().length() != 0) raw=r;		// 2
		biasCorrection = gd.getNextBoolean();					// 4
		b = gd.getNextChoice();	if (b.trim().length() != 0) bias=b;		// 5
		expCorrection = gd.getNextBoolean();					// 6

		return true;
		}

	/**
	 * Given the options and input names, retrieves the images (must be active!).
	 */
	protected boolean getImages()
		{
		// GET RAW IMAGE

		if (raw.trim().length() == 0)
			{
			rawImage = IJ.getImage();
			if (rawImage == null)
				{
				IJ.showMessage("No image available to be processed!");
				return false;
				}
			raw = rawImage.getTitle();
			}
		else	{
			rawImage = WindowManager.getImage(raw);
			if (rawImage == null)
				{
				IJ.showMessage("No raw image called \""+raw+"\" available!");
				return false;
				}
			}
		slices = rawImage.getImageStackSize();
		rawtimes = new float[slices];
		w = rawImage.getWidth();
		h = rawImage.getHeight();

		if (!newimage && rawImage.getBitDepth() != 32)
			{
			ImageConverter ic = new ImageConverter(rawImage);
			ic.convertToGray32();
			}

		// GET CORRECTION IMAGES

		if (biasCorrection)
			{
			biasImage = WindowManager.getImage(bias);
			if (biasImage == null)
				{
				IJ.showMessage("No bias image called \""+bias+"\" available!");
				return false;
				}
			else if (biasImage.getImageStackSize() != 1)
				{
				IJ.showMessage("Bias image is a stack!");
				return false;
				}
			else if (biasImage.getWidth() != w || biasImage.getHeight() != h)
				{
				IJ.showMessage("Bias image is the wrong size!");
				return false;
				}
			}

		if (expCorrection)
			{
			if (slices == 1)
				{
				hdr = FitsJ.getHeader(rawImage);
				if (hdr == null)
					{
					IJ.showMessage("Cannot extract FITS header for raw image!");
					return false;
					}
				rawtimes[0] = (float)FitsJ.getExposureTime(hdr);
				if (Float.isNaN(rawtimes[0]))
					{
					IJ.showMessage("Cannot extract exposure time for raw image!");
					return false;
					}
				}
			else	{
				for (int i=1; i <= slices; i++)
					{
					rawImage.setSlice(i);
					hdr = FitsJ.getHeader(rawImage);
					if (hdr == null)
						{
						IJ.showMessage("Cannot read FITS header for raw iamge #"+i);
						return false;
						}
					rawtimes[i-1] = (float)FitsJ.getExposureTime(hdr);
					if (Float.isNaN(rawtimes[i-1]))
						{
						IJ.showMessage("Cannot extract exposure time for raw image #"+i);
						return false;
						}
					}
				}
			}
		else	{
			rawtimes = new float[slices];
			for (int i=0; i < slices; i++)
				rawtimes[i] = 1.0f;
			}

		// CREATE RESULT IMAGE

		if (result == null || result.trim().length() == 0)
			result = newName(IJU.extractFilenameWithoutFitsSuffix(raw));		
		resultImage = IJ.createImage (result,"32-bit",w,h,1);
		return true;
		}

	/**
	 * Performs the actual image arithmetic.
	 */
	protected boolean process()
		{
		float[] rawData=null;
		float[] biasData=null;
		float[] resultData=null;
		int wh = w*h;

		ImageStack rawStack = null;

		String rawLabel = raw;
		String resultLabel = result;

		if (slices > 1)
			rawStack = rawImage.getStack();
		resultp = resultImage.getProcessor();

		// GET THE CALIBRATION DATA

		if (biasCorrection)
			{
			biasp = biasImage.getProcessor();
			biasp = biasp.convertToFloat();
			biasData = (float[])biasp.getPixels();
			}

		// PROCESS EACH SLICE OF THE RAW IMAGE STACK

		for (int i=1; i <= slices; i++)
			{
			rawImage.setSlice(i);

			rawp = rawImage.getProcessor();
			rawp = rawp.convertToFloat();
			rawData = (float[])rawp.getPixels();
			hdr = FitsJ.getHeader(rawImage);

			resultData = new float[wh];

			// PROCESS EACH PIXEL

			for (int n=0; n < wh; n++)
				{
				if (biasCorrection)
					resultData[n] += (rawData[n]-biasData[n])/rawtimes[n];
				else
					resultData[n] += rawData[n]/rawtimes[n];
				}

			// SAVE RESULTS

			resultp.setPixels((Object)resultData);
			resultp.resetMinAndMax();

			// NOTE PROCESSING IN FITS HEADER

			if (hdr != null)
				{
				String sl = "";
				if (slices > 1) sl = "["+i+"]";

				String history = "Process_image"+sl+" : "+resultLabel+" = "+rawLabel;
				if (biasCorrection)
					history += " - "+bias;
				if (darkCorrection)
					history += " - "+dark;
				if (expCorrection)
					history += "*("+rawtimes[i-1]+"/"+darktime+")";
				if (flatCorrection)
					history += ") / "+flat;
				else
					history += ")";

				int l=history.length();
				for (int n=0; n < l; n+=70)
					{
					if ((n+70) < l)
						hdr = FitsJ.addHistory("  "+history.substring(n,n+70),hdr);
					else
						hdr = FitsJ.addHistory("  "+history.substring(n,l),hdr);
					}

				// STORE HEADER, IF IT EXISTS

				if (slices == 1)
					FitsJ.putHeader(resultImage,hdr);
				else
					FitsJ.putHeader(resultStack,hdr,i);
				}
			}
		if (slices > 1) resultImage.setStack(result,resultStack);
		return true;	
		}

	/**
	 * Saves user selected values as ImageJ preference for later use.
	 */
	protected void savePreferences()
		{
		Prefs.set("ccd.biascorrection",biasCorrection);
		if (biasCorrection)
			Prefs.set("ccd.biasimage",bias);
		Prefs.set("ccd.darkcorrection",darkCorrection);
		if (darkCorrection)
			Prefs.set ("ccd.darkimage",dark);
		Prefs.set("ccd.flatcorrection",flatCorrection);
		if (flatCorrection)
			Prefs.set("ccd.flatimage",flat);
		Prefs.set("ccd.expcorrection",expCorrection);
		}

	/**
	 * Constructs a logical name for a processed image.
	 */
	public static String newName(String oldName)
		{
		return new String(oldName+"_PROC");
		}

	/**
	 * Checks if a string is present in an array of strings.
	 */
	public static boolean contains(String[] arr, String str)
		{
		int n=arr.length;
		if (arr == null || n == 0)
			return false;
		for (int i=0; i < n; i++)
			{
			if (arr[i].equals(str)) return true;
			}
		return false;
		}
	}
