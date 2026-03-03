package org.example;

public class TestConnection {

    public static void main(String[] args) {

        try {
            if (DBConnection.getConnection() != null) {
                System.out.println("Connected Successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
