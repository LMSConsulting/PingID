package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.metadata;

import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ResourceUnknownStateException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceException;
import com.pingidentity.io.cpl.pingidforworkforce.exception.ServiceUnknownStateException;
import com.pingidentity.io.cpl.pingidforworkforce.request.ConnectionFields;
import com.pingidentity.io.cpl.pingidforworkforce.resource.ResourceUniqueId;
import com.pingidentity.io.cpl.pingidforworkforce.resource.User;
import org.shaded.pingidforworkforce.apache.client.methods.HttpUriRequest;

public interface UserRequestBuilder {
	public HttpUriRequest buildGetUserRequest(ConnectionFields var1, ResourceUniqueId var2)
			throws ResourceException, ServiceException;

	public HttpUriRequest buildUpdateUserRequest(ConnectionFields var1, User var2)
			throws ResourceException, ServiceException;
	
	public HttpUriRequest buildCreateUserRequest(ConnectionFields var1, ResourceUniqueId var2)
			throws ResourceException, ServiceUnknownStateException;
	
	public HttpUriRequest buildDeleteUserRequest(ConnectionFields var1, ResourceUniqueId var2)
			throws ResourceException, ServiceUnknownStateException;

	public HttpUriRequest buildActivateOrSuspendUserRequest(ConnectionFields var1, User var2)
			throws ResourceUnknownStateException, ServiceUnknownStateException;
}