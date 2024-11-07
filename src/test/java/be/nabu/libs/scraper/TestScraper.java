/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
		// with newer versions it seems pretty print is enabled by default, changing the result
		//assertEquals(getExpected("immoweb.expected.xml"), rewritten);
		assertEquals(getExpected("immoweb.expected.xml").replaceAll("[\\s]+", ""), rewritten.replaceAll("[\\s]+", ""));
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
