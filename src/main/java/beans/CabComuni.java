package beans;

import java.sql.Date;

import org.apache.commons.lang3.StringUtils;

public class CabComuni {

	private Long comuneId;
	private String cab;
	private Date dataIniValidita;
	private Date dataFineValidita;
	private Long provinciaId;
	private String comune;
	private String codCatastale;
	
	public Long getComuneId() {
		return comuneId;
	}
	public void setComuneId(Long comuneId) {
		this.comuneId = comuneId;
	}
	public String getCab() {
		return StringUtils.leftPad(cab, 6, "0");
	}
	public void setCab(String cab) {
		this.cab = cab;
	}
	public Date getDataIniValidita() {
		return dataIniValidita;
	}
	public void setDataIniValidita(Date dataIniValidita) {
		this.dataIniValidita = dataIniValidita;
	}
	public Date getDataFineValidita() {
		return dataFineValidita;
	}
	public void setDataFineValidita(Date dataFineValidita) {
		this.dataFineValidita = dataFineValidita;
	}
	public Long getProvinciaId() {
		return provinciaId;
	}
	public void setProvinciaId(Long provinciaId) {
		this.provinciaId = provinciaId;
	}
	public String getComune() {
		return comune;
	}
	public void setComune(String comune) {
		this.comune = comune;
	}
	public String getCodCatastale() {
		return codCatastale;
	}
	public void setCodCatastale(String codCatastale) {
		this.codCatastale = codCatastale;
	}
	
}
