
package memsim;

/**
 *
 * @author sound
 */
public class ProcessorFactory {

    public enum MemoryArchitecture {HARVARD, VON_NEUMANN};

    private ProcessorFactory()
    {
    }

    public static Processor createProcessor(MemoryArchitecture m) throws IllegalArgumentException
    {
        switch(m)
        {
            case HARVARD:
                return new HarvardProcessor();
            case VON_NEUMANN:
                return new VonNeumannProcessor();
            default:
                throw new IllegalArgumentException("type of memory architecture not recognised");
        }
    }
}
