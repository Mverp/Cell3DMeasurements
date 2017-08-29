package createmarkerimages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import data.Coordinates;
import data.PointValue;
import ij.IJ;
import ij.ImagePlus;
import utils.Nucleus3DFileUtils;

/**
 * The plugin Create_Marker_Image_3D is able to create marker images with different point detection methods in 2D and 3D. The plugin recognize the type of image and switch to 2D or 3D. For the 3D part the options are: Manual points, Manual points
 * translate and Mexican Hat. For the 2D part the options are: Manual points, mexican Hat, distancetransform and watershedpoints. - Mexican Hat: Fiji -> Plugin -> LoG3D (Daniel Sage) - Maxima finder: Fiji -> Plugin -> 3D -> 3D Maxima finder (code is
 * adapted)
 *
 * @author Esther
 *
 */
public class MarkerImageCreator3D extends BaseMarkerImageCreator
{

	/**
	 * Method markerImage3D loops through all the selected point detection methods
	 *
	 * @param originalImage,
	 *            the 3D gray scale image
	 * @param title,
	 *            of the original image to give the marker image the same name
	 * @param slices,
	 *            the amount of slices of the original image
	 * @param directory,
	 *            where the manual point file is save and were the marker images need to be saved
	 * @param names,
	 *            to select the manual point file
	 */
	static public void markerImage3D(final ImagePlus aOriginalImage, final String aDetectionMethod, final File aInputDirectory, final String aInputFileName, final File aOutputDirectory)
			throws IOException
	{
		final int totalSlices = aOriginalImage.getNSlices();

		// For each point detection method that is chosen by the user
		// Determine the point detection method and create the marker image name
		final String titlemethod = createOutputFileName(aOriginalImage, aDetectionMethod);
		if (aDetectionMethod.equals(MANUALPOINTS))
		{
			// Create a list (n=slices) of list (n= point detection points per slide)
			final List<List<PointValue>> listofslices = readManualPoints(totalSlices, aInputDirectory, aInputFileName);
			// Create a new image and save the marker image and the marker file

			createMarkerImage(aOriginalImage, listofslices, aOutputDirectory, titlemethod);
		}
		else // Mexican hat
		{
			// Perform the mexican hat and save the marker image and marker file
			mexicanHatMaster(aOriginalImage, titlemethod, aOutputDirectory);
		}
	}


	static public List<List<PointValue>> readManualPoints(final int aNrOfSlices, final File aDirectory, final String aFileName) throws IOException
	{

		IJ.log("   Start Manual Points");
		// Create a list (n=slices) of list (n= point detection points per slide)
		final List<List<PointValue>> listOfSlices = new ArrayList<>(aNrOfSlices);
		for (int i = 0; i < aNrOfSlices; i++)
		{
			listOfSlices.add(i, new ArrayList<PointValue>());
		}

		final List<Coordinates> seeds = Nucleus3DFileUtils.readNucleusSeeds(aDirectory, aFileName);
		for (final Coordinates seed : seeds)
		{
			listOfSlices.get((int) seed.getZcoordinate()).add(new PointValue(seed.getXcoordinate(), seed.getYcoordinate(), seed.getZcoordinate()));
		}

		return listOfSlices;
	}

}