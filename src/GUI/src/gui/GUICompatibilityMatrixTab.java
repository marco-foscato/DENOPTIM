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

package gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;


/**
 * A panel for handling of compatibility matrix.
 * 
 * @author Marco Foscato
 */

public class GUICompatibilityMatrixTab extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * Unique identified for instances of this inspector
	 */
	public static AtomicInteger CPMapTabUID = new AtomicInteger(1);

	/**
	 * Flag signaling that loaded data has changes since last save
	 */
	private boolean unsavedChanges = false;

	private JButton btnLoadCPMap;
	private JButton btnImportAPClasses;

	private CompatibilityMatrixForm cpMapHandler;
	

	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUICompatibilityMatrixTab(GUIMainPanel mainPanel)
	{
		super(mainPanel, "Compatibility Matrix #" + CPMapTabUID.getAndIncrement());
		super.setLayout(new BorderLayout());
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel and add buttons.
	 */
	private void initialize() {
		
		// BorderLayout is needed to allow dynamic resizing!
		this.setLayout(new BorderLayout()); 
		
		// This card structure includes center and south panels:
		// - (Center) the CPMap handler
		// - (South) general controls (load, save, close)

		cpMapHandler = new CompatibilityMatrixForm();
		this.add(cpMapHandler, BorderLayout.CENTER);
		
		// Panel with buttons to the bottom of the frame
		
		JPanel commandsPane = new JPanel();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		
		btnLoadCPMap = new JButton("Load Compatibility Matrix",
					UIManager.getIcon("FileView.directoryIcon"));
		btnLoadCPMap.setToolTipText(String.format("<html><body width='%1s'>"
				+ "Reads from file all compatibility matrix data including: "
				+ "<ul><li>APClass compatibility rules</li>"
				+ "<li>APClass-to-Bond order rules</li>"
				+ "<li>Capping rules</li>"
				+ "<li>Forbidden ends definitions.</li></ul></html>",250));
		btnLoadCPMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = DenoptimGUIFileOpener.pickFile();
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				cpMapHandler.importCPMapFromFile(inFile);
			}
		});
		commandsPane.add(btnLoadCPMap);
		
		btnImportAPClasses = new JButton("Import APClasses");
		btnImportAPClasses.setToolTipText(String.format("<html>"
				+ "<body width='%1s'>Appends all APClasses to the list of "
				+ "all classes in the current tab.</html>",300));
		btnImportAPClasses.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Set<File> files = DenoptimGUIFileOpener.pickManyFile();
				cpMapHandler.importAllAPClassesFromFragmentLibs(files, false);
			}
		});
		commandsPane.add(btnImportAPClasses);
		
		
		JButton btnSaveFrags = new JButton("Save Compatibility Matrix",
				UIManager.getIcon("FileView.hardDriveIcon"));
		btnSaveFrags.setToolTipText(String.format("<html><body width='%1s'>"
				+ "Writes to file all compatibility matrix data including: "
				+ "<ul><li>APClass compatibility rules</li>"
				+ "<li>APClass-to-Bond order rules</li>"
				+ "<li>Capping rules</li>"
				+ "<li>Forbidden ends definitions.</li></ul></html>",250));
		btnSaveFrags.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outFile = DenoptimGUIFileOpener.saveFile();
				if (outFile == null || cpMapHandler == null)
				{
					return;
				}
				cpMapHandler.writeCopatibilityMatrixFile(outFile);
				unsavedChanges = false;
			}
		});
		commandsPane.add(btnSaveFrags);

		JButton btnCanc = new JButton("Close Tab");
		btnCanc.setToolTipText("Closes this compatibility matrix editor.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("<html>Hover over the buttons and fields "
                    + "to get a tip.</html>");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'>"
						+ "<p>The Compatibility Matrix tab allows to create, "
						+ "inspect, and edit "
						+ "compatibility matrix data, which includes: "
						+ "<ul>"
						+ "<li>the actual compatibility matrix,</li>"
						+ "<li>the APClass-to-bond order map,</li>"
						+ "<li>the APClass-capping rules,</li>"
						+ "<li>the list of APClasses that cannot be left "
						+ "unused (i.e., the forbidden ends)</li>"
						+ "</ul></p></html>";
				JOptionPane.showMessageDialog(null, 
						String.format(txt, 400),
	                    "Tips",
	                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
	}
	
//-----------------------------------------------------------------------------
	
	public void importCPMapFromFile(File inFile)
	{
		cpMapHandler.importCPMapFromFile(inFile);
	}

//-----------------------------------------------------------------------------

	/**
	 * Check whether there are unsaved changes.
	 * @return <code>true</code> if there are unsaved changes.
	 */
	
	public boolean hasUnsavedChanges()
	{
		return unsavedChanges;
	}
		
//-----------------------------------------------------------------------------
  	
}
