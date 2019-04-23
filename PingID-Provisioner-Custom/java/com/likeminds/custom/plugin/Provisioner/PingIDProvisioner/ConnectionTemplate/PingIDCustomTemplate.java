package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.ConnectionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlException;
import org.sourceid.saml20.adapter.conf.SimpleFieldList;
import org.sourceid.saml20.adapter.gui.FieldDescriptor;
import org.sourceid.saml20.domain.ConnectionBase;
import org.sourceid.saml20.domain.SpConnection;
import org.sourceid.saml20.domain.util.api.ConnectionDeserializer;
import org.sourceid.util.license.LicenseManager;

import com.pingidentity.common.util.Closer;
import com.pingidentity.common.util.xml.InvalidXmlException;
import com.pingidentity.connection.template.pingidforworkforce.PingIDForWorkforceTemplate;
import com.pingidentity.module.connection.ConnectionModuleConfiguration;
import com.pingidentity.provisioner.domain.mgmt.ProvisionerConfig;
import com.pingidentity.provisioner.domain.mgmt.ProvisionerManager;
import com.pingidentity.templates.connection.ConnectionTemplate;

public class PingIDCustomTemplate implements ConnectionTemplate {
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(PingIDForWorkforceTemplate.class);
	public static final String TEMPLATE_ID = "PingIDCustomProvisioner";
	public static final String TEMPLATE_DESCRIPTION = "PingID Custom Connector";
	private static final String PING_ID_CONNECTION = "pingid-custom-connection.xml";

	public List<FieldDescriptor> getFieldDescriptors() {
		return null;
	}

	public String getId() {
		return TEMPLATE_ID;
	}

	public String getDescription() {
		return TEMPLATE_DESCRIPTION;
	}

	public ConnectionBase getConnection(SimpleFieldList fieldList) {
		SpConnection spConnection = null;
		try {
			ConnectionDeserializer deserializer = this.getConnectionDeserializer();
			if (deserializer != null) {
				List problems = deserializer.getProblems();
				if (problems.isEmpty()) {
					spConnection = (SpConnection) deserializer.getConnection();
				} else {
					log.error((Object) ("Unable to load PluginName Connection Template: " + problems.toString()));
				}
				if (spConnection != null) {
					spConnection.setOrganization(null);
					if (LicenseManager.isSaasProvisioningEnabled()
							&& ProvisionerManager.getProvisionerGlobalSettings().isEnabledInGui()) {
						ProvisionerConfig provisionerConfig = new ProvisionerConfig(spConnection.getEntityId());
						spConnection
								.addConnectionModuleConfiguration((ConnectionModuleConfiguration) provisionerConfig);
					}
				}
			}
		} catch (InvalidXmlException | IOException | XmlException e) {
			log.error((Object) ("Unable to load PluginName Connection Template: " + e.getMessage()));
		}
		return spConnection;
	}

	public String getNote() {
		return null;
	}

	public List<String> checkPrerequisites() {
		return null;
	}

	private ConnectionDeserializer getConnectionDeserializer() throws InvalidXmlException, XmlException, IOException {
		ConnectionDeserializer deserializer;
		deserializer = null;
		Enumeration<URL> resources = null;
		InputStream googleInputStream = null;
		try {
			Object content;
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			resources = classLoader.getResources(PING_ID_CONNECTION);
			URL url = resources.nextElement();
			if (resources.hasMoreElements()) {
				log.warn(
						(Object) "Multiple xml template files detected. Loading the first found. (pingid-connection.xml)");
			}
			if ((content = url.getContent()) instanceof InputStream) {
				googleInputStream = (InputStream) content;
				deserializer = new ConnectionDeserializer(googleInputStream);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			Closer.close(googleInputStream);
		}
		return deserializer;
	}
}
