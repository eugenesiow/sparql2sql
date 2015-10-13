package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.ARQInternalErrorException;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

public class QueryIterTriplePatternAlt extends QueryIterRepeatApply
{
	private final Triple pattern ;
    
    public QueryIterTriplePatternAlt( QueryIterator input,
                                   Triple pattern , 
                                   ExecutionContext cxt)
    {
        super(input, cxt) ;
        this.pattern = pattern ;
    }

    @Override
    protected QueryIterator nextStage(Binding binding)
    {
        return new TripleMapper(binding, pattern, getExecContext()) ;
    }
    
    static int countMapper = 0 ; 
    static class TripleMapper extends QueryIter
    {
        private Node s ;
        private Node p ;
        private Node o ;
        private Binding binding ;
        private ClosableIterator<Triple> graphIter ;
        private Binding slot = null ;
        private boolean finished = false ;
        private volatile boolean cancelled = false ;

        TripleMapper(Binding binding, Triple pattern, ExecutionContext cxt)
        {
            super(cxt) ;
            this.s = substitute(pattern.getSubject(), binding) ;
            this.p = substitute(pattern.getPredicate(), binding) ;
            this.o = substitute(pattern.getObject(), binding) ;
            this.binding = binding ;
            Node s2 = tripleNode(s, "s") ;
            Node p2 = tripleNode(p, "p") ;
            Node o2 = tripleNode(o, "o") ;
            Graph graph = cxt.getActiveGraph() ;
            
//            System.out.println(s2 + " " + p2 + " " + o2);
            
            ExtendedIterator<Triple> iter = graph.find(s2, p2, o2) ;
            // Stream.
            this.graphIter = iter ;
        }

        private static Node tripleNode(Node node, String type)
        {
            if ( node.isVariable() )
                return Node.ANY ;
            else if(node.isURI()) {
            	if(type.equals("s") || type.equals("o"))
            		return Node.ANY;
            } else if(node.isLiteral()) {
        		return Node.ANY;
            }
            return node ;
        }

        private static Node substitute(Node node, Binding binding)
        {
            if ( Var.isVar(node) )
            {
                Node x = binding.get(Var.alloc(node)) ;
                if ( x != null )
                    return x ;
            }

            return node ;
        }

        private Binding mapper(Triple r)
        {
            BindingMap results = BindingFactory.create(binding) ;

            if ( ! insert(s, r.getSubject(), results) )
                return null ; 
            if ( ! insert(p, r.getPredicate(), results) )
                return null ;
            if ( ! insert(o, r.getObject(), results) )
                return null ;
//            System.out.println(results);
            return results ;
        }
        
        public static void addInfoBinding(Node val, BindingMap results) {
        	int count = 0;
        	Var v = Var.alloc("_info_"+count);
        	Node x = results.get(v) ;
        	while(x!=null) {
        		count++;
        		v = Var.alloc("_info_"+count);
            	x = results.get(v) ;
        	}
        	results.add(v, val);
        }

        private static boolean insert(Node inputNode, Node outputNode, BindingMap results)
        {        	
        	if(inputNode.isURI()) {
        		if(outputNode.isURI()) {
	        		if(outputNode.getURI().contains("{")) {
//	        			System.out.println(inputNode + " " + outputNode);
	        			//check uri
	        			if(FormatUtil.compareUriPattern(inputNode.getURI(), outputNode.getURI())) {
	        				String pattern = outputNode.getURI()+";END";
	        				String uri = inputNode.getURI()+";END";
	        				List<String> cols = FormatUtil.extractCols(pattern);
	        				pattern = pattern.replaceAll("\\{.*?}", "(.*?)");
	        				Matcher m = Pattern.compile(pattern).matcher(uri);
	        				
	        				while(m.find()) {
	        					if(cols.size()<=m.groupCount()) {
		        					for(int i=1;i<=cols.size();i++) {
		        						String rightExpr = m.group(i);
		        						if(rightExpr.startsWith("{")) {
		        							rightExpr = rightExpr.replace("{", "").replace("}", "");
		        						} else {
		        							rightExpr = "'"+rightExpr+"'";
		        						}
		        						addInfoBinding(NodeFactory.createLiteral(cols.get(i-1)+"="+rightExpr),results);
		        					}
	        					}
	        				}
	        				
	        				return true;
	        			}
	        			else
	        				return false;
	        		} else if(!inputNode.equals(outputNode))
	        			return false;
        		} else {
        			return false;
        		}
        	}
//        	if(inputNode.isBlank() && outputNode.isBlank()) {
//        		if(!inputNode.getBlankNodeId().equals(outputNode.getBlankNodeId())) {
//        			return false;
//        		}
//        	}
        	if(inputNode.isLiteral()) {
        		String outVal = outputNode.getLiteralValue().toString();
        		String[] outParts = outVal.split("\\.");
        		if(outParts.length>1) {
        			if(!Character.isDigit(outParts[1].charAt(0))) {
        				addInfoBinding(NodeFactory.createLiteral(outVal+"='"+inputNode.getLiteralValue().toString()+"'"),results);
        				return true;
        			}
        		}
        		if(inputNode.getLiteralDatatype()!=null && outputNode.getLiteralDatatype()!=null) {
	        		if(!inputNode.getLiteralDatatype().equals(outputNode.getLiteralDatatype()))
	        			return false;
        		}
        		if(!inputNode.getLiteralValue().toString().equals(outputNode.getLiteralValue().toString()))
        			return false;
        		return true;
        	}
        	
            if ( ! Var.isVar(inputNode) )
                return true ;
            
            Var v = Var.alloc(inputNode) ;
            Node x = results.get(v) ;
            
            if ( x != null )
                return outputNode.equals(x) ;
            
            results.add(v, outputNode) ;
            return true ;
        }
        
        @Override
        protected boolean hasNextBinding()
        {
            if ( finished ) return false ;
            if ( slot != null ) return true ;
            if ( cancelled )
            {
                graphIter.close() ;
                finished = true ;
                return false ;
            }

            while(graphIter.hasNext() && slot == null )
            {
                Triple t = graphIter.next() ;
                slot = mapper(t) ;
            }
            if ( slot == null )
                finished = true ;
            return slot != null ;
        }

        @Override
        protected Binding moveToNextBinding()
        {
            if ( ! hasNextBinding() ) 
                throw new ARQInternalErrorException() ;
            Binding r = slot ;
            slot = null ;
            return r ;
        }

        @Override
        protected void closeIterator()
        {
            if ( graphIter != null )
                NiceIterator.close(graphIter) ;
            graphIter = null ;
        }
        
        @Override
        protected void requestCancel()
        {
            // The QueryIteratorBase machinary will do the real work.
            cancelled = true ;
        }
    }
}
