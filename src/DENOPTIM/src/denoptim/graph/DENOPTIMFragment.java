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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.json.DENOPTIMgson;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.MutationType;

/**
 * Class representing a continuously connected portion of chemical object
 * holding attachment points.
 *
 * @author Marco Foscato
 */

public class DENOPTIMFragment extends DENOPTIMVertex
{
    /**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;

    /**
     * attachment points on this vertex
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAPs;

    /**
     * List of AP sets that are related to each other, so that we
     * call them "symmetric" (though symmetry is a fuzzy concept here).
     */
    private ArrayList<SymmetricSet> lstSymAPs;
    
	/**
	 * Molecular representation of this fragment
	 */
	private IAtomContainer mol;

	
//-----------------------------------------------------------------------------

    /**
     * Constructor of an empty fragment
     */
    
    public DENOPTIMFragment()
    {
        super(VertexType.MolecularFragment);
        this.lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricSet>();
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        this.mol = builder.newAtomContainer();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a molecular fragment kind of vertex.
     * @param vertexId unique identified of the vertex
     */

    public DENOPTIMFragment(int vertexId)
    {
        super(VertexType.MolecularFragment, vertexId);
        this.lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricSet>();
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        this.mol = builder.newAtomContainer();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from another atom container, which has APs only as 
     * molecular properties. WARNING: other properties of the atom container
     * are not imported!
     * @param vertexId the identifier of the vertex to construct
     * @param mol the molecular representation
     * @throws DENOPTIMException 
     */
    
    public DENOPTIMFragment(int vertexId, IAtomContainer mol, BBType bbt)
            throws DENOPTIMException
    {     
        super (VertexType.MolecularFragment, vertexId);
        
        this.setBuildingBlockType(bbt);
        
        this.lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricSet>();
        
        this.mol = DENOPTIMMoleculeUtils.makeSameAs(mol);

        Object prop = mol.getProperty(DENOPTIMConstants.APSTAG);
        if (prop != null)
        {
            projectPropertyToAP(prop.toString());
        }
        
        ArrayList<SymmetricSet> simAP = identifySymmetryRelatedAPSets(this.mol, 
                getAttachmentPoints());
        setSymmetricAPSets(simAP);
        
        this.setAsRCV(getNumberOfAPs() == 1
                && APClass.RCAAPCLASSSET.contains(
                        getAttachmentPoints().get(0).getAPClass()));
    }

//------------------------------------------------------------------------------

    public DENOPTIMFragment(int vertexId, IAtomContainer mol, BBType bbt,
                            boolean isRCV)
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
    
    public DENOPTIMFragment(IAtomContainer mol, BBType bbt)
            throws DENOPTIMException
    {    	
    	this(-1,mol,bbt);
    }
    
//------------------------------------------------------------------------------
    
    private void updateSymmetryRelations()
    {
        setSymmetricAPSets(identifySymmetryRelatedAPSets(mol, 
                getAttachmentPoints()));
    }
    
//------------------------------------------------------------------------------
    
    private static ArrayList<SymmetricSet> identifySymmetryRelatedAPSets(
            IAtomContainer mol,
            ArrayList<DENOPTIMAttachmentPoint> daps)
    {
        ArrayList<SymmetricSet> lstCompatible = new ArrayList<>();
        for (int i=0; i<daps.size()-1; i++)
        {
            ArrayList<Integer> lst = new ArrayList<>();
            Integer i1 = i;
            lst.add(i1);
            
            boolean alreadyFound = false;
            for (SymmetricSet previousSS : lstCompatible)
            {
                if (previousSS.contains(i1))
                {
                    alreadyFound = true;
                    break;
                }
            }
            if (alreadyFound)
            {
                continue;
            }
        
            DENOPTIMAttachmentPoint d1 = daps.get(i);
            for (int j=i+1; j<daps.size(); j++)
            {
                DENOPTIMAttachmentPoint d2 = daps.get(j);
                if (atomsAreCompatible(mol, d1.getAtomPositionNumber(),
                                    d2.getAtomPositionNumber()))
                {
                    // check if reactions are compatible w.r.t. symmetry
                    if (d1.getAPClass().compareTo(d2.getAPClass()) == 0)
                    {
                        Integer i2 = j;
                        lst.add(i2);
                    }
                }
            }
            
            if (lst.size() > 1)
            {
                lstCompatible.add(new SymmetricSet(lst));
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
     * Adds an attachment point with a dummy APClass and dummy properties.
     * This is used only for testing purposes.
     */
    public void addAP() {
        addAP(0);
        //TODO-V3 unify
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with a dummy APClass.
     * @param atomPositionNumber the index of the source atom (0-based)
     */
    
    //TODO: since APs can be on any vertex, and vertexes are not required to
    // contain atoms, the information of which atom is an AP rooted should be
    // stored and managed by the implementation of vertex that do contain atoms.
    // The DENOPTIMFragment should thus be charged with keeping the reference to
    // the atom that holds the AP.
    
    public void addAP(int atomPositionNumber) {
        DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(this, 
                atomPositionNumber);
        getAttachmentPoints().add(ap);

        //TODO-V3 unify
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param apClass the APClass
     */
    public void addAP(int atomPositionNumber, APClass apClass) {
        addAP(atomPositionNumber, null, apClass);

        //TODO-V3 unify
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param dirVec the AP direction vector end (the beginning at the 
     * coordinates of the source atom). This must array have 3 entries.
     * @param apClass the APClass
     * @return the reference to the created AP.
     */
    public DENOPTIMAttachmentPoint addAP(int atomPositionNumber, Point3d dirVec, APClass apClass) {
        DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(this,
                atomPositionNumber, dirVec, apClass);
        getAttachmentPoints().add(ap);
        
        IAtom srcAtm = mol.getAtom(atomPositionNumber);
        
        ArrayList<DENOPTIMAttachmentPoint> apList = new ArrayList<>();
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
        //TODO-V3 unify
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

    public DENOPTIMAttachmentPoint addAPOnAtom(IAtom srcAtm, APClass apc, Point3d vector)
            throws DENOPTIMException
    {
        int atmId = mol.indexOf(srcAtm);
        return this.addAP(atmId, new Point3d(vector.x, vector.y, vector.z), apc);
    }

//-----------------------------------------------------------------------------
    
    ArrayList<DENOPTIMAttachmentPoint> getAPsFromAtom(IAtom srcAtm)
    {
        @SuppressWarnings("unchecked")
		ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
        		(ArrayList<DENOPTIMAttachmentPoint>) srcAtm.getProperty(
        				DENOPTIMConstants.ATMPROPAPS);
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
			ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
					getAPsFromAtom(srcAtm);
        	num = apsOnAtm.size();
    	}
    	return num;
    }
	
//-----------------------------------------------------------------------------
    
    /**
     * Changes the properties of each APs as to reflect the current atom list.
     * DENOPTIMAttachmentPoint include the index of their source atom, and this
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
            	ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
            			getAPsFromAtom(srcAtm);
	            for (int i = 0; i < apsOnAtm.size(); i++)
	            {
	                DENOPTIMAttachmentPoint ap = apsOnAtm.get(i);
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
     * @return the list of APs. Note that these APs cannot respond to changes
     * in the atom list!
     */
    
    public ArrayList<DENOPTIMAttachmentPoint> getCurrentAPs()
    {
    	updateAPs();
    	
    	lstAPs.clear();
    	
        for (IAtom srcAtm : mol.atoms())
        {
        	if (srcAtm.getProperty(DENOPTIMConstants.ATMPROPAPS) != null)
            {
        		ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
        				getAPsFromAtom(srcAtm);
        		lstAPs.addAll(apsOnAtm);
            }
        }

        //Reorder according to DENOPTIMAttachmentPoint priority
        lstAPs.sort(new DENOPTIMAttachmentPointComparator());
        
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
	    if (mol.getProperty(DENOPTIMConstants.APSTAG) == null)
	    {
	    	System.out.println("WARNING: no tag " 
	    			+ DENOPTIMConstants.APSTAG + "found in fragment."
	    			+ " No AP created.");
	    	return;
        }
	    allAtomsProp = mol.getProperty(DENOPTIMConstants.APSTAG).toString();
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
			    
			    DENOPTIMAttachmentPoint ap = 
			    		new DENOPTIMAttachmentPoint(this, moreAPonThisAtm[0]);
			    
			    int atmID = ap.getAtomPositionNumber();
			    //WARNING the atmID is already 0-based
			    lstAPs.add(ap);
			    for (int j = 1; j<moreAPonThisAtm.length; j++ )
			    {
			    	//WARNING here we have to switch to 1-based enumeration
			    	// because we import from SDF string
					DENOPTIMAttachmentPoint apMany = 
							new DENOPTIMAttachmentPoint(this, atmID+1
									+ DENOPTIMConstants.SEPARATORAPPROPAAP
									+ moreAPonThisAtm[j]);
					lstAPs.add(apMany);
			    }
			} 
			else 
			{
			    DENOPTIMAttachmentPoint ap = 
			    		new DENOPTIMAttachmentPoint(this, onThisAtm);
			    lstAPs.add(ap);
			}
	    }

	    projectListAPToAPProperties();

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
    public void projectListAPToAPProperties() throws DENOPTIMException
    {
        // Write attachment points in the atoms
        for (int i = 0; i < lstAPs.size(); i++)
        {
            DENOPTIMAttachmentPoint ap = lstAPs.get(i);
            int atmID = ap.getAtomPositionNumber();
            
            if (atmID > mol.getAtomCount())
            {
                throw new DENOPTIMException("Fragment property defines AP "
                        + "with out-of-borders atom index (" + atmID + ").");
            }
            
            IAtom atm = mol.getAtom(atmID);
            if (atm.getProperty(DENOPTIMConstants.ATMPROPAPS) != null)
            {
                ArrayList<DENOPTIMAttachmentPoint> oldAPs = 
                        getAPsFromAtom(atm);
                oldAPs.add(ap);
                atm.setProperty(DENOPTIMConstants.ATMPROPAPS,oldAPs);
            } 
            else
            {
                ArrayList<DENOPTIMAttachmentPoint> aps = 
                        new ArrayList<DENOPTIMAttachmentPoint>();
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
        LinkedHashMap<Integer,List<DENOPTIMAttachmentPoint>> apsPerAtom =
                new LinkedHashMap<>();
        for (IAtom atm : mol.atoms())
        {   
            if (atm.getProperty(DENOPTIMConstants.ATMPROPAPS) == null)
            {
                continue;
            }
            int atmID = mol.indexOf(atm);
            ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
                    getAPsFromAtom(atm);
            for (DENOPTIMAttachmentPoint ap : apsOnAtm)
            {
                if (apsPerAtom.containsKey(atmID))
                {
                    apsPerAtom.get(atmID).add(ap);
                } else {
                    List<DENOPTIMAttachmentPoint> lst = 
                            new ArrayList<DENOPTIMAttachmentPoint>();
                    lst.add(ap);
                    apsPerAtom.put(atmID,lst);
                }
            }
        }
        //WARNING! In the mol.property we use 1-to-n+1 instead of 0-to-n
        mol.setProperty(DENOPTIMConstants.APSTAG, 
                DENOPTIMAttachmentPoint.getAPDefinitionsForSDF(apsPerAtom));
    }

//-----------------------------------------------------------------------------

    /**
     * Returns a deep copy of this fragments.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    public DENOPTIMFragment clone()
    {   
    	DENOPTIMFragment clone = new DENOPTIMFragment();
		try {
		    this.projectAPsToProperties();
		    //deep copy of mol is created in the DENOPTIMFragment constructor
			clone = new DENOPTIMFragment(getVertexId(),mol,getBuildingBlockType());
		} catch (Exception e) {
		    e.printStackTrace();
			String msg = "Failed to clone DENOPTIMFragment! " +clone;
			System.err.println(msg);
		}
		clone.setBuildingBlockId(this.getBuildingBlockId());
		clone.setBuildingBlockType(this.getBuildingBlockType());

        clone.setMutationTypes(this.getUnfilteredMutationTypes());
		
		ArrayList<SymmetricSet> cLstSymAPs = new ArrayList<SymmetricSet>();
        for (SymmetricSet ss : this.getSymmetricAPSets())
        {
            cLstSymAPs.add(ss.clone());
        }
        clone.setSymmetricAPSets(cLstSymAPs);
        clone.setAsRCV(this.isRCV());
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
        return mol;
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

    public int getAtomNumber(IAtom atom)
    {
        return mol.getAtomNumber(atom);
    }

//-----------------------------------------------------------------------------

    public int getAtomCount()
    {
        return mol.getAtomCount();
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
    
    public void removeAtomAndConnectedElectronContainers(IAtom atom)
    {
        mol.removeAtomAndConnectedElectronContainers(atom);
    }
    
//-----------------------------------------------------------------------------
    
    public List<IAtom> getConnectedAtomsList(IAtom atom)
    {
        return mol.getConnectedAtomsList(atom);
    }
    
//-----------------------------------------------------------------------------
    
    public int getConnectedAtomsCount(IAtom atom)
    {
        return mol.getConnectedAtomsCount(atom);
    }
 
//-----------------------------------------------------------------------------

    // The Vertex class has properties and getter/setter for properties.
    // This looks like an overkill and a creates confusion as to where exactly 
    // are the properties stored. 
    // TODO: distinguish between the properties of the vertex and those of the 
    // atom container.
    
    @Override
    public Object getProperty(Object description)
    {
        return mol.getProperty(description);
    }
    
//-----------------------------------------------------------------------------
    // The Vertex class has properties and getter/setter for properties.
    // This looks like an overkill and a creates confusion as to where exactly 
    // are the properties stored. 
    // TODO: distinguish between the properties of the vertex and those of the 
    // atom container.
    
    @Override
    public void setProperty(Object description, Object property)
    {
        mol.setProperty(description, property);
    }
    
//-----------------------------------------------------------------------------
    
    // The Vertex class has properties and getter/setter for properties.
    // This looks like an overkill and a creates confusion as to where exactly 
    // are the properties stored. 
    // TODO: distinguish between the properties of the vertex and those of the 
    // atom container.
    
    public void setProperties(Map<Object, Object> properties)
    {
        mol.setProperties(properties);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another fragment ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertices represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMFragment other, StringBuilder reason)
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

    public int getHeavyAtomsCount()
    {
        return DENOPTIMMoleculeUtils.getHeavyAtomCount(mol);
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
    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }
    
//------------------------------------------------------------------------------

    @Override
    protected void setSymmetricAPSets(ArrayList<SymmetricSet> sAPs)
    {
        this.lstSymAPs = sAPs;
    }
    
//------------------------------------------------------------------------------

    @Override
    public ArrayList<SymmetricSet> getSymmetricAPSets()
    {
        return lstSymAPs;
    }
    
//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this vertex.
     * @param ignoredTypes a collection of mutation types to ignore. Vertexes
     * that allow only ignored types of mutation will
     * not be considered mutation sites.
     * @return the list of vertexes that allow any non-ignored mutation type.
     */
    
    @Override
    public List<DENOPTIMVertex> getMutationSites(List<MutationType> ignoredTypes)
    {
        List<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
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
    
    public static DENOPTIMFragment fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        DENOPTIMFragment returnedFrag = gson.fromJson(json, 
                DENOPTIMFragment.class);
        
        //TODO-gg this whole IF block will disappear once finished
        boolean oldApproach = false;
        if (returnedFrag.mol == null || returnedFrag.mol.getAtomCount()==0)
        {
            // In this case we have read a fragment from before we started
            // writing the mol to json.
            DENOPTIMFragment fragNoMol = gson.fromJson(json, DENOPTIMFragment.class);
            if (FragmentSpace.isDefined())
            {
                // If a fragment space exists, we rebuild the fragment from the
                // library, and attach to it the serialized data
                DENOPTIMFragment fragWithMol;
                try
                {
                    DENOPTIMVertex v = FragmentSpace.getVertexFromLibrary(
                                fragNoMol.getBuildingBlockType(),
                                fragNoMol.getBuildingBlockId());
                    if (v instanceof DENOPTIMFragment)
                    {
                        fragWithMol = (DENOPTIMFragment) v;
                    } else {
                        throw new JsonParseException("Building block "
                                + fragNoMol.getBuildingBlockType() + " "
                                + fragNoMol.getBuildingBlockId()
                                + " is not a " + DENOPTIMFragment.class.getName()
                                + "as claimed by JSON representation.");
                    }
                } catch (DENOPTIMException e)
                {
                    throw new JsonParseException("Could not get "
                            + fragNoMol.getBuildingBlockType() + " "
                            + fragNoMol.getBuildingBlockId()
                            + " from "
                            + "library. ",e);
                }
                ArrayList<SymmetricSet> cLstSymAPs =
                        new ArrayList<SymmetricSet>();
                for (SymmetricSet ss : fragNoMol.getSymmetricAPSets())
                {
                    cLstSymAPs.add(ss.clone());
                }

                fragWithMol.setMutationTypes(fragNoMol.getUnfilteredMutationTypes());
                fragWithMol.setSymmetricAPSets(cLstSymAPs);
                fragWithMol.setAsRCV(fragNoMol.isRCV());
                fragWithMol.setVertexId(fragNoMol.getVertexId());
                for (int iap = 0; iap<fragNoMol.getNumberOfAPs(); iap++)
                {
                    DENOPTIMAttachmentPoint oriAP = fragNoMol.getAP(iap);
                    DENOPTIMAttachmentPoint newAP = fragWithMol.getAP(iap);
                    newAP.setID(oriAP.getID());
                }
                returnedFrag = fragWithMol;
            } else {
                System.err.println("WARNING: undefined fragment space. "
                        + "Templates will contain fragments with no "
                        + "molecular representation. To avoid this, "
                        + "first "
                        + "define the fragment space, and "
                        + "then work with "
                        + "templates.");
                returnedFrag = fragNoMol;
            }
            oldApproach = true;
        }
        
        for (DENOPTIMAttachmentPoint ap : returnedFrag.getAttachmentPoints())
        {
            ap.setOwner(returnedFrag);
        }
        
        if (!oldApproach)
        {
            //TODO: one day this will disappear because of better handling of APs
            try
            {
                returnedFrag.projectListAPToAPProperties();
            } catch (DENOPTIMException e)
            {
                // This should never happen.
                e.printStackTrace();
            }
        }

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
    public IAtom getAtomHoldingAP(DENOPTIMAttachmentPoint ap)
    {
        if (ap.getOwner() != this)
            return null;
       
        return mol.getAtom(ap.getAtomPositionNumber());
    }
    
//------------------------------------------------------------------------------

}
