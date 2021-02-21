import java.io.IOException;
import java.text.ParseException;
import java.util.Scanner;

public class Testing {
    public static void main(String[] args) throws IOException, ParseException {

        Scanner scanner = new Scanner(System.in);

        PivoLab pivo = new PivoLab();
        pivo.readFile();

        pivo.interact(scanner);
    }
}
