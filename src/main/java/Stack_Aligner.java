// Stack_Aligner.java

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
 * Based on MultiAperture_.java
 * 
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2006-Oct-10
 *
 * @version 1.1
 * @date 2006-Nov-29
 * @changes Made this version an extension of MultiAperture_ to make alignment more robust.
 *
 * @version 1.2
 * @date 2009-FEB-09
 * @changes Added whole pixel shift.
 */
public class Stack_Aligner extends MultiAperture_
	{
	boolean normalize = false;
	boolean whole = true;
	boolean firstImage = true;
	String label = "Aligned_";
	double[] xRef = null;
	double[] yRef = null;
	double flux = 0.0;

	/**
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	 */
	public int setup (String arg, ImagePlus img)
		{
		if (img == null) return DONE;		// ONLY WORKS IF THERE'S AN IMAGE
		IJ.register(Stack_Aligner.class);
		return super.setup(arg,img);
		}

	/**
	 * Initializes the reference position arrays.
	 */
	protected boolean prepare ()
		{
		if (stackSize < 2) return false;
		doStack = true;
		return super.prepare();
		}

	/**
	 * Adds the aperture parameters to the list of apertures.
	 */
	protected void addAperture ()
		{
		super.addAperture ();
		}

	/**
	 * Dialog for this MultiAperture_ sub-class
	 */
	protected GenericDialog dialog()
		{
		// CREATE DIALOGUE WINDOW
		GenericDialog gd = new GenericDialog("Stack Aligner");

		// REQUIRED FIELDS

		gd.addNumericField ("   Maximum number of apertures per image :", nAperturesMax,0,6,"  (right click to finalize)");
		if (stackSize > 1)
			{
			gd.addNumericField("           First slice :", firstSlice,0);
			gd.addNumericField("           Last  slice :", lastSlice,0);
			}
		gd.addCheckbox ("Use previous "+nAperturesStored+" apertures (1-click for first aperture).",previous && nAperturesStored > 0);
		gd.addCheckbox ("Use single step mode (right click to exit).",singleStep);
		gd.addMessage (" ");
		gd.addCheckbox ("Put results in stack's own measurements table.", !oneTable);
		gd.addCheckbox ("All measurements from one image on the same line.",wideTable);
		gd.addMessage (" ");

		// NON-REQUIRED FIELDS (mirrored in finishFancyDialog())

        normalize = Prefs.get ("stackAligner.normalize", normalize);
        whole = Prefs.get ("stackAligner.whole", whole);
		gd.addCheckbox ("Remove background and scale to common level", normalize);
		gd.addCheckbox ("Align only to whole pixels (no interpolation)!",whole);
		gd.addMessage ("After pressing the \"OK\" button, select the image alignment object(s).");
		gd.addMessage ("To finalize object selection, right click. To abort the process, press <ESC>.");
		return gd;
		}

	/**
	 * Parses the non-required fields of the dialog and cleans up thereafter.
	 */
	protected boolean finishFancyDialog (GenericDialog gd)
		{
		normalize = gd.getNextBoolean();
		whole     = gd.getNextBoolean();

		Prefs.set ("stackAligner.normalize", normalize);
		Prefs.set ("stackAligner.whole", whole);   

		xPos = new double[nApertures];
		yPos = new double[nApertures];
		ngot = 0;
		xRef = new double[nApertures];
		yRef = new double[nApertures];
		return true;
		}

	/**
	 * Perform photometry on each image of selected sub-stack.
	 */
	protected void processStack ()
		{
		// GET MEAN APERTURE BRIGHTNESS

		if (firstImage)
			{
			for (int i=0; i < nApertures; i++)
				{
				xRef[i] = xPos[i];
				yRef[i] = yPos[i];
				}
			String titl = img.getShortTitle();
			if (titl == null)
			titl = img.getTitle();
			label += titl;
			}

		// PROCESS STACK

		super.processStack ();

		// RENAME RESULTING ALIGNED STACK

		img.setTitle(label);
		}

	/**
	 * Performs processing of single images.
	 */
	protected void processImage ()
		{
		// NORMAL APERTURE MEASUREMENTS (INCLUDING MEAN back and source)

		super.processImage ();

		if (firstImage)
			flux = (target+others)/nApertures;

		// PERFORM BACKGROUND SUBTRACTION AND NORMALIZATION

		if (normalize)
			{
			imp.resetRoi();		// SO CAN PERFORM ON WHOLE IMAGE
			imp.add (-back);	// REMOVE MEAN BACKGROUND
			if (!firstImage)		// NORMALIZE TO STANDARD FLUX
				imp.multiply (flux/((target+others)/nApertures));
			}

		// SHIFT IMAGE

		double dx = 0.0;
		double dy = 0.0;
		for (int i=0; i < nApertures; i++)
			{
			dx += xPos[i]-xRef[i];
			dy += yPos[i]-yRef[i];
			}
		dx /= nApertures;
		dy /= nApertures;
		img.setProcessor ("Aligned_"+img.getStack().getShortSliceLabel(slice), shiftedImage(dx,dy));
		firstImage = false;
		}

	/**
	 * Shifts image linearly by an amount (dx,dy).
	 */
	protected ImageProcessor shiftedImage (double dx, double dy)
		{
		int h = imp.getHeight();
		int w = imp.getWidth();

		ImageProcessor ip = imp.duplicate ();

		for (int j=0; j < h; j++)
			{
			double y = (double)j+dy;
			int iy = (int)(y+Centroid.PIXELCENTER);
			if (whole)
				{
				for (int i=0; i < w; i++)
					{
					double x = (double)i+dx;
					int ix = (int)(x+Centroid.PIXELCENTER);
					double d = (double)imp.getPixelValue(ix, iy);
					ip.putPixelValue (i,j,d);
					}
				}
			else	{
				for (int i=0; i < w; i++)
					{
					double x = (double)i+dx;
					double d = imp.getInterpolatedPixel(x, y);
					ip.putPixelValue (i,j,d);
					}
				}
			}
		return ip;
		}

	}
