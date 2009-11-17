
package memsim;

import memsim.exceptions.*;
import java.math.BigInteger;

/**
 *
 * @author rdeva
 */
public class Core implements Runnable {

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
    private int registers[] = new int[NUM_REGISTERS];
    private int cpsrRegister = 0;
    private final int N_MASK = 1 << 31, //masks to helpout when fiddling w/ CPSR reg
                      Z_MASK = 1 << 30,
                      C_MASK = 1 << 29,
                      V_MASK = 1 << 28;
    private Memory programMem, dataMem;
    private Bound programBound, dataBound;

    public Memory getDataMemory() {
        return dataMem;
    }

    public Memory getProgramMemory() {
        return programMem;
    }

    public Core(Memory programMem, Bound programBound, Memory dataMem, Bound dataBound) {
        this.programBound = programBound;
        this.dataBound = dataBound;
        this.programMem = programMem;
        this.dataMem = dataMem;
        registers[15] = 0;
    }

    public void run() {
        for (int c = 0; c < programMem.getSize(); c += 4)
            try
            {
                exec(programMem.readWord(c, programBound));
            }
            catch (Exception e)
            {
                System.err.println("some exception occured while running a program");
                e.printStackTrace(System.err);
            }

    }

    private void exec(int instruction) throws UnknownFormatException, UnimplementedInstructionException,
            MemoryAccessException {

        int op = removeTrailingZeroes(instruction & generateMask(25, 27));
        
        switch (op) {
            case 0x0:

                if (removeTrailingZeroes(instruction & generateMask(4,4)) == 0) {
                    if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1
                        && removeTrailingZeroes(instruction & generateMask(23,23)) == 0
                        && removeTrailingZeroes(instruction & generateMask(20,20)) == 0) {

                        parseInstrExt0(instruction);
                    } else {
                        /* dataproc immediate shift */
                        int opcode = removeTrailingZeroes(instruction & generateMask(21,24));
                        boolean changeStatus = (instruction & generateMask(20,20)) > 0;
                        int rn = removeTrailingZeroes(instruction & generateMask(16,19));
                        int rd = removeTrailingZeroes(instruction & generateMask(12,15));
                        int rotate = removeTrailingZeroes(instruction & generateMask(8,11));
                        int imm = removeTrailingZeroes(instruction & generateMask(0,7));

                        //rotate imm value
                        for (int i = 0; i < rotate % 8; ++i) //the % is there because rotatin 8 bit int by 9
                                                            //is same as rotating once
                        {
                            int temp = imm & (1<<7);
                            imm <<= 1;
                            imm = imm + ((temp > 0) ? 1 : 0);
                        }
                        
                        int op2 = imm;
                        int result;
                        switch(opcode)
                        {
                            case 0: //AND
                                result = registers[rd] = registers[rn] & op2;
                                break;
                            case 1: //EOR
                                result = registers[rd] = registers[rn] ^ op2;
                                break;
                            case 2:
                                result = registers[rd] = registers[rn] - op2;
                                break;
                            case 3:
                                result = registers[rd] = op2 - registers[rn];
                                break;
                            case 4:
                                result = registers[rd] = registers[rn] + op2; //FIXME: bug here not checking for overlfow
                                break;
                            case 5:
                                result = registers[rd] = registers[rn] + op2 + (cpsrRegister & C_MASK);
                                break;
                            case 6:
                                result = registers[rd] = registers[rn] - op2 + (cpsrRegister & C_MASK) - 1;
                                break;
                            case 7:
                                result = registers[rd] = op2 - registers[rn] + (cpsrRegister & C_MASK) - 1;
                                break;
                            case 8:
                                result = registers[rn] & op2;
                                break;
                            case 9:
                                result = registers[rn] ^ op2;
                                break;
                            case 10:
                                result = registers[rn] - op2;
                                break;
                            case 11:
                                result = registers[rn] + op2;
                                break;
                            case 12:
                                result = registers[rd] = registers[rn] | op2;
                                break;
                            case 13:
                                result = registers[rd] = op2;
                                break;
                            case 14:
                                result = registers[rd] = registers[rn] & ~op2;
                                break;
                            default: //15
                                result = registers[rd] = ~op2;
                        }

                        if (changeStatus)
                        {
                            if (result == 0)
                                cpsrRegister |= Z_MASK;
                            else
                                cpsrRegister &= ~Z_MASK;

                            if (result < 0)
                                cpsrRegister |= N_MASK;
                            else
                                cpsrRegister &= ~N_MASK;

                            //FIXME: need to check for overflow

                        }
                        //return ARMV4_TypeDataProcessing;
                    }
                } else if (removeTrailingZeroes(instruction & generateMask(7,7)) == 0) {
                    if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1
                        && removeTrailingZeroes(instruction & generateMask(23,23)) == 0
                        && removeTrailingZeroes(instruction & generateMask(20,20)) == 0) {
                        /* misc */
                        parseInstrExt1(instruction);
                    } else {
                        /* dataproc register shift */
                        
                        int opcode = removeTrailingZeroes(instruction & generateMask(21,24));
                        boolean changeStatus = (instruction & generateMask(20,20)) > 0;
                        int rn = removeTrailingZeroes(instruction & generateMask(16,19));
                        int rd = removeTrailingZeroes(instruction & generateMask(12,15));
                        int shift = removeTrailingZeroes(instruction & generateMask(4,11));
                        int shiftType = removeTrailingZeroes(instruction & generateMask(5,6));
                        int shiftAmount = 0;
                        int rm = removeTrailingZeroes(instruction & generateMask(0,3));

                        int op2 = 0;
                        
                        if ((shift & 1) == 0)
                        {
                            shiftAmount = removeTrailingZeroes(instruction & generateMask(7,11));
                        }
                        else
                        {
                            int rs = removeTrailingZeroes(instruction & generateMask(8,11));
                            shiftAmount = registers[rs] & 0xff;
                        }


                        op2 = shiftHelper(shiftType, shiftAmount, registers[rm]);
                        
                        int result;
                        switch(opcode)
                        {
                            case 0: //AND
                                result = registers[rd] = registers[rn] & op2;
                                break;
                            case 1: //EOR
                                result = registers[rd] = registers[rn] ^ op2;
                                break;
                            case 2:
                                result = registers[rd] = registers[rn] - op2;
                                break;
                            case 3:
                                result = registers[rd] = op2 - registers[rn];
                                break;
                            case 4:
                                result = registers[rd] = registers[rn] + op2; //FIXME: bug here not checking for overlfow
                                break;
                            case 5:
                                result = registers[rd] = registers[rn] + op2 + (cpsrRegister & C_MASK);
                                break;
                            case 6:
                                result = registers[rd] = registers[rn] - op2 + (cpsrRegister & C_MASK) - 1;
                                break;
                            case 7:
                                result = registers[rd] = op2 - registers[rn] + (cpsrRegister & C_MASK) - 1;
                                break;
                            case 8:
                                result = registers[rn] & op2;
                                break;
                            case 9:
                                result = registers[rn] ^ op2;
                                break;
                            case 10:
                                result = registers[rn] - op2;
                                break;
                            case 11:
                                result = registers[rn] + op2;
                                break;
                            case 12:
                                result = registers[rd] = registers[rn] | op2;
                                break;
                            case 13:
                                result = registers[rd] = op2;
                                break;
                            case 14:
                                result = registers[rd] = registers[rn] & ~op2;
                                break;
                            default: //15
                                result = registers[rd] = ~op2;
                        }

                        if (changeStatus)
                        {
                            if (result == 0)
                                cpsrRegister |= Z_MASK;
                            else
                                cpsrRegister &= ~Z_MASK;

                            if (result < 0)
                                cpsrRegister |= N_MASK;
                            else
                                cpsrRegister &= ~N_MASK;

                            //FIXME: need to check for overflow

                        }
                        /* ParseInstrDataProc(instr); */
                        //return ARMV4_TypeDataProcessing;
                    }
                } else if (removeTrailingZeroes(instruction & generateMask(4,4)) == 1
                            && removeTrailingZeroes(instruction & generateMask(7,7)) == 1) {
                    /* Multipliers, extra load/stores */
                    parseInstrExt2(instruction);
                }
                break;
            case 0x1:
                if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1
                    && removeTrailingZeroes(instruction & generateMask(23,23)) == 0
                    && removeTrailingZeroes(instruction & generateMask(20,20)) == 0) {
                    
                    if (removeTrailingZeroes(instruction & generateMask(21,21)) == 1) {
                        /* MSR immediate */
                        throw new UnimplementedInstructionException("msr is not implemented");
                        //return ARMV4_TypeStatusRegister;
                    } else {
                        /* undefined */
                        throw new UnknownFormatException();
                        //return ARMV4_TypeUndefined;
                    }
                } else {
                    /* dataproc immediate */
                    //TODO: implement this (can't find in ARM manual )
                    //return ARMV4_TypeDataProcessing;
                }
                break;
            case 0x2:
                /* load/store immediate offset */
                boolean offset = (instruction & generateMask(25, 25)) > 0;
                boolean preIndex = (instruction & generateMask(24, 24)) > 0;
                boolean addOffset = (instruction & generateMask(23,23)) > 0;
                boolean transferByte = (instruction & generateMask(22,22)) > 0;
                boolean writeBack  = (instruction & generateMask(21,21)) > 0;
                boolean load = (instruction & generateMask(20,20)) > 0;
                int rn = removeTrailingZeroes(instruction & generateMask(16,19));
                int rd = removeTrailingZeroes(instruction & generateMask(15,12));
                int imm = removeTrailingZeroes(instruction & generateMask(0,11));
                

                //detemine if instruction is register or imm indexed
                if (offset) //register indexed
                {
                    int rm = removeTrailingZeroes(instruction & generateMask(0,3));
                    int shiftType = removeTrailingZeroes(instruction & generateMask(5,6));
                    int shiftAmount = removeTrailingZeroes(instruction & generateMask(7,11));
                    imm = this.shiftHelper(shiftType, shiftAmount, registers[rm]);
                }
                else
                {
                    //nothing to do, default value is good
                }

                //calculate effective addr
                int addr = registers[rn];
                if (preIndex)
                {
                    if (addOffset)
                        addr += imm;
                    else
                        addr = addr - imm;
                }

                if (load)
                {
                    int data;
                    if (transferByte) //load a byte
                    {
                        data = dataMem.readByte(addr, dataBound) & 0xff;
                    }
                    else //load a word
                    {
                       data = dataMem.readWord(addr, dataBound);
                    }
                    registers[rd] = data;
                }
                else //store
                {

                    if (transferByte) //store a byte
                    {
                        byte byte1 = (byte)removeTrailingZeroes((registers[rd] & generateMask(0, 7)));
                        dataMem.writeByte(addr, byte1, dataBound);
                    }
                    else //store a word
                    {
                        dataMem.writeWord(addr, registers[rd], dataBound);
                    }
                }


                if (!preIndex)//postIndex
                {
                    if (addOffset)
                        addr += imm;
                    else
                        addr = addr - imm;
                }

                if (writeBack)
                {
                    registers[rn] = addr;
                }


                //return ARMV4_TypeLoadStoreSingle;
                break;
            case 0x3:
                if (removeTrailingZeroes(instruction & generateMask(4,4)) == 0) {
                    /* load/store register offset */

                    //return ARMV4_TypeLoadStoreSingle;
                } else {
                    /* undefined */

                    //return ARMV4_TypeUndefined;
                }
                break;
            case 0x4:
                /* load/store multiple */

                //return ARMV4_TypeLoadStoreMultiple;
                break;
            case 0x5:
                /* B and BL */
                /* ParseInstrBranch(instr); */

                //return ARMV4_TypeBranch;
                break;
            case 0x6:
                /* co-processor load/store (LDC/STC) and DSP register transfers (undefined)*/

                //return ARMV4_TypeLoadStoreCoprocessor;
                break;
            case 0x7:
                if (removeTrailingZeroes(instruction & generateMask(24,24)) == 1) {
                    /* SWI */
                    throw new UnimplementedInstructionException("SWI is not implemented");
                    //return ARMV4_TypeSoftwareInterrupt;
                } else {
                    if (removeTrailingZeroes(instruction & generateMask(4,4)) == 1) {
                        /* co-processor register transfers (MRC/MCR) */
                        throw new UnimplementedInstructionException("MRC/MCR not implemented");
                        //return ARMV4_TypeRegisterTransferCoprocessor;
                    } else {
                        /* co-processor data processing (CDP) */
                        throw new UnimplementedInstructionException("CDP is not implemented");
                        //return ARMV4_TypeDataProcessingCoprocessor;
                    }
                }
                //break;
            default:
                throw new UnknownFormatException("instruction cannot be parsed");
                //return ARMV4_TypeUndefined;
        }
        //return ARMV4_TypeUndefined;

    }

    public void parseInstrExt0(int instr) throws
            UnimplementedInstructionException, UnknownFormatException
    {
        /* this function depends on prior check of whether bit4 is zero */
        if (removeTrailingZeroes((instr & generateMask(24, 27))) == 0) {
            if (removeTrailingZeroes((instr & generateMask(21, 21))) == 1) {
                /* TODO: Add bits[15:12] = 1 [11:8] = 0 */
                /* msr */
                throw new UnimplementedInstructionException("msr is not implemented");
                //return ARMV4_TypeStatusRegister;
            } else {
                /* TODO: Add bits[19:16] = 1 [11:8] = 0 */
                /* mrs */
                throw new UnimplementedInstructionException("mrs is not implemented");
               // return ARMV4_TypeStatusRegister;
            }
        } else {
            if (removeTrailingZeroes((instr & generateMask(7, 7))) == 1) {
                /* Undefined DSP instruction */
                throw new UnknownFormatException("instruction cannot be parsed");
                //return ARMV4_TypeUndefined;
            } else {
                throw new UnknownFormatException("instruction cannot be parsed");
                //return ARMV4_TypeUndefined;
            }
        }
        //return ARMV4_TypeUndefined;
    }

    private void parseInstrExt1(int instr) throws 
            UnknownFormatException, UnimplementedInstructionException {
        /* opcode = bit[22:21] and bit[7:4] */
        int op = (instr >> 21) & 0x3;
        switch (removeTrailingZeroes(instr & generateMask(24, 27))) {
            case 0x1:
                /* BX or (undefined) CLZ */
                if (op == 0x1) {
                    /* BX */
                    int rd = removeTrailingZeroes(instr & generateMask(0, 4));
                    registers[14] = registers[15] + 4; //link register := PC + 4
                    registers[15] = registers[rd]; //PC := Rn
                    //return ARMV4_TypeBranch;
                } else {
                    throw new UnknownFormatException("instruction cannot be parsed");
                    //return ARMV4_TypeUndefined;
                }
                break;
            case 0x3:
                /* (undefined) BLX */
                throw new UnimplementedInstructionException("BLX has not been implemented");
                //return ARMV4_TypeUndefined;
                //break;
            case 0x5:
                /* (undefined) DSP add/sub */
                throw new UnimplementedInstructionException("DSP Add/Sub has not been implemented");
                //return ARMV4_TypeUndefined;
                //break;
            case 0x7:
                /* (undefined) BKPT */
                throw new UnimplementedInstructionException("BKPT has not been implemented");
                //return ARMV4_TypeUndefined;
                //break;
            default:
                //return ARMV4_TypeUndefined;
        }
        /* make the compiler shut up */
        //return ARMV4_TypeUndefined;
    }


    private void parseInstrExt2(int instr) throws memsim.exceptions.MemoryAccessException{
        /* MUL/MLA,SMLAL,SMULL,UMLAL,UMULL, STRH/LDRH, LDRSB, LDRH and two undefined*/
        int op = (instr >> 5) & 0x3;
        switch (op) {
            /* Multiply, multiply long, swap */
            case 0x0:
                if (removeTrailingZeroes(instr & generateMask(22, 25)) == 0) {
                    if (removeTrailingZeroes(instr & generateMask(21, 21)) == 1) {
                        /* mla */
                        boolean updateStatus = 
                                removeTrailingZeroes(instr & generateMask(20,20)) == 1;
                        int rd = removeTrailingZeroes(instr & generateMask(16, 19));
                        int rn = removeTrailingZeroes(instr & generateMask(12, 15));
                        int rs = removeTrailingZeroes(instr & generateMask(8, 11));
                        int rm = removeTrailingZeroes(instr & generateMask(0, 3));

                        //to take care of overflows, do math in BigInt and mask
                        //down to 32 bits if necessary

                        BigInteger rmBig = new BigInteger(Integer.toString(registers[rm])),
                                rsBig = new BigInteger(Integer.toString(registers[rs])),
                                rnBig = new BigInteger(Integer.toString(registers[rn])),
                                rdBig;

                        rdBig = rnBig.add(rmBig.multiply(rsBig));
                        //can it fit in 32 bits?
                        if (rdBig.compareTo(new BigInteger(((1 << 32) - 1) + "")) <= 0)
                        {
                            registers[rd] = rdBig.intValue();
                        }
                        else
                        {
                            //isolate the lower 32 bits, discard anything else
                            registers[rd] =
                                    rdBig.and(new BigInteger(((1 << 32) - 1) + "")).intValue();
                        }

                        //update the status registers if need be
                        if (updateStatus)
                        {
                            //note in ARMv4 and under, C flag was unpredictable,
                            //but in ARMv5 and up, C flag was uneffected.
                            //simulator assumes v5+
                            if (registers[rd] < 0) //update N flag
                                cpsrRegister |= N_MASK;
                            else
                                cpsrRegister &= ~N_MASK;

                            if (registers[rd] == 0)
                                cpsrRegister |= Z_MASK;
                            else
                                cpsrRegister &= ~Z_MASK;
                        }
                            
                        //return ARMV4_TypeMultiplication;
                    } else {
                        /* mul */
                        boolean updateStatus =
                                removeTrailingZeroes(instr & generateMask(20,20)) == 1;
                        int rd = removeTrailingZeroes(instr & generateMask(16, 19));
                        int rs = removeTrailingZeroes(instr & generateMask(8, 11));
                        int rm = removeTrailingZeroes(instr & generateMask(0, 3));

                        BigInteger rmBig = new BigInteger(Integer.toString(registers[rm])),
                                rsBig = new BigInteger(Integer.toString(registers[rs])),
                                rdBig;

                        rdBig = rmBig.multiply(rsBig);
                        //can it fit in 32 bits?
                        if (rdBig.compareTo(new BigInteger(((1 << 32) - 1) + "")) <= 0)
                        {
                            registers[rd] = rdBig.intValue();
                        }
                        else
                        {
                            //isolate the lower 32 bits, discard anything else
                            registers[rd] =
                                    rdBig.and(new BigInteger(((1 << 32) - 1) + "")).intValue();
                        }

                                                //update the status registers if need be
                        if (updateStatus)
                        {
                            //note in ARMv4 and under, C flag was unpredictable,
                            //but in ARMv5 and up, C flag was uneffected.
                            //simulator assumes v5+
                            if (registers[rd] < 0) //update N flag
                                cpsrRegister |= N_MASK;
                            else
                                cpsrRegister &= ~N_MASK;

                            if (registers[rd] == 0)
                                cpsrRegister |= Z_MASK;
                            else
                                cpsrRegister &= ~Z_MASK;

                        }
                        
                        //return ARMV4_TypeMultiplication;
                    }
                } else if (removeTrailingZeroes(instr & generateMask(23, 24)) == 1) {
                    int rdHigh = removeTrailingZeroes(instr & generateMask(16,19)),
                                rdLow = removeTrailingZeroes(instr & generateMask(12,15)),
                                rs = removeTrailingZeroes(instr & generateMask(8,11)),
                                rm = removeTrailingZeroes(instr & generateMask(0,3));
                    boolean updateStatus = removeTrailingZeroes(instr & generateMask(20,20)) > 0;
                    
                    switch (removeTrailingZeroes(instr & generateMask(21, 22))) {
                        case 0x0:
                        {
                            BigInteger result =
                                    new BigInteger(Integer.toString(registers[rs]))
                                    .multiply(new BigInteger(Integer.toString(registers[rm])));
                            byte[] resultBytes = result.toByteArray();

                            registers[rdHigh] = resultBytes[0];
                            registers[rdLow] = resultBytes[1];

                            if (updateStatus)
                            {
                                if (registers[rs] < 0 ^ registers[rm] < 0)
                                    cpsrRegister |= N_MASK;
                                else
                                    cpsrRegister &= ~N_MASK;

                                if (registers[rs] == 0 || registers[rm] == 0)
                                    cpsrRegister |= Z_MASK;
                                else
                                    cpsrRegister &= ~Z_MASK;

                                //note in ARMv4 and under, C and V flag was unpredictable,
                                //but in ARMv5 and up, C and V flag was uneffected.
                                //simulator assumes v5+
                            }
                            //return ARMV4_TypeMultiplication; /* smull */
                            break;
                        }
                        case 0x1:
                        {
                            long toAccumulate = (registers[rdHigh] << 32) + registers[rdLow];
                            BigInteger toAccumulateBig = new BigInteger(Long.toString(toAccumulate));
                            BigInteger multiplyResult =
                                    new BigInteger(Integer.toString(registers[rs]))
                                    .multiply(new BigInteger(Integer.toString(registers[rm])));
                            BigInteger result = toAccumulateBig.add(multiplyResult);
                            
                            byte[] resultBytes = result.toByteArray();
                            registers[rdHigh] = resultBytes[0];
                            registers[rdLow] = resultBytes[1];

                            if (updateStatus)
                            {
                                if (result.compareTo(BigInteger.ZERO) < 0)
                                    cpsrRegister |= N_MASK;
                                else
                                    cpsrRegister &= ~N_MASK;

                                if (result.compareTo(BigInteger.ZERO) == 0)
                                    cpsrRegister |= Z_MASK;
                                else
                                    cpsrRegister &= ~Z_MASK;

                                //note in ARMv4 and under, C and V flag was unpredictable,
                                //but in ARMv5 and up, C and V flag was uneffected.
                                //simulator assumes v5+
                            }
                            //return ARMV4_TypeMultiplication; /* smlal */
                            break;
                        }
                        case 0x2:
                        {
                            BigInteger result =
                                    new BigInteger(Integer.toString(registers[rs]))
                                    .multiply(new BigInteger(Integer.toString(registers[rm])));
                            byte[] resultBytes = result.toByteArray();

                            registers[rdHigh] = resultBytes[0];
                            registers[rdLow] = resultBytes[1];

                            if (updateStatus)
                            {
                                if (registers[rs] < 0 ^ registers[rm] < 0)
                                    cpsrRegister |= N_MASK;
                                else
                                    cpsrRegister &= ~N_MASK;

                                if (registers[rs] == 0 || registers[rm] == 0)
                                    cpsrRegister |= Z_MASK;
                                else
                                    cpsrRegister &= ~Z_MASK;

                                //note in ARMv4 and under, C and V flag was unpredictable,
                                //but in ARMv5 and up, C and V flag was uneffected.
                                //simulator assumes v5+
                            }
                            //return ARMV4_TypeMultiplication; /* umull */
                            break;
                        }
                        case 0x3:
                        {
                            //TODO check all unsigned operations, the toString()s might cause trouble
                            long toAccumulate = (registers[rdHigh] << 32) + registers[rdLow];
                            BigInteger toAccumulateBig = new BigInteger(Long.toString(toAccumulate));
                            BigInteger multiplyResult =
                                    new BigInteger(Integer.toString(registers[rs]))
                                    .multiply(new BigInteger(Integer.toString(registers[rm])));
                            BigInteger result = toAccumulateBig.add(multiplyResult);

                            byte[] resultBytes = result.toByteArray();
                            registers[rdHigh] = resultBytes[0];
                            registers[rdLow] = resultBytes[1];

                            if (updateStatus)
                            {
                                if (result.compareTo(BigInteger.ZERO) < 0)
                                    cpsrRegister |= N_MASK;
                                else
                                    cpsrRegister &= ~N_MASK;

                                if (result.compareTo(BigInteger.ZERO) == 0)
                                    cpsrRegister |= Z_MASK;
                                else
                                    cpsrRegister &= ~Z_MASK;
                            }
                            //note in ARMv4 and under, C and V flag was unpredictable,
                            //but in ARMv5 and up, C and V flag was uneffected.
                            //simulator assumes v5+

                            //return ARMV4_TypeMultiplication; /* umlal */
                            break;
                        }
                    }
                } else if (((instr >> 20) & 0x1B) == 0x10) {
                    if (removeTrailingZeroes(instr & generateMask(22, 22)) == 1) {
                        /* swapb */
                        int rn = removeTrailingZeroes(instr & generateMask(16,19)),
                            rd = removeTrailingZeroes(instr & generateMask(12,15)),
                            rm = removeTrailingZeroes(instr & generateMask(0,3));

                        byte rnData = dataMem.readByte(rn, dataBound);
                        dataMem.writeByte(rn, (byte)(registers[rm] & generateMask(0,7)), dataBound);
                        registers[rd] = ((int)rnData) & generateMask(0,7);
                        //return ARMV4_TypeAtomicSwap;
                    } else {
                        /* swp */
                        int rn = removeTrailingZeroes(instr & generateMask(16,19)),
                            rd = removeTrailingZeroes(instr & generateMask(12,15)),
                            rm = removeTrailingZeroes(instr & generateMask(0,3));

                        int rnData = dataMem.readWord(rn, dataBound);
                        dataMem.writeWord(rn, registers[rm], dataBound);
                        registers[rd] = rnData;
                        //return ARMV4_TypeAtomicSwap;
                    }
                }
                break;

            /* Load/store half-word ldrh/strh (unsigned)*/
            case 0x1:
            {
                boolean preIndex = (instr & generateMask(24, 24)) > 0;
                boolean addOffset = (instr & generateMask(23,23)) > 0;
                boolean writeBack  = (instr & generateMask(21,21)) > 0;
                boolean load = (instr & generateMask(20,20)) > 0;
                int rn = removeTrailingZeroes(instr & generateMask(16,19));
                int rd = removeTrailingZeroes(instr & generateMask(15,12));
                int offset = removeTrailingZeroes(instr & generateMask(8,11)); //get higher nibble
                byte sh = (byte)removeTrailingZeroes(instr & generateMask(5,6));//don't worry about this

                //detemine if instr is register or imm indexed
                if (offset == 0) //register indexed
                {
                    int rm = removeTrailingZeroes(instr & generateMask(0,3));
                    offset = registers[rm];
                }
                else
                {
                    offset <<= 4;
                    offset &= ~0xff; //clean lower nibble
                    offset +=(byte)removeTrailingZeroes(instr & generateMask(0,3));//get lower nibble
                }

                //calculate effective addr
                int addr = registers[rn];
                if (preIndex)
                {
                    if (addOffset)
                        addr += offset;
                    else
                        addr = addr - offset;
                }
                
                if (load)
                {
                    byte data1 = dataMem.readByte(addr, dataBound);
                    byte data2 = dataMem.readByte(addr+1, dataBound);
                    int data = ((((int)data2) << 8) & 0xff) + (((int)data1) & 0xff);
                    registers[rd] = data;
                }
                else //store
                {
                    byte byte1 = (byte)removeTrailingZeroes((registers[rd] & generateMask(0, 7)));
                    byte byte2 = (byte)removeTrailingZeroes((registers[rd] & generateMask(8, 15)));
                    dataMem.writeByte(addr, byte1, dataBound);
                    dataMem.writeByte(addr + 1, byte2, dataBound);
                }

                if (!preIndex)//postIndex
                {
                    if (addOffset)
                        addr += offset;
                    else
                        addr = addr - offset;
                }

                if (writeBack)
                {
                    registers[rn] = addr;
                }

                //return ARMV4_TypeLoadStoreExtra;
                break;
            }
            /* Load signed half-word */
            case 0x2:
            case 0x3:
                /* TODO: check for L = 0 && S = 1 which is unpredictable */
                if (removeTrailingZeroes(instr & generateMask(22, 22)) == 0
                    && removeTrailingZeroes(instr & generateMask(20, 20)) == 1) {
                    /* Register offset */
                    if (removeTrailingZeroes(instr & generateMask(5, 5)) == 1) {
                        /* ldrsh */
                        boolean preIndex = (instr & generateMask(24, 24)) > 0;
                        boolean addOffset = (instr & generateMask(23,23)) > 0;
                        boolean writeBack  = (instr & generateMask(21,21)) > 0;
                        boolean load = (instr & generateMask(20,20)) > 0;
                        int rn = removeTrailingZeroes(instr & generateMask(16,19));
                        int rd = removeTrailingZeroes(instr & generateMask(15,12));
                        int offset = removeTrailingZeroes(instr & generateMask(8,11)); //get higher nibble
                        byte sh = (byte)removeTrailingZeroes(instr & generateMask(5,6));//don't worry about this
                        int rm = removeTrailingZeroes(instr & generateMask(0,3));

                        offset = registers[rm];

                        int addr = registers[rn];
                        if (preIndex)
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        byte data1 = dataMem.readByte(addr, dataBound);
                        byte data2 = dataMem.readByte(addr+1, dataBound);
                        int data = (data2 << 8) + data1; //note, I get sign extension
                                                        //for free when converting
                                                        //from byte to int

                        
                        registers[rd] = data;

                        if (!preIndex)//postIndex
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        if (writeBack)
                        {
                            registers[rn] = addr;
                        }
                        //return ARMV4_TypeLoadStoreExtra;
                    } else {
                        /* ldrsb */
                        boolean preIndex = (instr & generateMask(24, 24)) > 0;
                        boolean addOffset = (instr & generateMask(23,23)) > 0;
                        boolean writeBack  = (instr & generateMask(21,21)) > 0;
                        boolean load = (instr & generateMask(20,20)) > 0;
                        int rn = removeTrailingZeroes(instr & generateMask(16,19));
                        int rd = removeTrailingZeroes(instr & generateMask(15,12));
                        int offset = removeTrailingZeroes(instr & generateMask(8,11)); //get higher nibble
                        byte sh = (byte)removeTrailingZeroes(instr & generateMask(5,6));//don't worry about this
                        int rm = removeTrailingZeroes(instr & generateMask(0,3));

                        offset = registers[rm];

                        int addr = registers[rn];
                        if (preIndex)
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        byte data1 = dataMem.readByte(addr, dataBound);
                        int data = data1; //free sign extension by converting
                                            //byte to int!

                        registers[rd] = data;

                        if (!preIndex)//postIndex
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        if (writeBack)
                        {
                            registers[rn] = addr;
                        }
                        //return ARMV4_TypeLoadStoreExtra;
                    }
                } else if (removeTrailingZeroes(instr & generateMask(22, 22)) == 1
                        && removeTrailingZeroes(instr & generateMask(20, 20)) == 1) {
                    /* Immediate offset */
                    if (removeTrailingZeroes(instr & generateMask(5, 5)) == 0) {
                        /* ldrsh */
                        boolean preIndex = (instr & generateMask(24, 24)) > 0;
                        boolean addOffset = (instr & generateMask(23,23)) > 0;
                        boolean writeBack  = (instr & generateMask(21,21)) > 0;
                        boolean load = (instr & generateMask(20,20)) > 0;
                        int rn = removeTrailingZeroes(instr & generateMask(16,19));
                        int rd = removeTrailingZeroes(instr & generateMask(15,12));
                        int offset = removeTrailingZeroes(instr & generateMask(8,11)) << 4; //get higher nibble
                        offset += removeTrailingZeroes(instr & generateMask(0,3));
                        byte sh = (byte)removeTrailingZeroes(instr & generateMask(5,6));//don't worry about this

                        int addr = registers[rn];
                        if (preIndex)
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        byte data1 = dataMem.readByte(addr, dataBound);
                        byte data2 = dataMem.readByte(addr+1, dataBound);
                        int data = (data2 << 8) + data1; //note, I get sign extension
                                                        //for free when converting
                                                        //from byte to int

                        registers[rd] = data;

                        if (!preIndex)//postIndex
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        if (writeBack)
                        {
                            registers[rn] = addr;
                        }
                        //return ARMV4_TypeLoadStoreExtra;
                    } else {
                        /* ldrsb */
                        boolean preIndex = (instr & generateMask(24, 24)) > 0;
                        boolean addOffset = (instr & generateMask(23,23)) > 0;
                        boolean writeBack  = (instr & generateMask(21,21)) > 0;
                        boolean load = (instr & generateMask(20,20)) > 0;
                        int rn = removeTrailingZeroes(instr & generateMask(16,19));
                        int rd = removeTrailingZeroes(instr & generateMask(15,12));
                        int offset = removeTrailingZeroes(instr & generateMask(8,11)) << 4; //get higher nibble
                        offset += removeTrailingZeroes(instr & generateMask(0,3));
                        byte sh = (byte)removeTrailingZeroes(instr & generateMask(5,6));//don't worry about this

                        int addr = registers[rn];
                        if (preIndex)
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        byte data1 = dataMem.readByte(addr, dataBound);
                        
                        int data = data1; //note, I get sign extension
                                           //for free when converting
                                            //from byte to int

                        registers[rd] = data;

                        if (!preIndex)//postIndex
                        {
                            if (addOffset)
                                addr += offset;
                            else
                                addr = addr - offset;
                        }

                        if (writeBack)
                        {
                            registers[rn] = addr;
                        }
                        //return ARMV4_TypeLoadStoreExtra;
                    }
                } else {
                    //return ARMV4_TypeUndefined;
                }
                break;
            default:
                //return ARMV4_TypeUndefined;
        }
        //return ARMV4_TypeUndefined; /* Make the compiler shut up */
    }


    private int generateMask(int from_bit, int to_bit) {
        if (from_bit < 0 || to_bit > 31 || from_bit > to_bit) {
            throw new IllegalArgumentException("this must be true: from_bit >= 0 and to_bit <= 31 and from_bit <= to_bit");
        }

        return ((2 << (to_bit - from_bit)) - 1) << from_bit;
    }

    private int removeTrailingZeroes(int x) {
        int guard = 0;
        while ((x & 1) == 0) {
            x = x >> 1;
            ++guard;

            if (guard > 32) //if x == 0 is passed in, then this might be infinite loop
            {
                break;
            }
        }

        return x;

    }

    /**
     * Helps with shifting operations in data processing instrs
     * @param shiftType 2 bit value, where
     * <code>
     * 00 = logical left
     * 01 = logical right
     * 10 = arithmetic right
     * 11 = rotate right
     * </code>
     * @param shiftAmount  Amount to do the shift by
     * @param shiftValue Value to be shifted
     * @return the shifted value of shiftValue
     */
    private int shiftHelper(int shiftType, int shiftAmount, int shiftValue)
    {
        switch (shiftType) {
            case 0:
                return shiftValue << shiftAmount;
            case 1: //logic shift right
                int temp = shiftValue;
                for (int i = 0; i < shiftAmount; ++i) {
                    temp = (temp >> 1) & ~(1 << 31);
                }
                return temp;
            case 2: //arithmetic right shift
                return shiftValue >> shiftAmount;
            case 3:
            default:
                for (int i = 0; i < shiftAmount; ++i)
                {
                    int temp2 = shiftValue & 1;
                    shiftValue >>= 1;
                    shiftValue &= 0xff;
                    shiftValue |= ((temp2 > 0) ? 0x80 : 0);
                }
                return shiftValue;
        }
    }
}
