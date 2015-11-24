package uk.ac.soton.ldanalytics.sparql2sql.riot;

import java.io.InputStream;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.util.Context;

public class RDFDataManager extends RDFDataMgr {
	
    private static ReaderRIOT getReader(ContentType ct)
    {
        Lang lang = RDFLanguages.contentTypeToLang(ct) ;
        if ( lang == null )
            return null ;
        ReaderRIOTFactory r = RDFParserRegister.getFactory(lang) ;
        if ( r == null )
            return null ;
        return r.create(lang) ;
    }

    // -----
    // Readers are algorithms and must be stateless (or they must create a per
    // run instance of something) because they may be called concurrency from
    // different threads. The Context Reader object gives the per-run
    // configuration.

    private static void process(StreamRDF destination, TypedInputStream in, String baseUri, Lang lang, Context context)
    {
        // Issue is whether lang overrides all.
        // Not in the case of remote conneg, no file extension, when lang is default. 
//        // ---- NEW
//        if ( lang != null ) {
//            ReaderRIOT reader = createReader(lang) ;
//            if ( reader == null )
//                throw new RiotException("No parser registered for language: "+lang.getLabel()) ;
//            reader.read(in, baseUri, lang.getContentType(), destination, context) ;
//            return ;
//        }
//        // ---- NEW
        
        ContentType ct = WebContent.determineCT(in.getContentType(), lang, baseUri) ;
        if ( ct == null )
            throw new RiotException("Failed to determine the content type: (URI="+baseUri+" : stream="+in.getContentType()+")") ;

        ReaderRIOT reader = getReader(ct) ;
        if ( reader == null )
            throw new RiotException("No parser registered for content type: "+ct.getContentType()) ;
        reader.read(in, baseUri, ct, destination, context) ;
    }
	
    /** Read triples into a Model with bytes from an InputStream.
     *  A base URI and a syntax can be provided.
     *  The base URI defualts to "no base" in which case the data should have no relative URIs.
     *  The lang gives the syntax of the stream. 
     * @param graph     Destination for the RDF read.
     * @param in        InputStream
     * @param base      Base URI 
     * @param lang      Language syntax
     */
    public static void read(Graph graph, InputStream in, String base, Lang lang)
    {
        StreamRDF dest = StreamRDFLib.graph(graph) ;
        process(dest, new TypedInputStream(in), base, lang, null) ;
    }

	
    /** Read triples into a Model with bytes from an InputStream.
     *  A base URI and a syntax can be provided.
     *  The base URI defualts to "no base" in which case the data should have no relative URIs.
     *  The lang gives the syntax of the stream. 
     * @param model     Destination for the RDF read.
     * @param in        InputStream
     * @param base      Base URI 
     * @param lang      Language syntax
     */
    public static void read(Model model, InputStream in, String base, Lang lang)
    { read(model.getGraph(), in, base, lang) ; }
}
