package be.nabu.libs.scraper;

import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="instance")
public class Configuration {
	
	private List<Step> steps;
	private String id, result;
	
	@XmlElement(name="step")
	public List<Step> getSteps() {
		return steps;
	}
	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	@XmlAttribute
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@XmlAttribute
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}


	public static class Step {
		private String target, action, description;
		private String value;
		private List<Configuration> instances;
		private String instance;
		
		@XmlAttribute
		public String getTarget() {
			return target;
		}
		public void setTarget(String target) {
			this.target = target;
		}
		@XmlAttribute
		public String getAction() {
			return action;
		}
		public void setAction(String action) {
			this.action = action;
		}
		@XmlElement(name="instance")
		public List<Configuration> getInstances() {
			return instances;
		}
		public void setInstances(List<Configuration> instances) {
			this.instances = instances;
		}
		@XmlAttribute
		public String getInstance() {
			return instance;
		}
		public void setInstance(String instance) {
			this.instance = instance;
		}
		@XmlAttribute
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		@XmlAttribute
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		
	}

	/**
	 * Sets an id on the configuration if none is present and the config is not at the root
	 * Also sets an id on the step if none is given
	 */
	public static Unmarshaller createUnmarshaller() {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setListener(new Listener() {
				public void afterUnmarshal(Object target, Object parent) {
					if (target instanceof Configuration && parent instanceof Step) {
						Configuration configuration = (Configuration) target;
						if (configuration.getId() == null)
							configuration.setId(UUID.randomUUID().toString());
					}
					else if (target instanceof Step && parent instanceof Configuration) {
						Step step = (Step) target;
						Configuration configuration = (Configuration) parent;
						if (step.getInstance() == null) {
							if (configuration.getId() == null)
								configuration.setId(UUID.randomUUID().toString());
							step.setInstance(configuration.getId());
						}
					}
				}
			});
			return unmarshaller;
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
}
