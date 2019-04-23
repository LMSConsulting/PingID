package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.metadata;

import com.pingidentity.integrations.provisioner.metadata.capability.pingidforworkforce.CapabilityBuilder;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.Capabilities;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.Capability;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.CapabilityType;

public class PingIDCustomCapabilityBuilder implements CapabilityBuilder {
	public Capabilities build() {
		Capabilities capabilities = new Capabilities();
		capabilities.add(new Capability(CapabilityType.CHECK_CONNECTION));
		capabilities.add(new Capability(CapabilityType.GET_INFO));
		capabilities.add(new Capability(CapabilityType.GET_CONNECTION_PROFILES));
		capabilities.add(new Capability(CapabilityType.GET_ATTRIBUTES));
		capabilities.add(new Capability(CapabilityType.GET_USERS));
		capabilities.add(new Capability(CapabilityType.CREATE_USERS));
		capabilities.add(new Capability(CapabilityType.UPDATE_USERS));
		capabilities.add(new Capability(CapabilityType.DELETE_USERS));
		return capabilities;
	}
}