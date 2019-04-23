package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID;

import com.pingidentity.integrations.provisioner.pingidforworkforce.ConnectionFieldKey;
import com.pingidentity.integrations.provisioner.pingidforworkforce.ResourceFieldKey;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceUnknownStateException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceUnknownStateException;
import com.pingidentity.io.cpl.pingidforworkforce.request.ConnectionFields;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceAttributes;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceUniqueId;
import com.pingidentity.io.cpl.pingidforworkforce.resource.User;
import com.pingidentity.saas.definition.pingidforworkforce.UserRequestBuilder;
import com.pingidentity.saas.wrapper.pingidforworkforce.UserProfile;
import com.pingidentity.saas.wrapper.pingidforworkforce.request_builder.PingIDForWorkforceEndpoint;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jose4j.pingidforworkforce.base64url.Base64;
import org.jose4j.pingidforworkforce.jws.JsonWebSignature;
import org.jose4j.pingidforworkforce.keys.HmacKey;
import org.jose4j.pingidforworkforce.lang.JoseException;
import org.json.simple.JSONObject;
import org.shaded.pingidforworkforce.apache.HttpEntity;
import org.shaded.pingidforworkforce.apache.client.methods.HttpPost;
import org.shaded.pingidforworkforce.apache.client.methods.HttpUriRequest;
import org.shaded.pingidforworkforce.apache.entity.ContentType;
import org.shaded.pingidforworkforce.apache.entity.StringEntity;

public class PingIDCustomRequestBuilder implements UserRequestBuilder {
	private static final String SCHEME = "https://";
	private static final String API_VERSION = "4.9";
	
	private static final String CREATE_USER_URL = "/pingid/rest/4/adduser/do";
	private static final String OFFLINE_DEVICE_URL = "/pingid/rest/4/offlinepairing/do";
	private static final String UNPAIR_DEVICE_URL = "pingid/rest/4/unpairdevice/do";
	
	public HttpUriRequest buildAddEmailRequest(ConnectionFields connectionFields, User user) 
			throws ResourceException, ServiceException{
		String addDeviceUrl = this.getUrl(OFFLINE_DEVICE_URL, connectionFields);
		UserProfile userProfile = new UserProfile(user.getResourceAttributes());
		JSONObject jsonDevice = this.buildDeviceJSON(userProfile);
		String requestToken = this.buildRequestToken(jsonDevice, connectionFields);
		HttpPost httpRequest = new HttpPost(addDeviceUrl);
		StringEntity entity = new StringEntity(requestToken, ContentType.APPLICATION_JSON);
		httpRequest.setEntity((HttpEntity) entity);
		return httpRequest;
	}
	
	public HttpUriRequest buildunpairDeviceRequest(ConnectionFields connectionFields, User user, String deviceID) 
			throws ResourceException, ServiceException{
		String removeDeviceUrl = this.getUrl(UNPAIR_DEVICE_URL, connectionFields);
		UserProfile userProfile = new UserProfile(user.getResourceAttributes());
		JSONObject jsonDevice = this.buildUnpairDeviceJSON(userProfile, deviceID);
		String requestToken = this.buildRequestToken(jsonDevice, connectionFields);
		HttpPost httpRequest = new HttpPost(removeDeviceUrl);
		StringEntity entity = new StringEntity(requestToken, ContentType.APPLICATION_JSON);
		httpRequest.setEntity((HttpEntity) entity);
		return httpRequest;
	}

	private JSONObject buildUnpairDeviceJSON(UserProfile userProfile, String deviceID) {
		JSONObject jsonDevice = new JSONObject();
		jsonDevice.put((Object) "userName", userProfile.getUsername());
		jsonDevice.put((Object) "deviceId", deviceID);
		return jsonDevice;
	}
	
	private JSONObject buildDeviceJSON(UserProfile userProfile) {
		JSONObject jsonDevice = new JSONObject();
		jsonDevice.put((Object) "username", userProfile.getUsername());
		jsonDevice.put((Object) "type", "EMAIL");
		jsonDevice.put((Object) "pairingData", userProfile.getEmail());
		jsonDevice.put((Object) "validateUniqueDevice", "true");
		return jsonDevice;
	}
	
	public HttpUriRequest buildGetUserRequest(ConnectionFields connectionFields, ResourceUniqueId id)
			throws ResourceException, ServiceException {
		String getUserUrl = this.getUrl(PingIDForWorkforceEndpoint.GET_USER, connectionFields);
		UserProfile userProfile = new UserProfile(id.getId());
		JSONObject user = userProfile.getJsonUser();
		String requestToken = this.buildRequestToken(user, connectionFields);
		HttpPost httpRequest = new HttpPost(getUserUrl);
		StringEntity entity = new StringEntity(requestToken, ContentType.APPLICATION_JSON);
		httpRequest.setEntity((HttpEntity) entity);
		return httpRequest;
	}

	public HttpUriRequest buildUpdateUserRequest(ConnectionFields connectionFields, User user)
			throws ResourceException, ServiceException {
		String updateUserUrl = this.getUrl(PingIDForWorkforceEndpoint.UPDATE_USER, connectionFields);
		UserProfile userProfile = new UserProfile(user.getResourceAttributes());
		JSONObject jsonUser = userProfile.getJsonUser();
		String requestToken = this.buildRequestToken(jsonUser, connectionFields);
		HttpPost httpRequest = new HttpPost(updateUserUrl);
		StringEntity entity = new StringEntity(requestToken, ContentType.APPLICATION_JSON);
		httpRequest.setEntity((HttpEntity) entity);
		return httpRequest;
	}

	public HttpUriRequest buildDeleteUserRequest(ConnectionFields connectionFields, ResourceUniqueId id)
			throws ResourceException, ServiceUnknownStateException {
		String deleteUserUrl = this.getUrl(PingIDForWorkforceEndpoint.DELETE_USER, connectionFields);
		UserProfile userProfile = new UserProfile(id.getId());
		JSONObject user = userProfile.getJsonUser();
		String requestToken = this.buildRequestToken(user, connectionFields);
		HttpPost httpRequest = new HttpPost(deleteUserUrl);
		StringEntity entity = new StringEntity(requestToken, ContentType.APPLICATION_JSON);
		httpRequest.setEntity((HttpEntity) entity);
		return httpRequest;
	}
	
	public HttpUriRequest buildCreateUserRequest(ConnectionFields connectionFields, User user)
			throws ResourceException, ServiceUnknownStateException {
		String createUserUrl = this.getUrl(CREATE_USER_URL, connectionFields);
		UserProfile userProfile = new UserProfile(user.getResourceAttributes());
		JSONObject jsonUser = userProfile.getJsonUser();
		String requestToken = this.buildRequestToken(jsonUser, connectionFields);
		HttpPost httpRequest = new HttpPost(createUserUrl);
		StringEntity entity = new StringEntity(requestToken, ContentType.APPLICATION_JSON);
		httpRequest.setEntity((HttpEntity) entity);
		return httpRequest;
	}

	public HttpUriRequest buildActivateOrSuspendUserRequest(ConnectionFields connectionFields, User user)
			throws ResourceUnknownStateException, ServiceUnknownStateException {
		ResourceAttributes attrs = user.getResourceAttributes();
		String activateSuspendUrl = null;
		String status = attrs.getValueOf(ResourceFieldKey.ACTIVATE_USER.getLabel());
		activateSuspendUrl = Boolean.FALSE.toString().equalsIgnoreCase(status)
				? this.getUrl(PingIDForWorkforceEndpoint.SUSPEND_USER, connectionFields)
				: this.getUrl(PingIDForWorkforceEndpoint.ACTIVATE_USER, connectionFields);
		UserProfile userProfile = new UserProfile(user.getResourceUniqueId().getId());
		JSONObject jsonUser = userProfile.getJsonUser();
		String requestToken = this.buildRequestToken(jsonUser, connectionFields);
		HttpPost httpRequest = new HttpPost(activateSuspendUrl);
		StringEntity entity = new StringEntity(requestToken, ContentType.APPLICATION_JSON);
		httpRequest.setEntity((HttpEntity) entity);
		return httpRequest;
	}

	private String getUrl(PingIDForWorkforceEndpoint type, ConnectionFields connectionFields) {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(SCHEME);
		urlBuilder.append(this.getFormattedDomain(connectionFields));
		urlBuilder.append(type.getEndpoint());
		return urlBuilder.toString();
	}
	
	private String getUrl(String Url, ConnectionFields connectionFields) {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(SCHEME);
		urlBuilder.append(this.getFormattedDomain(connectionFields));
		urlBuilder.append(Url);
		return urlBuilder.toString();
	}

	private String buildRequestToken(JSONObject requestBody, ConnectionFields connectionFields)
			throws ResourceUnknownStateException, ServiceUnknownStateException {
		JSONObject requestHeader = this.buildRequestHeader(connectionFields);
		JSONObject payload = new JSONObject();
		payload.put((Object) "reqHeader", (Object) requestHeader);
		payload.put((Object) "reqBody", (Object) requestBody);
		JsonWebSignature jws = new JsonWebSignature();
		jws.setAlgorithmHeaderValue("HS256");
		jws.setHeader("org_alias", (String) connectionFields.get((Object) ConnectionFieldKey.ORG_ALIAS.getKey()));
		jws.setHeader("token", (String) connectionFields.get((Object) ConnectionFieldKey.TOKEN.getKey()));
		jws.setPayload(payload.toJSONString());
		HmacKey key = new HmacKey(Base64
				.decode((String) ((String) connectionFields.get((Object) ConnectionFieldKey.BASE64_KEY.getKey()))));
		jws.setKey((Key) key);
		String jwsCompactSerialization = null;
		try {
			jwsCompactSerialization = jws.getCompactSerialization();
		} catch (JoseException e) {
			throw new ServiceUnknownStateException(
					String.format("Unable to create Request Token for User: %s.  Error Message: %s",
							requestBody.get((Object) "userName"), e.getMessage()));
		}
		return jwsCompactSerialization;
	}

	private JSONObject buildRequestHeader(ConnectionFields connectionFields) {
		JSONObject reqHeader = new JSONObject();
		reqHeader.put((Object) "locale", (Object) "en");
		reqHeader.put((Object) "orgAlias", connectionFields.get((Object) ConnectionFieldKey.ORG_ALIAS.getKey()));
		reqHeader.put((Object) "secretKey", connectionFields.get((Object) ConnectionFieldKey.TOKEN.getKey()));
		reqHeader.put((Object) "timestamp", (Object) this.getCurrentTimeStamp());
		reqHeader.put((Object) "version", (Object) API_VERSION);
		return reqHeader;
	}

	private String getCurrentTimeStamp() {
		Date currentDate = new Date();
		SimpleDateFormat PingIDDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return PingIDDateFormat.format(currentDate);
	}

	private String getFormattedDomain(ConnectionFields connectionFields) {
		String domain = (String) connectionFields.get((Object) ConnectionFieldKey.DOMAIN.getKey().trim());
		if ("/".equals(domain.substring(domain.length() - 1))) {
			domain = domain.substring(0, domain.length() - 1);
		}
		return domain;
	}
}