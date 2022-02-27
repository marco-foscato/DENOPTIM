package denoptim.main;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import denoptim.main.Main.RunType;

public class CLIOptions extends Options
{
    /**
     * Version ID
     */
    private static final long serialVersionUID = 3L;
    
    /**
     * Option requesting the printing of the help message.
     */
    protected static Option help;
    
    /**
     * Option specifying the input file
     */
    protected static Option input;
    
    /**
     * Option requesting only the printing of the version.
     */
    protected static Option version;
    
    /**
     * Option controlling the type of run.
     */
    protected static Option run;
    
    /**
     * The only, static instance of this class
     */
    private static final CLIOptions instance = new CLIOptions();
    
//------------------------------------------------------------------------------    
    
    private CLIOptions() 
    {
        help = new Option("h","help",false, "Print help message");
        help.setRequired(false);
        this.addOption(help);
       
        input = new Option("f","file",true, "Specifies the file to "
                + "process.");
        input.setRequired(false);
        this.addOption(input);
        
        run = new Option("r","run",true, "Request a specific type of "
                + "run. Choose among " + RunType.values() + ". Unless the type "
                        + "of run is '" + RunType.GUI + "', this option must "
                        + "be coupled with '-" + input.getOpt() + " <arg>'"
                        + " to provide a file with the input "
                        + "parameters.");
        run.setRequired(false);
        this.addOption(run);
        
        version = new Option("v","version",false, "Prints the version "
                + "of denoptim.");
        version.setRequired(false);
        this.addOption(version);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the singleton instance of this class.
     * @return
     */
    public static CLIOptions getInstance()
    {
        return instance;
    }
    
//------------------------------------------------------------------------------
    
}
