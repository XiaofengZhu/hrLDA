package cc.mallet.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupService {
	 private String token;
	 private String model;
	 private String serviceUri;
	 
	 
	 public LookupService() throws Exception{//
		 init(null,null,null);
	 }
	 public LookupService(String token, String model, String serviceUri) throws Exception{
		 init(token,model,serviceUri);
	 }//LookupService
	 
	 private void init(String token, String model, String serviceUri) throws Exception{
		 if(token==null){
		 this.token=System.getenv("NGRAM_TOKEN");//if system environment is set
		 }else{
		 this.token=token;
		 }
		 if(this.token==null){
			 throw new Exception("token must be specified, either asan argument, or as an environment variable named NGRAM_TOKEN");
		 }
		 
		 if(model==null){
			 String temModel=System.getenv("NGRAM_MODEL");//if system environment is set
			 if(temModel!=null){
				 this.model=parseModel(temModel);
	 
			 }else{
				 this.model = "bing-body/2013-12/5";
			 }
		 }else{
			 this.model=parseModel(model);
		 }
		 
		 if(serviceUri==null){
			 this.serviceUri=System.getenv("NGRAM_SERVICEURI");//if system environment is set
			 if(this.serviceUri==null){
				 this.serviceUri="http://weblm.research.microsoft.com/rest.svc/";
			 }
		 }else{
			 this.serviceUri=serviceUri;
		 }
	 }//init LookupService
	 
	 private static String parseModel(String model){
		 Pattern p=Pattern.compile("urn:ngram:(.*):(.*):(\\d+)");
		 Matcher mat=p.matcher(model);
		 if(mat.find()){
			return mat.group(1)+"/" + mat.group(2) + "/" + 
			mat.group(3);
		 } else return model;
	 }
	 
	 public String getModel(){
	 
		 return this.model;
	 }
	 
	 public static String[] getModels() throws Exception{
		 LookupService service=new LookupService("bogus",null,null);
		 URL url=new URL(service.serviceUri);
		 BufferedReader in=new BufferedReader(new 
		 InputStreamReader(url.openStream()));
		 List<String> list=new ArrayList();
		 String str="";
		 while((str=in.readLine())!=null){
			 list.add(str);
		 }
		 in.close();
	 return list.toArray(new String[0]);
	 }
	 
	 public void setModel(String model){
		 this.model=parseModel(model);
	}
	
	 private String encodeURL(String url){
		return url.replace(" ", "%20").replace("&", "%26");
	 }
	
	 private String getData(String phrase,String operation, 
		HashMap<String,String> args) throws Exception{
		if(this.model==null){
			throw new Exception("model must be specified, either as an argument to the LookupService constructor, or as an environment variable named NGRAM_MODEL");
		}
		String urlAddr=this.serviceUri+this.model+"/"+operation+"?p="+encodeURL(phrase)+"&u="+this.token;
		if(args!=null){
			Iterator it=args.keySet().iterator();
			String tem="";
			while(it.hasNext()){
				tem=(String)it.next();
				urlAddr=urlAddr+"&"+tem+"="+encodeURL(args.get(tem));
			}
		}
		 try{
			 URL url=new URL(urlAddr);
			 URLConnection con=url.openConnection();
			 con.setReadTimeout(10000);
			 con.setConnectTimeout(10000);
			 BufferedReader in=new BufferedReader(new 
			 InputStreamReader(url.openStream()));
			 String str="";
			 String data="";
			 while((str=in.readLine())!=null){
			 data=data+str+"\r\n";
			}
			 return data.trim();
		 }catch(SocketTimeoutException e){
			 System.out.println(urlAddr);
			 return getData(phrase,operation,args);
		}
	}
	
	 private double getProbalilityData(String phrase,String operation) throws NumberFormatException, Exception{
	 return Double.valueOf(getData(phrase,operation,null));
	 }
	 
	 public double getJointProbalility(String phrase) throws Exception, Exception{
	  return getProbalilityData(phrase,"jp");
	 }
	 
	 public double getConditionalProbability(String phrase) throws Exception,Exception{  
		 return getProbalilityData(phrase,"cp");  
	 }
	 
	
	 public List<String> generate(String phrase,int maxgen) throws Exception{	  
		 List<String> result=new ArrayList<String>();
		 HashMap<String,String> arg=new HashMap<String,String>();
		 int nstop=maxgen;
		 int tem=0;
		 while(true){
			 tem=Math.min(1000, nstop);
			 arg.put("n", String.valueOf(tem));
			 String[] res=getData(phrase,"gen",arg).split("\r\n");
			 if(res.length<=2)break;
			  nstop=nstop-res.length+2;
			  arg.put("cookie", res[0]);
			 for(int i=2; i<res.length; i++){
				 result.add(res[i]);
			 }
		}
	 return result;
	 }
  public static void main(String[] args) throws Exception{
	  LookupService lookupService = new LookupService("c4fe4d42-2c7e-471d-859f-d412aee7b432","bing-body/2013-12/5","http://weblm.research.microsoft.com/rest.svc/");
	  //System.out.println(lookupService.getJointProbalility("the largest apple"));
	  System.out.println(lookupService.getConditionalProbability("capital city"));
	  System.out.println(lookupService.getConditionalProbability("the city"));
  }
	}
