package cc.mallet.util;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class MyTagger {

   private MaxentTagger tagger = null;
    
   public MyTagger(){
       try{

         this.tagger =new MaxentTagger("english-left3words-distsim.tagger");  
       }catch(Exception e){
           // javax.swing.JOptionPane.showMessageDialog(null,"Fail to create MyTagger"+e.toString());
           
       }
        
   }
   public MaxentTagger getTagger() {
    return this.tagger;

   }
   public String Tag(String sentence){
    String tagged = this.tagger.tagString(sentence);
    return tagged;
   }

}