package uk.ac.soton.ldanalytics.sparql2sql.model;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;

public class SNode {
	private Resource res = null;
	private Node node = null;
	
	public SNode(Resource res, Node node) {
		this.res = res;
		this.node = node;
	}
	
	public Resource getResource() {
		return res;
	}
	
	public Node getNode() {
		return node;
	}
	
	@Override
	public String toString() {
		return "[res:"+res+",node:"+node+"]";
	}
	
	@Override
    public boolean equals(Object obj) {
		SNode snode = (SNode)obj;
		if(snode.getNode().equals(res) && snode.getResource().equals(node))
			return true;
		return false;
	}
	
	@Override
    public int hashCode() {
        return res.hashCode() + node.hashCode();
    }
}
