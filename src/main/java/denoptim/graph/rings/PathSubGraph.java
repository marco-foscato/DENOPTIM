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

package denoptim.graph.rings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Vertex;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.Randomizer;


/**
 * This object represents a path in a {@link DGraph}. The path 
 * involving more than one {@link Vertex} and {@link Edge}.
 *
 * @author Marco Foscato 
 */

public class PathSubGraph
{
    /**
     * The graph representation of this path. Neither vertexes nor edges
     * belong to the original graph.
     */
    private DGraph graph;

    /**
     * The string identifier of this path
     */
    private String chainID;
    private String revChainID;
    private ArrayList<String> allPossibleChainIDs = new ArrayList<String>();

    /**
     * The vertex representing the first RCA: head of the path
     */
    private Vertex vA;

    /**
     * The vertex representing the second RCA: the tail of the path
     */
    private Vertex vB;

    /**
     * The turning point in the graph: after this point the direction of 
     * edges becomes opposite than before.
     */
    private Vertex turningPointVert;

    /**
     * The list of vertices of the original graph and involved in the path.
     */
    private List<Vertex> vertPathVAVB;

    /**
     * The list of edges of the original graph and involved in the path.
     */
    private List<Edge> edgesPathVAVB;

    /**
     * The molecular representation of the fragment in the path
     */
    private IAtomContainer iacPathVAVB;

    /**
     * The list of atoms in the shortest path
     */
    private List<IAtom> atomsPathVAVB;

    /**
     * The list of bonds in the shortest path.
     * Note that the <code>IBond</code>s are those of the entire molecule
     * not of the container representing only this path.
     */
    private List<IBond> bondsPathVAVB;

    /**
     * Per each bond the pair of point used to define the torsion
     */
    private ArrayList<ArrayList<Point3d>> dihedralRefPts;

    /**
     * The flag defining whether this object has already a molecular
     * representation or not
     */
    private boolean hasMolRepr = false; 

    /**
     * The string of atoms involved (atom numbers from full molecule list)
     */
    private String atmNumStr;

    /**
     * The list of closable conformations, if any is found
     */
    private RingClosingConformations rcc;
    
    // Set to true to write useful debug data to file and log
    private boolean debug = false;

//-----------------------------------------------------------------------------

    /**
     * Constructs a new PathSubGraph specifying the first and last vertex of 
     * the path
     */

    public PathSubGraph(Vertex vA, Vertex vB, DGraph molGraph)
    {
        this.vA = vA;
        this.vB = vB;
        
        // Identify the path between vA/vB and the seed of the spanning tree
        List<Vertex> vAToSeed = new ArrayList<Vertex>();
        molGraph.getParentTree(vA, vAToSeed);
        vAToSeed.add(0, vA);
        List<Vertex> vBToSeed = new ArrayList<Vertex>();
        molGraph.getParentTree(vB, vBToSeed);
        vBToSeed.add(0, vB);
        
        if (Collections.disjoint(vAToSeed, vBToSeed))
        {
            vertPathVAVB = new ArrayList<Vertex>();
            edgesPathVAVB = new ArrayList<Edge>();
            return;
        }
        
        // find XOR plus junction vertex (turning point)
        vertPathVAVB = new ArrayList<Vertex>();
        edgesPathVAVB = new ArrayList<Edge>();
        turningPointVert = null;
        for (int i=0; i<vAToSeed.size(); i++)
        {
            // We dig from vA towards the seed of the graph
            if (vBToSeed.contains(vAToSeed.get(i)))
            {
                // We are at the turning point vertex: were the vA->seed path
                // meets the vB->seed path
                turningPointVert = vAToSeed.get(i);
                vertPathVAVB.add(vAToSeed.get(i));
                
                int idStart = vBToSeed.indexOf(vAToSeed.get(i))-1;
                
                for (int j=idStart; j>-1; j--)
                {
                    // we climb towards vB
                    vertPathVAVB.add(vBToSeed.get(j));
                    edgesPathVAVB.add(vBToSeed.get(j).getEdgeToParent());
                }
                break;
            }
            else
            {   
                vertPathVAVB.add(vAToSeed.get(i));
                edgesPathVAVB.add(vAToSeed.get(i).getEdgeToParent());
            }
        }
        
        // Build the DENOPTIMGraph and ID of this sub graph
        chainID = "";
        revChainID = "";
        int tpId = -1;
        int tpIdRev = -1;
        Vertex vertBack;
        Vertex vertHere;
        Vertex vertFrnt;
        boolean insideOut = false;
        ArrayList<Vertex> gVertices = new ArrayList<Vertex>();
        ArrayList<Edge> gEdges = new ArrayList<Edge>();
        for (int i=1; i < vertPathVAVB.size()-1; i++)
        {
            vertBack = vertPathVAVB.get(i-1);
            vertHere = vertPathVAVB.get(i);
            vertFrnt = vertPathVAVB.get(i+1);
            Edge edgeToBack = edgesPathVAVB.get(i-1);
            Edge edgeToFrnt = edgesPathVAVB.get(i);
            
            // Avoid assumption that all paths need to go via scaffold
            if (edgeToBack.getSrcAP().getOwner()==vertBack)
                insideOut = true;
            
            int apIdBack2Here = -1;
            int apIdHere2Back = -1;
            int apIdHere2Frnt = -1;
            int apIdFrnt2Here = -1;
            if (vertHere == turningPointVert)
            {
                insideOut = true;
                apIdBack2Here = edgeToBack.getTrgAPID();
                apIdHere2Back = edgeToBack.getSrcAPID();
                apIdHere2Frnt = edgeToFrnt.getSrcAPID();
                apIdFrnt2Here = edgeToFrnt.getTrgAPID();
                tpId = i-1;
                tpIdRev = vertPathVAVB.size()-i;
            }
            else
            {
                if (insideOut)
                {
                    apIdBack2Here = edgeToBack.getSrcAPID();
                    apIdHere2Back = edgeToBack.getTrgAPID();
                    apIdHere2Frnt = edgeToFrnt.getSrcAPID();
                    apIdFrnt2Here = edgeToFrnt.getTrgAPID();
                }
                else
                {
                    apIdBack2Here = edgeToBack.getTrgAPID();
                    apIdHere2Back = edgeToBack.getSrcAPID();
                    apIdHere2Frnt = edgeToFrnt.getTrgAPID();
                    apIdFrnt2Here = edgeToFrnt.getSrcAPID();
                }
            }
            
            String[] ids = vertHere.getPathIDs(vertHere.getAP(apIdHere2Back),
                    vertHere.getAP(apIdHere2Frnt));
            chainID = chainID + ids[0];
            revChainID = ids[1] + revChainID;
            
            // We must work with clones of the actual vertices/edges
            Vertex cloneVertBack = vertBack.clone();
            Vertex cloneVertHere = vertHere.clone();
            Vertex cloneVertFrnt = vertFrnt.clone();
            
            // Collect vertices and make edges to build a graph from A to B
            if (i == 1) {
                gVertices.add(cloneVertBack);
            } else if (i > 1)
            {
                // Need to collect the reference to the vertex defined in the 
                // previous cycle. 
                cloneVertBack = gVertices.get(gVertices.size()-1);
            }
            gVertices.add(cloneVertHere);
            gEdges.add(new Edge(cloneVertBack.getAP(apIdBack2Here),
                    cloneVertHere.getAP(apIdHere2Back),
                    edgeToBack.getBondType()));
            if (i == vertPathVAVB.size()-2)
            {
                gVertices.add(cloneVertFrnt);
                gEdges.add(new Edge(cloneVertHere.getAP(apIdHere2Frnt),
                        cloneVertFrnt.getAP(apIdFrnt2Here),
                        edgeToFrnt.getBondType()));
            }
        }

        // Build the DENOPTIMGraph with edges directed from VA to VB
        this.graph = new DGraph(gVertices,gEdges);

    	// prepare alternative chain IDs
    	String[] pA = chainID.split("_");
    	String[] pB = revChainID.split("_");
    	allPossibleChainIDs.add(chainID + "%" + tpId);
    	allPossibleChainIDs.add(revChainID + "%" + tpIdRev);
    	for (int i=1; i<pA.length; i++)
    	{
    	    String altrnA = "";
    	    String altrnB = "";
    	    for (int j=0; j<pA.length; j++)
    	    {
        		if ((i+j) < pA.length)
        		{
                    altrnA = altrnA + pA[i+j] + "_";
                    altrnB = altrnB + pB[i+j] + "_";
        		} else {
        		    altrnA = altrnA + pA[i+j-pA.length] + "_";
                    altrnB = altrnB + pB[i+j-pA.length] + "_";
        		}
    	    }
    	    allPossibleChainIDs.add(altrnA + "%" + tpId);
    	    allPossibleChainIDs.add(altrnB + "%" + tpIdRev);
    	}
        chainID = chainID + "%" + tpId;
        revChainID = revChainID + "%" + tpIdRev;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a path as a DENOPTIMGraph from the first argument to the
     * second one. The vertices in the path have vacant APs where they would
     * connect to another vertex in the graph that from and to belongs to.
     * The direction of the edges are the same as the graph that from and to
     * belongs to.
     *
     * An empty graph is returned if from == to.
     *
     * @param from start of path.
     * @param to end of path.
     * @throws IllegalArgumentException if from cannot reach to.
     */
    public static DGraph findPath(Vertex from, Vertex to) {
        DGraph g = new DGraph();
        try {
            if (from == to) {
                return g;
            }

            Iterator<AttachmentPoint> path = findPath(from, to,
                    new HashSet<>()).iterator();

            if (!path.hasNext()) {
                return g;
            }

            AttachmentPoint srcAP = path.next().clone();
            Vertex srcVertex = srcAP.getOwner().clone();
            srcAP.setOwner(srcVertex);

            g.addVertex(srcVertex);

            AttachmentPoint trgAP = path.next().clone();
            Vertex trgVertex = trgAP.getOwner().clone();
            trgAP.setOwner(trgVertex);

            g.appendVertexOnAP(srcAP, trgAP);

            while (path.hasNext()) {
                srcAP = path.next().clone();
                srcVertex = srcAP.getOwner().clone();
                srcAP.setOwner(srcVertex);

                trgAP = path.next().clone();
                trgVertex = trgAP.getOwner().clone();
                trgAP.setOwner(trgVertex);

                g.appendVertexOnAP(srcAP, trgAP);
            }
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
        return g;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a sequence of APs that is the path from vertex from to
     * vertex to. An empty sequence is returned if from cannot reach to. This
     * usually happens if from and to are not part of the same graph.
     * @param from start of path
     * @param to end of path
     * @param visited vertices already visited.
     * @return the path of APs from vertex from to vertex to.
     */
    private static Iterable<AttachmentPoint> findPath(
            Vertex from, Vertex to, Set<Long> visited) {

        long fromId = from.getVertexId();
        if (visited.contains(fromId)) {
            return new ArrayList<>();
        }
        visited.add(fromId);

        for (AttachmentPoint fromAP : from.getAttachmentPoints()) {
            Edge e = fromAP.getEdgeUser();
            AttachmentPoint adjAP = e.getSrcVertex() == fromId ?
                    e.getTrgAP() : e.getSrcAP();
            Vertex adj = adjAP.getOwner();

            if (adj == to) {
                return Arrays.asList(fromAP, adjAP);
            }

            Iterable<AttachmentPoint> path = findPath(adj, to, visited);
            // Non-empty if there exists a path
            if (path.iterator().hasNext()) {
                List<AttachmentPoint> extendedPath =
                        new ArrayList<AttachmentPoint>(Arrays.asList(
                                fromAP, adjAP));
                path.iterator().forEachRemaining(extendedPath::add);
                return extendedPath;
            }
        }
        // Dead end
        return Collections.emptyList();
    }

//------------------------------------------------------------------------------

    /**
     * Creates the molecular representation, list of atoms and bonds involved
     * in the path between the head and tail.
     * @param mol the full molecule
     * @param make3D if <code>true</code> makes the method generate the 3D
     * coordinates of the chain of fragments by rototranslation of the
     * 3D fragments so that to align the APvectors
     * @throws DENOPTIMException
     */

    public void makeMolecularRepresentation(IAtomContainer mol, boolean make3D,
            Logger logger, Randomizer randomizer)
                                                       throws DENOPTIMException
    {
        // Build molecular representation 
        ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder(logger, randomizer);
        iacPathVAVB = tb.convertGraphTo3DAtomContainer(graph);
        Map<IAtom,ArrayList<AttachmentPoint>> apsPerAtom = 
                iacPathVAVB.getProperty(DENOPTIMConstants.MOLPROPAPxATOM);
        Map<IBond,ArrayList<AttachmentPoint>> apsPerBond =
                iacPathVAVB.getProperty(DENOPTIMConstants.MOLPROPAPxBOND);

        if (debug)
        {
    	    String f = "/tmp/pathSubGraph.sdf";
    	    System.out.println("Find SDF representation of path in: " + f);
    	    DenoptimIO.writeSDFFile(f,iacPathVAVB,false);
    	    DenoptimIO.writeSDFFile(f,mol,true);
    	}

        // Get shortest atom path between the two ends of the chain
        atomsPathVAVB = new ArrayList<IAtom>();
        try {
            IAtom head = iacPathVAVB.getAtom(0);
            IAtom tail = iacPathVAVB.getAtom(iacPathVAVB.getAtomCount()-1);
            ShortestPaths sp = new ShortestPaths(iacPathVAVB, head);
            atomsPathVAVB = new ArrayList<IAtom>(Arrays.asList(sp.atomsTo(tail)));
        } catch (Throwable t) {
            throw new DENOPTIMException("PathTools Exception: " + t);
        }

        // Identify which atoms in mol represent the RCA in the current chain.
        // Since we are looking for the verteces of the RCA atoms
        // there is only one atom per each of the two vertexID required
        
        IAtom e0 = null;
        IAtom e1 = null;
        for (IAtom atm : mol.atoms())
        {
            long vrtId = (Long) atm.getProperty(
                    DENOPTIMConstants.ATMPROPVERTEXID);
            if (vrtId == vertPathVAVB.get(0).getVertexId())
                e0 = atm;
                      
            if (vrtId == vertPathVAVB.get(vertPathVAVB.size()-1).getVertexId())
                e1 = atm;
            
            if (e0 != null && e1 != null)
                break;
        }
        List<IAtom> ends = new ArrayList<IAtom>();
        ends.add(e0);
        ends.add(e1);

        // Get path in mol that corresponds to the shortest path in iacPathVAVB
        // This is done to get the bond properties from mol that is a 
        // fully defined molecule.
        // Note that multiple paths with length equal to the shortest length 
        // are possible if there are rings within fragments (vertices), 
        // but in such case the alternative paths involve only non-rotatable.
        // Therefore, the actual identity of the bond doesn't matter in
        // this particular context.
        List<IAtom> pathInFullMol = new ArrayList<IAtom>();
        try {
            IAtom currentAtm = null;
            long prevAtmInPathVID = -1;
            long currAtmInPathVID = -1;
            long nextAtmInPathVID = -1;
            List<IAtom> candidates = new ArrayList<IAtom>();
            for (int i=1; i<(atomsPathVAVB.size()); i++)
            {
                //We have already found the atoms corresponding to the extremes
                if (i==1)
                {
                    currentAtm = ends.get(0);
                    pathInFullMol.add(currentAtm);
                    candidates.addAll(mol.getConnectedAtomsList(currentAtm));
                } else if (i==(atomsPathVAVB.size()-1))
                {
                    pathInFullMol.add(ends.get(1));
                    break;
                }
                
                // Now, standard behaviour for all other non-extreme cases
                prevAtmInPathVID = getVertexIdInPath(atomsPathVAVB.get(i-1));
                currAtmInPathVID = getVertexIdInPath(atomsPathVAVB.get(i));
                nextAtmInPathVID = getVertexIdInPath(atomsPathVAVB.get(i+1));
                
                if (prevAtmInPathVID != currAtmInPathVID)
                {
                    for (IAtom c : candidates)
                    {
                        if (getVertexIdInPath(c) == currAtmInPathVID)
                        {
                            currentAtm = c;
                            pathInFullMol.add(currentAtm);
                            candidates.clear();
                            
                            for (IAtom c2 : mol.getConnectedAtomsList(c))
                            {
                                if (!pathInFullMol.contains(c2))
                                    candidates.add(c2);
                            }
                            break;
                        }
                    }
                    continue;
                } else {
                    //NB:  currentAtm remains the same
                    List<IAtom> newCandidates = new ArrayList<IAtom>(); 
                    for (IAtom nbr : candidates)
                    {
                        boolean foundNextLevel = false;
                        for (IAtom nbrNbr : mol.getConnectedAtomsList(nbr))
                        {
                            if (pathInFullMol.contains(nbrNbr)
                                    || candidates.contains(nbrNbr))
                                continue;
                            
                            long vid = getVertexIdInPath(nbrNbr);
                            if (vid == nextAtmInPathVID 
                                    && currAtmInPathVID!=nextAtmInPathVID)
                            {
                                ShortestPaths sp = new ShortestPaths(mol, 
                                        currentAtm);
                                List<IAtom> itnraVertPath = new ArrayList<IAtom>(
                                        Arrays.asList(sp.atomsTo(nbr)));

                                // currentAtm was already added: skip it
                                for (int j=1; j<itnraVertPath.size(); j++)
                                    pathInFullMol.add(itnraVertPath.get(j));
                                
                                currentAtm = nbr;
                                newCandidates.clear();
                                newCandidates.add(nbrNbr);
                                foundNextLevel = true;
                                break;
                            } else {
                                if (vid == currAtmInPathVID)
                                {
                                    newCandidates.add(nbrNbr);
                                }
                            }
                        }
                        if (foundNextLevel)
                            break;
                    }
                    candidates.clear();
                    candidates.addAll(newCandidates);
                    continue;
                } 

            }
        } catch (Throwable t) {
            throw new DENOPTIMException("PathTools Exception: " + t, t);
        }
        if (pathInFullMol.size() != atomsPathVAVB.size())
        {
            throw new DENOPTIMException("Paths have different size! Unable to "
                        + "proceed in the evaluation of ring closability. "
                        + "Please report this to the author.");
        }

        // Identify the path of bonds between head and tail
        // This is taken from the fully defined mol to inherit rotatability
        // and allow identification of bonds used in multiple rings-closing
        // chains.
        bondsPathVAVB = new ArrayList<IBond>();
        for (int i=0; i < pathInFullMol.size()-1; i++)
        {
            IBond bnd = mol.getBond(pathInFullMol.get(i), 
                                    pathInFullMol.get(i+1));
            bondsPathVAVB.add(bnd);
        }

        // Define points used to calculate dihedral angle of each bond
        // excluding the first and last
        dihedralRefPts = new ArrayList<ArrayList<Point3d>>();
        String keyPropVrtID = DENOPTIMConstants.ATMPROPVERTEXID;
        for (int it=3; it<atomsPathVAVB.size(); it++)
        {
            ArrayList<Point3d> fourPoints = new ArrayList<Point3d>();
            Point3d p0;
            Point3d p1;
            Point3d p2;
            Point3d p3;
            IAtom a0 = atomsPathVAVB.get(it-3);
            IAtom a1 = atomsPathVAVB.get(it-2);
            IAtom a2 = atomsPathVAVB.get(it-1);
            IAtom a3 = atomsPathVAVB.get(it);
            long vIdA0 = a0.getProperty(keyPropVrtID);
            long vIdA1 = a1.getProperty(keyPropVrtID);
            long vIdA2 = a2.getProperty(keyPropVrtID);
            long vIdA3 = a3.getProperty(keyPropVrtID);
            IBond bndA1A2 = iacPathVAVB.getBond(a1,a2);

            // trivial for points 1 and 2
            p0 = a0.getPoint3d(); // only initialization
            p1 = a1.getPoint3d();
            p2 = a2.getPoint3d();
            p3 = a3.getPoint3d(); // only initialization

            // chose point 0
            if (vIdA0 != vIdA1)
            {
                // use the point identified by the ap on atom a1 that 
                // has the lowest index in the list of aps on a1, 
                // but is not the AP used to bind a1 and a2
                for (AttachmentPoint ap : apsPerAtom.get(a1))
                {
                    if (apsPerBond.keySet().contains(bndA1A2))
                    {
                        if (apsPerBond.get(bndA1A2).contains(ap))
                        {
                           continue;
                        }
                    }
                    p0 = new Point3d(ap.getDirectionVector());
                    break;
                }
            }
            else
            {
                // Choose the atom connected to a1 that has the lowest index
                // in the IAtomContainer and is not a2
                List<IAtom> nbrsOfA1 = iacPathVAVB.getConnectedAtomsList(a1);
                int lowIDs = 10000000;
                for (IAtom nbrOfA1 : nbrsOfA1)
                {
                    if (nbrOfA1 == a2)
                    {
                        continue;
                    }
                    int atmID = iacPathVAVB.indexOf(nbrOfA1);
                    if (atmID < lowIDs)
                    {
                        lowIDs = atmID;
                    }
                }
                p0 = new Point3d(iacPathVAVB.getAtom(lowIDs).getPoint3d());
            }

            // choose point 3 (as done for point 0)
            if (vIdA2 != vIdA3)
            {
                // use the point identified by the ap on atom a2 that 
                // has the lowest index in the list of aps on a2, 
                // but is not the AP used to bind a1 and a1
                for (AttachmentPoint ap : apsPerAtom.get(a2))
                {
                    if (apsPerBond.keySet().contains(bndA1A2))
                    {
                        if (apsPerBond.get(bndA1A2).contains(ap))
                        {
                           continue;
                        }
                    }
                    p3 = new Point3d(ap.getDirectionVector());
                    break;
                }
            }
            else
            {
                // Choose the atom connected to a2 that has the lowest index
                // in the IAtomContainer and is not a1.
                List<IAtom> nbrsOfA2 = iacPathVAVB.getConnectedAtomsList(a2);
                int lowIDs = 10000000;
                for (IAtom nbrOfA2 : nbrsOfA2)
                {
                    if (nbrOfA2 == a1)
                    {
                        continue;
                    }
                    int atmID = iacPathVAVB.indexOf(nbrOfA2);
                    if (atmID < lowIDs)
                    {
                        lowIDs = atmID;
                    }
                }
                p3 = new Point3d(iacPathVAVB.getAtom(lowIDs).getPoint3d());
            }

            fourPoints.add(p0);
            fourPoints.add(p1);
            fourPoints.add(p2);
            fourPoints.add(p3);

            dihedralRefPts.add(fourPoints);
        }

        if (debug)
        {
            System.out.println("Points for dihedral angle definition: ");
            for (ArrayList<Point3d> fp : dihedralRefPts)
            {
                for (Point3d p : fp)
                {
                    System.out.println("  "+p);
                }
            System.out.println("  ");
            }
        }

        // set flag
        hasMolRepr = true;
    }
    
//-----------------------------------------------------------------------------
    
    private long getVertexIdInPath(IAtom a)
    {
        return a.getProperty(DENOPTIMConstants.ATMPROPVERTEXID, Long.class)
                .longValue();
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the string representation of the path
     */

    public String getChainID()
    {
        return chainID;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all the possible IDs for this chain. The alternatives differ in
     * the position of the chord.
     */

    public List<String> getAllAlternativeChainIDs()
    {
        return allPossibleChainIDs;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the vertex representing the head of the chain
     */

    public Vertex getHeadVertex()
    {
        return vA;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the vertex representing the tail of the chain
     */

    public Vertex getTailVertex()
    {
        return vB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the length of the list of edges involved in this path
     */

    public int getPathLength()
    {
        return edgesPathVAVB.size();
    }
//-----------------------------------------------------------------------------

    /**
     * Returns the list of verteces involved
     */

    public List<Vertex> getVertecesPath()
    {
        return vertPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of edges involved
     */

    public List<Edge> getEdgesPath()
    {
        return edgesPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns true if the molecular representation has been set
     */

    public boolean hasMolecularRepresentation()
    {
        return hasMolRepr;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the molecular representation 
     */
    public IAtomContainer getMolecularRepresentation()
    {
        return iacPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of atoms in the path between the head and the tail
     */

    public List<IAtom> getAtomPath()
    {
        return atomsPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the string with atom symbols and number for an easy
     * identification of the path in a molecular structure
     */

    public String getAtomRefStr()
    {
        return atmNumStr;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of bonds in the path between the head and the tail.
     * Note that the <code>IBond</code>s are those of the entire molecule
     * not of the <code>IAtomContainer</code> representing only this path.
     */

    public List<IBond> getBondPath()
    {
        return bondsPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of point to be used to define the torsion of a bond
     * uniquely (independently on the substituents present in this chain) as
     * a dihedral angle.
     */

    public ArrayList<ArrayList<Point3d>> getDihedralRefPoints()
    {
        return dihedralRefPts;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the ring closing conformations
     */

    public RingClosingConformations getRCC()
    {
        return rcc;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the ring closing conformations to this object
     */

    public void setRCC(RingClosingConformations rcc)
    {
        this.rcc = rcc;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @return a string describing this path to a human reader
     */
    
    @Override
    public String toString()
    {
       StringBuilder sb = new StringBuilder();
       boolean first = true;
       for (Vertex v : vertPathVAVB)
       {
           if (!first)
           {
               sb.append("-");
           }
           sb.append(v.getVertexId());
           first = false;
       }
       return sb.toString();
    }

    public DGraph getGraph() {
        return graph;
    }

//-----------------------------------------------------------------------------

}
