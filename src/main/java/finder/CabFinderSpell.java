package finder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.opencsv.CSVReader;

import beans.CabComuni;
import beans.ItemCsv;
import config.Constants;
import ml.symspell.SymSpell;
import ml.symspell.strategy.AddressSearchStategy;
import ml.symspell.strategy.PartialLengthStrategy;
import ml.symspell.strategy.RemoveWordStrategy;
import ml.symspell.strategy.SettingStrategy;
import ml.symspell.strategy.TruncatedWordStrategy;
import utils.FileUtils;
import utils.StringUtils;

public class CabFinderSpell {

	private static final Logger log = LogManager.getLogger(CabFinderSpell.class);
	
	private static final String FILE_CSV_IN = CabFinder.FILE_CSV_IN+"-notMatch";//-notMatch";//"test";//
	private static final String PATH_FILE = "C:\\Nps\\Doc\\csv\\";

	
	private static List<ItemCsv> listaIn = new ArrayList<ItemCsv>();
	private static List<ItemCsv> listaOut = new ArrayList<ItemCsv>();
	private static List<ItemCsv> listaNotMatch =  new ArrayList<ItemCsv>();
	
	
	private static List<String> listaComuniSpell = new ArrayList<>();
	private static Map<String, String> listaComuniCab = new HashMap<>();
	
	static Map<String, ItemCsv> cachePrevious = new HashMap<String, ItemCsv>();
	static Map<String, Map<String, ItemCsv>> cachePreviousByProv = new HashMap<String, Map<String, ItemCsv>>();
	
	private static final int SYM_SPELL_DISTANCE_MAX = 2;
	private static boolean acceptSecondHitAsSuccess = false;
	
	private static boolean detailLogOn = true;
	
	private static Connection conn = null;
			
	public static void main(String[] args) {

		Instant start = Instant.now();
		
		try {
			
			log.info("START");
			
			//lista in
			readCsvIn();
			
			listaNotMatch.addAll(listaIn);
			
			//ricerca nei finali precedenti
			findInPrevious();
			
			//ricerca con SymSpell
			findBySymSpell();
			
			//ricerca con SymSpell da stringa parziale
			findBySymSpellEndProv();
			
			//ricerca con SymSpell con address precedenti
			//findBySymSpellPrevious();
			
			//ricerca con SymSpell con address parziali
			findBySymSpellPartial();
			
			//liste out
			writeCsvOut();
			
			log.info("END");
			
		} catch (Exception e) {
			log.error(e.getMessage());
		}finally {
			if(conn!=null)
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		
		//TODO write stat
		
		Instant end = Instant.now();
		Duration timeElapsed = Duration.between(start, end);
		System.out.println("Total time: "+ timeElapsed.getSeconds() +" sec");
	}

	private static void writeCsvOut() throws IOException {
		
		//FileUtils.writeCsv(listaOut, FILE_CSV_IN, false);
		//FileUtils.writeCsv(listaNotMatch, FILE_CSV_IN, false);
		listaOut.addAll(listaNotMatch);
		FileUtils.writeCsv(listaOut, FILE_CSV_IN+"-SS", false);
		
		FileUtils.cleanFiles(FILE_CSV_IN+"-SS");
		
	}

	private static void findBySymSpell() throws SQLException {
		log.info("-----------------------------");
		log.info("findBySymSpell");
		
		List<ItemCsv> listaNotMatchApp =  new ArrayList<ItemCsv>();
		
		listaComuniSpell = getComuniSpellFromDb();
		
		//SymSpell.CreateDictionary("C:\\Nps\\NpsTools\\comuni-spell.txt","");
		SymSpell.CreateDictionary(getComuniSpellWithoutCab(listaComuniSpell));
		
		for (ItemCsv item : listaNotMatch) {
			try {

				if(item.getAddressCity()==null || item.getProvince()==null
						|| item.getAddressCity().trim().isEmpty() || item.getProvince().trim().isEmpty()){
					listaNotMatchApp.add(item);
					continue;
				}
				
				List<String> results = new ArrayList<>();
				String candidate = (item.getProvince()+"_"+item.getAddressCity().replace(" ", "_")).toLowerCase();
				SymSpell.Lookup(candidate, "", SYM_SPELL_DISTANCE_MAX).forEach(x -> results.add(x.term));
				
				// first or second match count as success
				String match = StringUtils.matchCandidate(item.getAddressCity(), results);
				if (match!=null) {
					item.setAddressCityMatch(convertToAddress(match));
					item.setCab(listaComuniCab.get(match.toUpperCase()));
					listaOut.add(item);
					logItem(item);
					//success++;
					//truePositives++;
				}else {
					listaNotMatchApp.add(item);
				}
			} catch (Exception e) {
				listaNotMatchApp.add(item);
				e.printStackTrace();
				log.error(item.getAddressCity());
			}
		}
		
		listaNotMatch.clear();
		listaNotMatch.addAll(listaNotMatchApp);
		
		stepStat();
	}


	private static void findBySymSpellEndProv() {
		log.info("-----------------------------");
		log.info("findBySymSpellEndProv");
		
		List<ItemCsv> listaNotMatchApp =  new ArrayList<ItemCsv>();
		
		for (ItemCsv item : listaNotMatch) {
			try {

				if(item.getAddressCity()==null || item.getProvince()==null
						|| item.getAddressCity().trim().isEmpty() || item.getProvince().trim().isEmpty()){
					listaNotMatchApp.add(item);
					continue;
				}
				
				List<String> results = new ArrayList<>();
				String candidate = (item.getProvince()+"_"+item.getAddressCity().replace(" ", "_")).toLowerCase();
				
				if(candidate==null || candidate.length()<=5 || !item.getProvince().equalsIgnoreCase(candidate.substring(candidate.length()-2))) {
					listaNotMatchApp.add(item);
					continue;
				}
				
				candidate = candidate.substring(0, candidate.length()-2);
				
				SymSpell.Lookup(candidate, "", SYM_SPELL_DISTANCE_MAX).forEach(x -> results.add(x.term));
				
				// first or second match count as success
				String match = StringUtils.matchCandidate(item.getAddressCity(), results);
				if (match!=null) {
					item.setAddressCityMatch(convertToAddress(match));
					item.setCab(listaComuniCab.get(match.toUpperCase()));
					listaOut.add(item);
					logItem(item);
					//success++;
					//truePositives++;
				}else {
					listaNotMatchApp.add(item);
				}
			} catch (Exception e) {
				listaNotMatchApp.add(item);
				e.printStackTrace();
				log.error(item.getAddressCity());
			}
		}
		
		
		listaNotMatch.clear();
		listaNotMatch.addAll(listaNotMatchApp);
		
		stepStat();
	}	
	
	private static void findBySymSpellPrevious() {
		log.info("-----------------------------");
		log.info("findBySymSpellPrevious");
		
		List<ItemCsv> listaNotMatchApp =  new ArrayList<ItemCsv>();
		
		List<String> listaComuniSpellPrevious = getComuniSpellPrevious();
		SymSpell.CreateDictionary(getComuniSpellWithoutCab(listaComuniSpellPrevious));
		
		for (ItemCsv item : listaNotMatch) {
			try {

				if(item.getAddressCity()==null || item.getProvince()==null
						|| item.getAddressCity().trim().isEmpty() || item.getProvince().trim().isEmpty()){
					listaNotMatchApp.add(item);
					continue;
				}
				
				List<String> results = new ArrayList<>();
				String candidate = (item.getProvince()+"_"+item.getAddressCity().replace(" ", "_")).toLowerCase();
				SymSpell.Lookup(candidate, "", SYM_SPELL_DISTANCE_MAX).forEach(x -> results.add(x.term));
				
				// first or second match count as success
				String match = StringUtils.matchCandidate(item.getAddressCity(), results);
				if (match!=null) {
					item.setAddressCityMatch(convertToAddress(match));
					//item.setCab(listaComuniCab.get(match.toUpperCase()));
					//TODO dal match bisogna trovare la descrizione coretta
					String cab = listaComuniCab.get(match.toUpperCase());
					if(cab==null || cab.isEmpty()) {//TODO cab != ***
						//System.out.println("--->"+match);
						listaNotMatchApp.add(item);
						continue;
					}
					
					item.setCab(cab);
					listaOut.add(item);
					logItem(item);
					//success++;
					//truePositives++;
				}else {
					listaNotMatchApp.add(item);
				}
			} catch (Exception e) {
				listaNotMatchApp.add(item);
				e.printStackTrace();
				log.error(item.getAddressCity());
			}
		}
		
		
		listaNotMatch.clear();
		listaNotMatch.addAll(listaNotMatchApp);
		
		stepStat();
	}	
	
	private static void findBySymSpellPartial() {
		log.info("-----------------------------");
		log.info("findByPartial");
		
		List<ItemCsv> listaNotMatchApp =  new ArrayList<ItemCsv>();
	
		for (ItemCsv itemCsv : listaNotMatch) {
			
			try {
				log.debug("----- PR:"+itemCsv.getProvince()+ " AD:"+itemCsv.getAddressCity());
				
				if(itemCsv.getProvince().isEmpty() || itemCsv.getAddressCity().isEmpty()
						|| itemCsv.getAddressCity().length()<=4) {
					listaNotMatchApp.add(itemCsv);
					continue;
				}
				
				CabComuni best = evaluateBest(itemCsv.getAddressCity(), itemCsv.getProvince());
				
				if(best!=null) {
					itemCsv.setAddressCityMatch(best.getComune());
					itemCsv.setCab(best.getCab());
					//log.debug("----------->"+best.getComune()+"-"+best.getCab());
					listaOut.add(itemCsv);
					logItem(itemCsv);
				}else {
					log.debug("--->X");
					listaNotMatchApp.add(itemCsv);
				}
					
			} catch (Exception e) {
				listaNotMatchApp.add(itemCsv);
				e.printStackTrace();
				log.error(itemCsv.getAddressCity());
			}
			
		}
		
		listaNotMatch.clear();
		listaNotMatch.addAll(listaNotMatchApp);
		
		stepStat();		
	}
	
	private static CabComuni evaluateBest(String candidate, String province/*, List<CabComuni> comuni*/) throws SQLException, Exception {
		
		//Evaluate strategy
		
		AddressSearchStategy strategy = new PartialLengthStrategy();
		strategy.createComuniKeys(candidate, province, CabFinder.getConnection());
		
		CabComuni cc = strategy.lookup(candidate);
		if(cc!=null)
			return cc;
		
		strategy = new RemoveWordStrategy();
		strategy.setSettings(new SettingStrategy(2, 1));
		strategy.createComuniKeys(candidate, province, strategy.getComuniPartialDb());
		
		cc = strategy.lookup(candidate);
		if(cc!=null)
			return cc;
		
		strategy.setSettings(new SettingStrategy(2, 2));
		strategy.createComuniKeys(candidate, province, strategy.getComuniPartialDb());
		
		cc = strategy.lookup(candidate);
		if(cc!=null)
			return cc;
		
		strategy = new TruncatedWordStrategy();
		//strategy.setSettings(new SettingStrategy(2, 1));
		strategy.createComuniKeys(candidate, province, CabFinder.getConnection());
		
		cc = strategy.lookup(candidate);
		if(cc!=null)
			return cc;
		
		return null;
	}

	private static List<CabComuni> getComuniForDictionary(String candidate, List<CabComuni> comuni) {

		Map<String, String> comuniApp = new HashMap<>();
		
		for (CabComuni cabComuni : comuni) {
			String comune = cabComuni.getComune(); 
			String comuneKey = comune.length()>candidate.length()?comune.substring(0, candidate.length()-1):comune;
			if(comuniApp.containsKey(comuneKey))
				return new ArrayList<CabComuni>();
		}
		
		return null;
	}

	private static List<String> getComuniSpellPrevious() {
		
		List<String> listaComuniPrevious = new ArrayList<>();
		
		cachePrevious.forEach((address, item) -> {
			if(item.getProvince()!=null && !item.getProvince().isEmpty()
					&& item.getCab()!=null && !item.getCab().isEmpty()) {
				listaComuniPrevious.add(item.getProvince()+"_"+address.replace(" ", "_")+"##"+item.getCab());
			}
		});
		
		return listaComuniPrevious;
	}

	private static List<String> getComuniSpellWithoutCab(List<String> listaComuniSpell) {
//		return listaComuniSpell.stream()
//				.map(c -> c.substring(0, c.indexOf("##")))
//				.collect(Collectors.toList());
		
		List<String> listaWithoutCab = new ArrayList<>();
		listaComuniCab.clear();
		
		for (String c : listaComuniSpell) {
			String[] a = c.split("##");
			listaWithoutCab.add(a[0]);
			listaComuniCab.put(a[0], a[1]);
		}
		
		return listaWithoutCab;
	}

	private static List<String> getComuniSpellFromDb() throws SQLException {
		log.info("getComuniSpellFromDb");		
		
		Connection connection = CabFinder.getConnection();
		
		PreparedStatement ps = null;
		
		List<String> listaComuniSpell = new ArrayList<>();
		
		try {
			StringBuilder strSelect = new StringBuilder();

			strSelect.append("select regexp_replace(p.sigla_provincia||'_'||C.comune, '\\s', '_')||'##'||c.cab ");
			strSelect.append("from cab_comuni C join province p on C.provincia_id=P.provincia_id ");
			strSelect.append("where data_fine_validita > sysdate ");
			strSelect.append("order by  p.sigla_provincia, comune");
			
			ps = connection.prepareStatement(strSelect.toString());

			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				listaComuniSpell.add(rs.getString(1));
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
		
		return listaComuniSpell;
	}

//	public static Connection getConnection() {
//
////		String svilHost = Constants.DB_HOST_SVIL;
////		String svilUser =  Constants.DB_USER_SVIL;
////		String svilPassword =  Constants.DB_PWD_SVIL;
//		
//		String testHost = Constants.DB_HOST_TEST;
//		String testUser = Constants.DB_USER_TEST;
//		String testPassword = Constants.DB_PWD_TEST;
//		
//		try {
//			Class.forName("oracle.jdbc.driver.OracleDriver");
//			// step2 create the connection object
//			
//			
//			conn = DriverManager.getConnection("jdbc:oracle:thin:@//"+testHost,
//					testUser, testPassword);
//			
//		} catch (ClassNotFoundException | SQLException e) {
//			log.error("Host: "+testHost);
//			log.error("testUser: "+testUser);
//			log.error("testPassword: "+testPassword);
//			e.printStackTrace();
//		}
//
//		return conn;
//	}

	private static void stepStat() {
		log.info("Items Out     : "+listaOut.size()+" "+(listaOut.size()*100/listaIn.size())+"%");
		log.info("Items NotMatch: "+listaNotMatch.size()+" "+(listaNotMatch.size()*100/listaIn.size())+"%");
	}

//	private static String getCabMatch(String match) {
//		
//		for
//		return "999999";
//	}

	private static String convertToAddress(String sIn) {
		return sIn.substring(3).replace("_", " ").toUpperCase();
	}

	private static void findInPrevious() throws IOException {
		log.info("-----------------------------");
		log.info("findInPrevious");
		
		List<ItemCsv> listaNotMatchApp =  new ArrayList<ItemCsv>();
		
		//load previous
		if(cachePrevious.isEmpty()) {
			loadPrevious();
			log.info("CachePrevious: "+cachePrevious.size());
		}
		
		for (ItemCsv item : listaNotMatch) {
			ItemCsv itemPrevious = cachePrevious.get(item.getAddressCity());
			
			if(itemPrevious!=null && itemPrevious.getAddressCityMatch()!=null) {
				item.setAddressCityMatch(itemPrevious.getAddressCityMatch());
				item.setCab(itemPrevious.getCab());
				listaOut.add(item);
				logItem(item);
			}else {
				listaNotMatchApp.add(item);
			}
		}
		
		//TODO Add best in previous
		
		listaNotMatch.clear();
		listaNotMatch.addAll(listaNotMatchApp);
		
		stepStat();
	}

	private static void readCsvIn() throws FileNotFoundException, IOException {
		
		String fileName = PATH_FILE+FILE_CSV_IN+".csv";
		
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> r = reader.readAll();
            r.forEach(x -> listaIn.add(new ItemCsv(x)));
        }		
        
        log.info("Items in: "+listaIn.size());
        
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
				log.debug("Previous:"+f+"-"+listCsv.size());
				
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

	private static void logItem(ItemCsv item) {
		if(detailLogOn) {
			log.info("MATCH:"+item.getAddressCity()+"-->"+item.getAddressCityMatch()+":"+item.getCab());
		}
	}
}
