// DIMM_.java

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Analyses an image stack assuming it represents a DIMM movie consisting of two neighboring images of the same star.
 *
 * Based on MultiAperture_.java
 * 
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2007-Apr-10
 */
public class DIMM_ extends MultiAperture_
	{
	int width=0;
	int height=0;
	int xOff=0;
	int yOff=0;

	/**
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	*/
	public int setup (String arg, ImagePlus img)
		{
                if (img == null) return DONE;           // ONLY WORKS IF THERE'S AN IMAGE
                IJ.register(DIMM_.class);
                return super.setup(arg,img);
                }

	/**
	 * Replaces MultiAperture_'s mouse-based input.
	 */
	public void run (ImageProcessor ip)
		{
		imp = ip;
		super.getMeasurementPrefs();
		wideTable = true;
                if ( !setUpApertures() || !super.prepare () )
			{
			img.unlock();
			return;
			}
                canvas.clear();

		processStack();
		img.unlock();
                }


	/**
	 * Define the apertures and decide on the sub-stack if appropriate.
	 */
	protected boolean setUpApertures ()
		{
		width = img.getWidth();
		height = img.getHeight();

		// CREATE DIALOGUE WINDOW

		GenericDialog gd = new GenericDialog("Differential Image Motion Measurer");

		gd.addNumericField ("First slice",1,0);
		gd.addNumericField ("Last slice",stackSize,0);

		// RUN DIALOGUE

		gd.showDialog();
		if (gd.wasCanceled())
			{
			cancelled = true;
			return false;
			}

		nApertures = 2;
		ngot = 2;

		firstSlice = (int)gd.getNextNumber();
		if (gd.invalidNumber() || firstSlice < 1)
			firstSlice=1;
		lastSlice = (int)gd.getNextNumber();
		if (gd.invalidNumber() || lastSlice > stackSize)
			lastSlice=stackSize;
		if (firstSlice > lastSlice)
			{
			int i=firstSlice;
			firstSlice=lastSlice;
			lastSlice=i;
			}
		doStack = true;
		return true;
		}
	/**
	 *
	 */
	protected void processStack ()
		{
		xOld = (double[])xPos.clone();
		yOld = (double[])xPos.clone();
		img.setSlice(firstSlice);
		findPair();
		xOld[0] = xPos[0];
		yOld[0] = yPos[0];
		xOld[1] = xPos[1];
		yOld[1] = yPos[1];
		super.processStack();
		}

	/**
	 * Performs processing of single images.
	 */
	protected void processImage ()
		{
		findPair();
		super.processImage ();
		}

	/**
	 * Finds two images to be measured.
	 */
	protected boolean findPair ()
		{
		Rectangle[] recs = new Rectangle[] {new Rectangle(0,0,0,0), new Rectangle(0,0,0,0) };

		int nrecs = 0;
		double dmax = imp.getMax();
		double dmin = imp.getMin();
		double thresh = dmin+0.5*(dmax-dmin);

		// FIND THE MAX

		for (int j=0; j < height; j++)
			{
			for (int i=0; i < width; i++)
				{
				double val = p.getPixelValue (i,j);
				if (val > thresh)
					{
					if (nrecs == 0 || ! recs[0].contains(i,j))	// NEXT SOURCE
						{
						if (nrecs == 2) nrecs=1;
						recs[nrecs].x      = x-w/2;
						recs[nrecs].width  = w;
						recs[nrecs].y      = y-h/2;
						recs[nrecs].height = h;
						nrecs++;
						}
					}
				}
			}

		if (nrecs != 2)
			{
			IJ.beep();
			IJ.showStatus("Could not find two stars");
			return false;
			}
		return true;
		}
	}
