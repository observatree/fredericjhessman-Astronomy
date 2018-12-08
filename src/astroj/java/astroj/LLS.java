// LLS.java

package astroj;

import java.io.*;
import java.text.*;
import java.util.*;

import ij.IJ;

import Jama.*;

/**
 * Solves general linear least-square problems using singular value decomposition.
 * The method setFunctionType() provide standard functions; others have to be provided by a user-class.
 *
 * @author F.V. Hessman, Hessman@Astro.physik.Uni-Goettingen.de
 * @date 2007-04-17
 * @version 1.0
 *
 * @date 2012-09-27
 * @changes Cleaned up the example, permitted multi-dimensional x data, fixed bugs.
 */
public class LLS implements LinearFunction
	{
	protected int nData = 0;
	protected int size = 0;
	protected int rank = 0;
	protected DecimalFormat format;
	protected GFormat f;

	protected double[] coef = null;
	protected double[] cerr = null;

	public boolean verbose = true;

	// Jama OBJECTS NEEDED FOR THE FIT

	protected Matrix aMatrix = null;	// MODEL MATRIX
	protected Matrix bVector = null;	// DATA VECTOR
	protected Matrix xVector = null;	// COEFFICIENTS
	protected Matrix wMatrix = null;	// DIAGONAL MATRIX IF EDITED INVERSE SINGULAR VALUES 
	protected Matrix covMatrix = null;
	protected SingularValueDecomposition svd = null;

	// SUPPORTED STANDARD FUNCTIONS (ALL OTHERS MUST BE PROVIDED BY USER OR SUBCLASSES)

	public static int NO_FUNCTION = 0;
	public static int LINE_FUNCTION = 1;
	public static int PARABOLA_FUNCTION = 2;
	public static int HYPERBOLA_FUNCTION = 3;
	public static int FOCUS_FUNCTION = 4;
	public static int POLYNOMIAL_FUNCTION = 5;
	public static int GENERIC_LINEAR_FUNCTION = 6;
	public static int OTHER_FUNCTION = 7;

	protected int funcType;		// FOR STANDARD FUNCTIONS
	protected int[] ipar;
	protected double[] dpar;

	public static String bar = new String("-----------------------------------------------------------------------------------------------------------------------------------\n");

	/**
	 * Initializes the LLS object.
	 */
	public LLS ()
		{
		funcType = NO_FUNCTION;
		size = 0;
		format = new DecimalFormat ("0.000000E00");
		f = new GFormat ("8.4");
		}

	/**
	 * Used to set the function if provided by LLS.
	 */
	public void setFunctionType (int typ, int[] iparam, double[] dparam)
		{
		funcType = typ;
		if (funcType == LINE_FUNCTION)
			size = 2;
		else if (funcType == PARABOLA_FUNCTION)
			size = 3;
		else if (funcType == HYPERBOLA_FUNCTION)
			size = 3;
		else if (funcType == FOCUS_FUNCTION)	// SLOPE OF HYPERBOLA FIXED AT dparam[0]
			{
			size = 2;
			dpar = new double[] { dparam[0] };
			}
		else if (funcType == POLYNOMIAL_FUNCTION)
			size = iparam[0];	// iparam[0] IS NUMBER OF PARAMETERS
		else if (funcType == GENERIC_LINEAR_FUNCTION)
			size = iparam[0];
		}

	// ------------- LinearFunction METHODS ---------------

	/**
	 * Names of standard models.
	 */
	public String modelName ()
		{
		if (funcType == LINE_FUNCTION)
			return new String ("Line y = c[0]+c[1]*x");
		else if (funcType == PARABOLA_FUNCTION)
			return new String ("Parabola y = c[0]+c[1]*x+c[2]*x^2");
		else if (funcType == HYPERBOLA_FUNCTION)
			return new String ("Hyperbola y^2 = c[0]+c[1]*x+c[2]*x^2");
		else if (funcType == FOCUS_FUNCTION)
			return new String ("Focus Curve with Assumed Asymptotic Slope="+dpar[0]);
		else if (funcType == POLYNOMIAL_FUNCTION)
			return new String ("Polynomial of Order "+(size-1));
		else if (funcType == GENERIC_LINEAR_FUNCTION)
			return new String ("Generic linear function of rank "+size);
		else
			return new String ("Unknown");
		}

	/**
	 * Whether function is actually linear or not.
	 */
	public boolean isReallyQuadratic ()
		{
		if (funcType == HYPERBOLA_FUNCTION)
			return true;
		else if (funcType == FOCUS_FUNCTION)
			return true;
		return false;
		}

	/**
	 * Values of standard fit functions.
	 */
	public double model (int indx, double[] x, double[] c)
		{
		if (funcType == NO_FUNCTION) return Double.NaN;

		double[][] m = modelFunctions(indx,x,0.0,1.0);
		double val = 0.0;
		for (int j=0; j < c.length; j++)
			val += c[j]*m[0][j];

		// SPECIAL TREATMENT OF ACTUALLY NON-LINEAR FUNCTIONS

		if (funcType == HYPERBOLA_FUNCTION)
			val = Math.sqrt(val);
		else if (funcType == FOCUS_FUNCTION)
			val = Math.sqrt(val+dpar[0]*dpar[0]*x[0]*x[0]);

		return val;
		}

	/**
	 * Standard model functions.
	 */
	public double[][] modelFunctions (int indx, double[] x, double y, double wgt)
		{
		if (funcType == NO_FUNCTION) return null;

		double[][] r = null;
		double xx = x[0];

		//  y^2 = c[0] + c[1]*x + c[2]*x^2; slope=sqrt(c[2]), xmin= -0.5*c[1]/slope^2; ymin^2 = c[0]-xmin^2*slope^2
		//  e.g. xmin=6.0, ymin=1.0, slope=1.0 => c[0]=37, c[1]=-12, c[2]=1

		if (funcType == HYPERBOLA_FUNCTION)
			{
			r = new double[3][4];
			r[0][0] = 1.0*wgt;	r[0][1] = xx*wgt;	r[0][2] = xx*xx*wgt;		r[0][3] = y*y*wgt;
			r[1][0] = xx*wgt;	r[1][1] = xx*xx*wgt;	r[1][2] = xx*xx*xx*wgt;		r[1][3] = xx*y*y*wgt;
			r[2][0] = xx*xx*wgt;	r[2][1] = xx*xx*xx*wgt;	r[2][2] = xx*xx*xx*xx*wgt;	r[2][3] = xx*xx*y*y*wgt;
			}

		// y^2-slope^2*x^2 = c[0] + c[1]*x; slope=dpar[0]; ...

		else if (funcType == FOCUS_FUNCTION)
			{
			r = new double[2][3];
			double yy = y*y-dpar[0]*dpar[0]*xx*xx;
			r[0][0] = 1.0*wgt;	r[0][1] = xx*wgt;	r[0][2] = yy*wgt;
			r[1][0] = xx*wgt;	r[1][1] = xx*xx*wgt;	r[1][2] = xx*yy*wgt;
			}

		// y = c[0]+c[1]*x+...+c[n-1]*x^(n-1)

		else if (funcType == LINE_FUNCTION || funcType == PARABOLA_FUNCTION || funcType == POLYNOMIAL_FUNCTION)
			{
			r = new double[size][size+1];
			for (int j=0; j < size; j++)
				{
				for (int i=0; i < size; i++)
					r[j][i] = Math.pow(xx,i+j)*wgt;
				r[j][size] = Math.pow(xx,j)*y*wgt;
				}
			}
		// y = c[0]*x[0]+c[1]*x[1]+...+c[n-1]*x[n-1]

		else if (funcType == GENERIC_LINEAR_FUNCTION)
			{
			r = new double[size][size+1];
			for (int j=0; j < size; j++)
				{
				for (int i=0; i < size; i++)
					r[j][i] = x[i]*x[j];
				r[j][size] = x[j]*y*wgt;
				// System.out.println("j,x,y,wgt="+j+","+x[j]+","+y+","+wgt);
				// IJ.log("j,x,y,wgt="+j+","+x[j]+","+y+","+wgt);
				}
			}
		return r;
		}

	/**
	 * Size of model.
	 */
	public int numberOfParameters ()
		{
		return size;
		}


	/**
	 * Returns the fitted coefficients corresponding to the model provided by func and the data x[][],y[],sig[] (if indeed used by func).
	 */
	public double[] fit (double[][] x, double[] y, double[] sig, LinearFunction func)
		{
		// MAKE SURE FUNCTION PROVIDED

		if (func == null)
			{
			System.err.println ("No linear function provided!");
			IJ.log ("No linear function provided!");
			return null;
			}
		size = func.numberOfParameters();
		rank = size;

		// NOW DO ACTUAL FIT

		nData = 0;

		// GET ALL SUMS OF MODEL FUNCTION PRODUCTS

		double[][] xsums = new double[size][size];
		double[][] ysums = new double[size][1];
		int n = y.length;
		for (int k=0; k < n; k++)			// FOR ALL DATA SETS k 
			{
			double w = getWeightFor (k,y,sig,func.isReallyQuadratic());
			if (w > 0.0)
				{
				nData++;
				double[] xx = x[k];
				/*
				for (int i=0; i < xx.length; i++)
					{
					System.out.println("k,y,x="+k+", "+y[k]+", "+xx[i]);
					IJ.log("k,y,x="+k+", "+y[k]+", "+xx[i]);
					}
				*/
				double[][] m = func.modelFunctions(k,xx,y[k],w);
				Matrix mMatrix = new Matrix(m);
				mMatrix.print(format,12);
				for (int j=0; j < size; j++)		// FOR ALL ROWS
					{
					for (int i=0; i < size; i++)	// FOR ALL COLUMNS
						xsums[j][i] += m[j][i];
					ysums[j][0] += m[j][size];
					}
				}
			}

		// CONSTRUCT MATRICES

		constructMatrices (xsums,ysums);

		// GET RETURNED COEFFICIENTS:   X = V*W*U^T*B

		coef = xVector.getColumnPackedCopy();

		// IF NO ERRORS AVAILABLE, ESTIMATE VARIANCE AND MULTIPLY THROUGH COVARIANCE MATRIX

		if (sig == null)
			{
			double var = 0.0;
			for (int k=0; k < n; k++)
				{
				double[] xk = null;
				double yk = Double.NaN;
				if (x != null) xk=x[k];
				if (y != null) yk=y[k];
				double omc;
				double yfit = func.model (k,xk,coef);
				if (func.isReallyQuadratic())
					omc = y[k]*y[k]-yfit*yfit;
				else
					omc = y[k]-yfit;
				var += omc*omc;
				}
			var /= (nData-size);	// -rank); !!!!
			covMatrix.timesEquals(var);
			}

		cerr = new double[size];
		for (int k=0; k < size; k++)
			cerr[k] = Math.sqrt(covMatrix.get(k,k));

		if (verbose)
			{
			System.err.println("Covariance matrix:");
			covMatrix.print(format,12);
			System.err.println("Coefficient Vector:");
			xVector.print(format,12);
			}

		return coef.clone();
		}

	/**
	 * Heart of fitting method which solves for the coefficients.
	 */
	protected boolean constructMatrices (double[][] xsums, double[][] ysums)
		{
		aMatrix = null;
		bVector = null;
		xVector = null;
		wMatrix = null;
		svd = null;

		aMatrix = new Matrix(xsums);
		bVector = new Matrix(ysums);

		if (verbose)
			{
			System.err.println ("aMatrix:");
			aMatrix.print (format,12);
			System.err.println ("bVector:");
			bVector.print (format,12);
			}

		// OBTAIN SVD OF NORMAL EQUATION MATRIX

		try	{
			svd = new SingularValueDecomposition(aMatrix);
			}
		catch (RuntimeException e)
			{
			System.err.println (""+e.getMessage());
			return false;
			}
		rank = svd.rank();
		if (rank != size)
			{
			System.err.println ("Singular values! rank="+rank+" != size="+size);
			// IJ.log ("Singular values!? rank="+rank+" != size="+size);
			}

		// CHECK FOR SINGULAR VALUES

		Matrix vMatrix = svd.getV();
		Matrix wMatrix = svd.getS();
		Matrix uMatrix = svd.getU();
		if (vMatrix == null || wMatrix == null || uMatrix == null)
			{
			System.err.println ("V,W, or U matrix is null!");
			System.err.println ("aMatrix:");
			aMatrix.print (format,12);
			IJ.log ("V,W, or U matrix is null!");
			return false;
			}

		if (verbose)
			{
			System.out.println("SVD singular value matrix:");
			wMatrix.print(format,12);
			System.out.println("SVD U matrix:");
			uMatrix.print(format,12);
			System.out.println("SVD V matrix:");
			vMatrix.print(format,12);
			}
		double svd0 = wMatrix.get(0,0);
		for (int j=0; j < size; j++)
			{
			if (wMatrix.get(j,j)/svd0 < 1.e-8)
				wMatrix.set(j,j,0.0);
			else
				wMatrix.set(j,j,1./wMatrix.get(j,j));
			}
		if (verbose)
			{
			System.out.println("SVD diagonal weight matrix:");
			wMatrix.print (format,12);
			}

		// CONSTRUCT COVARIANCE MATRIX C(j,k) = SUM i=0,1 OF V(j,i)*V(k,i)/w(i)^2

		covMatrix = new Matrix(size,size);
		for (int j=0; j < size; j++)
			{
			for (int i=0; i < size; i++)
				{
				double sum = 0.0;
				for (int k=0; k < size; k++)
					sum += vMatrix.get(j,k)*vMatrix.get(i,k)*wMatrix.get(k,k);
				covMatrix.set(i,j,sum);
				}
			}

		// GET VECTOR FROM WHICH THE COEFFICIENTS CAN BE DERIVED:   X = V*W*U^T*B

		xVector = vMatrix.times(wMatrix).times(uMatrix.transpose()).times(bVector);

		// USE BUILT-IN SOLVER
		xVector = aMatrix.solve(bVector);
		return true;
		}

	/**
	 * Returns the standard errors to the previously fitted coefficients.
	 */
	public double[] coefficientErrors()
		{
		return cerr.clone();
		}

	/**
	 * Returns a copy of the covariance matrix.
	 */
	public Matrix covarianceMatrix()
		{
		return (Matrix)covMatrix.clone();
		}

	/**
	 * Returns the effective value of 1./sig[k]^2, even when sig == null or sig[k] = NaN.
	 *
	 * @param k		the index of the fitted measurement.
	 * @param y		the array of fitted measurements.
	 * @param sig		the array of measurment errors.
	 * @param quadratic	a flag which says whether the weights should be calculated assuming the function is really quadratic.
	 */
	protected double getWeightFor (int k, double[] y, double[] sig, boolean quadratic)
		{
		double yk = y[k];
		if (Double.isNaN(yk)) return 0.0;

		double w = 1.0;
		if (sig != null)
			{
			double wavg = 0.0;
			int navg = 0;
			for (int i=0; i < sig.length; i++)
				{
				if (! Double.isNaN(sig[i]) && sig[i] > 0.0)
					{
					wavg += sig[i];
					navg++;
					}
				}
			wavg /= navg;

			double sigk = sig[k];
			if (Double.isNaN(sigk) || sigk <= 0.0)
				w = 0.0;
			else	{
				w = wavg*wavg/(sigk*sigk);
				if (quadratic)
					{
					if (sigk > Math.abs(2.*yk))
						w = 0.25*w*w;	// NOT FORMALLY RIGHT BUT MORE ROBUST
					else
						w = 0.25*w/(yk*yk);
					}
				}
			}
		return w;
		}

	/**
	 * Outputs the results of the last fit as a gigantic String.
	 */
	public String results (double[][] x, double[] y, double[] sig, LinearFunction func, DecimalFormat df)
		{
		if (size < 1 || coef == null || cerr == null)
			return new String ("No fit to print out!");

		// DecimalFormat f = df;
		// if (f == null) f = format;

		double chisqr = 0.0;
		double rms = 0.0;

		String s = "Least Squares Fit of Following Data to a "+func.modelName()+"\n";
		s += bar+"i\t";
		if (x[0].length == 1)
			s += "x\t";
		else	{
			for (int i=0; i < x[0].length; i++)
				s += "x"+i+"\t";
			}
		s += "y\terr\twgt\tyfit\tO-C\n";
		s += bar;
		for (int i=0; i < y.length; i++)
			{
			double sg = 1.0;
			if (sig != null) sg = sig[i];
			double w = getWeightFor (i,y,sig,false);
			double[] xx = x[i];
			double yfit = func.model (i,xx,coef);
			double omc = y[i]-yfit;
			if (w > 0.0)
				{
				chisqr += omc*omc/(sg*sg);
				rms += omc*omc;
				}
			s += ""+(i+1)+"\t";
			for (int j=0; j < x[0].length; j++)
				s += f.format(xx[j])+"\t";
			s +=	 f.format(y[i])+"\t"
				+f.format(sg)+"\t"
				+f.format(w)+"\t"
				+f.format(yfit)+"\t"
				+f.format(omc)+"\n";
			}
		s += bar;
		chisqr /= (nData-size);
		rms = Math.sqrt(rms/nData);
		s += "Fitted coefficients:\n";
		for (int k=0; k < size; k++)
			{
			double sigmas = Double.NaN;
			if (cerr[k] != 0.0) sigmas = Math.abs(coef[k]/cerr[k]);
			s += "\tc["+k+"] = "
				+format.format(coef[k])+" +/- "+format.format(cerr[k])
				+"   ("+(int)sigmas+"-sigma)\n";
			}
		s += bar;
		s += "R.M.S.: "+f.format(rms)+"\n";

		if (sig != null)
			s += "Reduced Chi-Square: "+f.format(chisqr)+"\n";
		else
			s += "Estimated Reduced Chi-Square: "+f.format(chisqr/(rms*rms))+"\n";
		// s += "Covariance Matrix:\n";
		// covMatrix.print(format,12);

		// y = c[0]+c[1]*x+c[2]*x^2, xmin/max= -0.5*c[1]/c[2], ymin/max = c[0]-0.25*c[1]^2/c[2]

		if (funcType == PARABOLA_FUNCTION)
			{
			s += bar;
			s += "Derived Quantities:\n";
			double ctr    = -0.5*coef[1]/coef[2];
			double ctrErr = ctr * Math.sqrt(
							cerr[1]*cerr[1]/(coef[1]*coef[1])
						+	cerr[2]*cerr[2]/(coef[2]*coef[2]) 
						-	2.0*covMatrix.get(1,2)/(coef[1]*coef[2]));
			double ymn = coef[0]-0.25*coef[1]*coef[1]/coef[2];
			// double ymnErr = ymn * Math.sqrt(
			s += "\tcenter:\t\t"+f.format(ctr)+"  +/-  "+f.format(ctrErr)+"\n";
			s += "\tmin/max:\t"+f.format(ymn)+"\n";
			}

		//  y^2 = c[0] + c[1]*x + c[2]*x^2; slope=sqrt(c[2]), xmin= -0.5*c[1]/slope^2; ymin^2 = c[0]-xmin^2*slope^2
		// FITTED FUNCTION IS y^2

		else if (funcType == HYPERBOLA_FUNCTION)
			{
			s += bar;
			s += "Derived Quantities:\n";
			// s += "coef="+coef[0]+","+coef[1]+","+coef[2]+"\n";
			double sl    = Math.sqrt(coef[2]);
			double slErr = 0.5*cerr[2]/sl;
			double ctr    = -0.5*coef[1]/coef[2];
			double ctrErr = ctr * Math.sqrt(
							cerr[1]*cerr[1]/(coef[1]*coef[1])
						+	cerr[2]*cerr[2]/(coef[2]*coef[2]) 
						-	2.0*covMatrix.get(1,2)/(coef[1]*coef[2])
						);
			double ymn = Math.sqrt(coef[0]-0.25*coef[1]*coef[1]/coef[2]);
			// s += "sl="+sl+", slErr="+slErr+", ctr="+ctr+", ctrErr="+ctrErr+"\n";
			// s += "\tslope of asymptote:\t"+f.format(sl)+"  +/-  "+f.format(slErr)+"\n";
			s += "\tslope of asymptote:\t"+format.format(sl)+"  +/-  "+format.format(slErr)+"\n";
			s += "\tcenter:\t\t\t"+f.format(ctr)+"  +/-  "+f.format(ctrErr)+"\n";
			s += "\tmin/max:\t\t"+f.format(ymn)+"\n";
			}

		// y^2 = c[0] + c[1]*x + slope^2*x^2; xmin= -0.5*c[1]/slope^2; ymin^2 = c[0]-xmin^2*slope^2
		// FITTED FUNCTION IS y^2-slope^2*x^2

		else if (funcType == FOCUS_FUNCTION)
			{
			s += bar;
			s += "Derived Quantities:\n";
			double sl = dpar[0];
			double c2 = sl*sl;
			double ctr = -0.5*coef[1]/c2;
			double ctrErr = Math.abs(ctr*cerr[1]/coef[1]);
			double ymn = Math.sqrt(coef[0]-0.25*coef[1]*coef[1]/c2);
			s += "\tslope of asymptote:\t"+f.format(sl)+"\n";
			s += "\tcenter:\t\t\t"+f.format(ctr)+"  +/-  "+f.format(ctrErr)+"\n";
			s += "\tmin/max:\t\t"+f.format(ymn)+"\n";
			}

		s += bar;
		return s;
		}

	/**
	 * Test program.  Yes, this isn't elegant, but - hey - it works!
	 */
	public static void main (String[] args)
		{
		// ENOUGH ARGUMENTS?

		if (args.length < 3)
			{
			// args: filename=#0, cols=#1, function=#2, optional=#3
			System.err.println("Syntax:\n\tjava LLS {filename} {ycol,errcol,xcol,...} {function} {optional_param}\n");
			System.out.println ("Supported functions are: line,parabola,hyperbola,focus,polynomial,general");
			return;
			}

		LLS lls = new LLS();
		int nc[] = null;
		int nf = 1;
	
		double[][] x = null;
		double[] y = null;
		double[] s = null;

		// lls.verbose = true;

		// DEFAULT PARAMETERS

		lls.setFunctionType (LINE_FUNCTION,null,null);
		double[] par = null;
		int[] cols = new int[] {2,-1,1};	// DEFAULT ycol,errcol,xcol

		// GET COLUMNS

		String[] pars = args[1].split(",");
		int maxcol = 0;
		if (pars.length >= 3)
			{
			cols = new int[pars.length];
			for (int i=0; i < pars.length; i++)
				{
				try	{
					cols[i] = Integer.parseInt(pars[i]);
					}
				catch (NumberFormatException e)
					{
					System.err.println(e.getMessage());
					return;
					}
				if (cols[i] > maxcol) maxcol=cols[i];
				// System.out.println ("cols["+i+"]="+cols[i]);
				}
			}

		// GET USER PARAMETERS

		if (args[2].equals("line"))
			{
			lls.setFunctionType (LINE_FUNCTION,null,null);
			nc = new int[] {2};
			}
		else if (args[2].equals("parabola"))
			{
			lls.setFunctionType (PARABOLA_FUNCTION,null,null);
			nc = new int[] {3};
			}
		else if (args[2].equals("hyperbola"))
			{
			lls.setFunctionType (HYPERBOLA_FUNCTION,null,null);
			nc = new int[] {3};
			}
		else if (args[2].equals("focus"))
			{
			par = new double[1];
			try	{
				par[0] = Double.parseDouble(args[3]);
				}
			catch (NumberFormatException e)
				{
				System.err.println(e.getMessage());
				return;
				}
			lls.setFunctionType (FOCUS_FUNCTION,null,par);
			nc = new int[] {2};
			}
		else if (args[2].equals("polynomial"))
			{
			int n=0;
			try	{
				n = Integer.parseInt(args[3]);
				}
			catch (NumberFormatException e)
				{
				System.err.println(e.getMessage());
				return;
				}
			nc = new int[] {n};
			lls.setFunctionType (POLYNOMIAL_FUNCTION,nc,null);
			}
		else if (args[2].equals("general"))
			{
			nf = cols.length-2;
			nc = new int[] {nf};
			lls.setFunctionType (GENERIC_LINEAR_FUNCTION,nc,null);
			}
		else	{
			System.out.println ("Not enough input or unsupported function: "+args[2]);
			return;
			}

		try	{
			// HOW MANY LINES?

			int num = 0;
			String line;
			BufferedReader in = new BufferedReader (new FileReader(args[0]));
			while ((line=in.readLine()) != null)
				{
				if (!line.trim().startsWith("#")) num++;
				}
			in.close();

			x = new double[num][nf];
			y = new double[num];
			if (cols[1] > 0) s = new double[num];

			// READ DATA

			num = 0;
			in = new BufferedReader (new FileReader(args[0]));
			while ((line=in.readLine()) != null)
				{
				if (!line.trim().startsWith("#"))
					{
					pars = line.trim().split("\t");
					if (pars.length < maxcol)
						pars = line.trim().split(",");
					if (pars.length < maxcol)
						pars = line.trim().split(" ");
					try	{
						for (int i=0; i < nf; i++)
							x[num][i] = Double.parseDouble(pars[cols[i+2]-1]);
						y[num] = Double.parseDouble(pars[cols[0]-1]);
						if (cols[1] > 0)
							s[num] = Double.parseDouble(pars[cols[1]-1]);
						// System.out.println("y["+num+"]="+y[num]);
						num++;
						}
					catch (Exception e)
						{
						System.out.println (e.getMessage());
						return;
						}
					}
				}
			in.close();
			}
		catch (Exception e)
			{
			System.out.println (e.getMessage());
			return;
			}

		// FIT DATA

		double[] c = null;
		c = lls.fit (x,y,s,lls);
		System.out.println (lls.results (x,y,s,lls,null));
		}
	}

