package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID;


import com.pingidentity.integrations.provisioner.pingidforworkforce.SaasFactory;
import com.pingidentity.saas.definition.pingidforworkforce.SaasHttpService;
import com.pingidentity.saas.definition.pingidforworkforce.SaasUserApi;
import com.pingidentity.saas.definition.pingidforworkforce.SaasUserBuilder;
import com.pingidentity.saas.wrapper.pingidforworkforce.http_service.HttpClientFactory;
import com.pingidentity.saas.wrapper.pingidforworkforce.http_service.PingIDForWorkforceHttpClientFactory;
import com.pingidentity.saas.wrapper.pingidforworkforce.http_service.PingIDForWorkforceHttpService;
import com.pingidentity.saas.wrapper.pingidforworkforce.http_service.PingIDForWorkforceValidator;
import com.pingidentity.saas.wrapper.pingidforworkforce.http_service.Validator;

import com.pingidentity.saas.wrapper.pingidforworkforce.resource_builder.PingIDForWorkforceResourceBuilder;
import com.pingidentity.saas.wrapper.pingidforworkforce.resource_builder.deserialization.Deserializer;
import com.pingidentity.saas.wrapper.pingidforworkforce.resource_builder.deserialization.PingIDForWorkforceDeserializer;

public class PingIDCustomWorkerFactory implements SaasFactory {
	public SaasUserApi make() {
		PingIDCustomRequestBuilder requestBuilder = new PingIDCustomRequestBuilder();
		PingIDForWorkforceValidator validator = new PingIDForWorkforceValidator();
		PingIDForWorkforceHttpService httpService = new PingIDForWorkforceHttpService((Validator) validator,
				(HttpClientFactory) new PingIDForWorkforceHttpClientFactory());
		PingIDForWorkforceDeserializer deserializer = new PingIDForWorkforceDeserializer();
		PingIDForWorkforceResourceBuilder userBuilder = new PingIDForWorkforceResourceBuilder(
				(Deserializer) deserializer);
		PingIDCustomSaasUserAPI saasUserApi = new PingIDCustomSaasUserAPI(requestBuilder,
				(SaasHttpService) httpService, (SaasUserBuilder) userBuilder);
		return saasUserApi;
	}
}