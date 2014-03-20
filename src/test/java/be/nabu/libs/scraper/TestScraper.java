package be.nabu.libs.scraper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

public class TestScraper extends TestCase {
	public void testImmoweb() throws IOException, SAXException, JAXBException {
		Scraper scraper = new Scraper(Thread.currentThread().getContextClassLoader().getResource("immoweb.html"));
		InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("immoweb.xml");
		String rewritten;
		try {
			rewritten = scraper.rewrite((Configuration) Configuration.createUnmarshaller().unmarshal(input)).toString();
		}
		finally {
			input.close();
		}
		assertEquals(getExpected("immoweb.expected.xml"), rewritten);
	}
	
	public static String getExpected(String name) throws IOException {
		InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			int read = 0;
			byte [] buffer = new byte[4096];
			while ((read = input.read(buffer)) != -1)
				output.write(buffer, 0, read);
			return new String(output.toByteArray(), "UTF-8");
		}
		finally {
			input.close();
		}
	}
}
