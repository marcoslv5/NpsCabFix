package ml.symspell.strategy;

public class SettingStrategy {

	private int symSpellDistanceMax = -1;
	private int nRemoveWord=1;
	
	public SettingStrategy() {
	}
	
	public SettingStrategy(int symSpellDistanceMax) {
		this.symSpellDistanceMax=symSpellDistanceMax;
	}
	
	public SettingStrategy(int symSpellDistanceMax, int nRemovedWord) {
		this.symSpellDistanceMax=symSpellDistanceMax;
		this.nRemoveWord=nRemovedWord;
	}
	
	public int getSymSpellDistanceMax() {
		return symSpellDistanceMax;
	}
	public void setSymSpellDistanceMax(int symSpellDistanceMax) {
		this.symSpellDistanceMax = symSpellDistanceMax;
	}
	public int getnRemoveWord() {
		return nRemoveWord;
	}
	public void setnRemoveWord(int nRemoveWord) {
		this.nRemoveWord = nRemoveWord;
	}
	
	
}
