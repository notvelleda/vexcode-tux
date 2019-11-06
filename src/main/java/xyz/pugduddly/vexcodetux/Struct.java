package xyz.pugduddly.vexcodetux;

import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.lang.reflect.Array;

public class Struct {
    private static String unpackRLE(String rle) {
        String buf = "";
        String result = "";
        for (int i = 0; i < rle.length(); i ++) {
            char c = rle.charAt(i);
            if (c >= '0' && c <= '9') {
                buf += c;
            } else {
                int amount = 1;
                if (buf.length() != 0)
                    amount = Integer.parseInt(buf);
                buf = "";
                for (int j = 0; j < amount; j ++)
                    result += c;
            }
        }
        return result;
    }

    public static int calcSize(String fmt) {
        fmt = unpackRLE(fmt);
        int size = 0;
        for (int i = 0; i < fmt.length(); i ++) {
            switch (fmt.charAt(i)) {
                case 'x':
                case 'c':
                case 'b':
                case 'B':
                case '?':
                case 's':
                case 'p':
                    size += 1;
                    break;
                case 'h':
                case 'H':
                    size += 2;
                    break;
                case 'i':
                case 'I':
                case 'l':
                case 'L':
                case 'f':
                    size += 4;
                    break;
                case 'q':
                case 'Q':
                case 'd':
                    size += 8;
                    break;
            }
        }
        return size;
    }

    public static byte[] pack(String fmt, Object... args) {
        int size = calcSize(fmt);
        fmt = unpackRLE(fmt);
        ByteBuffer buf = ByteBuffer.allocate(size + 1);
        int argsIdx = 0;
        int subIdx = 0;
        int bufIdx = 0;
        for (int i = 0; i < fmt.length(); i ++) {
            Object toPut = null;
            if (argsIdx < args.length) {
                toPut = args[argsIdx];
                if (toPut != null && toPut.getClass().isArray()) {
                    toPut = Array.get(toPut, subIdx);
                } else {
                    subIdx = 0;
                }
            }
            boolean inc = false;
            switch (fmt.charAt(i)) {
                case '@':
                case '=':
                    buf.order(ByteOrder.nativeOrder());
                    break;
                case '<':
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    break;
                case '>':
                case '!':
                    buf.order(ByteOrder.BIG_ENDIAN);
                    break;
                case 'x':
                    buf.put(bufIdx, (byte) 0);
                    bufIdx += 1;
                    break;
                case 's':
                case 'p':
                case 'c':
                    buf.putChar(bufIdx, (char) toPut);
                    inc = true;
                    bufIdx += 1;
                    break;
                case 'b':
                case 'B':
                    buf.put(bufIdx, (byte) toPut);
                    inc = true;
                    bufIdx += 1;
                    break;
                case '?':
                    buf.put(bufIdx, (byte) ((boolean) toPut ? 1 : 0));
                    inc = true;
                    bufIdx += 1;
                    break;
                case 'h':
                case 'H':
                    buf.putShort(bufIdx, (short) toPut);
                    inc = true;
                    bufIdx += 2;
                    break;
                case 'i':
                case 'I':
                case 'l':
                case 'L':
                    buf.putInt(bufIdx, (int) toPut);
                    inc = true;
                    bufIdx += 4;
                    break;
                case 'f':
                    buf.putFloat(bufIdx, (float) toPut);
                    inc = true;
                    bufIdx += 4;
                    break;
                case 'q':
                case 'Q':
                    buf.putLong(bufIdx, (int) toPut);
                    inc = true;
                    bufIdx += 8;
                    break;
                case 'd':
                    buf.putDouble(bufIdx, (int) toPut);
                    inc = true;
                    bufIdx += 8;
                    break;
            }
            if (inc && args[argsIdx] != null && args[argsIdx].getClass().isArray() && subIdx < Array.getLength(args[argsIdx]) - 1) {
                subIdx ++;
            } else if (inc) {
                subIdx = 0;
                argsIdx ++;
            }
        }
        return removeElement(buf.array(), size);
    }

    private static byte[] removeElement(byte[] arr, int index) { 
        if (arr == null || index < 0 || index >= arr.length) {
            return arr;
        }

        byte[] newArr = new byte[arr.length - 1];
        for (int i = 0, k = 0; i < arr.length; i ++) {
            if (i == index) {
                continue; 
            }
            newArr[k ++] = arr[i]; 
        }

        return newArr;
    }

    public static Object[] unpack(String fmt, byte[] packed) {
        int size = calcSize(fmt);
        fmt = unpackRLE(fmt);
        ByteBuffer buf = ByteBuffer.allocate(size).put(packed);
        ArrayList<Object> result = new ArrayList<Object>();
        int bufIdx = 0;
        String stringBuf = "";
        for (int i = 0; i < fmt.length(); i ++) {
            switch (fmt.charAt(i)) {
                case '@':
                case '=':
                    buf.order(ByteOrder.nativeOrder());
                    break;
                case '<':
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    break;
                case '>':
                case '!':
                    buf.order(ByteOrder.BIG_ENDIAN);
                    break;
                case 'x':
                    bufIdx += 1;
                    break;
                case 's':
                case 'p':
                    stringBuf += (char) buf.get(bufIdx);
                    if ((i < fmt.length() - 1 && fmt.charAt(i + 1) == 'c') || (i == fmt.length() - 1)) {
                        result.add(stringBuf);
                        stringBuf = "";
                    }
                    bufIdx += 1;
                    break;
                case 'c':
                    result.add((char) buf.get(bufIdx));
                    bufIdx += 1;
                    break;
                case 'b':
                case 'B':
                    result.add(buf.get(bufIdx));
                    bufIdx += 1;
                    break;
                case '?':
                    result.add(buf.get(bufIdx) == 1);
                    bufIdx += 1;
                    break;
                case 'h':
                case 'H':
                    result.add(buf.getShort(bufIdx));
                    bufIdx += 2;
                    break;
                case 'i':
                case 'I':
                case 'l':
                case 'L':
                    result.add(buf.getInt(bufIdx));
                    bufIdx += 4;
                    break;
                case 'f':
                    result.add(buf.getFloat(bufIdx));
                    bufIdx += 4;
                    break;
                case 'q':
                case 'Q':
                    result.add(buf.getLong(bufIdx));
                    bufIdx += 8;
                    break;
                case 'd':
                    result.add(buf.getDouble(bufIdx));
                    bufIdx += 8;
                    break;
            }
        }
        return result.toArray(new Object[result.size()]);
    }
}