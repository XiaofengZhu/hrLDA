package com.hrLDA.main;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import cc.mallet.pipe.*;//TokenSequence2FeatureSequenceWithBigrams
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;//InstanceList

public class ImportDir {

    Pipe pipe;
    String endString;

    public ImportDir(String endString) {
        this.endString=endString;
        pipe = buildPipe();
    }

    public Pipe buildPipe() {
        ArrayList <Pipe> pipeList = new ArrayList <Pipe>();

        // Read data from File objects
        pipeList.add(new Input2CharSequenceFilter("UTF-8"));
//        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false) );
        pipeList.add( new TokenSequence2FeatureSequence() );

        

        /** Regular expression for what constitutes a token.
        *This pattern includes Unicode letters, Unicode numbers, 
        *and the underscore character. Alternatives:
        *"\\S+"   (anything not whitespace)
        *"\\w+"    ( A-Z, a-z, 0-9, _ )
        *"[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        *a group of only punctuation marks)
        */        
//        Pattern tokenPattern =
//                Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}");        
//
//        // Tokenize raw strings
//        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        pipeList.add(new TokenSequenceRemoveNonAlpha());
        
        // Normalize all tokens to all lowercase
        pipeList.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        pipeList.add(new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), 
        		"UTF-8", false, false, false) );


        return new SerialPipes(pipeList);
    }

    public InstanceList readDirectory(File directory) {
        return readDirectories(new File[] {directory});
    }

    public InstanceList readDirectories(File[] directories) {
        
        // Construct a file iterator, starting with the 
        //  specified directories, and recursing through subdirectories.
        // The second argument specifies a FileFilter to use to select
        //  files within a directory.
        // The third argument is a Pattern that is applied to the 
        //   filename to produce a class label. In this case, I've 
        //   asked it to use the last directory name in the path.
        FileIterator iterator =
            new FileIterator(directories,
                             new TxtFilter(),
                             FileIterator.LAST_DIRECTORY);
        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        InstanceList instances = new InstanceList(pipe);

        // Now process each instance provided by the iterator.
        instances.addThruPipe(iterator);

        return instances;
    }

    /** This class illustrates how to build a simple file filter */
    class TxtFilter implements FileFilter {

        /** Test whether the string representation of the file 
         *   ends with the correct extension. Note that {@ref FileIterator}
         *   will only call this filter if the file is not a directory,
         *   so we do not need to test that it is a file.
         */
        public boolean accept(File file) {
            return file.toString().endsWith(".txt");
        }
    }

}