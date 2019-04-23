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
			emailDeviceID = this.getDeviceID(this.getUserJSON(jsonUserResponse,user,connectionFields));
			pingidforworkforceUser.getCplUser().getResourceAttributes().add(ResourceFieldKey.ACTIVATE_USER.getLabel(),
					(Values) updateUser.getResourceAttributes()
							.get((Object) ResourceFieldKey.ACTIVATE_USER.getLabel()));
			updateUser = pingidforworkforceUser.getCplUser();
		}
		
		if(isEmailChanged(user) && emailDeviceID != null) {
			HttpUriRequest request = this.requestBuilder.buildunpairDeviceRequest(connectionFields,user,emailDeviceID);
			this.httpService.executeFullRequest((Resource) user, request, connectionFields);
			request = this.requestBuilder.buildAddEmailRequest(connectionFields,user);
			this.httpService.executeFullRequest((Resource) user, request, connectionFields);
		}
		HttpUriRequest request = this.requestBuilder.buildUpdateUserRequest(connectionFields, updateUser);
		this.httpService.executeFullRequest((Resource) updateUser, request, connectionFields);
		return this.getUserBySaasGuid(connectionFields, user);
	}
	
	
	private String getDeviceID(JSONObject userObject) {
		if(userObject.containsKey("responseBody")) {
			JSONObject responseBody = (JSONObject) userObject.get("responseBody");
			if(responseBody.containsKey("userDetails")) {
				JSONObject userDetails = (JSONObject) responseBody.get("userDetails");
				if(userDetails.containsKey("devicesDetails")) {
					JSONArray devicesDetails = (JSONArray) userDetails.get("devicesDetails");
					for (Object device :devicesDetails) {
						JSONObject jsonDevice = (JSONObject) device;
						if(jsonDevice.containsKey("type") && "EMAIL".equals(jsonDevice.get("type").toString().toUpperCase()))
								return jsonDevice.get("deviceId").toString();
					}
				}
				
			}
		}
		return null;
	}
	
	public static void main(String[] args) throws ParseException {
		String jsonUser = "{\r\n" + 
				"  \"responseBody\": {\r\n" + 
				"    \"sameDeviceUsersDetails\": [],\r\n" + 
				"    \"userDetails\": {\r\n" + 
				"      \"userName\": \"TESTUSER8\",\r\n" + 
				"      \"userInBypass\": false,\r\n" + 
				"      \"email\": \"vikram+testuser8@likemindsconsulting.com\",\r\n" + 
				"      \"lname\": \"User8\",\r\n" + 
				"      \"userId\": 0,\r\n" + 
				"      \"bypassExpiration\": null,\r\n" + 
				"      \"deviceDetails\": {\r\n" + 
				"        \"enrollment\": \"2019-04-03 10:54:34.536\",\r\n" + 
				"        \"sentNotClaimedSms\": -1,\r\n" + 
				"        \"sentClaimedSms\": -1,\r\n" + 
				"        \"availableNotClaimedSms\": 0,\r\n" + 
				"        \"availableClaimedSms\": 0,\r\n" + 
				"        \"pushEnabled\": false,\r\n" + 
				"        \"displayID\": \"vikram+testuser8@likemindsconsulting.com\",\r\n" + 
				"        \"phoneNumber\": null,\r\n" + 
				"        \"deviceRole\": \"PRIMARY\",\r\n" + 
				"        \"deviceModel\": null,\r\n" + 
				"        \"appVersion\": null,\r\n" + 
				"        \"countryCode\": null,\r\n" + 
				"        \"deviceId\": 1216975380183657000,\r\n" + 
				"        \"deviceUuid\": \"10e390a9-143e-2a28-10e3-90a9143e2a28\",\r\n" + 
				"        \"email\": \"vikram+testuser8@likemindsconsulting.com\",\r\n" + 
				"        \"hasWatch\": false,\r\n" + 
				"        \"nickname\": \"Email 1\",\r\n" + 
				"        \"osVersion\": null,\r\n" + 
				"        \"type\": \"Email\"\r\n" + 
				"      },\r\n" + 
				"      \"lastTransactions\": [],\r\n" + 
				"      \"devicesDetails\": [\r\n" + 
				"        {\r\n" + 
				"          \"enrollment\": \"2019-04-03 10:54:34.536\",\r\n" + 
				"          \"sentNotClaimedSms\": -1,\r\n" + 
				"          \"sentClaimedSms\": -1,\r\n" + 
				"          \"availableNotClaimedSms\": 0,\r\n" + 
				"          \"availableClaimedSms\": 0,\r\n" + 
				"          \"pushEnabled\": false,\r\n" + 
				"          \"displayID\": \"vikram+testuser8@likemindsconsulting.com\",\r\n" + 
				"          \"phoneNumber\": null,\r\n" + 
				"          \"deviceRole\": \"PRIMARY\",\r\n" + 
				"          \"deviceModel\": null,\r\n" + 
				"          \"appVersion\": null,\r\n" + 
				"          \"countryCode\": null,\r\n" + 
				"          \"deviceId\": 1216975380183657000,\r\n" + 
				"          \"deviceUuid\": \"10e390a9-143e-2a28-10e3-90a9143e2a28\",\r\n" + 
				"          \"email\": \"vikram+testuser8@likemindsconsulting.com\",\r\n" + 
				"          \"hasWatch\": false,\r\n" + 
				"          \"nickname\": \"Email 1\",\r\n" + 
				"          \"osVersion\": null,\r\n" + 
				"          \"type\": \"Email\"\r\n" + 
				"        }\r\n" + 
				"      ],\r\n" + 
				"      \"userEnabled\": true,\r\n" + 
				"      \"fname\": \"Test\",\r\n" + 
				"      \"picURL\": \"BF7R2EUAPTRVK76FCDA7TVBE4HTATL3HYI6PKABIJID3G2PXTLCPCRQ=\",\r\n" + 
				"      \"spList\": [],\r\n" + 
				"      \"lastLogin\": null,\r\n" + 
				"      \"status\": \"ACTIVE\",\r\n" + 
				"      \"role\": \"REGULAR\"\r\n" + 
				"    },\r\n" + 
				"    \"errorMsg\": \"ok\",\r\n" + 
				"    \"errorId\": 200,\r\n" + 
				"    \"uniqueMsgId\": \"webs_ZkhwWK-XR5iK7XgmB0fBsuNJ6F5xyIeBj9YtyRHB0PE\",\r\n" + 
				"    \"clientData\": null\r\n" + 
				"  }\r\n" + 
				"}";
		JSONParser parser = new JSONParser(); 
		System.out.println(new PingIDCustomSaasUserAPI(null, null, null).getDeviceID((JSONObject) parser.parse(jsonUser)));
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
		request = this.requestBuilder.buildAddEmailRequest(connectionFields,user);
		this.httpService.executeFullRequest((Resource) user, request, connectionFields);
		
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