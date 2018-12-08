// Centroid.java		

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.awt.*;
import java.util.*;
 
/**
 * Calculates the centroid position of an object within an ImageJ ROI.
 *
 * @author F.V. Hessman, Georg-August-Universit t G ttingen
 * @version 1.0
 * @date 2004-Dec-12
 *
 * @autor F.V. Hessman, Georg-August-Universit t G ttingen
 * @version 1.1
 * @date 2006-May-2
 * @changes Added FittedPlane background option.
 *
 * @version 1.2
 * @date 2006-Dec-14
 * @changes Widths are now Gaussian FWHM based on empirical calibration.
 *
 * @version 1.3
 * @date 2006-Dec-20
 * @changes Support for orientation angle and roundness.
 *
 * @version 1.4
 * @date 2007-Jan-30
 * @changes Optional no-positioning (if a given position should be used).
 *
 * @version 1.5
 * @date 2007-Apr-10
 * @changes Added status report to measure???() method "forgiving" error messages.
 *
 * @version 1.6
 * @date 2008-May-11
 * @changes Formal definition of ImageJ pixel center position (not uniformly treated: sometimes the edges,
 * not center, has integer position).
 *
 * @version 1.7
 * @date 2009-Jul-05
 * @changes Added calculation of variance within aperture
 *
 * @version 1.8
 * @date 2011-Mar-24
 * @changes Circular instead of rectangular window
 *
 * @version 1.9
 * @author K.A. Collins, University of Louisville
 * @date 2011-Mar-24
 * @changes Added centroid iterations
 *
 * @version 1.10
 * @author FVH
 * @date 2011-Apr-07
 * @changes Corrected code for the case reposition = false
 *
 * @version 1.11
 * @author FVH
 * @date 2012-Sep-16
 */

public class Centroid
	{
	public static double PIXELCENTER = 0.5;

	protected double xCenter,yCenter,radius,back;
	protected double xWidth,yWidth;
	protected double angle, ecc;
	protected double srcmax;
	protected double variance;

	FittedPlane plane = null;
	boolean usePlane = false;
	boolean reposition = true;

	public boolean forgiving = false;
	protected boolean debug;

	/**
	 * Default instantiation with constant background.
	 */
	public Centroid ()
		{
		usePlane = false;
		debug = Prefs.get ("astroj.debug",false);
		}

	/** 
	 * Optional instantiation with planar background
	 */
	public Centroid (boolean withPlane)
		{
		usePlane = withPlane;
		debug = Prefs.get ("astroj.debug",false);
		}

	/**
	 * Determine whether Centroid should reposition the aperture.
	 */
	public void setPositioning (boolean flag)
		{
		reposition = flag;
		}

	/**
	 * Set the current position of the aperture.
	 */
	public void setPosition (double xx, double yy)
		{
		xCenter = xx;
		yCenter = yy;
		}

	/**
	 * Finds the centroid of an object contained within the current ROI.
	 */
	public boolean measureROI (ImageProcessor imp)
		{
		Rectangle rct = imp.getRoi().getBounds();
		int ic = rct.x+rct.width/2;
		int jc = rct.y+rct.height/2;
		double r2 = (double)(rct.width*rct.height)/Math.PI;	// 2011-MAR-24: 0.25 -> 1/pi
		radius = Math.sqrt(r2)+0.5;
		// if (debug) IJ.log("Centroid: "+rct.x+","+rct.y+","+rct.width+","+rct.height+", r="+radius);

		if (reposition)	// THEN USE POSITION OF ROI
			{
			xCenter = (double)rct.x+0.5*(double)rct.width;	// +Centroid.PIXELCENTER;
			yCenter = (double)rct.y+0.5*(double)rct.height;	// +Centroid.PIXELCENTER;
			// if (debug) IJ.log("Centroid: assumed position is "+xCenter+","+yCenter);
			}
		if (getBackground(imp,rct) && measure (imp, rct))
			return true;
		else
			return false;
		}

	/**
	 * Finds the centroid of an object at a given position and radius.
	 */
	public boolean measureXYR (ImageProcessor imp, double xx, double yy, double rr)
		{
		xCenter = xx;
		yCenter = yy;
		radius = rr;
		int i = (int)(xx-rr);
		int j = (int)(yy-rr);
		int w = (int)(2.*rr);
		int h = w;
		Rectangle rct = new Rectangle (i,j,w,h);
		if (getBackground (imp,rct) && measure (imp,rct))
			return true;
		else
			return false;
		}

	/**
	 * Finds the background from the edges of a Rectangle.
	 */
	public boolean getBackground (ImageProcessor imp, Rectangle rct)
		{
		int i,j;
		double val=-17.0;

		back = 0.0;
		int backCount = 0;
		int i1 = rct.x;
		int i2 = i1+rct.width-1;
		int j1 = rct.y;
		int j2 = j1+rct.height-1;

		back = 0.0;
		if (usePlane)
			plane = new FittedPlane (rct.width*rct.height);

		// FIND MAX VALUE

		srcmax=imp.getPixelValue(i1,j1);
		for (j=j1; j <= j2; j++)
			{
			for (i=i1; i <= i2; i++)
				{
				val = imp.getPixelValue(i,j);
				if (val > srcmax) srcmax=val;
				}
			}

		// GUESS THE SKY BACKGROUND USING THE OUTSIDE PIXELS OF THE ROI

		int wid = rct.height/3;	// SO THAT THE EXTREME CORNERS AREN'T USED!
		double z;
		for (j=j1+wid; j <= j2-wid; j++)
			{
			z = imp.getPixelValue(i1,j);
			back += z;
			backCount++;
			if (usePlane) plane.addPoint (i1,j,z);

			z = imp.getPixelValue(i2,j);
			back += z;
			backCount++;
			if (usePlane) plane.addPoint (i2,j,z);
			}
		wid = rct.width/3;
		for (i=i1+wid; i <= i2-wid; i++)
			{
			z = imp.getPixelValue(i,j1);
			back += z;
			backCount++;
			if (usePlane) plane.addPoint (i,j1,z);

			z= imp.getPixelValue(i,j2);
			back += z;
			backCount++;
			if (usePlane) plane.addPoint (i,j2,z);
			}
		if (backCount > 0)
			{
			back /= (double)backCount;
			if (usePlane && ! plane.fitPlane())
				{
				showMessage ("Centroid ERROR : cannot fit plane to background!");
				return false;
				}
			}
		else	{
			showMessage ("Centroid ERROR : no background points for centroid!");
			return false;
			}

		srcmax -= back;
		return true;
		}

	/**
	 * Finds the centroid (optional), width, and angle of an object contained within the current ROI.
	 */
	public boolean measure (ImageProcessor imp, Rectangle rct)
		{
		double val;
		int backCount = 0;
		int i1 = rct.x;
		int i2 = i1+rct.width;	// 2011-MAR-24 REMOVED -1;
		int j1 = rct.y;
		int j2 = j1+rct.height;	// 2011-MAR-24 REMOVED -1;
		int num = 0;

		double xC = 0.0;
		double yC = 0.0;
		double wgt = 0.0;
		double avg = 0.0;
		double rad = 0.0;
		double di,dj;

		angle = 0.0;
		ecc = 0.0;
		variance = 0.0;

		// if (debug) IJ.log("Centroid: measuring at center "+xCenter+","+yCenter+" (PIXELCENTER="+Centroid.PIXELCENTER+")");
		boolean stillMoving = true;
		int iteration = 100;
		if (!reposition) iteration=1;
		while (stillMoving && iteration > 0)
            {
            for (int j=j1; j <= j2; j++)
                {
                dj = (double)j-yCenter+Centroid.PIXELCENTER;
                for (int i=i1; i <= i2; i++)
                    {
                    di = (double)i-xCenter+Centroid.PIXELCENTER;
                    rad = Math.sqrt(di*di+dj*dj);
                    if (rad <= radius)	// 2011-MAR-24 CORRECTED TO CIRCULAR REGION
                        {
                        if (usePlane) back = plane.valueAt (i,j);
                        val = (imp.getPixelValue(i,j)-back)/srcmax;
                        wgt += val;
                        xC  += val*di;
                        yC  += val*dj;
                        num++;
						// if (debug) IJ.log(""+num+": j,i="+j+","+i+", dj,di"+dj+","+di+", val="+val+", wgt="+wgt);
                        }
                    }
                }
            if (num > 0) avg = wgt/(double)num;
            if (wgt != 0.0)
                {
                xC /= wgt;
                yC /= wgt;
                }
            else	{
                showMessage ("Centroid ERROR : no signal in centroid using rectangle i1,j1="+i1+","+j1+", i2,j2="+i2+","+j2);
                return false;
                }

            // CHECK RESULTS

            int ix = (int)xC;
            int iy = (int)yC;
			if (reposition && (ix > rct.width/2 || iy > rct.height/2)) return false;
			iteration--;
            if (Math.abs(xC) < 0.00001 && Math.abs(yC) < 0.00001)
                {
                stillMoving = false;
				// if (debug) IJ.log("interation = "+iteration);
                }
            if (reposition)
                {
                xCenter += xC;
                yCenter += yC;
                i1 = (int)xCenter - rct.width / 2;
                i2 = i1 + rct.width;
                j1 = (int)yCenter - rct.height / 2;
                j2 = j1 + rct.height;
                Rectangle newRct = new Rectangle(i1, j1, rct.width, rct.height);
                if (!getBackground(imp, newRct)) return false;
                }
			// if (debug) IJ.log("Centroid: new position is "+xCenter+","+yCenter);
            }
        
		// COMPUTE THE MOMENT WIDTHS AND VARIANCE
		xWidth = 0.0;
		yWidth = 0.0;
		wgt = 0.0;
		double mxy = 0.0;
		for (int j=j1; j <= j2; j++)
			{
			dj = (double)j-yCenter+Centroid.PIXELCENTER;
			for (int i=i1; i <= i2; i++)
				{
				di = (double)i-xCenter+Centroid.PIXELCENTER;
				rad = Math.sqrt(di*di+dj*dj);
				if (rad <= radius)	// 2011-MAR-24 CORRECTED TO CIRCULAR REGION
					{
                    if (usePlane) back = plane.valueAt (i,j);
                    val = (imp.getPixelValue(i,j)-back)/srcmax;
                    wgt += val;
                    xWidth += val*di*di;
                    yWidth += val*dj*dj;
                    mxy += val*di*dj;
                    variance += (val-avg)*(val-avg);
                    }
                }
			}
		if (wgt != 0.0)
			{
			xWidth /= wgt;	// m20
			yWidth /= wgt;	// m02
			mxy /= wgt;	// m11

			angle = 0.5*180.0*Math.atan2(2.0*mxy,xWidth-yWidth)/Math.PI;
			ecc = ((xWidth-yWidth)*(xWidth-yWidth)+4.0*mxy*mxy)/
				   ((xWidth+yWidth)*(xWidth+yWidth));

			xWidth = Math.sqrt(xWidth);
			yWidth = Math.sqrt(yWidth);

			if (num > 1) variance /= (double)(num-1);
			}
		else	{
			showMessage ("Centroid ERROR : no data for moment widths!");
			return false;
			}

		// NORMALIZE WIDTHS TO GIVE GAUSSIAN FWHM (EMPIRICAL FACTOR)

		xWidth /= 0.3602;
		yWidth /= 0.3602;
		return true;
		}

	public double x() { return xCenter; }
	public double y() { return yCenter; }
	public double r() { return radius; }
	public double background() { return back; }
	public double width() { return xWidth; }
	public double height() { return yWidth; }
	public double orientation() { return angle; }
	public double roundness() { return 1.0-ecc; }
	public double variance () { return variance; }
	public double signal () { return srcmax*xWidth*yWidth; }

	public void setRadiusTo (ImageProcessor imp, int r)
		{
		Rectangle rct = imp.getRoi().getBounds();	// E.G. CREATED BY this.mousePressed()
		if (rct == null) return;
		int ic = rct.x+rct.width/2;
		int jc = rct.y+rct.height/2;
		imp.setRoi (ic-r,jc-r,2*r,2*r);
		}

	public void showMessage (String msg)
		{
		if (forgiving)
			{
			IJ.beep();
			IJ.showStatus (msg);
			IJ.log(msg);
			IJ.wait(1000);
			}
		else	{
			IJ.showMessage (msg);
			IJ.log(msg);
		    }
	    }
	}
