/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim.exceptions;

/**
 *
 * @author sound
 */
public class MemoryAccessException extends Exception {

    public MemoryAccessException()
    {
        super();
    }

    public MemoryAccessException(String msg)
    {
        super(msg);
    }

    public MemoryAccessException(Throwable cause)
    {
        super(cause);
    }

    public MemoryAccessException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

}
