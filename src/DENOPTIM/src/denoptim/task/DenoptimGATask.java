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

package denoptim.task;

import denoptim.utils.TaskUtils;
//import denoptimga.DenoptimGA;

/**
 * Task that runs a DENOPTIM experiment with the genetic algorithm. This class
 * meant to run and manage a call to the DenoptimGA Main class from within the
 * GUI.
 */

public class DenoptimGATask extends Task
{

//------------------------------------------------------------------------------
    
    public DenoptimGATask()
    {
    	super(TaskUtils.getUniqueTaskIndex());
    }

//------------------------------------------------------------------------------

    @Override
    public Object call()
    {
    	System.out.println("Running DenoptimGA task "+id+" called...");
    	
    	//TODO
    	
    	
    	System.out.println("DenoptimGA task "+id+" done!");
		return null;
    }
    
//------------------------------------------------------------------------------ 

    public static void denoptimGAMainCaller(String[] args)
    {
    	//DenoptimGA.main(args);
    }
    
//------------------------------------------------------------------------------

}
