// From https://github.com/nutsalhan87/groupex
package org.labs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class Groupex {
    private static final int BLOCK_SIZE = Long.BYTES;

    private Linker linker = Linker.nativeLinker();
    private SymbolLookup stdlib = linker.defaultLookup();
    private MethodHandle syscall = linker.downcallHandle(
        stdlib.find("syscall").orElseThrow(), 
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,   // Return type: long
            ValueLayout.JAVA_LONG,   // SYS_FUTEX
            ValueLayout.JAVA_LONG,   // uint32_t* uaddr
            ValueLayout.JAVA_INT,    // int futex_op
            ValueLayout.JAVA_INT,    // uint32_t val
            ValueLayout.JAVA_LONG,   // const struct timespec *timeout, or: uint32_t val2
            ValueLayout.JAVA_LONG,   // uint32_t *uaddr2 
            ValueLayout.JAVA_INT     // uint32_t val3
        )
    );
    

    private Arena shared = Arena.ofShared();
    // private VarHandle alvh = MethodHandles.arrayElementVarHandle(Long[].class);
    private VarHandle alvh;
    private MemorySegment blocks;

    private static long getMask(int index) {
        return 1 << (index % BLOCK_SIZE);
    }

    public Groupex(int size) throws IllegalArgumentException {
        if (size <= 0) {
            throw new IllegalArgumentException("Size of the Groupex should be grater than 0");
        }
        this.blocks = shared.allocate(MemoryLayout.sequenceLayout(((size - 1) / Long.BYTES) + 1, ValueLayout.JAVA_INT));
        this.blocks.fill((byte) 0); // Возможно, при аллокации и так заполняется нулями. TODO: проверить.
        // this.alvh = ; // TODO: каким типом корректно задать VarHandle?
    }

    public void lock(int index) {
        validateIndex(index);
        int blockIndex = index / BLOCK_SIZE;
        long mask = getMask(index);
        long prevBlock = (Long) this.alvh.getAndBitwiseOrAcquire(this.blocks, blockIndex, mask); // TODO: Определить корректное взаимодейтсвие между VarHandle и MemorySegment 
        if ((prevBlock | mask) == prevBlock) {
            this.lockSlow(index, mask);
        }
    }

    public void unlock(int index) {
        validateIndex(index);
        int blockIndex = index / BLOCK_SIZE;
        long mask = getMask(index);
        this.alvh.getAndBitwiseAndAcquire(this.blocks, blockIndex, ~mask); // TODO: Определить корректное взаимодейтсвие между VarHandle и MemorySegment 
        // this.syscall.invokeExact() // TODO: Как вызывать futex syscall? Нужен SYS_futex, и возможно, его можно получить как-то при помощи linker и stdlib
    }

    private void lockSlow(int index, long mask) {

    }

    private void validateIndex(int index) throws IndexOutOfBoundsException {
        if (index >= blocks.length * BLOCK_SIZE) {
            throw new IndexOutOfBoundsException(String.format("Index out of range: must be in [0; %i] but it is %i", blocks.length * BLOCK_SIZE - 1, index));
        }
    }
}
