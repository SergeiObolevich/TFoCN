package controller;

import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import util.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class SerialPortController {
    public enum PackageState { GOOD, DAMAGED, WRONG_DESTINATION_ADDRESS, NONE }

    public static final byte XON_CHARACTER = 17;
    public static final byte XOFF_CHARACTER = 19;

    private static final int PACKAGE_DATA_OFFSET = 4;
    private static final int PACKAGE_DATA_SIZE_OFFSET = 3;
    private static final int PACKAGE_SOURCE_OFFSET = 2;
    private static final int PACKAGE_DESTINATION_OFFSET = 1;
    private static final int FCS_DEFAULT = 255;
    private static final int PACKAGE_DATA_LENGTH = 251;
    private static final byte PACKAGE_BEGINNING_FLAG = 'a';

    private static final int BAUD_RATE = SerialPort.BAUDRATE_9600;
    private static final int DATA_BITS = SerialPort.DATABITS_8;
    private static final int PARITY = SerialPort.PARITY_NONE;
    private static final int STOP_BITS = SerialPort.STOPBITS_1;

    private SerialPort serialPort = null;
    private byte address = 0;
    private byte senderAddress = 0;
    private SerialPortEventListener serialPortEventListener;
    private boolean XonXoffFlowControlEnable = false;
    private boolean XoffIsSet = true;
    private int receivedBytes = 0;
    private int sentBytes = 0;

    private PackageState packageState = PackageState.NONE;

    public String bytesToBits(byte[] bytes) {
        String binary="";
        for(int i = 0; i < bytes.length; i++) {
            binary += String.format("%8s", Integer.toBinaryString(bytes[i] & 0xFF)).
                    replace(' ', '0');
        }
        return binary;
    }

    public String stuffData(String data) {
        int zeroCounter = 0;
        int unitCounter = 0;
        String result = "";
        for(int i = 0; i < data.length(); i++) {
            if(data.charAt(i) == '0') {
                zeroCounter++;
                unitCounter = 0;
                result = result + data.charAt(i);
            } else {
                result = result + data.charAt(i);
                zeroCounter = 0;
                unitCounter++;
            }
            if(zeroCounter == 0 && unitCounter == 2) {
                result = result + '0';
                zeroCounter = 0;
                unitCounter = 0;
            }
        }
        if(result.length()%8 != 0) {
            String nulls="";
            for(int i = 0; i < 8 - (result.length()%8); i++) {
                nulls+="0";
            }
            result = result +  nulls;
        }
        return result;
    }

    public String unstuffData(String data) {
        int zeroCounter = 0;
        int unitCounter = 0;
        String result = "";
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == '0') {
                zeroCounter++;
                unitCounter = 0;
                result = result + data.charAt(i);
            } else {
                zeroCounter = 0;
                unitCounter++;
                result = result + data.charAt(i);
            }
            if(zeroCounter == 0 && unitCounter == 2) {
                if(i == data.length() - 1)
                    break;
                if (i != data.length() - 2)
                    result = result + data.charAt(i + 2);
                i = i + 2;
                if (data.charAt(i) == '1') {
                    unitCounter = 1;
                    zeroCounter = 0;
                }
                else {
                    unitCounter = 0;
                    zeroCounter = 1;
                }
            }
        }
        if(result.length() % 8 != 0) {
            String nulls = "";
            for(int i = 0; i < result.length() % 8; i++){
                nulls+="0";
            }
            result=result.substring(0, result.length() - nulls.length());
        }
        return result;
    }

    public byte[] getBytes(String data) {
        byte[] bytes = new byte[data.length()/8];
        int i = 0;
        int k = 0;
        for(int j = 8; j < data.length() + 1; j+=8){
            String str = data.substring(i, j);
            i+=8;
            int number = Integer.parseInt(str,2);
            bytes[k] = (byte)number;
            k++;
        }
        return bytes;
    }

    public static ArrayList<String> getPortNames() {
        return new ArrayList<>(Arrays.asList(SerialPortList.getPortNames()));
    }

    @Deprecated
    public static byte buildAddress(String portName) {
        if(portName == null || portName.isEmpty()) {
            throw new NullPointerException("The value of param portName must be a non-null string!");
        }
        return Byte.parseByte(portName.substring(3));
    }

    public void setSerialPort(SerialPort serialPort, byte address, boolean XonXoffFlowControlEnable) throws SerialPortException {
        this.serialPort = serialPort;
        this.XonXoffFlowControlEnable = XonXoffFlowControlEnable;

        serialPort.openPort();

        this.address = address;

        serialPort.setParams(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);

        serialPort.addEventListener(this.serialPortEventListener);
    }

    public SerialPortController(SerialPortEventListener serialPortEventListener) {
        this.serialPortEventListener = serialPortEventListener;
    }

    public boolean getXOffState() {
        return this.XoffIsSet;
    }

    public byte getAddress() {
        return address;
    }

    public byte getSenderAddress() {
        return senderAddress;
    }

    public String getName() {
        return serialPort.getPortName();
    }

    public void setReceivedBytes(int receivedBytes) {
        this.receivedBytes = receivedBytes;
    }

    public int getReceivedBytes() {
        return receivedBytes;
    }

    public void setSentBytes(int sentBytes) {
        this.sentBytes = sentBytes;
    }

    public int getSentBytes() {
        return sentBytes;
    }

    public PackageState getPackageState() {
        return packageState;
    }

    public void setXoffState(boolean value) {
        this.XoffIsSet = value;
    }

    public boolean getXonXoffFlowControlMode() {
        return XonXoffFlowControlEnable;
    }

    public boolean portIsOpened() {
        return serialPort != null;
    }

    public boolean closeSerialPort() throws SerialPortException{
        this.address = 0;
        this.sentBytes = 0;
        this.receivedBytes = 0;
        boolean result = this.serialPort.closePort();
        this.serialPort = null;

        return result;
    }

    public void clearBuffer() throws SerialPortException {
        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not bu null!");
        }

        this.serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
    }

    public byte[] decomposePackage(byte[] source) {
        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not be null!");
        }

        if(source.length < 5) {
            throw new IllegalArgumentException("Package was damaged. It's size is too small!");
        }

        byte[] tempBytes = new byte[source.length - 1];
        for(int i = 0, j = 1; j < source.length; i++, j++) {
            tempBytes[i] = source[j];
        }

        String bits = "";
        bits += bytesToBits(tempBytes);
        String mask = unstuffData(bits);
        tempBytes = getBytes(mask);

        byte[] result = new byte[tempBytes.length];
        result[0] = source[0];

        for(int i = 1, j = 0; j < tempBytes.length - 1; i++, j++) {
            result[i] = tempBytes[j];
        }

        return result;
    }

    public ArrayList<byte[]> composePackages(byte[] source, byte destinationAddress) {
        if(this.address == destinationAddress) {
            throw new IllegalArgumentException("Source and destination address can't be equal!");
        }

        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not bu null!");
        }

        ArrayList<byte[]> data = new ArrayList<>();
        ArrayList<byte[]> packageForStuffing = new ArrayList<>();
        ArrayList<byte[]> stuffedPackage = new ArrayList<>();
        ArrayList<byte[]> donePackage = new ArrayList<>();
        int untrackedDataSize = 0;
        int amountOfPackages = source.length / PACKAGE_DATA_LENGTH;
        if ((source.length - amountOfPackages * PACKAGE_DATA_LENGTH) > 0) {
            untrackedDataSize = source.length - amountOfPackages * PACKAGE_DATA_LENGTH;
            amountOfPackages++;
        }
        int i = 0, k = 0;
        byte[] dump;
        if(amountOfPackages == 1 && untrackedDataSize < PACKAGE_DATA_LENGTH){
            dump = new byte[untrackedDataSize];
            for (byte var : source) {
                dump[i] = var;
                i++;
                if (i == untrackedDataSize)
                    data.add(dump);
            }
        }
        else {
            dump = new byte[PACKAGE_DATA_LENGTH];
            for (byte var : source) {
                dump[i] = var;
                i++;
                if (k == amountOfPackages && i == untrackedDataSize)
                    data.add(dump);
                if (i == PACKAGE_DATA_LENGTH) {
                    data.add(dump);
                    k++;
                    i = 0;
                    if ((k == amountOfPackages - 1) && (untrackedDataSize != 0)) {
                        dump = new byte[untrackedDataSize];
                        k++;
                    } else
                        dump = new byte[PACKAGE_DATA_LENGTH];
                }
            }
        }
        int dataIndexStart;
        for(int j = 0; j < amountOfPackages; j++){
            byte[] bytePackage = new byte[data.get(j).length + 4];
            bytePackage[0] = destinationAddress;
            bytePackage[1] = this.address;
            bytePackage[2] = (byte)data.get(j).length;
            dataIndexStart = 3;
            for(i = 0; i < data.get(j).length; i++){
                bytePackage[dataIndexStart] = data.get(j)[i];
                dataIndexStart++;
            }
            bytePackage[data.get(j).length + 3] = (byte)FCS_DEFAULT;
            packageForStuffing.add(bytePackage);
        }
        for (byte[] sell: packageForStuffing) {
            String bits = bytesToBits(sell);
            String stuffedBits = stuffData(bits);
            stuffedPackage.add((getBytes(stuffedBits)));
        }
        int j;
        for(i = 0; i < amountOfPackages; i++){
            byte[] temp = new byte[stuffedPackage.get(i).length + 1];
            temp[0] = PACKAGE_BEGINNING_FLAG;
            for(j = 0, k = 1; j < stuffedPackage.get(i).length; j++, k++){
                temp[k] = stuffedPackage.get(i)[j];
            }
            donePackage.add(temp);
        }
        return donePackage;
    }

    public boolean sendXon(byte destination) throws SerialPortException {
        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not bu null!");
        }

        if(destination < 0) {
            throw new IllegalArgumentException("The value of param destination is invalid!");
        }

        byte[] xon = { XON_CHARACTER };
        byte[] xonPackage = this.composePackages(xon, destination).get(0);
        return this.serialPort.writeBytes(xonPackage);
    }

    public boolean sendXoff(byte destination) throws SerialPortException {
        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not bu null!");
        }

        if(destination < 0) {
            throw new IllegalArgumentException("The value of param destination is invalid!");
        }

        byte[] xoff = { XOFF_CHARACTER };
        byte[] xoffPackage = composePackages(xoff, destination).get(0);
        return this.serialPort.writeBytes(xoffPackage);
    }

    public byte[] read(int byteCount) throws SerialPortException {
        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not be null!");
        }

        byte[] result = this.serialPort.readBytes(byteCount);

        if (result[0] != PACKAGE_BEGINNING_FLAG) {
            this.packageState = PackageState.DAMAGED;
            return result;
        }

        byte[] decomposedSource;

        try {
            decomposedSource = decomposePackage(result);
        } catch (Exception exception) {
            this.packageState = PackageState.DAMAGED;
            Utils.runOnUIThread(() -> Utils.showExceptionDialog(exception));
            return result;
        }

        if(this.address != decomposedSource[PACKAGE_DESTINATION_OFFSET]) {
            this.packageState = PackageState.WRONG_DESTINATION_ADDRESS;
            return decomposedSource;
        }

        this.senderAddress = decomposedSource[PACKAGE_SOURCE_OFFSET];

        byte[] data = this.retrievePackageData(decomposedSource);

        if(this.XonXoffFlowControlEnable) {
            if (data[0] != XON_CHARACTER && data[0] != XOFF_CHARACTER) {
                this.receivedBytes += result.length;
            }
        } else {
            this.receivedBytes += result.length;
        }

        packageState = PackageState.GOOD;

        return data;
    }

    private byte[] retrievePackageData(byte[] source) {
        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not bu null!");
        }

        int dataSize = Utils.byteToUnsignedInt(source[PACKAGE_DATA_SIZE_OFFSET]);

        byte[] data = new byte[dataSize];

        for(int i = PACKAGE_DATA_OFFSET, j = 0; j < dataSize; i++, j++) {
            data[j] = source[i];
        }

        return data;
    }

    private boolean checkFCS(byte[] message) {
        return message[message.length - 1] == (byte)SerialPortController.FCS_DEFAULT;
    }

    public boolean write(ArrayList<byte[]> messages) throws SerialPortException  {
        if(serialPort == null) {
            throw new NullPointerException("The value of param serialPort can not bu null!");
        }

        boolean result = false;

        if(!(this.XoffIsSet && this.XonXoffFlowControlEnable)) {
            for(byte[] message : messages) {
                result = this.serialPort.writeBytes(message);

                if (result) {
                    this.sentBytes += message.length;
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
        }
        return result;
    }
}