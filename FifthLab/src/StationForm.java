import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class StationForm {
    private byte mainStation;
    private byte sourceAddress;
    private byte destinationAddress;

    private String[] destinations = { "3", "9", "27" };
    private String destination = "";

    private TextArea inputArea;
    private TextArea outputArea;
    private Text tokenText;
    private Text sourceText;
    private Button start;

    StationForm(byte source, byte dest, byte mainStation) {
        this.sourceAddress = source;
        this.destinationAddress = dest;
        this.mainStation = mainStation;

        sourceText = new Text();

        if(source == 3)
            sourceText.setText("3");
        else if(source == 9)
            sourceText.setText("9");
        else if (source == 27)
            sourceText.setText("27");
        this.mainStation = mainStation;
    }

    public byte getSourceAddress(){
        return sourceAddress;
    }
    public byte getDestinationAddress(){
        return destinationAddress;
    }
    public byte getMainStation(){
        return mainStation;
    }
    public Button getStartButton(){
        return start;
    }
    public TextArea getOutputArea(){
        return outputArea;
    }
    public Text getTokenText(){
        return tokenText;
    }
    public TextArea getInputArea(){
        return inputArea;
    }
    public String getDestination() {
        return destination;
    }
    public void setDestinationAddress(byte dest){
        destinationAddress = dest;
    }

    public void start(Stage stage) {
        try {
            HBox horiz=new HBox();
            VBox rootLayout = new VBox();
            rootLayout.setPadding(new Insets(5));
            rootLayout.setSpacing(5);
            rootLayout.setFillWidth(true);

            Label labelSourceAddress = new Label("Source: ");
            horiz.getChildren().addAll(labelSourceAddress,sourceText);
            rootLayout.getChildren().add(horiz);
            ObservableList<String> items = FXCollections.observableArrayList(destinations);
            ComboBox<String> destinationsList = new ComboBox<>(items);
            destinationsList.setPromptText("Destination addresses");
            destinationsList.valueProperty().
                    addListener((observableValue, previous, current) ->
                            destination = current);
            destinationsList.setMinHeight(30);
            destinationsList.setPrefWidth(300);
            rootLayout.getChildren().add(destinationsList);

            Label labelDebug = new Label("Debug: ");
            tokenText = new Text();

            rootLayout.getChildren().add(labelDebug);
            final Separator separatorTwo = new Separator();
            separatorTwo.setPrefWidth(300);
            separatorTwo.setValignment(VPos.CENTER);
            rootLayout.getChildren().add(separatorTwo);
            rootLayout.getChildren().add(tokenText);
            final Separator separatorThree = new Separator();
            separatorThree.setPrefWidth(300);
            separatorThree.setValignment(VPos.CENTER);
            rootLayout.getChildren().add(separatorThree);

            Label labelInput = new Label("Input");
            rootLayout.getChildren().add(labelInput);

            inputArea = new TextArea();
            inputArea.setPrefWidth(250);
            inputArea.setMaxHeight(30);
            rootLayout.getChildren().add(inputArea);

            Label labelOutput = new Label("Output");
            rootLayout.getChildren().add(labelOutput);

            outputArea = new TextArea();
            outputArea.setEditable(false);
            outputArea.setPrefHeight(100);
            outputArea.setWrapText(true);
            rootLayout.getChildren().add(outputArea);

            start = new Button("Start");
            start.setMinHeight(30);
            start.setPrefWidth(300);
            if(mainStation == 1)
                rootLayout.getChildren().add(start);

            Scene scene = new Scene(rootLayout, 300, 380);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}