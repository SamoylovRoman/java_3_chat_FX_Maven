package server;

import java.sql.*;

public class DBAuthService implements AuthService {

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet rs;
    private static PreparedStatement psInsert;
    private static PreparedStatement psUpdate;

    public DBAuthService() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:db_chat.db");
            stmt = connection.createStatement();
            psInsert = connection.prepareStatement("INSERT INTO userdata (login,password,nickname) VALUES (?,?,?);");
            psUpdate = connection.prepareStatement("UPDATE userdata SET nickname=? WHERE nickname=?;");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            rs = stmt.executeQuery("SELECT nickname FROM userdata WHERE (login='" + login +
                    "') AND (password='" + password + "');");
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {

        try {
            psInsert.setString(1, login);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        try {
            int tmp = psInsert.executeUpdate();
            System.out.println("Количество затронутых строк: " + tmp);
        } catch (SQLException throwables) {
            System.out.println("Ошибка добавления повторной записи");
            return false;
        }
        return true;
    }

    @Override
    public boolean changeNickName(String oldNick, String newNick) {
        System.out.println("Хотим поменять никнейм " + oldNick + " на новый никнейм " + newNick);
        try {
            psUpdate.setString(1, newNick);
            psUpdate.setString(2, oldNick);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            psUpdate.executeUpdate();
        } catch (SQLException throwables) {
            System.out.println("Сменить никнейм не удалось");
            return false;
        }
        return true;
    }
}
