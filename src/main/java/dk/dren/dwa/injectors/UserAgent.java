package dk.dren.dwa.injectors;

import lombok.Getter;

/**
 * This is just an example class, don't use it, there are many better user agent parsers.
 */
public class UserAgent implements AutoCloseable {

	@Getter
	String header;
	
	@Getter
	private boolean sucky;
	
	public UserAgent(String header) {
		this.header = header;	
		this.sucky = header!=null ? header.contains("MSIE") : false;
	}

	@Override
	public void close() throws Exception {
		
	}
}
