package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.StageGenerator;

public class StageGeneratorAlt implements StageGenerator {
	StageGenerator other ;
    
    public StageGeneratorAlt(StageGenerator other)
    {
        this.other = other ;
    }
    
    
    @Override
    public QueryIterator execute(BasicPattern pattern, 
                                 QueryIterator input,
                                 ExecutionContext execCxt)
    {
        // Just want to pick out some BGPs (e.g. on a particular graph)
        // Test ::  execCxt.getActiveGraph() 
        if ( ! ( execCxt.getActiveGraph() instanceof GraphBase ) )
            // Example: pass on up to the original StageGenerator if
            // not based on GraphBase (which most Graph implementations are). 
            return other.execute(pattern, input, execCxt) ;
        
//        System.err.println("MyStageGenerator.compile:: triple patterns = "+pattern.size()) ;

        // Stream the triple matches together, one triple matcher at a time. 
        QueryIterator qIter = input ;
        for (Triple triple : pattern.getList()) {
        	Map<String,String> literals = new HashMap<String,String>();
            qIter = new QueryIterTriplePatternAlt(qIter, triple, execCxt) ;
        }
        return qIter ;
    }
}
