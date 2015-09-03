package at.jku.cis.radar.service;


import android.graphics.Color;

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

import at.jku.cis.radar.model.Action;
import at.jku.cis.radar.model.Event;

public class EventDOMParser {

    private static final String EVENT = "event";
    private static final String EVENTS = "events";
    private static final String ACTION = "action";
    private static final String ACTIONS = "actions";
    private static final String COLOR = "color";
    private static final String NAME = "name";

    public List<Event> processXML(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document xmlEventDocument = documentBuilder.parse(inputStream);
        Element rootElement = xmlEventDocument.getDocumentElement();
        if (rootElement.getNodeName().equals(EVENTS)) {
            NodeList children = rootElement.getChildNodes();
            return generateEvents(children);
        }
        return null;
    }

    private List<Event> generateEvents(NodeList nodeList) {
        List<Event> eventList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE && nodeList.item(i).getNodeName().equals(EVENT)) {
                Node subEvent = nodeList.item(i);
                eventList.add(generateEvent(subEvent));
            }
        }
        return eventList;
    }


    private List<Action> generateActions(NodeList nodeList) {
        List<Action> actionList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE && nodeList.item(i).getNodeName().equals(ACTION)) {
                actionList.add(generateAction(nodeList.item(i)));
            }
        }
        return actionList;
    }

    private Event generateEvent(Node node) {
        List<Event> subEvent = null;
        List<Action> actions = null;
        String name = ((Element) node).getAttribute(NAME);
        int color = Color.parseColor(((Element) node).getAttribute(COLOR));

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeName().equals(EVENTS)) {
                subEvent = generateEvents(childNodes.item(i).getChildNodes());
            } else if (childNodes.item(i).getNodeName().equals(ACTIONS)) {
                actions = generateActions(childNodes.item(i).getChildNodes());
            }
        }
        return new Event(name, color, subEvent, actions);
    }

    private Action generateAction(Node node) {
        String actionName = ((Element) node).getAttribute("name");
        return new Action(actionName);
    }
}
