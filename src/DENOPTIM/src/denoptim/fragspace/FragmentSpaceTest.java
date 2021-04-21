package denoptim.fragspace;

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

import java.io.File;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.vecmath.Point3d;

import denoptim.molecule.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.utils.GraphUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for fragment space
 * 
 * @author Marco Foscato
 */

public class FragmentSpaceTest
{
    private static final String SEP = System.getProperty("file.separator");

    @TempDir 
    static File tempDir;

    private final Random rng = new Random();
    
	private static final String APSUBRULE = "0";

    private static final String RULAPCS = "apc-S";
    private static final String RULAPC1 = "apc-1";
    private static final String RULAPC2 = "apc-2";
    private static final String RULAPC3 = "apc-3";
    private static final String RULAPC4 = "apc-4";
    private static final String RULAPC5 = "apc-5";
    private static final String RULAPC6 = "apc-6";
    private static final String RULAPCC1 = "cap-1";
    private static final String RULAPCC2 = "cap-2";
    
    private static final BBType BBTFRAG = BBType.FRAGMENT;
    
	private static APClass APCS;
	private static APClass APC1;
	private static APClass APC2;
	private static APClass APC3;
    private static APClass APC4;
    private static APClass APC5;
    private static APClass APC6;
	private static APClass APCC1;
	private static APClass APCC2;

//------------------------------------------------------------------------------
	/*
	 * Ideally this could be run once before all tests of this class. However,
	 * attempts to use @BeforeAll have shown that the static FragmentSpace does 
	 * not remain defined after for all tests. As a workaround, the definition
	 * of the fragment space is run @BeforeEach test.
	 */
	@BeforeEach
	private void buildFragmentSpace() throws DENOPTIMException
	{
	    assertTrue(tempDir.isDirectory(),"Should be a directory ");
	    
        try
        {
            APCS = APClass.make(RULAPCS
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC1 = APClass.make(RULAPC1
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC2 = APClass.make(RULAPC2
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC3 = APClass.make(RULAPC3
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC4 = APClass.make(RULAPC4
                    + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC5 = APClass.make(RULAPC5
                    + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC6 = APClass.make(RULAPC6
                    + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APCC1 = APClass.make(RULAPCC1
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APCC2 = APClass.make(RULAPCC2
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
        } catch (DENOPTIMException e)
        {
            //This will not happen
        }
	       
        HashMap<String,BondType> boMap = new HashMap<String,BondType>();
        boMap.put(RULAPCS,BondType.SINGLE);
        boMap.put(RULAPC1,BondType.SINGLE);
        boMap.put(RULAPC2,BondType.SINGLE);
        boMap.put(RULAPC3,BondType.SINGLE);
        boMap.put(RULAPC4,BondType.SINGLE);
        boMap.put(RULAPC5,BondType.SINGLE);
        boMap.put(RULAPC6,BondType.DOUBLE);
        boMap.put(RULAPCC1,BondType.SINGLE);
        boMap.put(RULAPCC2,BondType.SINGLE);
        
        FragmentSpace.setBondOrderMap(boMap);

        String rootName = tempDir.getAbsolutePath() + SEP;

    	ArrayList<DENOPTIMFragment> fragLib = new ArrayList<DENOPTIMFragment>();
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAPOnAtom(a3, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAPOnAtom(a3, APC1, new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAPOnAtom(a3, APC2, new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAPOnAtom(a1, APC3, new Point3d(new double[]{3.0, 0.0, 3.3}));
    	frg1.projectAPsToProperties();
    	fragLib.add(frg1);
    	
        DENOPTIMFragment frg2 = new DENOPTIMFragment();
        Atom a21 = new Atom("N", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a22 = new Atom("H", new Point3d(new double[]{1.0, 1.1, 2.2}));
        frg2.addAtom(a21);
        frg2.addAtom(a22);
        frg2.addBond(new Bond(a21, a22));
        frg2.addAPOnAtom(a22, APC2, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg2.addAPOnAtom(a22, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg2.projectAPsToProperties();
        fragLib.add(frg2);
        
        DENOPTIMFragment frg3 = new DENOPTIMFragment();
        Atom a31 = new Atom("P", new Point3d(new double[]{0.0, 1.1, 2.2}));
        frg3.addAtom(a31);
        frg3.addAPOnAtom(a31, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg3.addAPOnAtom(a31, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg3.addAPOnAtom(a31, APC3, new Point3d(new double[]{0.0, 0.0, 1.1}));
        frg3.projectAPsToProperties();
        fragLib.add(frg3);
        
        DENOPTIMFragment frg8 = new DENOPTIMFragment();
        Atom a81 = new Atom("C", new Point3d(new double[]{0.0, 1.1, -2.2}));
        Atom a82 = new Atom("C", new Point3d(new double[]{1.0, 1.1, -2.2}));
        Atom a83 = new Atom("C", new Point3d(new double[]{2.0, 1.1, -2.2}));
        frg8.addAtom(a81);
        frg8.addAtom(a82);
        frg8.addAtom(a83);
        frg8.addBond(new Bond(a81, a82));
        frg8.addBond(new Bond(a82, a83));
        frg8.addAPOnAtom(a83, APC4, new Point3d(new double[]{0.0, 2.2, -3.3}));
        frg8.addAPOnAtom(a83, APC4, new Point3d(new double[]{0.0, 0.0, -3.3}));
        frg8.addAPOnAtom(a83, APC6, new Point3d(new double[]{0.0, 0.0, -1.1}));
        frg8.addAPOnAtom(a82, APC5, new Point3d(new double[]{1.0, 0.1, -2.2}));
        frg8.addAPOnAtom(a82, APC5, new Point3d(new double[]{1.0, 0.1, -1.2}));
        frg8.addAPOnAtom(a82, APC5, new Point3d(new double[]{1.0, 2.1, -2.2}));
        frg8.addAPOnAtom(a81, APC5, new Point3d(new double[]{3.0, 0.0, -3.3}));
        frg8.projectAPsToProperties();
        fragLib.add(frg8);
        
        String fragLibFile = rootName + "frags.sdf";
        DenoptimIO.writeFragmentSet(fragLibFile, fragLib);
        
    	ArrayList<DENOPTIMFragment> scaffLib = new ArrayList<DENOPTIMFragment>();
        DENOPTIMFragment scaf0 = new DENOPTIMFragment();
        Atom a41 = new Atom("O", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a42 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a43 = new Atom("Ru", new Point3d(new double[]{2.0, 1.1, 2.2}));
        scaf0.addAtom(a41);
        scaf0.addAtom(a42);
        scaf0.addAtom(a43);
        scaf0.addBond(new Bond(a41, a42));
        scaf0.addBond(new Bond(a42, a43));
        scaf0.addAPOnAtom(a43, APCS, new Point3d(new double[]{0.0, 2.2, 3.3}));
        scaf0.addAPOnAtom(a43, APCS, new Point3d(new double[]{0.0, 0.0, 3.3}));
        scaf0.addAPOnAtom(a43, APCS, new Point3d(new double[]{0.0, 0.0, 1.1}));
        scaf0.addAPOnAtom(a41, APCS, new Point3d(new double[]{3.0, 0.0, 3.3}));
        scaf0.projectAPsToProperties();
        // NB: in the sorted list the last AP is first!
        scaffLib.add(scaf0);
        
        DENOPTIMFragment scaf1 = new DENOPTIMFragment();
        Atom a51 = new Atom("Zn", new Point3d(new double[]{5.0, 1.1, 2.2}));
        scaf1.addAtom(a51);
        scaf1.addAPOnAtom(a51, APCS, new Point3d(new double[]{5.0, 2.2, 3.3}));
        scaf1.addAPOnAtom(a51, APCS, new Point3d(new double[]{5.0, 0.0, 3.3}));
        scaf1.addAPOnAtom(a51, APCS, new Point3d(new double[]{5.0, 0.0, 1.1}));
        scaf1.projectAPsToProperties();
        scaffLib.add(scaf1);
        
        String scaffLibFile = rootName + "scaff.sdf";
        DenoptimIO.writeFragmentSet(scaffLibFile, scaffLib);
        
        ArrayList<DENOPTIMFragment> cappLib = new ArrayList<DENOPTIMFragment>();
        DENOPTIMFragment cap1 = new DENOPTIMFragment();
        Atom a61 = new Atom("H", new Point3d(new double[]{10.0, 1.1, 2.2}));
        cap1.addAtom(a61);
        cap1.addAPOnAtom(a61, APCC1, new Point3d(new double[]{13.0, 0.0, 3.3}));
        cap1.projectAPsToProperties();
        cappLib.add(cap1);
        
        DENOPTIMFragment cap2 = new DENOPTIMFragment();
        Atom a71 = new Atom("Cl", new Point3d(new double[]{10.0, 1.1, 2.2}));
        cap2.addAtom(a71);
        cap2.addAPOnAtom(a71, APCC2, new Point3d(new double[]{13.0, 0.0, 3.3}));
        cap2.projectAPsToProperties();
        cappLib.add(cap2);

        String capLibFile = rootName + "caps.sdf";
        DenoptimIO.writeFragmentSet(capLibFile, cappLib);
        
    	HashMap<APClass,ArrayList<APClass>> cpMap = 
    	        new HashMap<APClass,ArrayList<APClass>>();
    	ArrayList<APClass> lst1 = new ArrayList<APClass>();
    	lst1.add(APC1);
    	lst1.add(APC2);
    	cpMap.put(APCS, lst1);
    	ArrayList<APClass> lst2 = new ArrayList<APClass>();
    	lst2.add(APC2);
    	cpMap.put(APC1, lst2);
    	ArrayList<APClass> lst3 = new ArrayList<APClass>();
    	lst3.add(APC2);
    	lst3.add(APC3);
    	cpMap.put(APC2, lst3);
    	
    	HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
    	capMap.put(APCS, APCC2);
    	capMap.put(APC1, APCC1);
    	capMap.put(APC2, APCC1);

    	HashSet<APClass> ends = new HashSet<APClass>();
    	ends.add(APC3);
    	
    	String cpmFile = rootName + "cpm.dat";
    	DenoptimIO.writeCompatibilityMatrix(cpmFile, cpMap, boMap, capMap,ends);

    	/*
    	//Just in case one day we want to have also an RC-CPMap
    	HashMap<APClass,ArrayList<APClass>> rcCpMap = 
    			new HashMap<APClass,ArrayList<APClass>>();
    	*/
    	
    	FragmentSpace.defineFragmentSpace(scaffLibFile, fragLibFile, capLibFile,
    	        cpmFile);
	}
	
//-----------------------------------------------------------------------------        
    
    @Test
    public void testSymmetry() throws Exception
    {
        assertTrue(FragmentSpace.isDefined(),"FragmentSpace is defined");
        DENOPTIMVertex v = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(),3,BBType.FRAGMENT);
        
        assertEquals(2,v.getSymmetricAPSets().size(),
                "Number of symmetric sets of APs");
        
        Map<APClass,Integer> expectedCount = new HashMap<APClass,Integer>();
        expectedCount.put(APC4, 2);
        expectedCount.put(APC5, 3);
        for (SymmetricSet ss : v.getSymmetricAPSets())
        {
            APClass apc = v.getAP(ss.get(0)).getAPClass();
            assertEquals(expectedCount.get(apc),ss.size(), 
                    "Number of APs in symmetric set for APClass "+apc);
        }
    }
	
//-----------------------------------------------------------------------------    	
	
    @Test
    public void testGetFragsWithAPClass() throws Exception
    {
        assertTrue(FragmentSpace.isDefined(),"FragmentSpace is defined");
    	ArrayList<IdFragmentAndAP> l = FragmentSpace.getFragsWithAPClass(APC2);
    	assertEquals(4,l.size(),"Wrong size of AP IDs with given APClass.");
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,BBTFRAG,3,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,BBTFRAG,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	
    	boolean found1 = false;
    	boolean found2 = false;
    	boolean found3 = false;
    	boolean found4 = false;
    	for (IdFragmentAndAP id : l)
    	{
    		if (id.sameFragAndAp(ref1))
    			found1 = true;
    		if (id.sameFragAndAp(ref2))
    			found2 = true;
    		if (id.sameFragAndAp(ref3))
    			found3 = true;
    		if (id.sameFragAndAp(ref4))
    			found4 = true;
    	}
    	
    	assertTrue(found1, "Missing first AP ID.");
    	assertTrue(found2, "Missing second AP ID.");
    	assertTrue(found3, "Missing third AP ID.");
    	assertTrue(found4, "Missing fourth AP ID.");
    	
    	FragmentSpace.clearAll();
    }

//-----------------------------------------------------------------------------    	
    	
    @Test
    public void testGetFragAPsCompatibleWithClass() throws Exception
    {
        assertTrue(FragmentSpace.isDefined(),"FragmentSpace is defined");
    	ArrayList<IdFragmentAndAP> lst = 
    			FragmentSpace.getFragAPsCompatibleWithClass(APC1);
    	
    	assertEquals(4,lst.size(),"Size of compatible APs list is wrong.");
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,BBTFRAG,3,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,BBTFRAG,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	
    	boolean found1 = false;
    	boolean found2 = false;
    	boolean found3 = false;
    	boolean found4 = false;
    	for (IdFragmentAndAP id : lst)
    	{
    		if (id.sameFragAndAp(ref1))
    			found1 = true;
    		if (id.sameFragAndAp(ref2))
    			found2 = true;
    		if (id.sameFragAndAp(ref3))
    			found3 = true;
    		if (id.sameFragAndAp(ref4))
    			found4 = true;
    	}
    	
    	assertTrue(found1, "Missing first AP ID.");
    	assertTrue(found2, "Missing second AP ID.");
    	assertTrue(found3, "Missing third AP ID.");
    	assertTrue(found4, "Missing fourth AP ID.");
    	
    	FragmentSpace.clearAll();
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetFragAPsCompatibleWithTheseAPs() throws Exception
    {
        assertTrue(FragmentSpace.isDefined(),"FragmentSpace is defined");
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	
    	/*
    	System.out.println("SRC1 "+FragmentSpace.getAPClassForFragment(src1)+" => "
    	+FragmentSpace.getCompatibleAPClasses(FragmentSpace.getAPClassForFragment(src1)));
    	System.out.println("SRC2 "+FragmentSpace.getAPClassForFragment(src2)+" => "
    	+FragmentSpace.getCompatibleAPClasses(FragmentSpace.getAPClassForFragment(src2)));
    	System.out.println("Frags with SRC1: "+FragmentSpace.getFragAPsCompatibleWithClass(
    	        FragmentSpace.getAPClassForFragment(src1)));
        System.out.println("Frags with SRC2: "+FragmentSpace.getFragAPsCompatibleWithClass(
                FragmentSpace.getAPClassForFragment(src2)));
    	*/
    	
    	ArrayList<IdFragmentAndAP> lst = 
    			FragmentSpace.getFragAPsCompatibleWithTheseAPs(srcAPs);
    	
    	assertEquals(4,lst.size(),"Size of compatible APs list is wrong.");
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,BBTFRAG,3,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,BBTFRAG,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	
    	boolean found1 = false;
    	boolean found2 = false;
    	boolean found3 = false;
    	boolean found4 = false;
    	for (IdFragmentAndAP id : lst)
    	{            
    		if (id.sameFragAndAp(ref1))
    			found1 = true;
    		if (id.sameFragAndAp(ref2))
    			found2 = true;
    		if (id.sameFragAndAp(ref3))
    			found3 = true;
    		if (id.sameFragAndAp(ref4))
    			found4 = true;
    	}
    	
    	assertTrue(found1, "Missing first AP ID.");
    	assertTrue(found2, "Missing second AP ID.");
    	assertTrue(found3, "Missing third AP ID.");
    	assertTrue(found4, "Missing fourth AP ID.");
    	
    	FragmentSpace.clearAll();
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetFragmentsCompatibleWithTheseAPs()
    {
        assertTrue(FragmentSpace.isDefined(),"FragmentSpace is defined");
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	
    	ArrayList<DENOPTIMVertex> lst = 
    			FragmentSpace.getFragmentsCompatibleWithTheseAPs(srcAPs);
    	
    	assertEquals(3,lst.size(),"Wrong number of compatible fragments.");
    	
    	FragmentSpace.clearAll();	
    }

//------------------------------------------------------------------------------

    /**
     * Check that the following graph's fused ring gets added to the fragment
     * space. Dots are chords
     *   ↑         ↑
     * ← C1 - C2 - C3 →
     *   .  / |    ↓
     *   C4 . C5 →
     *   ↓    ↓
     */
    @Test
    public void testFusedRingAddedToFragmentLibrary() {
        try {
            TestCase testCase = getTestCase();

            ArrayList<DENOPTIMVertex> fragLib = FragmentSpace.getFragmentLibrary();
            assertEquals(0, fragLib.size());

            FragmentSpace.addFusedRingsToFragmentLibrary(testCase.graph);

            assertEquals(1, fragLib.size());
            DENOPTIMVertex actual = fragLib.get(0);
            assertTrue(testCase.expected.sameAs(actual, new StringBuilder()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    private TestCase getTestCase() throws DENOPTIMException {
        List<DENOPTIMVertex> carbons = IntStream
                .range(0, 4)
                .mapToObj(i -> getCarbonVertex())
                .collect(Collectors.toList());

        DENOPTIMGraph g = new DENOPTIMGraph();
        DENOPTIMVertex c1 = carbons.get(0), c2 = carbons.get(1),
                c4 = carbons.get(2), c5 = carbons.get(3);
        DENOPTIMVertex c3 = getCarbonVertex();

        g.addVertex(c1);
        g.appendVertexOnAP(c1.getAP(0), c2.getAP(0));
        g.appendVertexOnAP(c2.getAP(1), c3.getAP(0));
        g.appendVertexOnAP(c2.getAP(2), c5.getAP(0));
        g.appendVertexOnAP(c2.getAP(3), c4.getAP(0));

        DENOPTIMRing r124 = new DENOPTIMRing(Arrays.asList(c1, c2, c4));
        DENOPTIMRing r425 = new DENOPTIMRing(Arrays.asList(c4, c2, c5));
        g.addRing(r124);
        g.addRing(r425);

        g.renumberGraphVertices();

        DENOPTIMTemplate t = getExpectedTemplate(g, c3);

        return new TestCase(g, t);
    }

//------------------------------------------------------------------------------

    private DENOPTIMTemplate getExpectedTemplate(DENOPTIMGraph g,
                                                 DENOPTIMVertex c3) {
        DENOPTIMGraph innerGraph = g.clone();
        innerGraph.renumberGraphVertices();
        DENOPTIMVertex c3Inner = innerGraph.getVertexAtPosition(g
                .getIndexOfVertex(c3.getVertexId()));
        innerGraph.removeVertex(c3Inner);

        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.FRAGMENT);
        t.setInnerGraph(innerGraph);
        return t;
    }

//------------------------------------------------------------------------------

    private DENOPTIMFragment getCarbonVertex() {
        try {
            IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
            IAtom carbon = builder.newAtom();
            carbon.setSymbol("C");
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(carbon);
            DENOPTIMFragment v = new DENOPTIMFragment(-1, mol,
                    BBType.FRAGMENT, true);
            for (int i = 0; i < 4; i++) {
                v.addAP(0, APC1, getRandomVector(), 1);
            }
            FragmentSpace.getBondOrderMap().put(APC1.getRule(),
                    DENOPTIMEdge.BondType.SINGLE);
            return v;
        } catch (DENOPTIMException e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
        return null;
    }

//------------------------------------------------------------------------------

    private Point3d getRandomVector() {
        int precision = 10 * 10 * 10 * 10;

        Supplier<Double> randomCoord = () ->
                (double) (Math.round(rng.nextDouble() * (double) precision)) /
                        ((double) precision);

        return new Point3d(randomCoord.get(), randomCoord.get(),
                randomCoord.get());
    }

//------------------------------------------------------------------------------

    private static final class TestCase {
        final DENOPTIMGraph graph;
        final DENOPTIMTemplate expected;

        TestCase(DENOPTIMGraph g, DENOPTIMTemplate expected) {
            this.graph = g;
            this.expected = expected;
        }
    }

//------------------------------------------------------------------------------
}
