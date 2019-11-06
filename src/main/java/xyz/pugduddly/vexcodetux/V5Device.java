package xyz.pugduddly.vexcodetux;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import com.fazecast.jSerialComm.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

// Basically just a Java port of the PROS device code lmao
public class V5Device {
    private static byte ACK_BYTE = 0x76;
    private static long DEFAULT_TIMEOUT = 2000l;
    private static byte CHANNEL_PIT = 0;
    private static byte CHANNEL_DOWNLOAD = 1;
    private static byte FUNCTION_UPLOAD = 1;
    private static byte FUNCTION_DOWNLOAD = 2;
    public static byte VID_USER = 1;
    public static byte VID_SYSTEM = 15;
    public static byte VID_RMS = 16;
    public static byte VID_PROS = 24;
    public static byte VID_MW = 32;
    public static byte DONT_RUN = 0;
    public static byte RUN_IMMEDIATELY = 1;
    public static byte RUN_SCREEN = 3;
    public static byte TARGET_DDR = 0;
    public static byte TARGET_FLASH = 1;
    public static byte TARGET_SCREEN = 2;
    private SerialPort port;

    public static SerialPort findV5SystemPort() {
        ArrayList<SerialPort> ports = new ArrayList<SerialPort>();
        SerialPort allPorts[] = SerialPort.getCommPorts();
        for (SerialPort port : allPorts) {
            if (port.getPortDescription​().contains("VEX") || port.getPortDescription​().contains("V5")) {
                ports.add(port);
            }
        }
        Collections.sort(ports, (SerialPort p1, SerialPort p2) -> p1.getSystemPortName().compareTo(p2.getSystemPortName()));
        if (ports.size() == 0)
            return null;
        else
            return ports.get(0);
    }

    public static SerialPort findV5UserPort() {
        ArrayList<SerialPort> ports = new ArrayList<SerialPort>();
        SerialPort allPorts[] = SerialPort.getCommPorts();
        for (SerialPort port : allPorts) {
            if (port.getPortDescription​().contains("VEX") || port.getPortDescription​().contains("V5")) {
                ports.add(port);
            }
        }
        Collections.sort(ports, (SerialPort p1, SerialPort p2) -> p1.getSystemPortName().compareTo(p2.getSystemPortName()));
        if (ports.size() == 0)
            return null;
        else
            return ports.get(1);
    }

    public V5Device(SerialPort port) {
        this.port = port;
        initPort(port);
    }

    public static void initPort(SerialPort port) {
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 125, 0);
        port.setBaudRate(115200);
        port.setNumDataBits​(8);
        port.setNumStopBits​(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
    }

    public void openPort() {
        initPort(this.port);
        this.port.openPort();
    }

    public void closePort() {
        this.port.closePort();
    }

    public boolean isPortOpen() {
        return this.port.isOpen();
    }

    public void getSystemStatus() throws VEXCommException {
        Object[] rx = this.txrxExtStruct((byte) 0x22, new byte[0], "<x12B3xBI12x", true, true, DEFAULT_TIMEOUT);
        System.out.println("System version: " + (byte) rx[0] + "." + (byte) rx[1] + "." + (byte) rx[2] + "-" + (byte) rx[3]);
        System.out.println("CPU0 version: " + (byte) rx[4] + "." + (byte) rx[5] + "." + (byte) rx[6] + "-" + (byte) rx[7]);
        System.out.println("CPU1 version: " + (byte) rx[8] + "." + (byte) rx[9] + "." + (byte) rx[10] + "-" + (byte) rx[11]);
        System.out.println("Touch version: " + (byte) rx[12]);
        System.out.println("System ID: " + (int) rx[13]);
    }

    public void getSystemVersion() throws VEXCommException {
        Object[] rx = this.txrxSimpleStruct((byte) 0xa4, ">8B", DEFAULT_TIMEOUT);
        System.out.println("System version: " + (byte) rx[0] + "." + (byte) rx[1] + "." + (byte) rx[2] + "-" + (byte) rx[3] + "." + (byte) rx[4]);
        String product = "missingno";
        String productFlags = "None";
        switch ((byte) rx[5]) {
            case 0x10:
                product = "Brain";
                break;
            case 0x11: 
                product = "Controller";
                switch ((byte) rx[6]) {
                    case 0x02:
                        productFlags = "Connected";
                        break;
                }
                break;
        }
        System.out.println("Product: " + product);
        System.out.println("Product Flags: " + productFlags);
    }

    public Message querySystem() throws VEXCommException {
        return this.txrxSimplePacket((byte) 0x21, 0x0a, DEFAULT_TIMEOUT);
    }

    /* FILE TRANSFER */

    public void writeProgram(File file, String remoteName, int slot, String version, String icon, String description, byte runAfter) throws IOException, VEXCommException {
        this.writeProgram(file, remoteName, slot, version, icon, description, runAfter, TARGET_FLASH, (byte) 0, false);
    }

    private static String generateIniFile(String remoteName, int slot, String version, String icon, String description, String date) {
        String ini = "[program]\n";
        ini += "version: " + version + "\n";
        ini += "name: " + remoteName + "\n";
        ini += "slot: " + slot + "\n";
        ini += "icon: " + icon + "\n";
        ini += "description: " + description + "\n";
        ini += "date: " + date + "\n";
        return ini;
    }

    public void writeProgram(File file, String remoteName, int slot, String version, String icon, String description, byte runAfter, byte target, byte quirk, boolean compress) throws IOException, VEXCommException {
        String remoteBase = "slot_" + (slot + 1);
        if (target == TARGET_DDR) {
            this.writeFile(file, remoteBase + ".bin", target, runAfter, "", (byte) 0, compress);
            return;
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        String ini = this.generateIniFile(remoteName, slot, version, icon, description, df.format(new Date()));
        if ((quirk & 0xff) == 1) {
            this.writeFile(file, remoteBase + ".bin", target, runAfter, "", (byte) 0, compress);
            this.writeFile(ini.getBytes(StandardCharsets.US_ASCII), "ini", remoteBase + ".ini", target, DONT_RUN, "", (byte) 0, compress);
        } else if ((quirk & 0xff) == 0) {
            //this.executeProgramFile("", VID_USER, false);
            this.writeFile(ini.getBytes(StandardCharsets.US_ASCII), "ini", remoteBase + ".ini", target, DONT_RUN, "", (byte) 0, compress);
            this.writeFile(file, remoteBase + ".bin", target, runAfter, "", (byte) 0, compress);
        } else {
            throw new VEXCommException("Unknown quirk option: " + quirk);
        }
    }

    public Message executeProgramFile(String fileName, byte vid, boolean run) throws VEXCommException {
        byte options = 0;
        if (run) options |= 0x80;
        byte[] txPayload = Struct.pack("<2B24B", vid, options, padArray(fileName.getBytes(StandardCharsets.US_ASCII), 24));
        return this.txrxExtPacket((byte) 0x18, txPayload, 0, true, true, DEFAULT_TIMEOUT);
    }

    public void writeFile(File file, String remoteFile, byte target, byte runAfter, String linkedFilename, byte linkedVid, boolean compress) throws IOException, VEXCommException {
        this.writeFile(Files.readAllBytes(Paths.get(file.toString())), Utils.getExtension(file), remoteFile, target, runAfter, linkedFilename, linkedVid, compress);
    }

    public void writeFile(byte[] fileBytes, String type, String remoteFile, byte target, byte runAfter, String linkedFilename, byte linkedVid, boolean compress) throws IOException, VEXCommException {
        // TODO: add gzip compression for files
        int crc32 = (int) CRC.VEX_CRC32.compute(fileBytes);
        int addr = 0x03800000;
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        Object[] ftMeta = this.ftInitialize(remoteFile, FUNCTION_UPLOAD, target, VID_USER, true, (byte) 0, fileBytes.length, addr, crc32, type, timestamp, 0x01000000);
        if (linkedFilename.length() != 0)
            this.ftSetLink(linkedFilename, linkedVid, (byte) 0);
        assert (int) ftMeta[1] /* file size */ >= fileBytes.length;
        if (remoteFile.length() > 24)
            remoteFile = remoteFile.substring(0, 24);
        short maxPacketSize = (short) (((short) ftMeta[0] /* max packet size */) / 2);
        for (int i = 0; i < fileBytes.length; i += maxPacketSize) {
            int packetSize = maxPacketSize;
            if (i + maxPacketSize > fileBytes.length)
                packetSize = fileBytes.length - i;
            this.ftWrite(addr + i, Arrays.copyOfRange(fileBytes, i, i + packetSize));
        }
        this.ftComplete(runAfter);
    }

    public void readFile(File file, String remoteFile, byte vid, byte target) throws VEXCommException, IOException {
        Object[] metadata = this.getFileMetadata(remoteFile, vid, (byte) 0);
        int addr = (int) metadata[2] /* addr */;
        Object[] ftMeta = this.ftInitialize(remoteFile, FUNCTION_DOWNLOAD, target, vid, true, (byte) 0, 0, addr, 0, Utils.getExtension(remoteFile), (int) metadata[5] /* timestamp */ - 946706400, 0);
        int fileLen = (int) ftMeta[1] /* fileSize */;
        short maxPacketSize = (short) ftMeta[0] /* maxPacketSize */;
        OutputStream stream = new FileOutputStream(file);
        for (int i = 0; i < fileLen; i += maxPacketSize) {
            short packetSize = maxPacketSize;
            if (i + maxPacketSize > fileLen)
                packetSize = (short) (fileLen - i);
            stream.write(this.ftRead(addr + i, (short) packetSize));
        }
        stream.close();
        this.ftComplete((byte) 0);
    }

    public byte[] readFile(String remoteFile, byte vid, byte target) throws VEXCommException, IOException {
        ArrayList<byte[]> arr = new ArrayList<byte[]>();
        Object[] metadata = this.getFileMetadata(remoteFile, vid, (byte) 0);
        int addr = (int) metadata[2] /* addr */;
        Object[] ftMeta = this.ftInitialize(remoteFile, FUNCTION_DOWNLOAD, target, vid, true, (byte) 0, 0, addr, 0, Utils.getExtension(remoteFile), (int) metadata[5] /* timestamp */ - 946706400, 0);
        int fileLen = (int) ftMeta[1] /* fileSize */;
        short maxPacketSize = (short) ftMeta[0] /* maxPacketSize */;
        for (int i = 0; i < fileLen; i += maxPacketSize) {
            short packetSize = maxPacketSize;
            if (i + maxPacketSize > fileLen)
                packetSize = (short) (fileLen - i);
            arr.add(this.ftRead(addr + i, (short) packetSize));
        }
        this.ftComplete((byte) 0);
        byte[] out = new byte[0];
        for (int i = 0; i < arr.size(); i ++) {
            out = joinArrays(out, arr.get(i));
        }
        return out;
    }

    private Object[] getFileMetadata(String fileName, byte vid, byte options) throws VEXCommException {
        byte[] txPayload = Struct.pack("<2B24B", vid, options, padArray(fileName.getBytes(StandardCharsets.US_ASCII), 24));
        Object[] rx = this.txrxExtStruct((byte) 0x19, txPayload, "<B3L4sLL24s", true, true, DEFAULT_TIMEOUT);
        rx[5] = ((int) rx[5]) + 946706400;
        return rx; // linkedVid (byte), size (int), addr (int), crc (int), type (String), timestamp (int), version (int), linkedFilename (String)
    }

    /* LOW LEVEL FILE TRANSFER */

    private Object[] ftInitialize(String name, byte function, byte target, byte vid, boolean overwrite, byte options, int length, int addr, int crc, String type, int timestamp, int version) throws VEXCommException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
        if (overwrite) options |= 1;
        timestamp -= 946706400;
        byte[] txPayload = Struct.pack("<4B3I4B2I24B", function, target, vid, options, length, addr, crc, padArray(typeBytes, 4), timestamp, version, padArray(nameBytes, 24));
        Object[] rx = this.txrxExtStruct((byte) 0x11, txPayload, "<H2I", true, true, DEFAULT_TIMEOUT * 5);
        return rx; // maxPacketSize (short), fileSize (int), crc (int)
    }

    private Message ftSetLink(String linkName, byte vid, byte options) throws VEXCommException {
        byte[] linkNameBytes = linkName.getBytes(StandardCharsets.US_ASCII);
        byte[] txPayload = Struct.pack("<2B24B", vid, options, padArray(linkNameBytes, 24));
        return this.txrxExtPacket((byte) 0x15, txPayload, 0, true, true, DEFAULT_TIMEOUT);
    }

    private Message ftWrite(int addr, byte[] payload) throws VEXCommException {
        if (payload.length % 4 != 0) {
            payload = joinArrays(payload, new byte[4 - (payload.length % 4)]);
        }
        byte[] txPayload = Struct.pack("<I" + payload.length + "B", addr, payload);
        return this.txrxExtPacket((byte) 0x13, txPayload, 0, true, true, DEFAULT_TIMEOUT);
    }

    private byte[] ftRead(int addr, short numBytes) throws VEXCommException {
        short actualNumBytes = (short) (numBytes + (numBytes % 4 == 0 ? 0 : 4 - numBytes % 4));
        byte[] txPayload = Struct.pack("<IH", addr, actualNumBytes);
        Object[] struct = this.txrxExtStruct((byte) 0x14, txPayload, "<I" + actualNumBytes + "B", true, false, DEFAULT_TIMEOUT);
        byte[] ret = new byte[numBytes];
        for (int i = 0; i < numBytes; i ++)
            ret[i] = (byte) struct[i + 1];
        return ret;
    }

    private Message ftComplete(byte options) throws VEXCommException {
        byte[] txPayload = Struct.pack("<B", options);
        return this.txrxExtPacket((byte) 0x12, txPayload, 0, true, true, DEFAULT_TIMEOUT * 10);
    }

    /* STRUCTS */

    private Object[] txrxExtStruct(byte command, byte[] txData, String unpackFmt, boolean checkLength, boolean checkAck, long timeout) throws VEXCommException {
        Message rx = this.txrxExtPacket(command, txData, Struct.calcSize(unpackFmt), checkLength, checkAck, timeout);
        return Struct.unpack(unpackFmt, rx.getPayload());
    }

    private Object[] txrxSimpleStruct(byte command, String unpackFmt, long timeout) throws VEXCommException {
        Message rx = this.txrxSimplePacket(command, Struct.calcSize(unpackFmt), timeout);
        return Struct.unpack(unpackFmt, rx.getPayload());
    }

    /* LOW LEVEL PACKET FUNCTIONS */

    private byte[] writePacket(byte command, byte[] data) throws VEXCommException {
        if (!this.port.isOpen())
            throw new VEXCommException("Port is not open");

        byte[] packet;

        if (data.length > 0)
            packet = joinArrays(this.formSimplePacket(command), data);
        else
            packet = this.formSimplePacket(command);
        
        int bytesToRead = this.port.bytesAvailable​();
        if (bytesToRead > 0)
            this.port.readBytes(new byte[bytesToRead], bytesToRead);
        
        for (int i = 0; i < packet.length; i ++) { // Janky workaround for bug in jSerialComm
            this.port.writeBytes(new byte[] { packet[i] }, 1);
            /*try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }

        return packet;
    }
    
    private Packet readPacket(long timeout) throws VEXCommException {
        if (!this.port.isOpen())
            throw new VEXCommException("Port is not open");
        
        long startTime = System.currentTimeMillis();
        byte[] responseHeader = new byte[] {
            (byte) 0xaa,
            (byte) 0x55
        };
        int stackIdx = 0;
        ArrayList<Byte> rx = new ArrayList<Byte>();

        while ((rx.size() > 0 || System.currentTimeMillis() - startTime < timeout) && stackIdx < responseHeader.length) {
            byte[] arr = new byte[1];
            if (this.port.bytesAvailable() > 0) {
                if (this.port.readBytes(arr, 1) > 0) {
                    byte b = arr[0];
                    if (stackIdx < responseHeader.length && b == responseHeader[stackIdx]) {
                        stackIdx ++;
                        rx.add(b);
                    } else {
                        System.out.println("Tossing received bytes because 0x" + Integer.toHexString(b) + " didn't match");
                        stackIdx = 0;
                        rx.clear();
                    }
                }
            }
        }

        if (System.currentTimeMillis() - startTime >= timeout)
            throw new VEXCommException("Reached timeout while waiting for response (header)");

        if ((byte) rx.get(0) == responseHeader[0] && (byte) rx.get(1) == responseHeader[1]) {
            byte[] arr = new byte[1];
            byte command;
            int payloadLength;

            startTime = System.currentTimeMillis();
            while (this.port.bytesAvailable() < 1 && System.currentTimeMillis() - startTime < timeout) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (System.currentTimeMillis() - startTime >= timeout)
                throw new VEXCommException("Reached timeout while waiting for response (command)");
            this.port.readBytes(arr, 1);
            rx.add(arr[0]);
            command = arr[0];

            startTime = System.currentTimeMillis();
            while (this.port.bytesAvailable() < 1 && System.currentTimeMillis() - startTime < timeout) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (System.currentTimeMillis() - startTime >= timeout)
                throw new VEXCommException("Reached timeout while waiting for response (payloadLength)");
            this.port.readBytes(arr, 1);
            rx.add(arr[0]);
            payloadLength = (int) arr[0];

            if (command == 0x56 && (payloadLength & 0x80) == 0x80) {
                startTime = System.currentTimeMillis();
                while (this.port.bytesAvailable() < 1 && System.currentTimeMillis() - startTime < timeout) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (System.currentTimeMillis() - startTime >= timeout)
                    throw new VEXCommException("Reached timeout while waiting for response (payloadLength)");
                this.port.readBytes(arr, 1);
                rx.add(arr[0]);
                payloadLength = ((payloadLength & 0x7f) << 8) + arr[0];
            }

            byte[] payload = new byte[payloadLength];
            startTime = System.currentTimeMillis();
            while (this.port.bytesAvailable() < payloadLength && System.currentTimeMillis() - startTime < timeout) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (System.currentTimeMillis() - startTime >= timeout)
                throw new VEXCommException("Reached timeout while waiting for response (payload)");
            this.port.readBytes(payload, payloadLength);
            for (int i = 0; i < payloadLength; i ++)
                rx.add(payload[i]);

            return new Packet(command, payload, ArrayUtils.toPrimitive(rx.toArray(new Byte[rx.size()])));
        } else {
            throw new VEXCommException("Couldn't find response header");
        }
    }

    private Message txrxPacket(byte command, byte[] txData, long timeout) throws VEXCommException {
        byte[] tx = this.writePacket(command, txData);
        Packet rx = this.readPacket(timeout);
        Message msg = new Message(rx.getRaw(), tx, rx.getCommand(), rx.getPayload());
        return msg;
    }

    private Message txrxSimplePacket(byte command, int rxLen, long timeout) throws VEXCommException {
        Message msg = this.txrxPacket(command, new byte[0], timeout);
        if (msg.getCommand() != command)
            throw new VEXCommException("Received command doesn't match sent command.");
        if (msg.getPayload().length != rxLen)
            throw new VEXCommException("Received data doesn't match expected length");
        return msg;
    }

    private byte[] formSimplePacket(byte msg) {
        return new byte[] { 
            (byte) 0xc9,
            (byte) 0x36,
            (byte) 0xb8,
            (byte) 0x47,
            msg
        };
    }

    /* LOW LEVEL EXTENDED PACKET FUNCTIONS */

    private Message rxExtPacket(Message msg, byte command, int rxLength, boolean checkLength, boolean checkAck) throws VEXCommException {
        assert msg.getCommand() == (byte) 0x56;

        long crc = CRC.VEX_CRC16.compute(msg.getRX());
        if (crc != 0) {
            throw new VEXCommException("CRC of message didn't match 0: " + crc);
        }

        assert msg.getPayload()[0] == command;

        byte[] payload = removeElement(msg.getPayload(), 0);
        payload = removeElement(payload, payload.length - 1);
        payload = removeElement(payload, payload.length - 1);

        if (checkAck) {
            String reason = null;

            switch (payload[0]) {
                case (byte) 0xff: reason = "General NACK"; break;
                case (byte) 0xce: reason = "CRC error on received packet"; break;
                case (byte) 0xd0: reason = "Payload too small"; break;
                case (byte) 0xd1: reason = "Request transfer size too large"; break;
                case (byte) 0xd2: reason = "Program CRC error"; break;
                case (byte) 0xd3: reason = "Program file error"; break;
                case (byte) 0xd4: reason = "Attempted to download/upload unititialized"; break;
                case (byte) 0xd5: reason = "Initialization invalid for this function"; break;
                case (byte) 0xd6: reason = "Data not a multiple of 4 bytes"; break;
                case (byte) 0xd7: reason = "Packet address does not match expected"; break;
                case (byte) 0xd8: reason = "Data downloaded does not match initial length"; break;
                case (byte) 0xd9: reason = "Directory entry does not exist"; break;
                case (byte) 0xda: reason = "Max user files, no more room for another user program"; break;
                case (byte) 0xdb: reason = "User file exists"; break;
            }

            if (reason != null) {
                throw new VEXCommException("Device NACKed with reason: " + reason);
            } else if (payload[0] != this.ACK_BYTE) {
                throw new VEXCommException("Device didn't ACK");
            }

            payload = removeElement(payload, 0);
        }

        if (payload.length != rxLength && checkLength) {
            throw new VEXCommException("Received length doesn't match " + rxLength + " (got " + payload.length + ")");
        }

        return new Message(msg.getRX(), msg.getTX(), msg.getCommand(), payload);
    }
    
    private Message txrxExtPacket(byte command, byte[] txData, int rxLength, boolean checkLength, boolean checkAck, long timeout) throws VEXCommException {
        byte[] txPayload = this.formExtendedPayload(command, txData);
        Message rx = this.txrxPacket((byte) 0x56, txPayload, timeout);
        return this.rxExtPacket(rx, command, rxLength, checkLength, checkAck);
    }

    private byte[] formExtendedPayload(byte msg, byte[] payload) {
        byte[] payloadLength = new byte[] { (byte) payload.length };
        assert payload.length < 0x7fff;
        if (payload.length >= 0x80) {
            payloadLength = new byte[] { (byte) ((payload.length >> 8) | 0x80), (byte) (payload.length & 0xff) };
        }
        byte[] packet = joinArrays(new byte[] { msg }, payloadLength, payload);
        int crc = (int) CRC.VEX_CRC16.compute(joinArrays(this.formSimplePacket((byte) 0x56), packet));
        packet = joinArrays(packet, new byte[] { (byte) (crc >> 8), (byte) (crc & 0xff) });
        assert CRC.VEX_CRC16.compute(joinArrays(this.formSimplePacket((byte) 0x56), packet)) == 0;
        return packet;
    }

    /* UTIL FUNCTIONS */

    private static byte[] joinArrays(byte[]... args) {
        int byteCount = 0;
        for (byte[] arg : args) {
            byteCount += arg.length;
        }
        byte[] returnArray = new byte[byteCount];
        int offset = 0;
        for (byte[] arg : args) {
            System.arraycopy(arg, 0, returnArray, offset, arg.length);
            offset += arg.length;
        }
        return returnArray;
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

    private static byte[] padArray(byte[] arr, int length) {
        byte[] ret = new byte[length];
        for (int i = 0; i < arr.length; i ++)
            ret[i] = arr[i];
        return ret;
    }
}

class Packet {
    private byte command;
    private byte[] payload;
    private byte[] raw;

    public Packet(byte command, byte[] payload, byte[] raw) {
        this.command = command;
        this.payload = payload;
        this.raw = raw;
    }

    public byte getCommand() {
        return this.command;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public byte[] getRaw() {
        return this.raw;
    }
}