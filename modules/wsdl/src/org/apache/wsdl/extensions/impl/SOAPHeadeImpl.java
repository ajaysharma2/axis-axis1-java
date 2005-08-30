package org.apache.wsdl.extensions.impl;

import org.apache.wsdl.extensions.SOAPHeader;

import javax.xml.namespace.QName;

/**
 * Created by IntelliJ IDEA.
 * User: Ajith
 * Date: Aug 30, 2005
 * Time: 12:19:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SOAPHeadeImpl extends SOAPBodyImpl implements SOAPHeader {

    private QName messageName = null;
    private String part = null;

    public SOAPHeadeImpl() {
        this.type = SOAP_HEADER;
    }

    public QName getMessage() {
        return messageName;
    }

    public void setMessage(QName message) {
        this.messageName = message;
    }

    public String part() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }

}