package beans;

public class CabComuniMatch extends CabComuni {

	CabComuni comuneBestMatch;
	CabComuni comunePartialMatch;
	
	int indexBestMatch;
	int indexPartialMatch;
	
	String provincia;
	
	public CabComuniMatch() {
	}
	
	public CabComuniMatch(CabComuni cabComuni) {
		if(cabComuni!=null) {
			this.setCab(cabComuni.getCab());
			this.setCodCatastale(cabComuni.getCodCatastale());
			this.setComune(cabComuni.getComune());
			this.setComuneId(cabComuni.getComuneId());
			this.setDataFineValidita(cabComuni.getDataFineValidita());
			this.setDataIniValidita(cabComuni.getDataIniValidita());
			this.setProvinciaId(cabComuni.getProvinciaId());
		}
		this.indexBestMatch = -1;
	}
	
	public CabComuniMatch(CabComuni cabComuni, int indexBestMatch) {
		if(cabComuni!=null) {
			this.setCab(cabComuni.getCab());
			this.setCodCatastale(cabComuni.getCodCatastale());
			this.setComune(cabComuni.getComune());
			this.setComuneId(cabComuni.getComuneId());
			this.setDataFineValidita(cabComuni.getDataFineValidita());
			this.setDataIniValidita(cabComuni.getDataIniValidita());
			this.setProvinciaId(cabComuni.getProvinciaId());
		}
		this.indexBestMatch = indexBestMatch;
	}

	public CabComuni getComuneBestMatch() {
		return comuneBestMatch;
	}
	public void setComuneBestMatch(CabComuni comuneBestMatch) {
		this.comuneBestMatch = comuneBestMatch;
	}
	public CabComuni getComunePartialMatch() {
		return comunePartialMatch;
	}
	public void setComunePartialMatch(CabComuni comunePartialMatch) {
		this.comunePartialMatch = comunePartialMatch;
	}

	public int getIndexBestMatch() {
		return indexBestMatch;
	}

	public void setIndexBestMatch(int indexBestMatch) {
		this.indexBestMatch = indexBestMatch;
	}

	public String getProvincia() {
		return provincia;
	}

	public void setProvincia(String provincia) {
		this.provincia = provincia;
	}

	public int getIndexPartialMatch() {
		return indexPartialMatch;
	}

	public void setIndexPartialMatch(int indexPartialMatch) {
		this.indexPartialMatch = indexPartialMatch;
	}
	
}
