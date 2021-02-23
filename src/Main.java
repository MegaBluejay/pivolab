import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        PivoLab pivo = new PivoLab();

        try {
            pivo.readFile();
        } catch (PivoFileException e) {
            System.out.println("Error reading pivo file: " + e.getMessage());
            return;
        }

        pivo.interact(scanner, false);
    }
}
