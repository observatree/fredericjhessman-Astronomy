// Seeing_Profile.java

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.measure.Calibration;

import java.awt.*;
import java.util.*;
import java.text.*;

import astroj.*;

/**
 * Plots radial profile of star-like object.
 */
public class Seeing_Profile implements PlugInFilter
	{
	ImagePlus imp;
	boolean canceled=false;
	double X0;
	double Y0;
	double mR;
	double peak;
	double background;
	double fwhm;

	int nBins=100;
	double[] radii = null;
	double[] means = null;

	Centroid center;
	Plot pw;

	boolean estimate = true;
	boolean recenter = true;
	boolean subtract = false;

	static public double SEEING_RADIUS1 = 1.7;	// IN UNITS OF fwhm
	static public double SEEING_RADIUS2 = 1.9;
	static public double SEEING_RADIUS3 = 2.55; // EQUAL NUMBERS OF PIXELS

	NumberFormat  nf;

	public int setup(String arg, ImagePlus imp)
		{
		this.imp = imp;
		return DOES_ALL+NO_UNDO+ROI_REQUIRED;
		}

	public void run(ImageProcessor ip)
		{
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits (2);

		Rectangle r = imp.getRoi().getBounds();
		if (r == null) return;

		int x = r.x;
		int y = r.y;
		int w = r.width;
		int h = r.height;
		X0 = (double)x+Centroid.PIXELCENTER+0.5*(double)(w-1);
		Y0 = (double)y+Centroid.PIXELCENTER+0.5*(double)(w-1);
		mR = 0.5*(double)(w+h);
		// IJ.log("x,y,w,h="+x+","+y+","+w+","+h+", i,j="+i+","+j+", x,y="+X0+","+Y0);

		// RESHOW ROI AS CIRCLE, ASK FOR RADIUS

		centerROI();
		doDialog();
		centerROI();
		if (canceled) return;

		// GET CENTROID WITHIN ROI

		if (recenter)
			{
			center = new Centroid();
			center.measureROI (ip);
			X0 = center.x();
			Y0 = center.y();
			// mR = center.r();
			background = center.background();
			}
		else
			background = crudeBackground(ip);
		centerROI();

		// FIND RADIAL DISTRIBUTION

		doRadialDistribution(ip);
		plotAllPoints(ip);

		// ASK WHETHER TO SAVE APERTURE RADII DERIVED FROM PSF

		if (estimate) saveRadii();
		if (subtract) subtractProfile();
		}

	/**
	 * Calculates a crude background value using the edges of the ROI.
	 */
	protected double crudeBackground (ImageProcessor ip)
		{
		Rectangle r = ip.getRoi().getBounds();
		int x = r.x;
		int y = r.y;
		int w = r.width;
		int h = r.height;
		double b = 0.0;
		int n=0;
		for (int i=x; i < x+w; i++)
			{
			b += ip.getPixelValue(i,y);
			n++;
			}
		for (int i=x; i < x+w; i++)
			{
			b += ip.getPixelValue(i,y+h);
			n++;
			}
		for (int j=y; j < y+h; j++)
			{
			b += ip.getPixelValue(x,j);
			n++;
			}
		for (int j=y; j < y+h; j++)
			{
			b += ip.getPixelValue(x+w,j);
			n++;
			}
		if (n == 0)
			return 0.0;
		else
			return b/(double)n;
		}

	/**
	 * Calculates the radial distribution of intensities around the center (X0,Y0).
	 */
	protected void doRadialDistribution(ImageProcessor ip)
		{
		nBins = (int)mR;

		radii = new double[nBins];
		means = new double[nBins];
		int[] count = new int[nBins];

		double R,z;

		int xmin = (int)(X0-mR);
		int xmax = (int)(X0+mR);
		int ymin = (int)(Y0-mR);
		int ymax = (int)(Y0+mR);
		peak = ip.getPixelValue((int)X0,(int)Y0);

		// ACCUMULATE ABOUT CENTROID POSITION

		for (int j=ymin; j < ymax; j++)
			{
			double dy = (double)j+Centroid.PIXELCENTER-Y0;
			for (int i=xmin; i < xmax; i++)
				{
				double dx = (double)i+Centroid.PIXELCENTER-X0;
				R = Math.sqrt(dx*dx+dy*dy);
				int k = (int)(R*(double)(nBins-1)/mR);
				int thisBin = k;
				if (thisBin >= nBins)  thisBin = nBins-1;
				z = ip.getPixelValue(i,j);
				radii[thisBin] += R;
				means[thisBin] += z;
				count[thisBin]++;
				if (z > peak) peak=z;
				}
			}

		// NORMALIZE

		radii[0] = 0.0;
		means[0] = 1.0;
		peak -= background;
		for (int k=1; k < nBins; k++)
			{
			if (count[k] > 0)
				{
				means[k]  =  ((means[k] / count[k]) - background)/peak;
				radii[k] /= count[k];
				}
			}

		// CALIBRATE X-AXIS USING LEFT-OVER BIN

		Calibration cal = imp.getCalibration();
		if (cal.getUnit() == "pixel")
			{
			for (int k=1; k < nBins; k++)
				radii[k]  *= cal.pixelWidth;
			}

		// FIND FWHM

		fwhm = 0.0;
		for (int k=1; k < nBins; k++)
			{
			if (means[k-1] > 0.5 && means[k] <= 0.5)
				{
				double m = (means[k]-means[k-1])/(radii[k]-radii[k-1]);
				double b = means[k]-m*radii[k];
				fwhm = 2.0*(0.5-b)/m;
				break;
				}
			}

		// CREATE PLOT WITH LABELS

		pw = new Plot ("Seeing Profile",
				"Radius ["+cal.getUnits()+"]",
				"Normalized Profile",
				radii, means);
		pw.setLimits (0.0,mR,-0.1,1.1);
		pw.draw();
		pw.addLabel(0.5,0.3,"Image: "+imp.getShortTitle());
		pw.addLabel(0.5,0.4,"Center: "+nf.format(X0)+","+nf.format(Y0));
		pw.addLabel(0.5,0.5,"FWHM: "+nf.format(fwhm)+" ["+cal.getUnits()+"]");
		pw.show();
		}

	/**
	 * Dialogue which lets the user adjust the radius.
	 */
	protected void doDialog()
		{
		canceled=false;
		GenericDialog gd = new GenericDialog("Seeing Profile for "+imp.getShortTitle(), IJ.getInstance());
		gd.addMessage ("X center (pixels) : "+nf.format(X0));
		gd.addMessage ("Y center (pixels) : "+nf.format(Y0));
		gd.addNumericField("Radius (pixels):", mR,2);
		gd.addCheckbox ("Recenter", recenter);
		gd.addCheckbox ("Estimate aperture radii", estimate);
		gd.addCheckbox ("Subtract mean profile", subtract);

		gd.showDialog();
		if (gd.wasCanceled())
			{
			canceled = true;
			return;
			}
		mR=gd.getNextNumber();
		if (gd.invalidNumber())
			{
			IJ.error("Invalid input Number");
			canceled=true;
			return;
			}
		recenter = gd.getNextBoolean();
		estimate = gd.getNextBoolean();
		subtract = gd.getNextBoolean();
		}

	/**
	 * Plots all the points in the ROI versus radius as points (not binned).
	 */
	protected void plotAllPoints (ImageProcessor ip)
		{
		int xmin = (int)(X0-mR);
		int xmax = (int)(X0+mR);
		int ymin = (int)(Y0-mR);
		int ymax = (int)(Y0+mR);
		int n = (xmax-xmin+1)*(ymax-ymin+1);

		double radii[] = new double[n];
		double fluxes[] = new double[n];
		double val = 0.0;

		int num=0;
		for (int j=ymin; j < ymax; j++)
			{
			double dy = (double)j+Centroid.PIXELCENTER-Y0;
			for (int i=xmin; i < xmax; i++)
				{
				double dx = (double)i+Centroid.PIXELCENTER-X0;
				double R = Math.sqrt(dx*dx+dy*dy);
				radii[num] = R;
				val = (ip.getPixelValue(i,j)-background)/peak;
				fluxes[num] = val;
				num++;
				}
			}
		pw.setColor (Color.BLUE);
		pw.addPoints (radii, fluxes, PlotWindow.BOX);
		pw.draw();
		pw.show();
		}

	protected void centerROI ()
		{
		IJ.makeOval((int)(X0-mR), (int)(Y0-mR), (int)(2*mR+1.0), (int)(2*mR+1.0));
		}

	protected void saveRadii ()
		{
		double r1 = (int)(fwhm*SEEING_RADIUS1);
		double r2 = (int)(fwhm*SEEING_RADIUS2);
		double r3 = (int)(fwhm*SEEING_RADIUS3);

		pw.setColor (Color.RED);
		double x[] = new double[] {0.0, r1,   r1,  r2,   r2,   r3,  r3};
		double y[] = new double[] {1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0};
		pw.addPoints (x, y, Plot.LINE);
		pw.draw();

		pw.addLabel ((0.5*r1/mR)-0.05,0.09,"SOURCE");
		pw.addLabel (1.0*r2/mR,0.09,"BACKGROUND");
		GenericDialog gd = new GenericDialog ("Save Aperture Radii?");
		gd.addMessage ("The following aperture radii were derived from the seeing profile :");
		gd.addNumericField ("Source radius : ", r1,2);
		gd.addNumericField ("Minimum background radius [pixels] : ", r2,2);
		gd.addNumericField ("Maximum background radius [pixels] : ",r3,2);
		gd.addMessage ("Select CANCEL to keep previous aperture radii.");
		gd.showDialog();
		if (gd.wasCanceled()) return;

		r1 = gd.getNextNumber();
		if (! gd.invalidNumber())
			Prefs.set (Aperture_.AP_PREFS_RADIUS, r1);
		r2  = gd.getNextNumber();
		if (! gd.invalidNumber())
			Prefs.set (Aperture_.AP_PREFS_RBACK1, r2);
		r3 = gd.getNextNumber();
		if (! gd.invalidNumber())
			Prefs.set (Aperture_.AP_PREFS_RBACK2, r3);
        Prefs.set("setaperture.aperturechanged",true);
		}


	double maximumOf(double[] arr)
		{
		int n=arr.length;
		double mx = arr[0];
		for (int i=1; i < n; i++)
			mx = (arr[i] > mx)? arr[i] : mx;
		return mx;
		}

	/**
	 * Subtract the mean profile from the entire image.
	 */

	void subtractProfile()
		{
		int h = imp.getHeight();
		int w = imp.getWidth();
		double x,y,r,z,c;
		ImageProcessor ip = imp.getProcessor();

		for (int j=0; j < h; j++)
			{
			y = (double)j-Y0;
			for (int i=0; i < w; i++)
				{
				x = (double)i-X0;
				r = Math.sqrt(x*x+y*y);
				if (r >= radii[nBins-1])
					z = means[nBins-1];
				else	{
					int k=nBins-1;
					while (r < radii[k] && k > 0) k--;
					z = means[k];
					}
				c = ip.getPixelValue(i,j)-peak*z;
				ip.putPixelValue(i,j,c);
				}
			}
		}
	}


