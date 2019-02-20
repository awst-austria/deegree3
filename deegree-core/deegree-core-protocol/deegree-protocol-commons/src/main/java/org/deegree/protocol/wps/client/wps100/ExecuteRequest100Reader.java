package org.deegree.protocol.wps.client.wps100;

import static org.deegree.commons.xml.CommonNamespaces.OWS_11_NS;
import static org.deegree.commons.xml.CommonNamespaces.XLNNS;
import static org.deegree.protocol.wps.WPSConstants.WPS_100_NS;
import static org.deegree.protocol.wps.WPSConstants.XML_MIMETYPE_EXPR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.deegree.commons.tom.ows.CodeType;
import org.deegree.commons.xml.stax.XMLStreamReaderDoc;
import org.deegree.commons.xml.stax.XMLStreamUtils;
import org.deegree.protocol.ows.exception.OWSExceptionReader;
import org.deegree.protocol.ows.exception.OWSExceptionReport;
import org.deegree.protocol.wps.client.input.BBoxInput;
import org.deegree.protocol.wps.client.input.BinaryInput;
import org.deegree.protocol.wps.client.input.ExecutionInput;
import org.deegree.protocol.wps.client.input.LiteralInput;
import org.deegree.protocol.wps.client.input.XMLInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for WPS 1.0.0 execute request documents. Should be able to read what ExecuteRequest100Writer writes.
 * Currently, this is incomplete and just reads input parameters.
 *
 */
public class ExecuteRequest100Reader {

    private static Logger LOG = LoggerFactory.getLogger( ExecuteRequest100Reader.class );

    private OMElement rootElement;

    /**
     * Creates an {@link ExecuteRequest100Reader} instance.
     *
     * @param reader an {@link XMLStreamReader} instance, never <code>null</code>
     */
    public ExecuteRequest100Reader( XMLStreamReader reader ) {
        if ( reader.getEventType() != XMLStreamConstants.START_DOCUMENT ) {
            rootElement = new StAXOMBuilder( new XMLStreamReaderDoc( reader ) ).getDocumentElement();
        } else {
            rootElement = new StAXOMBuilder( reader ).getDocumentElement();
        }
    }

    /**
     * (Convenience) method to read an ProcessExecution object for a process from a (optionally gzipped) file.
     * @param filename The filename of the (potentially gzipped) xml file.
     * @return The request object describing the execution request.
     * @throws OWSExceptionReport
     * @throws IOException
     * @throws XMLStreamException
     */
    /*public static ProcessExecution loadProcessExecutionFromFile( String filename )
                            throws IOException,
                            OWSExceptionReport,
                            XMLStreamException {

        InputStream inStream = getInputStreamFromZippedFile( filename );
        ExecuteRequest100Reader reader = getRequestReaderForStream( inStream );
        ProcessExecution request = reader.parseExecute();
        inStream.close();
        return request;
    }*/

    /**
     * (Convenience) method to read the list of ExecutionInput objects for a process from an (optionally gzipped) file.
     * @param filename The filename of the (potentially gzipped) xml file.
     * @return The list of ExecutionInput objects describing the inputs to the execution.
     * @throws OWSExceptionReport
     * @throws IOException
     * @throws XMLStreamException
     */
    public static List<ExecutionInput> loadProcessInputsFromFile( String filename )
                            throws IOException,
                            OWSExceptionReport,
                            XMLStreamException {

        InputStream inStream = getInputStreamFromZippedFile( filename );
        return loadProcessInputsFromStream( inStream );
    }

    /**
     * (Convenience) method to read the list of ExecutionInput objects for a process from an InputStream.
     * @param inStream the stream to read from
     * @return The list of ExecutionInput objects describing the inputs to the execution.
     * @throws OWSExceptionReport
     * @throws IOException
     * @throws XMLStreamException
     */
    public static List<ExecutionInput> loadProcessInputsFromStream( InputStream inStream )
                            throws IOException,
                            OWSExceptionReport,
                            XMLStreamException {

        ExecuteRequest100Reader reader = getRequestReaderForStream( inStream );
        List<ExecutionInput> inputs = reader.parseInputs();
        inStream.close();
        return inputs;
    }

    private static InputStream getInputStreamFromZippedFile( String filename )
                            throws IOException {
        LOG.debug( "Loading request from file: " + filename );

        File file = new File( filename );
        InputStream inStream = new FileInputStream( file );
        try {
            @SuppressWarnings("resource")
            GZIPInputStream zipStream = new GZIPInputStream( inStream );
            inStream = zipStream;
        } catch ( ZipException e ) {
            LOG.debug( "This doesn't seem to be a gzip file: ", e );
            // re-create stream because the gzip input stream already consumed a few bytes
            inStream.close();
            inStream = new FileInputStream( file );
        }

        return inStream;
    }

    private static ExecuteRequest100Reader getRequestReaderForStream( InputStream is )
                            throws OWSExceptionReport,
                            IOException,
                            XMLStreamException {
        XMLInputFactory inFactory = XMLInputFactory.newInstance();
        XMLStreamReader xmlReader = inFactory.createXMLStreamReader( is );
        XMLStreamUtils.nextElement( xmlReader );
        if ( OWSExceptionReader.isExceptionReport( xmlReader.getName() ) ) {
            throw OWSExceptionReader.parseExceptionReport( xmlReader );
        }
        return new ExecuteRequest100Reader( xmlReader );
    }

    /**
     * Parses a WPS 1.0.0 execute request document. Must contain an Execute tag.
     * Schema is: http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd
     *
     * @return an {@link ProcessExecution} object
     * @throws XMLStreamException
     */
/*    protected ProcessExecution parseExecute()
                            throws XMLStreamException {

        checkPreconditions( rootElement );

        ProcessExecution exec = null;

        CodeType procId = parseIdentifier( rootElement );
        System.out.println( procId );

        List<ExecutionInput> inputs = parseDataInputList( rootElement );
        System.out.println( inputs );

        ResponseFormat responseForm = parseResponseForm( rootElement );
        System.out.println( responseForm );

        return exec;
    }*/

    /**
     * Parses the execution inputs in a WPS 1.0.0 execute request document.
     * Must contain a DataInputs tag.
     * Schema is: http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd
     *
     * @return a list of {@link ExecutionInput} objects
     * @throws XMLStreamException
     */
    protected List<ExecutionInput> parseInputs()
                            throws XMLStreamException {
        return parseDataInputList( rootElement );
    }

/*    private void checkPreconditions( OMElement el ) {
        QName ex = new QName( WPS_100_NS, "Execute" );
        QName qname = el.getQName();
        if ( !ex.equals( qname ) )
            throw new RuntimeException( "Expected Execute request but got: " + qname );

        String service = el.getAttributeValue( new QName( null, "service" ) );
        String version = el.getAttributeValue( new QName( null, "version" ) );

        if ( !"wps".equals( service.toLowerCase() ) )
            throw new RuntimeException( String.format( "Can't handle service type %s, can only handle WPS", service ) );
        if ( !"1.0.0".equals( version ) )
            throw new RuntimeException( String.format( "Can't handle WPS version %s, can only handle 1.0.0",
                                                       service ) );
    }*/

    private CodeType parseIdentifier( OMElement el ) {
        OMElement codeTypeElement = el.getFirstChildWithName( new QName( OWS_11_NS, "Identifier" ) );
        if ( codeTypeElement == null )
            throw new RuntimeException( "Required Identifier element not found." );
        String codeSpace = codeTypeElement.getAttributeValue( new QName( "codeSpace" ) );
        String value = codeTypeElement.getText();
        return new CodeType( value, codeSpace );
    }

    private List<ExecutionInput> parseDataInputList( OMElement el )
                            throws XMLStreamException {
        ArrayList<ExecutionInput> inputs = new ArrayList<ExecutionInput>();

        final QName dataInputsQName = new QName( WPS_100_NS, "DataInputs" );

        OMElement dataInputsEl = ( dataInputsQName.equals( el.getQName() ) )
                                ? el
                                : el.getFirstChildWithName( dataInputsQName );

        if ( dataInputsEl == null )
            return inputs;

        @SuppressWarnings("unchecked")
        Iterator<OMElement> inputsIter = dataInputsEl.getChildrenWithName( new QName( WPS_100_NS, "Input" ) );
        while ( inputsIter.hasNext() ) {
            OMElement inputEl = inputsIter.next();
            ExecutionInput inp = parseDataInput( inputEl );
            inputs.add( inp );
        }

        if ( inputs.size() < 1 )
            throw new RuntimeException( "DataInputs element contains no valid Input elements." );

        return inputs;
    }

    private ExecutionInput parseDataInput( OMElement el ) {
        ExecutionInput input = null;

        CodeType inputId = parseIdentifier( el );

        // ignore Abstract and Title, we can't store them in the ExecutionInput anyway.

        OMElement refEl = el.getFirstChildWithName( new QName( WPS_100_NS, "Reference" ) );
        OMElement dataEl = el.getFirstChildWithName( new QName( WPS_100_NS, "Data" ) );

        // input is a reference
        if ( refEl != null ) {
            input = parseInputReference( inputId, refEl );
        }
        // input is embedded in the xml document
        else if ( dataEl != null ) {
            OMElement child = (OMElement) dataEl.getChildElements().next();
            QName qname = child.getQName();
            if ( qname.equals( new QName( WPS_100_NS, "LiteralData" ) ) ) {
                input = parseLiteralInput( inputId, child );
            } else if ( qname.equals( new QName( WPS_100_NS, "BoundingBoxData" ) ) ) {
                input = parseBoundingBoxInput( inputId, child );
            } else if ( qname.equals( new QName( WPS_100_NS, "ComplexData" ) ) ) {
                input = parseComplexInput( inputId, child );
            }
        } else {
            throw new RuntimeException( "Input element contains no valid Reference or Data element." );
        }

        return input;
    }

    private ExecutionInput parseInputReference( CodeType inputId, OMElement el ) {
        // All the BinaryInput and XMLInput objects can store are the link, mimetype, encoding, and schema.
        // So don't bother with the Header and Body tags.
        String href = el.getAttributeValue( new QName( XLNNS, "href" ) );
        String mimeType = el.getAttributeValue( new QName( "mimeType" ) );
        String encoding = el.getAttributeValue( new QName( "encoding" ) );
        String schema = el.getAttributeValue( new QName( "schema" ) );
        URI uri = null;

        try {
            uri = new URI( href );
        } catch ( Exception e ) {
            throw new RuntimeException( "Can't parse reference URI", e );
        }

        if ( mimeType != null && ( mimeType.matches( XML_MIMETYPE_EXPR ) ) ) {
            return new XMLInput( inputId, uri, true, mimeType, encoding, schema );

        } else {
            return new BinaryInput( inputId, uri, true, mimeType, encoding );
        }
    }

    private ExecutionInput parseLiteralInput( CodeType inputId, OMElement el ) {
        String value = el.getText();
        String dataType = el.getAttributeValue( new QName( "dataType" ) );
        String uom = el.getAttributeValue( new QName( "uom" ) );
        return new LiteralInput( inputId, value, dataType, uom );
    }

    private ExecutionInput parseBoundingBoxInput( CodeType inputId, OMElement el ) {
        String crs = el.getAttributeValue( new QName( "crs" ) );
        OMElement lCornerEl = el.getFirstChildWithName( new QName( OWS_11_NS, "LowerCorner" ) );
        OMElement uCornerEl = el.getFirstChildWithName( new QName( OWS_11_NS, "UpperCorner" ) );

        String[] lowerStr = lCornerEl.getText().split( " ", 3 );
        String[] upperStr = uCornerEl.getText().split( " ", 3 );

        double[] lower = new double[2];
        double[] upper = new double[2];

        for ( int i = 0; i < lower.length; i++ )
            lower[i] = Double.parseDouble( lowerStr[i] );

        for ( int i = 0; i < upper.length; i++ )
            upper[i] = Double.parseDouble( upperStr[i] );

        return new BBoxInput( inputId, lower, upper, crs );
    }

    private ExecutionInput parseComplexInput( CodeType inputId, OMElement el ) {
        String mimeType = el.getAttributeValue( new QName( "mimeType" ) );
        String encoding = el.getAttributeValue( new QName( "encoding" ) );
        String schema = el.getAttributeValue( new QName( "schema" ) );

        // xml input parameter
        if ( mimeType != null && ( mimeType.matches( XML_MIMETYPE_EXPR ) ) ) {
            try {
                OMElement xmlEl = (OMElement) el.getChildElements().next();
                XMLStreamReader reader = xmlEl.getXMLStreamReader();
                XMLStreamUtils.skipStartDocument( reader );
                XMLInput input = new XMLInput( inputId, reader, mimeType, encoding, schema );
                return input;
            } catch ( Exception e ) {
                throw new RuntimeException( "Can't extract xml input parameter: ", e );
            }
        }
        // binary input parameter
        else {
            String content = el.getText();
            byte[] decodedContent = Base64.getDecoder().decode( content );
            InputStream inputStream = new ByteArrayInputStream( decodedContent );
            BinaryInput input = new BinaryInput( inputId, inputStream, mimeType, encoding );
            return input;
        }
    }

/*    private ResponseFormat parseResponseForm( OMElement el ) {
        ResponseFormat responseFormat = null;

        OMElement responseFormEl = el.getFirstChildWithName( new QName( WPS_100_NS, "ResponseForm" ) );

        if ( responseFormEl == null )
            return responseFormat;

        OMElement responseDocEl = responseFormEl.getFirstChildWithName( new QName( WPS_100_NS, "ResponseDocument" ) );
        if ( responseDocEl != null ) {
            // TODO parse ResponseDocument here
        }
        // if we don't have a ResponseDocument, we must have a RawDataOutput
        else {
            OMElement rawOutputEl = responseFormEl.getFirstChildWithName( new QName( WPS_100_NS, "RawDataOutput" ) );
            if ( rawOutputEl == null )
                throw new RuntimeException( "ResponseForm element is missing ResponseDocument / RawDataOutput." );

            // TODO parse RawDataOutput here
        }

        return responseFormat;
    }*/
}
