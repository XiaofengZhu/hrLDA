package com.hrLDA.main;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import cc.mallet.pipe.Input2CharSequenceFilter;
import cc.mallet.pipe.Pipe;	// TokenSequence2FeatureSequenceWithBigrams
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequenceWithBigrams;
import cc.mallet.pipe.TokenSequenceNRelations;
import cc.mallet.pipe.TokenSequenceNRelationsNoMS;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.InstanceList;	// InstanceList

public class ImportDirWithRelation {

    private Pipe pipe;
    private String endString,databaseName, username, password, tableName, tempRelationTableName;
    private boolean mwNgram;
    private int numOfThreads;

    public ImportDirWithRelation(String endString,String databaseName, String username, 
    		String password, String tableName, String tempRelationTableName, boolean mwNgram, 
    		int numOfThreads) {
        this.endString=endString;
        this.databaseName=databaseName;
        this.username = username;
        this.password =password;
        this.tableName = tableName;
        this.tempRelationTableName = tempRelationTableName;
        this.mwNgram = mwNgram;
        this.numOfThreads = numOfThreads;
        this.pipe = buildPipe();
    }

    public Pipe buildPipe() {
        ArrayList <Pipe> pipeList = new ArrayList <Pipe>();

        // Read data from File objects
        pipeList.add( new Input2CharSequenceFilter("UTF-8"));
        

		try {
			if (this.mwNgram) pipeList.add(new TokenSequenceNRelations(
					new File("stoplists/en.txt"), "UTF-8", new int[] {2}, true, 
					endString,databaseName, username, password, tableName));
			// Without using MS Ngram Service
			else pipeList.add(new TokenSequenceNRelationsNoMS(
					new File("stoplists/en.txt"), "UTF-8", new int[] {2}, true, 
					endString,databaseName, username, password, 
					tableName, tempRelationTableName,
					numOfThreads));
		} catch (Exception e) {
			System.err.println("Error in building pipe!");
		}
		
        /**Rather than storing tokens as strings, convert 
         *them to integers by looking them up in an alphabet.
         *pipeList.add(new TokenSequence2FeatureSequence());
         */

        pipeList.add(new TokenSequence2FeatureSequenceWithBigrams());

        return new SerialPipes(pipeList);
    }

    public InstanceList readDirectory(File directory) {
        return readDirectories(new File[] {directory});
    }

    public InstanceList readDirectories(File[] directories) {
        
        /**Construct a file iterator, starting with the 
         *specified directories, and recursing through sub directories.
         *The second argument specifies a FileFilter to use to select
         *files within a directory.
         *The third argument is a Pattern that is applied to the 
         *filename to produce a class label. In this case, I've 
         *asked it to use the last directory name in the path.
         */
        FileIterator iterator =
            new FileIterator(directories,
                             new TxtFilter(),
                             FileIterator.LAST_DIRECTORY);
        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        InstanceList instances = new InstanceList(this.pipe);

        // Now process each instance provided by the iterator.
        instances.addThruPipe(iterator);

        return instances;
    }

    
    /** This class illustrates how to build a simple txt file filter */
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