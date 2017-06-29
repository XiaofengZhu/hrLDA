package com.hrLDA.query.ontology;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

public class SPARQL {

	public static void main (String args[]) {
	FileManager.get().addLocatorClassLoader(SPARQL.class.getClassLoader());
	Model model = FileManager.get().loadModel("result.rdf");
       

    // Create a new query
    String queryString =        
      "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  "+   		  
        "select ?uri "+
        "where { "+
         "?uri rdfs:subClassOf <http://semiconductor-packaging/ontology/Area>  "+       
        "} \n ";
    Query query = QueryFactory.create(queryString);

    System.out.println("----------------------");

    System.out.println("Query Result Sheet");

    System.out.println("----------------------");

    System.out.println("Direct&Indirect Descendants (model)");

    System.out.println("-------------------");

   
    // Execute the query and obtain results
    QueryExecution qe = QueryExecutionFactory.create(query, model);
    com.hp.hpl.jena.query.ResultSet results =  qe.execSelect();

    // Output query results    
    ResultSetFormatter.out(System.out, results, query);

    qe.close();
    
    System.out.println("----------------------");
    System.out.println("Only Direct Descendants");
    System.out.println("----------------------");
    
}

}
