package denoptim.fitness.descriptors;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.ICountFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.IDenoptimDescriptor;


/**
 * Calculates the molecular similarity against a target compound the
 * fingerprint of which is given as parameter.
 */

public class TanimotoMolSimilarity extends AbstractMolecularDescriptor 
implements IMolecularDescriptor, IDenoptimDescriptor
{
    //private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(
    //        TanimotoMolSimilarity.class);
    private IBitFingerprint referenceFingerprint;
    private IFingerprinter fingerprinter;
    private static final String[] PARAMNAMES = new String[] {
            "referenceFingerprint","fingerprinterImplementation"};

    private static final String[] NAMES  = {"TanimotoSimilarity"};

//------------------------------------------------------------------------------
    
    /**
     * Constructor for a TanimotoMolSimilarity object
     */
    public TanimotoMolSimilarity() {}

//------------------------------------------------------------------------------
      
    /**
     * Get the specification attribute of Tanimoto molecular similarity.
     * @return the specification of this descriptor.
     */
    @Override
    public DescriptorSpecification getSpecification()
    {
        //TODO
        return new DescriptorSpecification("Denoptim source code", 
                this.getClass().getName(), "DENOPTIM project");
    }

//------------------------------------------------------------------------------
    
    /**
     * Gets the parameterNames attribute of the TanimotoMolSimilarity object.
     * @return the parameterNames value
     */
    @Override
    public String[] getParameterNames() {
        return PARAMNAMES;
    }

//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public Object getParameterType(String name)
    {
        if (name.equals(PARAMNAMES[0]))
        {
            return IBitFingerprint.class;
        } else if (name.equals(PARAMNAMES[1])) {
            return IFingerprinter.class;
        } else {
            throw new IllegalArgumentException("No parameter for name: "+name);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the parameters attribute of TanimotoMolSimilarity object.
     * The descriptor takes one parameter, i.e., the fingerprint against witch 
     * we want to calculate similarity.
     * @param params the array of parameters
     */
    @Override
    public void setParameters(Object[] params) throws CDKException
    {
        if (params.length != 2)
        {
            throw new IllegalArgumentException("TanimotoMolSimilarity only "
                    + "expects one parameter");
        }
        if (!(params[0] instanceof IBitFingerprint))
        {
            throw new IllegalArgumentException("Expected parameter of type " 
                    + getParameterType(PARAMNAMES[0]).getClass().getName());
        }
        if (!(params[1] instanceof IFingerprinter))
        {
            throw new IllegalArgumentException("Expected parameter of type " 
                    + getParameterType(PARAMNAMES[1]).getClass().getName());
        }
        referenceFingerprint = (IBitFingerprint) params[0];
        fingerprinter = (IFingerprinter) params[1];
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Object[] getParameters()
    {
        Object[] params = new Object[2];
        params[0] = referenceFingerprint;
        params[1] = fingerprinter;
        return params;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDescriptorNames()
    {
        return NAMES;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public DescriptorValue calculate(IAtomContainer mol)
    {
        DoubleResult result;
        if (referenceFingerprint==null)
        {
            throw new IllegalStateException("Reference fingerprint not set. "
                    + "Cannot calculate Tanimoto similarity.");
        }
        if (fingerprinter==null)
        {
            throw new IllegalStateException("Fingerprinter not set. "
                    + "Cannot calculate Tanimoto similarity.");
        }
        
        try
        {
            result = new DoubleResult(Tanimoto.calculate(referenceFingerprint, 
                    fingerprinter.getBitFingerprint(mol)));
        } catch (CDKException e)
        {
            result = new DoubleResult(Double.NaN);
        }
        
        return new DescriptorValue(getSpecification(),
                getParameterNames(),
                getParameters(),
                result,
                getDescriptorNames());
    }

//------------------------------------------------------------------------------
   
    /** {@inheritDoc} */
    @Override
    public IDescriptorResult getDescriptorResultType()
    {
        return new DoubleResultType();
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getDictionaryTitle()
    {
        return "Tanimoto Molecular Similarity";
    }
    
//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public String getDictionaryDefinition()
    {
        return "The Tanimoto Molecular Similarity is calculated between the "
                + "a reference fingerprint given upon definition of the "
                + "descriptor (see parameters), and a molecule given as "
                + "argument when calculating the value of the descriptor. "
                + "Fingerprint are obtained from the defined "
                + "<code>IFingerprinter</code> (see parameters) as "
                + "<code>IFingerprinter.getBitFingerprint(mol)</code>";
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDictionaryClass()
    {
        return new String[] {"molecular"};
    }

//------------------------------------------------------------------------------

}
