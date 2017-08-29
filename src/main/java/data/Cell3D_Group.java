package data;

import java.util.ArrayList;
import java.util.List;

import ij.IJ;

/**
 * A representation of a connected group of cells in the image. This can be a single cells, a group of migrating cells or the spheroid and connected strands.
 *
 * @author Merijn van Erp, Esther Markus
 */
public class Cell3D_Group
{
	// The migration mode possibilities
	public static final String UNKNOWN = "UNKNOWN", SINGLE = "SINGLE_CELL", DUAL = "DUAL_CLUSTER", MULTI = "MULTI_CLUSTER", NONE = "NONE";

	private final ArrayList<Cell3D> members = new ArrayList<>();

	// The migration modes based on automatic cells and manual markers
	private String migrationMode = UNKNOWN;
	private String manualMigrationMode = null;

	// The list of different cells by marker migration mode
	private final ArrayList<Integer> unknown = new ArrayList<>();
	private final ArrayList<Integer> multiCell = new ArrayList<>();
	private final ArrayList<Integer> dualCell = new ArrayList<>();
	private final ArrayList<Integer> singleCells = new ArrayList<>();
	private final ArrayList<Integer> noneCell = new ArrayList<>();

	// Store for centre calculations
	private double meanX = 0;
	private double meanY = 0;
	private double meanZ = 0;

	// Count the cell identification correctness
	private boolean countNucleusMarkers = false;
	private double nucleusSelected = 0;
	private int nucleusNOTSelected = 0;
	private int nucleusExcludedBorder = 0;
	private int nucleiToSmall = 0;
	private int nucleusExludedBorderAndSize = 0;
	private int excludedWasCorrectSegmented = 0;
	private int excludedWasOverSegmented = 0;
	private int excludedWasUnderSegmented = 0;
	private int nucleusTwiceExcluded = 0;
	private int nucleuswithMulipleMarkers = 0;
	private int nucleusWithSeed = 0;
	private int nucleusWithSeedAndMarker = 0;

	// Counts of the migration mode correctness
	private boolean countNucleusMigrationMode = false;
	private int nucleusWithCorrectMigrationMode = 0;
	private int nucleusWithWrongMigrationMode = 0;
	private int nucleusWithoutMigrationMode = 0;
	private int singleCellWithoutMigrationMode = 0;
	private int singleCellWithWrongMigrationMode = 0;
	private int singleCellWithCorrectMigrationMode = 0;
	private int coreCellWithoutMigrationMode = 0;
	private int coreCellWithMigrationMode = 0;
	private int dualCellWithCorrectMigrationMode = 0;
	private int dualCellWithoutMigrationMode = 0;
	private int dualCellWithWrongMigrationMode = 0;
	private int multiCellWithCorrectMigrationMode = 0;
	private int multiCellWithWrongMigrationMode = 0;
	private int multiCellWithoutMigrationMode = 0;
	private int singleCellFalsePositive = 0;

	// Sizes
	private double totalVolume = 0;
	private double totalVolumeCell = 0;

	// Mean of the cell measurements
	private double meanGrayValue = 0;
	private double meanSTDV = 0;
	private double meanSkewness = 0;
	private double meanKurtosis = 0;
	private double meanVolume = 0;
	private double meanNumberOfVoxels = 0;
	private double meanSurfaceArea = 0;
	private double meanSphericities = 0;
	private double[] meanEllipsoid = null;
	private double[] meanElongation = null;
	private double[] meanInscribedSphere = null;

	private double meanGray3D = 0;
	private double meanSTDV3D = 0;

	private double meanVolumePixels;
	private double meanVolumeUnits;
	private double meanAreaPixels;
	private double meanAreaUnits;

	private double meanCompactness;
	private double meanSphericity;
	private double meanElongatio;
	private double meanFlatness;
	private double meanSpareness;


	/**
	 * Create a Cell3D_Group with at least one member.
	 *
	 * @param aStartMember
	 *            The first Cell3D for this group
	 */
	public Cell3D_Group(final Cell3D aStartMember)
	{
		this.members.add(aStartMember);
	}


	/**
	 * Create a Cell3D_Group out of a List of Cell3D objects.
	 *
	 * @param aStartMembers
	 *            The first set of Cell3D objects of this Cell3D_Group
	 */
	public Cell3D_Group(final List<Cell3D> aStartMembers)
	{
		this.members.addAll(aStartMembers);
	}


	/**
	 * Compare the migration mode of the group as determined by the automated migration analysis with the 'golden truth' of the manual migration mode(s) of the marker(s) contained in the Cell3Ds themselves. Note that it compares on a pairwise (group
	 * vs cell) basis, so it is theoretically possible to have a single-cell group containing two manually-annotated single-cell markers and count that as two successes.
	 */
	private void countMigrationModeAccuracy()
	{
		final String migrationModeGroup = getMigrationmode();

		// Now check the manual migration mode of all member Cell3Ds
		for (int i = 0; i < this.members.size(); i++)
		{
			final String migrationModeIndividual = this.members.get(i).getMarkerMigrationMode();
			if (!migrationModeGroup.equals(migrationModeIndividual))
			{
				// We have a disagreement, determine which specific type and keep count of it.
				if (migrationModeGroup.equals(SINGLE))
				{
					// This should not have been a single cell
					this.singleCellFalsePositive = this.singleCellFalsePositive + 1;
				}
				else if (migrationModeGroup.equals(UNKNOWN) || migrationModeGroup.equals("NONE"))
				{
					// Note: UNKNOWN is mostly considered the spheroid itself, so no migration, while NONE stand in for 'no migration mode set'.
					this.nucleusWithoutMigrationMode = this.nucleusWithoutMigrationMode + 1;

					// Now determine what should have been
					if (migrationModeIndividual.equals(SINGLE))
					{
						this.singleCellWithoutMigrationMode = this.singleCellWithoutMigrationMode + 1;
					}
					else if (migrationModeIndividual.equals(DUAL))
					{
						this.dualCellWithoutMigrationMode = this.dualCellWithoutMigrationMode + 1;
					}
					else if (migrationModeIndividual.equals(MULTI))
					{
						this.multiCellWithoutMigrationMode = this.multiCellWithoutMigrationMode + 1;
					}
				}
				else
				{
					// Not single or no migration (the two major cases). Differentiation of the false positives is not needed.
					this.nucleusWithWrongMigrationMode = this.nucleusWithWrongMigrationMode + 1;

					// The differentiation of the 'should have beens' is interesting however.
					if (migrationModeIndividual.equals(SINGLE))
					{
						this.singleCellWithWrongMigrationMode = this.singleCellWithWrongMigrationMode + 1;
					}
					else if (migrationModeIndividual.equals(DUAL))
					{
						this.dualCellWithWrongMigrationMode = this.dualCellWithWrongMigrationMode + 1;
					}
					else if (migrationModeIndividual.equals(MULTI))
					{
						this.multiCellWithWrongMigrationMode = this.multiCellWithWrongMigrationMode + 1;
					}
					else if (migrationModeIndividual.equals(UNKNOWN))
					{
						this.coreCellWithMigrationMode = this.coreCellWithMigrationMode + 1;
					}
				}
			}
			else
			{
				// Yay, correct migration analysis
				this.nucleusWithCorrectMigrationMode = this.nucleusWithCorrectMigrationMode + 1;

				// Now count what went well
				if (migrationModeIndividual.equals(SINGLE))
				{
					this.singleCellWithCorrectMigrationMode = this.singleCellWithCorrectMigrationMode + 1;
				}
				else if (migrationModeIndividual.equals(UNKNOWN))
				{
					this.coreCellWithoutMigrationMode = this.coreCellWithoutMigrationMode + 1;
				}
				else if (migrationModeIndividual.equals(DUAL))
				{
					this.dualCellWithCorrectMigrationMode = this.dualCellWithCorrectMigrationMode + 1;
				}
				else if (migrationModeIndividual.equals(MULTI))
				{
					this.multiCellWithCorrectMigrationMode = this.multiCellWithCorrectMigrationMode + 1;
				}
			}
		}

		// Register that the counting has been cached
		this.countNucleusMigrationMode = true;
	}


	/**
	 * For all cell nuclei in the group, count two things: 1) have they been disqualified for any reason and 2) have they been correctly segmented. The latter is determined by the number of manual markers that is contained in the cell. The aim is 1
	 * marker per cell. If this is the case, they are correctly segmented. Too few (i.e. 0) and the cell is over-segmented, while too many (> 1) is an under-segmented cell.
	 */
	private void countNucleusMarkers()
	{
		for (final Cell3D cell : this.members)
		{
			final Nucleus3D nucleus = cell.getNucleus();
			final boolean hasSeed = nucleus.getSeed() != null;
			final int countMarkers = nucleus.getMarkersCount();
			if (nucleus.isBorderNucleus() || nucleus.isDisqualified() || nucleus.isTooSmall())
			{
				// Why was is excluded?
				if ((nucleus.isBorderNucleus() || nucleus.isTooSmall()) && nucleus.isDisqualified())
				{
					this.nucleusTwiceExcluded = this.nucleusTwiceExcluded + 1;
				}
				else if (nucleus.isBorderNucleus() && nucleus.isTooSmall())
				{
					this.nucleusExludedBorderAndSize = this.nucleusExludedBorderAndSize + 1;
				}
				else if (nucleus.isBorderNucleus())
				{
					this.nucleusExcludedBorder = this.nucleusExcludedBorder + 1;
				}
				else if (nucleus.isDisqualified())
				{
					this.nucleusNOTSelected = this.nucleusNOTSelected + 1;
				}
				else if (nucleus.isTooSmall())
				{
					this.nucleiToSmall = this.nucleiToSmall + 1;
				}

				// Count correct segmentation
				if (nucleus.getMarkersCount() == 0)
				{
					this.excludedWasOverSegmented = this.excludedWasOverSegmented + 1;
				}
				else if (nucleus.getMarkersCount() == 1)
				{
					this.excludedWasCorrectSegmented = this.excludedWasCorrectSegmented + 1;
				}
				else
				{
					this.excludedWasUnderSegmented = this.excludedWasUnderSegmented + 1;
				}
			}
			else
			{
				// Not excluded or disqualified!
				this.nucleusSelected = this.nucleusSelected + 1;
				if (hasSeed)
				{
					// Has seed, so now see if correctly segmented
					if (countMarkers == 0)
					{
						this.nucleusWithSeed = this.nucleusWithSeed + 1;
					}
					else if (countMarkers == 1)
					{
						this.nucleusWithSeedAndMarker = this.nucleusWithSeedAndMarker + 1;
					}
					else
					{
						this.nucleuswithMulipleMarkers = this.nucleuswithMulipleMarkers + 1;
					}
				}
				else
				{
					// No seed? -> Error
					if (countMarkers == 0)
					{
						IJ.log("ERROR: there is a nucleus: " + nucleus.getLabel() + " without a seed and without a marker");
					}
					else if (countMarkers == 1)
					{
						IJ.log("ERROR there is a nucleus: " + nucleus.getLabel() + " without a seed with a marker");
					}
					else
					{
						IJ.log("ERROR: nucleus: " + nucleus.getLabel() + " failed");
					}
				}
			}
		}

		// Mark that the nucleus count has been cached
		this.countNucleusMarkers = true;
	}


	/**
	 * Get the point that has as coordinates the mean of all x-, y-, and z-coordinates.
	 *
	 * @return The Coordinates containing the mean of all x-, y-, and z-coordinates.
	 */
	public double[] getCenterXYZ()
	{
		for (final Cell3D cell : this.members)
		{
			final Nucleus3D nucleus = cell.getNucleus();
			this.meanX = this.meanX + nucleus.getSeed().getXcoordinate();
			this.meanY = this.meanY + nucleus.getSeed().getYcoordinate();
			this.meanZ = this.meanZ + nucleus.getSeed().getZcoordinate();
		}
		this.meanX = this.meanX / getMemberCount();
		this.meanY = this.meanY / getMemberCount();
		this.meanZ = this.meanZ / getMemberCount();
		final double[] result = { this.meanX, this.meanY, this.meanZ };
		return result;
	}


	/**
	 * Get, as a String, the labels of all the cells marked as part of the core or at least the largest cluster (tagged as 'UNKNOWN').
	 *
	 * @return The String containing all the cell labels separated by single spaces.
	 */
	public String getCoreCellLabels()
	{
		String names = "";
		for (int i = 0; i < this.unknown.size(); i++)
		{
			names = names + " " + this.unknown.get(i);
		}
		return names;
	}


	/**
	 * Get the number of cells that should belong to the core of the spheroid but that actually got assigned a migration label.
	 *
	 * @return The number of mislabelled core cells.
	 */
	public int getCoreCellWithMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.coreCellWithMigrationMode;
	}


	/**
	 * Get the number of shperoid-core cells that correctly got assigned no migration label.
	 *
	 * @return The number of correctly labelled core cells.
	 */
	public int getCoreCellWithoutMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.coreCellWithoutMigrationMode;
	}


	/**
	 * Get, as a String, the labels of all the cells marked as part of a dual cell cluster.
	 *
	 * @return The String containing all the cell labels separated by single spaces.
	 */
	public String getDualCellLabels()
	{
		String names = "";
		for (int i = 0; i < this.dualCell.size(); i++)
		{
			names = names + " " + this.dualCell.get(i);
		}
		return names;
	}


	/**
	 * Get the number of cells correctly labelled as belonging to a pair of migrating cells.
	 *
	 * @return The number of correctly labelled dual migration cells in this Cell3D_Group
	 */
	public int getDualCellWithCorrectMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.dualCellWithCorrectMigrationMode;
	}


	public int getDualCellWithoutMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.dualCellWithoutMigrationMode;
	}


	public int getDualCellWithWrongMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.dualCellWithWrongMigrationMode;
	}


	public String getManualMigrationModeGroup()
	{
		if (this.manualMigrationMode == null)
		{
			boolean dualIdentity = false;
			for (final Cell3D cell : this.members)
			{
				final Nucleus3D nucleus = cell.getNucleus();
				final String migrationMode = cell.getMarkerMigrationMode();
				switch (migrationMode)
				{
				case UNKNOWN:
					this.unknown.add(nucleus.getLabel());
					break;
				case MULTI:
					this.multiCell.add(nucleus.getLabel());
					break;
				case DUAL:
					this.dualCell.add(nucleus.getLabel());
					break;
				case SINGLE:
					this.singleCells.add(nucleus.getLabel());
					break;
				case NONE:
					this.noneCell.add(nucleus.getLabel());
					break;
				default:
					// Nothing needed, though this should not occur
				}

				if (this.manualMigrationMode == null)
				{
					this.manualMigrationMode = migrationMode;
				}
				else if (this.manualMigrationMode != migrationMode)
				{
					dualIdentity = true;
				}
			}

			if (dualIdentity == true)
			{
				this.manualMigrationMode = "MULTIPLE_IDENTITIES";
				IJ.log("MULTIPLE_IDENTITIES " + this.singleCells.size() + " " + this.dualCell.size() + " " + this.multiCell.size() + " " + this.unknown.size());
			}
		}
		return this.manualMigrationMode;
	}


	public double getMeanAreaPixels()
	{
		if (this.meanAreaPixels == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanAreaPixels = this.meanAreaPixels + cell.getNucleus().getMeasurements().getAreaPixels();
			}
			this.meanAreaPixels = this.meanAreaPixels / this.members.size();
		}
		return this.meanAreaPixels;
	}


	public double getMeanAreaUnits()
	{
		if (this.meanAreaUnits == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanAreaUnits = this.meanAreaUnits + cell.getNucleus().getMeasurements().getAreaUnit();
			}
			this.meanAreaUnits = this.meanAreaUnits / this.members.size();
		}
		return this.meanAreaUnits;
	}


	public double getMeanCompactness()
	{
		if (this.meanCompactness == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanCompactness = this.meanCompactness + cell.getNucleus().getMeasurements().getCompactness();
			}
			this.meanCompactness = this.meanCompactness / this.members.size();
		}
		return this.meanCompactness;
	}


	public double[] getMeanEllipsoid()
	{
		if (this.meanEllipsoid == null)
		{
			this.meanEllipsoid = new double[9];
			for (final Cell3D cell : this.members)
			{
				for (int j = 0; j < 9; j++)
				{
					this.meanEllipsoid[j] = this.meanEllipsoid[j] + cell.getNucleus().getMeasurements().getEllipsoid()[j];
				}
			}
			for (int j = 0; j < 9; j++)
			{
				this.meanEllipsoid[j] = this.meanEllipsoid[j] / this.members.size();
			}
		}
		return this.meanEllipsoid;
	}


	public double getMeanElongatio()
	{
		if (this.meanElongatio == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanElongatio = this.meanElongatio + cell.getNucleus().getMeasurements().getElongatio();
			}
			this.meanElongatio = this.meanElongatio / this.members.size();
		}
		return this.meanElongatio;
	}


	public double[] getMeanElongation()
	{
		if (this.meanElongation == null)
		{
			this.meanElongation = new double[3];
			for (final Cell3D cell : this.members)
			{
				for (int j = 0; j < 3; j++)
				{
					this.meanElongation[j] = this.meanElongation[j] + cell.getNucleus().getMeasurements().getElongations()[j];
				}
			}
			for (int j = 0; j < 3; j++)
			{
				this.meanElongation[j] = this.meanElongation[j] / this.members.size();
			}
		}
		return this.meanElongation;
	}


	public double getMeanFlatness()
	{
		if (this.meanFlatness == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanFlatness = this.meanFlatness + cell.getNucleus().getMeasurements().getFlatness();
			}
			this.meanFlatness = this.meanFlatness / this.members.size();
		}
		return this.meanFlatness;
	}


	public double getMeanGray3D()
	{
		if (this.meanGray3D == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanGray3D = this.meanGray3D + cell.getNucleus().getMeasurements().getMeanIntensity3D();
			}
			this.meanGray3D = this.meanGray3D / this.members.size();
		}
		return this.meanGray3D;
	}


	public double getMeanGrayValue()
	{
		if (this.meanGrayValue == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanGrayValue = this.meanGrayValue + cell.getNucleus().getMeasurements().getMeanIntensity();
			}
			this.meanGrayValue = this.meanGrayValue / this.members.size();
		}
		return this.meanGrayValue;
	}


	public double[] getMeanInscribedSphere()
	{
		if (this.meanInscribedSphere == null)
		{
			this.meanInscribedSphere = new double[4];
			for (final Cell3D cell : this.members)
			{
				for (int j = 0; j < 4; j++)
				{
					this.meanInscribedSphere[j] = this.meanInscribedSphere[j] + cell.getNucleus().getMeasurements().getInscribedSphere()[j];
				}
			}
			for (int j = 0; j < 4; j++)
			{
				this.meanInscribedSphere[j] = this.meanInscribedSphere[j] / this.members.size();
			}
		}
		return this.meanInscribedSphere;
	}


	public double getMeanKurtosis()
	{
		if (this.meanKurtosis == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanKurtosis = this.meanKurtosis + cell.getNucleus().getMeasurements().getKurtosis();
			}
			this.meanKurtosis = this.meanKurtosis / this.members.size();
		}
		return this.meanKurtosis;
	}


	public double getMeanNumberOfVoxels()
	{
		if (this.meanNumberOfVoxels == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanNumberOfVoxels = this.meanNumberOfVoxels + cell.getNucleus().getNumberOfVoxels();
			}
			this.meanNumberOfVoxels = this.meanNumberOfVoxels / this.members.size();
		}
		return this.meanNumberOfVoxels;
	}


	public double getMeanSkewness()
	{
		if (this.meanSkewness == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanSkewness = this.meanSkewness + cell.getNucleus().getMeasurements().getSkewness();
			}
			this.meanSkewness = this.meanSkewness / this.members.size();
		}
		return this.meanSkewness;
	}


	public double getMeanSpareness()
	{
		if (this.meanSpareness == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanSpareness = this.meanSpareness + cell.getNucleus().getMeasurements().getSpareness();
			}
			this.meanSpareness = this.meanSpareness / this.members.size();
		}
		return this.meanSpareness;
	}


	public double getMeanSphericities()
	{
		if (this.meanSphericities == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanSphericities = this.meanSphericities + cell.getNucleus().getMeasurements().getSphericities();
			}
			this.meanSphericities = this.meanSphericities / this.members.size();
		}
		return this.meanSphericities;
	}


	public double getMeanSphericity()
	{
		if (this.meanSphericity == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanSphericity = this.meanSphericity + cell.getNucleus().getMeasurements().getSphericity();
			}
			this.meanSphericity = this.meanSphericity / this.members.size();
		}
		return this.meanSphericity;
	}


	public double getMeanSTDV()
	{
		if (this.meanSTDV == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanSTDV = this.meanSTDV + cell.getNucleus().getMeasurements().getStandardDeviation();
			}
			this.meanSTDV = this.meanSTDV / this.members.size();
		}
		return this.meanSTDV;
	}


	public double getMeanSTDV3D()
	{
		if (this.meanSTDV3D == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanSTDV3D = this.meanSTDV3D + cell.getNucleus().getMeasurements().getMeanIntensity3D();
			}
			this.meanSTDV3D = this.meanSTDV3D / this.members.size();
		}
		return this.meanSTDV3D;
	}


	public double getMeanSurfaceArea()
	{
		if (this.meanSurfaceArea == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanSurfaceArea = this.meanSurfaceArea + cell.getNucleus().getMeasurements().getSurfaceArea();
			}
			this.meanSurfaceArea = this.meanSurfaceArea / this.members.size();
		}
		return this.meanSurfaceArea;
	}


	public double getMeanVolume()
	{
		if (this.meanVolume == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanVolume = this.meanVolume + cell.getNucleus().getVolume();
			}
			this.meanVolume = this.meanVolume / this.members.size();
		}
		return this.meanVolume;
	}


	public double getMeanVolumePixels()
	{
		if (this.meanVolumePixels == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanVolumePixels = this.meanVolumePixels + cell.getNucleus().getMeasurements().getVolumePixels();
			}
			this.meanVolumePixels = this.meanVolumePixels / this.members.size();
		}
		return this.meanVolumePixels;
	}


	public double getMeanVolumeUnits()
	{
		if (this.meanVolumeUnits == 0)
		{
			for (final Cell3D cell : this.members)
			{
				this.meanVolumeUnits = this.meanVolumeUnits + cell.getNucleus().getMeasurements().getVolumeUnit();
			}
			this.meanVolumeUnits = this.meanVolumeUnits / this.members.size();
		}
		return this.meanVolumeUnits;
	}


	public Cell3D getMember(final int index)
	{
		return this.members.get(index);
	}


	public int getMemberCount()
	{
		return this.members.size();
	}


	public String getMemberNames()
	{
		String names = "";
		for (final Cell3D cell : this.members)
		{
			if (cell.getNucleus() != null)
			{
				names = names + cell.getNucleus().getLabel() + " ";
			}

		}
		return names;
	}


	public List<Cell3D> getMembers()
	{
		return this.members;
	}


	/**
	 * Get the migration mode of this group as determined by the number of cells in the group (or UNKNOWN for the first and largest group).
	 *
	 * @return The migration mode as a String (or null if not set).
	 */
	public String getMigrationmode()
	{
		return this.migrationMode;
	}


	/**
	 * Get, as a String, the labels of all the cells marked as part of a multi-cell group.
	 *
	 * @return The String containing all the cell labels separated by single spaces.
	 */
	public String getMultiCellLabels()
	{
		String labels = "";
		for (final int cellLabel : this.multiCell)
		{
			labels = labels + cellLabel + " ";
		}

		if (!labels.equals(""))
		{
			// Remove the last " "
			labels = labels.substring(0, labels.length() - 1);
		}
		return labels;
	}


	/**
	 * Get the number of cells correctly labelled as belonging to a group (more than 2) of migrating cells.
	 *
	 * @return The number of correctly labelled multi-migration cells in this Cell3D_Group
	 */
	public int getMultiCellWithCorrectMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		// IJ.log("multiCellWithCorrectMigrationMode" + multiCellWithCorrectMigrationMode);
		return this.multiCellWithCorrectMigrationMode;
	}


	public int getMultiCellWithoutMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		/// IJ.log("MultiCellsWithoutMigrationmode" + multiCellWithoutMigrationMode);
		return this.multiCellWithoutMigrationMode;
	}


	public int getMultiCellWithWrongMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		// IJ.log("multiCellWithWrongMigrationMode" + multiCellWithWrongMigrationMode);
		return this.multiCellWithWrongMigrationMode;
	}


	public String getNoneCellNames()
	{
		String names = "";
		for (int i = 0; i < this.noneCell.size(); i++)
		{
			names = names + " " + this.noneCell.get(i);
		}
		return names;
	}


	public int getNucleusCorrectSegmented()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleusWithSeedAndMarker;
	}


	public int getNucleusExcludedBorder()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleusExcludedBorder;
	}


	public int getNucleusExcludedBorderAndSize()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleusExludedBorderAndSize;
	}


	public int[] getNucleusExcludedType()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		final int[] result = { this.excludedWasCorrectSegmented, this.excludedWasOverSegmented, this.excludedWasUnderSegmented };
		return result;
	}


	public int getNucleusNOTSelected()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleusNOTSelected;
	}


	public int getNucleusOverSegmented()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleusWithSeed;
	}


	public double getNucleusSelected()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleusSelected;
	}


	public int getNucleusToSmall()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleiToSmall;
	}


	public int getNucleusTwiceExcluded()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleusTwiceExcluded;
	}


	public int getNucleusUnderSegmented()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		return this.nucleuswithMulipleMarkers;
	}


	public int getNucleusWithCorrectMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}

		return this.nucleusWithCorrectMigrationMode;
	}


	public int getNucleusWithoutMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.nucleusWithoutMigrationMode;
	}


	public int getNucleusWithWrongMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.nucleusWithWrongMigrationMode;
	}


	public double getPercentageNucleusCorrectSegmented()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		final double percentage = (this.nucleusWithSeedAndMarker / this.nucleusSelected) * 100;
		return percentage;
	}


	public double getPercentageNucleusOverSegmented()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		final double percentageOverSegmented = (this.nucleusWithSeed / this.nucleusSelected) * 100;

		return percentageOverSegmented;
	}


	public double getPercentageNucleusUnderSegmented()
	{
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}
		final double percentage = (this.nucleuswithMulipleMarkers / this.nucleusSelected) * 100;
		return percentage;
	}


	public double getPercentageNucleusWithCorrectMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}

		final double percentageNucleusWithCorrectMigrationMode = (this.nucleusWithCorrectMigrationMode / this.nucleusSelected) * 100;
		return percentageNucleusWithCorrectMigrationMode;
	}


	public double getPercentageNucleusWithoutMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}

		final double percentageWithoutMigrationMode = (this.nucleusWithoutMigrationMode / this.nucleusSelected) * 100;
		return percentageWithoutMigrationMode;
	}


	public double getPercentageNucleusWrongMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		if (!this.countNucleusMarkers)
		{
			countNucleusMarkers();
		}

		final double percentageWrongMigrationMode = (this.nucleusWithWrongMigrationMode / this.nucleusSelected) * 100;
		return percentageWrongMigrationMode;
	}


	public boolean getPresenceOfMember(final Cell3D aCandidateMember)
	{
		boolean present = false;
		final int labelCandidateMember = aCandidateMember.getNucleus().getLabel();
		for (final Cell3D cell : this.members)
		{
			if (cell != null)
			{
				final int labelMember = cell.getNucleus().getLabel();
				if (labelMember == labelCandidateMember)
				{
					present = true;
					break;
				}
			}
		}
		return present;
	}


	public int getSingleCellFalsePositive()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.singleCellFalsePositive;
	}


	/**
	 * Get, as a String, the labels of all the cells marked as a single cell.
	 *
	 * @return The String containing all the cell labels separated by single spaces.
	 */
	public String getSingleCellLabels()
	{
		String names = "";
		for (int i = 0; i < this.singleCells.size(); i++)
		{
			names = names + " " + this.singleCells.get(i);
		}
		return names;
	}


	/**
	 * Get the number of cells correctly labelled as single migrating cells.
	 *
	 * @return The number of correctly labelled single migrating cells in this Cell3D_Group
	 */
	public int getSingleCellWithCorrectMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.singleCellWithCorrectMigrationMode;
	}


	/**
	 * Get the number of single migrating cells without a migration label (i.e. core cells).
	 *
	 * @return The number of single migrating cells without a migration label in this Cell3D_Group
	 */
	public int getSingleCellWithoutMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.singleCellWithoutMigrationMode;
	}


	public int getSingleCellWithWrongMigrationMode()
	{
		if (!this.countNucleusMigrationMode)
		{
			countMigrationModeAccuracy();
		}
		return this.singleCellWithWrongMigrationMode;
	}


	public double getTotalVolume()
	{
		if (this.totalVolume == 0)
		{
			for (int i = 0; i < getMemberCount(); i++)
			{
				this.totalVolume = this.totalVolume + getMember(i).getNucleus().getVolume();
			}
		}
		return this.totalVolume;
	}


	public double getTotalVolumeCell()
	{
		if (this.totalVolumeCell == 0)
		{
			for (int i = 0; i < getMemberCount(); i++)
			{
				this.totalVolumeCell = this.totalVolumeCell + getMember(i).getVolume();
			}
		}
		return this.totalVolumeCell;
	}


	public void setMember(final Cell3D aMember)
	{
		this.members.add(aMember);
	}


	public void setMigrationmode(final String aMigrationMode)
	{
		this.migrationMode = aMigrationMode;
	}
}
