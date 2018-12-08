// Roi_Astrometry.java

import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Random;

import astroj.*;

/**
 * Uses the Rois of the current image, measures them and produces a WCS.
 *
 * @version 1.1
 * @date 2012-Nov-20
 * @author F. Hessman (Goettingen)
 */
public class Roi_Astrometry implements PlugInFilter, AmoebaFunction
	{
	public ImagePlus img;
	protected ImageCanvas canvas;
	protected OverlayCanvas ocanvas;
	protected WCS wcs = null;

	protected String[] header = null;
	protected double radius = 10.0;		// ARCMINUTES

	Centroid centre = null;
	protected double aper = 20.;

	protected double[] xPix;
	protected double[] yPix;
	protected double[] xWCS;
	protected double[] yWCS;
	protected double[] wgts;
	protected double[] mags;

	protected int npts=0;

	protected Boolean measure  = true;
	protected Boolean fitCDELT = false;
	protected Boolean fitCRPIX = true;

	public int setup(String arg, ImagePlus img)
		{
		this.img = img;
		return DOES_ALL;
		}

	public void run(ImageProcessor ip)
		{
		wcs = new WCS(img);
		centre = new Centroid(true);
		centre.setPositioning (true);
                centre.forgiving = true;
		if (!getHeader() || !getRois() || !dialog())
			{
			img.unlock();
			return;
			}
		recenter(ip);
		refineWCS(img);
		}

	/**
	 * Gets rough radius from the FITS header of the image.
	 */
	protected boolean getHeader ()
		{
		header = FitsJ.getHeader(img);
		if (header == null || header.length == 0)
			return false;
		return true;
		}

	/**
	 * Converts decimal coordinates into hexadecimal Strings.
	 */
	public String dms (double degs)
		{
		String sgn = "+";
		if (degs < 0.) sgn = "-";
		double adegs = Math.abs(degs);
		int d = (int)adegs;
		int m = (int)((adegs-d)*60.);
		double s = (adegs-d-m/60.)*3600.;
		String mm = ""+m;
		if (m < 10) mm = "0"+m;
		String ss = ""+s;
		if (s < 10.) ss = "0"+s;
		return sgn+d+":"+mm+":"+ss;
		}

	/**
	 * Dialog so that the user can change the search field size and
	 * determine if the USNO stars should be used to obtain a new
	 * astrometric solution.
	 */
	protected boolean dialog()
		{
		GenericDialog gd = new GenericDialog("Roi Astrometry");
		gd.addCheckbox("Re-measure all stars",measure);
		gd.addNumericField(".. with aperture [pixels]",aper,0);
		gd.addCheckbox("Refine CDELT",fitCDELT);
		gd.addCheckbox("Refine CRPIX",fitCRPIX);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		measure = gd.getNextBoolean();
		aper = (int)gd.getNextNumber();
		fitCDELT = gd.getNextBoolean();
		fitCRPIX = gd.getNextBoolean();
		return true;
		}

	/**
	 * chi**2 function used by Amoeba.
	 */
	public double userFunction (double[] p, double nix)
		{
		double chisqr = 0.0;
		double[] xy;
		for (int i=0; i < npts; i++)
			{
			double x = xPix[i];		// ImageJ PIXELS
			double y = yPix[i];
			double alpha = xWCS[i];
			double delta = yWCS[i];
			xy = wcs2xy(alpha,delta,p);
			chisqr += ( (xy[0]-x)*(xy[0]-x)+(xy[1]-y)*(xy[1]-y) );	// * wgts[i] !!!
			}
		return chisqr/(2*npts);
		}

	/**
	 * Converts RA,DEC into ImageJ x,y-pixel positions.
	 */
	double[] wcs2xy (double alpha, double delta, double[] p)
		{
		double[] crval = new double[] {p[0],p[1]};			// FIT CRVAL
		wcs.setCRVAL(crval);
		double[][] pc  = new double[][] {{p[2],p[3]},{p[4],p[5]}};	// FIT PC MATRIX
		wcs.setPC(pc);
		int nc = 6;
		if (fitCDELT)
			{
			double[] cdelt = new double[] {p[nc],p[nc+1]};			// FIT CDELT
			wcs.setCDELT(cdelt);
			nc += 2;
			}
		if (fitCRPIX)
			{
			double[] crpix = new double[] {p[nc],p[nc+1]};			// FIT CDELT
			wcs.setCRPIX(crpix);
			nc += 2;
			}
		return wcs.wcs2pixels(alpha,delta);
		}

	/**
	 * Gets all ApertureRoi measurements from the current OverlayCanvas.
	 */
	public boolean getRois()
		{
		ocanvas = OverlayCanvas.getOverlayCanvas(img);
		if (ocanvas == null)
			{
			IJ.error("Cannot access overlay!");
			return false;
			}
		Roi[] rois = ocanvas.getRois();
		if (rois == null || rois.length == 0)
			{
			IJ.error("No overlay rois!");
			return false;
			}

		int n = 0;
		for (int i=0; i < rois.length; i++)
			{
			if (rois[i] instanceof ApertureRoi) n += 1;
			}
		if (n == 0)
			{
			IJ.error("No Roi measurements!");
			return false;
			}
		xPix = new double[n];
		yPix = new double[n];
		xWCS = new double[n];
		yWCS = new double[n];
		wgts = new double[n];
		npts = 0;
		double[] radii = null;

		for (int i=0; i < rois.length; i++)
			{
			if (rois[i] instanceof ApertureRoi)
				{
				ApertureRoi roi = (ApertureRoi)rois[i];
				double[] pos = roi.getCenter();
				double[] wpos = roi.getWCS();
				xPix[npts] = pos[0];
				yPix[npts] = pos[1];
				xWCS[npts] = wpos[0];
				yWCS[npts] = wpos[1];
				wgts[npts] = 1.0;
				npts += 1;

				radii = roi.getRadii();				
				}
			}
		aper *= radii[0]/aper;
		return true;
		}

	/**
	 * Recenters the ROIs.
	 */
	protected void recenter (ImageProcessor ip)
		{
		for (int i=0; i < npts; i++)
			{
			double x = xPix[i];
			double y = yPix[i];
			centre.setPosition (x,y);
			if (!centre.measureXYR (ip, x,y,aper))
				IJ.beep();
			else    {
				xPix[i] = centre.x();
				yPix[i] = centre.y();
				// w = 2.+Math.abs(centre.signal());
				}
			}
		}

	/**
	 * Takes RA,DEC from USNO stars and refined pixel positions to get a better astrometric soluion.
	 */
	protected void refineWCS (ImagePlus img)
		{
		Random rand = new Random();
		Amoeba simplex = new Amoeba();
		simplex.setFunction(this);

		// INITIAL GUESS OF PARAMETERS
		int npar = 6;
		int n = npar;
		if (fitCDELT) npar += 2;
		if (fitCRPIX) npar += 2;
		double[] par = new double[npar];

		double[] crval = wcs.getCRVAL();
		par[0] = crval[0]; par[1] = crval[1];

		double[][] pc  = wcs.getPC();
		par[2] = pc[0][0]; par[3] = pc[0][1]; par[4] = pc[1][0]; par[5] = pc[1][1];

		double[] cdelt = new double[] {0.,0.};
		if (fitCDELT)
			{
			cdelt = wcs.getCDELT();
			par[n] = cdelt[0]; par[n+1] = cdelt[1];
			n += 2;
			}
		double[] crpix = new double[] {0.,0.};
		if (fitCRPIX)
			{
			crpix = wcs.getCRPIX();
			par[n] = crpix[0]; par[n+1] = crpix[1];
			n += 2;
			}

		// CREATE SIMPLEX PARAMETER SETS
		double[][] pars = new double[npar+1][npar];
		for (int j=0; j < npar+1; j++)
			{
			n = 6;
			pars[j][0] = par[0]+20.*rand.nextGaussian()/3600./Math.cos(Math.PI*par[1]/180.);
			pars[j][1] = par[1]+20.*rand.nextGaussian()/3600.;

			pars[j][2] = par[2]+0.1*rand.nextGaussian();	// ROTATION MATRIX
			pars[j][3] = par[3]+0.1*rand.nextGaussian();
			pars[j][4] = par[4]+0.1*rand.nextGaussian();
			pars[j][5] = par[5]+0.1*rand.nextGaussian();

			if (fitCDELT)	// SCALE
				{
				pars[j][n]   = par[n]  *(1.+0.1*Math.random());
				pars[j][n+1] = par[n+1]*(1.+0.1*Math.random());
				n += 2;
				}
			if (fitCRPIX)	// PIXEL CENTER
				{
				pars[j][n]   = par[n]  +10.*rand.nextGaussian();
				pars[j][n+1] = par[n+1]+10.*rand.nextGaussian();
				n += 2;
				}
			}

		IJ.log("\nStarting solution: "+this.userFunction(par,0.0));
		for (int i=0; i < npar; i++)
			IJ.log("\tp["+i+"]="+par[i]);
		simplex.optimize(pars, 1.e-6, 20000,100);
		par = simplex.solution();
		IJ.log("\nFinal solution: "+this.userFunction(par,0.0));
		for (int i=0; i < npar; i++)
			IJ.log("\tp["+i+"]="+par[i]);

		GFormat gf = new GFormat("7.2");
		IJ.log("\nDetailed fits to data:\n\tNo.\tX\tY\tXfit\tYfit\tRA\tDEC\tdX\tdY");
		for (int i=0; i < npts; i++)
			{
			double x = xPix[i];		// ImageJ PIXELS
			double y = yPix[i];
			double alpha = xWCS[i];
			double delta = yWCS[i];
			double[] xy = wcs2xy(alpha,delta,par);
			IJ.log("\t"+i+"\t"+gf.format(x)+"\t"+gf.format(y)+"\t"+gf.format(xy[0])+"\t"+gf.format(xy[1])+"\t"+alpha+"\t"+delta+"\t"+gf.format(xy[0]-x)+"\t"+gf.format(xy[1]-y));
			}

		// SAVE PARAMETERS

		crval[0] = par[0]; crval[1] = par[1];
		wcs.setCRVAL(crval);
		pc[0][0] = par[2]; pc[0][1] = par[3]; pc[1][0] = par[4]; pc[1][1] = par[5];
		wcs.setPC(pc);
		n = 6;
		if (fitCDELT)
			{
			cdelt[0] = par[n]; cdelt[1] = par[n+1];
			wcs.setCDELT(cdelt);
			n += 2;
			}
		if (fitCRPIX)
			{
			crpix[0] = par[n]; crpix[1] = par[n+1];
			wcs.setCRPIX(crpix);
			n += 2;
			}
		wcs.saveFITS(img);

		// SHOW NEW POSITIONS

		IJ.log("\tFinal positions shown in red.");
		for (int i=0; i < npts; i++)
			{
			double x = xPix[i];		// ImageJ PIXELS
			double y = yPix[i];
			double alpha = xWCS[i];
			double delta = yWCS[i];
			double[] xy = wcs.wcs2pixels(alpha,delta);
			OvalRoi roi = new OvalRoi ((int)(xy[0]-aper),(int)(xy[1]-aper),2*(int)aper,2*(int)aper);
			roi.setColor(Color.red);
			roi.setImage (img);
			ocanvas.add (roi);
			ocanvas.repaint();
			}
		}
	}
