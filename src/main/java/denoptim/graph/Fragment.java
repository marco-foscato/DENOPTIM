package denoptim.graph;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.jgrapht.graph.DefaultUndirectedGraph;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import com.google.gson.Gson;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.Edge.BondType;
import denoptim.json.DENOPTIMgson;
import denoptim.utils.MathUtils;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.MutationType;
import denoptim.utils.Randomizer;

/**
 * Class representing a continuously connected portion of chemical object
 * holding attachment points.
 *
 * @author Marco Foscato
 */

public class Fragment extends Vertex
{
    /**
     * attachment points on this vertex
     */
    private List<AttachmentPoint> lstAPs;

    /**
     * List of AP sets that are related to each other, so that we
     * call them "symmetric" (though symmetry is a fuzzy concept here).
     */
    private List<SymmetricAPs> lstSymAPs;
    
	/**
	 * Molecular representation of this fragment
	 */
	private IAtomContainer mol;
	
	/**
	 * jGraph representation used for detecting fragment isomorphism.
	 */
	private DefaultUndirectedGraph<FragIsomorphNode,FragIsomorphEdge> 
	    jGraphFragIsomorphism;

	
//-----------------------------------------------------------------------------

    /**
     * Constructor of an empty fragment
     */
    
    public Fragment()
    {
        super(VertexType.MolecularFragment);
        this.lstAPs = new ArrayList<AttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricAPs>();
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        this.mol = builder.newAtomContainer();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a molecular fragment kind of vertex.
     * @param vertexId unique identified of the vertex
     */

    public Fragment(long vertexId)
    {
        super(VertexType.MolecularFragment, vertexId);
        this.lstAPs = new ArrayList<AttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricAPs>();
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        this.mol = builder.newAtomContainer();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from an atom container, which has APs only as 
     * molecular properties. Property 
     * {@link DENOPTIMConstants#ISOMORPHICFAMILYID} is copied into the fragment,
     * if present.
     * WARNING: other properties of the atom container
     * are not imported!
     * @param l the identifier of the vertex to construct
     * @param mol the molecular representation
     * @throws DENOPTIMException 
     */
    
    public Fragment(long l, IAtomContainer mol, BBType bbt)
            throws DENOPTIMException
    {     
        super (VertexType.MolecularFragment, l);
        
        this.setBuildingBlockType(bbt);
        
        this.lstAPs = new ArrayList<AttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricAPs>();
        
        this.mol = MoleculeUtils.makeSameAs(mol);
        MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(this.mol);

        Object prop = mol.getProperty(DENOPTIMConstants.APSTAG);
        if (prop != null)
        {
            projectPropertyToAP(prop.toString());
        }
        
        List<SymmetricAPs> simAP = identifySymmetryRelatedAPSets(this.mol, 
                getAttachmentPoints());
        setSymmetricAPSets(simAP);
        
        this.setAsRCV(getNumberOfAPs() == 1
                && APClass.RCAAPCLASSSET.contains(
                        getAttachmentPoints().get(0).getAPClass()));
        
        Object isomorph = mol.getProperty(DENOPTIMConstants.ISOMORPHICFAMILYID);
        if (isomorph != null)
        {
            this.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID, 
                    isomorph.toString());
        }
    }

//------------------------------------------------------------------------------

    public Fragment(long vertexId, IAtomContainer mol, BBType bbt, boolean isRCV)
            throws DENOPTIMException {
        this(vertexId, mol, bbt);
        this.setAsRCV(isRCV);
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from another atom container, which has APs only as 
     * molecular properties. WARNING: properties of the atom container are not
     * imported!
     * @param mol the molecular representation
     * @throws DENOPTIMException 
     */
    
    public Fragment(IAtomContainer mol, BBType bbt)
            throws DENOPTIMException
    {    	
    	this(-1,mol,bbt);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if atoms and APs contained in this fragment have non-zero 3D 
     * coordinates.
     */
    public boolean is3D()
    {
        double maxDist = 0.0;
        Point3d origin = new Point3d(0,0,0);
        for (IAtom atm : atoms())
        {
            Point3d p = atm.getPoint3d();
            if (p==null)
                return false;
            double dist = MathUtils.distance(origin, p);
            if (dist > maxDist)
            {
                maxDist = dist;
            }
        }
        for (AttachmentPoint ap : getAttachmentPoints())
        {
            Point3d p = ap.getDirectionVector();
            if (p==null)
                return false;
            double dist = MathUtils.distance(origin, p);
            if (dist > maxDist)
            {
                maxDist = dist;
            }
        }
        
        return maxDist > 0.001;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Depends on an healthy list of attachment points with properly set pointers
     * to the source atoms.
     */
    private void updateSymmetryRelations()
    {
        setSymmetricAPSets(identifySymmetryRelatedAPSets(mol, 
                getAttachmentPoints()));
    }
    
//------------------------------------------------------------------------------
    
    private static List<SymmetricAPs> identifySymmetryRelatedAPSets(
            IAtomContainer mol, List<AttachmentPoint> daps)
    {
        List<SymmetricAPs> lstCompatible = new ArrayList<>();
        for (int i=0; i<daps.size()-1; i++)
        {
            AttachmentPoint apI = daps.get(i);
            SymmetricAPs ss = new SymmetricAPs();
            ss.add(apI);
            
            boolean alreadyFound = false;
            for (SymmetricAPs previousSS : lstCompatible)
            {
                if (previousSS.contains(apI))
                {
                    alreadyFound = true;
                    break;
                }
            }
            if (alreadyFound)
            {
                continue;
            }
        
            for (int j=i+1; j<daps.size(); j++)
            {
                AttachmentPoint apJ = daps.get(j);
                if (atomsAreCompatible(mol, apI.getAtomPositionNumber(),
                                    apJ.getAtomPositionNumber()))
                {
                    // check if reactions are compatible w.r.t. symmetry
                    if (apI.getAPClass()!=null && apJ.getAPClass()!=null 
                            && apI.getAPClass().compareTo(apJ.getAPClass()) == 0)
                    {
                        ss.add(apJ);
                    }
                }
            }
            
            if (ss.size() > 1)
            {
                lstCompatible.add(ss);
            }
        }
        
        return lstCompatible;
    }
 
//------------------------------------------------------------------------------

    /**
     * Checks if the atoms at the given positions have similar environments
     * i.e. are similar in atom types etc.
     * @param mol
     * @param a1 atom position
     * @param a2 atom position
     * @return <code>true</code> if atoms have similar environments
     */

    private static boolean atomsAreCompatible(IAtomContainer mol, int a1, int a2)
    {
        // check atom types
        IAtom atm1 = mol.getAtom(a1);
        IAtom atm2 = mol.getAtom(a2);

        if (atm1.getSymbol().compareTo(atm2.getSymbol()) != 0)
            return false;

        // check connected bonds
        if (mol.getConnectedBondsCount(atm1)!=mol.getConnectedBondsCount(atm2))
            return false;

        // check connected atoms
        if (mol.getConnectedBondsCount(atm1)!=mol.getConnectedBondsCount(atm2))
            return false;

        List<IAtom> la1 = mol.getConnectedAtomsList(atm2);
        List<IAtom> la2 = mol.getConnectedAtomsList(atm2);

        int k = 0;
        for (int i=0; i<la1.size(); i++)
        {
            IAtom b1 = la1.get(i);
            for (int j=0; j<la2.size(); j++)
            {
                IAtom b2 = la2.get(j);
                if (b1.getSymbol().compareTo(b2.getSymbol()) == 0)
                {
                    k++;
                    break;
                }
            }
        }

        return k == la1.size();
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with a dummy APClass.
     * @param atomPositionNumber the index of the source atom (0-based)
     */
    
    public void addAP(int atomPositionNumber) {
        APClass apc = null;
        addAP(atomPositionNumber, null, apc);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param apClass the APClass
     */
    public void addAP(int atomPositionNumber, APClass apClass) {
        addAP(atomPositionNumber, null, apClass);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param dirVec the AP direction vector end (the beginning at the 
     * coordinates of the source atom).
     * @param apClass the APClass
     * @return the reference to the created AP.
     */
    public AttachmentPoint addAP(int atomPositionNumber, Point3d dirVec, 
            APClass apClass) 
    {
        AttachmentPoint ap = new AttachmentPoint(this,
                atomPositionNumber, dirVec, apClass);
        getAttachmentPoints().add(ap);
        
        IAtom srcAtm = mol.getAtom(atomPositionNumber);
        
        ArrayList<AttachmentPoint> apList = new ArrayList<>();
        if (getAPCountOnAtom(srcAtm) > 0) {
            apList = getAPsFromAtom(srcAtm);
        }
        apList.add(ap);
        srcAtm.setProperty(DENOPTIMConstants.ATMPROPAPS, apList);
        
        updateSymmetryRelations();
        
        return ap;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtmId the index of the source atom in the atom list of this 
     * chemical representation. Index must be 0-based.
     * @param apc the attachment point class, or null, if class should not 
     * be defines.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @param valence the valences used by this AP.
     * @throws DENOPTIMException 
     */

    public void addAP(int srcAtmId, APClass apc, Point3d vector) 
            throws DENOPTIMException
    {
        IAtom srcAtm = mol.getAtom(srcAtmId);
        addAPOnAtom(srcAtm, apc, vector);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtm the source atom in the atom list of this 
     * chemical representation.
     * @param apc the attachment point class, or null, if class should not 
     * be defines.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @return the reference to the created AP.
     * @throws DENOPTIMException 
     */

    public AttachmentPoint addAPOnAtom(IAtom srcAtm, APClass apc, 
            Point3d vector) throws DENOPTIMException
    {
        int atmId = mol.indexOf(srcAtm);
        return this.addAP(atmId, new Point3d(vector.x, vector.y, vector.z), apc);
    }
    
//-----------------------------------------------------------------------------
    
    public void removeAP(AttachmentPoint ap)
    {
        if (!getAttachmentPoints().contains(ap))
            return;
        
        IAtom srcAtm = mol.getAtom(ap.getAtomPositionNumber());
        
        ArrayList<AttachmentPoint> apList = new ArrayList<>();
        if (getAPCountOnAtom(srcAtm) > 0) {
            apList = getAPsFromAtom(srcAtm);
        }
        apList.remove(ap);

        getAttachmentPoints().remove(ap);
        srcAtm.setProperty(DENOPTIMConstants.ATMPROPAPS, apList);
        
        updateSymmetryRelations();
    }

//-----------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
    public ArrayList<AttachmentPoint> getAPsFromAtom(IAtom srcAtm)
    {
        ArrayList<AttachmentPoint> apsOnAtm = new ArrayList<AttachmentPoint>();
        Object prop = srcAtm.getProperty(DENOPTIMConstants.ATMPROPAPS);
        if (prop!=null)
        {
            apsOnAtm = (ArrayList<AttachmentPoint>) prop;
        }
        return apsOnAtm;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Returns the number of APs currently defined on a specific atom source.
     * @param srcAtmId the index of the atom
     * @return the number of APs
     * @throws DENOPTIMException 
     */
    
    public int getAPCountOnAtom(int srcAtmId)
    {
        IAtom srcAtm = mol.getAtom(srcAtmId);
        return getAPCountOnAtom(srcAtm);
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the number of APs currently defined on a specific atom source.
     * @param srcAtm the source atom
     * @return the number of APs
     * @throws DENOPTIMException 
     */
    
    public int getAPCountOnAtom(IAtom srcAtm)
    {
    	int num = 0;
    	if (srcAtm.getProperty(DENOPTIMConstants.ATMPROPAPS) != null)
    	{
			ArrayList<AttachmentPoint> apsOnAtm = 
					getAPsFromAtom(srcAtm);
        	num = apsOnAtm.size();
    	}
    	return num;
    }
	
//-----------------------------------------------------------------------------
    
    /**
     * Changes the properties of each APs as to reflect the current atom list.
     * AttachmentPoint include the index of their source atom, and this
     * method updates such indexes to reflect the current atom list.
     * This method is needed upon reordering of the atom list.
     */
    
    public void updateAPs()
    {
        for (int atmId = 0; atmId<mol.getAtomCount(); atmId++)
        {
            IAtom srcAtm = mol.getAtom(atmId);
            if (srcAtm.getProperty(DENOPTIMConstants.ATMPROPAPS) != null)
            {
            	List<AttachmentPoint> apsOnAtm = getAPsFromAtom(srcAtm);
	            for (int i = 0; i < apsOnAtm.size(); i++)
	            {
	                AttachmentPoint ap = apsOnAtm.get(i);
	                ap.setAtomPositionNumber(atmId);
	            }
            }
        }
        updateSymmetryRelations();
    }

//-----------------------------------------------------------------------------
    
    /**
     * Collects APs currently defined as properties of the atoms.
     * Converts the internal notation defining APs (i.e., APs are stored in
     * as atom-specific properties) to the standard DENOPTIM formalism (i.e.,
     * APs are collected in a molecular property).
     * WARNING: cannot be used after altering the atom list!
     * @return the list of APs. Note that these APs cannot respond to changes
     * in the atom list!
     */
    
    public List<AttachmentPoint> getCurrentAPs()
    {
    	updateAPs();
    	
    	lstAPs.clear();
    	
        for (IAtom srcAtm : mol.atoms())
        {
        	if (srcAtm.getProperty(DENOPTIMConstants.ATMPROPAPS) != null)
            {
        		ArrayList<AttachmentPoint> apsOnAtm = 
        				getAPsFromAtom(srcAtm);
        		lstAPs.addAll(apsOnAtm);
            }
        }

        //Reorder according to DENOPTIMAttachmentPoint priority
        lstAPs.sort(new AttachmentPointComparator());
        
        return lstAPs;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Uses the molecular property defining attachment points to create the 
     * DENOPTIMAttachmentPoint objects in the Atom objects. Does not
     * overwrite existing APs in the atoms.
     * @throws DENOPTIMException
     */
    
    public void projectPropertyToAP() throws DENOPTIMException
    {
	    String allAtomsProp = "";    
	    if (getProperty(DENOPTIMConstants.APSTAG) == null)
	    {
	    	return;
        }
	    allAtomsProp = getProperty(DENOPTIMConstants.APSTAG).toString();
	    projectPropertyToAP(allAtomsProp);
    }
	    
//-----------------------------------------------------------------------------
    
    /**
     * Uses a string formatted like the molecular property used for 
     * defining attachment points to create the 
     * DENOPTIMAttachmentPoint objects in the Atom objects. Does not
     * overwrite existing APs in the atoms.
     * @param allAtomsProp the content of the molecular property.
     * @throws DENOPTIMException
     */
    
    public void projectPropertyToAP(String allAtomsProp) throws DENOPTIMException
    {
        lstAPs.clear();
    	if (allAtomsProp.trim().equals(""))
    	{
    		return;
    	}
    	
    	// Cleanup current APs in atom objects
    	for (int ii=0 ; ii<mol.getAtomCount(); ii++)
    	{
    		IAtom atm = mol.getAtom(ii);   		
    		atm.removeProperty(DENOPTIMConstants.ATMPROPAPS);
    	}
	   
	    // Collect all the APs as objects
	    String[] atomsProp = allAtomsProp.split(
	    		DENOPTIMConstants.SEPARATORAPPROPATMS);
	    for (int i = 0; i< atomsProp.length; i++)
	    {
			String onThisAtm = atomsProp[i];
			if (onThisAtm.contains(DENOPTIMConstants.SEPARATORAPPROPAPS))
			{
			    String[] moreAPonThisAtm = onThisAtm.split(
			    		DENOPTIMConstants.SEPARATORAPPROPAPS);
			    
			    AttachmentPoint ap = 
			    		new AttachmentPoint(this, moreAPonThisAtm[0]);
			    
			    int atmID = ap.getAtomPositionNumber();
			    //WARNING the atmID is already 0-based
			    lstAPs.add(ap);
			    for (int j = 1; j<moreAPonThisAtm.length; j++ )
			    {
			    	//WARNING here we have to switch to 1-based enumeration
			    	// because we import from SDF string
					AttachmentPoint apMany = 
							new AttachmentPoint(this, atmID+1
									+ DENOPTIMConstants.SEPARATORAPPROPAAP
									+ moreAPonThisAtm[j]);
					lstAPs.add(apMany);
			    }
			} 
			else 
			{
			    AttachmentPoint ap = 
			    		new AttachmentPoint(this, onThisAtm);
			    lstAPs.add(ap);
			}
	    }

	    projectListAPToAtomProperties();

        updateSymmetryRelations();
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Takes the list of APs from the field of this class and projects the APs
     * into properties of the {@link IAtom}s. This method does not update
     * the symmetry relation between APs because it might have been read
     * from file, which takes priority over recalculating it.
     * @throws DENOPTIMException
     */
    public void projectListAPToAtomProperties()
    {
        // Write attachment points in the atoms
        for (int i = 0; i < lstAPs.size(); i++)
        {
            AttachmentPoint ap = lstAPs.get(i);
            int atmID = ap.getAtomPositionNumber();
            
            IAtom atm = mol.getAtom(atmID);
            if (atm.getProperty(DENOPTIMConstants.ATMPROPAPS) != null)
            {
                ArrayList<AttachmentPoint> oldAPs = 
                        getAPsFromAtom(atm);
                oldAPs.add(ap);
                atm.setProperty(DENOPTIMConstants.ATMPROPAPS,oldAPs);
            } 
            else
            {
                ArrayList<AttachmentPoint> aps = 
                        new ArrayList<AttachmentPoint>();
                aps.add(ap);
                
                atm.setProperty(DENOPTIMConstants.ATMPROPAPS,aps);
            }
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Finds the DENOPTIMAttachmentPoint objects defined as properties of
     * the atoms in this container, and defines the string-based molecular
     * property used to print attachment points in SDF files.
     */
    public void projectAPsToProperties()
    {
        
        // Prepare the string-representation of unused APs on this graph
        LinkedHashMap<Integer,List<AttachmentPoint>> apsPerAtom =
                new LinkedHashMap<>();
        for (IAtom atm : mol.atoms())
        {   
            if (atm.getProperty(DENOPTIMConstants.ATMPROPAPS) == null)
            {
                continue;
            }
            int atmID = mol.indexOf(atm);
            ArrayList<AttachmentPoint> apsOnAtm = 
                    getAPsFromAtom(atm);
            for (AttachmentPoint ap : apsOnAtm)
            {
                if (apsPerAtom.containsKey(atmID))
                {
                    apsPerAtom.get(atmID).add(ap);
                } else {
                    List<AttachmentPoint> lst = 
                            new ArrayList<AttachmentPoint>();
                    lst.add(ap);
                    apsPerAtom.put(atmID,lst);
                }
            }
        }
        //WARNING! In the mol.property we use 1-to-n+1 instead of 0-to-n
        setProperty(DENOPTIMConstants.APSTAG, 
                AttachmentPoint.getAPDefinitionsForSDF(apsPerAtom));
    }

//-----------------------------------------------------------------------------

    /**
     * Returns a deep copy of this fragments.
     */
    
    @Override
    public Fragment clone()
    {   
    	Fragment clone = new Fragment();
    	clone.setVertexId(this.getVertexId());
    	try
        {
            clone.mol = MoleculeUtils.makeSameAs(mol);
        } catch (DENOPTIMException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    	
    	// WARNING: here we are not recovering all info from APs: we take only
    	// some info. This can be improved...
        for (AttachmentPoint ap : lstAPs)
        {
            AttachmentPoint cAp = new AttachmentPoint(clone,
                    ap.getAtomPositionNumber(),
                    ap.getDirectionVector(),
                    ap.getAPClass());
            cAp.setCutId(ap.getCutId());
            cAp.setID(ap.getID());
            clone.lstAPs.add(cAp);
        }
        
        clone.projectListAPToAtomProperties();
        
		clone.setBuildingBlockId(this.getBuildingBlockId());
		clone.setBuildingBlockType(this.getBuildingBlockType());

        clone.setMutationTypes(this.getUnfilteredMutationTypes());
        
        List<SymmetricAPs> cLstSymAPs = new ArrayList<SymmetricAPs>();
        for (SymmetricAPs symAPs : this.getSymmetricAPSets())
        {
            SymmetricAPs cSymAPs = new SymmetricAPs();
            for (AttachmentPoint ap : symAPs)
            {
                cSymAPs.add(clone.getAP(ap.getIndexInOwner()));
            }
            cLstSymAPs.add(cSymAPs);
        }
        clone.setSymmetricAPSets(cLstSymAPs);
        
        clone.setAsRCV(this.isRCV());
        clone.setProperties(this.copyStringBasedProperties());
        if (uniquefyingPropertyKeys!=null)
            clone.uniquefyingPropertyKeys.addAll(uniquefyingPropertyKeys);
		return clone;
    }

//-----------------------------------------------------------------------------

    @Override
    public IAtomContainer getIAtomContainer()
    {
        this.projectAPsToProperties();
        for (int atmPos=0; atmPos<mol.getAtomCount(); atmPos++)
        {
            IAtom atm = mol.getAtom(atmPos);
            atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID, getVertexId());
            atm.setProperty(DENOPTIMConstants.ATMPROPORIGINALATMID, atmPos);
        }
        mol.setProperty(DENOPTIMConstants.APSTAG, 
                getProperty(DENOPTIMConstants.APSTAG));
        mol.setProperty(DENOPTIMConstants.VERTEXJSONTAG,this.toJson());
        mol.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID,
                getProperty(DENOPTIMConstants.ISOMORPHICFAMILYID));
        return mol;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Although this type of vertex contains atoms, its content is fixed. 
     * So, there is no need to regenerate its chemical representation. 
     * Accordingly, this method is ignoring all the parameters and calling
     * {@link #getIAtomContainer()}
     */
    @Override
    public IAtomContainer getIAtomContainer(Logger logger, 
            Randomizer rng, boolean removeUsedRCAs, boolean rebuild)
    {
        return getIAtomContainer();
    }
    
//-----------------------------------------------------------------------------

    public Iterable<IAtom> atoms()
    {
        return mol.atoms();
    }
    
//-----------------------------------------------------------------------------

    public Iterable<IBond> bonds()
    {
        return mol.bonds();
    }

//-----------------------------------------------------------------------------

    public void addAtom(IAtom atom)
    {
        mol.addAtom(atom);
    }   
    
//-----------------------------------------------------------------------------

    public IAtom getAtom(int number)
    {
        return mol.getAtom(number);
    }

//-----------------------------------------------------------------------------

    public int indexOf(IAtom atom)
    {
        return mol.indexOf(atom);
    }
    
//-----------------------------------------------------------------------------

    public int getAtomCount()
    {
        return mol.getAtomCount();
    }
    
//-----------------------------------------------------------------------------

    public int getBondCount()
    {
        return mol.getBondCount();
    }
    
//-----------------------------------------------------------------------------

    public void addBond(IBond bond)
    {
        mol.addBond(bond);
    }
    
//-----------------------------------------------------------------------------
    
    public IBond removeBond(int position)
    {
        return mol.removeBond(position);
    }

//-----------------------------------------------------------------------------
   
    public IBond removeBond(IAtom atom1, IAtom atom2)
    {
       return mol.removeBond(atom1, atom2);
    }
    
//-----------------------------------------------------------------------------
    
    public void removeBond(IBond bond)
    {
        mol.removeBond(bond);
    }
    
//-----------------------------------------------------------------------------
    
    public void removeAtom(IAtom atom)
    {
        removeAtoms(Arrays.asList(atom));
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Removes a list of atoms and updates the list of attachment points.
     * Use this method instead of {@link #removeAtom(IAtom)}
     * to run the regeneration of the list of APs only once instead of for 
     * every atom deletion.
     * @param atoms the atoms to remove.
     */
    public void removeAtoms(Collection<IAtom> atoms)
    {
        for (IAtom atom : atoms)
            mol.removeAtom(atom);
        
        lstAPs.clear();
        
        for (int atmId = 0; atmId<mol.getAtomCount(); atmId++)
        {
            IAtom srcAtm = mol.getAtom(atmId);
            if (srcAtm.getProperty(DENOPTIMConstants.ATMPROPAPS) != null)
            {
                ArrayList<AttachmentPoint> apsOnAtm = getAPsFromAtom(srcAtm);
                for (int i = 0; i < apsOnAtm.size(); i++)
                {
                    AttachmentPoint ap = apsOnAtm.get(i);
                    ap.setAtomPositionNumber(atmId);
                }
                lstAPs.addAll(apsOnAtm);
            }
        }

        //Reorder according to DENOPTIMAttachmentPoint priority
        lstAPs.sort(new AttachmentPointComparator());
        
        updateSymmetryRelations();
    }
    
//-----------------------------------------------------------------------------
    
    public List<IAtom> getConnectedAtomsList(IAtom atom)
    {
        return mol.getConnectedAtomsList(atom);
    }
    
//-----------------------------------------------------------------------------
    
    public int getConnectedAtomsCount(IAtom atom)
    {
        return mol.getConnectedBondsCount(atom);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another fragment ignoring vertex IDs. Ignores isomerism
     * and geometric differences as it only considers the number of atoms and 
     * bonds.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertices represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(Fragment other, StringBuilder reason)
    {
        if (this.containsAtoms() && other.containsAtoms())
        {
            if (this.mol.getAtomCount() != other.mol.getAtomCount())
            {
                reason.append("Different atom count (" + this.mol.getAtomCount()
                        + ":" + other.mol.getAtomCount() + "); ");
            }
            if (this.mol.getBondCount() != other.mol.getBondCount())
            {
                reason.append("Different bond count (" + this.mol.getBondCount()
                        + ":" + other.mol.getBondCount() + "); ");
            }
        }
        return sameVertexFeatures(other, reason);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Creates a graph representation of this fragment where both atoms and 
     * {@link AttachmentPoint}s are represented as nodes, and edges are either
     * bonds or atom-to-attachment point connections. Note that dummy atoms
     * meant to heal linearities  ({@value DENOPTIMConstants#DUMMYATMSYMBOL} 
     * with only one connected neighbor) are ignored.
     * @return the graph.
     */
    public DefaultUndirectedGraph<FragIsomorphNode,FragIsomorphEdge> 
        getJGraphFragIsomorphism()
    {
        if (jGraphFragIsomorphism != null)
        {
            return jGraphFragIsomorphism;
        }
        jGraphFragIsomorphism = new DefaultUndirectedGraph<>(
                FragIsomorphEdge.class);
        
        Map<IAtom,FragIsomorphNode> atmToNode = 
                new HashMap<IAtom,FragIsomorphNode>();
        Set<IAtom> ignoredDu = new HashSet<IAtom>();
        for (IAtom atm : mol.atoms())
        {
            if (DENOPTIMConstants.DUMMYATMSYMBOL.equals(
                    MoleculeUtils.getSymbolOrLabel(atm))
                    && mol.getConnectedBondsCount(atm)<2)
            {
                //Ignore Du on linearities
                ignoredDu.add(atm);
                continue;
            }
            FragIsomorphNode node = new FragIsomorphNode(atm);
            atmToNode.put(atm,node);
            jGraphFragIsomorphism.addVertex(node);
        }
        for (IBond bnd : mol.bonds())
        {
            if (bnd.getAtomCount() > 2)
            {
                throw new IllegalArgumentException("Cannot handle bonds that involve "
                        + "more than two atoms");
            }
            if (ignoredDu.contains(bnd.getAtom(0)) 
                    || ignoredDu.contains(bnd.getAtom(0)))
            {
                //Ignore Du on linearities
                continue;
            }

            FragIsomorphEdge edge = new FragIsomorphEdge(bnd);
            jGraphFragIsomorphism.addEdge(atmToNode.get(bnd.getAtom(0)), 
                    atmToNode.get(bnd.getAtom(1)), edge);
        }
        for (AttachmentPoint ap : getAttachmentPoints())
        {
            FragIsomorphNode node = new FragIsomorphNode(ap);
            jGraphFragIsomorphism.addVertex(node);
            FragIsomorphEdge edge = new FragIsomorphEdge();
            jGraphFragIsomorphism.addEdge(atmToNode.get(mol.getAtom(
                    ap.getAtomPositionNumber())), node, edge);
        }
        
        return jGraphFragIsomorphism;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks for isomorphism of the graph representation of this and another 
     * fragment. The graph representation results from the combination of the
     * molecular structure (atoms as nodes, bonds as edges of the graph) 
     * and the presence of attachment points (attachment points as nodes of the
     * graph). See {@link #getJGraphFragIsomorphism()} for details.
     * Does not consider three-dimensional features. Does not consider dummy
     * atoms on linearities so, de facto, fragments can be isomorphic and have a 
     * different number of atoms and bonds.
     * @param other the other building block to consider.
     * @return <code>true</code> if at least one isomorphism exists.
     */
    public boolean isIsomorphicTo(Vertex other)
    {
        if (!(other instanceof Fragment))
        {
            return false;
        }
        Fragment otherFrag = (Fragment) other;
          
        // NB: do not add a comparison of #atoms or #bonds because they may be
        // different as a result of a different number of dummy atoms placed
        // on linearities. Such atoms are not considered when checking for 
        // isomorphism so, de facto, fragments can be isomorphic and have a 
        // different number of atoms and bonds.
         
        if (this.getAttachmentPoints().size() 
                != otherFrag.getAttachmentPoints().size())
        {
            return false;
        }
  
        FragmentIsomorphismInspector fii = new FragmentIsomorphismInspector(
                this, otherFrag); 
        return fii.isomorphismExists();
    }
    
//------------------------------------------------------------------------------

    public int getHeavyAtomsCount()
    {
        return MoleculeUtils.getHeavyAtomCount(mol);
    }

//------------------------------------------------------------------------------

    public boolean containsAtoms()
    {
        if (mol.getAtomCount() > 0)
            return true;
        else
            return false;
    }
    
//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return getVertexId() + "_" + (getBuildingBlockId() + 1) + "_" +
                getBuildingBlockType().toOldInt();
    }
    
//------------------------------------------------------------------------------

    @Override
    public List<AttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }
    
//------------------------------------------------------------------------------

    @Override
    protected void setSymmetricAPSets(List<SymmetricAPs> sAPs)
    {
        this.lstSymAPs = sAPs;
    }
  
//-----------------------------------------------------------------------------

    @Override
    protected void addSymmetricAPSet(SymmetricAPs symAPs)
    {
        if (this.lstSymAPs==null)
        {
            this.lstSymAPs = new ArrayList<SymmetricAPs>();
        }
        this.lstSymAPs.add(symAPs);
    }
    
//------------------------------------------------------------------------------

    @Override
    public List<SymmetricAPs> getSymmetricAPSets()
    {
        return lstSymAPs;
    }
    
//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this vertex.
     * @param ignoredTypes a collection of mutation types to ignore. vertices
     * that allow only ignored types of mutation will
     * not be considered mutation sites.
     * @return the list of vertices that allow any non-ignored mutation type.
     */
    
    @Override
    public List<Vertex> getMutationSites(List<MutationType> ignoredTypes)
    {
        List<Vertex> lst = new ArrayList<Vertex>();
        switch (getBuildingBlockType())
        {
            case CAP:
                break;
                
            default:
                if (getMutationTypes(ignoredTypes).size()>0)
                    lst.add(this);
                break;
        }
        return lst;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Produces a string that represents this vertex and that adheres to the 
     * JSON format.
     * @return the JSON format as a single string
     */
    
    public String toJson()
    {    
        Gson gson = DENOPTIMgson.getWriter();
        String jsonOutput = gson.toJson(this);
        return jsonOutput;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads a JSON string and returns an instance of this class.
     * Fields that depend on the context of this vertex, such as the graph 
     * 'owner' of this vertex and the 'user' of each attachment point, 
     * are recovered, if needed, upon deserialization of the graph that 
     * contains this vertex.
     * @param json the string to parse.
     * @return a new instance of this class.
     */
    
    public static Fragment fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        Fragment returnedFrag = gson.fromJson(json, 
                Fragment.class);
        
        for (AttachmentPoint ap : returnedFrag.getAttachmentPoints())
        {
            ap.setOwner(returnedFrag);
        }
        
        returnedFrag.projectListAPToAtomProperties();

        return returnedFrag;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the atom where the given attachment point is rooted, i.e., the
     * atom that is involved in the bond resulting from using the attachment 
     * point to form an edge with a {@link BondType} that leads to bond 
     * creation.
     * @param ap the attachment point to find the source of.
     * @return the source atom of the attachment point, or null is the 
     * attachment point does not belong to this vertex.
     */
    public IAtom getAtomHoldingAP(AttachmentPoint ap)
    {
        if (ap.getOwner() != this)
            return null;
       
        return mol.getAtom(ap.getAtomPositionNumber());
    }
    
//------------------------------------------------------------------------------

}
