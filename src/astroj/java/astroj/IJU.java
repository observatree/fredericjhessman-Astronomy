// IJU.java

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/**
 * Various static utilities which really should be in the ImageJ IJ class.
 */
public class IJU
	{

	/**
	 * Returns a list of currently displayed images, in reversed order (presumably latest most interesting)
	 */
	public static String[] listOfOpenImages (String def)
		{
		int off=0;
		int[] imageList = WindowManager.getIDList();
		if (imageList == null)
			return null;
		int n = imageList.length;
		if (def != null) off=1;
		String[] images = new String[n+off];
		for (int i=n-1; i >= 0; i--)
			{
			ImagePlus im = WindowManager.getImage (imageList[i]);
			images[i+off] = im.getTitle();
			}
		if (def != null) images[0] = new String(def);
		return images;
		}

	/**
	 * Handy methods for extracting directory, filename, and suffix from a path.
	 */
	public static String extractDirectory (String path)
		{
		if (path == null) return null;
		String slash = "/";
		if (IJ.isWindows()) slash = "\\";
		int i = path.lastIndexOf(slash);
		if (i == -1)
			return null;
		else
			return path.substring (0,i)+slash;
		}

	public static String extractFilename (String path)
		{
		if (path == null) return null;
		String slash = "/";
		if (IJ.isWindows()) slash = "\\";
		int i = path.lastIndexOf(slash);
		if (i == -1)
			return path;
		else
			return path.substring (i+1);
		}

	public static String extractFilenameWithoutSuffix (String path)
		{
		if (path == null) return null;
		String filename = extractFilename (path);
		int i = filename.lastIndexOf(".");
		if (i == -1)
			return filename;
		else
			return filename.substring (0,i-1);
		}

	public static String extractFilenameWithoutFitsSuffix (String path)
		{
		if (path == null) return null;
		String filename = extractFilename (path);
		int i;
		i = filename.lastIndexOf(".fits");
		if (i > 1) return filename.substring (0,i-1);
		i = filename.lastIndexOf(".fts");
		if (i > 1) return filename.substring (0,i-1);
		i = filename.lastIndexOf(".fit");
		if (i > 1) return filename.substring (0,i-1);
		return filename;
		}

	public static String extractFilenameSuffix (String path)
		{
		if (path == null) return null;
		String filename = extractFilename (path);
		int i = filename.lastIndexOf(".");
		if (i == -1)
			return null;
		else
			return filename.substring (i+1);
		}

	/**
	 * Checks to see if this image name is already in use.  If so, it appends a number.
	 */
	public static String uniqueDisplayedImageName (String name)
		{
		String[] used = listOfOpenImages(null);
		if (used == null)
			return name;

		String basic = extractFilenameWithoutSuffix(name);
		String suffix = extractFilenameSuffix (name);

		for (int i=0; i < used.length; i++)
			{
			if (used[i].equals(name))
				{
				int k=1;
				int kk;
				String test = basic+"-"+k+"."+suffix;
				do	{
					kk=k;
					for (int j=0; j < used.length; j++)
						{
						if (used[i].equals(test)) k++;
						}
					} while (k != kk);
				return basic+"-"+k+"."+suffix;
				}
			}
		return name;
		}

	/**
	 * Help routine to find minimum value in a double array.
	 */
	public static double minOf (double[] arr)
		{
		int n=arr.length;
		if (n == 0) return Double.NaN;

		double mn = arr[0];
		for (int i=1; i < n; i++)
			{
			if (!Double.isNaN(arr[i]) && arr[i] < mn) mn=arr[i];
			}
		return mn;
		}

	/**
	 * Help routine to find the maximum value in a double array.
	 */
	public static double maxOf (double[] arr)
		{
		int n=arr.length;
		if (n == 0) return Double.NaN;

		double mx = arr[0];
		for (int i=1; i < n; i++)
			{
			if (!Double.isNaN(arr[i]) && arr[i] > mx) mx=arr[i];
			}
		return mx;
		}

	/**
	 * Help routine to find minimum value in a double matrix.
	 */
	public static double minOf (double[][] arr)
		{
		int ny=arr.length;
		if (ny == 0) return Double.NaN;
		int nx=arr[0].length;
		if (nx == 0) return Double.NaN;

		double mn = arr[0][0];
		for (int j=0; j < ny; j++)
			for (int i=0; i < nx; i++)
				{
				if (!Double.isNaN(arr[j][i]) && arr[j][i] < mn) mn=arr[j][i];
				}
		return mn;
		}

	/**
	 * Help routine to find maximum value in a double matrix.
	 */
	public static double maxOf (double[][] arr)
		{
		int ny=arr.length;
		if (ny == 0) return Double.NaN;
		int nx=arr[0].length;
		if (nx == 0) return Double.NaN;

		double mx = arr[0][0];
		for (int j=0; j < ny; j++)
			for (int i=0; i < nx; i++)
				{
				if (!Double.isNaN(arr[j][i]) && arr[j][i] > mx) mx=arr[j][i];
				}
		return mx;
		}

	/**
	 * Help routine to find std. dev. of values in a double array.
	 */
	public static double sigmaOf (double[] arr)
		{
		int n=arr.length;
		if (n == 0) return Double.NaN;

		double avg = averageOf(arr);
		double sigma = 0.0;
		double diff = 0.0;
		int num = 0;
		for (int i=0; i < n; i++)
			{
			if (!Double.isNaN(arr[i]))
				{
				diff = (arr[i]-avg);
				sigma += diff*diff;
				num += 1;
				}
			}
		if (num < 2)
			return Double.NaN;
		else
			return Math.sqrt(sigma/(num-1));
		}

	/**
	 * Help routine to find std. dev. of values in a double matrix.
	 */
	public static double sigmaOf (double[][] arr)
		{
		int ny=arr.length;
		if (ny == 0) return Double.NaN;
		int nx=arr[0].length;
		if (nx == 0) return Double.NaN;

		double avg = averageOf(arr);
		double sigma = 0.0;
		double diff = 0.0;
		int num = 0;
		for (int j=0; j < ny; j++)
			for (int i=0; i < nx; i++)
				{
				if (!Double.isNaN(arr[j][i]))
					{
					diff = (arr[j][i]-avg);
					sigma += diff*diff;
					num += 1;
					}
				}
		if (num < 2)
			return Double.NaN;
		else
			return Math.sqrt(sigma/(num-1));
		}

	/**
	 * Help routine to find average of values in a double matrix.
	 */
	public static double averageOf (double[][] arr)
		{
		int ny=arr.length;
		if (ny == 0) return Double.NaN;
		int nx=arr[0].length;
		if (nx == 0) return Double.NaN;

		double sum = 0.0;
		int num = 0;
		for (int j=0; j < ny; j++)
			for (int i=0; i < nx; i++)
				{
				if (!Double.isNaN(arr[j][i]))
					{
					sum += arr[j][i];
					num += 1;
					}
				}
		if (num == 0)
			return Double.NaN;
		else
			return sum/num;
		}

	/**
	 * Help routine to find average of values in a double array.
	 */
	public static double averageOf (double[] arr)
		{
		int n=arr.length;
		if (n == 0) return Double.NaN;

		double sum = 0.0;
		int num = 0;
		for (int i=0; i < n; i++)
			{
			if (!Double.isNaN(arr[i]))
				{
				sum += arr[i];
				num += 1;
				}
			}
		if (num == 0)
			return Double.NaN;
		else
			return sum/num;
		}

	}
