package ml.symspell.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import beans.CabComuni;
import config.Constants;
import finder.CabFinder;

/**
 * Risolve casi come:
 * In: SAN MARTINO B.A.
 * -SAN MAURO DI SALINE
 * -SAN BONIFACIO
 * -SAN GIOVANNI ILARIONE
 * -SAN GIOVANNI LUPATOTO
 * -SAN MARTINO BUON ALBERGO
 * -SAN PIETRO DI MORUBIO
 * -SAN PIETRO IN CARIANO
 * -SAN ZENO DI MONTAGNA
 * Out: SAN MARTINO BUON ALBERGO
 * @author Marco Salvatore
 *
 */
public class TruncatedWordStrategy implements AddressSearchStategy {

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
		
		String[] aCandidate = candidate.split(" ");
		String lastWordCandidate = aCandidate[aCandidate.length-1];
		boolean withDot = lastWordCandidate.indexOf(".")>0;
		lastWordCandidate = lastWordCandidate.replace(".", "");
		
		if(aCandidate.length<=1 || lastWordCandidate.length()>2)
			return;
			
		List<CabComuni> comuniFiltered = comuni.stream()
				.filter(c -> c.getComune().split(" ").length>=3)
				.collect(Collectors.toList());
		
		for (CabComuni cc : comuniFiltered) {
			String comune =cc.getComune();
			String[] aComune = comune.split(" ");
			
			boolean isOk = true;
			
			for (int i = 0; i < lastWordCandidate.length(); i++) {
				char c1 = lastWordCandidate.charAt(i);
				char c2 = aComune[aComune.length-lastWordCandidate.length()+i].charAt(0);
				if(c1!=c2) {
					isOk=false;
					break;
				}
			}
			if(isOk)
				listaKeys.add(new CabComuniKey(getComuneTruncated(comune, lastWordCandidate.length(), withDot), cc));
		}
	}
	private String getComuneTruncated(String comune, int lastWordLength, boolean withDot) {
		String[] aComune = comune.split(" ");
		String comuneTruncated = "";
		
		for (int i = 0; i < aComune.length; i++) {
			if(i-lastWordLength<0)
				comuneTruncated += aComune[i]+" ";
			else
				comuneTruncated += aComune[i].charAt(0)+(withDot?".":"");
		}
		
		return comuneTruncated;
	}

}
