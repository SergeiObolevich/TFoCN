import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class CollisionController {
    private static final int MAX_ATTEMPTS = 10;
    private static final char COLLISION_SYMBOL = 'X';
    private static final int COLLISION_DURATION = 200;

    private byte buffer = 0;

    @FXML private Button sendButton;
    @FXML private Button clearButton;
    @FXML private TextArea inputZone;
    @FXML private TextArea outputZone;
    @FXML private TextArea debugZone;

    private void send(byte data) {
        this.buffer = data;
    }

    private boolean collision() {
        return (System.currentTimeMillis() % 2) == 1;
    }

    private boolean busy() {
        return (System.currentTimeMillis() % 2) == 1;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int random(int n) {
        return (int)Math.round(Math.random() * Math.pow(3, n));
    }

    private static void runOnUIThread(Runnable task) {
        if(task == null) throw new NullPointerException("Task can't be null.");
        if(Platform.isFxApplicationThread()) task.run();
        else Platform.runLater(task);
    }

    private class SendTask extends Task<Void> {
        @Override
        protected Void call() {
            runOnUIThread(() -> {
                inputZone.setEditable(false);
                sendButton.setDisable(true);
                clearButton.setDisable(true);
            });

            byte[] line = inputZone.getText().getBytes();

            for (byte symbol: line) {
                StringBuilder collisions = new StringBuilder();
                int attempts = 0;
                boolean sending = true;

                while(sending) {
                    while (busy());
                    send(symbol);
                    sleep(COLLISION_DURATION);

                    if(collision()) {
                        collisions.append(COLLISION_SYMBOL);
                        attempts += 1;

                        if (attempts > MAX_ATTEMPTS) {
                            sending = false;
                        } else {
                            sleep(random(attempts));
                        }
                    } else {
                        runOnUIThread(() -> {
                            outputZone.appendText((char)symbol + "");
                            debugZone.appendText(collisions + "\n");
                        });
                        sending = false;
                    }
                }
            }
            runOnUIThread(() -> {
                inputZone.setEditable(true);
                sendButton.setDisable(false);
                clearButton.setDisable(false);
            });
            inputZone.setText("");
            outputZone.appendText("\n");
            return null;
        }
    }

    @FXML protected void onClear(ActionEvent event) {
        inputZone.setText("");
        outputZone.setText("");
        debugZone.setText("");
    }

    @FXML protected void onSend(ActionEvent event) {
        new Thread(new SendTask()).start();
    }
}