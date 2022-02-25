package ml.symspell.strategy;

public abstract class AbstractFactoryStrategy {
	
	abstract AddressSearchStategy getStrategy(int type);
}
