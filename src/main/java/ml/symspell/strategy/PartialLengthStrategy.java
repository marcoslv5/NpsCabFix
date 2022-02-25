package ml.symspell.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beans.CabComuni;
import config.Constants;
import finder.CabFinder;

/**
 * Risolve casi come:
 * In: POGGIO MI
 *	-POGGIO CATINO
 *	-POGGIO SAN LORENZO
 *	-POGGIO BUSTONE
 *	-POGGIO NATIVO
 *	-POGGIO MIRTETO
 *	-POGGIO MOIANO
 * 	Out: POGGIO MIRTETO
 * @author Marco Salvatore
 *
 */
public class PartialLengthStrategy implements AddressSearchStategy {

	
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

	@Override
	public void createComuniKeys(String candidate, String province, List<CabComuni> comuni) throws Exception {
		Set<String> comuniApp = new HashSet<>();
		
		for (CabComuni cabComuni : comuniPartialDb) {
			String comune = cabComuni.getComune();
			//System.out.println("-"+comune);
			String comuneKey = comune.length()>candidate.length()?comune.substring(0, candidate.length()):comune;
			if(comuniApp.contains(comuneKey)) {
				listaKeys.clear();
				return;
			}else
				comuniApp.add(comuneKey);
			
			listaKeys.add(new CabComuniKey(comuneKey, cabComuni));
		}
		
	}

}
