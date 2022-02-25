package finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.opencsv.CSVReader;

import beans.CabComuni;
import beans.CabComuniMatch;
import beans.ComuneProv;
import beans.ItemCsv;
import config.Constants;
import utils.FileUtils;
import utils.StringDistance;
import utils.StringUtils;

/**
 * Prima eseguire questo
 * Poi CabDbPaertialFinder
 * 
 * Controllare i cf con ControllaFile3
 * 
 * Il file out finale metterlo nella dir finali
 * 
 * @author Marco Salvatore
 *
 */
public class CabFinder {

	private static final Logger log = LogManager.getLogger(CabFinder.class);

	//private static final String FILE_CSV_IN = "test1";
	public static String FILE_CSV_IN;// = "2022_02_14-02_20";

	public static String PATH_FILE;// = "C:\\Nps\\Doc\\csv\\";
	
	public static Properties prop = null;

	
	private static int countFoundByCap = 0;
	private static int countFoundByPrevious = 0;
	private static int countFoundByBestPrevious = 0;
	
	static Map<String, CabComuniMatch> cacheComuniFounded = new HashMap<String, CabComuniMatch>();
	static Map<String, List<CabComuni>> cacheComuniByProv = new HashMap<String, List<CabComuni>>();
	static Map<String, CabComuni> cacheCapoluoghi = new HashMap<String, CabComuni>();
	static Map<String, List<ItemCsv>> cacheCapFounded = new HashMap<String, List<ItemCsv>>();
	static Map<String, ItemCsv> cachePrevious = new HashMap<String, ItemCsv>();
	static Map<String, Map<String, ItemCsv>> cachePreviousByProv = new HashMap<String, Map<String, ItemCsv>>();


	public static void main(String[] args) throws IOException {

		Instant start = Instant.now();
		
		//loadProperties();
		//log.error("xxxx");
		
		Connection conn = getConnection();

		List<ItemCsv> listaOut = new ArrayList<ItemCsv>();
		List<ItemCsv> listaIn = null;
		
		int testMax = 10000;
		int testCount = 0;
		
		try {
			listaIn = new ArrayList<ItemCsv>();
			
			//listaIn = readCsvTest();
			
			listaIn = readCsv();
			
			log.info(listaIn.size());
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		if (conn == null) {
			log.error("NO CONN");
			return;
		}
			
		for (ItemCsv itemCsv : listaIn) {
			
			if(itemCsv.getSubjectCode().equalsIgnoreCase("SUBJECT_CODE")) 
				continue;
			
			//---
			log.info(itemCsv.getSubjectCode()+"-"+itemCsv.getAddressCity());
			//---
			
			String comuneRaw = itemCsv.getAddressCity();
			String provincia = itemCsv.getProvince();
			
			try {
				
				ItemCsv itemPrevious = findInPrevious(itemCsv);
				
				if(itemPrevious!=null) {
					listaOut.add(itemPrevious);
					countFoundByPrevious++;
					continue;
				}
				
				//---------
				itemPrevious = findBestInPrevious(itemCsv);
				
				if(itemPrevious!=null) {
					listaOut.add(itemPrevious);
					countFoundByBestPrevious++;
					continue;
				}
				//---------
				
				CabComuniMatch cabComuni = findCab(comuneRaw, provincia, conn);
				
				if(cabComuni==null) {
					log.error("NOT FOUND: "+comuneRaw);
					listaOut.add(itemCsv);
				}else {
					ItemCsv itemCsvNew = (ItemCsv) itemCsv.clone();
					
					if(cabComuni.getCodCatastale()==null) {
						findCabBestPartial(comuneRaw, cabComuni, itemCsvNew);
					}else {
						log.info("==> FOUND: "+cabComuni.getComune()+"-"+comuneRaw);
						itemCsvNew.setAddressCityMatch(cabComuni.getComune());
					}
					itemCsvNew.setCab(cabComuni.getCab());
					listaOut.add(itemCsvNew);
					addCapCache(itemCsvNew);
				}
//						log.info("Cab:" + cabComuni.getCab());
				
			} catch (Exception e) {
				e.printStackTrace();
				log.error("ERR:"+itemCsv.getAddressCity()+"-"+itemCsv.getProvince()+":"+e.getMessage());
			}
			
			log.info("------- "+testCount);
			
			
			if(testCount>testMax)
				break;
			else 
				testCount++;
		}
		
		//---
		listaOut = findByCap(listaOut);
		//---
		
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		analisi(listaOut);
		
		// SPELL START
		//listaOut = CabFinderSpell.mainExternal(listaOut, conn);
		//SPELL END
		
		FileUtils.writeCsv(listaOut, FILE_CSV_IN, false);

		FileUtils.cleanFiles(FILE_CSV_IN);
		
		Instant end = Instant.now();
		Duration timeElapsed = Duration.between(start, end);
		System.out.println("Total time : "+ timeElapsed.getSeconds() +" sec");
		FileUtils.addStatFile("Total time : "+ timeElapsed.getSeconds() +" sec", true);
		
	}

	public static String getProperty(String key) {
		if(CabFinder.prop==null) {
			CabFinder.loadProperties();
		}
		return CabFinder.prop.getProperty(key);
	}
	
	private static void loadProperties() {
        try (InputStream input = new FileInputStream("src/main/resources//config.properties")) {

        	Properties properties = new Properties();

            // load a properties file
            properties.load(input);

            // get the property value and print it out
            System.out.println(properties.getProperty(Constants.CONFIG_FILE_PATH));
            System.out.println(properties.getProperty(Constants.CONFIG_FILE_CSV));
            System.out.println(properties.getProperty(Constants.CONFIG_DB_HOST));
            System.out.println(properties.getProperty(Constants.CONFIG_DB_USER));
            System.out.println(properties.getProperty(Constants.CONFIG_DB_PWD));
            
            PATH_FILE = properties.getProperty(Constants.CONFIG_FILE_PATH);
            FILE_CSV_IN = properties.getProperty(Constants.CONFIG_FILE_CSV);
            
//            Constants.DB_HOST_TEST = prop.getProperty("db.host");
//            Constants.DB_USER_TEST = prop.getProperty("db.user");
//            Constants.DB_PWD_TEST = prop.getProperty("db.password");
            
            CabFinder.prop=properties;

        } catch (IOException ex) {
            ex.printStackTrace();
        }
	}

	/**
	 * Dai precedenti file lavorati, crea una map per provincia.
	 * Per la singola provincia trova l'address migliore.
	 * Se questo è valutato buono lo prende.
	 * @param itemCsv
	 * @return
	 */
	private static ItemCsv findBestInPrevious(ItemCsv itemCsv) {
		
		if(itemCsv.getProvince()==null || itemCsv.getProvince().isEmpty())
			return null;
		
		Map<String, ItemCsv> previousByProv = cachePreviousByProv.get(itemCsv.getProvince());
		
		if(previousByProv==null) {
			previousByProv = cachePrevious.entrySet().stream()
		        .filter(x -> x.getValue().getProvince().equals(itemCsv.getProvince()))
		        .filter(x -> x.getValue().getAddressCityMatch() != null)
		        .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
			
			cachePreviousByProv.put(itemCsv.getProvince(), previousByProv);
		}
		
		Collection<ItemCsv> itemsPreviousByProv = previousByProv.values();
		
		int absoluteBest = 1000;
		ItemCsv masterBest = null;
		
		List<ItemCsv> listBest = new ArrayList<>();
		
		for (ItemCsv item : itemsPreviousByProv) {
			int best = StringDistance.distance(item.getAddressCity(), itemCsv.getAddressCity());
			if(best<absoluteBest) {
				masterBest = item;
				absoluteBest = best;
				item.setIndexBestNum(best);
				listBest.add(item);
			}
		}
		
		listBest.sort((o1, o2) ->Integer.compare(o1.getIndexBestNum(), o2.getIndexBestNum()));		
		
		log.info("findBestInPrevious: "+ itemCsv.getAddressCity());
		//findTop10(listBest);
		ItemCsv itemBestPrevious = evaluateBestPrevoius(listBest);
		
		if(itemBestPrevious!=null) {
			itemCsv.setAddressCityBestPrevious(itemBestPrevious.getAddressCityBest());
			itemCsv.setCabBest(itemBestPrevious.getCab());
			return itemCsv;
		}
		
		return null;
	}

	/**
	 * Valuta se un address tra i migliori già lavorati
	 * @param listBest
	 * @return
	 */
	private static ItemCsv evaluateBestPrevoius(List<ItemCsv> listBest) {
		
		if(listBest.isEmpty())
			return null;
		
		ItemCsv itemCsv = listBest.get(0);
		String address = itemCsv.getAddressCity();
		
		boolean selected = false;
		
		if(address.isEmpty() || address.length()<4)
			return null;
		
		switch (itemCsv.getIndexBestNum()) {
		case 1:
			selected=true;
			break;
		case 2:
			if(address.length()>=7)
				selected=true;
			break;
		case 3:
			if(address.length()>=11)
				selected=true;
			break;		
		case 4:
			if(address.length()>=15)
				selected=true;
			break;		
		case 5:
			if(address.length()>=19)
				selected=true;
			break;				
		default:
			break;
		}
		
		//log.info((selected?"":"NOT ")+"SELECTED " + address + " - " + itemCsv.getAddressCityMatch());
		if(selected) {
			log.info("SELECTED " + address + " - " + itemCsv.getAddressCityMatch());
			return itemCsv;
		}else {
			log.info("NOT SELECTED " + address + " - " + itemCsv.getAddressCityMatch());
			return null;
		}
		
		
	}

//	private static void findTop10(List<ItemCsv> listBest) {
//
//		try {
//			for (int i = 0; i < 10; i++) {
//				ItemCsv item = listBest.get(i);
//				log.info((i+1) + " : " + item.getIndexBestNum() + " : " + item.getAddressCity() + " - " + item.getAddressCityMatch());
//			}
//		} catch (IndexOutOfBoundsException e) {
//		}
//		
//	}

//	private static int indexNotEmpty(PriorityQueue<ItemCsv> maxHeap) {
//		if(maxHeap.size()<10)
//		for (int i = 9; i < 0; i--) {
//			if(maxHeap.)
//		}
//			
//		}
//		return 0;
//	}
//
//	private static void addBest10(ItemCsv[] items10, int best, ItemCsv item) {
//
//		
//	}



	/**
	 * Cerca nei valori già trovati dai file precedentemente elaborati
	 * @param item
	 * @return
	 * @throws IOException
	 */
	private static ItemCsv findInPrevious(ItemCsv item) throws IOException {
		if(cachePrevious.isEmpty()) {
			loadPrevious();
			log.info("CachePrevoius: "+cachePrevious.size());
		}
		
		ItemCsv itemPrevious = cachePrevious.get(item.getAddressCity());
		
		if(itemPrevious==null || itemPrevious.getAddressCityMatch()==null)
			return null;
		
		item.setAddressCityMatch(itemPrevious.getAddressCityMatch());
		item.setCab(itemPrevious.getCab());
		
		return item;
	}

	/**
	 * Carica i valori già trovati dai file precedentemente elaborati
	 * @throws IOException
	 */
	private static void loadPrevious() throws IOException{
		
		List<String> listFiles = Stream.of(new File("C:\\Nps\\Doc\\csv\\finali").listFiles())
			      .filter(file -> !file.isDirectory())
			      .map(File::getName)
			      .collect(Collectors.toList());
		
		Collections.sort(listFiles); 
		
		listFiles.stream().forEach(f -> {
//			System.out.println(f);
			try {
				List<ItemCsv> listCsv = FileUtils.readCsv(PATH_FILE+"finali\\", f);
				log.info("Previous:"+f+"-"+listCsv.size());
				
				listCsv.stream()
					.forEach(ic -> putAddressNotEmpty(ic));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	/**
	 * In cache solo se indirizzo valorizzato
	 * @param item
	 * @return
	 */
	private static Object putAddressNotEmpty(ItemCsv item) {
		if(item.getAddressCity()!=null && item.getAddressCity().trim().length()>1) {
			cachePrevious.put(item.getAddressCity(), item);
		}
		return null;
	}

	private static List<ItemCsv> readCsvTest() {
		List<ItemCsv> lista = new ArrayList<ItemCsv>();
		
		lista.add(new ItemCsv("MNFCLD00A51A001J","TERRANOVA DI SIBARI",	"CS", "35131",""));
		lista.add(new ItemCsv("XNFCLD00A51A001X","MONTEROTONO",	"CS", "35131",""));
//		lista.add(new ItemCsv("PLMPRZ74S66B619Q",	"BELLARIA IGEA MARINA",	"RN", "47814", ""));
//		lista.add(new ItemCsv("BRRMNL00H07C573C",	"BELLARIA",	"RN", "47814", ""));
//		lista.add(new ItemCsv("BNVRGN57P61A294Q",	"CADTELAMMARE",	"NA", "80053", ""));
//		lista.add(new ItemCsv("KNTMMM94C13Z317D",	"CALOLZIO",	"LC", "23801", ""));
		
//		listaIn.add(new ItemCsv("VRGSHN79P58Z504I",	"1CZ",	"CZ", "88100", ""));
//		listaIn.add(new ItemCsv("BNDPRI67P27I804B",	"BG",	"BG", "24122", ""));
		
//		listaIn.add(new ItemCsv("BBTMRA55P49E054W",	"GIUGLIANO",	"NA", "80014", ""));
//		listaIn.add(new ItemCsv("BBTMRA55P49E054W",	"GIUGLIANO",	"NA", "80014", ""));
//		listaIn.add(new ItemCsv("BBTMRA55P49E054W",	"GIUGLIANO",	"RM", "80014", ""));
//		listaIn.add(new ItemCsv("BBTMRA55P49E054W",	"GIUGLIANO",	"RM", "80014", ""));
		
//		listaIn.add(new ItemCsv("FRRMRA71M55L259Y",	"TDG",	"NA", "80059", ""));
//		listaIn.add(new ItemCsv("DMSSVT79P27F839S",	"AF",	"NA", "80021", ""));
//		listaIn.add(new ItemCsv("GRDGPP93A31I452O",	"PT",	"SS", "7046", ""));
//		listaIn.add(new ItemCsv("GRLVGN98P50E625B",	"S",	"LI", "57100", ""));
//		listaIn.add(new ItemCsv("CRLGNR71R26L245P",	"BELLARIA IGEA MARINA",	"RM", "47814", ""));
//		listaIn.add(new ItemCsv("BGLNTA77R66H294Q",	"BELLARIA IGEA MARINA",	"RN", "47814", ""));
//		listaIn.add(new ItemCsv("KHYFTN69D61Z330I",	"CIRO MARINA",	"CS", "87062", ""));
//		listaIn.add(new ItemCsv("CMPNGL85S48B774W",	"CIRO MARINA",	"KR", "88811", ""));
		
		//listaIn.add(new ItemCsv("BLTMSL00A58B157F", "LONATE DEL GARDA", "BS", "25017", ""));
		
		return lista;
	}

	/**
	 * Statistiche
	 * 
	 * @param listaOut
	 */
	private static void analisi(List<ItemCsv> listaOut) {
		int countFound = 0;
		int countNotFound = 0;
		int countBest = 0;
		int countPartial = 0;
		
		for (ItemCsv item : listaOut) {
			if(item.getAddressCityMatch()!=null)
				countFound++;
			else {
				countNotFound++;
				if(item.getAddressCityBest()!=null)
					countBest++;
				if(item.getAddressCityPartial()!=null)
					countPartial++;
			}
		}
		
		log.info("Totali      :"+listaOut.size());
		log.info("Trovati     :"+countFound);
		log.info("Non trovati :"+countNotFound);
		log.info("Previous    :"+countFoundByPrevious);
		log.info("Best        :"+countBest);
		log.info("Partial     :"+countPartial);
		log.info("ByCap       :"+countFoundByCap);
		log.info("Best Prev   :"+countFoundByBestPrevious);
		
		FileUtils.addStatFile(Arrays.asList(
				"Totali      :"+listaOut.size(),
				"Trovati     :"+countFound,
				"Non trovati :"+countNotFound,
				"Previous    :"+countFoundByPrevious,
				"Best        :"+countBest,
				"Partial     :"+countPartial,
				"ByCap       :"+countFoundByCap,
				"Best Prev   :"+countFoundByBestPrevious));
		
	}

	/**
	 * Cerca per cap, tra quelli che sono attualmente in elaborazione
	 * @param itemCsv
	 * @return
	 */
	private static List<ItemCsv> findByCap(List<ItemCsv> listaIn) {

		List<ItemCsv> listOut = new ArrayList<ItemCsv>();
		
		for (ItemCsv item : listaIn) {
			if(item.getAddressCityMatch()==null) {
				List<ItemCsv> listaByCap = cacheCapFounded.get(item.getCap());
				item = evaluateBestCapMatch(listaByCap, item);
			}
			
			listOut.add(item);
		}
		
		return listOut;
	}

	/**
	 * Valuta il migliore tra quelli trovati per cap.
	 * 
	 * @param listaByCap
	 * @param itemIn
	 * @return
	 */
	private static ItemCsv evaluateBestCapMatch(List<ItemCsv> listaByCap, ItemCsv itemIn) {
		
		if(listaByCap==null) {
			return itemIn;
		}
		
		int bestDistance = 1000;
		ItemCsv bestItem = null;
		
		for (ItemCsv item : listaByCap) {
			int distance = StringDistance.distance(item.getAddressCity(), itemIn.getAddressCity());
			if(distance<bestDistance) {
				bestDistance = distance;
				bestItem = item;
			}
		}
		
		countFoundByCap++;
		log.info("-----> BestCapMatch:"+itemIn.getAddressCity()+"-"+bestItem.getAddressCityMatch()+" : "+bestDistance);
		
		return bestItem;
	}

	/**
	 * Cerca cab per address parziale
	 *
	 * @param comuneRaw
	 * @param cabComuni
	 * @param itemCsvNew
	 */
	private static void findCabBestPartial(String comuneRaw, CabComuniMatch cabComuni, ItemCsv itemCsvNew) {
		if(cabComuni.getComuneBestMatch()!=null) {
			log.info("==> FOUND BEST   : "+cabComuni.getComuneBestMatch().getComune()+"-"+comuneRaw);
			itemCsvNew.setCabBest(cabComuni.getComuneBestMatch().getCab());
			itemCsvNew.setAddressCityBest(cabComuni.getComuneBestMatch().getComune());
			itemCsvNew.setIndexBest(cabComuni.getIndexBestMatch()+"");
		}
		if(cabComuni.getComunePartialMatch()!=null) {
			log.info("==> FOUND PARTIAL: "+cabComuni.getComunePartialMatch().getComune()+"-"+comuneRaw);
			itemCsvNew.setCabPartial(cabComuni.getComunePartialMatch().getCab());
			itemCsvNew.setAddressCityPartial(cabComuni.getComunePartialMatch().getComune());
			itemCsvNew.setIndexPartial(cabComuni.getIndexPartialMatch()+"");
		}
	}

	/**
	 * Cerca il cab:
	 * - address completo
	 * - address best match
	 * - address best partial match
	 * @param comuneRaw
	 * @param provincia
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	private static CabComuniMatch findCab(String comuneRaw, String provincia, Connection conn) throws Exception {
		log.info("findCab: " + comuneRaw);

		CabComuniMatch cabComuni = null;
		
		cabComuni = cacheComuniFounded.get(new ComuneProv(comuneRaw, provincia).toString());
		
		if(cabComuni!=null) {
			log.info("--> FROM CACHE <--");
			return cabComuni;
		}
		
		//Min 2 char
		if(comuneRaw.length()<2) {
			return null;
		}
		
		//Max 3 char
		if(comuneRaw.length()<=3) {
			return shortMatch(comuneRaw, provincia, conn);
		}
		
		//Like _
		String comuneRawClean = cleanComuneOne(comuneRaw);
		
		List<CabComuni> listaCabComuni = findCabDb(comuneRawClean, provincia, conn);
		
		if(listaCabComuni.isEmpty()) {
			//Like %
			comuneRawClean = cleanComuneMulti(comuneRaw);
			listaCabComuni = findCabDb(comuneRawClean, provincia, conn);
			
			if(!listaCabComuni.isEmpty() && listaCabComuni.size()==1) {
				cabComuni = new CabComuniMatch(listaCabComuni.get(0));
			}else {
				log.debug("MULTI COMUNI");
				listaCabComuni = findAllCabByProvDb(provincia, conn);
//				List<String> listaComuni = listaCabComuni.stream()
//						.map(c -> c.getComune())
//					    .collect(Collectors.toList());
				
				listaCabComuni.forEach(c -> {
				    log.debug(c.getComune());
				});
				
				cabComuni = new CabComuniMatch();
				cabComuni.setComune(comuneRaw);
				//ccm.setProvinciaId(provinciaId);
				
				//--- Partial Match ---
				CabComuniMatch partialComune = StringUtils.partialMatchIndexed(listaCabComuni, comuneRaw);
				cabComuni.setComunePartialMatch(partialComune);
				cabComuni.setIndexPartialMatch(partialComune.getIndexPartialMatch());
				cabComuni.setProvincia(provincia);
				
				//--- Best Match ---
				CabComuniMatch bestComune = StringDistance.minDistanceCabComuni(listaCabComuni, comuneRaw);
				cabComuni.setComuneBestMatch(bestComune);
				cabComuni.setIndexBestMatch(bestComune.getIndexBestMatch());
				cabComuni.setProvincia(provincia);
				
				//--- Contain Partial ---
				if(cabComuni.getComunePartialMatch()==null && 
						cabComuni.getIndexBestMatch()>Constants.MIN_INDEX_BEST_MATCH) {
					
					cabComuni.setComunePartialMatch(containPartial(listaCabComuni, comuneRaw));
				}
				
				//--- Partial Best Match ---
				if(cabComuni.getComunePartialMatch()==null && 
						cabComuni.getIndexBestMatch()>Constants.MIN_INDEX_BEST_MATCH) {
					
					cabComuni.setComunePartialMatch(partialBestMatch(listaCabComuni, comuneRaw));
				}
			}
		}else{
			cabComuni = new CabComuniMatch(listaCabComuni.get(0));
		}
		
		if(cabComuni!=null) {
			cacheComuniFounded.put(new ComuneProv(comuneRaw, provincia).toString(), cabComuni);
		}
		
		return cabComuni;
	}

	/**
	 * Inserisce nella cache per cap
	 * @param itemIn
	 */
	private static void addCapCache(ItemCsv itemIn) {
		
		if(itemIn.getAddressCityMatch()==null)
			return;
		
		List<ItemCsv> listaCap = cacheCapFounded.get(itemIn.getCap());
		if(listaCap==null) {
			listaCap = new ArrayList<>();
		}
		
		boolean notFounded = true;
		
		for (ItemCsv item : listaCap) {
			if(StringDistance.distance(item.getAddressCity(), itemIn.getAddressCity())<2) {
				notFounded = false;
				break;
			}
		}
		
		if(notFounded) {
			listaCap.add(itemIn);
			cacheCapFounded.put(itemIn.getCap(), listaCap);
		}
	}

	/**
	 * Se l'address concide con la provincia, ritorna il capoluogo
	 * 
	 * @param comuneRaw
	 * @param provincia
	 * @param conn
	 * @return
	 * @throws SQLException
	 * @throws Exception
	 */
	private static CabComuniMatch shortMatch(String comuneRaw, String provincia, Connection conn) throws SQLException, Exception {

		String comuneRawCleaned = comuneRaw.replaceAll("[^a-zA-Z]", "");
		
		if(comuneRawCleaned.equals(provincia) ||
				StringDistance.distance(provincia, comuneRawCleaned)==1) {
			
			CabComuni cc = findCapoluogo(provincia, conn);
			
			return new CabComuniMatch(cc);
		}
		
		return null;
	}

	/**
	 * Controlla se l'address è contenuto tra i comuni
	 * 
	 * @param listaCabComuni
	 * @param comuneRaw
	 * @return
	 */
	private static CabComuni containPartial(List<CabComuni> listaCabComuni, String comuneRaw) {

		for (CabComuni cabComune : listaCabComuni) {
			if(cabComune.getComune().contains(comuneRaw))
				return cabComune;
		}
		
		return null;
	}

	/**
	 * Ricerca per address parziale
	 * 
	 * @param listaCabComuni
	 * @param comuneRaw
	 * @return
	 */
	private static CabComuni partialBestMatch(List<CabComuni> listaCabComuni, String comuneRaw) {
		
		for (CabComuni c : listaCabComuni) {
			String[] splitComune = c.getComune().split(" ");
			for (int i = 0; i < splitComune.length; i++) {
				int distance = StringDistance.distance(splitComune[i], comuneRaw);
				if(distance<3)
					return c;
			}
			
			splitComune = c.getComune().split("-");
			for (int i = 0; i < splitComune.length; i++) {
				int distance = StringDistance.distance(splitComune[i], comuneRaw);
				if(distance<3)
					return c;
			}
		}
		
		return null;
	}

	/**
	 * Pulisce gli address compposti da più parole
	 * 
	 * @param comuneRawIn
	 * @return
	 */
	private static String cleanComuneMulti(String comuneRawIn) {
		String comuneRawCleaned = comuneRawIn.replaceAll("[^a-zA-Z]", "%");
		
		comuneRawCleaned = comuneRawCleaned.replace("%%%", "%").replace("%%", "%");
		
		log.debug(comuneRawCleaned);
		
		return comuneRawCleaned;
	}
	
	/**
	 * Pulisce i comuni composti da una sola parola
	 * 
	 * @param comuneRawIn
	 * @return
	 */
	private static String cleanComuneOne(String comuneRawIn) {
		String comuneRawCleaned = comuneRawIn.replaceAll("[^a-zA-Z]", "_");
		
		log.debug(comuneRawCleaned);
		
		return comuneRawCleaned;
	}	

	/**
	 * Cerca l'address sul db
	 * 
	 * @param comuneRaw
	 * @param provincia
	 * @param conn
	 * @return
	 * @throws SQLException
	 * @throws Exception
	 */
	public static List<CabComuni> findCabDb(String comuneRaw, String provincia, Connection conn)
			throws SQLException, Exception {
		PreparedStatement ps = null;
		List<CabComuni> listaCabComuni = new ArrayList<CabComuni>();
		
		try {
			StringBuilder strSelect = new StringBuilder();

			strSelect.append("SELECT c.* ");
			strSelect.append("FROM PERSEO_TRX_DBA.CAB_COMUNI c ");
			strSelect.append("INNER JOIN PERSEO_TRX_DBA.PROVINCE p ON p.PROVINCIA_ID = c.PROVINCIA_ID  ");
			strSelect.append("WHERE c.COMUNE like ? ");
			strSelect.append("AND p.SIGLA_PROVINCIA = ? ");
			strSelect.append("AND data_fine_validita > sysdate ");
			
			ps = conn.prepareStatement(strSelect.toString());

			ps.setString(1, comuneRaw);
			ps.setString(2, provincia);

			ResultSet rs = ps.executeQuery();
			
			log.debug(strSelect);
			log.debug(comuneRaw);
			log.debug(provincia);
			
			while (rs.next()) {
				listaCabComuni.add(toEntity(rs));
			}

			ps.close();
		} catch (SQLException es) {
			log.error("Error Sql " + es.getMessage());
			throw es;
		} catch (Exception e) {
			log.error("Error " + e.getMessage());
			throw e;
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		return listaCabComuni;
	}

	public static CabComuni findComuneByCabDb(String cab, Connection conn)
			throws SQLException, Exception {
		PreparedStatement ps = null;
		
		CabComuni comune = null;
		
		try {
			StringBuilder strSelect = new StringBuilder();

			strSelect.append("SELECT c.* ");
			strSelect.append("FROM PERSEO_TRX_DBA.CAB_COMUNI c ");
			strSelect.append("WHERE c.cab = ? ");
			strSelect.append("AND data_fine_validita > sysdate ");
			
			ps = conn.prepareStatement(strSelect.toString());

			ps.setString(1, cab);

			ResultSet rs = ps.executeQuery();
			
			log.debug(strSelect);
			log.debug(cab);
			
			if (rs.next()) {
				comune = toEntity(rs);
			}

			ps.close();
		} catch (SQLException es) {
			log.error("Error Sql " + es.getMessage());
			throw es;
		} catch (Exception e) {
			log.error("Error " + e.getMessage());
			throw e;
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		return comune;
	}
	
	private static List<CabComuni> findAllCabByProvDb(String provincia, Connection conn)
			throws SQLException, Exception {
		
		List<CabComuni> listaCabComuni = cacheComuniByProv.get(provincia);
		
		if(listaCabComuni!=null) {
			log.info("--> FROM CACHE PROV <--");
			return listaCabComuni;
		}
		
		PreparedStatement ps = null;
		
		listaCabComuni = new ArrayList<CabComuni>();
		
		try {
			StringBuilder strSelect = new StringBuilder();

			strSelect.append("SELECT c.* ");
			strSelect.append("FROM PERSEO_TRX_DBA.CAB_COMUNI c ");
			strSelect.append("INNER JOIN PERSEO_TRX_DBA.PROVINCE p ON p.PROVINCIA_ID = c.PROVINCIA_ID  ");
			strSelect.append("WHERE p.SIGLA_PROVINCIA = ? ");
			strSelect.append("AND data_fine_validita > sysdate ");
			
			ps = conn.prepareStatement(strSelect.toString());

			ps.setString(1, provincia);

			ResultSet rs = ps.executeQuery();
			
			log.debug(strSelect);
			log.debug(provincia);
			
			while (rs.next()) {
				listaCabComuni.add(toEntity(rs));
			}

			ps.close();
		} catch (SQLException es) {
			log.error("Error Sql " + es.getMessage());
			throw es;
		} catch (Exception e) {
			log.error("Error " + e.getMessage());
			throw e;
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		
		cacheComuniByProv.put(provincia, listaCabComuni);
		
		return listaCabComuni;
	}
	
	private static CabComuni findCapoluogo(String provincia, Connection conn)
			throws SQLException, Exception {
		
		CabComuni capoluogo = cacheCapoluoghi.get(provincia);
		
		if(capoluogo!=null) {
			log.info("--> FROM CACHE CAPOLUOGHI <--");
			return capoluogo;
		}
		
		PreparedStatement ps = null;
		
		try {
			StringBuilder strSelect = new StringBuilder();

			strSelect.append("SELECT c.* ");
			strSelect.append("FROM PERSEO_TRX_DBA.CAB_COMUNI c ");
			strSelect.append("INNER JOIN PERSEO_TRX_DBA.PROVINCE p ON p.PROVINCIA_ID = c.PROVINCIA_ID  ");
			strSelect.append("WHERE p.SIGLA_PROVINCIA = ? ");
			strSelect.append("AND UPPER(c.COMUNE) = UPPER(p.PROVINCIA) ");
			strSelect.append("AND data_fine_validita > sysdate ");
			
			ps = conn.prepareStatement(strSelect.toString());

			ps.setString(1, provincia);

			ResultSet rs = ps.executeQuery();
			
			log.debug(strSelect);
			log.debug(provincia);
			
			if (rs.next()) {
				capoluogo = toEntity(rs);
			}

			ps.close();
		} catch (SQLException es) {
			log.error("Error Sql " + es.getMessage());
			throw es;
		} catch (Exception e) {
			log.error("Error " + e.getMessage());
			throw e;
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		
		cacheCapoluoghi.put(provincia, capoluogo);
		
		return capoluogo;
	}	
	
	private static CabComuni toEntity(ResultSet rs) throws SQLException {
		CabComuni cabComuni = new CabComuni();

		cabComuni.setCab(rs.getString("CAB"));
		cabComuni.setComune(rs.getString("COMUNE"));
		cabComuni.setCodCatastale(rs.getString("COD_CATASTALE"));
		cabComuni.setComuneId(rs.getLong("COMUNE_ID"));
		cabComuni.setDataIniValidita(rs.getDate("DATA_INI_VALIDITA"));
		cabComuni.setDataFineValidita(rs.getDate("DATA_FINE_VALIDITA"));
		cabComuni.setProvinciaId(rs.getLong("PROVINCIA_ID"));
		return cabComuni;
	}
	
	public static Connection getConnection() {

		Connection conn = null;

//		String svilHost = Constants.DB_HOST_SVIL;
//		String svilUser =  Constants.DB_USER_SVIL;
//		String svilPassword =  Constants.DB_PWD_SVIL;
		
		String testHost = getProperty(Constants.CONFIG_DB_HOST); //Constants.DB_HOST_TEST;
		String testUser = getProperty(Constants.CONFIG_DB_USER); //Constants.DB_USER_TEST;
		String testPassword = getProperty(Constants.CONFIG_DB_PWD); //Constants.DB_PWD_TEST;
		
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			// step2 create the connection object
			
			
			conn = DriverManager.getConnection("jdbc:oracle:thin:@//"+testHost,
					testUser, testPassword);
			
		} catch (ClassNotFoundException | SQLException e) {
			log.error("Host: "+testHost);
			log.error("testUser: "+testUser);
			log.error("testPassword: "+testPassword);
			e.printStackTrace();
		}

		return conn;
	}
	
	private static List<ItemCsv> readCsv() throws FileNotFoundException, IOException{
		List<ItemCsv> lista=new ArrayList<ItemCsv>();
		
		String fileName = PATH_FILE+FILE_CSV_IN+".csv";
		
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> r = reader.readAll();
            r.forEach(x -> lista.add(new ItemCsv(x)));
//            for (String[] x : r) {
//            	log.info("CF-->"+x[0]);
//            	lista.add(new ItemCsv(x));
//			}
        }
        
		return lista;
	}

}

