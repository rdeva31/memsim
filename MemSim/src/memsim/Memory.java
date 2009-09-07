/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package memsim;

/**
 *
 * @author rdeva
 */
public class Memory {
    public static final int MIN_SIZE = 128;
    private int size;
    private int[] mem;

    /**
     * Create a memory unit with given size bytes
     * @param size
     * @throws IllegalArgumentException if size is not a power of 2 and not greater than MIN_SIZE bytes
     */
    public Memory(int size) throws IllegalArgumentException
    {
        int temp = size;
        if (size < MIN_SIZE)
            throw new IllegalArgumentException("Size smaller than " + MIN_SIZE + " bytes");

        //is it a power of 2?
        while ((temp & 1) == 0)
            temp = temp >> 1;
        if ((temp >> 1) != 0)
            throw new IllegalArgumentException("size(" + size + ") not a power of 2");

        this.size = size;
        mem = new int[size>>2]; //ints are 4 bytes, so need to divide size by 4
    }

    /**
     * Return the size of the memory in bytes
     * @return
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Read 1 byte at memory address memAddr within the accessible bound b.
     * @param memAddr memory addr (logical) to be read
     * @param b the memory region with read/write permission
     * @return byte at memory location memAddr
     * @throws java.rmi.AccessException if memory addr is out of bounds of the accessible region
     */
    public int readByte(int memAddr, Bound b) throws java.rmi.AccessException
    {
        int realAddr = b.getLowerBound() + ((memAddr >> 2) << 2);
        if (realAddr > b.getHigherBound())
            throw new java.rmi.AccessException("Memory addr out of bounds");
        
        int temp = mem[realAddr >> 2];
        
        switch (memAddr & 3)
        {
            case 0:
                return temp >> 24 & 0xff;
            case 1:
                return (temp >> 16) & 0xff;
            case 2:
                return (temp >> 8) & 0xff;
            case 3:
                return temp & 0xff;
            default:    //should never happen
                throw new RuntimeException("Code shouldn't have execed");
        }
    }

    /**
     * Read the 4 byte word at given memory address.
     * @param memAddr memory address to be read (must be word aligned)
     * @param b the memory region with read/write permission
     * @return
     * @throws IllegalArgumentException if the address isn't word aligned
     * @throws java.rmi.AccessException if memory addr is out of bounds of the accessible region
     */
    public int readWord(int memAddr, Bound b) throws IllegalArgumentException, java.rmi.AccessException
    {
        int realAddr = b.getLowerBound() + memAddr;
        
        if ((memAddr & 3) != 0)
            throw new IllegalArgumentException("address isn't word aligned");
        else if (realAddr > b.getHigherBound())
            throw new java.rmi.AccessException("Memory addr out of bounds");

        return mem[realAddr >> 2];
    }

    /**
     * Write the 4 byte word at given memory address.
     * @param memAddr memory address to be read (must be word aligned)
     * @param b the memory region with read/write permission
     * @param data data to write
     * @throws IllegalArgumentException if the address isn't word aligned
     * @throws java.rmi.AccessException if memory addr is out of bounds of the accessible region
     */
    public void writeWord(int memAddr, int data, Bound b) throws IllegalArgumentException, java.rmi.AccessException
    {
        int realAddr = b.getLowerBound() + memAddr;

        if ((memAddr & 3) != 0)
            throw new IllegalArgumentException("address isn't word aligned");
        else if (realAddr > b.getHigherBound())
            throw new java.rmi.AccessException("Memory addr out of bounds");

        mem[realAddr >> 2] = data;
    }

    /**
     * Write 1 byte at memory address memAddr within the accessible bound b.
     * @param memAddr memory addr (logical) to be read
     * @param data data to write
     * @param b the memory region with read/write permission
     * @return byte at memory location memAddr
     * @throws java.rmi.AccessException if memory addr is out of bounds of the accessible region
     */
    public void writeByte(int memAddr, byte data, Bound b) throws java.rmi.AccessException
    {
        int realAddr = b.getLowerBound() + ((memAddr >> 2) << 2);
        if (realAddr > b.getHigherBound())
            throw new java.rmi.AccessException("Memory addr out of bounds");

        realAddr = realAddr >> 2; //get rid of useless 2 byte offset
        int temp = mem[realAddr];


        //java promotes byte to int when adding and does sign extention on the
        //promoted int. So I need to clean the variable up
        int data_copy = (int)data & 0xff;

        // |________|________|________|________| => 1 word
        //   addr 0   addr 1   addr 2   addr 3
        switch (memAddr & 3)
        {
            case 3:
                mem[realAddr] = data_copy + (temp & ~255);
                break;
            case 2:
                mem[realAddr] = (data_copy << 8) + (temp & ~(255 << 8));
                break;
            case 1:
                mem[realAddr] = (data_copy << 16) + (temp & ~(255 << 16));
                break;
            case 0:
                mem[realAddr] = (data_copy << 24) + (temp & ~(255 << 24));
                break;
            default:    //should never happen
                throw new RuntimeException("Code shouldn't have execed");
        }
    }
}
