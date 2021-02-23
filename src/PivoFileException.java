public class PivoFileException extends Exception {

    public PivoFileException(String message) {
        super(message);
    }

    public static PivoFileException nFields(int line, int n) {
        return new PivoFileException("only " + n + " fields on line " + line);
    }

    public static PivoFileException invalidField(int line, String fieldName) {
        return new PivoFileException("invalid " + fieldName + "on line " + line);
    }

    public static PivoFileException notFound() {
        return new PivoFileException("file not found");
    }

    public static PivoFileException readProblem() {
        return new PivoFileException("problem reading file");
    }
}