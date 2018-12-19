import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends Application {
    private static final String MARKER = "*";
    private Timer timer;
    private StationModel stationModel;
    private byte addresses[] = { 3 , 9, 27 };

    public static void main(String[] args){
        launch(args);
    }
    @Override
    public void start(Stage stage)  {
        ArrayList<StationForm> stationForms = new ArrayList<>();
        stationForms.add(new StationForm(addresses[0],addresses[1],(byte)1));
        stationForms.add(new StationForm(addresses[1],addresses[2], (byte)0));
        stationForms.add(new StationForm(addresses[2], addresses[0], (byte)0));

        stage = new Stage();
        Stage stage2 = new Stage();
        Stage stage3 = new Stage();

        stage.setX(440);
        stage.setY(320);
        stage2.setX(790);
        stage2.setY(320);
        stage3.setX(1140);
        stage3.setY(320);

        stationForms.get(0).start(stage);
        stationForms.get(1).start(stage2);
        stationForms.get(2).start(stage3);

        timer = new Timer();

        stationForms.get(0).getStartButton().setOnAction((event) -> {
            stationModel = new StationModel();
            for (int i = 0; i < 3; i++) {
                int currentStation = i;

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println(currentStation);
                        try {
                            stationRoutine(stationForms.get(currentStation), stationModel);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },1000, 1000);

                stationForms.get(0).getStartButton().setDisable(true);
            }
        });

        stationForms.get(0).getTokenText().textProperty().addListener(observable -> sendPackage(stationForms.get(0)));
        stationForms.get(1).getTokenText().textProperty().addListener(observable -> sendPackage(stationForms.get(1)));
        stationForms.get(2).getTokenText().textProperty().addListener(observable -> sendPackage(stationForms.get(2)));
    }

    public static void stationRoutine(StationForm stationForm, StationModel myStationModal) throws InterruptedException {
        if (myStationModal.getControl() == 0) {
            myStationModal.setSource(stationForm.getSourceAddress());
            myStationModal.setDestination(stationForm.getDestinationAddress());
            myStationModal.setMonitor(stationForm.getMainStation());
        } else {
            runOnUIThread(() -> {
                if (myStationModal.getDestination() == stationForm.getSourceAddress()) {
                    String data = "";
                    data += (char) myStationModal.getData();

                    myStationModal.setStatus((byte) 1);
                    stationForm.getOutputArea().appendText(data);
                }
                if (myStationModal.getStatus() == 1 && stationForm.getMainStation() == 1) {
                    myStationModal.setControl((byte) 0);
                    myStationModal.freeData();
                    myStationModal.setStatus((byte) 0);
                }
            });
        }
        runOnUIThread(() -> stationForm.getTokenText().setText(MARKER));
        Thread.sleep(1000);
        runOnUIThread(() -> stationForm.getTokenText().setText(""));
    }

    public void sendPackage(StationForm stationForm) {
        if (!stationForm.getInputArea().getText().equals("")
                && !stationForm.getDestination().equals("")
                && stationModel.getControl() == 0
                && stationModel != null) {
            if (stationForm.getTokenText().getText().equals("*")) {
                if (stationForm.getDestination().equals("3")) {
                    stationForm.setDestinationAddress(addresses[0]);
                }
                if (stationForm.getDestination().equals("9")) {
                    stationForm.setDestinationAddress(addresses[1]);
                }
                if (stationForm.getDestination().equals("27")) {
                    stationForm.setDestinationAddress(addresses[2]);
                }
                stationModel.setControl((byte) 1);
                stationModel.setDestination(stationForm.getDestinationAddress());
                stationModel.setSource(stationForm.getSourceAddress());
                stationModel.setMonitor(stationForm.getMainStation());

                String reduced = stationForm.getInputArea().getText().substring(1);
                stationModel.setData(stationForm.getInputArea().getText().getBytes()[0]);
                stationForm.getInputArea().setText(reduced);
            }
        }
    }

    private static void runOnUIThread(Runnable task) {
        if(task == null) throw new NullPointerException("Task can't be null");
        if(Platform.isFxApplicationThread()) task.run();
        else Platform.runLater(task);
    }
}