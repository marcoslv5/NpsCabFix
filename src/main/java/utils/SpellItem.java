package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpellItem {

	private String matchValue=null; 
	private String province=null; 
	private List<String> candidateValues=new ArrayList<>();
	private final String SEPARATOR="-";
	
	public SpellItem() {
	}
	
	public SpellItem(String matchValue) {
		this.matchValue = matchValue;
	}
	
	public SpellItem(String matchValue, String province) {
		this.matchValue = matchValue;
		this.province = province;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getMatchValue() {
		return matchValue;
	}
	public String getMatchValueWithProvince() {
		return province+SEPARATOR+matchValue;
	}
	
	public void setMatchValue(String matchValue) {
		this.matchValue = matchValue;
	}
	public List<String> getCandidateValues() {
		return candidateValues;
	}
	public List<String> getCandidateValuesWithProvince() {
		return candidateValues.stream()
				.map(x -> province+SEPARATOR+x).collect(Collectors.toList());
	}
	
}
