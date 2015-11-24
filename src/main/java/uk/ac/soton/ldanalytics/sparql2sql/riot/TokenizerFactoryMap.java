package uk.ac.soton.ldanalytics.sparql2sql.riot;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.riot.tokens.TokenizerFactory;
import org.apache.jena.riot.tokens.TokenizerText;

public class TokenizerFactoryMap extends TokenizerFactory {
    
    /** Discouraged - be careful about character sets */
    @Deprecated
    public static Tokenizer makeTokenizer(Reader reader) {
        PeekReader peekReader = PeekReader.make(reader) ;
        Tokenizer tokenizer = new TokenizerTextMap(peekReader) ;
        return tokenizer ;
    }

    /** Safe use of a StringReader */
    public static Tokenizer makeTokenizer(StringReader reader) {
        PeekReader peekReader = PeekReader.make(reader) ;
        Tokenizer tokenizer = new TokenizerTextMap(peekReader) ;
        return tokenizer ;
    }

    public static Tokenizer makeTokenizerUTF8(InputStream in) {
        // BOM will be removed
        PeekReader peekReader = PeekReader.makeUTF8(in) ;
        Tokenizer tokenizer = new TokenizerTextMap(peekReader) ;
        return tokenizer ;
    }

    public static Tokenizer makeTokenizerASCII(InputStream in) {
        PeekReader peekReader = PeekReader.makeASCII(in) ;
        Tokenizer tokenizer = new TokenizerTextMap(peekReader) ;
        return tokenizer ;
    }

    public static Tokenizer makeTokenizerASCII(String string) {
        byte b[] = StrUtils.asUTF8bytes(string) ;
        ByteArrayInputStream in = new ByteArrayInputStream(b) ;
        return makeTokenizerASCII(in) ;
    }

    public static Tokenizer makeTokenizerString(String str) {
        PeekReader peekReader = PeekReader.readString(str) ;
        Tokenizer tokenizer = new TokenizerTextMap(peekReader) ;
        return tokenizer ;
    }
}
