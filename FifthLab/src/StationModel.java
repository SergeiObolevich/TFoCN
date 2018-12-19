public class StationModel {
    private byte control=0;
    private byte destination=0;
    private byte source=0;
    private byte status=0;
    private byte monitor=0;
    private byte data=0;

    void setControl(byte control) {
        this.control = control;
    }
    void setDestination(byte destination) {
        this.destination = destination;
    }
    void setSource(byte source) {
        this.source = source;
    }
    void setStatus(byte status) {
        this.status = status;
    }
    void setMonitor(byte monitor) {
        this.monitor = monitor;
    }
    void setData(byte data){
        this.data = data;
    }
    byte getControl() {
        return control;
    }
    byte getDestination() {
        return destination;
    }
    byte getStatus() {
        return status;
    }
    byte getData() {
        return data;
    }
    void freeData() {
        data = 0;
    }
}