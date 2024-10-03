import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileClientSwing {
    private JFrame frame;
    private JList<String> serverFilesList;
    private DefaultListModel<String> listModel;
    private JProgressBar uploadProgressBar, downloadProgressBar;
    private List<String> serverFiles = new ArrayList<>();
    private String serverAddress = "localhost";
    private int port = 5000;

    public FileClientSwing() {
        frame = new JFrame("File Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        listModel = new DefaultListModel<>();
        serverFilesList = new JList<>(listModel);

        uploadProgressBar = new JProgressBar(0, 100);
        downloadProgressBar = new JProgressBar(0, 100);

        JButton uploadButton = new JButton("Upload File");
        uploadButton.addActionListener(this::uploadFile);

        JButton downloadButton = new JButton("Download Selected File");
        downloadButton.addActionListener(this::downloadFile);

        JButton refreshButton = new JButton("Refresh File List");
        refreshButton.addActionListener(e -> refreshFileList());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(refreshButton);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new GridLayout(2, 1));
        progressPanel.add(new JLabel("Upload Progress:"));
        progressPanel.add(uploadProgressBar);
        progressPanel.add(new JLabel("Download Progress:"));
        progressPanel.add(downloadProgressBar);

        frame.getContentPane().add(new JScrollPane(serverFilesList), BorderLayout.CENTER);
        frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);
        frame.getContentPane().add(progressPanel, BorderLayout.SOUTH);

        refreshFileList();
        frame.setVisible(true);
    }

    private void uploadFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                new Thread(() -> {
                    try (Socket socket = new Socket(serverAddress, port)) {
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF("UPLOAD");
                        dos.writeUTF(file.getName());
                        dos.writeLong(file.length());

                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;
                        long fileSize = file.length();

                        while ((bytesRead = fis.read(buffer)) > 0) {
                            dos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            uploadProgressBar.setValue(progress);
                        }

                        fis.close();
                        JOptionPane.showMessageDialog(frame, "File uploaded: " + file.getName());
                        refreshFileList();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Error uploading file: " + ex.getMessage());
                    }
                }).start();
            }
        }
    }

    private void downloadFile(ActionEvent e) {
        String selectedFile = serverFilesList.getSelectedValue();
        if (selectedFile != null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose Save Location");
            fileChooser.setSelectedFile(new File(selectedFile));
            int returnValue = fileChooser.showSaveDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();
                new Thread(() -> {
                    try (Socket socket = new Socket(serverAddress, port)) {
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF("DOWNLOAD");
                        dos.writeUTF(selectedFile);

                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        boolean fileExists = dis.readBoolean();

                        if (fileExists) {
                            long fileSize = dis.readLong();
                            FileOutputStream fos = new FileOutputStream(saveFile);

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalBytesRead = 0;

                            while ((bytesRead = dis.read(buffer)) > 0) {
                                fos.write(buffer, 0, bytesRead);
                                totalBytesRead += bytesRead;
                                int progress = (int) ((totalBytesRead * 100) / fileSize);
                                downloadProgressBar.setValue(progress);
                                if (totalBytesRead >= fileSize) {
                                    break;
                                }
                            }

                            fos.close();
                            JOptionPane.showMessageDialog(frame, "File downloaded: " + saveFile.getName());
                        } else {
                            JOptionPane.showMessageDialog(frame, "File not found on the server.");
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Error downloading file: " + ex.getMessage());
                    }
                }).start();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a file to download.");
        }
    }

    private void refreshFileList() {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, port)) {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("LIST");

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                int fileCount = dis.readInt();
                serverFiles.clear();
                SwingUtilities.invokeLater(() -> listModel.clear());

                for (int i = 0; i < fileCount; i++) {
                    String fileName = dis.readUTF();
                    serverFiles.add(fileName);
                    SwingUtilities.invokeLater(() -> listModel.addElement(fileName));
                }

                System.out.println("File list refreshed.");

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error refreshing file list: " + ex.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileClientSwing::new);
    }
}