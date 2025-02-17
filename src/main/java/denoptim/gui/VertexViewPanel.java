/*
 *   DENOPTIM
 *   Copyright (C) 2020 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.gui;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Template;
import denoptim.graph.Vertex;


/**
 * A panel for visualizing vertices. This is a deck of cards that brings up a
 * specific card depending on the type of vertex to visualize.
 * 
 * @author Marco Foscato
 */

public class VertexViewPanel extends JPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The currently loaded vertex
	 */
	private Vertex vertex;
	
	/**
	 * Flag signalling that data about APs has been changed in the GUI
	 */
	public boolean alteredAPData = false;
	
	private JPanel titlePanel;
	
    private JLabel labTitle;
    private JButton btnSwitchToNodeViewer;
    private JButton btnSwitchToMolViewer;
    private JButton btnSwitchTo2DViewer;
    
    private JPanel centralPanel;
	
    private JPanel emptyViewerCard;
	private FragmentViewPanel fragViewer;
	private VertexAsGraphViewPanel graphNodeViewer;
	private VertexAsTwoDimStructureViewPanel twoDimViewer;
	private IVertexAPSelection activeViewer;
    protected final String EMPTYCARDNAME = "emptyCard";
    protected final String GRAPHVIEWERCARDNAME = "emptyVertesCard";
    protected final String MOLVIEWERCARDNAME = "fragViewerCard";
    protected final String TWODVIEWERCARDNAME = "twoDimViewerCard";
	    
	private boolean editableAPTable = false;
	
	/**
	 * Flag enabling/disabling the capability to switch between mol- and 
	 * graph-based viewer
	 */
	private boolean switchbleByVertexType = true;
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 */
	public VertexViewPanel(boolean editableTable)
	{
	    super(new BorderLayout());
		this.editableAPTable = editableTable;
		initialize();
	}
	
//-----------------------------------------------------------------------------

	private void initialize()
	{
	    centralPanel = new JPanel(new CardLayout());
	    this.add(centralPanel, BorderLayout.CENTER);
	    
	    titlePanel = new JPanel();
	            
        labTitle = new JLabel("");
        titlePanel.add(labTitle);
        
        btnSwitchToNodeViewer = new JButton("Node View");
        btnSwitchToNodeViewer.setToolTipText("Switch to graph node depiction "
                + "of this vertex.");
        btnSwitchToNodeViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchToGraphNodeViewer();
            }
        });
        btnSwitchToNodeViewer.setEnabled(false);
        titlePanel.add(btnSwitchToNodeViewer);
        
        btnSwitchToMolViewer = new JButton("3D Molecule View");
        btnSwitchToMolViewer.setToolTipText("Switch to 3D molecular depiction "
                + "of this vertex.");
        btnSwitchToMolViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchToMolecularViewer();
            }
        });
        btnSwitchToMolViewer.setEnabled(false);
        titlePanel.add(btnSwitchToMolViewer);
        
        btnSwitchTo2DViewer = new JButton("2D Molecular Structure");
        btnSwitchTo2DViewer.setToolTipText("Switch to 2D molecular depiction "
                + "of this vertex.");
        btnSwitchTo2DViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchTo2DViewer();
            }
        });
        btnSwitchTo2DViewer.setEnabled(false);
        titlePanel.add(btnSwitchTo2DViewer);
        
        this.add(titlePanel, BorderLayout.NORTH);
        
        emptyViewerCard = new JPanel();
        emptyViewerCard.setToolTipText("Vertices are displayed here.");
        centralPanel.add(emptyViewerCard, EMPTYCARDNAME);
        
        graphNodeViewer = new VertexAsGraphViewPanel(editableAPTable,300);
        graphNodeViewer.addPropertyChangeListener(
                IVertexAPSelection.APDATACHANGEEVENT, 
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                alteredAPData = true;
                firePropertyChange(IVertexAPSelection.APDATACHANGEEVENT, false, 
                        true);          
            }
        });
        centralPanel.add(graphNodeViewer, GRAPHVIEWERCARDNAME);
            
        fragViewer = new FragmentViewPanel(editableAPTable);
        fragViewer.addPropertyChangeListener(
                IVertexAPSelection.APDATACHANGEEVENT, 
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                alteredAPData = true;
                firePropertyChange(IVertexAPSelection.APDATACHANGEEVENT, false, 
                        true);          
            }
        });
        centralPanel.add(fragViewer, MOLVIEWERCARDNAME);
        
        twoDimViewer = new VertexAsTwoDimStructureViewPanel(editableAPTable);
        twoDimViewer.addPropertyChangeListener(
                IVertexAPSelection.APDATACHANGEEVENT, 
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                alteredAPData = true;
                firePropertyChange(IVertexAPSelection.APDATACHANGEEVENT, false, 
                        true);          
            }
        });
        centralPanel.add(twoDimViewer, TWODVIEWERCARDNAME);
        
        
        switchToEmptyCard();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check for unsaved edits to the AP data
	 * @return <code>true</code> if there are unsaved edits
	 */
	public boolean hasUnsavedAPEdits()
	{
		return alteredAPData;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Overrides the flag signaling unsaved edits to saying that there are no
	 * altered data.
	 */
	public void deprotectEdits()
	{
	    fragViewer.deprotectEdits();
	    graphNodeViewer.deprotectEdits();
		alteredAPData = false;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Enable/disable switch-able view. Does not overwrite the control set upon
	 * loading a vertex 
	 */
	public void setSwitchable(boolean switchable)
	{
	    if (switchbleByVertexType)
	    {
    	    if (switchable)
    	    {
    	        btnSwitchToMolViewer.setEnabled(true);
    	        btnSwitchToNodeViewer.setEnabled(true);
                btnSwitchTo2DViewer.setEnabled(true);
    	    } else {
    	        btnSwitchToMolViewer.setEnabled(false);
    	        btnSwitchToNodeViewer.setEnabled(false);
                btnSwitchTo2DViewer.setEnabled(false);
    	    }
	    }
	}
	
//-----------------------------------------------------------------------------
	
	private void switchToEmptyCard()
	{
        btnSwitchToMolViewer.setEnabled(false);
        btnSwitchToNodeViewer.setEnabled(false);
        btnSwitchTo2DViewer.setEnabled(false);
	    ((CardLayout) centralPanel.getLayout()).show(centralPanel, 
	            EMPTYCARDNAME);
	    activeViewer = null;
	}
	
//-----------------------------------------------------------------------------
	
	private void switchToGraphNodeViewer()
	{
	    ((CardLayout) centralPanel.getLayout()).show(centralPanel, 
	            GRAPHVIEWERCARDNAME);
	    activeViewer = graphNodeViewer;
	}
	
//-----------------------------------------------------------------------------
    
    private void switchToMolecularViewer()
    {
        ((CardLayout) centralPanel.getLayout()).show(centralPanel, 
                MOLVIEWERCARDNAME);
        activeViewer = fragViewer;
    }
    
//-----------------------------------------------------------------------------
    
    private void switchTo2DViewer()
    {
        ((CardLayout) centralPanel.getLayout()).show(centralPanel, 
                TWODVIEWERCARDNAME);
        activeViewer = twoDimViewer;
    }
    
//-----------------------------------------------------------------------------
	
	/**
	 * Loads a molecule build from a smiles string. The 3D geometry is either 
	 * taken from remote CACTUS service (requires connection
	 * to the Internet) or built with CDK tools, as a fall-back. The CDK
	 * builder, however will produce a somewhat lower quality conformation than
	 * that obtained from on online generator.
	 * @param smiles the SMILES of the molecule to load
	 * @return <code>true</code> if the SMILES could be converted into a 3D 
	 * structure
	 * @throws DENOPTIMException 
	 */
	public boolean loadSMILES(String smiles)
	{	
	    switchToMolecularViewer();
	    return fragViewer.loadSMILES(smiles);
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Returns the currently loaded vertex.
	 * In case of mismatch between the system loaded into the Jmol
	 * viewer and the one in the local memory, we take that from Jmol and
	 * made it be The 'current fragment'. Previously set references to the
	 * previous 'current fragment' will make no sense anymore.
	 * @return the chemical representation of what is currently visualised.
	 * Can be empty and null.
	 */
	public Vertex getLoadedStructure()
	{
	    Vertex v = null;
	    if (vertex == null || vertex instanceof Fragment)
	    {
	        v = fragViewer.getLoadedStructure();
	        switchToMolecularViewer();
	    } else {
	        v = vertex;
	    }
	    return v;
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Loads a structure in the Jmol viewer.
	 * @param mol the structure to load
	 */
	public void loadPlainStructure(IAtomContainer mol)
	{
	    fragViewer.loadPlainStructure(mol);
	    switchToMolecularViewer();
	}

//-----------------------------------------------------------------------------
    
    /**
     * Loads the given vertex to this viewer.
     * The molecular data is loaded in the Jmol viewer,
     * and the attachment point (AP) information in the the list of APs.
     * Jmol is not aware of AP-related information, so this also launches
     * the generation of the graphical objects representing the APs.
     * @param frag the fragment to visualize
     */
    public void loadVertexToViewer(Vertex v)
    {
        vertex = v;
        if (v instanceof Fragment) {
            labTitle.setText("Fragment");
            Fragment frag = (Fragment) v;
            loadFragmentToViewer(frag);
        } else if (v instanceof EmptyVertex) {
            labTitle.setText("EmptyVertex");
            EmptyVertex ev = (EmptyVertex) v;
            loadEmptyVertexToViewer(ev);
        } else if (v instanceof Template) {
            labTitle.setText("Template");
            Template tmpl = (Template) v;
            loadTemplateToViewer(tmpl);
        } else {
            System.err.println("Loading empty card as a result of vertex with " 
                    + "type " + v.getClass().getName());
            switchToEmptyCard();
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Loads the given empty vertex to this viewer. This type of vertex does
     * not have any associated molecular data, but does have attachment points
     * (APs) that are listed in table of APs.
     * @param ev the vertex to visualize
     */
    private void loadEmptyVertexToViewer(EmptyVertex ev)
    {
        btnSwitchToMolViewer.setEnabled(false);
        btnSwitchToNodeViewer.setEnabled(false);
        btnSwitchTo2DViewer.setEnabled(false);
        graphNodeViewer.loadVertexToViewer(ev);
        switchToGraphNodeViewer();
        switchbleByVertexType = false;
    }
    
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the given fragments to this viewer.
	 * The molecular data is loaded in the Jmol viewer,
	 * and the attachment point (AP) information in the the list of APs.
	 * Jmol is not aware of AP-related information, so this also launches
	 * the generation of the graphical objects representing the APs.
	 * @param frag the fragment to visualize
	 */
	private void loadFragmentToViewer(Fragment frag)
	{
        btnSwitchToMolViewer.setEnabled(true);
        btnSwitchToNodeViewer.setEnabled(true);
        btnSwitchTo2DViewer.setEnabled(true);
		fragViewer.loadFragmentToViewer(frag);
        graphNodeViewer.loadVertexToViewer(frag);
        twoDimViewer.loadVertexToViewer(frag);
        switchbleByVertexType = true;
        if (frag.is3D())
		{
            switchToMolecularViewer();
		} else {
		    switchTo2DViewer();
		}
	}

//-----------------------------------------------------------------------------
    
    /**
     * Loads the given template to this viewer.
     * @param tmpl the template to visualize
     */
    private void loadTemplateToViewer(Template tmpl)
    {       
        if (tmpl.containsAtoms())
        {
            Fragment frag;
            try
            {
                frag = new Fragment(tmpl.getVertexId(), 
                        tmpl.getIAtomContainer(), tmpl.getBuildingBlockType());

                loadFragmentToViewer(frag);
                btnSwitchToMolViewer.setEnabled(true);
                btnSwitchToNodeViewer.setEnabled(true);
                //TODO: maybe one day we'll enable looking at the 2D of the whole template
                btnSwitchTo2DViewer.setEnabled(false);
                switchbleByVertexType = true;
            } catch (DENOPTIMException e)
            {
                // We lease data in the viewer to increase speed of execution, but the
                // leftover is outdated! This is the meaning of 'true'
                fragViewer.clearAll(true);
                switchbleByVertexType = false;
            }
        } else {
            // We lease data in the viewer to increase speed of execution, but the
            // leftover is outdated! This is the meaning of 'true'
            fragViewer.clearAll(true);
            switchbleByVertexType = false;
        }
        graphNodeViewer.loadVertexToViewer(tmpl);
        switchToGraphNodeViewer();
        graphNodeViewer.setVertexSpecificEditableAPTable(false);
    }
    
//-----------------------------------------------------------------------------

	/**
	 * Removes the currently visualized molecule and AP table
	 */
	public void clearCurrentSystem()
	{
	    vertex = null;
	    graphNodeViewer.mapAPs = null;
	    graphNodeViewer.clearAPTable();
	    fragViewer.mapAPs = null;
		fragViewer.clearAPTable();
		switchToEmptyCard();
		// NB: avoid it very slow! Mol viewer gets update upon loading a new mol
		// clearMolecularViewer();
	}
	
//-----------------------------------------------------------------------------
    
    /**
     * Clears the molecular viewer.  <b>WARNING:</b> this is VERY SLOW: do not do
     * it unless you are sure you really need to clear the data. Typically,
     * if there is incoming data, you do not need to run this, as the old data 
     * will be overwritten anyway.
     * @param dataIsComing set <code>true</code> when there is incoming 
     * molecular data to visualize.
     */
    public void clearMolecularViewer(boolean dataIsComing)
    {
        fragViewer.clearMolecularViewer(dataIsComing);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Returns the map of attachment points in the currently active viewer.
     * @return the map of attachment points in the currently active viewer.
     */
    public Map<Integer,AttachmentPoint> getActiveMapAPs()
    {
        return activeViewer.getMapOfAPsInTable();
    }
    
//-----------------------------------------------------------------------------

    /**
     * @return the table model of the currently active viewer
     */
    public DefaultTableModel getAPTableModel()
    {
        return activeViewer.getAPTableModel();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Identifies which attachment points are selected in the currently active 
     * viewer.
     * @return the list of attachment points indexes
     */
    public ArrayList<Integer> getSelectedAPIDs()
    {
        return activeViewer.getSelectedAPIDs();
    }
	
//-----------------------------------------------------------------------------
    
	/**
	 * Identifies the atoms that are selected in the Jmol viewer
	 * @return the list of selected atoms
	 */
    protected ArrayList<IAtom> getAtomsSelectedFromJMol()
    {
        return fragViewer.getAtomsSelectedFromJMol();
    }
 	
//-----------------------------------------------------------------------------

	/**
	 * Allows to activate and deactivate the listener.
	 * @param var use <code>true</code> to activate the listener
	 */
    protected void activateTabEditsListener(boolean var)
    {
        fragViewer.activateTabEditsListener(var);
    }

//-----------------------------------------------------------------------------
    
    /*
     * This is needed to stop Jmol threads
     */
	protected void dispose() 
	{
		fragViewer.dispose();
	}
  	
//-----------------------------------------------------------------------------

}
