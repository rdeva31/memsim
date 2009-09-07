/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim;

/**
 *
 * @author sound
 */
public class Bound {
    private int high, low;

    /**
     * @param low the lower bound
     * @param high the higher bound
     */
    public Bound(int low, int high) throws IllegalArgumentException
    {
        if (low > high)
            throw new IllegalArgumentException("high cannot be less than low");
        this.low = low;
        this.high = high;
    }
    public int getLowerBound()
    {
        return low;
    }

    public int getHigherBound()
    {
        return high;
    }

}
