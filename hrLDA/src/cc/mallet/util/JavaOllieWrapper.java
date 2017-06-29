package cc.mallet.util;

import java.io.File;
import java.net.MalformedURLException;
import java.text.BreakIterator;
import java.util.Locale;

import edu.knowitall.ollie.Ollie;
import edu.knowitall.ollie.OllieExtraction;
import edu.knowitall.ollie.OllieExtractionInstance;
import edu.knowitall.ollie.confidence.OllieConfidenceFunction;
import edu.knowitall.tool.conf.impl.LogisticRegression;
import edu.knowitall.tool.parse.MaltParser;
import edu.knowitall.tool.parse.graph.DependencyGraph;



/** This is an example class that shows one way of using Ollie from Java. */
public class JavaOllieWrapper {
    // the extractor itself
    private Ollie ollie;

    // the parser--a step required before the extractor
    private MaltParser maltParser;

    // the path of the malt parser model file
    private static final String MALT_PARSER_FILENAME = "engmalt.linear-1.7.mco";

    public JavaOllieWrapper() throws MalformedURLException {
        // initialize MaltParser
        maltParser = new MaltParser(new File(MALT_PARSER_FILENAME));
        // initialize Ollie
        ollie = new Ollie();
    }

    /**
     * Gets Ollie extractions from a single sentence.
     * @param sentence
     * @return the set of ollie extractions
     */
    public Iterable<OllieExtractionInstance> extract(String sentence) {
        // parse the sentence       
        DependencyGraph graph = maltParser.dependencyGraph(sentence); 

        // run Ollie over the sentence and convert to a Java collection
        Iterable<OllieExtractionInstance> extrs = scala.collection.JavaConversions.asJavaIterable(ollie.extract(graph));
        
        return extrs;
    }

    public static void main(String args[]) throws MalformedURLException {
//        System.out.println(JavaOllieWrapper.class.getResource("/logback.xml"));
        // initialize
        JavaOllieWrapper ollieWrapper = new JavaOllieWrapper();
        // extract from a single sentence.
        String source = "U.S. president Barack Obama gave his inaugural address on January \n20, 2013. I like China.";
        source = source.replaceAll("\\s+", " ");
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);

        iterator.setText(source);
        int start = iterator.first();
        for (int end = iterator.next();
            end != BreakIterator.DONE;
            start = end, end = iterator.next()) {
//          System.out.println(source.substring(start,end));
            String sentence = source.substring(start,end);
            
            Iterable<OllieExtractionInstance> extrs = ollieWrapper.extract(sentence);
            LogisticRegression confFunc = OllieConfidenceFunction.loadDefaultClassifier();
            // print the extractions.
            for (OllieExtractionInstance inst : extrs) {
                OllieExtraction extr = inst.extr();
                double conf = confFunc.getConf(inst);
                System.out.println("Arg1=" + extr.arg1().text());
                System.out.println("Rel=" + extr.rel().text());
                System.out.println("Arg2=" + extr.arg2().text());
                System.out.println("Conf=" + conf); 
                System.out.println();
            }         
        }       

    }
}
