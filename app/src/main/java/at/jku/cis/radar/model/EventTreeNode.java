package at.jku.cis.radar.model;

import com.unnamed.b.atv.model.TreeNode;

import java.util.List;

public class EventTreeNode {

    private TreeNode treeNode;
    private XMLEvent event;

    public EventTreeNode(TreeNode treeNode, XMLEvent event) {
        this.treeNode = treeNode;
        this.event = event;
    }

    public XMLEvent getEvent() {
        return event;
    }

    public void setEvent(XMLEvent event) {
        this.event = event;
    }

    public TreeNode getTreeNode() {
        return treeNode;
    }

    public void setTreeNode(TreeNode treeNode) {
        this.treeNode = treeNode;
    }
}
