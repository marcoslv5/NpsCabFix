package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import beans.ItemCsv;
import finder.CabFinder;

public class ConvertAddressIA {

	public static List<AddressToEncode> listToEncode = new ArrayList<>();
	public static List<AddressEncoded> listEncoded = new ArrayList<>();
	
	public final static String vocabulary = "<>ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789&()+/.,:;'?!-_@[]Êìèéòçàù";
	public final static String vocabulary2 = "<>ABCDEFGHIJKLMNOPQRSTUVWXYZ.-";
	private static final String PATH_FILE = "C:\\Nps\\Doc\\csv\\";
	
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
		
		try (CSVWriter writer = new CSVWriter(new FileWriter(PATH_FILE+"\\addressEncoded"+".csv"), ',')) {
    		writer.writeAll(getListOut(listEncoded));
    	} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void encode(String f) {
		try {
			List<ItemCsv> listCsv = FileUtils.readCsv(PATH_FILE+"\\finali\\", f);
			//log.info("Previous:"+f+"-"+listCsv.size());
			
			ConvertAddressIA caia = new ConvertAddressIA();
			
			listCsv.stream()
				.filter(x -> !x.getAddressCity().equals("ADDRESS_CITY"))
				.filter(x -> x.getAddressCity()!=null 
					&& x.getAddressCity().trim().length()>0 
					&& x.getAddressCityMatch()!=null 
					&& x.getAddressCityMatch().trim().length()>0)
				.forEach(ic -> listToEncode.add(caia.new AddressToEncode(ic.getAddressCity(), ic.getProvince(), ic.getAddressCityMatch())));
			
			List<AddressToEncode> sortedList = listToEncode.stream().sorted(new Comparator<AddressToEncode>() {

				@Override
				public int compare(AddressToEncode a1, AddressToEncode a2) {
//						int result=-1;
//						try {
//							result = a1.getAddressMatch().compareTo(a2.getAddressMatch());
//						} catch (Exception e) {
//							System.out.println("1-"+a1.getAddressMatch()+" 2-"+a2.getAddressMatch());
//						}
					return a1.getAddressMatch().compareTo(a2.getAddressMatch());
				}
			}).collect(Collectors.toList());
			
			sortedList.forEach(x -> listEncoded.add(caia.new AddressEncoded(encodeValue(x.getProvince() + x.getAddress()), encodeValue(x.getAddressMatch()))));
			
			//sortedList.forEach(x -> System.out.println(x.getAddress()));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static List<String[]> getListOut(List<AddressEncoded> listEncoded) {
		List<String[]> listOut = new ArrayList<>();
		
		for (AddressEncoded ae : listEncoded) {
			listOut.add(new String[] {ae.getActualValue(), ae.getAugmentedValue()});
		}
		
		return listOut;
	}

	private static String encodeValue(String value) {
		String valueEncoded="0";
		for (int i = 0; i < value.length(); i++) {
			valueEncoded+=" "+encodeChar(value.charAt(i));
		}
		return valueEncoded+" 1";
	}


	
	private static int encodeChar(char charIn) {
		for (int j = 0; j < vocabulary.length(); j++) {
			if(charIn==vocabulary.charAt(j))
				return j;
		}
		System.out.println(charIn);
		return -1;
	}


//	private static Object putAddressEncoded(ItemCsv item) {
//		if(item.getAddressCity()!=null && item.getAddressCity().trim().length()>1) {
//			cachePrevious.put(item.getAddressCity(), item);
//		}
//		return null;
//	}
	
	public class AddressToEncode {
		
		private String address;
		private String province;
		private String addressMatch;
		
		
		public AddressToEncode(String address, String province, String addressMatch) {
			super();
			this.address = address;
			this.province = province;
			this.addressMatch = addressMatch;
		}
		
		public String getAddress() {
			return address;
		}
		public void setAddress(String address) {
			this.address = address;
		}
		public String getProvince() {
			return province;
		}
		public void setProvince(String province) {
			this.province = province;
		}
		public String getAddressMatch() {
			return addressMatch;
		}
		public void setAddressMatch(String addressMatch) {
			this.addressMatch = addressMatch;
		}
		
	}
	
	public class AddressEncoded {
		String actualValue;
		String augmentedValue;
		
		public AddressEncoded(String actualValue, String augmentedValue) {
			super();
			this.actualValue = actualValue;
			this.augmentedValue = augmentedValue;
		}
		
		public String getActualValue() {
			return actualValue;
		}
		public void setActualValue(String actualValue) {
			this.actualValue = actualValue;
		}
		public String getAugmentedValue() {
			return augmentedValue;
		}
		public void setAugmentedValue(String augmentedValue) {
			this.augmentedValue = augmentedValue;
		}
		
	}

	public static List<AddressEncoded> readCsv(String path, String file) throws FileNotFoundException, IOException{
		List<AddressEncoded> lista=new ArrayList<>();
		ConvertAddressIA caia = new ConvertAddressIA();
		
		String fileName = path+file;
		
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> r = reader.readAll();
            r.forEach(x -> lista.add(caia.new AddressEncoded("", "") ));
        }
        
		return lista;
	}
}

