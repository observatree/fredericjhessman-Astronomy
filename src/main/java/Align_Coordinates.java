// Align_Coordinates.java		

import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.text.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import Jama.*;
import astroj.*;

/**
 * Aligns the WCS coordinate systems of two images using two or more fiduciary aperture positions
 * in the OverlayCanvas's of the images.  The pixel positions from the work image are taken and
 * matched with the WCS positions of the reference image to produce a new WCS, which is then placed
 * in the FITS header of the work image.
 *
 * It is assumed that the aperture position measurements were made in the SAME ORDER! This could
 * be changed by adding an intelligent means of cross-identification, but....
 *
 * Based on Align_Image.java.
 *
 * Uses the JAMA matrix package: see http://math.nist.gov/javanumerics/jama/.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2012-SEP-20
 */
public class Align_Coordinates implements PlugInFilter
	{
	String[] images;
	int[] imageList;
	int numImages = 0;
	int current = 0;
	Boolean verbose = false;

	ImagePlus workImage, refImage;

	double[] xWork,yWork;
	double[] aRef,dRef;


	/** Standard ImageJ setup routine. */
	public int setup (String arg, ImagePlus imag)
		{
		IJ.register(Align_Coordinates.class);
		return DOES_ALL;
		}

	/**
	 * Aligns the WCS reference systems of two images selected by the user, effectively
	 * placing the work image on the astrometric system of the reference image.
	 */
	public void run (ImageProcessor ip)
		{
		// ARE THERE ENOUGH WINDOWS?
		workImage = WindowManager.getCurrentImage();
		imageList = WindowManager.getIDList();
		if (imageList == null)
			{
			IJ.error("No images are open!");
			return;
			}
		numImages = imageList.length;
		if (numImages < 2)
			{
			IJ.error ("At least two images with its own displayed measurements should be open.");
			return;
			}
		images = new String[numImages];
		for (int i=0; i < numImages; i++)
			{
			ImagePlus im = WindowManager.getImage (imageList[i]);
			if (im == workImage)
				current = i;
			if (im.getImageStackSize() > 1)
				{
				int slice = im.getCurrentSlice();
				ImageStack stack = im.getStack();
				images[i] = stack.getShortSliceLabel(slice);
				}
			else	{
				images[i] = im.getShortTitle();
				}
			}

		// SELECT THE FILES
		if (!selectFiles()) return;

		// GET THE ASTROMETRY DATA
		if (!getAstrometry()) return;

		// CREATE NEW WCS FOR WORK IMAGE
		createNewWCS();
		}

	/**
	 * Lets the user select the file to be rebinned and the reference file.
	 */
	protected boolean selectFiles()
		{
		// SHOW POSSIBLE IMAGES IN DIALOGUE
 		GenericDialog gd = new GenericDialog ("Align World Coordinates", IJ.getInstance());
		gd.addChoice ("Image to be aligned:", images, images[current]);
		int i=0;
		if (i == current) i++;
		gd.addChoice ("Reference image:", images, images[i]);
		gd.addCheckbox ("Verbose", false);

		// DO DIALOGUE
		gd.showDialog ();
		if (gd.wasCanceled())
			return false;

		// GET IMAGES
		int t = gd.getNextChoiceIndex();
		workImage = WindowManager.getImage(imageList[t]);
		if (workImage == null)
			{
			IJ.error ("Unable to access work image "+images[t]);
			return false;
			}
		t = gd.getNextChoiceIndex();
		refImage = WindowManager.getImage(imageList[t]);
		if (refImage == null)
			{
			IJ.error ("Unable to access reference image "+images[t]);
			return false;
			}

		// IMAGES HAVE TO BE DIFFERENT
		if (workImage == refImage)
			{
			IJ.error ("Image to be rebinned = reference image!");
			return false;
			}

		verbose = gd.getNextBoolean();
		return true;
		}

	/**
	 * Gets the (xpix,ypix) and (alpha,delta) positions from the ApertureRoi's attached
	 * to the work and reference images, respectively.  The pixel positions are stored
	 * in ImageJ rather than FITS format: the WCS objects keep track of the difference!
	 */
	protected boolean getAstrometry()
		{
		// GET ASSOCIATED ImageJ PIXEL POSITION MEASUREMENTS IN WORK IMAGE
		ImageCanvas canvas = workImage.getCanvas();
		if (!(canvas instanceof OverlayCanvas))
			{
			IJ.error ("Work image has not had any Aperture measurements!");
			return false;
			}
		OverlayCanvas ocanvas = (OverlayCanvas)canvas;
		Roi[] rois = ocanvas.getRois();
		int n = rois.length;
		if (n == 0)
			{
			IJ.error ("Work image does not have any Aperture measurements!");
			return false;
			}
		xWork = new double[n];
		yWork = new double[n];
		aRef  = new double[n];
		dRef  = new double[n];
		int ntot = 0;
		if (verbose)
			IJ.log("\nImage pixel positions (FITS format):");
		for (int i=0; i < n; i++)
			{
			Roi roi = rois[i];
			if (roi instanceof ApertureRoi)
				{
				ApertureRoi aroi = (ApertureRoi)roi;
				double[] xy = aroi.getCenter();
				xWork[ntot] = xy[0];		// ImageJ POSITIONS
				yWork[ntot] = xy[1];
				ntot++;
				if (verbose)
					IJ.log("\t"+ntot+"\t"+xy[0]+", "+xy[1]);
				}
			else
				IJ.log("\troi #"+i+" is not an ApertureRoi!");
			}

		// GET ASSOCIATED WCS POSITIONS IN REFERENCE IMAGE
		canvas = refImage.getCanvas();
		if (!(canvas instanceof OverlayCanvas))
			{
			IJ.error ("Reference image has not had any Aperture measurements!");
			return false;
			}
		ocanvas = (OverlayCanvas)canvas;
		rois = ocanvas.getRois();
		int m = rois.length;
		if (m == 0)
			{
			IJ.error ("Reference image does not have any Aperture measurements!");
			return false;
			}
		if (m != n)
			{
			IJ.error ("Images have different number of measurements - Align_Image cannot yet find the matches!");
			return false;
			}
		aRef = new double[m];
		dRef = new double[m];
		int npts = 0;
		if (verbose)
			IJ.log("\nReference WCS positions:");
		for (int i=0; i < n; i++)
			{
			Roi roi = rois[i];
			if (roi instanceof ApertureRoi)
				{
				ApertureRoi aroi = (ApertureRoi)roi;
				double[] xy = aroi.getWCS();
				aRef[npts] = xy[0];
				dRef[npts] = xy[1];
				npts++;
				if (verbose)
					IJ.log("\t"+npts+"\t"+xy[0]+", "+xy[1]);
				}
			}
		if (ntot != npts)
			{
			IJ.error ("Reference and work image have different number of measurements!");
			return false;
			}
		return true;
		}

	/**
	 * Calculates the linear transformation
	 *	z = c[0] + c[1]*x + c[2]*y
	 * using a standard least-squares solution.
	 */
	protected double[] transform (double[] x, double[] y, double[] z, Boolean verbos)
		{
		if (x == null || y == null || z == null)
			{
			IJ.beep();
			IJ.log("no data for transform!");
			return null;
			}
		int npts = x.length;
		if (npts == 0)
			{
			IJ.beep();
			IJ.log("no data for in arrays!");
			return null;
			}
		if (npts != y.length || npts != z.length)
			{
			IJ.beep();
			IJ.log("data arrays don't match!");
			return null;
			}

		LLS lls = new LLS();
		int[] nc = new int[] {3};
		lls.setFunctionType (LLS.GENERIC_LINEAR_FUNCTION,nc,null);
		double[][] data = new double[x.length][3];
		for (int i=0; i < x.length; i++)
			{
			data[i][0] = 1.0;
			data[i][1] = x[i];
			data[i][2] = y[i];
			}
		double[] c = lls.fit (data,z,null,lls);
		if (verbos) IJ.log(lls.results (data,z,null,lls,null));
		return c;
		}

	protected void createNewWCS()
		{
		int npts = xWork.length;
		int nx = workImage.getWidth();
		int ny = workImage.getHeight();
		double DEGRAD = 180./Math.PI;

		WCS workWCS = new WCS(workImage);
		WCS refWCS  = new WCS(refImage);
		WCS wcs     = new WCS(2);		// HAS LONPOLE=180deg, PC=unity

		double[] crpix = workWCS.getCRPIX();
		if (verbose)
			IJ.log("work CRPIX="+crpix[0]+", "+crpix[1]);

		// ESTIMATE CRVAL'S
		double[] crval = new double[2];
		double[] xw = new double[npts];	
		double[] yw = new double[npts];
		double[] xy = null;
		for (int i=0; i < npts; i++)
			{
			xy = workWCS.imagej2fits(xWork[i],yWork[i],true);
			xw[i] = xy[0];		// FITS PIXELS W.R.T. CRPIX
			yw[i] = xy[1];
			// IJ.log(""+i+":");
			// IJ.log("\txi,yi="+xWork[i]+","+yWork[i]);
			// IJ.log("\txw,yw="+xw[i]+","+yw[i]);
			// IJ.log("\ta,d="+aRef[i]+","+dRef[i]);
			}
		double[] ca = transform(xw,yw,aRef,verbose);	// RA(xw,yw)
		double[] cd = transform(xw,yw,dRef,verbose);	// DEC(xy,yw)
		if (ca == null || cd == null)
			{
			IJ.error("Cannot compute ca,cd transformations!");
			return;
			}
		crval[0] = ca[0];	// CRVAL1 is RA(0,0)
		crval[1] = cd[0];	// CRVAL2 IS DEC(0,0)
		if (verbose)
			IJ.log("Estimated CRVAL:"+crval[0]+", "+crval[1]);

		// COMBINE INFO INTO NEW WCS
		wcs.setNAXIS(nx,ny);
		wcs.setCRPIX(crpix);
		wcs.setCRVAL(crval);
		wcs.setCDELT(workWCS.getCDELT());
		wcs.setCTYPE(refWCS.getCTYPE());
		wcs.setCUNIT(refWCS.getCUNIT());

		// CALCULATE NEW PIXEL POSITIONS USING NEW WCS WITH NO ROTATION!
		double[] xnew = new double[npts];
		double[] ynew = new double[npts];
		for (int i=0; i < npts; i++)
			{
			xy =  wcs.imagej2fits(wcs.wcs2pixels(aRef[i],dRef[i]),true);	// PIXELS W.R.T. CRPIX
			if (xy == null)
				{
				IJ.log("Cannot calculate pixels for roi #"+i+", RA="+aRef[i]+", DEC="+dRef[i]);
				return;
				}
			xnew[i] = xy[0];
			ynew[i] = xy[1];
			// IJ.log(""+i+": "+xw[i]+","+yw[i]+" "+xnew[i]+","+ynew[i]);
			}

		// CALCULATE PC MATRIX
		double[] cx = transform(xw,yw,xnew,verbose);
		double[] cy = transform(xw,yw,ynew,verbose);
		if (cx == null || cy == null)
			{
			IJ.error("Cannot compute cx,cy transformations!");
			return;
			}
		double[][] pc = new double[2][2];
		pc[0][0] = cx[1]; pc[0][1] = cx[2];
		pc[1][0] = cy[1]; pc[1][1] = cy[2];
		wcs.setPC(pc);
		if (verbose)
			IJ.log("Estimated PC matrix: [["+pc[0][0]+", "+pc[0][1]+"]["+pc[1][0]+", "+pc[1][1]+"]]");

		double[] result = wcs.pixels2wcs(crpix[0],crpix[1]);
		wcs.setCRVAL(result);
		if (verbose)
			IJ.log("Estimated CRVAL:"+result[0]+", "+result[1]);

		// APPLY RESULT
		// wcs.log("Combined WCS:");
		wcs.saveFITS(workImage);
		}
	}
