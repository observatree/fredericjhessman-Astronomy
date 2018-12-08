// Copy_FITS_Header.java

import java.awt.*;

import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.*;

import astroj.*;

/**
 * Copies the FITS header from one image to another.  Optionally, a FITS HISTORY entry can be made.
 * 
 * @author F.V. Hessman
 * @date 2008-08-05
 * @version 1.0
 */
public class Copy_FITS_Header implements PlugIn 
	{
	static String OPTIONAL_HISTORY_ENTRY = new String("Your text for an optional history entry here!");

	public void run (String arg)
		{
		String[] header = null;

		// GET LIST OF CURRENT IMAGES

		String[] images = IJU.listOfOpenImages(null);	// displayedImages();
		if (images.length < 2)
			{
			IJ.showMessage("Copy FITS Header","Not enough images are open.");
			return;
			}

		// RUN DIALOG

		GenericDialog gd = new GenericDialog("Copy FITS Header");
		gd.addChoice ("from",images,images[0]);
		gd.addChoice ("to",images,images[1]);
		gd.addMessage ("Optional entry:");
		gd.addStringField("HISTORY",OPTIONAL_HISTORY_ENTRY,40);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		String image1 = gd.getNextChoice();
		String image2 = gd.getNextChoice();
		// IJ.log("Copying FITS header from image1="+image1+" to image2="+image2);
		String history = gd.getNextString();

		// CONNECT TO IMAGES, GET FITS HEADER

		ImagePlus img1 = WindowManager.getImage (image1);
		ImagePlus img2 = WindowManager.getImage (image2);
		if (img1 == null || img2 == null)
			{
			IJ.showMessage ("Unable to access selected images!");
			return;
			}

		header = FitsJ.getHeader(img1);
		if (header == null || header.length == 0)
			{
			IJ.showMessage ("Unable to access FITS header from image "+image1);
			return;
			}

		// ADD OPTIONAL HISTORY

		if (history != null && history.length() > 0 && !history.equals(OPTIONAL_HISTORY_ENTRY))
			{
			// IJ.log("Adding optional history entry: "+history);
			header = FitsJ.addHistory(history,header);
			}

		// CHECK FOR ROUGH CORRESPONDANCE OF DIMENSIONS

		int w1 = img1.getWidth();
		int w2 = img2.getWidth();
		int h1 = img1.getHeight();
		int h2 = img2.getHeight();
		int s1 = img1.getCurrentSlice();
		int s2 = img2.getCurrentSlice();
		if (w2 != w1 || h1 != h2 || s1 != s2)
			header = FitsJ.addHistory("CAUTION: dimensions do not match - just blind header copy!",header);

		// COPY HEADER

		FitsJ.putHeader(img2,header);
		}
	}
