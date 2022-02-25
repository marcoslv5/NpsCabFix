package finder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import beans.ItemCsv;
import utils.FileUtils;

/**
 * RICERCA PER CAP
 * Legge un csv con i cab trovati,
 * legge un csv con i cab NON trovati,
 * ricerca per cap e chiede di scegliere un indirizzo tra tutti i comuni che hanno lo stesso cap.
 * Crea 2 file csv -si e -no, rispettivamente con i nuovi cab trovati e quelli non trovati.
 *  
 * @author Marco Salvatore
 *
 */
public class CabCapFinder {

	
	private static final String FILE_CSV_IN = "2021_05-31_06-03-notMatch";
	private static final String FILE_CSV_OK = "2021_05-31_06-03-out";

	private static final String PATH_FILE = "C:\\Nps\\Doc\\csv\\";
	
	static Map<String, List<ItemCsv>> cacheCapFounded = new HashMap<String, List<ItemCsv>>();
	
//    public static void main(String[] args) {
//
//    	for (int i = 0; i < 3; i++) {
//			
//    		Scanner scanner = new Scanner(System.in);
//    		 System.out.println("Please enter your name : ");
//             String name = scanner.next(); // get string
//
//             System.out.println("Please enter your age : ");
//             int age = scanner.nextInt(); // get integer
//
//             System.out.format("Name : %s, Age : %d", name, age);
//            
//    	}
//    }	
	
	
	public static void main(String[] args) {
		
		try {
			List<ItemCsv> listaIn = readCsv(FILE_CSV_OK);
			List<ItemCsv> listaNotMatch = readCsv(FILE_CSV_IN);
			
			List<ItemCsv> listaSi = new ArrayList<ItemCsv>();
			List<ItemCsv> listaNo = new ArrayList<ItemCsv>();
			
			List<ItemCsv> lista = null;
					
			loadCap(listaIn);
			
			for (ItemCsv itemCsv : listaNotMatch) {

				System.out.println("--ADDRESS: " + itemCsv.getAddressCity());
				System.out.println("1-BEST   : " + itemCsv.getAddressCityBest());
				System.out.println("2-PARTIAL: " + itemCsv.getAddressCityPartial());

				lista = new ArrayList<ItemCsv>();
				lista.add(itemCsv);

				List<ItemCsv> listaCap = cacheCapFounded.get(itemCsv.getCap());

				if (listaCap != null) {
					int i = 3;
					for (ItemCsv cap : listaCap) {
						System.out.println(i + "-ADDRESS : " + cap.getAddressCityMatch());
						lista.add(cap);
						i++;
					}
				}

				Scanner scanner = new Scanner(System.in);

				System.out.println("Quale address? ");
				int choice = scanner.nextInt(); // get integer

				System.out.format("Indirizzo scelto : %d, Cab %s", choice, getAddress(choice, lista));
				System.out.println("--------");
				System.out.println();
				
				if(choice>0) {
					itemCsv.setAddressCityMatch(getAddress(choice, lista));
					itemCsv.setCab(getCab(choice, lista));
					listaSi.add(itemCsv);
				}else {
					listaNo.add(itemCsv);
				}
				// scanner.close();
			}
			
			writeCsv(listaSi, true, FILE_CSV_IN);
			writeCsv(listaNo, false, FILE_CSV_IN);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}
	
	private static String getCab(int choice, List<ItemCsv> lista) {

		switch (choice) {
		case 0:
			return "***";
		case 1:
			return lista.get(0).getCabBest();
		case 2:
			return lista.get(0).getCabPartial();
		default:
			return lista.get(choice-2).getCab();
		}
	}

	private static String getAddress(int choice, List<ItemCsv> lista) {

		switch (choice) {
		case 0:
			return "***";
		case 1:
			return lista.get(0).getAddressCityBest();
		case 2:
			return lista.get(0).getAddressCityPartial();
		default:
			return lista.get(choice-2).getAddressCityMatch();
		}
	}
	
	public static void loadCap(List<ItemCsv> listaOut) {
		
		boolean notFounded = true;
		
		for (ItemCsv itemCsv : listaOut) {
			List<ItemCsv> listaCap = cacheCapFounded.get(itemCsv.getCap());
			if(listaCap==null) {
				listaCap = new ArrayList<ItemCsv>();
			}
			
			notFounded = true;
			
			for (ItemCsv cap : listaCap) {
				if(cap.getAddressCity().equals(itemCsv.getAddressCity())) {
					notFounded = false;
					break;
				}
			}
			
			if(notFounded) {
				listaCap.add(itemCsv);
				cacheCapFounded.put(itemCsv.getCap(), listaCap);
			}
		}
				
	}
	
	public static List<ItemCsv> readCsv(String fileCsv) throws FileNotFoundException, IOException{
		List<ItemCsv> lista=new ArrayList<ItemCsv>();
		
		String fileName = PATH_FILE+fileCsv+".csv";
		
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> r = reader.readAll();
            r.forEach(x -> lista.add(new ItemCsv(x)));
        }
        
		return lista;
	}
    
    public static void writeCsv(List<ItemCsv> listIn, boolean match, String nomeFile) throws IOException {
    	
    	if(listIn == null || listIn.isEmpty()) {
    		System.out.println("Write Lista vuota");
    		return;
    	}

    	List<String[]> listOut = new ArrayList<>();
    	 
    	for (ItemCsv itemCsv : listIn) {
    		listOut.add(match?FileUtils.createItem(itemCsv):FileUtils.createItemNotMatch(itemCsv));
		}
    	
    	/*
    	String[] header = {"SUBJECT_CODE","ADDRESS_CITY","ADDRESS_PROVINCE","CAP","CAB",
							"ADDRESS_MATCH"};
    	
        String[] headerNotMatch = {"SUBJECT_CODE","ADDRESS_CITY","ADDRESS_PROVINCE","CAP","CAB",
				"ADDRESS_MATCH",
				"ADDRESS_BEST","CAB_BEST", "INDEX_BEST",
				"ADDRESS_PARTIAL","CAB_PARTIAL", "INDEX_PARTIAL"};
        
    	listOut.add(match?header:headerNotMatch);
    	*/
    	
    	String suffix = match?"si":"no";
    	
    	try (CSVWriter writer = new CSVWriter(new FileWriter(PATH_FILE+nomeFile+"-"+suffix+".csv"), ';')) {
    		writer.writeAll(listOut);
    	}
    }	
}
