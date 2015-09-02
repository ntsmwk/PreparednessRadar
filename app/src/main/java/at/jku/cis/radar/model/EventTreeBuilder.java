package at.jku.cis.radar.model;


import com.unnamed.b.atv.model.TreeNode;

import java.util.List;

public class EventTreeBuilder {


    public static EventTreeNode initializeEventTree(List<XMLEvent> eventList) {
        TreeNode rootNode = TreeNode.root();
        XMLEvent rootEvent = new XMLEvent("RootEvent", eventList, null);
        EventTreeNode rootEventNode = new EventTreeNode(rootNode, rootEvent);
        generateSubEventNodes(rootEventNode, eventList);
        return rootEventNode;
    }

    private static void generateSubEventNodes(EventTreeNode parent, List<XMLEvent> children) {
        EventTreeNode subNode;
        for (XMLEvent event : children) {
            subNode = new EventTreeNode(new TreeNode(event.getEventName()), event);
            subNode.getTreeNode().setSelectable(true);
            if (event.getSubEventList() != null) {
                generateSubEventNodes(subNode, event.getSubEventList());
            }
            parent.getTreeNode().addChild(subNode.getTreeNode());
        }
    }
}
