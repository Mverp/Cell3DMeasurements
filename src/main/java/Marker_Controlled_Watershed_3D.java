
import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import utils.GenUtils;

//TODO Deze plugin kan zowel 2D als 3D images aan
// Hierbij wordt er alleen onderscheid gemaakt in de threshold
// Moet ik de Mediaan filter ook aanpassen naar een 2D mogelijkheid?
// Of moet ik 2D en 3D helemaal loskoppelen?

/**
 *
 * @author Esther This plugins use the following plugins: - Median filter: Fiji -> Process -> Median(3D) - Threshold: Fiji -> Image -> Adjust -> Threshold - Exact Euclidean Distance Transform: Fiji -> Process -> Exact Euclidean Distance Transform(3D)
 *         - Marker Controlled Watershed: Fiji --> Plugins -> MorpholibJ
 */
public class Marker_Controlled_Watershed_3D extends Base_Segmentation_3D_Plugin
{
	private static final String FILTER = "Median";
	private static final String MANUAL = "Manual", LI = "Li", HUANG = "Huang", DEFAULT = "Default", TRIANGLE = "Triangle", INTERMODES = "InterModes", ISODATA = "IsoData", YEN = "Yen";
	private static final String MEAN = "Mean", MEDIAN = "Median";
	private static final String[] THRESHOLD = { MANUAL, LI, HUANG, DEFAULT, TRIANGLE, INTERMODES, ISODATA, YEN };
	private static String[] FILTERS = { MEDIAN, MEAN };
	public static final String WATERSHED_INFIX = "_MCWatershed_";
	public static final String DAPI = "_DAPI_";
	public static final String ACTIN = "_Actin_";
	private String threshold;
	private String thresholdActin;
	private String filter;
	private String filterActin;
	boolean[] calculateDams = { false, false };
	private final int[] lowerUpperThreshold = null;
	private ImagePlus originalImage = null;
	private int dapiChannel = -1;
	private int actinChannel = -1;
	boolean segmentActinChannel = false;


	/**
	 * Apply the threshold that is set by the user
	 *
	 * @param originalImageMedian
	 */
	private void applyThreshold(final ImagePlus aOriginalImageMedian, final String aThreshold)
	{
		IJ.log("   Start threshold");
		if (this.originalImage.getNSlices() > 1)
		{
			if (aThreshold == MANUAL)
			{
				IJ.setAutoThreshold(aOriginalImageMedian, "Default dark stack");
				IJ.setThreshold(aOriginalImageMedian, this.lowerUpperThreshold[0], this.lowerUpperThreshold[1], null);
				IJ.log("      Threshold: " + this.lowerUpperThreshold[0] + " - " + this.lowerUpperThreshold[1]);
				final String thresholdInput = "method=Default background=Dark black";
				Prefs.blackBackground = true;
				IJ.run(aOriginalImageMedian, "Convert to Mask", thresholdInput);
			}
			else
			{
				final String threhsoldInput1 = aThreshold + " dark stack";
				final String threhsoldInput2 = "method=" + aThreshold + " background=Dark black";
				IJ.setAutoThreshold(aOriginalImageMedian, threhsoldInput1);
				IJ.run(aOriginalImageMedian, "Convert to Mask", threhsoldInput2);
				IJ.log("      Treshold: " + aThreshold);
			}
		}
		else
		{
			if (aThreshold == MANUAL)
			{
				IJ.setAutoThreshold(aOriginalImageMedian, "Default dark");
				IJ.setThreshold(aOriginalImageMedian, this.lowerUpperThreshold[0], this.lowerUpperThreshold[1], null);
				IJ.log("      Threshold: " + this.lowerUpperThreshold[0] + " - " + this.lowerUpperThreshold[1]);
				Prefs.blackBackground = true;
				IJ.run(aOriginalImageMedian, "Convert to Mask", "");
			}
			else
			{
				final String theresholdInput = aThreshold + " dark";
				IJ.setAutoThreshold(aOriginalImageMedian, theresholdInput);
				IJ.run(aOriginalImageMedian, "Make Binary", "");
				IJ.log("      Threshold: " + aThreshold);
			}
		}
		IJ.log("   End threshold");
	}


	/**
	 * Ask the user to select the marker images.
	 *
	 * @param aNames
	 *            The list of images available
	 *
	 * @return The image if one has been chosen, null otherwise (none chosen or cancelled)
	 */
	private ImagePlus dialogInputMarkerImages(final String[] aNames)
	{
		final GenericDialog gd = new GenericDialog("Image");
		gd.addMessage("Select the marker images");
		gd.addChoice("Marker Image", aNames, aNames[0]);
		gd.showDialog();

		if (!gd.wasCanceled())
		{
			final String imageIndex1 = gd.getNextChoice();

			if (imageIndex1 != NONE)
			{
				return WindowManager.getImage(imageIndex1);
			}
		}

		return null;
	}


	/**
	 * Ask the user to select the original image
	 *
	 * @param aNames
	 *            The names of the available images
	 * @return True if the dialog was cancelled, false if not
	 */
	private boolean dialogOriginalImage(final String[] aNames)
	{
		final GenericDialog gd = new GenericDialog("Original image");
		gd.addMessage("Select the original image");
		gd.addChoice("Original image", aNames, aNames[0]);
		gd.showDialog();

		if (gd.wasOKed())
		{
			final String originalImageName = gd.getNextChoice();
			this.originalImage = (WindowManager.getImage(originalImageName));
		}

		return gd.wasCanceled();
	}


	/**
	 * Ask the user the settings for the manual threshold.
	 *
	 * @return True if the dialog has been cancelled, false otherwise.
	 */
	private boolean dialogThresholdSettingManual()
	{
		final GenericDialog gd = new GenericDialog("Settings for the manual threshold");

		gd.addNumericField("Lower threshold level", 0, 0);
		gd.addNumericField("Upper threshold level", 65536, 0);
		gd.showDialog();

		final int lowerThreshold = (int) gd.getNextNumber();
		final int upperThreshold = (int) gd.getNextNumber();

		this.lowerUpperThreshold[0] = lowerThreshold;
		this.lowerUpperThreshold[1] = upperThreshold;
		return gd.wasCanceled();

	}


	/**
	 * Dialogue ThresholdSetting ask the user to select the threshold of preference
	 *
	 * @return if the dialogue was chanced --> if is true plugging will be stopped
	 */
	private boolean dialogueThresholdSetting()
	{
		final GenericDialog gd = new GenericDialog("Settings for the threshold");

		gd.addChoice("Set the filter", FILTERS, FILTERS[0]);
		gd.addChoice("Set threshold nucleus segments", THRESHOLD, THRESHOLD[0]);
		gd.addCheckbox("Calculate dams nucleus segments", true);
		if (this.segmentActinChannel)
		{
			gd.addChoice("Set the filter for actin", FILTERS, FILTERS[0]);
			gd.addChoice("Set threshold actin segments", THRESHOLD, THRESHOLD[0]);
			gd.addCheckbox("Calculate dams actin segments", false);
		}

		gd.showDialog();
		this.filter = gd.getNextChoice();
		this.threshold = gd.getNextChoice();
		this.calculateDams[0] = gd.getNextBoolean();
		if (this.segmentActinChannel)
		{
			this.filterActin = gd.getNextChoice();
			this.thresholdActin = gd.getNextChoice();
			this.calculateDams[1] = gd.getNextBoolean();
		}

		return gd.wasCanceled();

	}


	private ImagePlus filterAndThresholdChannel(final String aFilter, final int aChannel, final String aThreshold)
	{
		final ImagePlus segmentImage = new Duplicator().run(this.originalImage, aChannel, aChannel, 1, this.originalImage.getNSlices(), 1, 1);
		segmentImage.setTitle(this.originalImage.getTitle());

		// Median Filter
		// the Do loop is to prevent the program continues without the median image
		ImagePlus originalImageFilter = null;
		if (aFilter.equals(MEDIAN))
		{
			IJ.log("   Start Median filter 3D");
			IJ.run(segmentImage, "Median (3D)", "");
			do
			{
				originalImageFilter = IJ.getImage();
			} while (originalImageFilter.getTitle().equals(segmentImage.getTitle()));
			IJ.log("   End Median filter 3D");
		}
		else if (aFilter.equals(MEAN))
		{
			IJ.log("   Start Mean filter 3D");
			originalImageFilter = segmentImage.duplicate();
			IJ.run(originalImageFilter, "Mean 3D...", "x=3 y=3 z=3");
			int count = 0;
			for (int i = 0; i < 10000000; i++)
			{
				count = count + 1;
			}
			originalImageFilter.show();
			IJ.log("   End Mean filter 3D");
		}

		// Start of the threshold
		applyThreshold(originalImageFilter, aThreshold);

		segmentImage.close();
		return originalImageFilter;
	}


	private ImagePlus markerControlledWatershed(final ImagePlus aImage, final ImagePlus aMarkerImage, final File aOutputDirectory, final String aChannelName, final String aThreshold,
			final boolean aCalculateDams)
	{
		IJ.log("   Start Distance Map");
		ImagePlus originalImageDistance;
		final String subTitleMedian = GenUtils.getTitleNoExtension(aImage);
		final String inputDistance = "map=EDT image=" + subTitleMedian + " mask=None threshold=1 inverse";
		IJ.run("3D Distance Map", inputDistance);
		do
		{
			originalImageDistance = IJ.getImage();
		} while (originalImageDistance.getTitle().equals(aImage.getTitle()));
		IJ.log("   End Distance Map");

		// Input strings for the marker-controlled watershed
		final String maskName = "mask=" + aImage.getShortTitle();
		final String inputName = "input=" + originalImageDistance.getShortTitle();

		// For each marker image a marker-controlled watershed is performed
		IJ.log("   Marker-Controlled Watershed " + aMarkerImage.getShortTitle());
		final String markerName = "marker=" + aMarkerImage.getShortTitle();
		String options = "use";
		if (aCalculateDams)
		{
			options = "calculate use";
		}
		final String input = inputName + " " + markerName + " " + maskName + " " + options;
		IJ.log("     " + input);
		// Marker-Controlled watershed
		IJ.run("Marker-controlled Watershed", input);

		final ImagePlus imageseg = IJ.getImage();

		// Create the name of the segmented Image
		final String title = aMarkerImage.getTitle();
		final String title2 = title.substring(title.indexOf("Markers_") + 8);
		String nameSegImage = "";
		if (aThreshold == MANUAL)
		{
			nameSegImage = File.separator + this.originalImage.getShortTitle() + aChannelName + "_" + FILTER + "_" + aThreshold + "-" + this.lowerUpperThreshold[0] + "-" + this.lowerUpperThreshold[1]
					+ WATERSHED_INFIX + title2;
		}
		else
		{
			nameSegImage = File.separator + this.originalImage.getShortTitle() + aChannelName + "_" + FILTER + "_" + aThreshold + WATERSHED_INFIX + title2;
		}

		// Save the segmented image
		final String name = aOutputDirectory.getPath() + nameSegImage;
		IJ.saveAs(imageseg, "Tiff", name);
		IJ.log("Segmented image is save as: " + nameSegImage);
		IJ.log("   Segmented image is saved in " + name);
		originalImageDistance.close();
		return imageseg;
	}


	@Override
	public void run(final String aArg)
	{
		// For all images, the names are listed in the array ImagesNames to select the original image and the marker images
		final int amountWindows = WindowManager.getImageCount();
		final String[] imagesNames = new String[amountWindows + 1];
		imagesNames[0] = NONE;
		for (int i = 0; i < amountWindows; i++)
		{
			imagesNames[i + 1] = WindowManager.getImage(i + 1).getTitle();
		}

		boolean wasCanceld = false;
		do
		{
			wasCanceld = dialogOriginalImage(imagesNames);
			if (wasCanceld == true)
			{
				return;
			}
		} while (this.originalImage == null);

		if (this.originalImage.getNChannels() > 1)
		{
			selectChannels(this.originalImage);
		}

		final ImagePlus markerImage = dialogInputMarkerImages(imagesNames);

		if (markerImage == null)
		{
			return;
		}

		// If threshold is Manual the dialogue 'DialogueThresholdSettingManual' is used to set the manual threshold
		if (dialogueThresholdSetting())
		{
			return;
		}

		if (this.threshold == MANUAL && dialogThresholdSettingManual()) // i.e. the settings for dapi == settings for actin
		{
			return;
		}

		// Select the directory where the segmented images need to be saves
		final String messageInPutFile = "Select the directory where the segmented image need to be saved";
		final String prefInputFile = "Marker_Controlled_Watershed_3D.OuputDir";

		final File directoryOutputFile = fileLoadOrFileSave(messageInPutFile, prefInputFile, true);
		if (directoryOutputFile == null)
		{
			return;
		}

		// Start of the marker-controlled watershed
		IJ.log("Start Marker-controlled watershed");

		final ImagePlus originalImageFilter = filterAndThresholdChannel(this.filter, this.dapiChannel, this.threshold);

		final ImagePlus nucSegmentImage = markerControlledWatershed(originalImageFilter, markerImage, directoryOutputFile, DAPI, this.threshold, this.calculateDams[0]);
		if (this.segmentActinChannel)
		{
			final ImagePlus actinImageFilter = filterAndThresholdChannel(this.filterActin, this.actinChannel, this.thresholdActin);

			// Add the original filtered image to the actin filtered image to fill in the nucleus gaps (little actin there).
			final ImageCalculator ic = new ImageCalculator();
			final ImagePlus actinImageCombine = ic.run("Add create stack", originalImageFilter, actinImageFilter);
			actinImageFilter.close();
			actinImageCombine.show();

			markerControlledWatershed(actinImageCombine, nucSegmentImage, directoryOutputFile, ACTIN, this.thresholdActin, this.calculateDams[1]);
			actinImageCombine.close();
		}
		originalImageFilter.close();
		IJ.log("End Marker-controlled watershed 3D");
	}


	private boolean selectChannels(final ImagePlus aImage)
	{
		final int channels = aImage.getNChannels();
		final GenericDialog gd = new GenericDialog("Set channels Original image");
		final String[] channelchooser = new String[channels];
		for (int i = 0; i < channels; i++)
		{
			channelchooser[i] = i + 1 + "";
		}
		gd.addChoice("Nucleus Channel", channelchooser, channelchooser[0]);
		gd.addCheckbox("Segment actinSignal", false);
		gd.addChoice("Actin Channel", channelchooser, channelchooser[1]);
		gd.showDialog();

		this.dapiChannel = Integer.parseInt(gd.getNextChoice());
		this.segmentActinChannel = gd.getNextBoolean();
		this.actinChannel = Integer.parseInt(gd.getNextChoice());
		return gd.wasCanceled();
	}

}
