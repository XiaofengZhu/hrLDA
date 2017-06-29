package  com.hrLDA.mysql;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hrLDA.doc.Document;


public class MySqlDB {
	
    
   private Connection conn=null;
   private Statement st=null;
   private ResultSet rs=null;
   private PreparedStatement mstmt = null;
   private String tableName;
   private String relationTableName;
    
   public MySqlDB(String databaseName,String userName,String password, 
		   String tableName, String relationTableName){
	   
       try{
	       //driver
	       Class.forName("com.mysql.jdbc.Driver").newInstance();
	       //db name, username, password
          this.conn=DriverManager.getConnection("jdbc:mysql://localhost:3306/"+databaseName,userName,password);
	       //sql statement
	      this.st=conn.createStatement();
	      this.tableName = tableName;  
	      this.relationTableName = relationTableName;
       }catch(Exception e){
           javax.swing.JOptionPane.showMessageDialog(null,"Fail to connect mysql database"+e.toString());
           System.exit(0);	// exit from the program 
       }
        
   }// end of MySqlDB(...)
   
   public void createRelationTable() {
	   
	      try{	    	  
		    String createSql=" create table " +this.relationTableName        //create table post_like  
	                +"(" 
	                +" relation_ID BIGINT(15) NOT NULL primary key AUTO_INCREMENT,"  
	                +" subject MEDIUMTEXT NOT NULL,"  
	                +" relation TEXT," 		                
	                +" object MEDIUMTEXT,"  
	                +" file_name TEXT NOT NULL,"  
	                +" sentence LONGTEXT"   		                
	            +");";
		    st.execute("drop table if exists "+this.relationTableName+" ;");  		    
		    st.executeUpdate(createSql);
		    }catch(Exception e){
		    	
			   javax.swing.JOptionPane.showMessageDialog(null,"Fail to create table "
					   +this.relationTableName+ " "+e.toString());
			   System.exit(0);	// exit from the program 		    	
		    }	  
	  }// end of createRelationTable() 

   public void insertDataToRelationTable(Document pptsDocument) {
	   String file_name = pptsDocument.getTxtFileName();
	   HashMap <String, String> relationhashMap = pptsDocument.getSlidesRelationhashMap();
	   
	   if (relationhashMap != null){
		   if (relationhashMap.size() > 0){
			   Iterator<HashMap.Entry<String, String>> iterator = relationhashMap.entrySet().iterator();
			   while(iterator.hasNext()){
			      Map.Entry<String, String> entry = iterator.next();

				   String subject = entry.getKey();
				   String object = entry.getValue();

				   // right way to remove entries from Map,
				   // avoids ConcurrentModificationException
				   iterator.remove();  
				                      		   
				   try{
						String sql = "INSERT INTO "+this.relationTableName
								+" (subject,relation,object,file_name, sentence) "
								+ "values (?, ?, ?, ?, ?)";
				
						mstmt = conn.prepareStatement(sql);
				
						mstmt.setString(1, subject);
						mstmt.setString(2, "contains");
					    mstmt.setString(3, object);
					    mstmt.setString(4, file_name);		
					    mstmt.setString(5, subject + object);			    
					    mstmt.execute();
				  }catch(Exception e){
					  javax.swing.JOptionPane.showMessageDialog(null,"Fail to insert current record"
				   +" to"+this.relationTableName+" "+e.toString());
				  System.exit(0);	// exit from the program 		    	
				}			   
			}// end of while(iterator.hasNext()){   
		   }		   
	   }
  
  } // end of insertDataToRelationTable(...)
   
   public void insertDataToRelationTable(String subject,String relation,String object,
		  String file_name, String sentence) {
      try{
			String sql = "INSERT INTO "+this.relationTableName
				+" (subject,relation,object,file_name, sentence) "
				+ "values (?, ?, ?, ?, ?)";

			mstmt = conn.prepareStatement(sql);
	
		    mstmt.setString(1, subject);
		    mstmt.setString(2, relation);
		    mstmt.setString(3, object);
		    mstmt.setString(4, file_name);		
		    mstmt.setString(5, sentence);			    
		    mstmt.execute();
      }catch(Exception e){
    	  javax.swing.JOptionPane.showMessageDialog(null,"Fail to insert current record"
			   +" to"+this.relationTableName+" "+e.toString());
    	  System.exit(0);	// exit from the program 		    	
	    }	  
  } // end of insertDataToRelationTable(...)
	  
  public void createTable() {
	  
      try{
	    String createSql=" create table " +this.tableName        //create table post_like  
                +"(" 
                +" file_ID BIGINT(11) NOT NULL primary key AUTO_INCREMENT,"  
                +" txtName TEXT ,"  
                +" fileName varchar(300)," 
                +" datePublished varchar(200),"  
                +" dateModified varchar(100)," 
                +" authors varchar(300) " 
            +");";
	    st.execute("drop table if exists "+this.tableName+" ;");  
	    st.executeUpdate(createSql);
	    }catch(Exception e){
	           javax.swing.JOptionPane.showMessageDialog(null,"Fail to create table "
	        		   +this.relationTableName+ " "+e.toString());
	           System.exit(0);	// exit from the program
	    }
      
  }// end of createTable(...)
  public void insertData(Document doc) {
	  
      try{

			String sql = "INSERT INTO "+this.tableName
					+" (txtName, fileName, datePublished, dateModified, authors ) "
					+ "values (?, ?, ?, ?, ?)";

			mstmt = conn.prepareStatement(sql);

		    mstmt.setString(1, doc.getTxtFileName());
		    mstmt.setString(2, doc.getOriginalFilePath());	
		    mstmt.setString(3, doc.getDatePublished());		    
		    mstmt.setString(4, doc.getDateModified());
		    mstmt.setString(5, doc.getAuthors());		    
		    mstmt.execute();
	    }catch(Exception e){
			   javax.swing.JOptionPane.showMessageDialog(null,"Fail to insert current record"
					   +this.tableName+" "+e.toString());	    	
	    }	  
      
  }// end of insertData(...)  
  
  public void insertData(String txtName,String fileName, String datePublished, 
		  String dateModified, String authors) {
	  
      try{

			String sql = "INSERT INTO "+this.tableName
					+" (txtName, fileName, datePublished, dateModified, authors ) "
					+ "values (?, ?, ?, ?, ?)";

			mstmt = conn.prepareStatement(sql);

		    mstmt.setString(1, txtName);
		    mstmt.setString(2, fileName);	
		    mstmt.setString(3, datePublished);		    
		    mstmt.setString(4, dateModified);
		    mstmt.setString(5, authors);		    
		    mstmt.execute();
	    }catch(Exception e){
			   javax.swing.JOptionPane.showMessageDialog(null,"Fail to insert current record"
					   +this.tableName+" "+e.toString());	    	
	    }	  
      
  }// end of insertData(...)  
  
  public String queryFileName(String txtName){
	  
     String result = "";
     String sqlStatement = "SELECT fileName from  "+this.tableName
    		 +" where txtName ="+"'"+txtName+"'";
       try{
           rs=st.executeQuery(sqlStatement);
          while(rs!=null && rs.next()){
        	   //column starts at 1
               result=rs.getString(1);              
          }
           return result;
       }catch(Exception e){
           javax.swing.JOptionPane.showMessageDialog(null,"fail to query record"
        		   +txtName+ " from"+this.tableName +" "+e.toString());
           return null;
       }
       
   }// end of queryFileName(...)
    
   /**Close MySQL DB connection*/ 
   public void close(){
	   
      try{
           
          this.st.close();
          this.mstmt.close();
          this.conn.close();
           
      }catch(Exception e){
          javax.swing.JOptionPane.showMessageDialog(null,"exception in closing the connection"
        		  +e.toString());
      } 
      
   }// end of close()
   
   
}
