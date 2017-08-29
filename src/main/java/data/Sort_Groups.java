package data;

import java.util.Comparator;

/**
 * A comparator of cell groups based on group size.
 *
 * @author Merijn van Erp, Esther Markus
 *
 */
public class Sort_Groups implements Comparator<Cell3D_Group>
{
	/**
	 * Compare two groups of cell groups based on group size.
	 *
	 * @param aFirstCellGroup
	 *            The first cell group
	 * @param aSecondCellGroup
	 *            The second cell group
	 */
	@Override
	public int compare(final Cell3D_Group aFirstCellGroup, final Cell3D_Group aSecondCellGroup)
	{
		return aSecondCellGroup.getMemberCount() - aFirstCellGroup.getMemberCount();
	}
}
