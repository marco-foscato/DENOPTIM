package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.graph.AttachmentPoint;
import denoptim.graph.FragIsomorphEdge;
import denoptim.graph.FragIsomorphNode;
import denoptim.graph.Fragment;

/**
 * Represents a fragment that can by clustered based on the 3*N coordinate of
 * atoms and attachment points, i.e.,  N is the sum of atoms and attachment points.
 */
public class ClusterableFragment implements Clusterable
{
    /**
     * Reference to original fragment
     */
    private Fragment frag;
    
    /**
     * Ordered list of nodes determined by isomorphism.
     */
    List<FragIsomorphNode> orderedNodes;
    //TODO-gg make private?
    
    /**
     * Ordered list of coordinated reflecting the ordered list of atoms/APs.
     */
    double[] allCoords;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor. Does not set the ordering of the atoms/APs. 
     * For that use {@link #setOrderOfNodes(Collection)}.
     * @param frag the fragment to wrap.
     */
    public ClusterableFragment(Fragment frag)
    {
        this.frag = frag;
    }

//------------------------------------------------------------------------------
    
    /**
     * Sets the order of nodes (i.e., atoms/APs) to use for geometric comparison
     * with other fragments, and update the vector of coordinates accordingly.
     * @param c the new order.
     */
    public void setOrderOfNodes(Collection<FragIsomorphNode> c)
    {
        orderedNodes = new ArrayList<FragIsomorphNode>(c);
        
        allCoords = new double[orderedNodes.size()*3];
        int i=-1;
        for (FragIsomorphNode node : orderedNodes)
        {
            i++;
            allCoords[i] = node.getPoint3d().x;
            i++;
            allCoords[i] = node.getPoint3d().y;
            i++;
            allCoords[i] = node.getPoint3d().z;
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Define the node order from the list of atoms and attachment points.
     * This method is useful to bypass the finding of a consistent node order 
     * via detection of the graph isomorphism.
     */
    public void setNaturalNodeOrder()
    {
        List<FragIsomorphNode> naturalOrder = new ArrayList<FragIsomorphNode>();
        Set<FragIsomorphNode> nodeSet = 
                frag.getJGraphFragIsomorphism().vertexSet();
        for (IAtom atm : frag.atoms())
        {
            for (FragIsomorphNode n : nodeSet)
            {
                if (n.getOriginal() == atm)
                {
                    naturalOrder.add(n);
                    break;
                }
            }
        }
        for (AttachmentPoint ap : frag.getAttachmentPoints())
        {
            for (FragIsomorphNode n : nodeSet)
            {
                if (n.getOriginal() == ap)
                {
                    naturalOrder.add(n);
                    break;
                }
            }
        }
        setOrderOfNodes(naturalOrder);
    }
    
//------------------------------------------------------------------------------
    
    protected void setCoordsVector(double[] coords)
    {
        this.allCoords = coords;
    }
    
//------------------------------------------------------------------------------
    
    protected void setCoordsVector(Point3d[] ptsJ)
    {
        this.allCoords = convertToCoordsVector(ptsJ);
    }
    
//------------------------------------------------------------------------------
    
    protected Fragment getOriginalFragment()
    {
        return frag;
    }

//------------------------------------------------------------------------------
    
    protected DefaultUndirectedGraph<FragIsomorphNode, FragIsomorphEdge> 
        getJGraphFragIsomorphism()
    {
        return frag.getJGraphFragIsomorphism();
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the vector of 3*N coordinates, if the ordering has been set
     *  by {@link #setOrderOfNodes(Collection)} if the vector of coordinates
     *  has been set by {@link #setCoordsVector(double[])}, 
     * or null if neither has happened.
     */
    @Override
    public double[] getPoint()
    {       
        return allCoords;
    }

//------------------------------------------------------------------------------
    
    public static double[] convertToCoordsVector(Point3d[] pts)
    {
        double[] coords = new double[pts.length*3];
        for (int j=0; j<pts.length; j++)
        {
            int i = j*3;
            coords[i] = pts[j].x;
            coords[i+1] = pts[j].y;
            coords[i+2] = pts[j].z;
        }
        return coords;
    }
    
//------------------------------------------------------------------------------
    
    public static Point3d[] convertToPointArray(double[] coords)
    {
        Point3d[] pts = new Point3d[coords.length/3];
        for (int j=0; j<coords.length/3; j++)
        {
            int i = j*3;
            pts[j] = new Point3d(coords[i], coords[i+1], coords[i+2]);
        }
        return pts;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a shallow copy where the vector of coordinates has a new 
     * reference. Thus, changes to the clone's coordinates vector do not affect 
     * the original.
     */
    @Override
    public ClusterableFragment clone()
    {
        ClusterableFragment clone = new ClusterableFragment(this.frag);
        clone.setOrderOfNodes(orderedNodes);
        return clone;
    }
    
//------------------------------------------------------------------------------

    public Fragment getTransformedCopy()
    {
        Fragment copy = frag.clone();
        
        Point3d[] newCoords = ClusterableFragment.convertToPointArray(allCoords);
        int j=0;
        for (FragIsomorphNode node : this.orderedNodes)
        {
            if (node.isAtm())
            {
                int atmId = frag.indexOf((IAtom) node.getOriginal());
                copy.getAtom(atmId).setPoint3d(newCoords[j]);
            } else {
                int apId = frag.getIndexOfAP((AttachmentPoint) node.getOriginal());
                copy.getAP(apId).setDirectionVector(newCoords[j]);
            }
            j++;
        }
        return copy;
    }

//------------------------------------------------------------------------------
    
}
