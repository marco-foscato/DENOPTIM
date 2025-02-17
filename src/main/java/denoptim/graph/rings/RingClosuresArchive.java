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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;


/**
 * Data structure to store and handle information about sub-structures 
 * (i.e., chains of fragments) and ring closure capability.
 *
 * @author Marco Foscato
 */

public class RingClosuresArchive
{
    /**
     * Index of the next entry to be added to the archive
     */
    private int nextRccId = 0;

    /**
     * Data structure containing the main information about the
     * <code>RingClosingConformation</code>s (values) per each 
     * <code>ClosableChain</code> (keys). Each value identifies
     * (i) a unique Id that permits to retrieve the list ring-closing 
     * conformations from the archive of serialized objects, and 
     * (ii) the closability of the chain.
     */
    private HashMap<String,ArrayList<String>> rccsPerChainId =
							     new HashMap<>();

    /**
     * Data structure containing the library of 
     * <code>ClosableChain</code>s per each fragment that can act as 
     * a turning point fragment (TP; i.e., the
     * fragments involved in the ring that has lower level in the
     * <code>DENOPTIMGraph</code>). The possible TPs are  
     * identified by the molecular fragment Id in the proper library of
     * fragments.
     */
    private HashMap<Integer,ArrayList<ClosableChain>> libCCxTPIdx =
                                                             new HashMap<>();
    
    /**
     * Parameters
     */
    private RingClosureParameters settings;

  //----------------------------------------------------------------------------

    /**
     * Construct an empty archive.
     */
    public RingClosuresArchive(){}
    
//----------------------------------------------------------------------------

    /**
     * Construct the library of ring closing substructures from an
     * existing index file.
     * @param rccIndex the index file.
     */

    public RingClosuresArchive(RingClosureParameters settings) throws DENOPTIMException
    {
        if (FileUtils.checkExists(settings.getRCCLibraryIndexFile()))
        {
            readLibraryOfRCCs(settings.getRCCLibraryIndexFile());
        }
    }

//----------------------------------------------------------------------------

    private void readLibraryOfRCCs(String filename) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line = null;
        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                String chainIdStr = parts[0];
                String rccIdNum = parts[1];
                String closability = parts[2];

                if (Integer.parseInt(rccIdNum) != nextRccId)
                {
                    String msg = "Expecting RCC Ids to be ordered (i.e., "
                                 + "1,2,3,...,n), but inconsistent number "
                                 + "is found in " + filename + " - Found" 
                                 + rccIdNum + " instead of " + nextRccId;
                    throw new DENOPTIMException(msg);
                }

                if (rccsPerChainId.containsKey(chainIdStr))
                {
                    String msg = "Found duplicate in library of RCCs";
                        throw new DENOPTIMException(msg);
                }

                addRecord(chainIdStr,rccIdNum,closability);
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (rccsPerChainId.isEmpty())
        {
            String err = "No entry taken from RCC index file: ";
            throw new DENOPTIMException(err + " " + filename);
        }
    }

//----------------------------------------------------------------------------

    private void addRecord(String chainId, String rccIdNum,
            String closability) throws DENOPTIMException
    {
        // Create record for RCC
        ArrayList<String> record = new ArrayList<String>();
        record.add(rccIdNum);
        record.add(closability);

        // Store (closability,rccIndex)-x-chainID
        rccsPerChainId.put(chainId,record);

        // Store (closable chain)-x-vertexMolId
        if (closability.equals("T"))
        {
//TODO per each fragment with more than 3 APs store the link to a ClosableChain
// not a new one. Might need to store the number of APs as a property of
// the ChainLinks, that is, change to format of the chainId string to include also that info
            ClosableChain cc = new ClosableChain(chainId);
            int tpId = cc.getTurningPointIdx();
            if (libCCxTPIdx.containsKey(tpId))
            {
                libCCxTPIdx.get(tpId).add(cc);
            }
            else
            {
                ArrayList<ClosableChain> lstCC = new ArrayList<ClosableChain>();
                lstCC.add(cc);
                libCCxTPIdx.put(tpId,lstCC);
            }
        }

        // Increment counter
        nextRccId++;
    }

//----------------------------------------------------------------------------

    /**
     * Append a new closable chain entry to the archive
     * @param chainId the string representing the chain of fragments
     * @param closable <code>true</code> for verified closable chains
     * @param rcc the ensemble of <code>RingClosingConformations</code> of
     * the chain
     */

    public void storeEntry(String chainId, boolean closable,
            RingClosingConformations rcc) throws DENOPTIMException
    {
    	String closability = "F";
    	if (closable)
    	{
    	    closability = "T";
    	}

        File file = new File(settings.getRCCLibraryIndexFile());
        long fileLength = file.length();
        RandomAccessFile rafile = null;
        FileChannel channel = null;
        FileLock lock = null;
        try
        {
            // Lock the index file
            rafile = new RandomAccessFile(file, "rw");
            channel = rafile.getChannel();

            int nTry = 0;
            while (true)
            {
                try
                {
                    lock = channel.tryLock();
                    if (lock != null)
                        break;
                }
                catch (OverlappingFileLockException e)
                {
                    nTry++;
                    settings.getLogger().log(Level.WARNING,"Attempt " + nTry
                             + " to get lock " + "for '"
                             + settings.getRCCLibraryIndexFile()
                             + "' failed. ");
                }
            }
        
            if (rccsPerChainId.containsKey(chainId))
            {
                throw new DENOPTIMException("Found duplicate in library of "
                            + " RingClosingConformations");
            }

            // Add record in maps (objects kept in memory)
            String rccId = Integer.toString(nextRccId);
            addRecord(chainId,rccId,closability);

            // Update RCC index file
            if (closable)
            {
                rafile.seek(fileLength);
                rafile.writeBytes(chainId + " " + rccId + " " + "T" +"\n");
                channel.force(true);
        		if (settings.serializeRCCs())
        		{
                    String rccFileName = settings.getRCCLibraryFolder()
                            + "/" + rccId + ".ser";
                    FileOutputStream fos = null;
                    ObjectOutputStream oos = null;
                    try
                    {
                        fos = new FileOutputStream(rccFileName);
                        oos = new ObjectOutputStream(fos);
                        oos.writeObject(rcc);
                        oos.close();
                        settings.getLogger().log(Level.FINE,
                                "Serialization to file " + rccFileName);
                    }
                    catch (Throwable t2)
                    {
                        throw new DENOPTIMException(t2);
                    }
                    finally
                    {
                        fos.flush();
                        fos.close();
                        fos = null;
                    }
        		}
            }
            else 
            {
                rafile.seek(fileLength);
                rafile.writeBytes(chainId + " " + rccId + " " + "F" +"\n");
                channel.force(true);
            }
        }
        catch (Throwable t)
        {
             throw new DENOPTIMException("Exception while trying to store " 
                     + chainId,t);
        }
        finally
        {
            try
            {
                if (channel != null)
                    channel.close();
                if (rafile != null)
                    rafile.close();
                if (lock != null && lock.isValid())
                    lock.release();
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException("RingClosuresArchive is unable "
                        + "to unlock file '" 
            			+ settings.getRCCLibraryIndexFile() 
            			+ "'. " + t);
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the library of closable chains having the given turning point
     * (i.e., the
     * fragments involved in the ring that  has lower level).
     * @param tpId the turning point molecule Id in the libraries.
     */

    public ArrayList<ClosableChain> getCCFromTurningPointId(int tpId)
    {
        if (libCCxTPIdx.containsKey(tpId))
        {
            return libCCxTPIdx.get(tpId);
        }
        return new ArrayList<ClosableChain>();
    }

//----------------------------------------------------------------------------

    /**
     * @param chain the candidate chain
     * @return the already visited chainId that correspond to the same
     * chain of fragments of <code>chain</code>, otherwise an empty string
     * is returned.
     */

    public String containsChain(PathSubGraph chain)
    {
    	String result = "";
    	for (String altChId : chain.getAllAlternativeChainIDs())
        {
    	    if (rccsPerChainId.containsKey(altChId))
    	    {
    	        result = altChId;
    		break;	    
    	    }
    	}
    	return result;
    }

//----------------------------------------------------------------------------

    /**
     * @return the closability of a chain
     */

    public boolean getClosabilityOfChain(String chainId)
    {
        ArrayList<String> rccRecord = rccsPerChainId.get(chainId);
        String closability = rccRecord.get(1);
        return closability.equals("T");
    }

//----------------------------------------------------------------------------

    /**
     * @return the <code>RingClosingConformations</code> of a chain as 
     * stored in the archive 
     */

    public RingClosingConformations getRCCsOfChain(String chainId)
                                                     throws DENOPTIMException
    {
        ArrayList<String> rccRecord = rccsPerChainId.get(chainId);
        int rccId = Integer.parseInt(rccRecord.get(0));
        String closability = rccRecord.get(1);

        RingClosingConformations rcc = new RingClosingConformations();
        if (closability.equals("T"))
        {
            rcc = getRCCsFromArchive(rccId);
            if (settings.getVerbosity() > 1)
            {
                settings.getLogger().log(Level.FINE, 
                        "Path is closable (from DB)");
            }
        }
        else
        {
            if (settings.getVerbosity() > 1)
            {
                settings.getLogger().log(Level.FINE, 
                        "Path is NOT closable (from DB)");
            }
        }
        return rcc;
    }

//----------------------------------------------------------------------------

    /**
     * Get serialized <code>RingClosingConformations</code> from archive
     * @param rccId the index identifying the 
     * <code>RingClosingConformations</code> in the archive 
     */

    public RingClosingConformations getRCCsFromArchive(int rccId)
                                                     throws DENOPTIMException
    {
        RingClosingConformations rcc = new RingClosingConformations();
        
        String rccFileName1 = settings.getRCCLibraryFolder() + 
                System.getProperty("file.separator") + rccId + ".ser";
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        boolean recoveringDone = false;
        try
        {
            fis = new FileInputStream(rccFileName1);
            ois = new ObjectInputStream(fis);
            rcc = (RingClosingConformations) ois.readObject();
            ois.close();
            settings.getLogger().log(Level.FINE, 
                    "Got serialized RCC from " + rccFileName1);
            recoveringDone = true;
        }
        catch (Throwable t2)
        {
            throw new DENOPTIMException(t2);
        }
        finally
        {
            try
            {
                fis.close();
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException(t);
            }
        }
        if (!recoveringDone)
        {
            String s = "Failed attempt to recover RCC. Check code.";
            throw new DENOPTIMException(s);
        }
        return rcc;
    }

//----------------------------------------------------------------------------
}
