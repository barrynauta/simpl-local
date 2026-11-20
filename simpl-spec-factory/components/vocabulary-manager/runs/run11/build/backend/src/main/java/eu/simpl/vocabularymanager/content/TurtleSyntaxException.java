package eu.simpl.vocabularymanager.content;

/** The first Turtle syntax error (D9): message and position, line and column 0 when unknown. */
public class TurtleSyntaxException extends Exception {

    private static final long serialVersionUID = 1L;

    private final long line;
    private final long column;

    public TurtleSyntaxException(String message, long line, long column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public long line() {
        return line;
    }

    public long column() {
        return column;
    }

    /** 'line N, column M', the column only when the parser gives one (D60, D65). */
    public String location() {
        if (line <= 0) {
            return "document";
        }
        return column > 0 ? "line " + line + ", column " + column : "line " + line;
    }
}
