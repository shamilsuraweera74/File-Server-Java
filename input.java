import java.io.*;

public class FileReadWriteDemo {
    public static void main(String[] args) {
        // Create a file named output.txt and write "Hello, world!" to it
        try (FileOutputStream fileOutputStream = new FileOutputStream("output.txt")) {
            String content = "Hello, world!";
            fileOutputStream.write(content.getBytes());
            System.out.println("Data written to the file successfully.");
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }

        // Read the contents of the file and print each line to the console
        try (FileInputStream fileInputStream = new FileInputStream("output.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Read from file: " + line);
            }
        } catch (IOException e) {
            System.err.println("An error occurred while reading from the file: " + e.getMessage());
        }
    }
}
