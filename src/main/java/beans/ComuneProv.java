package beans;

public class ComuneProv {

	private String comune;
	private String provincia;
	
	public ComuneProv(String comune, String provincia) {
		this.comune = comune;
		this.provincia = provincia;
	}
	public String getComune() {
		return comune;
	}
	public void setComune(String comune) {
		this.comune = comune;
	}
	public String getProvincia() {
		return provincia;
	}
	public void setProvincia(String provincia) {
		this.provincia = provincia;
	}
	@Override
	public String toString() {
		return comune + "#" + provincia;
	}
	
}
