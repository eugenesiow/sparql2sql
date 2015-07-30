package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.algebra.OpVisitor;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpDiff;
import com.hp.hpl.jena.sparql.algebra.op.OpDisjunction;
import com.hp.hpl.jena.sparql.algebra.op.OpDistinct;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpExtend;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpGraph;
import com.hp.hpl.jena.sparql.algebra.op.OpGroup;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpList;
import com.hp.hpl.jena.sparql.algebra.op.OpMinus;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpOrder;
import com.hp.hpl.jena.sparql.algebra.op.OpPath;
import com.hp.hpl.jena.sparql.algebra.op.OpProcedure;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.algebra.op.OpPropFunc;
import com.hp.hpl.jena.sparql.algebra.op.OpQuad;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadBlock;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpReduced;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpTopN;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;

public class SparqlOpVisitor implements OpVisitor {
	
	RdfTableMapping mapping = null;
	
	public void useMapping(RdfTableMapping mapping) {
		this.mapping = mapping;
	}

	public void visit(OpBGP bgp) {
		//result is a list of table and its columns to select and the variables they are tied to
		for(Model model:mapping.getMapping()) {
			List<Triple> patterns = bgp.getPattern().getList();
			for(Triple t:patterns) {
				Node subject = t.getSubject();
				Node predicate = t.getPredicate();
				Node object = t.getObject();
				Resource s = subject.isVariable() ? null : model.asRDFNode(subject).asResource();
				Property p = predicate.isVariable() ? null : model.createProperty(predicate.getURI());
				RDFNode o = object.isVariable() ? null : model.asRDFNode(object); //TODO: as of now it doesnt make sense to specify a literal object
				StmtIterator stmts = model.listStatements(s, p, o);
				System.out.println("pattern:"+t);
				while(stmts.hasNext()) {
					Statement stmt = stmts.next();
					checkSubject(t,patterns);
					System.out.println(stmt);
				}
			}
		}
	}

	private void checkSubject(Triple originalTriple, List<Triple> patterns) {
		for(Triple t:patterns) {
			if(!t.matches(originalTriple)) {
				if(t.subjectMatches(originalTriple.getSubject())) {
					System.out.println("match subject:"+t);
				}
			}
		}
	}

	public void visit(OpQuadPattern arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpQuadBlock arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpTriple arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpQuad arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpPath arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpTable arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpNull arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpProcedure arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpPropFunc arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpFilter arg0) {
		System.out.println("Filter");
		
	}

	public void visit(OpGraph arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpService arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpDatasetNames arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpLabel arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpAssign arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpExtend arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpJoin arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpLeftJoin arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpUnion arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpDiff arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpMinus arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpConditional arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpSequence arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpDisjunction arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpExt arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpList arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpOrder arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpProject arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpReduced arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpDistinct arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpSlice arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpGroup arg0) {
		System.out.println("group");
		
	}

	public void visit(OpTopN arg0) {
		
		
	}

}
