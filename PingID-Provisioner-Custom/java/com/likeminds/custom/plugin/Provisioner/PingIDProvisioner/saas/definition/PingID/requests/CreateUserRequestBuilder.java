package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID.requests;

import com.pingidentity.io.cpl.pingidforworkforce.metadata.Attributes;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.BaseAttribute;
import com.pingidentity.io.cpl.pingidforworkforce.request.ConnectionFields;
import com.pingidentity.io.cpl.pingidforworkforce.request.CreateUsersRequest;
import com.pingidentity.io.cpl.pingidforworkforce.request.Request;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceAttributes;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceUniqueId;
import com.pingidentity.io.cpl.pingidforworkforce.resource.User;
import com.pingidentity.prov.saas.pingidforworkforce.request.RequestBuilder;
import com.pingidentity.prov.saas.pingidforworkforce.request.RequestParameters;
import com.pingidentity.prov.saas.pingidforworkforce.request.UserRequestBuilder;
import com.pingidentity.prov.saas.pingidforworkforce.request.UserRequestParameters;
import com.pingidentity.provisioner.sdk.SaasUserData;


public class CreateUserRequestBuilder 
extends UserRequestBuilder implements RequestBuilder {
	protected final Attributes attributes;

	public CreateUserRequestBuilder(Attributes attributes, String activationAttributeName, boolean activeState) {
		super(activationAttributeName, activeState);
		this.attributes = attributes;
	}

	public Request build(RequestParameters requestParameters) {
		Attributes createableAttributes = new Attributes();
		for (String userAttributeKey : this.attributes.keySet()) {
			BaseAttribute attribute = (BaseAttribute) this.attributes.get((Object) userAttributeKey);
			boolean createable = attribute.isCreatable();
			boolean required = attribute.isRequiredOnCreate();
			if (!required && !createable)
				continue;
			createableAttributes.add(attribute);
		}
		UserRequestParameters parameters = (UserRequestParameters) requestParameters;
		SaasUserData saasUserData = parameters.getSaasUserData();
		ConnectionFields connectionFields = parameters.getConnectionFields();
		ResourceAttributes userAttributes = this.buildAttributes(saasUserData, createableAttributes);
		User user = new User(userAttributes, new ResourceUniqueId(saasUserData.getSaasUserGuid()));
		return new CreateUsersRequest(connectionFields, user);
	}
}