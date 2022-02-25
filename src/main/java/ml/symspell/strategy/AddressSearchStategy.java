package ml.symspell.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import beans.CabComuni;
import ml.symspell.SymSpell;
import utils.StringUtils;

public interface AddressSearchStategy {
	
	List<CabComuni> comuniPartialDb = new ArrayList<>();
	List<CabComuniKey> listaKeys = new ArrayList<>();
	Connection conn = null;
	
	SettingStrategy settings = new SettingStrategy();
	
	static final int DEFAULT_SYM_SPELL_DISTANCE_MAX = 2;
	
	void createComuniKeys(String candidate, String province, Connection conn) throws SQLException, Exception;
	void createComuniKeys(String candidate, String province, List<CabComuni> comuni) throws Exception;
	
//	void setSymSpellDistanceMax(int distance);
//	int getSymSpellDistanceMax(); 
	
	default CabComuni getCabComune(String match) {
		for (CabComuniKey cabComuniKey : listaKeys) {
			if(match.equals(cabComuniKey.getKey()))
				return cabComuniKey;
		}
		return null;
	}
	
	default List<CabComuni> getComuniPartialDb(){
		return comuniPartialDb;
	}
	
	default void setComuniPartialDb(List<CabComuni> comuni){
		comuniPartialDb.addAll(comuni);
	}
	
	default SettingStrategy getSettings(){
		return settings;
	}
	
	default void setSettings(SettingStrategy settingsIn){
		settings.setSymSpellDistanceMax(settingsIn.getSymSpellDistanceMax());
		settings.setnRemoveWord(settingsIn.getnRemoveWord());
	}
	
	default public CabComuni lookup(String candidate) {
		SymSpell.cleanDictionary();
		
		for (CabComuniKey comuneKey : listaKeys) {
			SymSpell.CreateDictionaryEntry(comuneKey.getKey(), "");
		}
		
		List<String> results = new ArrayList<>();
		SymSpell.Lookup(candidate, "", settings.getSymSpellDistanceMax()).forEach(x -> results.add(x.term));
		
		// first or second match count as success
		String match = StringUtils.matchCandidate(candidate, results);
		if (match!=null) {
			//System.out.println("MATCH------> "+match);
			return getCabComune(match);
		}
		return null;
	}
}
