/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fragspace;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import molecule.DENOPTIMAttachmentPoint;
import exception.DENOPTIMException;
import utils.FragmentUtils;
import constants.DENOPTIMConstants;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;


/**
 * Utility class for the fragment space
 * 
 * @author Marco Foscato
 */
public class FragmentSpaceUtils
{

//------------------------------------------------------------------------------

    /**
     * Classify a fragment in terms of the number of APs and possibly their 
     * type (AP-Class).
     * @param mol the molecular representation of the fragment
     * @param type the type of fragment
     * @param id the index pf the fragment in the library
     * @throws DENOPTIMException
     */

    public static void classifyFragment(IAtomContainer mol, int type, 
					    int fragId) throws DENOPTIMException
    {
	// Classify according to number of APs
        String apProperty = mol.getProperty(DENOPTIMConstants.APTAG).toString();
        String[] tmpArr = apProperty.split("\\s+");
        int nAps = tmpArr.length;
	if (nAps != 0)
	{
            if (FragmentSpace.getMapOfFragsPerNumAps().containsKey(nAps))
            {
                FragmentSpace.getFragsWithNumAps(nAps).add(fragId);
            }
            else
            {
                ArrayList<Integer> lst = new ArrayList<>();
                lst.add(fragId);
                FragmentSpace.getMapOfFragsPerNumAps().put(nAps,lst);
            }
	}

	if (FragmentSpaceParameters.useAPclassBasedApproach())
	{
	    // Collect classes per fragment
	    ArrayList<String> lstAPC = FragmentUtils.getClassesForFragment(mol);
            FragmentSpace.getMapAPClassesPerFragment().put(fragId,lstAPC);

	    // Classify according to AP-Classes
            ArrayList<DENOPTIMAttachmentPoint> lstAPs =
				       new ArrayList<DENOPTIMAttachmentPoint>();
	    try
	    {
                lstAPs = FragmentUtils.getAPForFragment(mol);
	    }
	    catch (DENOPTIMException de)
	    {
		String msg = de.getMessage() + " -> Check " 
			     + FragmentUtils.getFragmentType(type) 
			     + " MolID: " + fragId;
		throw new DENOPTIMException(msg);
	    }

	    for (int j=0; j<lstAPs.size(); j++)
	    {
		ArrayList<Integer> apId = new ArrayList<Integer>();
		apId.add(fragId);
		apId.add(j);
		String cls = lstAPs.get(j).getAPClass();
	        if (FragmentSpace.getMapFragsAPsPerAPClass().containsKey(cls))
		{
		    FragmentSpace.getMapFragsAPsPerAPClass().get(cls).add(apId);
		}
		else
		{
		    ArrayList<ArrayList<Integer>> outLst = 
					    new ArrayList<ArrayList<Integer>>();
		    outLst.add(apId);
		    FragmentSpace.getMapFragsAPsPerAPClass().put(cls,outLst);
		}
	    }
	}
    }

//------------------------------------------------------------------------------

}
