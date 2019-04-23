package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.metadata;

import com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID.PingIDCustomFieldsBuilder;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.AttributeMetadata;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ConnectionProfile;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ProvisionerInformation;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.ConnectionFieldsBuilder;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.MetadataService;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.MetadataServiceFactory;
import com.pingidentity.provisioner.saas.pingidforworkforce.PingIDForWorkforcePluginMetadataService;

public class PingIDCustomMetadataServiceFactory implements MetadataServiceFactory {
	private final ProvisionerInformation info;
	private final ConnectionProfile connectionProfile;
	private final AttributeMetadata attributeMetadata;

	public PingIDCustomMetadataServiceFactory(ProvisionerInformation info, ConnectionProfile connectionProfile,
			AttributeMetadata attributeMetadata) {
		this.info = info;
		this.connectionProfile = connectionProfile;
		this.attributeMetadata = attributeMetadata;
	}

	public MetadataService make() {
		return new PingIDForWorkforcePluginMetadataService(this.info,
				(ConnectionFieldsBuilder) new PingIDCustomFieldsBuilder(), this.connectionProfile,
				this.attributeMetadata);
	}
}