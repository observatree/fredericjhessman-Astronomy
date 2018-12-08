// Underflow_Fixer.java

import ij.*;
import ij.gui.*;
import ij.IJ.*;
import ij.measure.*;
import ij.process.*;
import ij.plugin.filter.*;

/**
 * Fixes the stupid unsigned int FITS conversion problem for digitally saturated pixels.
 * 2017-SEP-11, FVH
 */
public class Underflow_Fixer implements PlugInFilter
	{
	ImagePlus img;
	public int setup (String arg, ImagePlus imp)
		{
		IJ.register (Underflow_Fixer.class);
		this.img = imp;
		return (DOES_16+DOES_32);
		}
	public void run (ImageProcessor ip)
		{
		int nslices = img.getNSlices ();
		for (int s=1; s <= nslices; s++)
			{
			IJ.log ("Slice "+s+"/"+nslices);
			fix (s);
			}
		img.updateAndDraw ();
		}
	protected void fix (int slice)
		{
		img.setSlice (slice);
		ImageProcessor ip = img.getProcessor ();
		if (ip instanceof ShortProcessor)
			{
			ImageConverter conv = new ImageConverter (img);
			conv.convertToGray32 ();
			ip = img.getProcessor ();
			}
		int ni = ip.getWidth();
		int nj = ip.getHeight();
		Calibration cal = img.getCalibration();
		for (int j=0; j < nj; j++)
			{
			for (int i=0; i < ni; i++)
				{
				float val = ip.getPixelValue (i,j);
				double cval = cal.getCValue (val);
				if (cval < 0.0)
					{
					float lval = (float)(val+65536.);
					ip.setf (i,j,lval);
					// IJ.log (""+i+","+j+","+val+" -> "+lval);
					}
				}
			}
		cal.disableDensityCalibration ();
		}
	}
