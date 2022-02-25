package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.CSVWriter;

import beans.ItemCsv;

public class ConvertToSpell {

	private static final String PATH_FILE = "C:\\Nps\\Doc\\csv\\";
	private static List<SpellItem> listToEncode = new ArrayList<>();
	
	public static void main(String[] args) {
		
		List<String> listFiles = Stream.of(new File(PATH_FILE+"\\finali").listFiles())
			      .filter(file -> !file.isDirectory())
			      .map(File::getName)
			      .collect(Collectors.toList());
		
		Collections.sort(listFiles); 
				
		listFiles.stream().forEach(f -> {
//			System.out.println(f);
			encode(f);
		});
		
		List<SpellItem> sortedList = listToEncode.stream().sorted(new Comparator<SpellItem>() {
			@Override
			public int compare(SpellItem a1, SpellItem a2) {
				return a1.getMatchValue().compareTo(a2.getMatchValue());
			}
		}).collect(Collectors.toList());
		
		try (CSVWriter writer = new CSVWriter(new FileWriter(PATH_FILE+"\\spell-settest5"+".txt"), ',')) {
    		writer.writeAll(getListOut(sortedList));
    	} catch (IOException e) {
			e.printStackTrace();
		}

}

	private static List<String[]> getListOut(List<SpellItem> sortedList) {
		List<String[]> listOut = new ArrayList<>();
		
		for (SpellItem s : sortedList) {
			List<String> arrayOut = new ArrayList<>();
			//arrayOut.add(s.getMatchValue()+":true:");
			arrayOut.add(s.getMatchValueWithProvince()+":true:");
			arrayOut.addAll(s.getCandidateValuesWithProvince());
			
			String[] candidateArray = new String[arrayOut.size()];
			candidateArray = arrayOut.toArray(candidateArray);
			
			listOut.add(candidateArray);
		}
		
		return listOut;
	}

	private static void encode(String f) {
		try {
			List<ItemCsv> listCsv = FileUtils.readCsv(PATH_FILE+"\\finali\\", f);
			//log.info("Previous:"+f+"-"+listCsv.size());
			
			listCsv.stream()
				.filter(x -> !x.getAddressCity().equals("ADDRESS_CITY"))
				.filter(x -> x.getAddressCity()!=null 
					&& x.getAddressCity().trim().length()>0 
					&& x.getAddressCityMatch()!=null 
					&& x.getAddressCityMatch().trim().length()>0)
				.forEach(ic -> insertToEncodeList(ic.getAddressCity(), ic.getProvince(), ic.getAddressCityMatch()));
			
//			List<SpellItem> sortedList = listToEncode.stream().sorted(new Comparator<SpellItem>() {
//
//				@Override
//				public int compare(SpellItem a1, SpellItem a2) {
////						int result=-1;
////						try {
////							result = a1.getAddressMatch().compareTo(a2.getAddressMatch());
////						} catch (Exception e) {
////							System.out.println("1-"+a1.getAddressMatch()+" 2-"+a2.getAddressMatch());
////						}
//					return a1.getMatchValue().compareTo(a2.getMatchValue());
//				}
//			}).collect(Collectors.toList());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void insertToEncodeList(String addressCity, String province, String addressCityMatch) {
		System.out.println("A:"+addressCity+" M:"+addressCityMatch+" P:"+province);
		SpellItem item = null;
		try {
			item = getItemFromEncodeList(province, addressCityMatch);
		} catch (NoSuchElementException e) {
			System.out.println("NEW");
			item = new SpellItem(addressCityMatch.toLowerCase(), province.toLowerCase());
			item.getCandidateValues().add(addressCity.toLowerCase());
			listToEncode.add(item);
			return;
		}
		
		if(!isAddressPresent(item, addressCity)) {
			System.out.println("NEW CANDIDATE");
			item.getCandidateValues().add(addressCity.toLowerCase());
		}else {
			System.out.println("NO");
		}
	}

	private static boolean isAddressPresent(SpellItem item, String addressCity) {
		
		for (String candidate : item.getCandidateValues()) {
			if(candidate.equals(addressCity.toLowerCase()))
				return true;
		}
		return false;
	}

	private static SpellItem getItemFromEncodeList(String province, String addressCityMatch) {

		Optional<SpellItem> itemOpt = listToEncode.stream()
													.filter(x -> x.getProvince().equals(province.toLowerCase()))
													.filter(x -> x.getMatchValue().equals(addressCityMatch.toLowerCase()))
													.findFirst();
		
		
		return itemOpt.get();
	}
	
}
