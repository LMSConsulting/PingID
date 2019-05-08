package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.shaded.pingidforworkforce.apache.client.methods.HttpUriRequest;

import com.pingidentity.integrations.provisioner.pingidforworkforce.ConnectionFieldKey;
import com.pingidentity.integrations.provisioner.pingidforworkforce.ResourceFieldKey;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceException;
import com.pingidentity.io.cpl.pingidforworkforce.request.ConnectionFields;
import com.pingidentity.io.cpl.pingidforworkforce.resource.Resource;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceUniqueId;
import com.pingidentity.io.cpl.pingidforworkforce.resource.User;
import com.pingidentity.io.cpl.pingidforworkforce.resource.Values;
import com.pingidentity.saas.definition.pingidforworkforce.SaasHttpService;
import com.pingidentity.saas.definition.pingidforworkforce.SaasUser;
import com.pingidentity.saas.definition.pingidforworkforce.SaasUserApi;
import com.pingidentity.saas.definition.pingidforworkforce.SaasUserBuilder;
import com.pingidentity.saas.wrapper.pingidforworkforce.http_service.serialization.SerializationException;
import com.pingidentity.saas.wrapper.pingidforworkforce.resource_builder.deserialization.PingIDForWorkforceDeserializer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class PingIDCustomSaasUserAPI implements SaasUserApi{
	
	private static final String TRUE = "true";
	private PingIDCustomRequestBuilder requestBuilder;
	private SaasHttpService httpService;
	private SaasUserBuilder userBuilder;

	public PingIDCustomSaasUserAPI(PingIDCustomRequestBuilder requestBuilder, SaasHttpService httpService,
			SaasUserBuilder userBuilder) {
		this.requestBuilder = requestBuilder;
		this.httpService = httpService;
		this.userBuilder = userBuilder;
	}

	public SaasUser getUserBySaasGuid(ConnectionFields connectionFields, User user)
			throws ResourceException, ServiceException, SerializationException {
		HttpUriRequest request = this.requestBuilder.buildGetUserRequest(connectionFields, user.getResourceUniqueId());
		String userString = this.httpService.executeFullRequest((Resource) user, request, connectionFields);
		return this.userBuilder.buildUser(userString, user, connectionFields);
	}
	
	private String getResponseStringBySaasGuid(ConnectionFields connectionFields, User user) 
			throws ResourceException, ServiceException, SerializationException{
		HttpUriRequest request = this.requestBuilder.buildGetUserRequest(connectionFields, user.getResourceUniqueId());
		String userString = this.httpService.executeFullRequest((Resource) user, request, connectionFields);
		return userString;
	}

	public SaasUser updateUser(ConnectionFields connectionFields, User user)
			throws ServiceException, ResourceException, SerializationException {
		User updateUser = user;
		String emailDeviceID = "";
		if (this.notAllowedToUpdateUsers(connectionFields)) {
			String jsonUserResponse = this.getResponseStringBySaasGuid(connectionFields,user);
			SaasUser pingidforworkforceUser = this.userBuilder.buildUser(jsonUserResponse,user,connectionFields);
			pingidforworkforceUser.getCplUser().getResourceAttributes().add(ResourceFieldKey.ACTIVATE_USER.getLabel(),
					(Values) updateUser.getResourceAttributes()
							.get((Object) ResourceFieldKey.ACTIVATE_USER.getLabel()));
			updateUser = pingidforworkforceUser.getCplUser();
		}
		
		if(isEmailChanged(user) ) {
			String jsonUserResponse = this.getResponseStringBySaasGuid(connectionFields,user);
			emailDeviceID = this.getDeviceID(this.getUserJSON(jsonUserResponse,user,connectionFields));
			if(emailDeviceID != null) {
				HttpUriRequest request = this.requestBuilder.buildunpairDeviceRequest(connectionFields,user,emailDeviceID);
				this.httpService.executeFullRequest((Resource) user, request, connectionFields);
			}
			HttpUriRequest request = this.requestBuilder.buildAddEmailRequest(connectionFields,user);
			this.httpService.executeFullRequest((Resource) user, request, connectionFields);
		}
		HttpUriRequest request = this.requestBuilder.buildUpdateUserRequest(connectionFields, updateUser);
		this.httpService.executeFullRequest((Resource) updateUser, request, connectionFields);
		return this.getUserBySaasGuid(connectionFields, user);
	}
	
	
	private String getDeviceID(JSONObject userObject) {
				if(userObject.containsKey("devicesDetails")) {
					JSONArray devicesDetails = (JSONArray) userObject.get("devicesDetails");
					for (Object device :devicesDetails) {
						JSONObject jsonDevice = (JSONObject) device;
						if(jsonDevice.containsKey("type") && "EMAIL".equals(jsonDevice.get("type").toString().toUpperCase()))
								return jsonDevice.get("deviceId").toString();
					}
				}
		return null;
	}
	
	private JSONObject getUserJSON(String jsonResponseString, User user, ConnectionFields connectionFields)
			   throws ResourceException
	{
			JSONObject json = null;
			String jsonUser = "";
			try    {
				jsonUser = new PingIDForWorkforceDeserializer().decryptJSON(jsonResponseString, user, connectionFields);
				JSONParser parser = new JSONParser(); 
				json = (JSONObject) parser.parse(jsonUser);
			}catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 return json;
	}
	
	private boolean isEmailChanged(User user) {
		if(user.getResourceAttributes().containsKey("emailChanged")) {
			String emailChanged = user.getResourceAttributes().getValueOf("emailChanged");
			return Boolean.parseBoolean(emailChanged);
		}
		return false;
	}
	
	public void deleteUser(ConnectionFields connectionFields, User user)
			throws ServiceException, ResourceException, SerializationException {
		HttpUriRequest request = this.requestBuilder.buildDeleteUserRequest(connectionFields,
				user.getResourceUniqueId());
		this.httpService.executeFullRequest((Resource) user, request, connectionFields);
	}
	
	public void createUser(ConnectionFields connectionFields, User user)
			throws ServiceException, ResourceException, SerializationException {
		HttpUriRequest request = this.requestBuilder.buildCreateUserRequest(connectionFields,
				user);
		this.httpService.executeFullRequest((Resource) user, request, connectionFields);
		// Add Email address to PingID devices on create
		if(user.getResourceAttributes().containsKey("email")) {
			request = this.requestBuilder.buildAddEmailRequest(connectionFields,user);
			this.httpService.executeFullRequest((Resource) user, request, connectionFields);
		}
	}

	public void checkConnection(ConnectionFields connectionFields) throws ServiceException, SerializationException {
		try {
			HttpUriRequest request = this.requestBuilder.buildGetUserRequest(connectionFields,
					new ResourceUniqueId("id"));
			this.httpService.executeFullRequest((Resource) new User(), request, connectionFields);
		} catch (ResourceException request) {
			// empty catch block
		}
	}

	public void activateOrSuspendUser(ConnectionFields connectionFields, User user)
			throws ServiceException, ResourceException, SerializationException {
		HttpUriRequest request = this.requestBuilder.buildActivateOrSuspendUserRequest(connectionFields, user);
		this.httpService.executeFullRequest((Resource) user, request, connectionFields);
	}

	private boolean notAllowedToUpdateUsers(ConnectionFields connectionFields) {
		return connectionFields.containsKey((Object) ConnectionFieldKey.UPDATE_NEW_USERS.getKey())
				&& !((String) connectionFields.get((Object) ConnectionFieldKey.UPDATE_NEW_USERS.getKey()))
						.equalsIgnoreCase(TRUE);
	}
	
}