package org.exist.xquery.functions.util;

import org.exist.dom.QName;
import org.exist.memtree.DocumentImpl;
import org.exist.memtree.NodeImpl;
import org.exist.memtree.SAXAdapter;
import org.exist.xquery.*;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;

public class Parse extends BasicFunction {

    public final static FunctionSignature signature =
        new FunctionSignature(
            new QName( "parse", UtilModule.NAMESPACE_URI, UtilModule.PREFIX),
            "Parses the passed string value into an XML fragment. The string has to be " +
            "well-formed XML. An empty sequence is returned if the argument is an " +
            "empty string or sequence.",
            new SequenceType[] {
                new SequenceType( Type.STRING, Cardinality.ZERO_OR_ONE ),
            },
            new SequenceType( Type.NODE, Cardinality.ZERO_OR_MORE )
        );

    public Parse(XQueryContext context) {
        super(context, signature);
    }

    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        if (args[0].getItemCount() == 0)
            return Sequence.EMPTY_SEQUENCE;
        String xmlContent = args[0].itemAt(0).getStringValue();
        if (xmlContent.length() == 0)
            return Sequence.EMPTY_SEQUENCE;
        StringReader reader = new StringReader(xmlContent);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            InputSource src = new InputSource(reader);
            SAXParser parser = factory.newSAXParser();
            XMLReader xr = parser.getXMLReader();
            SAXAdapter adapter = new SAXAdapter();
            xr.setContentHandler(adapter);
            xr.parse(src);

            DocumentImpl doc = (DocumentImpl) adapter.getDocument();
            if (doc.getChildCount() == 1)
                return (NodeImpl) doc.getFirstChild();
            else {
                ValueSequence result = new ValueSequence();
                NodeImpl node = (NodeImpl) doc.getFirstChild();
                while (node != null) {
                    result.add(node);
                    node = (NodeImpl) node.getNextSibling();
                }
                return result;
            }
        } catch (ParserConfigurationException e) {
            throw new XPathException(getASTNode(), "Error while constructing XML parser: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new XPathException(getASTNode(), "Error while parsing XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new XPathException(getASTNode(), "Error while parsing XML: " + e.getMessage(), e);
        }
    }
}
