/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim.exceptions;

/**
 *
 * @author sound
 */
public class UnimplementedInstructionException extends Exception {
    
        public UnimplementedInstructionException()
    {
        super();
    }

    public UnimplementedInstructionException(String msg)
    {
        super(msg);
    }

    public UnimplementedInstructionException(Throwable cause)
    {
        super(cause);
    }

    public UnimplementedInstructionException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

}
