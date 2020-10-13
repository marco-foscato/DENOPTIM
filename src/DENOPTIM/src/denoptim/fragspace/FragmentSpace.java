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

package denoptim.fragspace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.GraphUtils;


/**
 * Class defining the fragment space
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class FragmentSpace
{
    /**
     * Data structure containing the molecular representation of
     * building blocks: scaffolds section - fragments that can be used
     * as seeds to grow a new molecule. 
     * WARNING! The objects stores in the library do not have a 
     * meaningful value for the two indexes representing the type
     * of building block and the position of the list of all building blocks
     * of that type.
     */
    private static ArrayList<DENOPTIMVertex> scaffoldLib = null;

    /**
     * Data structure containing the molecular representation of
     * building blocks: fragment section - fragments for general use.
     * WARNING! The objects stores in the library do not have a 
     * meaningful value for the two indexes representing the type
     * of building block and the position of the list of all building blocks
     * of that type.
     */
    private static ArrayList<DENOPTIMVertex> fragmentLib = null;

    /**
     * Data structure containing the molecular representation of
     * building blocks: capping group section - fragments with only
     * one attachment point used to saturate unused attachment
     * points on a graph. 
     * WARNING! The objects stores in the library do not have a 
     * meaningful value for the two indexes representing the type
     * of building block and the position of the list of all building blocks
     * of that type.
     */
    private static ArrayList<DENOPTIMVertex> cappingLib = null;

    /**
     * Data structure that stored the true entries of the 
     * attachment point classes compatibility matrix
     */
    private static HashMap<String, ArrayList<String>> compatMap;

    /**
     * Data structure that stores compatible APclasses for joining APs 
     * in ring-closing bonds. Symmetric, purpose specific
     * compatibility matrix.
     */
    private static HashMap<String, ArrayList<String>> rcCompatMap;

    /**
     * Data structure that stores the correspondence between bond order
     * and attachment point class.
     */
    private static HashMap<String, Integer> bondOrderMap;

    /**
     * Data structure that stores the AP-classes to be used to cap unused
     * APS on the growing molecule.
     */
    private static HashMap<String, String> cappingMap;

    /**
     * Data structure that stores AP classes that cannot be held unused
     */
    private static Set<String> forbiddenEndList;

    /**
     * Clusters of fragments based on the number of APs
     */
    private static HashMap<Integer, ArrayList<Integer>> fragPoolPerNumAP;

    /**
     * List of APclasses per each fragment
     */
    private static HashMap<Integer, ArrayList<String>> apClassesPerFrag;

    /** 
     * Clusters of fragments'AP based on AP classes
     */
    private static HashMap<String, ArrayList<ArrayList<Integer>>> 
							    fragsApsPerApClass;

    /**
     * APclass-specific constraints to constitutional symmetry
     */
    private static HashMap<String, Double> symmConstraints;
    
    /**
     * FLag defining use of AP class-based approach
     */
    protected static boolean apClassBasedApproch = false;
    
    /**
     * Flag signalling that this fragment space was built and validated
     */
    private static boolean isValid = false;
    
    /**
     * Index used to keep the order in a list of attachment points
     */
    public static AtomicInteger apID = new AtomicInteger();
    
//------------------------------------------------------------------------------
    
    /**
     * Define all components of a fragment space that does NOT 
     * implement the attachment point class-approach.
     * @param scaffLib library of fragments used to start the construction of
     * any new graph (i.e., seed or root fragments, a.k.a. scaffolds).
     * @param fragLib library of fragments for general purpose.
     * @param cappLib library of single-AP fragments used to cap free attachment 
     * points (i.e., the capping groups).
     * @param cpMap the APClass compatibility map. This data structure is a 
     * map of the APClass-on-growing-graph (key) to list of permitted APClasses
     * on incoming fragment (values).
     * @param boMap the map of APClass into bond order. This data structure is a 
     * map of APClass (keys) to bond order as integer (values).
     * @param capMap the capping rules. This data structure is a map of  
     * APClass-to-cap (keys) to APClass-of-capping-group (values).
     * @param forbEnds the list of forbidden ends, i.e., APClasses that cannot 
     * be left unused neither capped. 
     * @throws DENOPTIMException
     */
    public static void defineFragmentSpace(
    		ArrayList<DENOPTIMVertex> scaffLib,
    		ArrayList<DENOPTIMVertex> fragLib,
    		ArrayList<DENOPTIMVertex> cappLib) throws DENOPTIMException
    {
    	setScaffoldLibrary(scaffLib);
    	setFragmentLibrary(fragLib);
    	setCappingLibrary(cappLib);
    	apClassBasedApproch = false;
    	FragmentSpaceUtils.groupAndClassifyFragments(apClassBasedApproch);
    	isValid = true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Define all components of a fragment space that implements the attachment
     * point class-approach.
     * @param scaffLib library of fragments used to start the construction of
     * any new graph (i.e., seed or root fragments, a.k.a. scaffolds).
     * @param fragLib library of fragments for general purpose.
     * @param cappLib library of single-AP fragments used to cap free attachment 
     * points (i.e., the capping groups).
     * @param cpMap the APClass compatibility map. This data structure is a 
     * map of the APClass-on-growing-graph (key) to list of permitted APClasses
     * on incoming fragment (values).
     * @param boMap the map of APClass into bond order. This data structure is a 
     * map of APClass (keys) to bond order as integer (values).
     * @param capMap the capping rules. This data structure is a map of  
     * APClass-to-cap (keys) to APClass-of-capping-group (values).
     * @param forbEnds the list of forbidden ends, i.e., APClasses that cannot 
     * be left unused neither capped. 
     * @param rcCpMap the APClass compatibility matrix for ring closures.
     * @throws DENOPTIMException
     */
    public static void defineFragmentSpace(
    		ArrayList<DENOPTIMVertex> scaffLib,
    		ArrayList<DENOPTIMVertex> fragLib,
    		ArrayList<DENOPTIMVertex> cappLib,
    		HashMap<String,ArrayList<String>> cpMap,
    		HashMap<String,Integer> boMap,
    		HashMap<String,String> capMap,
    		HashSet<String> forbEnds,
    		HashMap<String,ArrayList<String>> rcCpMap) throws DENOPTIMException
    {
    	setScaffoldLibrary(scaffLib);
    	setFragmentLibrary(fragLib);
    	setCappingLibrary(cappLib);
    	setCompatibilityMatrix(cpMap);
    	apClassBasedApproch = true;
    	setBondOrderMap(boMap);
    	setCappingMap(capMap);
    	setForbiddenEndList(forbEnds);
    	setRCCompatibilityMatrix(rcCpMap);
   
    	FragmentSpaceUtils.groupAndClassifyFragments(apClassBasedApproch);
    	
    	isValid = true;
    	
    	//TODO-V3: remove: tmp code just for devel phase
        if (FragmentSpaceParameters.useTemplates)
        {
//            fragmentLib.add(DENOPTIMTemplate.getTestTemplate());
//            System.err.println("Added test template to fragment library");
            scaffoldLib = new ArrayList<>();
            scaffoldLib.add(DENOPTIMTemplate.getTestTemplate());
            System.err.println("WARNING! Running TEMP CODE: Replaced scaffold lib with single test template");
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Creates the fragment space as defined in a formatted text file with the 
     * parameters.
     * @param paramFile text file with the parameters defining the fragment
     * space.
     * @throws Exception
     */
    public static void defineFragmentSpace(String paramFile) throws Exception
    {
        String line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(paramFile));
            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }
			
				if (line.toUpperCase().startsWith("FS-"))
	            {
	                FragmentSpaceParameters.interpretKeyword(line);
	                continue;
	            }
				
	            if (line.toUpperCase().startsWith("RC-"))
	            {
	                RingClosureParameters.interpretKeyword(line);
	                continue;
	            }
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
        }
        finally
        {
            if (br != null)
            {
                br.close();
                br = null;
            }
        }    
		
		// This creates the static FragmentSpace object
		if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.checkParameters();
            FragmentSpaceParameters.processParameters();
        }
        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
            RingClosureParameters.processParameters();
        }
        
        isValid = true;
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks for valid definition of this fragment space
     * @return <code>true</code> if this fragment space has been defined
     */
    public static boolean isDefined()
    {
    	return isValid;
    }
    
//------------------------------------------------------------------------------

    /**
     * Check usage of APClass-based approach, i.e., uses attachment points with 
     * annotated data (i.e., the APClass) to evaluate compatibilities between
     * attachment points.
     * @return <code>true</code> if this fragment space makes use of 
     * APClass-based approach
     */
    public static boolean useAPclassBasedApproach()
    {
        return apClassBasedApproch;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Search for a specific AP on a specific fragment and finds out its class.
     * @param the identified of a specific attachment point.
     * @return the AP class or null
     */
    public static String getAPClassForFragment(IdFragmentAndAP apId)
    {
    	String cls = null;
    	try
    	{
    		DENOPTIMVertex frg = FragmentSpace.getVertexFromLibrary(
        			apId.getVertexMolType(), apId.getVertexMolId());
    		cls = frg.getAttachmentPoints().get(apId.getApId()).getAPClass();
    	}
    	catch (Throwable t)
    	{
    		cls = null;
    	}
    			
    	return cls;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a clone of the requested building block. The type of vertex
     * returned depends on the type stored in the library.
     * 
     * @param bbTyp the type of building block. This basically selects the 
     * sub library from which the building block is taken: 0 for scaffold (i.e.,
     * building blocks that can be used to start a new graph), 1 for
     * standard building blocks (i.e., can be used freely to grow or modify an 
     * existing graph), or 2 for capping group (i.e., can be used only to 
     * saturate attachment points that cannot remain unused in a finished
     * graph).
     * @param bbIdx the index (0-based) of the building block in
     * the corresponding library defied by the type of building 
     * block 'bbType'
     * @return a clone of the chosen building block.
     * @throws DENOPTIMException when the given indexes cannot be used, for
     * example, any of the indexes is out of range.  
     */

    public static DENOPTIMVertex getVertexFromLibrary(int bbTyp, int bbIdx) 
						        throws DENOPTIMException
    {
        // WARNING! This is were we first assign the bbTyp and bbIdx to
        // a vertex 'taken' from the library. Note that 'taken' means that we 
        // get a slightly modified copy of the vertex. In particular, the
        // objects stores in the library do not have a meaningful value for the
        // two indexes, so after we make a deep copy of them we assign the 
        // indexes according to bbTyp and bbIdx.
        
		String msg = "";
		if (fragmentLib == null || scaffoldLib == null || cappingLib == null)
		{
		    msg = "Cannot retrieve fragments before defining the FragmentSpace";
		    throw new DENOPTIMException(msg);
		}
		DENOPTIMVertex originalVrtx = null;
		switch (bbTyp)
		{
			case 0:
			    if (bbIdx < scaffoldLib.size())
			    {
			    	originalVrtx = scaffoldLib.get(bbIdx);	
			    }
			    else
			    {
				msg = "Mismatch between scaffold bbIdx and size of the library"
				      + ". MolId: " + bbIdx + " FragType: " + bbTyp;
		                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
				throw new DENOPTIMException(msg);
			    }
			    break;
			case 1:
	            if (bbIdx < fragmentLib.size())
	            {
	                originalVrtx = fragmentLib.get(bbIdx);
	            }
	            else
	            {
	                msg = "Mismatch between fragment bbIdx and size of the "
	                		+ "library" + ". MolId: " + bbIdx 
	                		+ " FragType: " + bbTyp;
	                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
	                throw new DENOPTIMException(msg);
	            }
			    break;
			case 2:
	            if (bbIdx < cappingLib.size())
	            {
	                originalVrtx = cappingLib.get(bbIdx);
	            }
	            else
	            {
	                msg = "Mismatch between capping group bbIdx and size "
	                		+ "of the library. MolId: " + bbIdx 
	                		+ " FragType: " + bbTyp;
	                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
	                throw new DENOPTIMException(msg);
	            }
	            break;
			default:
			    msg = "Unknown type of fragment '" + bbTyp + "'.";
		            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
			    throw new DENOPTIMException(msg);
		}
		
		DENOPTIMVertex clone = originalVrtx.clone();
		
		//TODO-V3: is there a better way to do this in a type-agnostic way?
		if (clone instanceof DENOPTIMFragment)
		{
    		((DENOPTIMFragment) clone).setMolId(bbIdx);
    		((DENOPTIMFragment) clone).setFragmentType(bbTyp);
		}
		//TODO-V3 keep it or trash it
		else if (clone instanceof DENOPTIMTemplate)
        {
            ((DENOPTIMTemplate) clone).setMolId(bbIdx);
            ((DENOPTIMTemplate) clone).setFragmentType(bbTyp);
        }
        else 
		{
		    System.err.println("WARNING! Recovering a vertex that is not an"
		            + " instance of fragment. Not setting bbType and bbIdx.");
		}
		
        return clone;
    }

//------------------------------------------------------------------------------

    public static ArrayList<DENOPTIMVertex> getScaffoldLibrary()
    {
        return scaffoldLib;
    }

//------------------------------------------------------------------------------

    public static ArrayList<DENOPTIMVertex> getFragmentLibrary()
    {
        return fragmentLib;
    }

//------------------------------------------------------------------------------

    public static ArrayList<DENOPTIMVertex> getCappingLibrary()
    {
        return cappingLib;
    }

//------------------------------------------------------------------------------

    /**
     * @param query the APClass of the attachment point on the capping group
     * @return all the capping groups which have the given APclass
     */
    public static ArrayList<Integer> getCappingGroupsWithAPClass(String query)
    {
	ArrayList<Integer> selected = new ArrayList<>();
	for (int i=0; i<cappingLib.size(); i++)
	{
	    String apc = "";
	    try 
	    {
            apc = getVertexFromLibrary(2, i).getAttachmentPoints().get(0).getAPClass();
		if (apc.equals(query))
		{
		    selected.add(i);
		}
	    }
	    catch (DENOPTIMException de)
	    {
		// nothing
	    } 
	}
	return selected;	
    }

//------------------------------------------------------------------------------

    /**
     * Load info from a compatibility matrix file.
     * This method imports information such as the compatibility matrix,
     * bond order map, and forbidden ends from
     * a compatibility matrix file (i.e., a formatted text file using DENOPTIM
     * keyword. This overrides any previous setting of such information in this
     * FragmentSpace.
     * @param inFile the pathname of the compatibility matrix file
     */

    public static void importCompatibilityMatrixFromFile(String inFile) 
							throws DENOPTIMException
    {
            setCompatibilityMatrix(new HashMap<String,ArrayList<String>>());
            setBondOrderMap(new HashMap<String,Integer>());
            setCappingMap(new HashMap<String,String>());
            setForbiddenEndList(new HashSet<String>());
            DenoptimIO.readCompatibilityMatrix(inFile,
						compatMap,
						bondOrderMap,
						cappingMap,
						forbiddenEndList);
    }

//------------------------------------------------------------------------------

    /**
     * Load info for ring closures compatibilities from a compatibility matrix 
     * file.
     * @param inFile the pathname of the RC-compatibility matrix file
     */

    public static void importRCCompatibilityMatrixFromFile(String inFile)
                                                        throws DENOPTIMException
    {
            setRCCompatibilityMatrix(new HashMap<String,ArrayList<String>>());
            DenoptimIO.readRCCompatibilityMatrix(inFile,rcCompatMap);
    }

//------------------------------------------------------------------------------

    public static HashMap<String, ArrayList<String>> getCompatibilityMatrix()
    {
        return compatMap;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getCompatibleAPClasses(String query)
    {
	return compatMap.get(query);
    } 

//------------------------------------------------------------------------------

    /**
     * Returns the compatibility matrix for ring closing fragment-fragment
     * connections or <code>null</code> if not provided in the parameters file.
     * @return 
     */

    public static HashMap<String,ArrayList<String>> getRCCompatibilityMatrix()
    {
        return rcCompatMap;
    }

//------------------------------------------------------------------------------

    public static HashMap<String, Integer> getBondOrderMap()
    {
        return bondOrderMap;
    }

//------------------------------------------------------------------------------

   /**
    * Returns the bond order for the given APClass, if defined.
    * @param apclass the APclass to be converted into bond order
    * @return the bond order as an integer, or 1 if either the
    * Fragment space is not defined, that is, the bond order map is 
    * <code>null</code>, or a fully defined map does not include any mapping 
    * for the given APClass.
    */
    public static Integer getBondOrderForAPClass(String apclass)
    {
        if (!isValid || bondOrderMap == null)
        {
            String msg = "Attempting to get bond order, but no "
                       + "FragmentSpace defined (i.e., null BondOrderMap). "
                       + "Assuming bond order one.";
            DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
            return 1;
        }
        else return bondOrderMap.getOrDefault(apclass, 1);
    }

//------------------------------------------------------------------------------

    public static HashMap<String, String> getCappingMap()
    {
        return cappingMap;
    }

//------------------------------------------------------------------------------

    /**
     * @param srcApClass the attachment point class of the attachment point to
     * be capped
     * @return the APClass of the capping group or null
     */

    public static String getCappingClass(String srcApClass)
    {
	return cappingMap.get(srcApClass);
    }

//------------------------------------------------------------------------------

    public static Set<String> getForbiddenEndList()
    {
        return forbiddenEndList;
    }

//------------------------------------------------------------------------------

    /**
     * Return the set of APClasses that used in the compatibility matrix
     * for the growing graph APs.
     * Note these APClasses do include subclasses.
     * For example, for AP with class <code>MyAPClass:0</code> the 
     * <code>0</code> is the subclass.
     * @return the lst of APClasses
     */

    public static Set<String> getAllAPClassesFromCPMap()
    {
        return FragmentSpace.getCompatibilityMatrix().keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Return the set of APClasses that are defined in the bond order map.
     * Note the APClasses in the bond order map fo not include the subclass.
     * For example, for AP with class <code>MyAPClass:0</code> the map
     * stores only <code>MyAPClass</code>.
     * @return the lst of APClasses
     */
    
    public static Set<String> getAllAPClassesFromBOMap()
    {
	return FragmentSpace.getBondOrderMap().keySet();
    }

//------------------------------------------------------------------------------

    public static HashMap<Integer,ArrayList<Integer>> getMapOfFragsPerNumAps()
    {
	return fragPoolPerNumAP;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of fragments with given number of APs
     * @param nAps the number of attachment points
     * @return the list of fragments as indexes in the library of fragments.
     */

    public static ArrayList<Integer> getFragsWithNumAps(int nAps)
    {
	ArrayList<Integer> lst = new ArrayList<>();
	if (fragPoolPerNumAP.containsKey(nAps))
	{
	    lst = fragPoolPerNumAP.get(nAps);
	}
        return lst;
    }

//------------------------------------------------------------------------------
   
    public static HashMap<Integer,ArrayList<String>> 
						    getMapAPClassesPerFragment()
    {
	return apClassesPerFrag;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the APclasses associated with a given fragment.
     * @param fragId the index pf the fragment in the library
     * @return the list of APclasses found of the fragment
     */

    public static ArrayList<String> getAPClassesPerFragment(int fragId)
    {
        return apClassesPerFrag.get(fragId);
    }

//------------------------------------------------------------------------------

    public static HashMap<String,ArrayList<ArrayList<Integer>>> 
						      getMapFragsAPsPerAPClass()
    {
        return fragsApsPerApClass;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of attachment points with the given class. The returned
     * identifiers have <code>vertex_id</code>=-1 
     * because these APs are only on the individual 
     * fragments held in the library and do not belong to any graph.
     * @param apclass 
     * @return the list of AP identifiers.
     */

    public static ArrayList<IdFragmentAndAP> getFragsWithAPClass(
								 String apclass)
    {
		ArrayList<IdFragmentAndAP> lst = new ArrayList<IdFragmentAndAP>();
		
		if (fragsApsPerApClass.containsKey(apclass))
		{
		    for (ArrayList<Integer> idxs : fragsApsPerApClass.get(apclass))
		    {
		    	IdFragmentAndAP apId = new IdFragmentAndAP(-1, //vertexId
		    											   idxs.get(0), //MolId,
														   1, //FragType
														   idxs.get(1), //ApId
														   -1, //noVSym
														   -1);//noAPSym
		    	lst.add(apId);
		    }
		}
        return lst;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Searches for all building blocks that are compatible with the given 
     * list of APs.
     * @param srcAPs the identifiers of APs meant to hold any of the desired
     * fragments.
     * @return a list of fragments.
     */
    public static ArrayList<DENOPTIMVertex> getFragmentsCompatibleWithTheseAPs(
    		ArrayList<IdFragmentAndAP> srcAPs)
    {
    	// First we get all possible APs on any fragment
    	ArrayList<IdFragmentAndAP> compatFragAps = 
				FragmentSpace.getFragAPsCompatibleWithTheseAPs(srcAPs);
    	
    	// then keep unique fragment identifiers, and store unique
		Set<Integer> compatFragIds = new HashSet<Integer>();
		for (IdFragmentAndAP apId : compatFragAps)
		{
			compatFragIds.add(apId.getVertexMolId());
		}
		
		// Then we pack-up the selected list of fragments
		ArrayList<DENOPTIMVertex> compatFrags = 
				new ArrayList<DENOPTIMVertex>();
		for (Integer fid : compatFragIds)
		{
			try {
				compatFrags.add(FragmentSpace.getVertexFromLibrary(1, fid));
			} catch (DENOPTIMException e) {
		        System.err.println("Exception while trying to get fragment '"+fid+"'!");
		        e.printStackTrace();
			}
		}
		
		return compatFrags;
    }
    
//------------------------------------------------------------------------------
   
    /**
     * Searches for all APs that are compatible with the given list of APs.
     * @param srcAPs the identifiers of APs meant to hold any of the desired
     * fragments.
     * @return a list of identifiers for APs on fragments in the library.
     */
    public static ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithTheseAPs(
    		ArrayList<IdFragmentAndAP> srcAPs)
    {
		ArrayList<IdFragmentAndAP> compFrAps = new ArrayList<IdFragmentAndAP>();
		boolean first = true;
		for (IdFragmentAndAP apId : srcAPs)
		{
			String srcApCls = getAPClassForFragment(apId);
			ArrayList<IdFragmentAndAP> compForOne = 
                                         getFragAPsCompatibleWithClass(srcApCls);
            
			if (first)
			{
				compFrAps.addAll(compForOne);
				first = false;
				continue;
			}
			
			ArrayList<IdFragmentAndAP> toKeep = new ArrayList<IdFragmentAndAP>();
			for (IdFragmentAndAP candAp : compFrAps)
			{
				for (IdFragmentAndAP newId : compForOne)
				{
					if (newId.sameFragAndAp(candAp))
					{
						toKeep.add(candAp);
						break;
					}
				}
			}
			
			compFrAps = toKeep;
			
			if (compFrAps.size()==0)
			{
				break;
			}
		}

		return compFrAps;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of attachment points found in the fragment 
     * space and that are compatible with a given AP class. 
     * Multiple APs can be found for each fragment, 
     * thus this method returns pairs of
     * integers identifying the fragment (first integer in the pair) and the
     * AP (second index in the pair).
     * @param srcApCls the AP class for which we want compatible APs
     */
    
    public static ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithClass(
    		String srcApCls)
    {
    	ArrayList<IdFragmentAndAP> compatFragAps = 
    			new ArrayList<IdFragmentAndAP>();
    	
    	// Take the compatible AP classes
		ArrayList<String> compatApClasses = 
		     FragmentSpace.getCompatibleAPClasses(srcApCls);
		
		// Find all APs with any compatible class
		if (compatApClasses != null)
		{
			for (String compClass : compatApClasses)
			{
			    compatFragAps.addAll(
			    		FragmentSpace.getFragsWithAPClass(compClass));
			}
		}
		
		return compatFragAps;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if the symmetry settings impose use of symmetry on attachment 
     * points of the given AP class. The value returned is the result 
     * of the action of symmetry-related keyword affecting the definition of
     * this FragmentSpace.
     * @param apClass the attachment point class
     * @return <code>true<code> if symmetry has the applied on APs of the
     * given class
     */

    public static boolean imposeSymmetryOnAPsOfClass(String apClass)
    {
	boolean res = true;
        if (hasSymmetryConstrain(apClass))
        {
            if (getSymmetryConstrain(apClass) <
                             (1.0 - DENOPTIMConstants.FLOATCOMPARISONTOLERANCE))
            {
                res = false;
            }
        }
        else
        {
            if (!FragmentSpaceParameters.enforceSymmetry())
            {
                res = false;
            }
        }
	return res;
    }
     

//------------------------------------------------------------------------------

    /**
     * Checks if there is a constraint on the constitutional 
     * symmetry probability for the given AP class.
     * @param apClass the attachment point class
     * @return <code>true<code> if there is a constraint on the constitutional
     * symmetry probability for the given AP class.
     */

    public static boolean hasSymmetryConstrain(String apClass)
    {
        return symmConstraints.containsKey(apClass);
    }

//------------------------------------------------------------------------------

    /**
     * Return the constitutional symmetry constrain for the given APclass,
     * or null.
     * The constrain is
     * a fixed probability that is not dependent on the distance from the
     * root of the DENOPTIMGraph (i.e. the level).
     * @param apClass the attachment point class
     * @return the constrained value of the symmetric substitution probability
     * (0.0 - 1.0).
     */

    public static double getSymmetryConstrain(String apClass)
    {
        return symmConstraints.get(apClass);
    }

//------------------------------------------------------------------------------

    public static void setScaffoldLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        scaffoldLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setFragmentLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        fragmentLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setCappingLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        cappingLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setCompatibilityMatrix(
					HashMap<String,ArrayList<String>> map)
    {
        compatMap = map;
    }

//------------------------------------------------------------------------------

    public static void setRCCompatibilityMatrix(
					HashMap<String,ArrayList<String>> map)
    {
       rcCompatMap = map;
    }

//------------------------------------------------------------------------------

    public static void setBondOrderMap(HashMap<String, Integer> map)
    {
        bondOrderMap = map;
    }

//------------------------------------------------------------------------------

    public static void setCappingMap(HashMap<String, String> map)
    {
        cappingMap = map;
    }

//------------------------------------------------------------------------------

    public static void setForbiddenEndList(Set<String> lst)
    {
        forbiddenEndList = lst;
    }

//------------------------------------------------------------------------------

    public static void setFragPoolPerNumAP(HashMap<Integer,ArrayList<Integer>>
									    map)
    {
        fragPoolPerNumAP = map;
    }

//------------------------------------------------------------------------------

    public static void setFragsApsPerApClass(
			      HashMap<String,ArrayList<ArrayList<Integer>>> map)
    {
        fragsApsPerApClass = map;
    }

//------------------------------------------------------------------------------

    public static void setAPClassesPerFrag(HashMap<Integer,ArrayList<String>> 
									    map)
    {
        apClassesPerFrag = map;
    }

//------------------------------------------------------------------------------

    public static void setSymmConstraints(HashMap<String,Double> map)
    {
        symmConstraints = map;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Clears all settings of this fragment space. All fields changed to
     * <code>null</code>.
     */
    public static void clearAll()
    {	
        scaffoldLib = null;
        fragmentLib = null;
        cappingLib = null;
        compatMap = null;
        rcCompatMap = null;
        bondOrderMap= null;
        cappingMap = null;
        forbiddenEndList = null;
        fragPoolPerNumAP = null;
        apClassesPerFrag = null;
        fragsApsPerApClass = null;
        symmConstraints = null;
		isValid = false;
    }
    
//------------------------------------------------------------------------------

}
