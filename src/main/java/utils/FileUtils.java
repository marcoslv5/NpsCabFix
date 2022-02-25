package utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import beans.ItemCsv;
import config.Constants;

public class FileUtils {

	private static final Logger log = LogManager.getLogger(FileUtils.class);
	
	private static final String PATH_FILE = "C:\\Nps\\Doc\\csv\\";
	
//	public static void main(String[] args) {
//		addStatFile("ciao");
//		addStatFile("ciao2");
//		addStatFile("ciao3");
//		
//		addStatFile(Arrays.asList("Aston Martin", "Bugatti"));
//	}
	
	public static List<ItemCsv> getFinaliFiles(){
		List<String> listFiles = Stream.of(new File(PATH_FILE+"\\finali").listFiles())
			      .filter(file -> !file.isDirectory())
			      .map(File::getName)
			      .collect(Collectors.toList());
		
		Collections.sort(listFiles); 
		
		List<ItemCsv> listAllCsv = new ArrayList<>();
		
		listFiles.stream().forEach(f -> {
			try {
				listAllCsv.addAll((FileUtils.readCsv(PATH_FILE+"\\finali\\", f)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		return listAllCsv;
	}
	
	public static List<ItemCsv> readCsv(String path, String file) throws FileNotFoundException, IOException{
		List<ItemCsv> lista=new ArrayList<ItemCsv>();
		
		String fileName = path+file;
		
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> r = reader.readAll();
            r.forEach(x -> lista.add(new ItemCsv(x)));
        }
        
		return lista;
	}
	
    public static List<String> getListNpsForSymSpell(String fileName) {
    	
    	List<String> listSS = new ArrayList<>();
    	
    	File f = new File(fileName);
        if(!(f.exists() && !f.isDirectory()))
        {
            System.out.println("File not found: " + fileName);
            return null;
        }

        System.out.println("Load test file ...");
        long startTime = System.currentTimeMillis();
        
        BufferedReader br = null;
        try {
			br = new BufferedReader(new FileReader(fileName));
			
			br.readLine();
			
	        String line;
	        while ((line = br.readLine()) != null) 
	        {
	        	listSS.add(parseWordsTestNps(line));
	        }
        }
        catch (Exception e) {
			e.printStackTrace();
		}
        //wordlist.TrimExcess();
        long endTime = System.currentTimeMillis();
        System.out.println("\rTest file: " + listSS.size() + " words"+ " in " + (endTime-startTime)+"ms ");
        
        return listSS;
	}
    
	//IN: RSURNL77D57Z129J;SAN GIORGIO;MN;46030
	//OUT: mn_san_giorgio
	private static String parseWordsTestNps(String line) {
		String[] splitLine = line.split(";");
		String s = splitLine[2].trim().toLowerCase()+"_"+splitLine[1].trim().replace(" ", "_").toLowerCase();
		
		return s;
	}		
	
    public static void writeCsv(List<ItemCsv> listIn, String fileCsvName, boolean console) throws IOException {
    	
    	if(listIn == null || listIn.isEmpty()) {
    		log.error("Write Lista vuota");
    		return;
    	}

        String[] header = {"SUBJECT_CODE","ADDRESS_CITY","ADDRESS_PROVINCE","CAP","CAB",
        					"ADDRESS_MATCH"};
        
        String[] headerNotMatch = {"SUBJECT_CODE","ADDRESS_CITY","ADDRESS_PROVINCE","CAP","CAB",
									"ADDRESS_MATCH",
									"ADDRESS_BEST","CAB_BEST", "INDEX_BEST",
									"ADDRESS_PARTIAL","CAB_PARTIAL", "INDEX_PARTIAL","ADDRESS_BEST_PREV"};
        
        List<String[]> listOut = new ArrayList<>();
        List<String[]> listNotMatch = new ArrayList<>();
        
        listOut.add(header);
        listNotMatch.add(header);
        //listNotMatch.add(headerNotMatch);
        
        int countTot = 0;
        int countOut = 0;
        int countNotMatch = 0;
        int countBest = 0;
        int countBestPrev = 0;
        int countPartial = 0;
        
        for (ItemCsv itemIn : listIn) {
        	countTot++;
        	switch (evaluate(itemIn)) {
				case 0:
					listOut.add(createItem(itemIn));
					countOut++;
					break;
				case 1:
					String[] itemNotMatch = createItemNotMatch(itemIn);    
	        		//listNotMatch.add(itemNotMatch);
					listNotMatch.add(createItem(itemIn));
	        		countNotMatch++;
					break;
				case 2:
					itemIn.setCab(itemIn.getCabBest());
					itemIn.setAddressCityMatch(itemIn.getAddressCityBest());
					listOut.add(createItem(itemIn));
					countOut++;
					countBest++;
					break;
				case 3:
					itemIn.setCab(itemIn.getCabPartial());
					itemIn.setAddressCityMatch(itemIn.getAddressCityPartial());
					listOut.add(createItem(itemIn));
					countOut++;
					countPartial++;
					break;
				case 4:
					itemIn.setCab(itemIn.getCabBest());
					itemIn.setAddressCityMatch(itemIn.getAddressCityBestPrevious());
					listOut.add(createItem(itemIn));
					countOut++;
					countBestPrev++;
					break;					
				default:
					break;
			}
        	
        	if((countTot+2)!=(listNotMatch.size()+listOut.size())) {
        		System.out.println("ERR");
        	}
        	
		}
        
        log.info("TOT  :"+countTot);
        log.info("OUT  :"+countOut);
        log.info("NOT  :"+countNotMatch);
        log.info("BEST :"+countBest);
        log.info("PART :"+countPartial);
        log.info("BPRE :"+countBestPrev);
        
        if(console) {
        	log.info("-----NOT-MATCH-----");
			log.info(headerNotMatch[1]+";"+headerNotMatch[2]+";"+
					headerNotMatch[5]+";"+headerNotMatch[6]+
					";"+headerNotMatch[8]+";"
					+headerNotMatch[9]+";"+headerNotMatch[11]+";");
			
        	log.info("-----NOT-MATCH-----");
        	for (String[] so : listNotMatch) {
        		log.info(so[1]+";"+so[2]+";"+so[5]+";"+so[6]+";"+so[8]+";"+so[9]+";"+so[11]+";");
			}
        	
        }else {
        	
        	try (CSVWriter writer = new CSVWriter(new FileWriter(PATH_FILE+fileCsvName+"-out.csv"), ';')) {
        		writer.writeAll(listOut);
        	}
        	
        	try (CSVWriter writer = new CSVWriter(new FileWriter(PATH_FILE+fileCsvName+"-notMatch.csv"), ';')) {
        		writer.writeAll(listNotMatch);
        	}
        }
    }
    
	private static int evaluate(ItemCsv itemIn) {
    	
    	if(!itemIn.getCab().equals("***")){
    		return 0;
    	}
    	
    	//Best Match = Partial Match
    	if(itemIn.getAddressCityBest()!=null && itemIn.getAddressCityPartial()!=null &&
    			itemIn.getAddressCityBest().equals(itemIn.getAddressCityPartial()))  {
    		return 2;
    	}
    	
    	//Previous Best Match
    	if(itemIn.getAddressCityBestPrevious()!=null)  {
    		return 4;
    	}
    	
    	//Not Match - best and partial
    	if(itemIn.getIndexBest()!=null && evaluateBestMatch(itemIn) &&
    			itemIn.getIndexPartial() !=null && Integer.parseInt(itemIn.getIndexPartial())<Constants.MAX_INDEX_PARTIAL_MATCH)  {
    		return 1;
    	}
    	
    	//Best Match
    	if(itemIn.getIndexBest()!=null && evaluateBestMatch(itemIn))  {
    		return 2;
    	}
    	
    	//Partial Match
    	if(itemIn.getIndexPartial() !=null && Integer.parseInt(itemIn.getIndexPartial())<Constants.MAX_INDEX_PARTIAL_MATCH)  {
    		return 3;
    	}
    	
    	//Not Match
    	return 1;
    }
    
	private static boolean evaluateBestMatch(ItemCsv itemIn) {

		float percentualeIndex = (Integer.parseInt(itemIn.getIndexBest()) * 100/ itemIn.getAddressCity().length());
		
		float percentualeMatch = 100-percentualeIndex;
		
		log.debug("evaluteBestMatch:"+percentualeMatch + itemIn.getAddressCity()+"-"+itemIn.getAddressCityBest());
		
		return percentualeMatch>=Constants.PERC_BEST_MATCH;
		
	}	
	
	public static String[] createItemNotMatch(ItemCsv itemIn) {
		
		return new String[] {itemIn.getSubjectCode(), 
				itemIn.getAddressCity(), 
				itemIn.getProvince(), 
				itemIn.getCap(), 
				itemIn.getCab(),
				itemIn.getAddressCityMatch()/*,
				itemIn.getAddressCityBest(),
				itemIn.getCabBest(),
				itemIn.getIndexBest(),
				itemIn.getAddressCityPartial(),
				itemIn.getCabPartial(),
				itemIn.getIndexPartial(),
				itemIn.getAddressCityBestPrevious()*/};
		
	}

	public static String[] createItem(ItemCsv itemIn) {
    	
    	return new String[] {itemIn.getSubjectCode(), 
				itemIn.getAddressCity(), 
				itemIn.getProvince(), 
				itemIn.getCap(), 
				itemIn.getCab(),
				itemIn.getAddressCityMatch()};
    	
	}	
	
	/**
	 * I file cvs di output sono puliti da " e ***
	 */
	public static void cleanFiles(String fileCsvName) {
		
		File fileOut = new File(PATH_FILE+fileCsvName+"-out.csv");
		File fileNotMatch = new File(PATH_FILE+fileCsvName+"-notMatch.csv");
		
        try {
            String data = org.apache.commons.io.FileUtils.readFileToString(fileOut, "UTF-8");
            data = data.replace("\"", "");
            org.apache.commons.io.FileUtils.writeStringToFile(fileOut, data, "UTF-8");
            
            data = org.apache.commons.io.FileUtils.readFileToString(fileNotMatch, "UTF-8");
            data = data.replace(";\"***\";", ";;");
            data = data.replace("\"", "");
            org.apache.commons.io.FileUtils.writeStringToFile(fileNotMatch, data, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
	public static void addStatFile(String line) {
		List<String> lines = new ArrayList<>();
		lines.add(line);
		addStatFile(lines);
	}
	
	public static void addStatFile(String line, boolean lineSpace) {
		List<String> lines = new ArrayList<>();
		
		if(lineSpace)
			lines.add("\r\n");
		
		lines.add(line);
		addStatFile(lines);
	}

	public static void addStatFile(List<String> lines) {
		Path pathStat = Paths.get(PATH_FILE, "stat.txt");  
		
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(pathStat, StandardOpenOption.APPEND))) {
			statWriteLines(lines, out);
			
		} catch (NoSuchFileException nfe) {
			
			try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(pathStat, StandardOpenOption.CREATE))) {
				
				statWriteLines(lines, out);
				
			} catch (IOException e) {
			    System.err.println(e);
			}
			
		} catch (IOException e) {
		    System.err.println(e);
		}
//		Writer writer = new BufferedWriter(new OutputStreamWriter(
//		        new FileOutputStream(file, true), "UTF-8"));
	}

	private static void statWriteLines(List<String> lines, OutputStream out) throws IOException {
		for (String s : lines) {
			out.write(s.getBytes());
			out.write(("\r\n").getBytes());
		}
	}
}
