// From https://github.com/nutsalhan87/groupex/blob/experiments/src/groupex_impl/raw_groupex_futex_bitset.rs
package org.labs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public class Groupex {
    private static final int BLOCK_SIZE = Integer.SIZE;
    private static final int SPIN_LIMIT = 5;
    private static final long SYS_FUTEX = 202; // amd64
    private static final int FUTEX_WAIT_BITSET = 9;
    private static final int FUTEX_WAKE_BITSET = 10;
    private static final int FUTEX_PRIVATE_FLAG = 128;
    private static final int EAGAIN = 11;
    private final static int EACCES = 13;
    private final static int EFAULT = 14;
    private final static int EINVAL = 22;
    private final static int ENOSYS = 38; 
    
    private final Linker linker = Linker.nativeLinker();

    private final StructLayout captureLayout = Linker.Option.captureStateLayout();
    private final VarHandle errnoHandle =
        captureLayout.varHandle(MemoryLayout.PathElement.groupElement("errno"));
    private final ThreadLocal<MemorySegment> captureTL =
        ThreadLocal.withInitial(() -> Arena.ofAuto().allocate(captureLayout));

    private final MethodHandle syscall = linker.downcallHandle(
        linker.defaultLookup().findOrThrow("syscall"), 
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,   // Return type: long
            ValueLayout.JAVA_LONG,   // SYS_FUTEX
            ValueLayout.JAVA_LONG,   // uint32_t* uaddr
            ValueLayout.JAVA_INT,    // int futex_op
            ValueLayout.JAVA_INT,    // uint32_t val
            ValueLayout.JAVA_LONG,   // const struct timespec *timeout, or: uint32_t val2
            ValueLayout.JAVA_LONG,   // uint32_t *uaddr2 
            ValueLayout.JAVA_INT     // uint32_t val3
        ),
        Linker.Option.captureCallState("errno"),
        Linker.Option.firstVariadicArg(1)
    );

    private final Arena shared = Arena.ofShared();
    private final VarHandle alvh;
    private final MemorySegment blocks;
    private final long blocksLength;

    private static int getMask(int index) {
        return 1 << (index % BLOCK_SIZE);
    }

    public Groupex(int size) throws IllegalArgumentException {
        if (size <= 0) {
            throw new IllegalArgumentException("Size of the Groupex should be grater than 0");
        }
        this.blocksLength = ((size - 1) / BLOCK_SIZE) + 1;
        var memoryLayout = MemoryLayout.sequenceLayout(this.blocksLength, ValueLayout.JAVA_INT);
        this.blocks = shared.allocate(memoryLayout);
        this.alvh = memoryLayout.varHandle(MemoryLayout.PathElement.sequenceElement());
    }

    public void lock(int index) {
        validateIndex(index);
        long blockIndex = index / BLOCK_SIZE;
        int mask = getMask(index);
        int prevBlock = (Integer) this.alvh.getAndBitwiseOrAcquire(this.blocks, 0L, blockIndex, mask);
        if ((prevBlock | mask) == prevBlock) {
            this.lockSlow(blockIndex, mask);
        }
    }

    public boolean tryLock(int index) {
        validateIndex(index);
        long blockIndex = index / BLOCK_SIZE;
        int mask = getMask(index);
        int prevBlock = (Integer) this.alvh.getAndBitwiseOrAcquire(this.blocks, 0L, blockIndex, mask);

        return (prevBlock | mask) != prevBlock;
    }

    public void unlock(int index) {
        validateIndex(index);
        long blockIndex = index / BLOCK_SIZE;
        int mask = getMask(index);
        this.alvh.getAndBitwiseAndRelease(this.blocks, 0L, blockIndex, ~mask);
        try {
            MemorySegment capture = captureTL.get();
            long result = (long) this.syscall.invokeExact(
                capture,
                SYS_FUTEX,
                this.blocks.asSlice(ValueLayout.JAVA_INT.scale(0, blockIndex)).address(),
                FUTEX_WAKE_BITSET | FUTEX_PRIVATE_FLAG,
                (int) 1,
                0L,
                0L,
                mask
            );
            int errno = (int) errnoHandle.get(capture, 0L);
            if (result == -1 && errno != EAGAIN) {
                throw new FutexException(errno);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void lockSlow(long blockIndex, int mask) {
        for (int spinCnt = 0; spinCnt < SPIN_LIMIT; ++spinCnt) {
            int prevBlock = (Integer) this.alvh.getAndBitwiseOrAcquire(this.blocks, 0L, blockIndex, mask);
            if ((prevBlock | mask) != prevBlock) {
                return;
            }
            for (int spin = 0; spin < (1 << spinCnt); ++spin) {
                Thread.onSpinWait();
            }
        }

        while (true) {
            int prevBlock = (Integer) this.alvh.getAndBitwiseOrAcquire(this.blocks, 0L, blockIndex, mask);
            if ((prevBlock | mask) != prevBlock) {
                return;
            }
            try {
                MemorySegment capture = captureTL.get();
                long result = (long) this.syscall.invokeExact(
                    capture,
                    SYS_FUTEX,
                    this.blocks.asSlice(ValueLayout.JAVA_INT.scale(0, blockIndex)).address(),
                    FUTEX_WAIT_BITSET | FUTEX_PRIVATE_FLAG,
                    prevBlock,
                    0L,
                    0L,
                    mask
                );
                int errno = (int) errnoHandle.get(capture, 0L);
                if (result == -1 && errno != EAGAIN) {
                    throw new FutexException(errno);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateIndex(int index) throws IndexOutOfBoundsException {
        if (index >= this.blocksLength * BLOCK_SIZE || index < 0) {
            throw new IndexOutOfBoundsException(String.format("Index out of range: must be in [0; %d] but it is %d", blocksLength * BLOCK_SIZE - 1, index));
        }
    }

    public static class FutexException extends RuntimeException {
        public FutexException(int errno) {
            String errorMsg;
            switch (errno) {
                case EACCES:
                    errorMsg = "No read access to the memory of a futex word";
                    break;
                case EFAULT:
                    errorMsg = "uaddr did not point to a valid user-space address";
                    break;
                case EINVAL:
                    errorMsg = "uaddr does not point to a valid object—that is, "
                        + "the address is not four-byte-aligned";
                    break;
                case ENOSYS:
                    errorMsg = "Invalid operation specified in op";
                    break;
                default:
                    errorMsg = String.format("Unknown futex error: errno=%d", errno);
            };
            super(String.format("Futex error: %s", errorMsg));
        }
    }
}
