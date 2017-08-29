package featureextractor.measurements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import data.Cell3D;
import data.Coordinates;
import data.Nucleus3D;
import data.SegmentMeasurements;
import featureextractor.Feature_Extractor_3D;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import utils.Measurer;

/**
 * This is a helper class that provides a list of Cell3D including measurements based on an original image, a DAPI channel image and (optionally) an actin channel image. This is the controlling class that will call any specific measurement
 * implementations if need be.
 */
public class CellMeasurer
{
	private static class VoxelData
	{
		public List<Double>[] voxelIntensityValues;

		public List<Coordinates>[] voxelCoordinates;

		public List<Coordinates>[] outlines;
	}

	private static final int EXTRA_SIGNAL_RADIUS = 10;


	/**
	 * Check if the value of a voxel is NOT equal to a given label value or zero. This method can be used to determine if a given voxel is part of the current segment (identified with the label given). If not, it will be added to the list of touching
	 * neighbours.
	 *
	 * @param aXCoordinate
	 *            The X-coordinate of the voxel
	 * @param aYCoordinate
	 *            The Y-coordinate of the voxel
	 * @param aZCoordinate
	 *            The Z-coordinate of the voxel
	 * @param aLabel
	 *            The current segment label (an Integer) to which to match the voxel value
	 * @param aImageStack
	 *            The image stack containing the voxels
	 * @param aTouchingNeighbourLabelList
	 *            The list of already found touching neighbour labels
	 */
	private static void checkVoxelLabel(final int aXCoordinate, final int aYCoordinate, final int aZCoordinate, final int aLabel, final ImageStack aImageStack,
			final Set<Integer> aTouchingNeighbourLabelList)
	{
		final int label = (int) aImageStack.getVoxel(aXCoordinate, aYCoordinate, aZCoordinate);
		if (label != 0 && label != aLabel)
		{
			aTouchingNeighbourLabelList.add(label);
		}
	}


	/**
	 * Compute the outline coordinates for each labelled segment. An outline coordinate is a non-zero voxel that is differently labelled compared to any of the directly adjacent voxels. The method returns an Array of Lists of Coordinates, where each
	 * list contains the outline coordinates of one nucleus. The indices in the result array match those of the 'labels' array.
	 *
	 * @param aLabelImage
	 *            The image where each nucleus is drawn completely in its label colours.
	 * @param aLabels
	 *            The list of available labels in the label image
	 * @param aLabelIndices
	 *            The index numbers for each label, to match the order of the outlines List with that of the labels List
	 *
	 * @return An array of lists of Coordinates where each list 'i' contains the outline Coordinates of the label at index 'i' in the labels input List.
	 */
	private static List<Coordinates>[] computeOutlines(final ImagePlus aLabelImage, final int[] aLabels, final Map<Integer, Integer> aLabelIndices)
	{
		// Compute the outlines of a nucleus object
		final int nrOfLabels = aLabels.length;
		final ImageStack image = aLabelImage.getImageStack();
		final int width = aLabelImage.getWidth();
		final int height = aLabelImage.getHeight();
		final int depth = aLabelImage.getNSlices();

		@SuppressWarnings("unchecked")
		final ArrayList<Coordinates>[] outlines = new ArrayList[nrOfLabels];

		// Make a list of each position in the array
		for (int i = 0; i < nrOfLabels; i++)
		{
			outlines[i] = new ArrayList<>();
		}

		// Loop through all the position of the images
		// This approach make use of the fact that the pixels outside of the have the value 0
		for (int z = 0; z < depth; z++)
		{
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					// Calculate the value of the current pixel and the next pixel
					final double value = image.getVoxel(x, y, z);

					// If these values are different add the pixel to the array which do not have the value 0
					// Resulting that you add only the pixels that are inside of the nucleus, and not outside
					final double valueNextX = x == width - 1 ? 0 : image.getVoxel(x + 1, y, z);
					if (valueNextX != value)
					{
						if (valueNextX > 0)
						{
							outlines[aLabelIndices.get((int) valueNextX)].add(new Coordinates(x + 1, y, z));
						}
						if (value > 0)
						{
							outlines[aLabelIndices.get((int) value)].add(new Coordinates(x, y, z));
						}
					}
					else if (x == 0 && value != 0)
					{
						// Also trigger if the starting border already has a non-zero label
						outlines[aLabelIndices.get((int) value)].add(new Coordinates(x, y, z));
					}

					// Do the same for Y direction
					final double valueNextY = y == height - 1 ? 0 : image.getVoxel(x, y + 1, z);
					if (valueNextY != value)
					{
						if (valueNextY > 0)
						{
							outlines[aLabelIndices.get((int) valueNextY)].add(new Coordinates(x, y + 1, z));
						}
						if (value > 0)
						{
							outlines[aLabelIndices.get((int) value)].add(new Coordinates(x, y, z));
						}
					}
					else if (y == 0 && value != 0)
					{
						// Also trigger if the starting border already has a non-zero label
						outlines[aLabelIndices.get((int) value)].add(new Coordinates(x, y, z));
					}

					// Do the same for Z direction
					final double valueNextZ = z == depth - 1 ? 0 : image.getVoxel(x, y, z + 1);
					if (valueNextZ != value)
					{
						if (valueNextZ > 0)
						{
							outlines[aLabelIndices.get((int) valueNextZ)].add(new Coordinates(x, y, z + 1));
						}
						if (value > 0)
						{
							outlines[aLabelIndices.get((int) value)].add(new Coordinates(x, y, z));
						}
					}
					else if (z == 0 && value != 0)
					{
						// Also trigger if the starting border already has a non-zero label
						outlines[aLabelIndices.get((int) value)].add(new Coordinates(x, y, z));
					}
				}
			}
		}
		return outlines;
	}


	/**
	 * Compute the 'touching' neighbours of a segment. Another segment is considered touching if it has at least a pixel directly adjacent to or 1 pixel removed from a pixel of the original nucleus. The exact distance required depends of whether
	 * background 'dams' have been used to distinguish between segment in the image or not. If no dams have been used, direct adjacency is required, while with dams the 1 pixel distance is sufficient to be considered touching.
	 *
	 * @param aOutline
	 *            The list of coordinates describing the border of the original segment
	 * @param aLabel
	 *            The label number of the original segment to differentiate from the other segments
	 * @param aLabelImage
	 *            The image containing the segments drawn in their label colour
	 * @param aCalculateDams
	 *            Have 'dams' been used to separate the segments in the label image
	 */
	private static Set<Integer> computeTouchingNeighbors(final List<Coordinates> aOutline, final int aLabel, final ImagePlus aLabelImage, final boolean aCalculateDams)
	{
		final ImageStack imageStack = aLabelImage.getImageStack();
		final TreeSet<Integer> list = new TreeSet<>();
		final int steps = aCalculateDams ? 2 : 1;

		// For each outline Coordinate of one nucleus determine the position of the coordinate
		for (final Coordinates coordinate : aOutline)
		{
			final int xValue = (int) coordinate.getXcoordinate();
			final int yValue = (int) coordinate.getYcoordinate();
			final int zValue = (int) coordinate.getZcoordinate();
			final int currentLabel = (int) imageStack.getVoxel(xValue, yValue, zValue);
			if (currentLabel == aLabel)
			{// If this pixel have the same value as the nucleus
				if (xValue < aLabelImage.getWidth() - steps && xValue >= steps)
				{
					checkVoxelLabel(xValue + steps, yValue, zValue, currentLabel, imageStack, list);
					checkVoxelLabel(xValue - steps, yValue, zValue, currentLabel, imageStack, list);
				}

				if (yValue < aLabelImage.getHeight() - steps && yValue >= steps)
				{
					checkVoxelLabel(xValue, yValue + steps, zValue, currentLabel, imageStack, list);
					checkVoxelLabel(xValue, yValue - steps, zValue, currentLabel, imageStack, list);
				}

				if (zValue < aLabelImage.getNSlices() - steps && zValue >= steps)
				{
					checkVoxelLabel(xValue, yValue, zValue + steps, currentLabel, imageStack, list);
					checkVoxelLabel(xValue, yValue, zValue - steps, currentLabel, imageStack, list);
				}
			}
		}
		return list;
	}


	/**
	 * Collect the coordinates of all nuclei/cells based on their label and simultaneously register the gray values of these voxels. This method simply goes pixel by pixel through the labelled image and stores any non-zero pixel location it finds
	 * under the appropriate label. The gray values are stored based on label number as well, but without any link to the pixel coordinates that produced them.
	 *
	 * @param aOriginalImage
	 *            The original intensity image
	 * @param aLabelImage
	 *            The image containing the labelled nuclei/cells
	 * @param aLabels
	 *            The list of available labels in the label image
	 * @param aLabelIndices
	 *            The index numbers for each label, to match the order of the results List with that of the labels List
	 *
	 * @return A VoxelData object containing the three lists for the outlines, the full voxel coordinates and the intensity values of each segment
	 */
	@SuppressWarnings("unchecked")
	private static VoxelData determineVoxelLocationAndintensity(final ImagePlus aOriginalImage, final ImagePlus aLabelImage, final int[] aLabels, final Map<Integer, Integer> aLabelIndices)
	{
		// Create two array lists, one with doubles and one with Coordinates, both with length of all the labels
		final int nrOfLables = aLabels.length;
		final List<Double>[] grayValueOfAllVoxels = new ArrayList[nrOfLables];
		final List<Coordinates>[] coordinatesOfAllVoxels = new ArrayList[nrOfLables];
		for (int i = 0; i < nrOfLables; i++)
		{
			grayValueOfAllVoxels[i] = new ArrayList<>();
			coordinatesOfAllVoxels[i] = new ArrayList<>();
		}

		// For each XYZ position in image
		// - add the gray value of the original image to the objectVoxels array
		// - add the coordinate to the objectXYZ array
		for (int z = 1; z <= aLabelImage.getNSlices(); z++)
		{
			final ImageProcessor grayIP = aOriginalImage.getImageStack().getProcessor(z);
			final ImageProcessor labelsIP = aLabelImage.getImageStack().getProcessor(z);

			for (int x = 0; x < aLabelImage.getWidth(); x++)
			{
				for (int y = 0; y < aLabelImage.getHeight(); y++)
				{
					final int labelValue = (int) labelsIP.getf(x, y);
					if (labelValue != 0)
					{
						grayValueOfAllVoxels[aLabelIndices.get(labelValue)].add((double) grayIP.getf(x, y));
						coordinatesOfAllVoxels[aLabelIndices.get(labelValue)].add(new Coordinates(x, y, z - 1)); // Slices start at 1, z-coordinates at 0
					}
				}
			}
		}
		// Compute the outline of each nucleus
		final List<Coordinates>[] outlinesvoxels = computeOutlines(aLabelImage, aLabels, aLabelIndices);

		final VoxelData voxData = new VoxelData();
		voxData.voxelIntensityValues = grayValueOfAllVoxels;
		voxData.voxelCoordinates = coordinatesOfAllVoxels;
		voxData.outlines = outlinesvoxels;
		return voxData;
	}


	/**
	 * Returns the set of unique labels existing in the given stack, excluding the value zero (used for background).
	 *
	 * @param aImage
	 *            a 3D label image
	 * @return the list of unique labels present in image (without background)
	 */
	private static int[] findAllLabels(final ImageStack aImage)
	{
		final TreeSet<Integer> labels = new TreeSet<>();

		// iterate on image pixels
		for (int z = 0; z < aImage.getSize(); z++)
		{
			for (int y = 0; y < aImage.getHeight(); y++)
			{
				for (int x = 0; x < aImage.getWidth(); x++)
				{
					labels.add((int) aImage.getVoxel(x, y, z));
				}
			}
		}
		// remove 0 if it exists
		if (labels.contains(0))
		{
			labels.remove(0);
		}

		// convert to array of integers
		final int[] array = new int[labels.size()];
		final Iterator<Integer> iterator = labels.iterator();
		for (int i = 0; i < labels.size(); i++)
			array[i] = iterator.next();

		return array;
	}


	/**
	 * Initialize the measurements by reading the input (grayscale) image and its corresponding labels.
	 *
	 * @param inputImage
	 *            input (grayscale) image
	 * @param labelImage
	 *            label image (labels are positive integer values)
	 */
	public static Cell3D[] getMeasuredCells(final ImagePlus aDAPIInputImage, final ImagePlus aActinInputImage, final ImagePlus aDAPILabelImage, final ImagePlus aActinLabelImage,
			final boolean[] aCalculateDams, final boolean[] aParticleMeasure, final boolean[] aMCIB3DMeasurementSet)
	{
		// TODO Why does the label image need to be a array and when does it contain 2 channels and when not?

		final int[] labels = findAllLabels(aDAPILabelImage.getImageStack());
		final int numLabels = labels.length;
		final Map<Integer, Integer> labelIndices = new HashMap<>();
		for (int i = 0; i < numLabels; i++)
		{
			labelIndices.put(labels[i], i);
		}

		final VoxelData dapiData = determineVoxelLocationAndintensity(aDAPIInputImage, aDAPILabelImage, labels, labelIndices);
		VoxelData actinData = new VoxelData();

		if (aActinLabelImage != null)
		{
			actinData = determineVoxelLocationAndintensity(aActinInputImage, aActinLabelImage, labels, labelIndices);
		}

		final Cell3D[] cells = new Cell3D[numLabels];
		final Calibration calibration = aDAPIInputImage.getCalibration();
		final double volumePerVoxel = calibration.pixelWidth * calibration.pixelHeight * calibration.pixelDepth;

		// Fill the array list of nucleus
		for (int i = 0; i < numLabels; i++)
		{
			Set<Integer> touchingNeighborsCell = null;
			if (aActinLabelImage != null)
			{
				touchingNeighborsCell = computeTouchingNeighbors(actinData.outlines[i], labels[i], aActinLabelImage, aCalculateDams[1]);
			}
			final Nucleus3D nucleus = new Nucleus3D(labels[i], dapiData.voxelCoordinates[i], dapiData.voxelIntensityValues[i], dapiData.outlines[i], volumePerVoxel);
			final Cell3D cell = new Cell3D(nucleus);
			if (aActinLabelImage != null && nucleus.getNumberOfVoxels() != 0)
			{
				cell.addCellFeatures(actinData.voxelCoordinates[i], actinData.voxelIntensityValues[i], actinData.outlines[i], touchingNeighborsCell);
			}
			cells[i] = cell;
		}

		ParticleAnalyzer3D.runParticleAnalyzer3D(cells, aParticleMeasure, aDAPIInputImage, aDAPILabelImage, labels);
		MCIB3DMeasurements.setMeasurements(cells, aMCIB3DMeasurementSet, aDAPIInputImage, aDAPILabelImage);

		return cells;
	}


	/**
	 * Measure the intensity of all relevant voxels in the image for each cell according to the measurement type given. Note that the measurement will be done in the active channel! The measured voxel intensities are summerized (by mean, median etc)
	 * and added to the cell extra-signal measurements.
	 *
	 * @param aSignalImage
	 *            The image to measure on
	 * @param aMeasurement
	 *            The type of measurement (e.g. only the nucleus or the entire cell or etc.)
	 * @param aCells
	 *            The list of cells to measure on.,
	 */
	public static void measureCoordinatesIntensity(final ImagePlus aSignalImage, final String aMeasurement, final Cell3D[] aCells)
	{
		final double x = aSignalImage.getCalibration().getX(1);
		final double z = aSignalImage.getCalibration().getZ(1);

		if (aMeasurement.equals(Feature_Extractor_3D.NUCLEAR_CENTER))
		{
			for (final Cell3D cell : aCells)
			{
				final List<Double> intensities = Measurer.getIntensity3D(cell.getNucleus().getSeed(), EXTRA_SIGNAL_RADIUS, z / x, aSignalImage);

				cell.addSignalMeasurements(new SegmentMeasurements(intensities));
			}
		}
		else if (aMeasurement.equals(Feature_Extractor_3D.NUCLEAR))
		{
			for (final Cell3D cell : aCells)
			{
				final List<Double> intensities = Measurer.getIntensityCell3D(cell.getNucleus().getNucleusCoordinates(), aSignalImage);

				cell.addSignalMeasurements(new SegmentMeasurements(intensities));
			}
		}
		else if (aMeasurement.equals(Feature_Extractor_3D.CELL))
		{
			for (final Cell3D cell : aCells)
			{
				final List<Double> intensities = Measurer.getIntensityCell3D(cell.getCoordinates(), aSignalImage);

				cell.addSignalMeasurements(new SegmentMeasurements(intensities));
			}
		}
		else
		{
			// Otherwise, measure cell without nucleus voxels.
			for (final Cell3D cell : aCells)
			{
				final List<Coordinates> coordinates = cell.getCoordinates();
				coordinates.removeAll(cell.getNucleus().getNucleusCoordinates());
				final List<Double> intensities = Measurer.getIntensityCell3D(coordinates, aSignalImage);

				cell.addSignalMeasurements(new SegmentMeasurements(intensities));
			}
		}
	}

}