package eu.simpl.vocabularymanager.validation;

/** The uploaded content is not valid Turtle (D9 syntax step). */
public class TurtleSyntaxException extends Exception {

    private static final long serialVersionUID = 1L;

    private final long line;
    private final long column;

    /**
     * @param message the parser message, cleaned
     * @param line the line, or -1 when unknown
     * @param column the column, or -1 when unknown
     * @param cause the parser exception
     */
    public TurtleSyntaxException(String message, long line, long column, Throwable cause) {
        super(message, cause);
        this.line = line;
        this.column = column;
    }

    /**
     * @return the issue detail of D9: starts with 'Encountered invalid Turtle syntax'
     */
    public String detail() {
        String message = getMessage();
        return message == null || message.isBlank()
                ? "Encountered invalid Turtle syntax."
                : "Encountered invalid Turtle syntax: " + message;
    }

    /**
     * @return 'line N, column M', the column only when the parser gives one (D60, D65)
     */
    public String location() {
        long safeLine = line > 0 ? line : 1;
        return column > 0 ? "line " + safeLine + ", column " + column : "line " + safeLine;
    }
}
