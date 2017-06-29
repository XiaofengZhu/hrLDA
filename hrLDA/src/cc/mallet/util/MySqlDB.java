package cc.mallet.util;

import java.sql.*;

public class MySqlDB {
    
   private Connection conn=null;
   private Statement st=null;
   private ResultSet rs=null;
   private PreparedStatement mstmt = null;
   private String tableName, tempRelationTableName;
    
   public MySqlDB(String databaseName,String userName,String password, String tableName, String tempRelationTableName){
       try{
	       //driver
	       Class.forName("com.mysql.jdbc.Driver").newInstance();
	       //db name, username, password
          conn=DriverManager.getConnection("jdbc:mysql://localhost:3306/"+databaseName,userName,password);
	       //sql statement
	       st=conn.createStatement();
         this.tableName = tableName;
         this.tempRelationTableName = tempRelationTableName;
       }catch(Exception e){
          javax.swing.JOptionPane.showMessageDialog(null,"Fail to connect mysql database "+e.toString());
          System.exit(0);           
       }
        
   }

  public void createTable() {
      try{
	  	// String tableName = "relations";
	    // String createSql=" create table if not exists" +this.tableName        //create table post_like  
     //            +"(" 
     //            +" relation_ID int(11) NOT NULL primary key AUTO_INCREMENT,"  
     //            +" subject varchar(100) NOT NULL,"  
     //            +" relation varchar(100) ," 		                
     //            +" object varchar(100) ,"  
     //            +" file_name varchar(100) NOT NULL,"  
     //            +" sentence varchar(500)"   		                
     //        +");";
	    st.execute("drop table if exists "+this.tableName +" ;");  
	    // st.executeUpdate(createSql);
      st.execute("create table "+this.tableName+ " like "+this.tempRelationTableName+";");
      st.execute("insert "+this.tableName+ " select * from "+this.tempRelationTableName+" where subject != file_name;");
	    }catch(Exception e){
        javax.swing.JOptionPane.showMessageDialog(null,"Fail to create table "
             +this.tableName+ "\n "
             +"Check if table "+this.tempRelationTableName+" exists"
             +e.toString());
	    	System.exit(0); 
	    }	  
  } 
  
  public void insertData(String subject,String relation,String object,String file_name, String sentence) {
      try{
			String sql = "INSERT INTO "+this.tableName
      +"(subject,relation,object,file_name, sentence)" 
      +"values (?, ?, ?, ?, ?)";

			mstmt = conn.prepareStatement(sql);

		    mstmt.setString(1, subject);
		    mstmt.setString(2, relation);
		    mstmt.setString(3, object);
		    mstmt.setString(4, file_name);		
        mstmt.setString(5, sentence);			    
		    mstmt.execute();
	    }catch(Exception e){
        javax.swing.JOptionPane.showMessageDialog(null,"Fail to insert current record "
          +this.tableName+" "+e.toString());	    	
	    }	  
  }  
   public ResultSet queryRelations(String subject){
    subject = subject.replaceAll("'","''");
     String sqlStatement = "SELECT relation,object,file_name from  "+this.tableName
     +" where subject ="+"'"+subject+"'";
       try{
           rs=st.executeQuery(sqlStatement);
           return rs;
       }catch(Exception e){
           javax.swing.JOptionPane.showMessageDialog(null,"fail to query record "
            +subject+" from "+this.tableName+" "+e.toString());
           return null;
       }
   }
    
   public ResultSet queryObjectRelations(String object){
     object = object.replaceAll("'","''");
     String sqlStatement = "SELECT relation,subject,file_name from  "+this.tableName
     +" where object ="+"'"+object+"'";
       try{
           rs=st.executeQuery(sqlStatement);
           return rs;
       }catch(Exception e){
           javax.swing.JOptionPane.showMessageDialog(null,"fail to query record "
            +object+" from "+this.tableName+" "+e.toString());
           return null;
       }
   }

   public ResultSet querySynonyms(String subject, String object){
     subject = subject.replaceAll("'","''");
     object = object.replaceAll("'","''");
     String sqlStatement = "SELECT * from  "+this.tableName
     +" where subject ="+"'"+subject+"'" +"and object ="+"'"+object+"'";
       try{
           rs=st.executeQuery(sqlStatement);
           return rs;
       }catch(Exception e){
           javax.swing.JOptionPane.showMessageDialog(null,"fail to query data "+e.toString());
           return null;
       }
   }
   public void close(){
      try{
           
          this.st.close();
          this.mstmt.close();
          this.conn.close();
           
      }catch(Exception e){
          javax.swing.JOptionPane.showMessageDialog(null,"exception in closing the connection"+e.toString());
      }       
   }
}
