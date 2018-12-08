// WCS.java

package astroj;

import java.awt.*;
import java.util.*;

import ij.*;

/**
 * A simple WCS object for the world coordinate systems used in most astronomical FITS files.
 * Uses the FitsJ class methods.
 *
 * The processing follows the general guidelines in
 *	Greisen, E.W. & Calabretta M.R. 2002, Astron. & Astrophys. Vol. 395, 1061-1075
 *	Calabretta M.R. & Greisen, E.W. 2002, Astron. & Astrophys. Vol. 395, 1077-1122
 *
 * Modifications by K. Collins 7/2010
 * Fixed three issues related to proper RA and DEC calculations for the TAN projection
 * - The "RA---TAN-SIP" extension caused problems with the "-TAN" substring matching
 *		changed: int i = CTYPE[0].lastIndexOf("-");
 *			 projection = CTYPE[0].substring(i+1);
 *		to:      projection = CTYPE[0].substring(5, 8);
 *
 * - Reversed the indices when loading the PC and CD Matrix coefficients
 *		changed: CD[k-1][i-1]  and PC[k-1][i-1]
 *		to:      CD[i-1][k-1]  and PC[i-1][k-1]
 *
 * - Reversed the order of the arguments in one statement when calculating the TAN projection
 *		changed: s[0] = DEGRAD*Math.atan2(-x[1],x[0]);
 *		to:      s[0] = DEGRAD*Math.atan2(x[0],-x[1]);
 *
 * - Added code to support the SIP non-linear distortion corrections following
 *		SHUPE, D.L. ET AL. 2008
 *
 * Modifications by F. Hessman 9/2012
 * - Renamed NAXIS to NAXES to avoid confusion with FITS keywords NAXISi
 * - Added wcs2pixel(), including support for inverse SIP corrections
 * - Added get and set methods for standard FITS WCS quantities
 * - Removed support for clumsy and vague WCS preferences.
 *
 */
public class WCS
	{
	int NAXES=0;		// CALLED "NAXIS"  IN FITS
	int WCSAXES=0;
	int[] NAXIS;		// CALLED "NAXIXi" IN FITS
	double[] CDELT;		// CALLED "CDELTi" IN FITS
	double[] CRPIX;		// CALLED "CRPIXi" IN FITS
	double[] CRVAL;		// CALLED "CRVALi" IN FITS
	String[] CTYPE = null;	// CALLED "CTYPEi" IN FITS
	String[] CUNIT = null;	// CALLED "CUNITi" IN FITS
	double[][] PC;		// CALLED "PCi_j"  IN FITS
	double[][] CD;		// CALLED "CDi_j"  IN FITS
	double[][] PCinv = null;
	double[][] CDinv = null;
	double LONPOLE = 180.;	// DEGREES

	double[][] A = null;
	double[][] B = null;
	int A_ORDER = -1;
	int B_ORDER = -1;
	boolean useSIPA=false;
	boolean useSIPB=false;

	double[][] AP = null;
	double[][] BP = null;
	int AP_ORDER = -1;
	int BP_ORDER = -1;
	boolean useSIPAP=false;
	boolean useSIPBP=false;

	public String coordsys;
	String projection;
	boolean useCD=false;
	boolean hasRADEC=false;
	boolean typeContainsSIP = false;
	boolean enoughInfo=false;
	public String logInfo = new String("");

	// public static String PREFS_NPIX1    = new String("wcs.npix1");
	// public static String PREFS_NPIX2    = new String("wcs.npix2");
	public static String PREFS_CRPIX1   = new String("wcs.crpix1");
	public static String PREFS_CRPIX2   = new String("wcs.crpix2");
	public static String PREFS_CRVAL1   = new String("wcs.crval1");
	public static String PREFS_CRVAL2   = new String("wcs.crval2");
	public static String PREFS_CDELT1   = new String("wcs.cdelt1");
	public static String PREFS_CDELT2   = new String("wcs.cdelt2");
	public static String PREFS_CTYPE1   = new String("wcs.ctype1");
	public static String PREFS_CTYPE2   = new String("wcs.ctype2");
	public static String PREFS_CD1_1    = new String("wcs.cd11");
	public static String PREFS_CD1_2    = new String("wcs.cd12");
	public static String PREFS_CD2_1    = new String("wcs.cd21");
	public static String PREFS_CD2_2    = new String("wcs.cd22");
	public static String PREFS_PC1_1    = new String("wcs.pc11");
	public static String PREFS_PC1_2    = new String("wcs.pc12");
	public static String PREFS_PC2_1    = new String("wcs.pc21");
	public static String PREFS_PC2_2    = new String("wcs.pc22");

	// public static String PREFS_USENPIX  = new String("wcs.usenpix");
	public static String PREFS_USECRPIX = new String("wcs.usecrpix");
	public static String PREFS_USECRVAL = new String("wcs.usecrval");
	public static String PREFS_USECDELT = new String("wcs.usecdelt");
	public static String PREFS_USECTYPE = new String("wcs.usectype");
	public static String PREFS_USECD    = new String("wcs.usecd");
	public static String PREFS_USEPC    = new String("wcs.usepc");

	Boolean useCDELTprefs = false;
	Boolean useCRPIXprefs = false;
	Boolean useCTYPEprefs = false;
	Boolean useCDprefs    = false;
	Boolean usePCprefs    = false;

	public WCS (int naxes)
		{
		initialize(naxes);
		// checkPreferences();
		}

	public WCS (ImagePlus img)
		{
		NAXES=0;
		String[] hdr = FitsJ.getHeader (img);
		if (hdr == null) return;
		process(img.getShortTitle(),img.getWidth(),img.getHeight(),img.getStackSize(),hdr);
		// checkPreferences();
		}

	public WCS (String[] hdr)
		{
		NAXES=0;
		if (hdr == null) return;
		process(null,-1,-1,-1, hdr);
		// checkPreferences();
		}

/*
	** Get previously saved preferences in order to correct faulty FITS headers. *
	public void checkPreferences ()
		{
		String s1,s2,s3,s4;
		double d1,d2,d3,d4;

		useCDELTprefs = Prefs.getBoolean (WCS.PREFS_USECDELT,false);
		if (useCDELTprefs)
			{
			IJ.log("using CDELT prefs...");
			s1 = Prefs.get (WCS.PREFS_CDELT1, "8.8672e-5");
			s2 = Prefs.get (WCS.PREFS_CDELT2, "8.8672e-5");
			try	{
				d1 = Double.parseDouble(s1);
				d2 = Double.parseDouble(s2);
				CDELT = new double[] {d1,d2};
				}
			catch (NumberFormatException e)
				{
				IJ.beep();
				IJ.log("Unable to read CDELT preferences!");
				useCDELTprefs = false;
				}
			}
		else
			IJ.log("NOT using CDELT prefs...");

		useCRPIXprefs = Prefs.getBoolean (WCS.PREFS_USECRPIX,false);
		if (useCRPIXprefs)
			{
			s1 = Prefs.get (WCS.PREFS_CRPIX1, "1.");
			s2 = Prefs.get (WCS.PREFS_CRPIX2, "1.");
			try	{
				d1 = Double.parseDouble(s1);
				d2 = Double.parseDouble(s2);
				CRPIX = new double[] {d1,d2};
				}
			catch (NumberFormatException e)
				{
				IJ.beep();
				IJ.log("Unable to read CRPIX preferences!");
				useCRPIXprefs = false;
				}
			}

		useCTYPEprefs = Prefs.getBoolean (WCS.PREFS_USECTYPE,false);
		if (useCTYPEprefs)
			{
			s1 = Prefs.get (WCS.PREFS_CTYPE1, "RA---TAN");
			s2 = Prefs.get (WCS.PREFS_CTYPE2, "DEC--TAN");
			CTYPE = new String[] {s1,s2};
			projection = CTYPE[0].substring(5, 8);
			typeContainsSIP = false;
			if (CTYPE[0].length() >= 12)
				if (CTYPE[0].substring(9,12).equals("SIP"))
					typeContainsSIP = true;
			coordsys = CTYPE[0]+","+CTYPE[1];
			}

		useCDprefs = Prefs.getBoolean (WCS.PREFS_USECD,false);
		if (useCDprefs)
			{
			s1 = Prefs.get (WCS.PREFS_CD1_1, "1.0");
			s2 = Prefs.get (WCS.PREFS_CD1_2, "0.0");
			s3 = Prefs.get (WCS.PREFS_CD2_1, "0.0");
			s4 = Prefs.get (WCS.PREFS_CD2_2, "1.0");
			try	{
				d1 = Double.parseDouble(s1);
				d2 = Double.parseDouble(s2);
				d3 = Double.parseDouble(s2);
				d4 = Double.parseDouble(s2);
				CD = new double[][] {{d1,d2},{d3,d4}};
				CDinv = invert(CD);
				useCD = true;
				}
			catch (NumberFormatException e)
				{
				IJ.beep();
				IJ.log("Unable to read CD matrix preferences!");
				useCDprefs = false;
				}
			}

		usePCprefs = Prefs.getBoolean (WCS.PREFS_USEPC,false);
		if (usePCprefs)
			{
			s1 = Prefs.get (WCS.PREFS_PC1_1, "1.0");
			s2 = Prefs.get (WCS.PREFS_PC1_2, "0.0");
			s3 = Prefs.get (WCS.PREFS_PC2_1, "0.0");
			s4 = Prefs.get (WCS.PREFS_PC2_2, "1.0");
			try	{
				d1 = Double.parseDouble(s1);
				d2 = Double.parseDouble(s2);
				d3 = Double.parseDouble(s2);
				d4 = Double.parseDouble(s2);
				PC = new double[][] {{d1,d2},{d3,d4}};
				PCinv = invert(PC);
				useCD = false;
				}
			catch (NumberFormatException e)
				{
				IJ.beep();
				IJ.log("Unable to read PC matrix preferences!");
				usePCprefs = false;
				}
			}
		}
*/

	protected void process (String title, int nx, int ny, int nz, String[] hdr)
		{
		// FITS HEADER PRESENT?

		int icard = FitsJ.findCardWithKey ("SIMPLE",hdr);
		int jcard = FitsJ.findCardWithKey ("END",hdr);
		if (icard < 0 || jcard < 0) return;

		hasRADEC = false;

		// GET NUMBER OF AXES

		NAXES = -1;
		icard = FitsJ.findCardWithKey ("NAXIS",hdr);
		if (icard >= 0)
			NAXES = FitsJ.getCardIntValue (hdr[icard]);
		if (NAXES <= 0)
			{
			logInfo += "Can not read NAXIS keyword in FITS header of "+title+"\n";
			return;
			}

		// CHECK IF THE NUMBER OF WCS AXES IS KNOWN AND DIFFERENT

		icard = FitsJ.findCardWithKey ("WCSAXES",hdr);
		if (icard >= 0)
			{
			WCSAXES = FitsJ.getCardIntValue (hdr[icard]);
			NAXES = WCSAXES;
			}

		// GIVEN NUMBER OF WCS AXES, RE-INITIALIZE MATRICES, ETC.

		initialize (NAXES);

		// GET SIZES OF AXES

		for (int j=1; j <= NAXES; j++)
			{
			icard = FitsJ.findCardWithKey ("NAXIS"+j, hdr);
			if (icard < 0)
				{
				logInfo += "Cannot find keyword NAXIS"+j+" in FITS header of "+title+"\n";
				return;
				}
			NAXIS[j-1] = FitsJ.getCardIntValue (hdr[icard]);
			}

		// MAKE SURE THEY AGREE WITH THE ImageJ NUMBERS

		if (NAXES >= 1 && nx >= 1 && NAXIS[0] != nx)
			{
			logInfo += "Horizontal axis size="+nx+" does not match FITS header! ("+NAXIS[0]+")\n";
			return;
			}
		if (NAXES >= 2 && ny >= 1 && NAXIS[1] != ny)
			{
			logInfo += "Vertial axis size="+ny+" does not match FITS header! ("+NAXIS[1]+")\n";
			return;
			}
		if (NAXES >= 3 && nz >= 1 && NAXIS[2] != nz)
			{
			logInfo += "Stack size="+nz+" does not match FITS header! ("+NAXIS[2]+")\n";
			return;
			}

		// TRY TO FIND RA-- and DEC- COORDINATE TYPES

		CTYPE[0] = null;
		String typ = "";
		String prefix = "";
		String abc[] = new String[] {	"",
						"A","B","C","D","E","F","G","H","I","J","K","L","M",
						"N","O","P","Q","R","S","T","U","V","W","X","Y","Z" };
		for (int k=0; k < abc.length && prefix == null; k++)
			{
			icard = FitsJ.findCardWithKey ("CTYPE1"+abc[k], hdr);
			if (icard > 0)
				{
				typ = FitsJ.getCardStringValue(hdr[icard]);
				if (typ != null && (typ.startsWith("RA--")))
					{
					CTYPE[0] = typ;
					prefix = abc[k];
					projection = CTYPE[0].substring(5, 8);
					if (CTYPE[0].length() >= 12)
						if (CTYPE[0].substring(9,12).equals("SIP"))
							typeContainsSIP = true;
					break;
					}
				}
			}
		if (CTYPE[0] == null)	// NO "RA--" FOUND, SO TAKE WHAT YOU CAN GET
			{
			icard = FitsJ.findCardWithKey ("CTYPE1", hdr);
			if (icard > 0)
				{
				CTYPE[0] = FitsJ.getCardStringValue (hdr[icard]);
				projection = CTYPE[0].substring(5, 8);
				if (CTYPE[0].length() >= 12)
					if (CTYPE[0].substring(9,12).equals("SIP"))
						typeContainsSIP = true;
				}
			else	{
				logInfo += "Cannot define world coordinate system (CTYPE1): not enough information in FITS header!\n";
				return;
				}
			}

		// GET MATCHING COORDINATE TYPES

		for (int k=2; k <= NAXES; k++)
			{
			icard = FitsJ.findCardWithKey ("CTYPE"+k+prefix, hdr);
			if (icard > 0)
				CTYPE[k-1] = FitsJ.getCardStringValue(hdr[icard]);
			}

		// CHECK IF CTYPE2n IS "DEC" AND COORDINATE SYSTEMS MATCH

		if (CTYPE[0].startsWith("RA--") && CTYPE[1].startsWith("DEC-"))
			{
			hasRADEC=true;
			if (CUNIT == null)
				{
				CUNIT = new String[NAXES];
				CUNIT[0] = "deg     ";
				CUNIT[1] = "deg     ";
				}
			}

		// GET TRANSFORMATION COEFFICIENTS (SCALE, TRANSLATION)

		try	{
			for (int k=1; k <= NAXES; k++)
				{
				icard = FitsJ.findCardWithKey ("CDELT"+k+prefix, hdr);
				if (icard > 0)
					CDELT[k-1] = FitsJ.getCardDoubleValue(hdr[icard]);
				icard = FitsJ.findCardWithKey ("CRPIX"+k+prefix, hdr);
				if (icard > 0)
					CRPIX[k-1] = FitsJ.getCardDoubleValue(hdr[icard]);
				icard = FitsJ.findCardWithKey ("CRVAL"+k+prefix, hdr);
				if (icard > 0)
					CRVAL[k-1] = FitsJ.getCardDoubleValue(hdr[icard]);
				}
			}
		catch (NumberFormatException e)
			{
			logInfo += "Error reading standard transformation coefficients in FITS header!\n";
			return;
			}

		// GET LINEAR TRANSFORMATION COEFFICIENTS (ROTATION, SCALE, SKEWNESS)

		int ifound=0;
		try	{
			for (int k=1; k <= NAXES; k++)
				{
				for (int i=1; i <= NAXES; i++)
					{
					icard = FitsJ.findCardWithKey ("PC"+i+"_"+k+prefix, hdr);
					if (icard > 0)
						{
						PC[i-1][k-1] = FitsJ.getCardDoubleValue(hdr[icard]);
						ifound++;
						}
					icard = FitsJ.findCardWithKey ("CD"+i+"_"+k+prefix, hdr);
					if (icard > 0)
						{
						useCD=true;
						CD[i-1][k-1] = FitsJ.getCardDoubleValue(hdr[icard]);
						ifound++;
						}
					}
				}
			if (useCD)
				CDinv = invert(CD);
			else
				PCinv = invert(PC);
			if (CDinv == null && PCinv == null)
				{
				IJ.beep();
				IJ.log("Cannot invert WCS matrices!");
				}
			}
		catch (NumberFormatException e)
			{
			logInfo += "Cannot read transformation matrices PC/CD in FITS header!\n";
			return;
			}

		// IF AVAILABLE, GET SIP NON-LINEAR DISTORTION COEFFICIENTS
		// SEE SHUPE, D.L. ET AL., 2005, ASPC 347, 491 (http://adsabs.harvard.edu/abs/2005ASPC..347..491S) 
		if (typeContainsSIP)
			{
			try	{
				// LOOK FOR A AND B POLYNOMIAL ORDERS
				if (NAXES > 1)
					{
					A_ORDER = -1;
					icard = FitsJ.findCardWithKey ("A_ORDER",hdr);
					if (icard >= 0)
						{
						A_ORDER = FitsJ.getCardIntValue (hdr[icard]);
						if (A_ORDER < 2 || A_ORDER > 9)
							{
							A_ORDER = -1;
							logInfo += "SIP A_ORDER out of range in FITS header!\n";
							}
						}
					else
						logInfo += "SIP A_ORDER not found in FITS header!\n";

					B_ORDER = -1;
					icard = FitsJ.findCardWithKey ("B_ORDER",hdr);
					if (icard >= 0)
						{
						B_ORDER = FitsJ.getCardIntValue (hdr[icard]);
						if (B_ORDER < 2 || B_ORDER > 9)
							{
							B_ORDER = -1;
							logInfo += "SIP B_ORDER out of range in FITS header!\n";
							}
						}
					else
						logInfo += "SIP B_ORDER not found in FITS header!\n";

					AP_ORDER = -1;
					icard = FitsJ.findCardWithKey ("AP_ORDER",hdr);
					if (icard >= 0)
						{
						AP_ORDER = FitsJ.getCardIntValue (hdr[icard]);
						if (AP_ORDER < 2 || AP_ORDER > 9)
							{
							AP_ORDER = -1;
							logInfo += "SIP AP_ORDER out of range in FITS header!\n";
							}
						}
					else	{
						logInfo += "SIP AP_ORDER not found in FITS header!\n";
						logInfo += "Cannot perform correct RA,DEC->i,j tranformation!\n";
						}

					BP_ORDER = -1;
					icard = FitsJ.findCardWithKey ("BP_ORDER",hdr);
					if (icard >= 0)
						{
						BP_ORDER = FitsJ.getCardIntValue (hdr[icard]);
						if (BP_ORDER < 2 || BP_ORDER > 9)
							{
							BP_ORDER = -1;
							logInfo += "SIP BP_ORDER out of range in FITS header!\n";
							}
						}
					else	{
						logInfo += "SIP BP_ORDER not found in FITS header!\n";
						logInfo += "Cannot perform correct RA,DEC->i,j tranformation!\n";
						}
					}
				}
			catch (NumberFormatException e)
				{
				logInfo += "Cannot read SIP order keywords in FITS header!\n";
				A_ORDER = -1;
				B_ORDER = -1;
				AP_ORDER = -1;
				BP_ORDER = -1;
				}

			//INITIALIZE SIP MATRICES
			if (A_ORDER >= 2 && A_ORDER <= 9)
				{
				A = new double[A_ORDER+1][A_ORDER+1];
				for (int q=0; q <= A_ORDER; q++)
					{
					for (int p=0; p <= A_ORDER; p++)
						A[p][q] = 0.0;
					}
				}
			if (B_ORDER >= 2 && B_ORDER <= 9)
				{
				B = new double[B_ORDER+1][B_ORDER+1];
				for (int q=0; q <= B_ORDER; q++)
					{
					for (int p=0; p <= B_ORDER; p++)
						B[p][q] = 0.0;
					}
				}
			if (AP_ORDER >= 2 && AP_ORDER <= 9)
				{
				AP = new double[AP_ORDER+1][AP_ORDER+1];
				for (int q=0; q <= AP_ORDER; q++)
					{
					for (int p=0; p <= AP_ORDER; p++)
						AP[p][q] = 0.0;
					}
				}
			if (BP_ORDER >= 2 && BP_ORDER <= 9)
				{
				BP = new double[BP_ORDER+1][BP_ORDER+1];
				for (int q=0; q <= BP_ORDER; q++)
					{
					for (int p=0; p <= BP_ORDER; p++)
						BP[p][q] = 0.0;
					}
				}

			//LOOK FOR SIP X-COORDINATE MATRIX COEFICIENTS A_p_q IN FITS HEADER
			if (A_ORDER >= 2 && A_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= A_ORDER; q++)
						{
						for (int p=0; p <= A_ORDER; p++)
							{
							if (q + p <= A_ORDER)
								{
								icard = FitsJ.findCardWithKey ("A_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									A[p][q] = FitsJ.getCardDoubleValue(hdr[icard]);
//									IJ.write("A["+p+"]["+q+"] = "+A[p][q]);
									useSIPA=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read SIP matrix A in FITS header!\n";
					return;
					}
				}

			//LOOK FOR SIP Y-COORDINATE MATRIX COEFICIENTS B_p_q IN FITS HEADER
			if (B_ORDER >= 2 && B_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= B_ORDER; q++)
						{
						for (int p=0; p <= B_ORDER; p++)
							{
							if (q + p <= B_ORDER)
								{
								icard = FitsJ.findCardWithKey ("B_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									B[p][q] = FitsJ.getCardDoubleValue(hdr[icard]);
//									IJ.write("B["+p+"]["+q+"] = "+B[p][q]);
									useSIPB=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read SIP matrix B in FITS header!\n";
					return;
					}
				}

			//LOOK FOR SIP INVERSE X-COORDINATE MATRIX COEFICIENTS AP_p_q IN FITS HEADER
			if (AP_ORDER >= 2 && AP_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= AP_ORDER; q++)
						{
						for (int p=0; p <= AP_ORDER; p++)
							{
							if (q + p <= AP_ORDER)
								{
								icard = FitsJ.findCardWithKey ("AP_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									AP[p][q] = FitsJ.getCardDoubleValue(hdr[icard]);
//									IJ.write("AP["+p+"]["+q+"] = "+AP[p][q]);
									useSIPAP=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read SIP matrix AP in FITS header!\n";
					return;
					}
				}

			//LOOK FOR SIP INVERSE Y-COORDINATE MATRIX COEFICIENTS BP_p_q IN FITS HEADER
			if (BP_ORDER >= 2 && BP_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= BP_ORDER; q++)
						{
						for (int p=0; p <= BP_ORDER; p++)
							{
							if (q + p <= BP_ORDER)
								{
								icard = FitsJ.findCardWithKey ("BP_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									BP[p][q] = FitsJ.getCardDoubleValue(hdr[icard]);
//									IJ.write("BP["+p+"]["+q+"] = "+BP[p][q]);
									useSIPBP=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read SIP matrix BP in FITS header!\n";
					return;
					}
				}
			}

		// GET PROJECTION PARAMETERS, IF ANY

		LONPOLE = 180.0;// DEGS
		icard = FitsJ.findCardWithKey ("LONPOLE"+prefix, hdr);
		if (icard > 0)
			LONPOLE = FitsJ.getCardDoubleValue(hdr[icard]);

		// COORDINATE SYSTEM

		coordsys = CTYPE[0].trim()+","+CTYPE[1].trim();
		enoughInfo = true;
		return;
		}

	/**
	 * Inverts 2x2 matrices
	 */
	public double[][] invert (double[][] m)
		{
		if (m == null || m.length != 2)
			{
			IJ.beep();
			IJ.log("Cannot invert matrix");
			if (m != null) IJ.log("\tdim="+m.length);
			return null;
			}
		double detm = m[0][0]*m[1][1]-m[0][1]*m[1][0];
		if (detm == 0.0)
			{
			IJ.beep();
			IJ.log("WCS matrix is singular!");
			return null;
			}
		double[][] inv = new double[2][2];
		inv[0][0] =  m[1][1]/detm;
		inv[0][1] = -m[0][1]/detm;
		inv[1][0] = -m[1][0]/detm;
		inv[1][1] =  m[0][0]/detm;
		return inv;
		}

	public void initialize (int naxes)
		{
		NAXES = naxes;
		if (naxes == 0) return;

		NAXIS = new int[NAXES];
		CDELT = new double[NAXES];
		CRPIX = new double[NAXES];
		CRVAL = new double[NAXES];
		PC = new double[NAXES][NAXES];
		CD = new double[NAXES][NAXES];
		CTYPE = new String[NAXES];
		CUNIT = new String[NAXES];
		for (int i=0; i < NAXES; i++)
			{
			NAXIS[i]=0;
			CDELT[i]=1.0;
			CRPIX[i]=1.0;
			CRVAL[i]=0.0;
			PC[i][i]=1.0;
			CD[i][i]=1.0;
			}
		CDinv = invert(CD);
		PCinv = invert(PC);
		typeContainsSIP=false;
		useCD=false;
		useSIPA=false;
		useSIPB=false;
		useSIPAP=false;
		useSIPBP=false;
		coordsys = "";
		projection = "";
		}

	/**
	 * Converts 2-D ImageJ pixel positions to coordinate system values.
	 */
	public double[] pixels2wcs (double x, double y)
		{
		double[] xy = new double[2];
		xy[0] = x; xy[1] = y;
		return pixels2wcs(xy);
		}

	/**
	 * Converts ImageJ pixel positions to coordinate system values.
	 */
	public double[] pixels2wcs (double[] pixels)
		{
		return pix2wcs (pixels, true);
		}

	/**
	 * Converts ImageJ pixel positions to coordinate system values.
	 * This version lets the rotation/scaling using CD/PC matrices be optional.
	 */
	public double[] pix2wcs (double[] pixels, Boolean useCDPC)
		{
		if (! enoughInfo)
			{
			IJ.beep();
			IJ.log ("pix2wcd: not enough info: "+logInfo);
			return null;
			}

		int n=pixels.length;
		if (n != NAXES)
			{
			IJ.beep();
			IJ.log ("pix2wcd: number of axes doesn't match: n="+n+", NAXIS="+NAXES);
			return null;
			}

		// TRANSLATE ImageJ COORDINATES TO FITS COORDINATES, CORRECTING FOR CRPIX
		double[] p = imagej2fits(pixels,true);

		// NON-LINEAR DISTORTION CORRECTION USING SIP CORRECTION MATRICES (SEE SHUPE, D.L. ET AL. 2008)
		if (useSIPA)
			{
			//CALCULATE SIP X-COORDINATE CORRECTION
			double xCorrection = 0.0;
			for (int qq=0; qq <= A_ORDER; qq++)
				{
				for (int pp=0; pp <= A_ORDER; pp++)
					{
					if (pp + qq <= A_ORDER)
						xCorrection += A[pp][qq]*Math.pow(p[0],(double)pp)*Math.pow(p[1],(double)qq);
					}
				}
			p[0] += xCorrection;
			}
		if (useSIPB)
			{
			//CALCULATE SIP Y-COORDINATE CORRECTION
			double yCorrection = 0.0;
			for (int qq=0; qq <= B_ORDER; qq++)
				{
				for (int pp=0; pp <= B_ORDER; pp++)
					{
					if (pp + qq <= B_ORDER)
						yCorrection += B[pp][qq]*Math.pow(p[0],(double)pp)*Math.pow(p[1],(double)qq);
					}
				}
			p[1] += yCorrection;
			}

		// CORRECT FOR ROTATION, SKEWNESS, SCALE

		double[] x = new double[n];	// IN PSEUDO DEGREES
		if (useCD && useCDPC)
			{
			for (int j=0; j < n; j++)
				{
				x[j] = 0.;
				for (int i=0; i < n; i++)
					x[j] += CD[j][i]*p[i];
				}
			}
		else if (useCDPC)
			{
			for (int j=0; j < n; j++)
				{
				x[j] = 0.;
				for (int i=0; i < n; i++)
					x[j] += PC[j][i]*p[i];
				x[j] *= CDELT[j];
				}
			}
		else	{
			for (int j=0; j < n; j++)
				x[j] = p[j]*CDELT[j];
			}

		// PROJECTION PLANE COORDINATES x TO NATIVE SPHERICAL COORDINATES s WHERE
		// s[0] INCREASES TO EAST! (SEE FIG. 3 IN CALABRETTA & GREISEN 2002; NOTE
		// THEIR arg(-y,x) = atan2(x,-y)).

		double[] s = new double[n];	// IN PSEUDO DEGREES
		double DEGRAD = 180.0/Math.PI;
		for (int j=0; j < n; j++)
			s[j] = x[j];
		if (projection.equals("TAN"))
			{
			double Rtheta = Math.sqrt(x[0]*x[0]+x[1]*x[1]);
			s[0] = DEGRAD*Math.atan2(x[0],-x[1]);		// NATIVE phi (E.G. R.A.) IN DEGS
			s[1] = DEGRAD*Math.atan2(DEGRAD,Rtheta);	// NATIVE theta~90 DEG (E.G. DEC.) IN DEGS
			}

		// NATIVE SPHERICAL COORDINATES s TO CELESTIAL COORDINATES c : SEE EQN.2 OF Calabretta & Greisen 2002, P. 1079
		double[] c = new double[n];
		for (int j=0; j < n; j++)
			c[j] = s[j]+CRVAL[j];
		if (projection.equals("TAN"))
			{
			double phi = s[0]/DEGRAD;		// NATIVE PHI IN RADIANS
			double theta = s[1]/DEGRAD;		// NATIVE THETA IN RADIANS
			double phiPole = LONPOLE /DEGRAD;	// IN RADIANS
			double deltaPole = CRVAL[1]/DEGRAD;	// IN RADIANS
			double sintcosP = Math.sin(theta)*Math.cos(deltaPole);
			double costsinPcosdp = Math.cos(theta)*Math.sin(deltaPole)*Math.cos(phi-phiPole);
			double costsindp = Math.cos(theta)*Math.sin(phi-phiPole);
			double sintsinP = Math.sin(theta)*Math.sin(deltaPole);
			double costcosPcosdp = Math.cos(theta)*Math.cos(deltaPole)*Math.cos(phi-phiPole);
			c[0] = CRVAL[0]+Math.atan2(-costsindp, sintcosP-costsinPcosdp)*DEGRAD;
			c[1] = Math.asin(sintsinP+costcosPcosdp)*DEGRAD;
			}
		return c;
		}

	/**
	 * Converts coordinate system values to ImageJ pixel positions.
	 */
	public double[] wcs2pixels (double a, double d)
		{
		double[] ad = new double[2];
		ad[0] = a; ad[1] = d;
		return wcs2pixels(ad);
		}

	/**
	 * Converts coordinate system values to ImageJ pixel positions.
	 */
	public double[] wcs2pixels (double[] c)
		{
		return wcs2pix (c,true);
		}

	/**
	 * Converts coordinate system values to ImageJ pixel positions.
	 * This version has an optional CD/PC transformation.
	 */
	public double[] wcs2pix (double[] c, Boolean useCDPC)
		{
		if (! enoughInfo)
			{
			IJ.beep();
			IJ.log ("Not enough info: "+logInfo);
			return null;
			}

		int n=c.length;
		if (n != NAXES)
			{
			IJ.beep();
			IJ.log ("Number of axes doesn't match: n="+n+", NAXIS="+NAXES);
			return null;
			}

		// CELESTIAL c to NATIVE SPHERICAL s: EQN.6 OF Calabretta & Greisen 2002, P. 1079
		double DEGRAD = 180.0/Math.PI;
		double[] s = new double[n];
		for (int j=0; j < n; j++)
			s[j] = c[j];				// RA,DEC IN DEG
		if (projection.equals("TAN"))
			{
			double alpha = c[0]/DEGRAD;		// RA  IN RADIANS
			double delta = c[1]/DEGRAD;		// DEC IN RADIANS
			double phiPole = LONPOLE/DEGRAD;	// IN RADIANS
			double alphaPole = CRVAL[0]/DEGRAD;	// IN RADIANS
			double deltaPole = CRVAL[1]/DEGRAD;	// IN RADIANS
			double sindcosP = Math.sin(delta)*Math.cos(deltaPole);
			double cosdsinPcosda = Math.cos(delta)*Math.sin(deltaPole)*Math.cos(alpha-alphaPole);
			double cosdsinda = Math.cos(delta)*Math.sin(alpha-alphaPole);
			double sindsinP = Math.sin(delta)*Math.sin(deltaPole);
			double cosdcosPcosda = Math.cos(delta)*Math.cos(deltaPole)*Math.cos(alpha-alphaPole);
			double phi = phiPole+Math.atan2(-cosdsinda, sindcosP-cosdsinPcosda);
			double theta = Math.asin(sindsinP+cosdcosPcosda);
			s[0] = phi*DEGRAD;			// NATIVE PHI   IN DEG
			s[1] = theta*DEGRAD;			// NATIVE THETA IN DEG
			}

		// NATIVE SPHERICAL s TO PROJECTION PLANE x
		double[] x = new double[n];
		for (int j=0; j < n; j++)
			x[j] = s[j];				// IN DEGS
		if (projection.equals("TAN"))
			{
			double Rtheta = DEGRAD/Math.tan(s[1]/DEGRAD);	// IN DEGS (EQN. 54)
			x[0] =  Rtheta*Math.sin(s[0]/DEGRAD);		// IN DEGS (EQN. 12)
			x[1] = -Rtheta*Math.cos(s[0]/DEGRAD);		// IN DEGS (EQN. 13)
			}

		// FINAL ANGULAR COORDINATES CORRECTED FOR ROTATION, SKEWNESS, SCALE
		double[] p = new double[n];	// IN PSEUDO DEGREES
		if (useCD && useCDPC)
			{
			if (CDinv == null)
				{
				IJ.beep();
				IJ.log("No inverse WCS CD matrix!");
				return null;
				}
			for (int j=0; j < n; j++)
				{
				p[j] = 0.0;
				for (int i=0; i < n; i++)
					p[j] += CDinv[j][i]*x[i];
				}
			}
		else if (useCDPC)
			{
			if (PCinv == null)
				{
				IJ.beep();
				IJ.log("No inverse WCS PC matrix!");
				return null;
				}
			for (int j=0; j < n; j++)
				{
				p[j] = 0.0;
				for (int i=0; i < n; i++)
					p[j] += PCinv[j][i]*x[i]/CDELT[i];
				}
			}
		else	{
			for (int j=0; j < n; j++)
				p[j] = x[j]/CDELT[j];
			}

		// ADD SIP PIXEL DISTORTIONS
		if (useSIPAP)
			{
			//CALCULATE SIP X-COORDINATE CORRECTION
			double xCorrection = 0.0;
			for (int qq=0; qq <= AP_ORDER; qq++)
				{
				for (int pp=0; pp <= AP_ORDER; pp++)
					{
					if (pp + qq <= AP_ORDER)
						xCorrection += AP[pp][qq]*Math.pow(p[0],(double)pp)*Math.pow(p[1],(double)qq);
					}
				}
			p[0] += xCorrection;
			}
		if (useSIPBP)
			{
			//CALCULATE SIP Y-COORDINATE CORRECTION
			double yCorrection = 0.0;
			for (int qq=0; qq <= BP_ORDER; qq++)
				{
				for (int pp=0; pp <= BP_ORDER; pp++)
					{
					if (pp + qq <= BP_ORDER)
						yCorrection += BP[pp][qq]*Math.pow(p[0],(double)pp)*Math.pow(p[1],(double)qq);
					}
				}
			p[1] += yCorrection;
			}

		// TRANSLATE FITS COORDINATES TO ImageJ COORDINATES
		double[] pixels = fits2imagej(p,true);
		return pixels;
		}

	public boolean hasRaDec ()
		{
		return hasRADEC;
		}
	public boolean hasWCS ()
		{
		return enoughInfo;
		}

	public String[] getWCSUnits()
		{
		if (CUNIT == null) return null;
		String[] units = CUNIT.clone();
		return units;
		}

	/** Places current WCS info back into FITS header - WARNING: NOT YET FINISHED! */
	public void saveFITS (ImagePlus img)
		{
		int icard=0;
		String[] hdr = FitsJ.getHeader (img);
		if (hdr == null) return;

		// CTYPEn
		icard=FitsJ.findCardWithKey("CTYPE1",hdr);
		if (icard > 0)
			{
			String ctyp = FitsJ.getCardStringValue(hdr[icard]);
			if (! ctyp.trim().equals(CTYPE[0]))
				{
				FitsJ.renameCardWithKey("CTYPE1","OLDCTYP1",hdr);
				hdr = FitsJ.setCard("CTYPE1",CTYPE[0],"coordinate type (WCS repair)",hdr);
				}
			}
		else
			hdr = FitsJ.setCard("CTYPE1",CTYPE[0],"coordinate type (WCS repair)",hdr);

		icard=FitsJ.findCardWithKey("CTYPE2",hdr);
		if (icard > 0)
			{
			String ctyp = FitsJ.getCardStringValue(hdr[icard]);
			if (! ctyp.trim().equals(CTYPE[1]))
				{
				FitsJ.renameCardWithKey("CTYPE2","OLDCTYP2",hdr);
				hdr = FitsJ.setCard("CTYPE2",CTYPE[1],"coordinate type (WCS repair)",hdr);
				}
			}
		else
			hdr = FitsJ.setCard("CTYPE2",CTYPE[1],"coordinate type (WCS repair)",hdr);

		// CRVALn
		icard=FitsJ.findCardWithKey("RA",hdr);
		if (icard > 0)
			FitsJ.renameCardWithKey("RA","OLD-RA",hdr);
		icard=FitsJ.findCardWithKey("DEC",hdr);
		if (icard > 0)
			FitsJ.renameCardWithKey("DEC","OLD-DEC",hdr);
		hdr = FitsJ.setCard("CRVAL1",CRVAL[0],"Right Ascension in decimal degrees (WCS repair)",hdr);
		hdr = FitsJ.setCard("CRVAL2",CRVAL[1],"Declination in decimal degrees (WCS repair)",hdr);
		
		// CRPIXn
		icard=FitsJ.findCardWithKey("CRPIX1",hdr);
		if (icard > 0)
			{
			double val = FitsJ.getCardDoubleValue(hdr[icard]);
			if (Math.abs(val-CRPIX[0]) > 0.01)
				{
				FitsJ.renameCardWithKey("CRPIX1","OLDCRPX1",hdr);
				hdr = FitsJ.setCard("CRPIX1",CRPIX[0],"reference pixel coordinate (WCS repair)",hdr);
				}
			}
		else
			hdr = FitsJ.setCard("CRPIX1",CRPIX[0],"reference pixel coordinate (WCS repair)",hdr);

		icard=FitsJ.findCardWithKey("CRPIX2",hdr);
		if (icard > 0)
			{
			double val = FitsJ.getCardDoubleValue(hdr[icard]);
			if (Math.abs(val-CRPIX[1]) > 0.01)
				{
				FitsJ.renameCardWithKey("CRPIX2","OLDCRPX2",hdr);
				hdr = FitsJ.setCard("CRPIX2",CRPIX[1],"reference pixel coordinate (WCS repair)",hdr);
				}
			}
		else
			hdr = FitsJ.setCard("CRPIX2",CRPIX[1],"reference pixel coordinate (WCS repair)",hdr);

		// FIX CDij
		if (useCD)
			{
			icard=FitsJ.findCardWithKey("CD1_1",hdr);
			if (icard > 0)
				{
				FitsJ.renameCardWithKey("CD1_1","OLD-CD11",hdr);
				FitsJ.renameCardWithKey("CD1_2","OLD-CD12",hdr);
				FitsJ.renameCardWithKey("CD2_1","OLD-CD21",hdr);
				FitsJ.renameCardWithKey("CD2_2","OLD-CD22",hdr);
				}
			hdr = FitsJ.setCard("CD1_1",CD[0][0],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("CD1_2",CD[0][1],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("CD2_1",CD[1][0],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("CD2_2",CD[1][1],"transformation matrix element (WCS repair)",hdr);
			}
		else	{
			icard=FitsJ.findCardWithKey("PC1_1",hdr);
			if (icard > 0)
				{
				FitsJ.renameCardWithKey("PC1_1","OLD-PC11",hdr);
				FitsJ.renameCardWithKey("PC1_2","OLD-PC12",hdr);
				FitsJ.renameCardWithKey("PC2_1","OLD-PC21",hdr);
				FitsJ.renameCardWithKey("PC2_2","OLD-PC22",hdr);
				}
			hdr = FitsJ.setCard("PC1_1",PC[0][0],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("PC1_2",PC[0][1],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("PC2_1",PC[1][0],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("PC2_2",PC[1][1],"transformation matrix element (WCS repair)",hdr);
			}

		// MISC
		if (CUNIT != null)
			{
			icard=FitsJ.findCardWithKey("CUNIT1",hdr);
			if (icard > 0)
				{
				String unit = FitsJ.getCardStringValue(hdr[icard]);
				if (! unit.trim().equals(CUNIT[0]))
					{
					FitsJ.renameCardWithKey("CUNIT1","OLDCUNI1",hdr);
					hdr = FitsJ.setCard("CUNIT1",CUNIT[0],"units of first axis (WCS repair)",hdr);
					}
				}
			else
				hdr = FitsJ.setCard("CUNIT1",CUNIT[0],"units of first axis (WCS repair)",hdr);

			icard=FitsJ.findCardWithKey("CUNIT2",hdr);
			if (icard > 0)
				{
				String unit = FitsJ.getCardStringValue(hdr[icard]);
				if (! unit.trim().equals(CUNIT[1]))
					{
					FitsJ.renameCardWithKey("CUNIT2","OLDCUNI2",hdr);
					hdr = FitsJ.setCard("CUNIT2",CUNIT[1],"units of second axis (WCS repair)",hdr);
					}
				}
			else
				hdr = FitsJ.setCard("CUNIT2",CUNIT[1],"units of second axis (WCS repair)",hdr);
			}

		icard=FitsJ.findCardWithKey("LONPOLE",hdr);
		if (icard > 0)
			{
			double val = FitsJ.getCardDoubleValue(hdr[icard]);
			if (Math.abs(val-LONPOLE) > 0.01)
				{
				FitsJ.renameCardWithKey("LONPOLE","OLDLNPOL",hdr);
				hdr = FitsJ.setCard("LONPOLE",LONPOLE,"longitude of the pole (WCS repair)",hdr);
				}
			}
		else
			hdr = FitsJ.setCard("LONPOLE",LONPOLE,"longitude of the pole (WCS repair)",hdr);

		// SAVE RESULT
		FitsJ.putHeader(img,hdr);
		}

	public double[] getCRPIX()
		{
		return CRPIX.clone();
		}
	public double[] getCRVAL()
		{
		return CRVAL.clone();
		}
	public double[] getCDELT()
		{
		return CDELT.clone();
		}
	public double[][] getPC()
		{
		return PC.clone();
		}
	public double[][] getCD()
		{
		return CD.clone();
		}
	public String[] getCTYPE()
		{
		return CTYPE.clone();
		}
	public String[] getCUNIT()
		{
		return CUNIT.clone();
		}
	public double getLONPOLE()
		{
		return LONPOLE;
		}
	public int getNAXES()
		{
		return NAXES;
		}
	public int getWCSAXES()
		{
		return WCSAXES;
		}
	public int[] getNAXIS()
		{
		return NAXIS.clone();
		}

	public void setCRPIX(double[] c)
		{
		useCRPIXprefs = false;
		CRPIX = c.clone();
		}
	public void setCRVAL(double[] c)
		{
		CRVAL = c.clone();
		}
	public void setCDELT(double[] c)
		{
		useCDELTprefs = false;
		CDELT = c.clone();
		}
	public void setPC(double[][] pc)
		{
		usePCprefs = false;
		PC = pc.clone();
		PCinv = invert(PC);
		if (PCinv == null)
			{
			IJ.beep();
			IJ.log("Cannot invert PC WCS matrix!");
			}
		}
	public void setCD(double[][] cd)
		{
		useCDprefs = false;
		CD = cd.clone();
		CDinv = invert(CD);
		if (CDinv == null)
			{
			IJ.beep();
			IJ.log("Cannot invert CD WCS matrix!");
			}
		}
	public void setCTYPE(String[] s)
		{
		useCTYPEprefs = false;
		CTYPE = s.clone();
		if (CTYPE.length >= 2)
			coordsys = CTYPE[0].trim()+","+CTYPE[1].trim();
		Boolean hasRA  = false;
		Boolean hasDEC = false;
		for (int i=0; i < CTYPE.length; i++)
			{
			if (CTYPE[i].contains("RA"))
				{
				hasRA = true;
				if (CTYPE[i].startsWith("RA---"))
					projection=CTYPE[i].substring(5,8);
				}
			else if (CTYPE[i].contains("DEC"))
				{
				hasDEC = true;
				if (CTYPE[i].startsWith("DEC--"))
					projection=CTYPE[i].substring(5,8);
				}
			}
		if (hasRA && hasDEC)
			{
			hasRADEC = true;
			enoughInfo = true;
			}
		}
	public void setCUNIT(String[] s)
		{
		CUNIT = s.clone();
		}
	public void setLONPOLE(double l)
		{
		LONPOLE = l;
		}
	public void setWCSAXES(int n)
		{
		WCSAXES = n;
		}
	public void setNAXIS(int nx, int ny)
		{
		NAXIS = new int[] {nx,ny};
		}

	public void setNAXIS(int[] nxy)
		{
		NAXIS = nxy.clone();
		}

	public void log(String label)
		{
		if (label != null) IJ.log(label);
		IJ.log("\tNAXIS    ="+NAXES);
		IJ.log("\tWCSNAXES ="+WCSAXES);
		for (int i=0; i < NAXES; i++)
			IJ.log("\tNAXIS"+(i+1)+"="+NAXIS[i]);
		for (int i=0; i < NAXES; i++)
			IJ.log("\tCDELT"+(i+1)+"="+CDELT[i]);
		for (int i=0; i < NAXES; i++)
			IJ.log("\tCRPIX"+(i+1)+"="+CRPIX[i]);
		for (int i=0; i < NAXES; i++)
			IJ.log("\tCRVAL"+(i+1)+"="+CRVAL[i]);
		for (int i=0; i < NAXES; i++)
			IJ.log("\tCTYPE"+(i+1)+"="+CTYPE[i]);
		for (int i=0; i < NAXES; i++)
			IJ.log("\tCUNIT"+(i+1)+"="+CUNIT[i]);
		if (useCD && CD != null)
			{
			for (int i=0; i < NAXES; i++)
				for (int j=0; j < NAXES; j++)
					IJ.log("\tCD"+(i+1)+"_"+(j+1)+"="+CD[i][j]);
			if (CDinv != null)
				{
				for (int i=0; i < NAXES; i++)
					for (int j=0; j < NAXES; j++)
						IJ.log("\tINVCD"+(i+1)+"_"+(j+1)+"="+CDinv[i][j]);
				}
			}
		else if (!useCD && PC != null)
			{
			for (int i=0; i < NAXES; i++)
				for (int j=0; j < NAXES; j++)
					IJ.log("\tPC"+(i+1)+"_"+(j+1)+"="+PC[i][j]);
			if (PCinv != null)
				{
				for (int i=0; i < NAXES; i++)
					for (int j=0; j < NAXES; j++)
						IJ.log("\tINVPC"+(i+1)+"_"+(j+1)+"="+PCinv[i][j]);
				}
			}
		IJ.log("\tLONPOLE="+LONPOLE);
		IJ.log("\tcoordsys="+coordsys);
		IJ.log("\tprojection="+projection);
		IJ.log("\tuseCD="+useCD);
		IJ.log("\thasRADEC="+hasRADEC);
		IJ.log("\tenoughInfo="+enoughInfo);
		if (useSIPA)
			{
			IJ.log("\tA_ORDER="+A_ORDER);
			// double[][] A;
			}
		if (useSIPB)
			{
			IJ.log("\tB_ORDER="+B_ORDER);
			// double[][] B;
			}
/*
		IJ.log("Preferences:");
		IJ.log("\tuseCDELTprefs\t"+useCDELTprefs);
		IJ.log("\tuseCRPIXprefs\t"+useCRPIXprefs);
		IJ.log("\tuseCTYPEprefs\t"+useCTYPEprefs);
		IJ.log("\tuseCDprefs\t"+useCDprefs);
		IJ.log("\tusePCprefs\t"+usePCprefs);
*/
		}

	/**
	 * Converts ImageJ pixels to FITS pixels.
	 * Note that ImageJ pixels are assumed to be shifted by Centroid.PIXELCENTER pixels relative to the FITS standard.
	 */
	public double[] imagej2fits (double[] pixels, Boolean useCRPIX)
		{
		double[] p = new double[pixels.length];
		for (int j=0; j < p.length; j++)
			{
			if (j == 1)
				// p[1] = NAXIS[1] - (pixels[1] - Centroid.PIXELCENTER) - CRPIX[1]; 	
				p[1] = NAXIS[1] - (pixels[1] - Centroid.PIXELCENTER); 	
			else
				// p[j] = (pixels[j] - Centroid.PIXELCENTER) + 1.0 - CRPIX[j];		
				p[j] = (pixels[j] - Centroid.PIXELCENTER) + 1.0;
			if (useCRPIX) p[j] -= CRPIX[j];
			}
		return p;
		}

	/**
	 * Converts 2-D ImageJ pixels to FITS pixels.
	 * Note that ImageJ pixels are assumed to be shifted by Centroid.PIXELCENTER pixels relative to the FITS standard.
	 */
	public double[] imagej2fits (double xpix, double ypix, Boolean useCRPIX)
		{
		double[] p = new double[] {xpix,ypix};
		return imagej2fits (p,useCRPIX);
		}

	/**
	 * Converts FITS pixels to ImageJ pixels.
	 * Note that ImageJ pixels are assumed to be shifted by Centroid.PIXELCENTER pixels relative to the FITS standard.
	 */
	public double[] fits2imagej (double[] p, Boolean useCRPIX)
		{
		double[] pixels = new double[2];
		for (int j=0; j < pixels.length; j++)
			{
			if (j == 1)
				{
				// p[1] = NAXIS[1] - (pixels[1] - Centroid.PIXELCENTER) - CRPIX[1]; 	
				pixels[1] = -p[1] + NAXIS[1] + Centroid.PIXELCENTER; 
				if (useCRPIX) pixels[1] -= CRPIX[1];
				}
			else	{
				// p[j] = (pixels[j] - Centroid.PIXELCENTER) + 1.0 - CRPIX[j];		
				pixels[j] = p[j] + Centroid.PIXELCENTER - 1.0;
				if (useCRPIX) pixels[j] += CRPIX[j];
				}
			}
		return pixels;
		}

	/**
	 * Converts 2-D FITS pixels to ImageJ pixels.
	 * Note that ImageJ pixels are assumed to be shifted by Centroid.PIXELCENTER pixels relative to the FITS standard.
	 */
	public double[] fits2imagej (double xpix, double ypix, Boolean useCRPIX)
		{
		double[] p = new double[] {xpix,ypix};
		return fits2imagej (p,useCRPIX);
		}

	}
