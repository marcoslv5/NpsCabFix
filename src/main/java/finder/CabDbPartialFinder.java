package finder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

import com.opencsv.CSVReader;

import beans.CabComuni;
import beans.ItemCsv;
import config.Constants;

/**
 * RICERCA CAB SU DB PER NOME PARZIALE
 * Legge un csv con i cab da trovare,
 * ricerca sul db per like comune parziale, solo n caratteri.
 * Crea 1 file csv con i nuovi cab trovati.
 *  
 * @author Marco Salvatore
 *
 */
public class CabDbPartialFinder {

	private static final String FILE_CSV_IN = CabFinder.getProperty(Constants.CONFIG_FILE_CSV);
	//CabFinder.FILE_CSV_IN+"-notMatch-SS-notMatch";
	private static final String FILE_CSV_OUT = FILE_CSV_IN;
	
	private static final String PATH_FILE = CabFinder.getProperty(Constants.CONFIG_FILE_PATH);
	//"C:\\Nps\\Doc\\csv\\";
	private static final int N_PARTIAL_CHAR = 4;
	
	static Map<String, ItemCsv> cacheFound = new HashMap<String, ItemCsv>();
	
	public static void main(String[] args) {
		
		try {
			Instant start = Instant.now();
			System.out.println("Ini");
			
			int count = 1;
			
			List<ItemCsv> listaIn = readCsv(FILE_CSV_IN);
			//log.error("xxxx");
			
			Connection conn = CabFinder.getConnection();
			
			List<ItemCsv> listaSi = new ArrayList<ItemCsv>();
			List<ItemCsv> listaNo = new ArrayList<ItemCsv>();
			
			for (ItemCsv itemCsv : listaIn) {


				System.out.println(count+"-"+itemCsv.getAddressCity()+"---"+itemCsv.getProvince());
				count++;
				
				ItemCsv cacheItem = cacheFound.get(itemCsv.getAddressCity());
				if(cacheItem!=null) {
					listaSi.add(cacheItem);
					continue;
				}
				
				String comuneRaw = null;
				
				if(itemCsv.getAddressCity().length()>4) {
					String address = itemCsv.getAddressCity().trim();
					comuneRaw = "%"+address.substring(0, N_PARTIAL_CHAR)+"%";
				}else {
					comuneRaw = "%"+itemCsv.getAddressCity().trim()+"%";
				}
				
				List<CabComuni> comuni = CabFinder.findCabDb(comuneRaw, itemCsv.getProvince(), conn);
				
				if(!comuni.isEmpty()) {
					int i = 1;
					for (CabComuni c : comuni) {
						System.out.println(i + "-ADDRESS : " + c.getComune()+"-"+itemCsv.getProvince());
						i++;
					}
					
					Scanner scanner = new Scanner(System.in);

					System.out.println("Quale address? ");
					int choice = scanner.nextInt(); // get integer

					
					if(choice>0) {
						try {
							System.out.format("Indirizzo scelto : %d, Cab %s", choice, getAddressComuni(choice, comuni).getComune());
							CabComuni comune = getAddressComuni(choice, comuni);
							itemCsv.setAddressCityMatch(comune.getComune());
							itemCsv.setCab(comune.getCab());
							listaSi.add(itemCsv);
							cacheFound.put(itemCsv.getAddressCity(), itemCsv);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}else {
						listaNo.add(itemCsv);
					}

				}else {
					listaNo.add(itemCsv);
				}
				
				System.out.println("--------");
				System.out.println();
				System.out.println();
				
			}
			
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			CabCapFinder.writeCsv(listaSi, true, FILE_CSV_OUT);
			CabCapFinder.writeCsv(listaNo, false, FILE_CSV_OUT);
			
			clearFiles();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Fine");
	}
	
	private static CabComuni getAddressComuni(int choice, List<CabComuni> comuni) {
		return comuni.get(choice-1);
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

	private static void clearFiles() {
		
		File fileNotMatchSi = new File(PATH_FILE+FILE_CSV_IN+"-si.csv");
		File fileNotMatchNo = new File(PATH_FILE+FILE_CSV_IN+"-no.csv");
		
        try {
            String data = FileUtils.readFileToString(fileNotMatchSi, "UTF-8");
            data = data.replace("\"", "");
            FileUtils.writeStringToFile(fileNotMatchSi, data, "UTF-8");
            
            data = FileUtils.readFileToString(fileNotMatchNo, "UTF-8");
            data = data.replace(";\"***\";", ";;");
            data = data.replace("\"", "");
            FileUtils.writeStringToFile(fileNotMatchNo, data, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
}
