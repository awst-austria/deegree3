//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-792 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.04.27 at 03:55:05 PM MESZ 
//


package org.deegree.commons.datasource.configuration;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RequestNamesType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="RequestNamesType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="GetMap"/>
 *     &lt;enumeration value="GetFeatureInfo"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "RequestNamesType")
@XmlEnum
public enum RequestNamesType {

    @XmlEnumValue("GetMap")
    GET_MAP("GetMap"),
    @XmlEnumValue("GetFeatureInfo")
    GET_FEATURE_INFO("GetFeatureInfo");
    private final String value;

    RequestNamesType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RequestNamesType fromValue(String v) {
        for (RequestNamesType c: RequestNamesType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
