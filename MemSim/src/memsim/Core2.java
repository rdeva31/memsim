
package memsim;


import memsim.exceptions.*;

/**
 *
 * @author rdeva
 */
public class Core2 implements Runnable{
    
    /**
     * 
        Register        Description
        R0              Argument1, Return Value, Temporary register
        R1              Argument2, Second 32-bits if double/int Return Value, Temporary register
        R2-R3           Arguments, Temporary registers
        R4-R10          Permenant registers. R7 is THUMB frame pointer
        R11             ARM frame pointer. Permanent register
        R12             Temporary register
        R13             Stack pointer. Permanent register
        R14             Link register. Permanent register
        R15             Program Counter
     */
    final static int NUM_REGISTERS = 16;
    int registers[] = new int[NUM_REGISTERS];

    Memory programMem, dataMem;
    Bound programBound, dataBound;
    
    public Core2(Memory programMem, Bound programBound, Memory dataMem, Bound dataBound)
    {
        this.programBound = programBound;
        this.dataBound = dataBound;
        this.programMem = programMem;
        this.dataMem = dataMem;
        registers[15] = 0;
    }
    
    public void run()
    {
        //TODO: fill in shit
    }

    private void exec(int instruction) throws UnknownFormatException, UnimplementedInstructionException,
                                        MemoryAccessException
    {
        final int condition = remove_trailing_zeroes(instruction & generate_mask(28, 31));
        final int format = remove_trailing_zeroes(instruction & generate_mask(26, 27));

        if (format == 0)// takes care of data processing, multiply, multiply long,
                        //single data swap, branch and exchange, half word data transfer
                        // (register and immidiate),
        {

        }
        else if (format == 1)//takes care of single data transfer and undefined.
                            //i'm going to pretend undefined instrs don't exist ad
                            //treat all instructions in this format as single data
                            //transfer instrs
        {

        }
        else if (format == 2)//takes care of block data transfer and branch
        {
            final int instrType = remove_trailing_zeroes(instruction & generate_mask(25,25));
            if (instrType == 0) //block data transfer
            {
                boolean load = (instruction & generate_mask(20,20)) > 0;
                int baseMemAddr = registers[remove_trailing_zeroes(instruction & generate_mask(16,19))];
                boolean writeBack = (instruction & generate_mask(21,21)) > 0; //TODO what the hell is this for?
                boolean psr = (instruction & generate_mask(22,22)) > 0;//TODO what the hell is this for
                boolean add = (instruction & generate_mask(23,23)) > 0;
                boolean preIncrement = (instruction & generate_mask(24,24)) > 0;
                boolean operateOnRegister[] = new boolean[NUM_REGISTERS];
                boolean operatedOnRegister[] = new boolean[NUM_REGISTERS];

                for (int c = 0; c < NUM_REGISTERS; ++c)
                {
                    if ((instruction & generate_mask(c,c)) > 0)
                        operateOnRegister[c] = true;
                    else
                        operateOnRegister[c] = false; //added this because i'm not sure what
                                                        // the default values are for local
                                                        // vars
                    operatedOnRegister[c] = false;
                }

                for (int c = 0; c < NUM_REGISTERS; ++c)
                {
                    //find the mem addr
                    if (preIncrement)
                    {
                        if (add)
                            baseMemAddr += 32;
                        else
                            baseMemAddr -= 32;
                    }
                    
                    //load or store that mem addr?
                    if (load)
                    {
                        //find appropriate register to move data into
                        int reg = -1;
                        for (int i = 0; i < NUM_REGISTERS; ++i)
                            if (operateOnRegister[c] && !operatedOnRegister[c])
                            {
                                reg = c;
                                break;
                            }
                        if (reg == -1) //done copying into registers
                            break;
                        registers[reg] = dataMem.readWord(baseMemAddr, dataBound);
                    }
                    else //store
                    {
                        //find appropriate register to move data out of 
                        int reg = -1;
                        for (int i = 0; i < NUM_REGISTERS; ++i)
                            if (operateOnRegister[c] && !operatedOnRegister[c])
                            {
                                reg = c;
                                break;
                            }
                        if (reg == -1) //done copying into registers
                            break;
                        dataMem.writeWord(baseMemAddr, registers[reg], dataBound);
                    }
                }
                
            }
            else if (instrType == 1) //branch
            {
                boolean toLink = ((instruction & generate_mask(24,24)) > 0);
                int offset = instruction & generate_mask(0,23);
                //the offset is stored as a 2's complement 24 bit offset
                //so need to left shift by two, sign extend to 32 bits
                //then add it to PC

                //sign extend only makes sense for negative offset,
                //so check to see if offset is negative
                if ((offset & generate_mask(23,23)) > 0)
                {
                    offset |= (1 << (32 - 23 + 1)) - 1;
                }

                offset = offset << 2;

                if (toLink)
                    registers[14] = registers[15];

                registers[15] += offset;
            }
            else
                throw new RuntimeException("shouldn't have execed");
        }
        else if (format == 3)//takes care of coprocessor data transfer, coporcessor register transfer, software interrupt
        {
            int instrType = remove_trailing_zeroes(instruction & generate_mask(24, 25));

            if (instrType == 2) // coprocessor data transfer, coporcessor register transfer
            {
                instrType = remove_trailing_zeroes(instruction & generate_mask(4,4));
                if (instrType == 0) //coprocessor data operation
                {
                    throw new UnimplementedInstructionException("coprocessor data transfers are unimplemented");
                }
                else if (instrType == 1) //coprocessor register transfer
                {
                    throw new UnimplementedInstructionException("coprocessor register transfers are unimplemented");
                }
                else
                    throw new RuntimeException("shouldn't have execed");
            }
            else if (instrType == 3) //software interrupt
            {
                throw new UnimplementedInstructionException("software interrupts are unimplemented");
            }
            else
                throw new UnknownFormatException("this format isn't valid");

        }
        else throw new UnknownFormatException("this format isn't valid");
    }

    private int generate_mask(int from_bit, int to_bit)
    {
        if (from_bit < 0 || to_bit > 31 || from_bit < to_bit)
            throw new IllegalArgumentException("this must be true: from_bit >= 0 and to_bit <= 31 and from_bit <= to_bit");

        return ((2 << (to_bit - from_bit + 1)) - 1) << from_bit;
    }

    private int remove_trailing_zeroes(int x)
    {
        int guard = 0;
        while ((x & 1) == 0)
        {
            x = x >> 1;
            ++guard;

            if (guard > 32) //if x == 0 is passed in, then this might be infinite loop
                break;
        }

        return x;

    }
}
