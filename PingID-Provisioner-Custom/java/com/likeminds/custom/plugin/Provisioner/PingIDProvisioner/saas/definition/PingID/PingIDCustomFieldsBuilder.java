package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID;

import java.util.ArrayList;
import java.util.List;

import org.sourceid.saml20.adapter.gui.CheckBoxFieldDescriptor;
import org.sourceid.saml20.adapter.gui.FieldDescriptor;
import org.sourceid.saml20.adapter.gui.SelectFieldDescriptor;

import com.pingidentity.integrations.provisioner.pingidforworkforce.ConnectionFieldKey;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ConnectionAttributes;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.CPLv3ConnectionFieldsBuilder;

public class PingIDCustomFieldsBuilder extends CPLv3ConnectionFieldsBuilder {
	private static final String PROVISIONING_OPTIONS = "Provisioning Options";
	private static final String[] REMOVE_ACTION_VALUES = new String[]{"Disable", "Delete"};

	public List<FieldDescriptor> build(ConnectionAttributes connectionAttributes) {
		List<FieldDescriptor> descriptors = super.build(connectionAttributes);
		ArrayList<FieldDescriptor> fieldsToRemove = new ArrayList<FieldDescriptor>();
		for (FieldDescriptor field : descriptors) {
			if (!this.toRemove(field.getName()))
				continue;
			fieldsToRemove.add(field);
		}
		descriptors.removeAll(fieldsToRemove);
		descriptors.add(this.addLabel(PROVISIONING_OPTIONS, PROVISIONING_OPTIONS));
		descriptors.add(this.addCheckbox(ConnectionFieldKey.CREATE_NEW_USERS.getKey(),
				ConnectionFieldKey.CREATE_NEW_USERS.getDesc()));
		descriptors.add(this.addCheckbox(ConnectionFieldKey.UPDATE_NEW_USERS.getKey(),
				ConnectionFieldKey.UPDATE_NEW_USERS.getDesc()));
		descriptors.add(this.addCheckbox(ConnectionFieldKey.DISABLE_NEW_USERS.getKey(),
				ConnectionFieldKey.DISABLE_NEW_USERS.getDesc()));
		descriptors.add(this.addSelectField(ConnectionFieldKey.REMOVE_ACTION.getKey(),
				ConnectionFieldKey.REMOVE_ACTION.getDesc(), REMOVE_ACTION_VALUES));
		return descriptors;
	}

	private FieldDescriptor addLabel(String key, String desc) {
		ProvisioningTargetLabelFieldDescriptor field = new ProvisioningTargetLabelFieldDescriptor(key, desc);
		return field;
	}

	private FieldDescriptor addCheckbox(String key, String desc) {
		CheckBoxFieldDescriptor field = new CheckBoxFieldDescriptor(key, desc);
		field.setDefaultValue(Boolean.TRUE.toString());
		return field;
	}

	private FieldDescriptor addSelectField(String key, String desc, String[] values) {
		SelectFieldDescriptor field = new SelectFieldDescriptor(key, desc, values);
		return field;
	}

	private boolean toRemove(String name) {
		return name.equals(ConnectionFieldKey.UPDATE_NEW_USERS.getKey())
				|| name.equals(ConnectionFieldKey.DISABLE_NEW_USERS.getKey())
				|| name.equals(ConnectionFieldKey.REMOVE_ACTION.getKey());
	}

	public class ProvisioningTargetLabelFieldDescriptor extends FieldDescriptor {
		private static final long serialVersionUID = 6029695094471889487L;

		public ProvisioningTargetLabelFieldDescriptor(String name, String description) {
			super(name, description);
		}
	}

}