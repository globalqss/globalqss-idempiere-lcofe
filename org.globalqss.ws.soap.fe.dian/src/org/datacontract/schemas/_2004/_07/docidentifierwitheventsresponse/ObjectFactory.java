
package org.datacontract.schemas._2004._07.docidentifierwitheventsresponse;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.datacontract.schemas._2004._07.docidentifierwitheventsresponse package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DocIdentifierWithEventsResponse_QNAME = new QName("http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", "DocIdentifierWithEventsResponse");
    private final static QName _DocIdentifierWithEventsResponseCsvBase64Bytes_QNAME = new QName("http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", "CsvBase64Bytes");
    private final static QName _DocIdentifierWithEventsResponseMessage_QNAME = new QName("http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", "Message");
    private final static QName _DocIdentifierWithEventsResponseStatusCode_QNAME = new QName("http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", "StatusCode");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.datacontract.schemas._2004._07.docidentifierwitheventsresponse
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DocIdentifierWithEventsResponse }
     * 
     */
    public DocIdentifierWithEventsResponse createDocIdentifierWithEventsResponse() {
        return new DocIdentifierWithEventsResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DocIdentifierWithEventsResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link DocIdentifierWithEventsResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", name = "DocIdentifierWithEventsResponse")
    public JAXBElement<DocIdentifierWithEventsResponse> createDocIdentifierWithEventsResponse(DocIdentifierWithEventsResponse value) {
        return new JAXBElement<DocIdentifierWithEventsResponse>(_DocIdentifierWithEventsResponse_QNAME, DocIdentifierWithEventsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", name = "CsvBase64Bytes", scope = DocIdentifierWithEventsResponse.class)
    public JAXBElement<byte[]> createDocIdentifierWithEventsResponseCsvBase64Bytes(byte[] value) {
        return new JAXBElement<byte[]>(_DocIdentifierWithEventsResponseCsvBase64Bytes_QNAME, byte[].class, DocIdentifierWithEventsResponse.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", name = "Message", scope = DocIdentifierWithEventsResponse.class)
    public JAXBElement<String> createDocIdentifierWithEventsResponseMessage(String value) {
        return new JAXBElement<String>(_DocIdentifierWithEventsResponseMessage_QNAME, String.class, DocIdentifierWithEventsResponse.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse", name = "StatusCode", scope = DocIdentifierWithEventsResponse.class)
    public JAXBElement<String> createDocIdentifierWithEventsResponseStatusCode(String value) {
        return new JAXBElement<String>(_DocIdentifierWithEventsResponseStatusCode_QNAME, String.class, DocIdentifierWithEventsResponse.class, value);
    }

}
