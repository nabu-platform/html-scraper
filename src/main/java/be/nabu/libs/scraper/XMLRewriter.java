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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.nabu.libs.scraper.Configuration.Step;

public class XMLRewriter {
			
	private Document document;
	
	public XMLRewriter(Document document) {
		this.document = document;
	}
	
	public XMLRewriter rewrite(Configuration configuration) {
		Map<String, XMLRewriter> instances = new HashMap<String, XMLRewriter>();
		instances.put(configuration.getId(), this);
		rewrite(configuration, instances);
		return configuration.getResult() != null ? instances.get(configuration.getResult()) : this;
	}
	
	final protected void rewrite(Configuration configuration, Map<String, XMLRewriter> instances) {
		for (Step step : configuration.getSteps())
			execute(configuration, step, instances);
	}
	
	protected void execute(Configuration configuration, Step step, Map<String, XMLRewriter> instances) {
		if (step.getAction() == null);
			// do nothing, empty steps are usually used as containers for instances
			// note that the only reason a subinstance is in a step is to be able to determine the order in which it must be processed
		else if (step.getAction().equals("drop"))
			instances.get(step.getInstance()).drop(instances.get(step.getInstance()).findAll(step.getTarget()));
		else if (step.getAction().equals("strip"))
			instances.get(step.getInstance()).strip(instances.get(step.getInstance()).findAll(step.getTarget()));
		else if (step.getAction().equals("rename"))
			instances.get(step.getInstance()).rename(instances.get(step.getInstance()).findAll(step.getTarget()), step.getValue());
		else if (step.getAction().equals("wrap"))
			instances.get(step.getInstance()).wrap(instances.get(step.getInstance()).findAll(step.getTarget()), step.getValue());
		else if (step.getAction().equals("substring"))
			instances.get(step.getInstance()).substring(instances.get(step.getInstance()).findAll(step.getTarget()), step.getValue());
		else if (step.getAction().equals("trim"))
			instances.get(step.getInstance()).trim();
		else if (step.getAction().equals("normalize"))
			instances.get(step.getInstance()).normalize();
		else if (step.getAction().equals("append"))
			instances.get(step.getInstance()).append(instances.get(step.getFrom() == null ? configuration.getId() : step.getFrom()).find(step.getTarget()), instances.get(step.getInstance()).findAll(step.getValue()));
		else if (step.getAction().equals("select"))
			instances.get(step.getInstance()).select(instances.get(configuration.getId()).find(step.getTarget()));
		else if (step.getAction().equals("extract"))
			instances.get(step.getInstance()).extract(instances.get(configuration.getId()).findAll(step.getTarget()));
		else if (step.getAction().equals("dropText"))
			instances.get(step.getInstance()).dropText(instances.get(configuration.getId()).findAll(step.getTarget()));
		else if (step.getAction().equals("after")) {
			if (step.getValue() != null)
				instances.get(step.getInstance()).after(instances.get(configuration.getId()).find(step.getTarget()), instances.get(step.getInstance()).findAll(step.getValue()));
			else
				instances.get(step.getInstance()).after(instances.get(step.getInstance()).find(step.getTarget()));
		}
		else if (step.getAction().equals("copy"))
			instances.get(step.getInstance()).copy(instances.get(step.getInstance()).findAll(step.getTarget()), step.getValue());
		else if (step.getAction().equals("before")) {
			if (step.getValue() != null)
				instances.get(step.getInstance()).before(instances.get(configuration.getId()).find(step.getTarget()), instances.get(step.getInstance()).findAll(step.getValue()));
			else
				instances.get(step.getInstance()).before(instances.get(step.getInstance()).find(step.getTarget()));
		}
		if (step.getInstances() != null) {
			for (Configuration child : step.getInstances()) {
				instances.put(child.getId(), instances.get(configuration.getId()).clone());
				rewrite(child, instances);
			}
		}
	}
	
	@Override
	public XMLRewriter clone() {
		Document cloned = newDocument();
		cloned.appendChild(cloned.importNode(document.getDocumentElement(), true));
		return new XMLRewriter(cloned);
	}
	
	public Node find(String query) {
		try {
			NodeList result = ((NodeList) getExpression(query).evaluate(document, XPathConstants.NODESET));
			return result.getLength() == 0 ? null : result.item(0);
		}
		catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public NodeList findAll(String query) {
		try {
			return ((NodeList) getExpression(query).evaluate(document, XPathConstants.NODESET));
		}
		catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String get(String query) {
		try {
			return ((String) getExpression(query).evaluate(document, XPathConstants.STRING));
		}
		catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Narrow the scope to a specific element
	 * @param node
	 * @return
	 */
	public XMLRewriter select(Node node) {
		if (node != null) {
			document.removeChild(document.getDocumentElement());
			document.appendChild(document.importNode(node, true));
		}
		else
			emptyDocument();
		return this;
	}
	
	/**
	 * In some cases the document has to be emptied out because you scoped to something that does not exist
	 */
	private void emptyDocument() {
		document = newDocument();
	}
	
	/**
	 * Select the bit after the given node (only the data on the same level)
	 */
	public XMLRewriter after(Node node) {
		if (node != null) {
			Document original = document;
			document = newDocument();
			Element element = document.createElement(original.getDocumentElement().getNodeName());
			document.appendChild(element);
			while (node.getNextSibling() != null) {
				node = node.getNextSibling();
				element.appendChild(document.importNode(node, true));
			}
		}
		else
			emptyDocument();
		return this;
	}
	
	public XMLRewriter after(Node insertAfter, NodeList newNodes) {
		if (insertAfter.getNextSibling() != null)
			before(insertAfter.getNextSibling(), newNodes);
		else {
			for (int i = 0; i < newNodes.getLength(); i++)
				insertAfter.getParentNode().appendChild(insertAfter.getOwnerDocument().importNode(newNodes.item(i), true));
		}
		return this;
	}
	/**
	 * Insert the new node after the given node
	 */
	public XMLRewriter after(Node insertAfter, Node newNode) {
		if (insertAfter.getNextSibling() != null)
			insertAfter.getParentNode().insertBefore(insertAfter.getOwnerDocument().importNode(newNode, true), insertAfter.getNextSibling());
		else
			insertAfter.getParentNode().appendChild(insertAfter.getOwnerDocument().importNode(newNode, true));
		return this;
	}

	/**
	 * Append the new nodes to the given node
	 */
	public XMLRewriter append(Node toNode, NodeList nodes) {
		for (int i = 0; i < nodes.getLength(); i++)
			append(toNode, nodes.item(i));
		return this;
	}
	
	public XMLRewriter append(Node toNode, Node node) {
		toNode.appendChild(toNode.getOwnerDocument().importNode(node, true));
		return this;
	}

	public XMLRewriter before(Node insertBefore, NodeList newNodes) {
		for (int i = 0; i < newNodes.getLength(); i++)
			before(insertBefore, newNodes.item(i));
		return this;
	}
	
	public XMLRewriter before(Node insertBefore, Node newNode) {
		insertBefore.getParentNode().insertBefore(insertBefore.getOwnerDocument().importNode(newNode, true), insertBefore);
		return this;
	}
	
	public XMLRewriter before(Node node) {
		if (node != null) {
			Document original = document;
			document = newDocument();
			Element element = document.createElement(original.getDocumentElement().getNodeName());
			document.appendChild(element);
			while (node.getPreviousSibling() != null) {
				node = node.getPreviousSibling();
				element.appendChild(document.importNode(node, true));
			}
		}
		else
			emptyDocument();
		return this;
	}
	
	public XMLRewriter strip(Node node) {
		Node target = node;
		for (int j = node.getChildNodes().getLength() - 1; j >= 0; j--)
			target = node.getParentNode().insertBefore(node.removeChild(node.getChildNodes().item(j)), target);
		node.getParentNode().removeChild(node);
		return this;
	}
	public XMLRewriter strip(NodeList list) {
		for (int i = list.getLength() - 1; i >= 0; i--)
			strip(list.item(i));
		return this;
	}

	public XMLRewriter wrap(NodeList list, String name) {
		for (int i = list.getLength() - 1; i >= 0; i--)
			wrap(list.item(i), name);
		return this;
	}
	public XMLRewriter wrap(Node node, String name) {
		Node parent = node.getParentNode();
		Element element = node.getOwnerDocument().createElement(name);
		parent.insertBefore(element, node);
		parent.removeChild(node);
		element.appendChild(node);
		return this;
	}
	
	public XMLRewriter drop(NodeList list) {
		for (int i = list.getLength() - 1; i >= 0; i--) {
			if (list.item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
				Attr attribute = (Attr) list.item(i);
				attribute.getOwnerElement().getAttributes().removeNamedItem(attribute.getName());
			}
			else if (list.item(i).getParentNode() != null)
				list.item(i).getParentNode().removeChild(list.item(i));
		}
		return this;
	}
	
	public XMLRewriter trim() {
		trim(document.getDocumentElement());
		return this;
	}
	
	public XMLRewriter normalize() {
		normalize(document.getDocumentElement());
		return this;
	}
	
	public XMLRewriter extract(NodeList list) {
		for (int i = list.getLength() - 1; i >= 0; i--)
			extract(list.item(i));
		return this;
	}
	public XMLRewriter extract(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Node parent = node.getParentNode();
			Node target = parent.getNextSibling();
			if (target == null)
				parent.getParentNode().appendChild(node);
			else
				parent.getParentNode().insertBefore(node, target);
		}
		else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			Attr attribute = (Attr) node;
			Node parent = attribute.getOwnerElement();
			Node target = parent.getNextSibling();
			Element newElement = node.getOwnerDocument().createElement(attribute.getName());
			newElement.setTextContent(attribute.getValue());
			if (target == null)
				parent.getParentNode().appendChild(newElement);
			else
				parent.getParentNode().insertBefore(newElement, target);
			attribute.getOwnerElement().removeAttribute(attribute.getName());
		}
		else if (node.getNodeType() == Node.TEXT_NODE) {
			Element newElement = node.getOwnerDocument().createElement("text");
			newElement.setTextContent(node.getTextContent());
			Node parent = node.getParentNode();
			parent.insertBefore(newElement, node);
			parent.removeChild(node);
		}
		return this;
	}
	
	private void trim(Node node) {
		for (int i = node.getChildNodes().getLength() - 1; i >= 0; i--) {
			Node child = node.getChildNodes().item(i);
			if (node.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE) {
				if (child.getTextContent().trim().isEmpty())
					node.removeChild(child);
				else
					child.setTextContent(child.getTextContent().trim());
			}
			else
				trim(child);
		}
	}
	
	private void normalize(Node node) {
		for (int i = node.getChildNodes().getLength() - 1; i >= 0; i--) {
			Node child = node.getChildNodes().item(i);
			normalize(child);
			String normalizedContent = child.getTextContent().trim().replaceAll("[\\s]{2,}", " ");
			if (child.getNodeType() == Node.TEXT_NODE && normalizedContent.isEmpty())
				node.removeChild(child);
			else if (isText(child))
				child.setTextContent(normalizedContent);
		}
	}
	public XMLRewriter substring(NodeList nodes, String newName) {
		for (int i = 0; i < nodes.getLength(); i++)
			substring(nodes.item(i), newName);
		return this;
	}
	public XMLRewriter substring(Node node, String regex) {
		if (isText(node)) {
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(node.getTextContent());
			if (matcher.find())
				node.setTextContent(matcher.group());
		}
		return this;
	}
	
	public XMLRewriter dropText(NodeList nodes) {
		for (int i = 0; i < nodes.getLength(); i++)
			dropText(nodes.item(i));
		return this;
	}
	public XMLRewriter dropText(Node node) {
		if (!isText(node)) {
			for (int i = node.getChildNodes().getLength() - 1; i >= 0; i--) {
				if (node.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE) {
					node.removeChild(node.getChildNodes().item(i));
				}
			}
		}
		return this;
	}
	
	private boolean isText(Node node) {
		if (node.getNodeType() == Node.TEXT_NODE)
			return true;
		else if (node.getNodeType() == Node.ELEMENT_NODE) {
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (node.getChildNodes().item(i).getNodeType() != Node.TEXT_NODE)
					return false;
			}
			return true;
		}
		else
			return false;
	}
	
	public XMLRewriter rename(NodeList nodes, String newName) {
		for (int i = 0; i < nodes.getLength(); i++)
			rename(nodes.item(i), newName);
		return this;
	}
	
	public XMLRewriter copy(NodeList nodes, String newName) {
		for (int i = 0; i < nodes.getLength(); i++)
			copy(nodes.item(i), newName);
		return this;
	}
	
	public XMLRewriter copy(Node node, String newName) {
		return copy(node, newName, false);
	}
	
	public XMLRewriter rename(Node node, String newName) {
		return copy(node, newName, true);
	}
	
	private XMLRewriter copy(Node node, String newName, boolean replace) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = node.getOwnerDocument().createElement(newName);
			NamedNodeMap attributes = ((Element) node).getAttributes();
			for (int i = 0; i < attributes.getLength(); i++)
				element.setAttribute(((Attr) attributes.item(i)).getName(), ((Attr) attributes.item(i)).getValue());
			Node reference = null;
			for (int i = node.getChildNodes().getLength() - 1; i >= 0; i--) {
				Node childNode = replace ? node.getChildNodes().item(i) : node.getOwnerDocument().importNode(node.getChildNodes().item(i), true);
				if (reference == null)
					reference = element.appendChild(childNode);
				else
					reference = element.insertBefore(childNode, reference);
			}
			if (node.equals(node.getOwnerDocument().getDocumentElement())) {
				if (!replace)
					throw new RuntimeException("Can not copy the root element");
				Document document = node.getOwnerDocument();
				document.removeChild(document.getDocumentElement());
				document.appendChild(element);
			}
			else {
				if (replace)
					node.getParentNode().replaceChild(element, node);
				else {
					if (node.getNextSibling() != null)
						node.getParentNode().insertBefore(element, node.getNextSibling());
					else
						node.getParentNode().appendChild(element);
				}
			}
		}
		else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			Attr attribute = (Attr) node;
			attribute.getOwnerElement().setAttribute(newName, attribute.getValue());
			if (replace)
				attribute.getOwnerElement().removeAttribute(attribute.getName());
		}
		return this;
	}
	
	@Override
	public String toString() {
		try {
			return document.getDocumentElement() == null ? null : toString(document.getDocumentElement());
		}
		catch (TransformerException e) {
			return super.toString();
		}
	}
	
	public static Document toDocument(InputStream xml) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		return factory.newDocumentBuilder().parse(xml);
	}
	
	public static XPathExpression getExpression(String query) throws XPathExpressionException {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		return xpath.compile(query);
	}
	
	public static String toString(Node node) throws TransformerException {
		StringWriter string = new StringWriter();
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(new DOMSource(node), new StreamResult(string));
		return string.toString();
	}
	
	public static Document newDocument() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		try {
			return factory.newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
}
