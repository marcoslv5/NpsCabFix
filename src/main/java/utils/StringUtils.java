package utils;

import java.util.ArrayList;
import java.util.List;

import beans.CabComuni;
import beans.CabComuniMatch;

public class StringUtils {

	public static void main(String[] args) {
/*
		BestCapMatch:CATELLAMMARE DI STABIA-CASTELLAMMARE DI STABIA : 2
		[main] INFO  2021-05-28 15:52:38,413 - -----> BestCapMatch:CATELVOLTURNO-CASERTA : 11
		[main] INFO  2021-05-28 15:52:38,413 - -----> BestCapMatch:CAVA DE TIRRENI-CAVA DEI TIRRENI : 1
		B0RGOCARBONARA-CARBONARA DI PO : 11
		BARCELLONA-BARCELLONA POZZO DI GOTTO : 6
		:BAGOLI-BARANO D'ISCHIA : 11
		BARCELLONA POZO DI GOTTO-BARCELLONA POZZO DI GOTTO : 2
		BOVISIO MASCIAGOMB-BOVISIO-MASCIAGO : 3
		C S G-CASTEL SAN GIORGIO : 14
		CASSANO ALLO-CASSANO ALLO IONIO
		CASTELFRANO-CASTELFRANCO PIANDISCO'
		CORIGLIANO-CORIGLIANO-ROSSANO
		*/
		List<String> lista = new ArrayList<String>();
		lista.add("CATELLAMMARE DI STABIA#CASTELLAMMARE DI STABIA");
		lista.add("CATELVOLTURNO#CASERTA");
		lista.add("CAVA DE TIRRENI#CAVA DEI TIRRENI");
		lista.add("B0RGOCARBONARA#CARBONARA DI PO");
		lista.add("BARCELLONA#BARCELLONA POZZO DI GOTTO");
		lista.add("BAGOLI#BARANO D'ISCHIA");
		lista.add("BARCELLONA POZO DI GOTTO#BARCELLONA POZZO DI GOTTO");
		lista.add("BOVISIO MASCIAGOMB#BOVISIO-MASCIAGO");
		lista.add("C S G#CASTEL SAN GIORGIO");
		lista.add("BOVISIO MASCIAGOMB#BOVISIO-MASCIAGO");
		lista.add("CASSANO ALLO#CASSANO ALLO IONIO");
		lista.add("CASTELFRANO#CASTELFRANCO PIANDISCO'");
		lista.add("CORIGLIANO#CORIGLIANO-ROSSANO");
		
		for (String s : lista) {
			String[] ss = s.split("#");
			indexPartialMatch(ss[1], ss[0]);
		}
	}
	

	public static void indexPartialMatch(String master, String toMatch) {
		//n parole
		//len parola
		
		//BARCELLONA-BARCELLONA POZZO DI GOTTO
		//1 parola su 4
		//10 char
		
		//BOVISIO MASCIAGOMB-BOVISIO-MASCIAGO
		//1 parola
		//7 char
		//+1parola simile
		
		//CASSANO ALLO-CASSANO ALLO IONIO
		//2 parole
		//7+4 char
		
		//dividere toMatch
		//esiste in master?
		//si
		//no, distance?
		
		String[] splitToMatch = toMatch.split("[\\W]");
		String[] splitMaster = master.split("[\\W]");
		
		int nMatch = indexPartialMatch(splitToMatch, splitMaster);
		
		System.out.println("toMatch:"+toMatch+" - "+"master:"+master+" = "+nMatch);
		
	}


	private static int indexPartialMatch(String[] splitToMatch, String[] splitMaster) {
		int nMatch = 0;
		for (int x = 0; x < splitToMatch.length; x++) {
			
			for (int i = 0; i < splitMaster.length; i++) {
				if(splitMaster[i].equals(splitToMatch[x]))
					nMatch++;
			}
		}
		return nMatch;
	}


	public static boolean partialMatch(String master, String toMatch, String charSplit) {
		
		String[] splitMaster = master.split(charSplit);
		
		for (int i = 0; i < splitMaster.length; i++) {
			String[] splitToMatch = toMatch.split(charSplit);
			for (int j = 0; j < splitToMatch.length; j++) {
				if(splitMaster[i].trim().equals(splitToMatch[j])) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static int countPartialMatch(String master, String toMatch, String charSplit) {
		
		String[] splitMaster = master.split(charSplit);
		int count = 0;
		
		for (int i = 0; i < splitMaster.length; i++) {
			String[] splitToMatch = toMatch.split(charSplit);
			for (int j = 0; j < splitToMatch.length; j++) {
				if(splitMaster[i].trim().equals(splitToMatch[j])) {
					count++;
				}
			}
		}
		
		return count;
	}	
	
	public static CabComuniMatch partialMatchIndexed(String master, String toMatch, String charSplit, CabComuni cabComune) {
		
		CabComuniMatch comuneMatch = new  CabComuniMatch(cabComune);
		
		String[] splitMaster = master.split(charSplit);
		
		for (int i = 0; i < splitMaster.length; i++) {
			String[] splitToMatch = toMatch.split(" ");
			for (int j = 0; j < splitToMatch.length; j++) {
				//if(splitMaster[i].trim().equals(splitToMatch[j])) {
					comuneMatch.setIndexPartialMatch(comuneMatch.getIndexPartialMatch()+StringDistance.distance(splitMaster[i].trim(), splitToMatch[j]));;
				//}
			}
		}
		
		return comuneMatch;
	}

	public static CabComuniMatch partialMatchIndexed(List<CabComuni> masterList, String toMatch) {
		
		List<CabComuniMatch> comuniMatch = new ArrayList<CabComuniMatch>();
		
		for (CabComuni master : masterList) {
			if(partialMatch(master.getComune(), toMatch, " ")) {
				comuniMatch.add(partialMatchIndexed(master.getComune(), toMatch, " ", master));
			}
		}
		
		for (CabComuni master : masterList) {
			if(partialMatch(master.getComune(), toMatch, "-")) {
				comuniMatch.add(partialMatchIndexed(master.getComune(), toMatch, "-", master));
			}
		}
		
		CabComuniMatch comuneMaxMatch = new  CabComuniMatch();
		comuneMaxMatch.setIndexPartialMatch(1000);
		
		for (CabComuniMatch comuneMatch : comuniMatch) {
			
			//System.out.println(comuneMatch.getComune()+":"+comuneMatch.getIndexPartialMatch());
			
			if(comuneMatch.getIndexPartialMatch()<comuneMaxMatch.getIndexPartialMatch()) {
				comuneMaxMatch = comuneMatch;
			}
		}
		
		return comuneMaxMatch;
	}
	
	public static CabComuni partialMatch(List<CabComuni> masterList, String toMatch) {
		
//		if(toMatch.equals("ABETONE CUTIGLIANO")) {
//			toMatch=toMatch;
//		}
		
		for (CabComuni master : masterList) {
			if(partialMatch(master.getComune(), toMatch, " "))
				return master;
		}
		
		for (CabComuni master : masterList) {
			if(partialMatch(master.getComune(), toMatch, "-"))
				return master;
		}
		
		return null;
	}	
	
	
	public static String matchCandidate(String candidate, List<String> results) {
		
		switch (results.size()) {
		case 0:
			return null;
		case 1:
			return results.get(0);
		default:
			return results.get(0);
		}
		
	}
}
