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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.htmlparser.sax.XmlSerializer;

import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import be.nabu.libs.scraper.Configuration.Step;

public class Scraper extends XMLRewriter {

	public Scraper(URL url) throws SAXException, IOException {
		this(url, null);
	}
	
	public Scraper(URL url, Proxy proxy) throws SAXException, IOException {
		super(toDocument(url, proxy));
	}
	
	public Scraper(InputStream input) throws SAXException, IOException, ParserConfigurationException {
		super(toDocument(html2xml(input)));
	}
	
	@Override
	protected void execute(Configuration configuration, Step step, Map<String, XMLRewriter> instances) {
		if (step.getAction() != null && step.getAction().equals("cleanup"))
			((Scraper) instances.get(step.getInstance())).cleanup();
		else
			super.execute(configuration, step, instances);
	}

	public void cleanup() {
		drop(findAll("/html/head|//script|//meta|//link|//img|//br|//@border|//@cellpadding|//@cellspacing|//@width|//@height|//@style|//style|//@valign|//@nowrap"));
		strip(findAll("//b|//u|//i|//font|//span|//p|//body|//strong"));
		normalize();
		drop(findAll("//td[not(node())]|//tr[not(node())]"));
	}
	
	public static Document toDocument(URL htmlUrl, Proxy proxy) throws SAXException, IOException {
		InputStream input = proxy == null ? htmlUrl.openStream() : htmlUrl.openConnection(proxy).getInputStream();
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			html2xml(input, output);
			return toDocument(new ByteArrayInputStream(output.toByteArray()));
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		finally {
			input.close();
		}
	}
	
	public static InputStream html2xml(InputStream input) throws IOException, SAXException {
		ByteArrayOutputStream output = new ByteArrayOutputStream(); 
		html2xml(input, output);
		return new ByteArrayInputStream(output.toByteArray());
	}
	
	public static void html2xml(InputStream input, OutputStream output) throws IOException, SAXException {
		ContentHandler serializer = new XmlSerializer(output);
		HtmlParser parser = new HtmlParser(XmlViolationPolicy.ALTER_INFOSET);
		parser.setContentHandler(serializer);
		parser.parse(new InputSource(input));
	}

}
