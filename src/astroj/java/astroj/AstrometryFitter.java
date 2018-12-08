// AstrometryFitter.java

package astroj;

import ij.measure.*;

public class AstrometryFitter
	{
	static double[] xNative = null;
	static double[] yNative = null;
	protected double[] ra  = null;
	protected double[] dec = null;

	Amoeba amoeba = null;

	public AstrometryFitter ()
		{
		amoeba = new Minimizer();
		amoeba.setFunction(this.chi2,4);
		amoeba.setMaxIterations(1000);
		}

	public void fit (double[] xn, double[] yn, double[] alpha, double[] delta)
		{
		xNative = xn.clone();
		yNative = yn.clone();
		ra  = alpha.clone();
		dec = delta.clone();

		double avgra  = IJU.averageOf(ra);
		double avgdec = IJU.averageOF(dec);
		double sigra  = IJU.sigmaOf(ra)
		double sigdec = IJU.sigmaOf(dec)
		double scale  = (IJU.maxOf(dec)-IJU.minOf(dec))/(IJU.maxOf(xn)-IJU.minOf(yn));

		double[]    p = new double[4] {avgra,avgdec,scale,180.};
		double[] varp = new double[4] {sigra,sigdec,1.5*scale,30.};

		amoeba.minimize(p,varp);
		IJ.log("Astrometric solution:");
		
		}

	public UserFunction chi2 (double[] p, double x)
		{
		int n = xNative.length;
		double alphaP = p[0];		// CRVAL1
		double deltaP = p[1];		// CRVAL2
		double scale  = p[2];		// CRDELT1=CRDELT2
		double phi0   = p[3];		// ROTATION FOR CD OR PC MATRIX

		int indx = (int)(x+0.01);
		int jndx = indx+n;

		double alpha = ra[indx];
		double delta = dec[indx];
		double phiP  = Math.PI;		// LONPOLE = 180 deg
		double sind  = Math.sin(delta);
		double cosd  = Math.cos(delta);
		double sindP = Math.sin(deltaP);
		double cosdP = Math.cos(deltaP);
		double sinda = Math.sin(alpha-alphaP);
		double cosda = Math.cos(alpha-alphaP);

		double rtheta = Math.asin(sind*sindP+cosd*cosdP*cosda);
		double phi = phiP + Math.atan2(-cosd*sinda,sind*cosdP-cosd*sindP*cosda) + phi0;

		if (indx < n)	// X
			return -rtheta*Math.sin(phi)/scale;	// MINUS SIGN BECAUSE CDELT1 < 0
		else		// Y
			return -rtheta*Math.cos(phi)/scale;	// MINUS SIGN BECAUSE PHI=0 IS SOUTH
		}
	}

