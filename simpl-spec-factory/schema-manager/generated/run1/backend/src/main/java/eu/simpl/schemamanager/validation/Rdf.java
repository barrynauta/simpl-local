package eu.simpl.schemamanager.validation;

import java.io.ByteArrayInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandler;

/** Turtle parsing and SHACL vocabulary terms. */
public final class Rdf {

    public static final String SH = "http://www.w3.org/ns/shacl#";
    public static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final Resource NODE_SHAPE = res(SH + "NodeShape");
    public static final Property RDF_TYPE = prop(RDF + "type");
    public static final Property SH_PROPERTY = prop(SH + "property");
    public static final Property SH_PATH = prop(SH + "path");
    public static final Property SH_CLASS = prop(SH + "class");
    public static final Property SH_TARGET_CLASS = prop(SH + "targetClass");
    public static final Property SH_DATATYPE = prop(SH + "datatype");
    public static final Property SH_IN = prop(SH + "in");
    public static final Property SH_NODE = prop(SH + "node");
    public static final Property SH_QUALIFIED_VALUE_SHAPE = prop(SH + "qualifiedValueShape");

    private static final String BASE = "http://schema-manager.invalid/base/";

    private Rdf() {
    }

    private static Resource res(String iri) {
        return ModelFactory.createDefaultModel().createResource(iri);
    }

    private static Property prop(String iri) {
        return ModelFactory.createDefaultModel().createProperty(iri);
    }

    /** Parses UTF-8 Turtle. Any parser error raises a RiotException with line and column. */
    public static Model parseTurtle(byte[] content) {
        Model model = ModelFactory.createDefaultModel();
        RDFParser.create()
                .source(new ByteArrayInputStream(content))
                .lang(Lang.TURTLE)
                .base(BASE)
                .errorHandler(STRICT)
                .parse(model.getGraph());
        return model;
    }

    private static final ErrorHandler STRICT = new ErrorHandler() {
        @Override
        public void warning(String message, long line, long col) {
            // Warnings (for example unusual IRIs) do not make the document invalid.
        }

        @Override
        public void error(String message, long line, long col) {
            throw new RiotException(position(line, col) + message);
        }

        @Override
        public void fatal(String message, long line, long col) {
            throw new RiotException(position(line, col) + message);
        }
    };

    private static String position(long line, long col) {
        if (line < 0) {
            return "";
        }
        return "[line: " + line + ", col: " + col + "] ";
    }
}
