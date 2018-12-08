// Photometer.java

package astroj;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.math.*;

/**
 * Simple aperture photometer using a circular aperture and a background annulus with
 * up to twice the radius.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.3
 * @date 2005-Feb-17
 *
 * @version 1.4
 * @date 2006-Apr-26
 * @changes Two explicit background radii (FVH)
 *
 * @version 1.5
 * @date 2006-Nov-16
 * @changes Corrected wrong tests for Double.NaN.
 *
 * @version 1.6
 * @date 2007-Jan-29
 * @changes Added support for calculation of errors via gain and RON of CCD.
 *
 * @version 1.7
 * @date 2009-May-11
 * @changes Modified to support ImageJ pixel position standard (centers on half-pixels).
 *
 * @version 1.8
 * @date 2010-Mar-18
 * @author Karen Collins (Univ. Louisvill/KY)
 * @changes
 *	1) Changed the "source error" calculation to include dark current and to use the formulation from MERLINE, W.
 *		& HOWELL, S.B., 1995, EXP.  ASTRON., 6, 163 (same as in "Handbook of CCD Astronomy" by Steve B. Howell).
 *		The resulting code is: serror = Math.sqrt((Math.abs(source*gain)+sourceCount*(1+(double)sourceCount/
 *		(double)backCount)*(back*gain+dark+ron*ron))/gain);
 *
 * @version 1.9
 * @date 2010-Nov-24
 * @author Karen Collins (Univ. Louisvill/KY)
 * @changes Added support for removal of stars from background region (>3 sigma from mean)
 *
 * @version 1.10
 * @date 2010-Nov-24
 * @author Karen Collins (Univ. Louisvill/KY)
 * @changes Merged fix for aperture position bug and support for debug flag (from F.V. Hessman)
 */
public class Photometer
	{
	/** Center position of aperture in pixels. */
	protected double xCenter=0, yCenter=0;
	/** Radius of source aperture in pixels. */
	protected double radius=0;
	/** Mean background/pixel and integrated source, calibrated if possible. */
	protected double back=0, back2=0, backMean=0, prevBackMean=0;
	protected double back2Mean, backstdev=0, source=0;
	/** Error in source based on photon statistics.*/
	protected double serror;
	/** Mean background/pixel and integrated source, uncalibrated. */
	protected double rawSource=0, rawBack=0;
	/** Radii of background aperture. */
	protected double rBack1, rBack2;
	/** Number of pixels in source and background apertures. */
	protected long sourceCount=0, backCount=0;
	/** Calibration object of client. */
	Calibration calib;
	/** Peak level in ROI */
	protected double peak;

	/** CCD gain [e- per ADU or count] */
	protected double gain = 1.0;
	/** CCD read-out-noise [e-] */
	protected double ron = 0.0;
        /** CCD Dark Current per pixel [e-] */
    protected double dark = 0.0;

    protected boolean removeBackStars = true;

    	/** Debug flag */
	protected boolean debug = false;

	/**
	 * Initializes Photometer without the client's Calibration.
	 */
	public Photometer()
		{
		calib = null;
		radius = Double.NaN;
		rBack1 = Double.NaN;
		rBack2  = Double.NaN;
		}

	/**
	 * Initializes Photometer with the client ImagePlus's Calibration object.
	 *
	 *	@param cal		client's Calibration object
	 */
	public Photometer(Calibration cal)
		{
		calib = cal;
		radius = Double.NaN;
		rBack1 = Double.NaN;
		rBack2 = Double.NaN;
		}

	/**
	 * Performs aperture photometry on the current image using given center and aperture radii.  
	 *
	 *	@param ip		ImageProcessor
	 *	@param x		x-position of aperture center (pixels)
	 *	@param y		y-position of aperture center (pixels)
	 *	@param rad		radius of source aperture (pixels)
	 *	@param rb1		inner radius of background annulus (pixels)
	 *	@param rb2		outer radius of background annulus (pixels)
	 */
	public void measure (ImageProcessor ip, double x, double y, double rad, double rb1, double rb2)
		{
		xCenter = x;
		yCenter = y;
		radius = rad;
		rBack1 = rb1;
		rBack2 = rb2;
		
		debug = Prefs.get ("astroj.debug",false);

		if (Double.isNaN(radius)) return;

		double r = rBack2;
		if (Double.isNaN(rBack2) || rBack2 < radius) r = radius;

		// GET TOTAL APERTURE BOUNDS

		double xpix = x;	// +Centroid.PIXELCENTER;	// POSITION RELATIVE TO PIXEL CENTER
		double ypix = y;	// +Centroid.PIXELCENTER;

		int i1 = (int)(xpix-r);
		if (i1 < 0)
			i1=0;
		else if (i1 >= ip.getWidth())
			i1=ip.getWidth()-1;

		int i2 = (int)(xpix+r)+1;
		if (i2 < 0)
			i2=0;
		else if (i2 >= ip.getWidth())
			i2=ip.getWidth()-1;

		int j1 = (int)(ypix-r);
		if (j1 < 0)
			j1=0;
		else if (j1 >= ip.getHeight())
			j1=ip.getHeight()-1;

		int j2 = (int)(ypix+r)+1;
		if (j2 < 0)
			j2=0;
		else if (j2 >= ip.getHeight())
			j2=ip.getHeight()-1;

		int ic = (int)xpix;
		int jc = (int)ypix;

		double r2b1 = 0.0;
		double r2b2 = 0.0;
		if (!Double.isNaN(rb1) && rb1 > radius) r2b1 = rb1*rb1;
		if (!Double.isNaN(rb2) && rb2 > radius) r2b2 = rb2*rb2;

		double r2src = rad*rad;
		double r2 = 0.0;
		double d,di,dj;

		// INTEGRATE STAR WITHIN APERTURE OF RADIUS radius, SKY OUTSIDE

		source = 0.0;
		back = 0.0;
		prevBackMean = 0;
		sourceCount = 0;
		backCount = 0;
		back2 = 0;

		peak = ip.getPixelValue(i1,j1);

		for (int j=j1; j <= j2; j++)
			{
			dj=(double)j+Centroid.PIXELCENTER-ypix;		// Center;
			for (int i=i1; i <= i2; i++)
				{
				di=(double)i+Centroid.PIXELCENTER-xpix;	// Center;
				r2=di*di+dj*dj;
				d = ip.getPixelValue(i,j);
				if (r2 < r2src)  // SOURCE
					{
					source += d;
					sourceCount++;
					if (d > peak) peak=d;
					// if (debug) IJ.log("i,j="+i+","+j+", source+="+d);
					}
				else if (r2 > r2b1 && r2 <= r2b2)  // BACKGROUND
					{
					back += d;
					back2 += d*d;
					backCount++;
					// if (debug) IJ.log("i,j="+i+","+j+", back+="+d);
					}
				}
			}

		if (backCount > 0)
			{
			back /= (double)backCount;	// MEAN BACKGROUND
			backMean = back;
			back2Mean = back2 /(double)backCount;
			}
// IJ.log("remove stars="+removeBackStars+"   background="+back+"    backcount="+backCount+"    backstdev="+backstdev);
		if (removeBackStars)
			{
			for (int iteration = 0; iteration < 10; iteration++)
				{
				backstdev = Math.sqrt(back2Mean - backMean*backMean);
				back = 0.0;
				back2 = 0.0;
				backCount = 0;
				for (int j=j1; j <= j2; j++)   // REMOVE STARS FROM BACKGROUND
					{
					dj=(double)j-ypix;		// Center;
					for (int i=i1; i <= i2; i++)
						{
						di=(double)i-xpix;	// Center;
						r2=di*di+dj*dj;
						d = ip.getPixelValue(i,j);
						if (r2 > r2b1 && r2 <= r2b2
								&& d < backMean + 2.0*backstdev 
								&& d > backMean - 2.0*backstdev)
							{
							back += d;   // FINAL BACKGROUND
							back2 += d*d;
							backCount++;
							}
						}
					}
				if (backCount > 0)
					{
					back /= (double)backCount;	// MEAN BACKGROUND
					backMean = back;
					back2Mean = back2 /(double)backCount;
					}
// IJ.log("remove stars="+removeBackStars+"   background="+back+"    backcount="+backCount+"    backstdev="+backstdev);
				if (Math.abs(prevBackMean - backMean) < 0.1) break;
				prevBackMean = backMean;
				}
			}

		serror = 0.;
		if (sourceCount > 0 && backCount > 0)
			{
			double btot = back*(double)sourceCount;
			source -= btot;

			// ERROR FROM GAIN (e-/count), RON (e-), DARK CURRENT (e-) AND POISSON STATISTICS
			// SEE MERLINE, W. & HOWELL, S.B., 1995, EXP. ASTRON., 6, 163
		 	serror = Math.sqrt((
				Math.abs(source*gain)+sourceCount*(1.+(double)sourceCount/(double)backCount)
					*(back*gain+dark+ron*ron+gain*gain*0.289))/gain);

			/* FOR DEBUGGING PURPOSES:
			// IJ.log("srccount="+sourceCount+", backcount="+backCount);
                        // IJ.log("back="+back+", source="+source);
                        // IJ.log("ron="+ron+", dark="+dark+", gain="+gain);
			*/
                        }
                

		// CALIBRATE INTENSITIES IF POSSIBLE

		rawSource = source;
		rawBack = 0.0;
		if (calib != null && calib.calibrated())
			{
			rawSource = calib.getRawValue(source);
			if (backCount > 0) rawBack = calib.getRawValue(back);
			}
		}


	/**
	 * Performs aperture photometry on the current image given a pre-calculated center and standard radii.
	 *
	 *	@param ip		ImageProcessor
	 *	@param x		x-position of aperture center (pixels)
	 *	@param y		y-position of aperture center (pixels)
	 */
	public void measure (ImageProcessor ip, double x, double y)
		{
		this.measure (ip,x,y,radius,rBack1,rBack2);
		}

	/**
	 * Gets the corresponding radius of the measurement aperture.
	 */
	public double getApertureRadius (int n)
		{
		if (n == 0)
			return radius;
		else if (n == 1)
			return rBack1;
		else
			return rBack2;
		}

	/**
	 * Sets the radio of the maximum radii of the source and background apertures.
	 *
	 * @param r1		the minimum background aperture radius.
	 * @param r2		the maximum background aperture radius.
	 */
	public void setBackgroundApertureRadii (double r1, double r2)
		{
		rBack1 = r1;
		rBack2 = r2;
		}

	/**
	 * Sets the CCD gain [e- per ADU] and RON [ADU]
	 *
	 * @param r1		the minimum background aperture radius.
	 * @param r2		the maximum background aperture radius.
	 */
	public void setCCD (double g, double n, double d)
		{
		gain = g;
		ron = n;
                dark = d;
		}

    public void setRemoveBackStars (boolean removeStars)
        {
        removeBackStars = removeStars;
        }

	/**
	 * Returns the current ratio of the source and background aperture radii.
	 */
	public double getBackgroundApertureFactor ()
		{
		return rBack2/radius;
		}


	public void setSourceApertureRadius(double r)
		{
		radius = r;
		}
	public double getSourceApertureRadius ()
		{
		return radius;
		}


	public double sourceBrightness()
		{
		return source;
		}
	public double backgroundBrightness()
		{
		return back;
		}
	public double sourceError()
		{
		return serror;
		}
	public double rawSourceBrightness()
		{
		return rawSource;
		}
	public double rawBackgroundBrightness()
		{
		return rawBack;
		}

	public long numberOfSourceAperturePixels()
		{
		return sourceCount;
		}
	public long numberOfBackgroundAperturePixels()
		{
		return backCount;
		}

	public double peakBrightness()
		{
		return peak;
		}
	}



