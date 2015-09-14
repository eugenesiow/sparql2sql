package uk.ac.soton.ldanalytics.sparql2sql.model;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;

public class NodeObj {
	private Node res = null;
	private Node node = null;
	
	public NodeObj(Node res, Node node) {
		this.res = res;
		this.node = node;
	}
	
	public Node getResource() {
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
    public int hashCode() {
        return node.hashCode();
    }
}
