package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.metadata;

import com.pingidentity.integrations.provisioner.metadata.pingidforworkforce.ProvisionerInformationBuilder;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ProvisionerInformation;

public class PingIDCustomProvisionerInformationBuilder implements ProvisionerInformationBuilder {
	public ProvisionerInformation build() {
		ProvisionerInformation provisonerInformation = new ProvisionerInformation();
		provisonerInformation.setKey("PingIDCustomProvisioner");
		provisonerInformation.setDisplayName("PingID Custom Connector");
		provisonerInformation.setVersion("1.0");
		provisonerInformation.setImageUrl(null);
		provisonerInformation
				.setIdentityProvider(false);
		provisonerInformation.setBaseURLRequired(false);
		provisonerInformation.setConnectionInformationRequired(false);
		return provisonerInformation;
	}
}