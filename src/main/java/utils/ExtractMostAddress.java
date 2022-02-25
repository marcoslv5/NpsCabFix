package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beans.ItemCsv;

public class ExtractMostAddress {

	public static void main(String[] args) {

		List<ItemCsv> listAllCsv = FileUtils.getFinaliFiles();
		
		Map<String, Integer> addressFrequency = new HashMap<>();
		
		String addressMost = "";
		int frequencyMost = 0;
		
		for (ItemCsv item : listAllCsv) {
			try {
				String address = item.getAddressCityMatch();
				Integer frequency = addressFrequency.get(address);
				
				if(address!=null && !address.equals("ADDRESS_MATCH")) {
					//frequency= frequency==null ? 1 : frequency++;
					if(frequency==null) {
						frequency=1; 
					}else {
						frequency++; 
					}
					
					addressFrequency.put(address, frequency);
					
					if(frequency>frequencyMost) {
						addressMost = address;
						frequencyMost = frequency;
					}
				}
			} catch (Exception e) {
				System.out.println(item.toString());
			}
		}
		
		System.out.println("Most: "+addressMost+" - "+frequencyMost);
		
		Map<String, String> actualValues = new HashMap<>();
		
		for (ItemCsv item : listAllCsv) {
			String addressMatch = item.getAddressCityMatch();
			String address = item.getAddressCity();
			
			if(addressMatch!=null && !address.equals("ADDRESS_MATCH") 
					&& addressMatch.equals(addressMost) && !actualValues.containsKey(address)) {
				actualValues.put(address, addressMatch);
				System.out.println(">"+address+"<");
			}
		}

	}

}
