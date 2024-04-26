import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class EncodingUtils {
    public static double decodeDouble(ByteBuffer buf) throws BufferUnderflowException {
        byte startByte = buf.get();
        if (startByte == (byte) 0xFF) {
            // Encoded as integer
            return decodeLong(buf);
        }
        // Regular double
        byte[] doubleBytes = new byte[8];
        doubleBytes[0] = startByte;
        buf.get(doubleBytes, 1, 7);
        return ByteBuffer.wrap(doubleBytes).getDouble();
    }

    public static int decodeInt(ByteBuffer buf) throws BufferUnderflowException {
        return (int) decodeLong(buf, 5);
    }

    public static long decodeLong(ByteBuffer buf) throws BufferUnderflowException{
        return decodeLong(buf, 10);
    }

    private static long decodeLong(ByteBuffer buf, int maxBytes) throws BufferUnderflowException {
        long result = 0;
        int firstByte = Byte.toUnsignedInt(buf.get());
        if (firstByte == 0) {
            return 0;
        }
        boolean negative = (firstByte & 0b01000000) != 0;
        result |= firstByte & 0b00111111;
        if ((firstByte & 0b10000000) == 0) {
            return negative ? -result : result;
        }

        --maxBytes;
        long place = 6;
        while (maxBytes-- > 0) {
            long b = Byte.toUnsignedLong(buf.get());
            assert((result & (0b01111111L << place)) == 0);
            result |= (b & 0b01111111L) << place;
            place += 7;
            if ((b & 0b10000000) == 0) {
                return negative ? -result : result;
            }
        }
        throw new BufferUnderflowException();  // The result buffer underflowed
    }
}