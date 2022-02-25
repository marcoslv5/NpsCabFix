package ml.symspell.strategy;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import beans.CabComuni;
import config.Constants;
import finder.CabFinder;

/**
 * Risolve casi come:
 * In: BARCELLONA DI GOTTO
 *	-BARCELLONA POZZO DI GOTTO
 * 	Out: BARCELLONA POZZO DI GOTTO
 * 
 *  In: BARCELLONA POZZO GOTTO
 *	-BARCELLONA POZZO DI GOTTO
 * 	Out: BARCELLONA POZZO DI GOTTO
 * @author Marco Salvatore
 *
 */
public class RemoveWordStrategy implements AddressSearchStategy {

	@Override
	public void createComuniKeys(String candidate, String province, Connection conn) throws SQLException, Exception {
				
		String comuneRaw = "%"+candidate.substring(0, Constants.N_PARTIAL_CHAR)+"%";
		
		comuniPartialDb.clear();
		comuniPartialDb.addAll(CabFinder.findCabDb(comuneRaw, province, conn));
		
		if(comuniPartialDb.isEmpty()) {
			return;
		}
		
		createComuniKeys(candidate, province, comuniPartialDb);
	}

	private void createListaKeys(String candidate, List<CabComuniKey> comuniNoXdWord)
			throws IllegalAccessException, InvocationTargetException {
		
		Set<String> comuniApp = new HashSet<>();
		
		for (CabComuniKey cck : comuniNoXdWord) {
			String comuneKey = cck.getKey();
			//System.out.println("-"+comuneKey);
			if(comuniApp.contains(comuneKey)) {
				listaKeys.clear();
				return;
			}else
				comuniApp.add(comuneKey);
			
			listaKeys.add(new CabComuniKey(comuneKey, cck));
		}
	}

	private List<CabComuniKey> getComuniNoXWord(List<CabComuni> comuni) {
		return comuni.stream()
		.map(c -> {
			String[] cs = c.getComune().split(" ");
			String key = "";
			for (int i = 0; i < cs.length; i++) {
				if(i!=settings.getnRemoveWord()) {
					key += cs[i]+" ";
				}
			}
			key=key.trim();
			CabComuniKey cck=null;
			try {
				//System.out.println("Key: "+key);
				cck = new CabComuniKey(key, c);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
            return cck;
        }).collect(Collectors.toList());
	}

	@Override
	public void createComuniKeys(String candidate, String province, List<CabComuni> comuni) throws Exception {
		List<CabComuni> comuniFiltered = comuni.stream()
				.filter(c -> c.getComune().split(" ").length>1)
				.collect(Collectors.toList());

		List<CabComuniKey> comuniNoXdWord = getComuniNoXWord(comuniFiltered);
		
		createListaKeys(candidate, comuniNoXdWord);
	}	
	
}
