import java.io.*;
import java.net.*;
import java.util.Arrays;

public class FileServer {
    public static void main(String[] args) {
        int port = 6000;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                String command = dis.readUTF();

                if (command.equals("UPLOAD")) {
                    receiveFile(dis);
                } else if (command.equals("DOWNLOAD")) {
                    String fileName = dis.readUTF();
                    sendFile(socket, fileName);
                } else if (command.equals("LIST")) {
                    listFiles(socket);
                }

                socket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void sendFile(Socket socket, String fileName) throws IOException {
        File file = new File(fileName);
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        if (file.exists()) {
            dos.writeBoolean(true);
            dos.writeLong(file.length());

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead);
            }

            fis.close();
            System.out.println("File sent: " + fileName);
        } else {
            dos.writeBoolean(false);
            System.out.println("File not found: " + fileName);
        }
    }

    private static void receiveFile(DataInputStream dis) throws IOException {
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        FileOutputStream fos = new FileOutputStream("server_" + fileName);
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytesRead = 0;

        while ((bytesRead = dis.read(buffer)) > 0) {
            fos.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
            if (totalBytesRead >= fileSize) {
                break;
            }
        }

        fos.close();
        System.out.println("File received: server_" + fileName);
    }

    private static void listFiles(Socket socket) throws IOException {
        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.startsWith("server_"));

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        if (files != null) {
            dos.writeInt(files.length);
            for (File file : files) {
                dos.writeUTF(file.getName());
            }
        } else {
            dos.writeInt(0);
        }

        System.out.println("File list sent to client.");
    }
}