package utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public class DecompressUtil {

	public static boolean decompressBase64(String compressed, OutputStream os) throws IOException {
		//System.out.println(">>>"+compressed+"<<<");
		
		try(GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(compressed)))) {
			copy(zis,os);
		}
		return true;
		
	}

	public static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[4098];
		for (int read = is.read(buffer); read >= 0; read = is.read(buffer)) os.write(buffer, 0, read);
	}
	
	public static boolean isCompressed(String inputValue) {
		
		if(inputValue == null)
			return false;
		
		inputValue = inputValue.trim();
		return !inputValue.startsWith("{");
	}	
}
