/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.molecule;

import java.util.ArrayList;
import java.util.logging.Level;
import java.io.Serializable;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.FragmentUtils;


/**
 * Data structure of a single vertex that contains information as to the
 * list of attachment points, type of this vertex and the index of the
 * molecular representation in a library of fragments of a given type
 * (scaffold, fragment, capping groups).
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMVertex implements Cloneable, Serializable
{
    /*
     * unique id associated with the vertex
     */
    private int vertexId;

    /*
     * store the integer id associated with the scaffold/fragment/capping
     */
    private int buildingBlockId;

    /*
     * attachment points for this Mol
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAP;

    /*
     * 0:scaffold, 1:fragment, 2:capping group
     */
    private int buildingBlockType;

    /*
     * While growing the graph, we associate a level with each vertex where the
     * scaffold has a level -1, while each layer adds 1
     */
    int recursiveLevel;

    /*
     * list of APs that behave in a similar manner when fragments are attached,
     * i.e., mirror the operation performed on symmetric set of APs.
     */
    protected ArrayList<SymmetricSet> lstSymmAP;

    /*
     * Flag indicating this as a ring closing vertex
     */
    private boolean isRCV;


//------------------------------------------------------------------------------

    public DENOPTIMVertex()
    {
        buildingBlockId = 0;
        lstAP = new ArrayList<>();
        vertexId = 0;
        buildingBlockType = 0;
        lstSymmAP = new ArrayList<>();
        isRCV = false;
    }
    
//------------------------------------------------------------------------------

    /**
     * 
     * @param vertexId unique identified of the vertex
     * @param bbId 0-based index of building block in the library
     * @param bbType choose the type of building block 0:scaffold, 1:fragment, 2:capping group
     */
    public DENOPTIMVertex(int vertexId, int bbId, int bbType)
    {
        this.vertexId = vertexId;
        this.buildingBlockId = bbId;
        this.buildingBlockType = bbType;
        IGraphBuildingBlock bb = null;
        try
        {
            bb = FragmentSpace.getFragment(bbType,bbId).clone();
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
            String msg = "Fatal error! Cannot continue. " + e.getMessage();
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            System.exit(0);
        }
        this.lstAP = new ArrayList<>();
        for (DENOPTIMAttachmentPoint ap : bb.getAPs())
        {
            this.lstAP.add(ap.clone());
        }
        this.lstSymmAP = new ArrayList<>();
        for (SymmetricSet ss : bb.getSymmetricAPsSets())
        {
            this.lstSymmAP.add(ss.clone());
        }
        isRCV = lstAP.size() == 1 && DENOPTIMConstants.RCAAPCLASSSET.contains(
                lstAP.get(0).getAPClass());
    }    

//------------------------------------------------------------------------------

    @Deprecated
    public DENOPTIMVertex(int m_vid, int m_molId,
            ArrayList<DENOPTIMAttachmentPoint> m_lstAP, int m_fragmentType)
    {
        buildingBlockId = m_molId;
        lstAP = m_lstAP;
        vertexId = m_vid;
        buildingBlockType = m_fragmentType;
        lstSymmAP = new ArrayList<>();
        isRCV = lstAP.size() == 1 && DENOPTIMConstants.RCAAPCLASSSET.contains(
                lstAP.get(0).getAPClass());
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex is a fragment
     */

    public int getFragmentType()
    {
        return buildingBlockType;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return the id of the molecule
     */
    public int getMolId()
    {
        return buildingBlockId;
    }

//------------------------------------------------------------------------------

    public void setMolId(int m_molId)
    {
        buildingBlockId = m_molId;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAP;
    }

//------------------------------------------------------------------------------

    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> m_lstAP)
    {
        lstAP = m_lstAP;
    }

//------------------------------------------------------------------------------

    public void setVertexId(int m_vid)
    {
        vertexId = m_vid;
    }

//------------------------------------------------------------------------------

    public int getVertexId()
    {
        return vertexId;
    }

//------------------------------------------------------------------------------

    public void setSymmetricAP(ArrayList<SymmetricSet> m_Sap)
    {
        lstSymmAP = m_Sap;
    }

//------------------------------------------------------------------------------

    public ArrayList<SymmetricSet> getSymmetricAP()
    {
        return lstSymmAP;
    }

//------------------------------------------------------------------------------

    /**
     * For the given attachment point index locate the symmetric partners
     * i.e. those with similar environments and class types.
     * @param m_dapidx inded of the attachment point which we want to get
     * the symmetrically related partners of.
     * @return the list of attachment point IDs, which include 
     * <code>m_dapidx</code> or <code>null</code> if no partners present
     */

    public SymmetricSet getSymmetricAPs(int m_dapidx)
    {
        for (SymmetricSet symmetricSet : lstSymmAP) {
            if (symmetricSet.contains(m_dapidx)) {
                return symmetricSet;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    public int getNumberOfAP()
    {
        return lstAP.size();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return list of attachment points that have free valences
     */

    public ArrayList<Integer> getFreeAPList()
    {
        ArrayList<Integer> lstAvailableAP = new ArrayList<>();
        for (int i=0; i<lstAP.size(); i++)
        {
            if (lstAP.get(i).isAvailable())
                lstAvailableAP.add(i);
        }
        return lstAvailableAP;
    }

//------------------------------------------------------------------------------

    public int getFreeAPCount()
    {
        int n = 0;
        for (DENOPTIMAttachmentPoint denoptimAttachmentPoint : lstAP) {
            if (denoptimAttachmentPoint.isAvailable())
                n++;
        }
        return n;
    }


//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (DENOPTIMAttachmentPoint denoptimAttachmentPoint : lstAP) {
            if (denoptimAttachmentPoint.isAvailable())
                return true;
        }
        return false;
    }

//------------------------------------------------------------------------------

    public void updateAttachmentPoint(int idx, int delta)
    {
        lstAP.get(idx).updateFreeConnections(delta);
    }

//------------------------------------------------------------------------------

    public void setLevel(int m_level)
    {
        recursiveLevel = m_level;
    }

//------------------------------------------------------------------------------

    public int getLevel()
    {
        return recursiveLevel;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex is a ring closing vertex
     */

    public boolean isRCV()
    {
        return isRCV;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex has symmetric APs
     */

    public boolean hasSymmetricAP()
    {
        return !lstSymmAP.isEmpty();
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return vertexId + "_" + (buildingBlockId + 1) + "_" +
                buildingBlockType + "_" + recursiveLevel;
    }

//------------------------------------------------------------------------------
    
    public void cleanup()
    {
        if (lstSymmAP != null)
        {
            lstSymmAP.clear();
        }
        if (lstAP != null)
        {
            lstAP.clear();
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a deep-copy of this vertex
     * @return a deep-copy
     */
    public DENOPTIMVertex clone()
    {
        return new DENOPTIMVertex(vertexId, buildingBlockId,
                buildingBlockType);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another vertex ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertexes represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMVertex other, StringBuilder reason)
    {
    	if (this.getFragmentType() != other.getFragmentType())
    	{
    		reason.append("Different fragment type ("+this.getFragmentType()+":"
					+other.getFragmentType()+"); ");
    		return false;
    	}
    	
    	if (this.getMolId() != other.getMolId())
    	{
    		reason.append("Different molID ("+this.getMolId()+":"
					+other.getMolId()+"); ");
    		return false;
    	}
    	
    	if (this.getFreeAPCount() != other.getFreeAPCount())
    	{
    		reason.append("Different number of free APs ("
    				+this.getFreeAPCount()+":"
					+other.getFreeAPCount()+"); ");
    		return false;
    	}
    	
    	if (this.lstAP.size() != other.lstAP.size())
    	{
    		reason.append("Different number of APs ("
    				+this.lstAP.size()+":"
					+other.lstAP.size()+"); ");
    		return false;
    	}
    	

    	for (DENOPTIMAttachmentPoint apT : this.lstAP)
    	{
    		boolean found = false;
    		for (DENOPTIMAttachmentPoint apO : other.lstAP)
        	{
		    	if (apT.equals(apO))
		    	{
		    		found = true;
		    		break;
		    	}
        	}
    		if (!found)
    		{
    			reason.append("No corresponding AP for "+apT);
    			return false;
    		}
    	}
    	
    	return true;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param cmpReac list of reactions of the source vertex attachment point
     * @return list of indices of the attachment points in vertex that has
     * the corresponding reaction
     */

    public ArrayList<Integer> getCompatibleClassAPIndex(
            String cmpReac
    ) {
        ArrayList<DENOPTIMAttachmentPoint> apLst = getAttachmentPoints();
        ArrayList<Integer> apIdx = new ArrayList<>();
        for (int i = 0; i < apLst.size(); i++)
        {
            DENOPTIMAttachmentPoint dap = apLst.get(i);
            if (dap.isAvailable())
            {
                // check if this AP has the compatible reactions
                String dapReac = dap.getAPClass();
                if (dapReac.compareToIgnoreCase(cmpReac) == 0)
                {
                    apIdx.add(i);
                }
            }
        }
        return apIdx;
    }
}
