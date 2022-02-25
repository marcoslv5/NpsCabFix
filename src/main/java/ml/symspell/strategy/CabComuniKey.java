package ml.symspell.strategy;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;

import beans.CabComuni;

public class CabComuniKey extends CabComuni  {
	
	private String key;

	public CabComuniKey(String key, CabComuni cabComuni) throws IllegalAccessException, InvocationTargetException {
		BeanUtils.copyProperties(this, cabComuni);
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	

}
