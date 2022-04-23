
package colombia.dian.wcf;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.datacontract.schemas._2004._07.docidentifierwitheventsresponse.DocIdentifierWithEventsResponse;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="GetDocIdentifierWithEventsResult" type="{http://schemas.datacontract.org/2004/07/DocIdentifierWithEventsResponse}DocIdentifierWithEventsResponse" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getDocIdentifierWithEventsResult"
})
@XmlRootElement(name = "GetDocIdentifierWithEventsResponse")
public class GetDocIdentifierWithEventsResponse {

    @XmlElementRef(name = "GetDocIdentifierWithEventsResult", namespace = "http://wcf.dian.colombia", type = JAXBElement.class, required = false)
    protected JAXBElement<DocIdentifierWithEventsResponse> getDocIdentifierWithEventsResult;

    /**
     * Gets the value of the getDocIdentifierWithEventsResult property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link DocIdentifierWithEventsResponse }{@code >}
     *     
     */
    public JAXBElement<DocIdentifierWithEventsResponse> getGetDocIdentifierWithEventsResult() {
        return getDocIdentifierWithEventsResult;
    }

    /**
     * Sets the value of the getDocIdentifierWithEventsResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link DocIdentifierWithEventsResponse }{@code >}
     *     
     */
    public void setGetDocIdentifierWithEventsResult(JAXBElement<DocIdentifierWithEventsResponse> value) {
        this.getDocIdentifierWithEventsResult = value;
    }

}
