// Remove_Calibraion.java

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

import astroj.*;

/**
 * This plugin simple removes all of the Calibraton already given to an image.
 * Compatible with older version of ImageJ that don't have the Command "Image > Adjust > Coordinates..."
 * 2017-AUG-24 (FV)
 */
 
public class Remove_Calibration implements PlugInFilter
	{
	ImagePlus img;

	public int setup (String arg, ImagePlus imp)
		{
		IJ.register(Calibrate_Spectrum.class);
		this.img = imp;
		return DOES_ALL;
		}

	public void run (ImageProcessor ip)
		{
		Calibration cal = new Calibration();
		img.setCalibration(cal);
		this.img.updateAndDraw();
		}
	}
