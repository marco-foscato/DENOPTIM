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

package denoptim.programs.combinatorial;

import java.io.File;

import denoptim.combinatorial.CombinatorialExplorerByLayer;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.task.ProgramTask;


/**
 * Combinatorial exploration of the fragment space. Graphs are built stepwise
 * layer by layer. Each layer of new graphs is stored in text files and,
 * if a graph is complete (i.e., it corresponds to a finished chemical entity),
 * optionally submitted to further processing, which is controlled by external
 * bash script. 
 * </br>
 * Combinations of building blocks (i.e., used defined root graphs or scaffold 
 * fragments, proper fragments, and capping groups) are generated serially while
 * each new graph is handled by a dedicated, asynchronous task, thus 
 * parallelizing the construction, evaluation, and post-processing of each 
 * new graph.
 * </br>
 * The exploration of a fragment space generated all combination of building
 * blocks according to the definition of the fragment space. Symmetry may be
 * enforced in the fragment space 
 * (see {@link FragmentSpaceParameters}). 
 * In such case, if symmetric attachment points are found on a 
 * scaffold/fragment/graph, then the exploration is restricted
 * to such combinations respecting the constitutional symmetry of the APs.
 *
 * @author Marco Foscato
 */

public class FragSpaceExplorer  extends ProgramTask
{
    private  CombinatorialExplorerByLayer combinatorialAlgorithm = null;

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public FragSpaceExplorer(File configFile, File workDir)
    {
        super(configFile, workDir);
    }
  
//------------------------------------------------------------------------------
    
    @Override
    public void runProgram() throws Throwable
    {
        CEBLParameters settings = new CEBLParameters();
        if (workDir != null)
        {
            settings.setWorkDirectory(workDir.getAbsolutePath());
        }
        settings.readParameterFile(configFilePathName.getAbsolutePath());
        settings.checkParameters();
        settings.processParameters();
        settings.startProgramSpecificLogger(loggerIdentifier);
        settings.printParameters();
        
        combinatorialAlgorithm = new CombinatorialExplorerByLayer(settings);
        combinatorialAlgorithm.run();
    }
    
//------------------------------------------------------------------------------

    protected void handleThrowable()
    {
        if (combinatorialAlgorithm != null)
        {
            combinatorialAlgorithm.stopRun();
        }
        super.handleThrowable();
    }
      
//------------------------------------------------------------------------------  

}
