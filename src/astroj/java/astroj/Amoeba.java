// Amoeba.java

package astroj;

import ij.IJ;

/**
 * Quick and dirty translation of NR ameoba.c into Java.
 */
public class Amoeba
	{
	static double TINY = 1.0e-10;

	protected int nmax;
	protected int ndim;
	protected int mdim;	// ndim+1
	protected int nfunk;
	protected int hi;
	protected int lo;
	protected double[][] pars;
	protected double[] parsums;
	protected double[] values;

	protected AmoebaFunction funk = null;

	public Amoeba ()
		{
		ndim=0;
		mdim=0;
		nfunk=0;
		hi=0;
		lo=0;
		pars = null;
		parsums = null;
		funk = null;
		nmax = 5000;
		}

	public void setFunction (AmoebaFunction f)
		{
		funk = f;
		}

	/**
	 * Runs simplex given a set of initial parameter sets, pars.
	 */
	public int optimize (double[][] p, double ftol, int nmx, int skip)
		{
		if (funk == null)
			{
			System.out.println("No function!");
			return 0;
			}

		double rtol,sum,swap,valsave,valtry;

		mdim = p.length;
		ndim = mdim-1;
		if (ndim != p[0].length)
			{
			System.out.println("Incorrect parameter sets!");
			return 0;
			}
		parsums  = new double[ndim];
		nfunk = 0;
		nmax = nmx;
		pars = p.clone();

		// GET VALUES AT EACH PARAMETER SET
		values = new double[mdim];
		for (int j=0; j < mdim; j++)
			values[j] = funk.userFunction (pars[j],0.0);	// DUMMY 2ND PARAMETER

		getPsum();
		while (true)
			{
			lo = 0;
			int nhi = 0;
			if (values[0] > values[1]) nhi=1;
			for (int j=0; j < mdim; j++)
				{
				if (values[j] <= values[lo])
					lo=j;
				if (values[j] > values[hi])
					{
					nhi=hi;
					hi=j;
					}
				else if (values[j] > values[nhi] && j != hi)
					nhi=j;
				}
			rtol = 2.0*Math.abs(values[hi]-values[lo])/(Math.abs(values[hi])+Math.abs(values[lo])+TINY);
			if (nfunk == 0)
				{
				IJ.log("\niteration "+nfunk+": "+values[lo]);
				for (int i=0; i < ndim; i++)
					IJ.log("\tpar["+i+"]="+pars[lo][i]);
				}
			if (rtol < ftol)
				{
				double nix;
				nix=values[0]; values[0]=values[lo]; values[lo]=nix;	// SWAP
				for (int i=1; i < ndim; i++)
					{
					nix=pars[0][i]; pars[0][i]=pars[lo][i]; pars[lo][i]=nix;	// SWAP
					}
				break;
				}
			if (nfunk >= nmax)
				{
				IJ.log("Amoeba: NMAX exceeded");
				return nfunk;
				}
			nfunk += 2;
			valtry = amotry(-1.0);
			if (valtry <= values[lo])
				valtry = amotry(2.0);
			else if (valtry >= values[nhi])
				{
				valsave = values[hi];
				valtry = amotry(0.5);
				if (valtry >= valsave)
					{
					for (int j=0; j < mdim; j++)
						{
						if (j != lo)
							{
							for (int i=0; i < ndim; i++)
								pars[j][i]=parsums[i]=0.5*(pars[j][i]+pars[lo][i]);
							values[j]=funk.userFunction (parsums,0.0); // DUMMY 2ND PARAMETER
							}
						}
					nfunk += ndim;
					getPsum();
					}
				}
			else
				nfunk -= 1;
			if (nfunk % skip == 0)
				{
				IJ.log("\niteration "+nfunk+": "+values[lo]);
				for (int i=0; i < ndim; i++)
					IJ.log("\tpar["+i+"]="+pars[lo][i]);
				}
			}
		return nfunk;
		}

	protected double amotry (double fac)
		{
		double[] ptry = new double[ndim];
		double fac1 = (1.0-fac)/ndim;
		double fac2 = fac1-fac;
		for (int i=0; i < ndim; i++)
			ptry[i] = parsums[i]*fac1-pars[hi][i]*fac2;
		double valtry = funk.userFunction (ptry,0.0);	// DUMMY 2ND PARAMETER
		if (valtry < values[hi])
			{
			values[hi]=valtry;
			for (int i=0; i < ndim; i++)
				{
				parsums[i] += ptry[i]-pars[hi][i];
				pars[hi][i] = ptry[i];
				}
			}
		return valtry;
		}

	protected void getPsum()
		{
		for (int i=0; i < ndim; i++)
			{
			double sum = 0.0;
			for (int j=0; j < mdim; j++)
				sum += pars[j][i];
			parsums[i]=sum;
			}
		return;
		}

	public double[] solution()
		{
		return pars[lo];
		}
	}
