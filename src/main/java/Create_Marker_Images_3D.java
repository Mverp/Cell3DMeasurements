
import java.io.File;
import java.io.IOException;

import createmarkerimages.BaseMarkerImageCreator;
import createmarkerimages.MarkerImageCreator3D;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.Duplicator;

/**
 * The plugin Create_Marker_Image_3D is able to create marker images with different point detection methods in 2D and 3D. The plugin recognize the type of image and switch to 2D or 3D. For the 3D part the options are: Manual points, Manual points
 * translate and Mexican Hat. For the 2D part the options are: Manual points, mexican Hat, distancetransform and watershedpoints. - Mexican Hat: Fiji -> Plugin -> LoG3D (Daniel Sage) - Maxima finder: Fiji -> Plugin -> 3D -> 3D Maxima finder (code is
 * adapted)
 *
 * @author Esther
 *
 */
public class Create_Marker_Images_3D extends Base_Segmentation_3D_Plugin
{
	// private static final String[] METHODS2D = { NONE, BaseMarkerImageCreator.MANUALPOINTS, BaseMarkerImageCreator.MEXICANHAT, BaseMarkerImageCreator.DISTANCETRANSFORM,
	// BaseMarkerImageCreator.WATERSHEDPONTS };

	private static final String[] METHODS3D = { NONE, BaseMarkerImageCreator.MANUALPOINTS, BaseMarkerImageCreator.MEXICANHAT };

	protected String pointDetectionMethod;


	/**
	 * Ask to select different methods for point detection. Repeating asking until it is cancelled.
	 *
	 * @return 'Exit' if the cancel button was chosen, 'Add extra marker images' if yes was chosen and 'Next step' for a choice of no.
	 */
	private boolean dialogMethodSelection(final String[] aMethods) // Dialog for the original image
	{
		final GenericDialog gd = new GenericDialog("Selection point detection method");

		// The input fields for each method to select the method and optional add a extra identity to the name
		gd.addMessage("Select the point detection method\n ");
		gd.addChoice("Method", aMethods, aMethods[0]);

		gd.showDialog();

		final String methodName = gd.getNextChoice();

		// When there is an input method, add name to order list and add name+identity to name list
		if (methodName != NONE)
		{
			this.pointDetectionMethod = methodName;
		}

		return gd.wasOKed();
	}


	@Override
	public void run(final String arg)
	{
		// Get the original image and test if it is a 2D or 3D image
		ImagePlus originalImage = IJ.getImage();

		if (originalImage.getNSlices() == 1)
		{
			IJ.showMessage("Prerequisite error", "The 3D marker images can only be created when the number of slices in the image > 1.");
			return;
		}

		boolean duplicated = false;
		if (originalImage.getNChannels() > 1)
		{
			// With a multi-channel image, select the current channel to work on. Duplicate it.
			final ImagePlus orgDup = new Duplicator().run(originalImage, originalImage.getChannel(), originalImage.getChannel(), 1, originalImage.getNSlices(), 1, originalImage.getNFrames());
			orgDup.setTitle(originalImage.getTitle());
			originalImage = orgDup;
			duplicated = true;
		}

		// Dialog for input to select the different point detection methods 3D, choose from String[] methods.
		final String[] methods = METHODS3D; // TODO : METHODS2D ?

		final boolean okayed = dialogMethodSelection(methods);

		if (!okayed || this.pointDetectionMethod == null)
		{
			// Nothing chosen, so ignore
			return;
		}

		File directoryInputFile = null;
		String nameInputFile = "";
		if (this.pointDetectionMethod.equals(BaseMarkerImageCreator.MANUALPOINTS))
		{
			// Select the directory where the manual annotations of the PointPicker 3D is located
			final String messageInPutFile = "Select the file with the manual annotations of the PointPicker 3D";
			final String prefInputFile = "Create_Marker_Image_3D.InputDir";

			directoryInputFile = fileLoadOrFileSave(messageInPutFile, prefInputFile, false);

			if (directoryInputFile == null)
			{
				return;
			}
			nameInputFile = directoryInputFile.getName();
		}

		// Select the directory where the marker files need to be saved
		final String messageOutPutFile = "Select the directory where the marker files need to be saved";
		final String prefOutputFile = "Create_Marker_Image_3D.OutputDir";
		final File directoryOutputFile = fileLoadOrFileSave(messageOutPutFile, prefOutputFile, true);
		if (directoryOutputFile == null)
		{
			return;
		}

		// In case of a 3D image start markerImage3D, which loops through all the point detection methods
		IJ.log("Start Create Marker Image 3D");

		try
		{
			MarkerImageCreator3D.markerImage3D(originalImage, this.pointDetectionMethod, directoryInputFile, nameInputFile, directoryOutputFile);

			if (duplicated)
			{
				// The 'originalImage' is actually a working duplicate. Clean it up.
				originalImage.changes = false;
				originalImage.close();
			}
		}
		catch (final IOException e)
		{
			IJ.log("Error In the line");
			return;
		}

		IJ.log("End Create marker image 3D");

		// TODO : 2D?
		// else if (originalImage.getNSlices() == 1) // In case of a 2D image start markerImage3D, which loops through all the point detection methods
		// {
		// IJ.log("Start Create Marker Image 2D");
		// try
		// {
		// MarkerImageCreator2D.markerImage2D(originalImage, directoryInputFile, directoryOutputFile, nameInputFile, this.pointDetectionMethod);
		// }
		// catch (final IOException e)
		// {
		// IJ.log("Error In the line");
		// return;
		// }
		//
		// if (closeOrig)
		// {
		// originalImage.changes = false;
		// originalImage.close();
		// }
		//
		// IJ.log("End Create marker image 2D");
		// }
	}

}