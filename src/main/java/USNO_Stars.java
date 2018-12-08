// USNO_Stars.java

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
 * Calls up a catalogue of USNO stars for the current image and optionally measures them.
 *
 * @version 1.1
 * @date 2012-Oct-06
 * @author F. Hessman (Göttingen)
 */
public class USNO_Stars implements PlugInFilter, AmoebaFunction
	{
	public ImagePlus img;
	protected ImageCanvas canvas;
	protected OverlayCanvas ocanvas;
	protected WCS wcs = null;

	protected String[] header = null;
	protected String ra = "12:34:56.7";
	protected String dec = "+76:54:32.1";
	protected double radius = 10.0;		// ARCMINUTES
	protected double bright = 8.0;		// BRIGHT LIMIT IN MAGN
	protected double faint  = 16.0;		// FAINT  LIMIT IN MAGN

	protected String[] catalog = null;
	protected Boolean fillTable = false;

	protected String[] labels = null;
 
	static String IDENT = "Identifier";
	static String RA = "R.A.[deg]";
	static String DEC = "Decl.[deg]";
	static String RMAG = "R[mag]";
	static String BMAG = "B[mag]";
	static String XPIXEL = "X";
	static String YPIXEL = "Y";
	static String WGT = "RawWeight";

	// protected String url1 = "http://archive.eso.org/skycat/servers/usnoa_res?catalogue=usnoa&epoch=2000.0&chart=0&format=2";
	protected String url1 = "http://archive.eso.org/skycat/servers/usnoa?catalogue=usnoa&epoch=2000.0&chart=0&format=0";
	protected String url2 = "&ra=";
	protected String url3 = "&dec=";
	protected String url4 = "&magbright=";
	protected String url5 = "&magfaint=";
	protected String url6 = "&radmax=";

	Centroid centre = null;
	protected double aper = 20.;

	protected double[] xPix;
	protected double[] yPix;
	protected double[] xWCS;
	protected double[] yWCS;
	protected double[] wgts;
	protected double[] rmags;
	protected double[] bmags;
	protected String[] names;
	protected int npts=0;

	protected Boolean measure  = false;
	protected Boolean refine   = false;
	protected Boolean fitCDELT = false;
	protected Boolean fitCRPIX = false;

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

		if (!getRaDecRadius() || !dialog())
			{
			img.unlock();
			return;
			}
		if (getCatalog() && parseCatalog(ip) && refine)
			refineWCS(img);
		}

	/**
	 * Gets rough RA,DEC from the FITS header of the image.
	 */
	protected boolean getRaDecRadius ()
		{
		header = FitsJ.getHeader(img);
		if (header == null || header.length == 0)
			return false;
		else	{
			int ny = this.img.getHeight();	// PIXELS
			double scale = 0.3/60.;		// ARCMIN/PIXEL
			int card = FitsJ.findCardWithKey("CDELT2",header);
			if (card > 0)
				{
				scale = FitsJ.getCardDoubleValue(header[card])*60.;
				radius = 1.5*ny*scale;
				}

			int racard = FitsJ.findCardWithKey("RA",header);
			int deccard = FitsJ.findCardWithKey("DEC",header);
			if (racard > 0 && deccard > 0)
				{
				ra = FitsJ.getCardStringValue(header[racard]).trim();
				dec = FitsJ.getCardStringValue(header[deccard]).trim();
				}
			else	{
				racard = FitsJ.findCardWithKey("CRVAL1",header);
				deccard = FitsJ.findCardWithKey("CRVAL2",header);
				int typcard = FitsJ.findCardWithKey("CTYPE1",header);
				if (racard > 0 && deccard > 0 && typcard > 0)
					{
					ra = FitsJ.getCardStringValue(header[racard]).trim();
					dec = FitsJ.getCardStringValue(header[deccard]).trim();
					String typ = FitsJ.getCardStringValue(header[typcard]).trim();
					if (typ.startsWith("RA"))
						{
						try	{
							double rad = Double.parseDouble(ra)/15.;
							ra = dms(rad);
							double decd = Double.parseDouble(dec);
							dec = dms(decd);
							}
						catch (NumberFormatException e)
							{
							IJ.error(e.getMessage());
							return false;
							}
						}
					}
				}
			}
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
		Boolean diddle = refine | fitCDELT | fitCRPIX;
		GenericDialog gd = new GenericDialog("USNO Query");
		gd.addStringField(RA,ra,12);
		gd.addStringField(DEC,dec,12);
		gd.addNumericField("Search radius [arcmin]",radius,2);
		gd.addNumericField("Bright limit [mag]",bright,2);
		gd.addNumericField("Faint  limit [mag]",faint,2);
		gd.addCheckbox("Save catalogue in table",fillTable);
		gd.addCheckbox("Measure all stars",measure);
		gd.addNumericField(".. with aperture [pixels]",aper,0);
		gd.addCheckbox("Refine...",diddle);
		gd.addCheckbox("... WCS",refine);
		gd.addCheckbox("... CDELT",fitCDELT);
		gd.addCheckbox("... CRPIX",fitCRPIX);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		ra = gd.getNextString().trim();
		dec = gd.getNextString().trim();
		radius = (int)gd.getNextNumber();
		bright = gd.getNextNumber();
		faint  = gd.getNextNumber();
		fillTable = gd.getNextBoolean();
		measure = gd.getNextBoolean();
		aper = (int)gd.getNextNumber();
		diddle = gd.getNextBoolean();
		refine = gd.getNextBoolean();
		fitCDELT = gd.getNextBoolean();
		fitCRPIX = gd.getNextBoolean();
		if (! diddle)
			{
			refine = false;
			fitCDELT = false;
			fitCRPIX = false;
			}
		return true;
		}

	/**
	 * Gets the USNO stars matching the image from ESO.
	 */
	protected boolean getCatalog()
		{
		String query = url1+url2+ra.replace(":","+")+url3+dec.replace(":","+")+url4+bright+url5+faint+url6+radius;
		IJ.log("ESO Query: "+query);
		String ht = new String(new char[] {9});
		try	{
			URL eso = new URL(query);
			InputStream stream = null;
			try	{
				stream = eso.openStream();
				}
			catch (IOException e)
				{
				IJ.error("Cannot open stream to ESO server!");
				return false;
				}
			BufferedReader out = new BufferedReader(new InputStreamReader(stream));
			String answer = "";
			String chunk = "";
			Boolean reached = false;
			while ((chunk = out.readLine()) != null)
				{
				if (chunk.contains("</pre>")) reached = false;
				if (reached && !chunk.equals("") && !chunk.contains("<b>"))
					{
					if (chunk.contains("</b>"))
						answer += chunk.substring(chunk.lastIndexOf(">")+1).trim()+"\n".replace(ht," ");
					else if (chunk.contains("RA2000"))
						labels = chunk.trim().replace(ht," ").replace("   "," ").replace("  "," ").split(" ");
					else
						answer += chunk.replace(ht," ").trim()+"\n";
					// IJ.log("["+chunk+"]");
					}
				if (chunk.contains("<pre>")) reached = true;
				}
			out.close();
			catalog = answer.split("\n");
			// IJ.log("Catalogue:\n"+answer+"\n(length="+catalog.length+")");
			return true;
			}
		catch (Exception e)
			{
			IJ.beep();
			IJ.error("Can't read ESO skycat response!\n:"+e.getMessage());
			return false;
			}
		}

	/**
	 * Parses the ESO output and displays the stars.
	 */
	protected Boolean parseCatalog(ImageProcessor ip)
		{
		MeasurementTable table = null;
		int nx = img.getWidth();
		int ny = img.getHeight();

		xPix  = new double[catalog.length];
		yPix  = new double[catalog.length];
		xWCS  = new double[catalog.length];
		yWCS  = new double[catalog.length];
		wgts  = new double[catalog.length];
		rmags = new double[catalog.length];
		bmags = new double[catalog.length];
		names = new String[catalog.length];
		npts = 0;

		if (canvas == null)
			{
                	canvas = img.getCanvas();
                	ocanvas = OverlayCanvas.getOverlayCanvas (img);
			ocanvas.clearRois();
			}

		double avg = 0.0;
		double ra = 0.0;
		double dec = 0.0;
		double rmag = 0.0;
		double bmag = 0.0;
		double w = 0.0;
		String name = "";
		for (int k=0; k < catalog.length; k++)
			{
			String entry = catalog[k].trim().replace("   "," ").replace("  "," ");
			String[] parts = entry.split(" ");
			// IJ.log(entry.replace(" ","_")+" ("+parts.length+") "+(int)entry.charAt(2));

			if (parts.length > 5)
				{
				w = 0.0;
				try	{
					name = parts[1];
					ra  = Double.parseDouble(parts[2]);	// IN DEG
					dec = Double.parseDouble(parts[3]);	// IN DEG
					rmag = Double.parseDouble(parts[4]);
					bmag = Double.parseDouble(parts[5]);
					}
				catch (NumberFormatException e)
					{
					IJ.beep();
					IJ.log(e.getMessage());
					IJ.log("Cannot parse USNO catalog entry: "+catalog[k]);
					// return false;
					}
				double[] rd = new double[] {ra,dec};
				double[] xy = wcs.wcs2pixels(rd);
				double x = xy[0];
				double y = xy[1];
				// IJ.log("Parsed USNO catalog entry: "+catalog[k]);

				// POSITION
				if (x > 5 && x < nx-5 &&
				    y > 5 && y < ny-5)
					{
					rmags[npts] = rmag;
					bmags[npts] = bmag;
					names[npts] = new String(name);
					xWCS[npts] = ra;
					yWCS[npts] = dec;
					if (measure)
						{
       	         				centre.setPosition (x,y);
       	                	 		if (!centre.measureXYR (ip, x,y,aper))
							IJ.beep();
						else	{
							x = centre.x();
							y = centre.y();
							w = 2.+Math.abs(centre.signal());
							}
						xPix[npts] = x;
						yPix[npts] = y;
						wgts[npts] = w;
						}
					else	{
						xPix[npts] = x;
						yPix[npts] = y;
						wgts[npts] = 0.0;
						}
					npts++;
					ApertureRoi roi = new ApertureRoi (x,y,aper,aper,aper,rmag,Color.green);
					roi.setImage (img);
					roi.setWCS(rd, new String[] {"deg     ","deg     "});
					ocanvas.add (roi);
					ocanvas.repaint();
					}
				}
			}

		if (npts == 0)	// && refine)
			{
			IJ.log ("Catalogue:");
			for (int k=0; k < catalog.length; k++)
				IJ.log (catalog[k].trim().replace("   "," ").replace("  "," "));
			IJ.error("No measureable points!");
			return false;
			}

		// FIND WGTS
		avg /= npts;
		// IJ.log("Here goes....!"+fillTable+","+npts);
		for (int i=0; i < npts; i++)
			{
			wgts[i] /= avg;
			if (wgts[i] < 0.1) wgts[i] = 0.1;
			if (wgts[i] > 10.0) wgts[i] = 10.0;
			if (fillTable)
				{
				if (table == null)
					{
					IJ.log("Creating USNO catalogue table...");
					table = new MeasurementTable("USNO Catalogue");
					}
				table.incrementCounter();
				table.addLabel (names[i]);
				table.addValue (RA,     xWCS[i]);
				table.addValue (DEC,    yWCS[i]);
				table.addValue (RMAG,   rmags[i]);
				table.addValue (BMAG,   bmags[i]);
				table.addValue (XPIXEL, xPix[i]);
				table.addValue (YPIXEL, yPix[i]);
				table.addValue (WGT,    wgts[i]);
				}
			// IJ.log(""+i+" "+xPix[i]+","+yPix[i]+", "+xWCS[i]+","+yWCS[i]+" "+wgts[i]);
			}
		if (fillTable && table != null) table.show();
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
			chisqr += (xy[0]-x)*(xy[0]-x)+(xy[1]-y)*(xy[1]-y);	// * wgts[i] !!!
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
		simplex.optimize(pars, 1.e-8, 10000,100);
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
