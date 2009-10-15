
package memsim;

import java.io.File;
/**
 *
 * @author rdeva
 */
public abstract class Processor implements Runnable {
    public abstract void setCores(int cores);
    public abstract int getCores();
    public abstract java.util.List<Core> getCoreList();

    /**
     * Load the contents into either program or data section of the memory.  progContent/dataContent
     * are allowed to be null.  If null, each block of memory remain the default or as previously set.
     *
     * Core index specifies, which of the cores' memory you wish to modify.
     * @param coreIndex the index of the core that you wish to modify
     * @param progContent file containing the text to be loaded into program memory.
     *                  May be null if no change is wished to be made.
     * @param dataContent file containing the data to be loaded into data memory.
     *                  May be null if no change is wished to be made.
     * @throws IndexOutOfBoundsException thrown if nonexistent index is specified
     * @throws java.io.IOException if either of the files cannot be accessed
     */
    public final void setCoreMemoryContents(int coreIndex, java.io.File progContent, java.io.File dataContent)
        throws IndexOutOfBoundsException, java.io.IOException
    {
        if (coreIndex >= getCores())
            throw new IndexOutOfBoundsException("coreIndex must be < numCores");

        if (progContent != null)
            Memory.loadIntoMemory(getCoreList().get(coreIndex).getProgramMemory(), progContent, -1);

        if (dataContent != null)
            Memory.loadIntoMemory(getCoreList().get(coreIndex).getDataMemory(), dataContent, -1);
    }
}
