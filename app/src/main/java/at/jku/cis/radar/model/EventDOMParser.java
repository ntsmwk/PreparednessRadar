package at.jku.cis.radar.model;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class EventDOMParser {


    public static List<XMLEvent> processXML(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document xmlEventDocument = documentBuilder.parse(is);
        Element rootElement = xmlEventDocument.getDocumentElement();
        if (rootElement.getNodeName().equals("events")) {
            NodeList children = rootElement.getChildNodes();
            return generateXMLEventList(children);
        }
        return null;
    }


    private static XMLEvent generateXMLElement(Node node) {
        List<XMLEvent> subEvent = null;
        List<XMLAction> actions = null;
        String eventName = ((Element) node).getAttribute("name");

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            {
                if (childNodes.item(i).getNodeName().equals("events")) {
                    subEvent = generateXMLEventList(childNodes.item(i).getChildNodes());
                } else if (childNodes.item(i).getNodeName().equals("actions")) {
                    actions = getGenerateXMLActionList(childNodes.item(i).getChildNodes());
                }
            }
        }
        return new XMLEvent(eventName, subEvent, actions);
    }

    private static List<XMLEvent> generateXMLEventList(NodeList events) {
        List<XMLEvent> eventList = new ArrayList<>();
        Node subEvent;
        for (int i = 0; i < events.getLength(); i++) {
            if(events.item(i).getNodeType() == Node.ELEMENT_NODE && events.item(i).getNodeName().equals("event")) {
                subEvent = events.item(i);
                eventList.add(generateXMLElement(subEvent));
            }
        }
        return eventList;
    }


    private static List<XMLAction> getGenerateXMLActionList(NodeList actions) {
        List<XMLAction> actionList = new ArrayList<>();
        Node action;
        for (int i = 0; i < actions.getLength(); i++) {
            if(actions.item(i).getNodeType() == Node.ELEMENT_NODE && actions.item(i).getNodeName().equals("action")) {
                String actionName = ((Element) (actions.item(i))).getAttribute("name");
                actionList.add(new XMLAction(actionName));
            }
        }
        return actionList;
    }


}
