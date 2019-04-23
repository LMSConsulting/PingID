package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.metadata;

import com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID.PingIDCustomSaasUserAPI;
import com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID.PingIDCustomWorkerFactory;
import com.pingidentity.integrations.provisioner.metadata.attribute.pingidforworkforce.AttributeBuilder;
import com.pingidentity.integrations.provisioner.metadata.attribute.pingidforworkforce.ConnectionProfilesBuilder;
import com.pingidentity.integrations.provisioner.metadata.attribute.pingidforworkforce.PingIDForWorkforceConnectionProfileBuilder;
import com.pingidentity.integrations.provisioner.metadata.attribute.pingidforworkforce.PingIDForWorkforceUserAttributeBuilder;
import com.pingidentity.integrations.provisioner.metadata.capability.pingidforworkforce.CapabilityBuilder;
import com.pingidentity.integrations.provisioner.metadata.enhancement.pingidforworkforce.EnhancementBuilder;
import com.pingidentity.integrations.provisioner.metadata.enhancement.pingidforworkforce.PingIDForWorkforceEnhancementBuilder;
import com.pingidentity.integrations.provisioner.metadata.pingidforworkforce.AttributeMetadataBuilder;
import com.pingidentity.integrations.provisioner.metadata.pingidforworkforce.PingIDForWorkforceAttributeMetadataBuilder;
import com.pingidentity.integrations.provisioner.metadata.pingidforworkforce.ProvisionerInformationBuilder;
import com.pingidentity.integrations.provisioner.pingidforworkforce.ApplicationMessage;
import com.pingidentity.integrations.provisioner.pingidforworkforce.ResourceFieldKey;
import com.pingidentity.integrations.provisioner.pingidforworkforce.UserStatus;
import com.pingidentity.io.cpl.pingidforworkforce.Worker;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceNotFoundException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceNotModifiedException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceUnknownStateException;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.AttributeMetadata;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ConnectionProfiles;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ProvisionerInformation;
import com.pingidentity.io.cpl.pingidforworkforce.request.CheckConnectionRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.ConnectionFields;
import com.pingidentity.io.cpl.pingidforworkforce.request.CreateUsersRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.DeleteUsersRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.GetAttributesRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.GetConnectionProfilesRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.GetInfoRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.GetUsersRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.UpdateUsersRequest;
import com.pingidentity.io.cpl.pingidforworkforce.resource.Resource;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceAttributes;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceUniqueId;
import com.pingidentity.io.cpl.pingidforworkforce.resource.User;
import com.pingidentity.io.cpl.pingidforworkforce.response.CheckConnectionResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.CreateUsersResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.DeleteUsersResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.GetAttributesResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.GetConnectionProfilesResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.GetInfoResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.GetUsersResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.UpdateUsersResponse;
import com.pingidentity.io.cpl.pingidforworkforce.response.UserAttempt;
import com.pingidentity.provisioner.ProvisioningMessage;
import com.pingidentity.saas.definition.pingidforworkforce.SaasUser;
import com.pingidentity.saas.definition.pingidforworkforce.SaasUserApi;
import com.pingidentity.saas.wrapper.pingidforworkforce.http_service.serialization.SerializationException;
import com.pingidentity.shaded.pingidforworkforce.common.adptlogger.AdptLogger;
import com.pingidentity.shaded.pingidforworkforce.common.adptlogger.Event;
import com.pingidentity.shaded.pingidforworkforce.common.adptlogger.LoggerConfiguration;
import org.shaded.pingidforworkforce.apache.util.TextUtils;

public class PingIDCustomWorker extends Worker {
	private ProvisionerInformationBuilder provisionerInformationBuilder;
	private ConnectionProfilesBuilder connectionProfilesBuilder;
	private AttributeMetadataBuilder attributeMetadataBuilder;
	private PingIDCustomSaasUserAPI saasWrapper;
	private LoggerConfiguration loggerConfiguration = new LoggerConfiguration.LoggerConfigurationBuilder("SPIW")
			.build();
	private AdptLogger adptLogger = new AdptLogger(ProvisioningMessage.class.getName(), this.loggerConfiguration);

	public PingIDCustomWorker() {
		PingIDCustomCapabilityBuilder capabilitiesBuilder = new PingIDCustomCapabilityBuilder();
		PingIDForWorkforceEnhancementBuilder enhancementBuilder = new PingIDForWorkforceEnhancementBuilder();
		PingIDForWorkforceUserAttributeBuilder userAttributes = new PingIDForWorkforceUserAttributeBuilder();
		this.provisionerInformationBuilder = new PingIDCustomProvisionerInformationBuilder();
		this.connectionProfilesBuilder = new PingIDForWorkforceConnectionProfileBuilder();
		this.attributeMetadataBuilder = new PingIDForWorkforceAttributeMetadataBuilder(
				(CapabilityBuilder) capabilitiesBuilder, (EnhancementBuilder) enhancementBuilder,
				(AttributeBuilder) userAttributes);
		this.saasWrapper = (PingIDCustomSaasUserAPI) new PingIDCustomWorkerFactory().make();
	}

	public PingIDCustomWorker(ProvisionerInformationBuilder provisionerInformationBuilder,
			ConnectionProfilesBuilder connectionProfilesBuilder, AttributeMetadataBuilder attributeMetadataBuilder,
			SaasUserApi saasWrapper) {
		this.provisionerInformationBuilder = provisionerInformationBuilder;
		this.connectionProfilesBuilder = connectionProfilesBuilder;
		this.attributeMetadataBuilder = attributeMetadataBuilder;
		this.saasWrapper = (PingIDCustomSaasUserAPI) saasWrapper;
	}

	public GetInfoResponse getInfo(GetInfoRequest request) {
		this.adptLogger.debug(
				new Event().setCode(ApplicationMessage.WARN.getCode()).setMessage("PingIDCustomWorker.getInfo"));
		ProvisionerInformation information = this.provisionerInformationBuilder.build();
		GetInfoResponse response = new GetInfoResponse();
		response.setInformation(information);
		return response;
	}

	public GetConnectionProfilesResponse getConnectionProfiles(GetConnectionProfilesRequest request) {
		this.adptLogger.debug(new Event().setCode(ApplicationMessage.WARN.getCode())
				.setMessage("PingIDForWorkforceWorker.getConnectionProfiles"));
		ConnectionProfiles connectionProfiles = this.connectionProfilesBuilder.build();
		GetConnectionProfilesResponse response = new GetConnectionProfilesResponse();
		response.setConnectionProfiles(connectionProfiles);
		return response;
	}

	public GetAttributesResponse getAttributes(GetAttributesRequest request) {
		this.adptLogger.debug(new Event().setCode(ApplicationMessage.WARN.getCode())
				.setMessage("PingIDForWorkforceWorker.getAttributes"));
		AttributeMetadata attributeMetadata = this.attributeMetadataBuilder.build();
		GetAttributesResponse response = new GetAttributesResponse();
		response.setAttributeMetadata(attributeMetadata);
		return response;
	}

	public CheckConnectionResponse checkConnection(CheckConnectionRequest request) {
		this.adptLogger.debug(new Event().setCode(ApplicationMessage.WARN.getCode())
				.setMessage("PingIDForWorkforceWorker.checkConnection"));
		ConnectionFields connectionFields = request.getConnectionFields();
		CheckConnectionResponse response = new CheckConnectionResponse();
		try {
			this.saasWrapper.checkConnection(connectionFields);
		} catch (ServiceException e) {
			response.addServiceException(e);
		} catch (SerializationException e) {
			response.addServiceException((ServiceException) new ServiceUnknownStateException(e.getMessage()));
		}
		return response;
	}

	public GetUsersResponse getUsers(GetUsersRequest request) {
		this.adptLogger.debug(
				new Event().setCode(ApplicationMessage.WARN.getCode()).setMessage("PingIDForWorkforceWorker.getUsers"));
		ConnectionFields connectionFields = request.getConnectionFields();
		User requestUser = (User) request.getUsers().get(0);
		ResourceUniqueId resourceUniqueId = requestUser.getResourceUniqueId();
		GetUsersResponse response = new GetUsersResponse();
		SaasUser user = null;
		UserAttempt userAttempt = new UserAttempt();
		response.addUserAttempt(userAttempt);
		try {
			try {
				user = this.saasWrapper.getUserBySaasGuid(connectionFields, requestUser);
			} catch (ResourceNotFoundException e) {
				return this.add404UserToResponse(resourceUniqueId, response);
			} catch (SerializationException e) {
				response.addServiceException((ServiceException) new ServiceUnknownStateException(e.getMessage()));
			}
			if (user != null) {
				userAttempt.setUser(user.getCplUser());
			}
		} catch (ServiceException e) {
			response.addServiceException(e);
			return response;
		} catch (ResourceException e) {
			userAttempt.addResourceException(e);
		}
		return response;
	}

	public UpdateUsersResponse updateUsers(UpdateUsersRequest request) {
		this.adptLogger.debug(new Event().setCode(ApplicationMessage.WARN.getCode())
				.setMessage("PingIDForWorkforceWorker.updateUsers"));
		ConnectionFields connectionFields = request.getConnectionFields();
		User user = (User) request.getUsers().get(0);
		if (user.getResourceUniqueId().getId() == null
				|| TextUtils.isEmpty((CharSequence) user.getResourceUniqueId().getId())) {
			ResourceUniqueId resourceUniqueId = new ResourceUniqueId(
					user.getResourceAttributes().getValueOf(ResourceFieldKey.USERNAME.getLabel()));
			user.setResourceUniqueId(resourceUniqueId);
		}
		UpdateUsersResponse response = new UpdateUsersResponse();
		UserAttempt userAttempt = new UserAttempt();
		response.addUserAttempt(userAttempt);
		SaasUser saasUser = null;
		SaasUser getSaasUser = null;
		try {
			getSaasUser = this.saasWrapper.getUserBySaasGuid(connectionFields, user);
		} catch (ServiceException e) {
			response.addServiceException(e);
			return response;
		} catch (ResourceException e) {
			userAttempt.addResourceException(e);
		} catch (SerializationException e) {
			userAttempt.addResourceException(
					(ResourceException) new ResourceNotModifiedException((Resource) user, e.getMessage()));
		}
		String pingIdUserStatus = null;
		ResourceAttributes updateAttrs = user.getResourceAttributes();
		String usorUserStatus = updateAttrs.getValueOf(ResourceFieldKey.ACTIVATE_USER.getLabel());
		if (getSaasUser != null) {
			User getUser = getSaasUser.getCplUser();
			ResourceAttributes attrs = getUser.getResourceAttributes();
			pingIdUserStatus = attrs.getValueOf(ResourceFieldKey.STATUS.getLabel());
		}
		if (this.userIsActiveOrSuspendedInPingID(pingIdUserStatus)) {
			try {
				if (this.userStatusIsDifferentInUSoR(pingIdUserStatus, usorUserStatus)) {
					this.saasWrapper.activateOrSuspendUser(connectionFields, user);
				}
			} catch (ServiceException e) {
				response.addServiceException(e);
				return response;
			} catch (ResourceException e) {
				userAttempt.addResourceException(e);
			} catch (SerializationException e) {
				userAttempt.addResourceException(
						(ResourceException) new ResourceNotModifiedException((Resource) user, e.getMessage()));
			}
		}
		try {
			saasUser = this.saasWrapper.updateUser(connectionFields, user);
			userAttempt.setUser(saasUser.getCplUser());
		} catch (ServiceException e) {
			response.addServiceException(e);
			return response;
		} catch (ResourceException e) {
			userAttempt.addResourceException(e);
		} catch (SerializationException e) {
			response.addServiceException((ServiceException) new ServiceUnknownStateException(e.getMessage()));
		}
		return response;
	}

	public DeleteUsersResponse deleteUsers(DeleteUsersRequest request) {
		this.adptLogger.debug(new Event().setCode(ApplicationMessage.WARN.getCode())
				.setMessage("PingIDForWorkforceWorker.deleteUsers"));
		ConnectionFields connectionFields = request.getConnectionFields();
		User user = (User) request.getUsers().get(0);
		DeleteUsersResponse response = new DeleteUsersResponse();
		UserAttempt userAttempt = new UserAttempt();
		response.addUserAttempt(userAttempt);
		try {
			this.saasWrapper.deleteUser(connectionFields, user);
		} catch (ServiceException e) {
			response.addServiceException(e);
			return response;
		} catch (ResourceException e) {
			userAttempt.addResourceException(e);
		} catch (SerializationException e) {
			response.addServiceException((ServiceException) new ServiceUnknownStateException(e.getMessage()));
		}
		return response;
	}
	
	public CreateUsersResponse createUsers(CreateUsersRequest request) {
		this.adptLogger.debug(new Event().setCode(ApplicationMessage.WARN.getCode())
				.setMessage("PingIDForWorkforceWorker.createUsers"));
		ConnectionFields connectionFields = request.getConnectionFields();
		User user = (User) request.getUsers().get(0);
		CreateUsersResponse response = new CreateUsersResponse();
		UserAttempt userAttempt = new UserAttempt();
		response.addUserAttempt(userAttempt);
		try {
			this.saasWrapper.createUser(connectionFields, user);
			if (user != null) {
				userAttempt.setUser(user);
			}
		} catch (ServiceException e) {
			response.addServiceException(e);
			return response;
		} catch (ResourceException e) {
			userAttempt.addResourceException(e);
		} catch (SerializationException e) {
			response.addServiceException((ServiceException) new ServiceUnknownStateException(e.getMessage()));
		}
		return response;
	}

	private GetUsersResponse add404UserToResponse(ResourceUniqueId resourceUniqueId, GetUsersResponse response) {
		ResourceNotFoundException ex = new ResourceNotFoundException((Resource) new User(resourceUniqueId),
				String.format("%s User Not Found", ApplicationMessage.ERR_RESOURCE_NOT_FOUND.getCode()));
		((UserAttempt) response.getUserAttempts().get(0)).addResourceException((ResourceException) ex);
		return response;
	}

	private boolean userIsActiveOrSuspendedInPingID(String pingIdUserStatus) {
		return pingIdUserStatus != null && !pingIdUserStatus.isEmpty()
				&& (pingIdUserStatus.equalsIgnoreCase(UserStatus.ACTIVE.getStatus())
						|| pingIdUserStatus.equalsIgnoreCase(UserStatus.SUSPENDED.getStatus()));
	}

	private boolean userStatusIsDifferentInUSoR(String pingIdUserStatus, String usorUserStatus) {
		return !UserStatus.findByStatus((String) pingIdUserStatus).getBooleanValue().equalsIgnoreCase(usorUserStatus);
	}
}