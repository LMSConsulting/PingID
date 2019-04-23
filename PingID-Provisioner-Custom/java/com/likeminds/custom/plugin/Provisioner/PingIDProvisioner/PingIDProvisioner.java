package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sourceid.saml20.adapter.conf.Field;
import org.sourceid.saml20.adapter.gui.FieldDescriptor;

import com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.metadata.PingIDCustomMetadataServiceFactory;
import com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.metadata.PingIDCustomWorker;
import com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID.PingIDCustomUserRequestFactory;
import com.pingidentity.integrations.provisioner.pingidforworkforce.ConnectionFieldKey;
import com.pingidentity.integrations.provisioner.pingidforworkforce.ResourceFieldKey;
import com.pingidentity.io.cpl.pingidforworkforce.IWorker;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceException;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.AttributeMetadata;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ConnectionProfile;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ProvisionerInformation;
import com.pingidentity.io.cpl.pingidforworkforce.request.ConnectionFields;
import com.pingidentity.io.cpl.pingidforworkforce.request.GetAttributesRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.GetConnectionProfilesRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.GetInfoRequest;
import com.pingidentity.prov.saas.pingidforworkforce.PluginWorker;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.MetadataService;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.MetadataServiceFactory;
import com.pingidentity.prov.saas.pingidforworkforce.request.DefaultUserRequestBuilderFactory;
import com.pingidentity.prov.saas.pingidforworkforce.request.RequestBuilderFactory;
import com.pingidentity.provisioner.saas.pingidforworkforce.PingIDForWorkforcePluginWorker;
import com.pingidentity.provisioner.sdk.AbstractSaasPlugin;
import com.pingidentity.provisioner.sdk.SaasPluginException;
import com.pingidentity.provisioner.sdk.SaasPluginFieldInfo;
import com.pingidentity.provisioner.sdk.SaasUserData;



public class PingIDProvisioner extends AbstractSaasPlugin {

	private static final long serialVersionUID = -2088797560933737361L;
	private static final String TRUE = "true";
	private transient PluginWorker worker;
	private transient PingIDCustomWorker cplWorker;
	private transient MetadataService metadataService;
	private static Logger logger;

	public PingIDProvisioner() {
		PingIDProvisioner.setLogger(Logger.getLogger(this.getClass()));
		this.cplWorker = new PingIDCustomWorker();
		ProvisionerInformation info = this.cplWorker.getInfo(new GetInfoRequest()).getInformation();
		ConnectionProfile profile = (ConnectionProfile) this.cplWorker
				.getConnectionProfiles(new GetConnectionProfilesRequest()).getConnectionProfiles().get(0);
		AttributeMetadata attributeMetadata = this.cplWorker.getAttributes(new GetAttributesRequest())
				.getAttributeMetadata();
		PingIDCustomMetadataServiceFactory metadataServiceFactory = new PingIDCustomMetadataServiceFactory(
				info, profile, attributeMetadata);
		PingIDCustomUserRequestFactory requestBuilderFactory = new PingIDCustomUserRequestFactory(
				(MetadataServiceFactory) metadataServiceFactory, ResourceFieldKey.ACTIVATE_USER.getLabel(),
				Boolean.valueOf(true));
		this.worker = new PingIDForWorkforcePluginWorker((IWorker) this.cplWorker,
				(RequestBuilderFactory) requestBuilderFactory);
		this.metadataService = metadataServiceFactory.make();
	}

	public PingIDProvisioner(Logger log, PluginWorker worker, MetadataService uiService) {
		PingIDProvisioner.setLogger(log);
		this.worker = worker;
		this.metadataService = uiService;
	}

	public static Logger getLogger() {
		return logger;
	}

	public static void setLogger(Logger logger) {
		PingIDProvisioner.logger = logger;
	}

	public String getSaasUserIdFieldName() {
		return ResourceFieldKey.getPrimaryId().getLabel();
	}

	public String getSaasUsernameFieldCode() {
		return ResourceFieldKey.getSecondaryId().getLabel();
	}

	public SaasUserData getUser(String saasUserGuid, String saasUsername) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ getUser()");
		String saasGuid = saasUserGuid;
		ConnectionFields connectionFields = this.makeConnectionFields();
		if (saasGuid == null || saasGuid.isEmpty()) {
			saasGuid = saasUsername != null && !saasUsername.isEmpty() ? saasUsername : "";
		}
		SaasUserData saasUserData = new SaasUserData(saasGuid);
		PingIDProvisioner.getLogger().info(
				(Object) String.format("Using %s: [%s] for getUser()", this.getSaasUsernameFieldCode(), saasUsername));
		try {
			saasUserData = this.worker.executeGetUser(saasUserData, connectionFields);
		} catch (ServiceException e) {
			throw new SaasPluginException(e.getFormattedMessage());
		} catch (ResourceException e) {
			throw new SaasPluginException(e.getFormattedMessage());
		}
		if (saasUserData == null) {
			PingIDProvisioner.getLogger().info((Object) ("User [" + saasUsername + "] was not found in target"));
		} else {
			PingIDProvisioner.getLogger().info((Object) ("User [" + saasUsername + "] was found in target"));
		}
		PingIDProvisioner.getLogger().debug((Object) "- getUser()");
		return saasUserData;
	}

	public String createUser(SaasUserData userData) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ createUser()");
		SaasUserData cloneUserData = userData;
		String id = "";
		ConnectionFields connectionFields = this.makeConnectionFields();
		if(connectionFields.containsKey((Object) ConnectionFieldKey.CREATE_NEW_USERS.getKey())) {
			id = this.executeCreateUser(cloneUserData);
		}
		PingIDProvisioner.getLogger().debug((Object) "- createUser()");
		return id;
	}

	public String updateUser(SaasUserData oldUserData, SaasUserData newUserData) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ updateUser()");
		String id = oldUserData.getSaasUserGuid();
		SaasUserData cloneUserData = this.cloneSaasUserDataWithSaasUserGuid(oldUserData.getSaasUserGuid(), newUserData);
		ConnectionFields connectionFields = this.makeConnectionFields();
		if (cloneUserData.isAccountEnabled()) {
			if(!oldUserData.getAttributeFirstValue("email").toLowerCase().
					equals(newUserData.getAttributeFirstValue("email").toLowerCase()))
				id=this.executeUpdateUserWithDeviceChange(cloneUserData);
			else 
				id = this.executeUpdateUser(cloneUserData);
		} else if (connectionFields.containsKey((Object) ConnectionFieldKey.REMOVE_ACTION.getKey())
				&& "Delete".equalsIgnoreCase(
						(String) connectionFields.get((Object) ConnectionFieldKey.REMOVE_ACTION.getKey()))
				&& this.metadataService.supportsDelete()) {
			this.deleteUser(newUserData);
		} else if (this.metadataService.supportsDisable()) {
			this.disableUser(newUserData);
		}
		PingIDProvisioner.getLogger().debug((Object) "- updateUser()");
		return id;
	}

	public void initSaasConnection(List<Field> arg0) throws SaasPluginException {
	}

	public void checkSaasConnection() throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ checkSaasConnection()");
		try {
			this.worker.executeCheckConnection(this.makeConnectionFields());
		} catch (ServiceException e) {
			throw new SaasPluginException(e.getFormattedMessage());
		}
		PingIDProvisioner.getLogger().debug((Object) "- checkSaasConnection()");
	}

	public void closeSaasConnection() throws SaasPluginException {
	}

	public List<FieldDescriptor> getConnectionParameterDescriptors() {
		return this.metadataService.getConnectionDescriptors();
	}

	public String getId() {
		return this.metadataService.getName();
	}

	public String getDescription() {
		return this.metadataService.getDescription();
	}

	public List<SaasPluginFieldInfo> getFieldInfo() throws SaasPluginException {
		return this.metadataService.getAttributeDescriptors();
	}

	public Properties loadDefaultMappings() throws IOException {
		return this.metadataService.getDefaultMappings();
	}

	public int getDefaultMaxThreads() {
		return 1;
	}

	private SaasUserData cloneSaasUserDataWithSaasUserGuid(String saasGuid, SaasUserData saasUserData) {
		SaasUserData clone = new SaasUserData(saasGuid);
		clone.setAccountEnabled(saasUserData.isAccountEnabled());
		clone.setAttributeMap(saasUserData.getAttributeMap());
		clone.setInternalGuid(saasUserData.getInternalGuid());
		return clone;
	}

	private String executeUpdateUser(SaasUserData userData) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ executeUpdateUser()");
		ConnectionFields connectionFields = this.makeConnectionFields();
		String saasGuid = null;
		if (connectionFields.containsKey((Object) ConnectionFieldKey.UPDATE_NEW_USERS.getKey())
				&& !((String) connectionFields.get((Object) ConnectionFieldKey.UPDATE_NEW_USERS.getKey()))
						.equalsIgnoreCase(TRUE)) {
			PingIDProvisioner.getLogger()
					.warn((Object) ("Unable to update user as the Update Users flag is set to false. User details: "
							+ userData.getAttributeFirstValue(ResourceFieldKey.getSecondaryId().getLabel())));
		} else {
			try {
				saasGuid = this.worker.executeUpdateUser(userData, connectionFields);
				PingIDProvisioner.getLogger().info((Object) "User was successfully updated in target");
			} catch (ServiceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			} catch (ResourceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			}
		}
		PingIDProvisioner.getLogger().debug((Object) "- executeUpdateUser()");
		return saasGuid;
	}
	
	private String executeUpdateUserWithDeviceChange(SaasUserData userData) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ executeUpdateUserWithDeviceChange()");
		ConnectionFields connectionFields = this.makeConnectionFields();
		String saasGuid = null;
		if (connectionFields.containsKey((Object) ConnectionFieldKey.UPDATE_NEW_USERS.getKey())
				&& !((String) connectionFields.get((Object) ConnectionFieldKey.UPDATE_NEW_USERS.getKey()))
						.equalsIgnoreCase(TRUE)) {
			PingIDProvisioner.getLogger()
					.warn((Object) ("Unable to update user as the Update Users flag is set to false. User details: "
							+ userData.getAttributeFirstValue(ResourceFieldKey.getSecondaryId().getLabel())));
		} else {
			try {
				userData.getAttributeMap().put("emailChanged",Arrays.asList("true"));
				saasGuid = this.worker.executeUpdateUser(userData, connectionFields);
				PingIDProvisioner.getLogger().info((Object) "User was successfully updated in target");
			} catch (ServiceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			} catch (ResourceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			}
		}
		PingIDProvisioner.getLogger().debug((Object) "- executeUpdateUserWithDeviceChange()");
		return saasGuid;
	}
	
	private String executeCreateUser(SaasUserData userData) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ executeCreateUser()");
		ConnectionFields connectionFields = this.makeConnectionFields();
		String saasGuid = null;
		if (connectionFields.containsKey((Object) ConnectionFieldKey.CREATE_NEW_USERS.getKey())
				&& !((String) connectionFields.get((Object) ConnectionFieldKey.CREATE_NEW_USERS.getKey()))
						.equalsIgnoreCase(TRUE)) {
			PingIDProvisioner.getLogger()
					.warn((Object) ("Unable to create user as the Create Users flag is set to false. User details: "
							+ userData.getAttributeFirstValue(ResourceFieldKey.getSecondaryId().getLabel())));
		} else {
			try {
				saasGuid = this.worker.executeCreateUser(userData, connectionFields);
				PingIDProvisioner.getLogger().info((Object) "User was successfully Created in target");
			} catch (ServiceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			} catch (ResourceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			}
		}
		PingIDProvisioner.getLogger().debug((Object) "- executeCreateUser()");
		return saasGuid;
	}

	private String disableUser(SaasUserData userData) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ disableUser()");
		ConnectionFields connectionFields = this.makeConnectionFields();
		String saasGuid = null;
		userData.setAccountEnabled(false);
		if (connectionFields.containsKey((Object) ConnectionFieldKey.DISABLE_NEW_USERS.getKey())
				&& !((String) connectionFields.get((Object) ConnectionFieldKey.DISABLE_NEW_USERS.getKey()))
						.equalsIgnoreCase(TRUE)) {
			PingIDProvisioner.getLogger()
					.warn((Object) ("Unable to disable user as the Disable flag is set to false. User details: "
							+ userData.getAttributeFirstValue(ResourceFieldKey.getSecondaryId().getLabel())));
		} else {
			try {
				saasGuid = this.worker.executeUpdateUser(userData, connectionFields);
				PingIDProvisioner.getLogger().info((Object) "User was successfully disabled in target");
			} catch (ServiceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			} catch (ResourceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			}
		}
		PingIDProvisioner.getLogger().debug((Object) "- disableUser()");
		return saasGuid;
	}

	private void deleteUser(SaasUserData userData) throws SaasPluginException {
		PingIDProvisioner.getLogger().debug((Object) "+ deleteUser()");
		if (userData.getSaasUserGuid() == null || StringUtils.isEmpty((String) userData.getSaasUserGuid())) {
			String saasUserGuid = userData.getAttributeFirstValue(ResourceFieldKey.USERNAME.getLabel());
			userData = new SaasUserData(saasUserGuid);
		}
		ConnectionFields connectionFields = this.makeConnectionFields();
		userData.setAccountEnabled(false);
		if (connectionFields.containsKey((Object) ConnectionFieldKey.DISABLE_NEW_USERS.getKey())
				&& !((String) connectionFields.get((Object) ConnectionFieldKey.DISABLE_NEW_USERS.getKey()))
						.equalsIgnoreCase(TRUE)) {
			PingIDProvisioner.getLogger()
					.warn((Object) ("Unable to delete user as the Disable flag is set to false. User details: "
							+ userData.getAttributeFirstValue(ResourceFieldKey.getSecondaryId().getLabel())));
		} else {
			try {
				this.worker.executeDeleteUser(userData, connectionFields);
				PingIDProvisioner.getLogger().info((Object) "User was successfully deleted in target");
			} catch (ServiceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			} catch (ResourceException e) {
				throw new SaasPluginException(e.getFormattedMessage());
			}
		}
		PingIDProvisioner.getLogger().debug((Object) "- deleteUser()");
	}

	private ConnectionFields makeConnectionFields() {
		ConnectionFields connectionFields = new ConnectionFields();
		for (Field field : this._parameters.getFields()) {
			connectionFields.put(field.getName(), field.getValue());
		}
		return connectionFields;
	}
}
