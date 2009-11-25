
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
        for (registers[15] = 0; registers[15] < programMem.getSize(); registers[15] += 4)
        {
            try
            {
                //TODO: check for condition codes here
                exec(programMem.readWord(registers[15], programBound));
            }
            catch (Exception e)
            {
                System.err.println("some exception occured while running a program at $pc " + registers[15]);
                e.printStackTrace(System.err);
                break;
            }
        }
    }

    private void exec(int instruction) throws UnknownFormatException, UnimplementedInstructionException,
            MemoryAccessException {

        System.out.printf("instr is %x\n", instruction);
        int op = ((instruction & generateMask(25, 27)) >> 25) & 0x7;

        switch (op) {
            case 0x0:

                if ((((instruction & generateMask(4,4)) >> 4) & 0x1) == 0) {
                    if ((((instruction & generateMask(24,24)) >> 24) & 0x1) == 1
                        && (((instruction & generateMask(23,23)) >> 23) & 0x1) == 0
                        && (((instruction & generateMask(20,20)) >> 20) & 0x1) == 0) {

                        parseInstrExt0(instruction);
                    } else {
                        /* dataproc immediate shift */
                        int opcode = ((instruction & generateMask(21,24)) >> 21) & 0xf;
                        boolean changeStatus = (instruction & generateMask(20,20)) > 0;
                        int rn = ((instruction & generateMask(16,19)) >> 16) & 0xf;
                        int rd = ((instruction & generateMask(12,15)) >> 12) & 0xf;
                        int rotate = ((instruction & generateMask(8,11)) >> 8) & 0xf;
                        int i = ((instruction & generateMask(25,25)) >> 25) & 0x1;
                        int imm = ((instruction & generateMask(0,7)) >> 0) & 0xff;

                        if (i == 0)
                        {
                            int rm = ((instruction & generateMask(0,3)) >> 0) & 0xf;
                            int shiftType = ((instruction & generateMask(5,6)) >> 5) & 0x3;
                            int shiftAmount = ((instruction & generateMask(7,11)) >> 7) & 0xf;
                            imm = this.shiftHelper(shiftType, shiftAmount, registers[rm]);
                        }
                        else
                        {
                            //rotate imm value
                            for (int c = 0; c < rotate % 8; ++c) //the % is there because rotatin 8 bit int by 9
                                                                //is same as rotating once
                            {
                                int temp = imm & (1<<7);
                                imm <<= 1;
                                imm = imm + ((temp > 0) ? 1 : 0);
                            }
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
                } else if ((((instruction & generateMask(7,7)) >> 7) & 1) == 0) {
                    if (((((instruction & generateMask(24,24)) >> 24)) & 0x1) == 1
                        && (((instruction & generateMask(23,23)) >> 23) & 0x1) == 0
                        && (((instruction & generateMask(20,20)) >> 20) & 0x1) == 0) {
                        /* misc */
                        parseInstrExt1(instruction);
                    } else {
                        /* dataproc register shift */

                        int opcode = ((instruction & generateMask(21,24)) >> 21) & 0xf;
                        boolean changeStatus = (instruction & generateMask(20,20)) > 0;
                        int rn = ((instruction & generateMask(16,19)) >> 16) & 0xf;
                        int rd = ((instruction & generateMask(12,15)) >> 12) & 0xf;
                        int shift = ((instruction & generateMask(4,11)) >> 4) & 0xff;
                        int shiftType = ((instruction & generateMask(5,6)) >> 5) & 0x3;
                        int shiftAmount = 0;
                        int rm = ((instruction & generateMask(0,3)) >> 0) & 0xf;

                        int op2 = 0;

                        if ((shift & 1) == 0)
                        {
                            shiftAmount = ((instruction & generateMask(7,11)) >> 7) & 0x1f;
                        }
                        else
                        {
                            int rs = ((instruction & generateMask(8,11)) >> 8) & 0xf;
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
                } else if ((((instruction & generateMask(4,4)) >> 4) & 0x1) == 1
                            && (((instruction & generateMask(7,7)) >> 7) & 0x1) == 1) {
                    /* Multipliers, extra load/stores */
                    parseInstrExt2(instruction);
                }
                break;
            case 0x1:
                if ((((instruction & generateMask(24,24)) >> 24) & 0x1) == 1
                    && (((instruction & generateMask(23,23)) >> 23) & 0x1) == 0
                    && (((instruction & generateMask(20,20)) >> 20) & 0x1) == 0) {

                    if ((((instruction & generateMask(21,21)) >> 21) & 0x1) == 1) {
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
            {
                /* load/store immediate offset */
                boolean offset = (instruction & generateMask(25, 25)) > 0;
                boolean preIndex = (instruction & generateMask(24, 24)) > 0;
                boolean addOffset = (instruction & generateMask(23,23)) > 0;
                boolean transferByte = (instruction & generateMask(22,22)) > 0;
                boolean writeBack  = (instruction & generateMask(21,21)) > 0;
                boolean load = (instruction & generateMask(20,20)) > 0;
                int rn = ((instruction & generateMask(16,19)) >> 16) & 0xf;
                int rd = ((instruction & generateMask(12,15)) >> 12) & 0xf;
                int imm = ((instruction & generateMask(0,11)) >> 0) & 0x3ff;


                //detemine if instruction is register or imm indexed
                if (offset) //register indexed
                {
                    int rm = ((instruction & generateMask(0,3)) >> 0) & 0xf;
                    int shiftType = ((instruction & generateMask(5,6)) >> 5) & 0x3;
                    int shiftAmount = ((instruction & generateMask(7,11)) >> 7) & 0x1f;
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
                        byte byte1 = (byte)(registers[rd] & generateMask(0, 7));
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
            }
            case 0x3:
                if ((((instruction & generateMask(4,4)) >> 4) & 0x1) == 0) {
                    /* load/store register offset */

                    //return ARMV4_TypeLoadStoreSingle;
                } else {
                    /* undefined */

                    //return ARMV4_TypeUndefined;
                }
                break;
            case 0x4:
                /* load/store multiple */
                boolean preIndex = (instruction & generateMask(24,24)) != 0;
                boolean addOffset = (instruction & generateMask(23,23)) != 0;
                boolean forcePSR = (instruction & generateMask(22,22)) != 0; //i'm not implementing this
                boolean writeBack = (instruction & generateMask(21,21)) != 0;
                boolean load = (instruction & generateMask(20,20)) != 0;

                int rn = ((instruction & generateMask(16,19)) >> 16) & 0xf;
                int registerList = ((instruction & generateMask(0,15)) >> 16) & 0xffff;

                int addr = registers[rn];

                for (int i = 0; i < 16; ++i)
                {
                    if (preIndex)
                    {
                        if (addOffset)
                            addr += 4;
                        else
                            addr = addr - 4;
                    }

                    if(((registerList >> i) & 1) != 0) //need to load/store register
                    {
                        if (load)
                        {
                            registers[i] = dataMem.readWord(addr, dataBound);
                        }
                        else //store
                        {
                            dataMem.writeWord(addr, registers[i], dataBound);
                        }
                    }

                    if (!preIndex)
                    {
                        if (addOffset)
                            addr += 4;
                        else
                            addr = addr - 4;
                    }
                }

                if (writeBack)
                    registers[rn] = addr;
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
                if ((((instruction & generateMask(24,24)) >> 24) & 0x1) == 1) {
                    /* SWI */
                    throw new UnimplementedInstructionException("SWI is not implemented");
                    //return ARMV4_TypeSoftwareInterrupt;
                } else {
                    if ((((instruction & generateMask(4,4)) >> 4) & 0x1) == 1) {
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
        if ((((instr & generateMask(24, 27)) >> 24) & 0xf) == 0) {
            if ((((instr & generateMask(21, 21)) >> 21) & 0x1) == 1) {
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
            if ((((instr & generateMask(7, 7)) >> 7) & 0x1) == 1) {
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
        switch ((((instr & generateMask(24, 27)) >> 24) & 0xf)) {
            case 0x1:
                /* BX or (undefined) CLZ */
                if (op == 0x1) {
                    /* BX */
                    int rd = (((instr & generateMask(0, 4)) >> 0) & 0x1f);
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
                if ((((instr & generateMask(22, 25)) >> 22) & 0xf) == 0) {
                    if ((((instr & generateMask(21, 21)) >> 21) & 0x1) == 1) {
                        /* mla */
                        boolean updateStatus =
                                (((instr & generateMask(20,20)) >> 20) & 0x1) == 1;
                        int rd = (((instr & generateMask(16, 19)) >> 16) & 0xf);
                        int rn = (((instr & generateMask(12, 15)) >> 12) & 0xf);
                        int rs = (((instr & generateMask(8, 11)) >> 8) & 0xf);
                        int rm = (((instr & generateMask(0, 3)) >> 0) & 0xf);

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
                                (((instr & generateMask(20,20)) >> 20) & 0x1) == 1;
                        int rd = (((instr & generateMask(16, 19)) >> 16) & 0xf);
                        int rs = (((instr & generateMask(8, 11)) >> 8) & 0xf);
                        int rm = (((instr & generateMask(0, 3)) >> 0) & 0xf);

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
                } else if ((((instr & generateMask(23, 24)) >> 23) & 0x3) == 1) {
                    int rdHigh = ((instr & generateMask(16,19)) >> 16) & 0xf,
                                rdLow = ((instr & generateMask(12,15)) >> 12) & 0xf,
                                rs = ((instr & generateMask(8,11)) >> 8) & 0xf,
                                rm = ((instr & generateMask(0,3)) >> 0) & 0xf;
                    boolean updateStatus = (((instr & generateMask(20,20)) >> 20) & 0x1) > 0;

                    switch ((((instr & generateMask(21, 22)) >> 21) & 0x3)) {
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
                    if ((((instr & generateMask(22, 22)) >> 22) & 0x1) == 1) {
                        /* swapb */
                        int rn = ((instr & generateMask(16,19)) >> 16) & 0xf,
                            rd = ((instr & generateMask(12,15)) >> 12) & 0xf,
                            rm = ((instr & generateMask(0,3)) >> 0) & 0xf;

                        byte rnData = dataMem.readByte(rn, dataBound);
                        dataMem.writeByte(rn, (byte)(registers[rm] & generateMask(0,7)), dataBound);
                        registers[rd] = ((int)rnData) & generateMask(0,7);
                        //return ARMV4_TypeAtomicSwap;
                    } else {
                        /* swp */
                        int rn = ((instr & generateMask(16,19)) >> 16) & 0xf,
                            rd = ((instr & generateMask(12,15)) >> 12) & 0xf,
                            rm = ((instr & generateMask(0,3)) >> 0) & 0xf;

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
                int rn = ((instr & generateMask(16,19)) >> 16) & 0xf;
                int rd = ((instr & generateMask(12,15)) >> 15) & 0xf;
                int offset = ((instr & generateMask(8,11)) >> 8) & 0xf; //get higher nibble
                byte sh = (byte)(((instr & generateMask(5,6)) >> 5) & 0x3);//don't worry about this

                //detemine if instr is register or imm indexed
                if (offset == 0) //register indexed
                {
                    int rm = ((instr & generateMask(0,3)) >> 0) & 0xf;
                    offset = registers[rm];
                }
                else
                {
                    offset <<= 4;
                    offset &= ~0xff; //clean lower nibble
                    offset +=(byte)((instr & generateMask(0,3)) >> 0) & 0xf;//get lower nibble
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
                    byte byte1 = (byte)(registers[rd] & generateMask(0, 7));
                    byte byte2 = (byte)(registers[rd] & generateMask(8, 15));
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
                if ((((instr & generateMask(22, 22)) >> 22) & 0x1) == 0
                    && (((instr & generateMask(20, 20)) >> 20) & 0x1) == 1) {
                    /* Register offset */
                    if ((((instr & generateMask(5, 5)) >> 5) & 0x1) == 1) {
                        /* ldrsh */
                        boolean preIndex = (instr & generateMask(24, 24)) > 0;
                        boolean addOffset = (instr & generateMask(23,23)) > 0;
                        boolean writeBack  = (instr & generateMask(21,21)) > 0;
                        boolean load = (instr & generateMask(20,20)) > 0;
                        int rn = ((instr & generateMask(16,19)) >> 16) & 0x3;
                        int rd = ((instr & generateMask(15,12)) >> 15) & 0xf;
                        int offset = ((instr & generateMask(8,11)) >> 8) & 0xf; //get higher nibble
                        byte sh = (byte)(((instr & generateMask(5,6)) >> 5) & 0x3);//don't worry about this
                        int rm = ((instr & generateMask(0,3)) >> 0) & 0xf;

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
                        int rn = ((instr & generateMask(16,19)) >> 16) & 0xf;
                        int rd = ((instr & generateMask(15,12)) >> 15) & 0xf;
                        int offset = ((instr & generateMask(8,11)) >> 8) & 0xf; //get higher nibble
                        byte sh = (byte)(((instr & generateMask(5,6)) >> 5) & 0x3);//don't worry about this
                        int rm = ((instr & generateMask(0,3)) >> 0) & 0xf;

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
                } else if ((((instr & generateMask(22, 22)) >> 22) & 0x1) == 1
                        && (((instr & generateMask(20, 20)) >> 20) & 0x1) == 1) {
                    /* Immediate offset */
                    if ((((instr & generateMask(5, 5)) >> 5) & 0x1) == 0) {
                        /* ldrsh */
                        boolean preIndex = (instr & generateMask(24, 24)) > 0;
                        boolean addOffset = (instr & generateMask(23,23)) > 0;
                        boolean writeBack  = (instr & generateMask(21,21)) > 0;
                        boolean load = (instr & generateMask(20,20)) > 0;
                        int rn = ((instr & generateMask(16,19)) >> 16) & 0xf;
                        int rd = ((instr & generateMask(15,12)) >> 15) & 0xf;
                        int offset = (((instr & generateMask(8,11)) >> 8) & 0xf) << 4; //get higher nibble
                        offset += ((instr & generateMask(0,3)) >> 0) & 0xf;
                        byte sh = (byte)(((instr & generateMask(5,6)) >> 5) & 0x3);//don't worry about this

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
                        int rn = ((instr & generateMask(16,19)) >> 16) & 0xf;
                        int rd = ((instr & generateMask(12,15)) >> 15) & 0xf;
                        int offset = (((instr & generateMask(8,11)) >> 8) & 0xf) << 4; //get higher nibble
                        offset += ((instr & generateMask(0,3)) >> 0) & 0xf;
                        byte sh = (byte)(((instr & generateMask(5,6)) >> 5) & 0x3);//don't worry about this

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
