package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private HBox authPanel;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox msgPanel;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Stage stage;
    private Stage regStage;
    private RegController regController;

    private boolean authenticated;
    private String nickname;

    private static String HISTORIES_DIRECTORY = "client/src/main/resources/histories/";
    private static int COUNT_OF_HISTORY_LINES = 15;

    private void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);

        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
            setTitle("Балабол");
        } else {
            setTitle(String.format("[ %s ] - Балабол", nickname));

//            loadHistory(loginField.getText());
        }
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                System.out.println("Bye");
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF("/end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
        createRegWindow();
    }


    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {

                    //цикл аутентификации
                    while (true) {

                        String str = in.readUTF();

                        if (str.startsWith("/authok ")) {
                            nickname = str.split("\\s")[1];
                            setAuthenticated(true);
                            loadHistory(loginField.getText());
                            break;
                        }
                        // доработка (тайм аут соединения)
                        if (str.equals("/end")) {
                            System.out.println("Пришло от сервера по таймауту: " + str);
//                            throw new EOFException();
                        }
                        if (str.startsWith("/regok")) {
                            regController.addMessageTextArea("Регистрация прошла успешно\n");
                        }
                        if (str.startsWith("/regno")) {
                            regController.addMessageTextArea("Зарегистрироваться не удалось\n" +
                                    "возможно, такой логин или никнэйм уже заняты\n");
                        }
                        System.out.println("Строка - " + str);
                        textArea.appendText(str + "\n");
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/changenick ")) {
                                String[] token = str.split("\\s", 2);
                                setTitle(String.format("[ %s ] - Балабол", token[1]));
                            }

                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split("\\s");
                                System.out.println(Arrays.toString(token));
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                        } else {
                            textArea.appendText(str + "\n");
                            saveRowOfHistory(loginField.getText(), str + "\n");
                        }
                    }
                }
//                catch (EOFException e) {
//                    System.out.println("Истекло время ожидания соединения сервером.\n Произошел разрыв соединения.");
//                }
                catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    setAuthenticated(false);
                    try {
                        socket.close();
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        if (textField.getText().trim().length() == 0) {
            return;
        }
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String msg = String.format("/auth %s %s",
                loginField.getText().trim(), passwordField.getText().trim());
        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String title) {
        Platform.runLater(() -> {
            stage.setTitle(title);
        });
    }

    public void clickClientList(MouseEvent mouseEvent) {
        System.out.println(clientList.getSelectionModel().getSelectedItem());
        textField.setText(String.format("/w %s ", clientList.getSelectionModel().getSelectedItem()));
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Registration to Balabol chat");
            regStage.setScene(new Scene(root, 350, 300));
            regStage.initModality(Modality.APPLICATION_MODAL);

            regController = fxmlLoader.getController();
            regController.setController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void regStageShow(ActionEvent actionEvent) {
        regStage.show();
    }

    public void tryRegistration(String login, String password, String nickname) {
        System.out.printf("login %s, password %s, nickname %s \n", login, password, nickname);
        String msg = String.format("/reg %s %s %s", login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
            System.out.println("Прошли подлкючение в трайтурегистратион");
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // метод загрузки истории из файла
    // если файла нет, то он создается
    public void loadHistory(String login) {

        // если файл истории не существует, то создаем его
        File file = new File(HISTORIES_DIRECTORY + "history_[" +
                login + "].txt");
        if (!file.exists()) { //создаем новый файл истории
            System.out.println("Такой файл не существует: " + file.toString());
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Не удалось создать файл истории");
                e.printStackTrace();
            }
        }

        // читаем строки из файла с помощью java.nio.file.Files.readAllLines()
        try {
            List<String> lines = Files.readAllLines(Paths.get(HISTORIES_DIRECTORY + "history_[" +
                    login + "].txt"), StandardCharsets.UTF_8);

            Platform.runLater(() -> { // зполняем textArea
                if (COUNT_OF_HISTORY_LINES < lines.size()) {
                    for (int i = lines.size() - COUNT_OF_HISTORY_LINES; i < lines.size(); i++) {
                        textArea.appendText(lines.get(i) + "\n");
                    }
                } else {
                    for (String line : lines) {
                        textArea.appendText(line + "\n");
                    }
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    //метод сохранения одной строки в файл
    // если файл не найден, он создается
    private void saveRowOfHistory(String login, String str) {
        try {
            Files.write(Paths.get(HISTORIES_DIRECTORY + "history_[" +
                            login + "].txt"), str.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
