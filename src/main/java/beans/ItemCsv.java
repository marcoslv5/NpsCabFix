package beans;

import org.apache.commons.lang3.StringUtils;

public class ItemCsv implements Cloneable{

	private String subjectCode;
	private String addressCity;
	private String province;
	private String cap;
	private String cab;
	
	private String addressCityMatch;
	private String addressCityBest;
	private String addressCityBestPrevious;
	private String cabBest;
	private String addressCityPartial;
	private String cabPartial;
	private String indexBest;
	private String indexPartial;
	private int indexBestNum;
	
	public ItemCsv(String[] csv) {
		
		//System.out.println(csv[0]);
		String[] splitted = csv[0].split(";");
		
		this.subjectCode = splitted[0];
		this.addressCity = splitted[1];
		if(splitted.length>2)
			this.province = splitted[2];
		if(splitted.length>3)
			this.cap = splitted[3];
		if(splitted.length>4)
			this.cab = splitted[4];
		
		if(splitted.length>5)
			this.addressCityMatch = splitted[5];
		if(splitted.length>6)
			this.addressCityBest = splitted[6];
		if(splitted.length>7)
			this.cabBest = splitted[7];
		if(splitted.length>8)
			this.indexBest = splitted[8];
		if(splitted.length>9)
			this.addressCityMatch = splitted[9];
		if(splitted.length>10)
			this.cabPartial = splitted[10];
		if(splitted.length>11)
			this.indexPartial = splitted[11];
		
	}
	
	public ItemCsv(String[] csv, boolean ready) {
		
		//System.out.println(csv[0]);
		String[] splitted = csv[0].split(";");
		
		this.subjectCode = splitted[0];
		this.addressCity = splitted[1];
		this.province = splitted[2];
		this.cap = splitted[3];
		this.cab = splitted[4];
		
	}
	
	public ItemCsv(String subjectCode, String addressCity, String province, String cap, String cab) {
		this.subjectCode = subjectCode;
		this.addressCity = addressCity;
		this.province = province;
		this.cap = cap;
		this.cab = cab;
	}
	
	public String getSubjectCode() {
		return subjectCode;
	}
	public void setSubjectCode(String subjectCode) {
		this.subjectCode = subjectCode;
	}
	public String getAddressCity() {
		return addressCity;
	}
	public void setAddressCity(String addressCity) {
		this.addressCity = addressCity;
	}
	public String getProvince() {
		if(cap==null)
			return "xxx";
		return province;
	}
	public void setProvince(String province) {
		this.province = province;
	}
	public String getCap() {
		if(cap==null)
			return "###";
		return cap;
	}
	public void setCap(String cap) {
		this.cap = cap;
	}
	public String getCab() {
		if(cab==null)
			return "***";
		return StringUtils.leftPad(cab, 6, "0");
	}
	public void setCab(String cab) {
		this.cab = cab;
	}
	
	public String getAddressCityMatch() {
		return addressCityMatch;
	}

	public void setAddressCityMatch(String addressCityMatch) {
		this.addressCityMatch = addressCityMatch;
	}

	public String getAddressCityBest() {
		return addressCityBest;
	}

	public void setAddressCityBest(String addressCityBest) {
		this.addressCityBest = addressCityBest;
	}

	public String getCabBest() {
		return StringUtils.leftPad(cabBest, 6, "0");
	}

	public void setCabBest(String cabBest) {
		this.cabBest = cabBest;
	}

	public String getAddressCityPartial() {
		return addressCityPartial;
	}

	public void setAddressCityPartial(String addressCityPartial) {
		this.addressCityPartial = addressCityPartial;
	}

	public String getCabPartial() {
		return StringUtils.leftPad(cabPartial, 6, "0");
	}

	public void setCabPartial(String cabPartial) {
		this.cabPartial = cabPartial;
	}

	public String getIndexBest() {
		return indexBest;
	}

	public void setIndexBest(String indexBest) {
		this.indexBest = indexBest;
	}

	public String getIndexPartial() {
		return indexPartial;
	}

	public void setIndexPartial(String indexPartial) {
		this.indexPartial = indexPartial;
	}

	public int getIndexBestNum() {
		return indexBestNum;
	}
	
	public void setIndexBestNum(int indexBestNum) {
		this.indexBestNum = indexBestNum;
	}
	
	public String getAddressCityBestPrevious() {
		return addressCityBestPrevious;
	}

	public void setAddressCityBestPrevious(String addressCityBestPrevious) {
		this.addressCityBestPrevious = addressCityBestPrevious;
	}

	@Override
	public Object clone() {
	    try {
	        return (ItemCsv) super.clone();
	    } catch (CloneNotSupportedException e) {
	        return new ItemCsv(this.subjectCode, this.addressCity, this.province, this.cap, this.cab);
	    }
	}

	
}
