package com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID;

import com.likeminds.custom.plugin.Provisioner.PingIDProvisioner.saas.definition.PingID.requests.CreateUserRequestBuilder;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.Attributes;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.BaseAttribute;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.BooleanAttribute;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.CapabilityType;
import com.pingidentity.io.cpl.pingidforworkforce.metadata.ReferenceAttribute;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.MetadataService;
import com.pingidentity.prov.saas.pingidforworkforce.metadata.MetadataServiceFactory;
import com.pingidentity.prov.saas.pingidforworkforce.request.CheckConnectionRequestBuilder;

import com.pingidentity.prov.saas.pingidforworkforce.request.DeleteUserRequestBuilder;
import com.pingidentity.prov.saas.pingidforworkforce.request.GetUserRequestBuilder;
import com.pingidentity.prov.saas.pingidforworkforce.request.RequestBuilder;
import com.pingidentity.prov.saas.pingidforworkforce.request.RequestBuilderFactory;
import com.pingidentity.prov.saas.pingidforworkforce.request.UpdateUserRequestBuilder;

public class PingIDCustomUserRequestFactory  implements RequestBuilderFactory
{
  protected final MetadataService metadataService;
  protected String activationAttributeName;
  protected Boolean activeState;
  
  public PingIDCustomUserRequestFactory(MetadataServiceFactory metadataServiceFactory)
  {
	  metadataService = metadataServiceFactory.make();
  }


  public PingIDCustomUserRequestFactory(MetadataServiceFactory metadataServiceFactory, String activationAttributeName, Boolean activeState)
  {
     metadataService = metadataServiceFactory.make();
     this.activationAttributeName = activationAttributeName;
     this.activeState = activeState;
  }


  public RequestBuilder make(CapabilityType capabilityType)
  {
     switch (capabilityType)
    {
    case GET_USERS: 
       return new GetUserRequestBuilder();

    case CREATE_USERS: 
       return new CreateUserRequestBuilder(metadataService.getUserAttributes(), 
        getActivationAttributeName(), getActiveState());

    case UPDATE_USERS: 
    	Attributes updateAttributes = metadataService.getUserAttributes();
    	
    	BooleanAttribute emailChanged = new BooleanAttribute("emailChanged");
    	emailChanged.setCreatable(Boolean.valueOf(false));
    	emailChanged.setDefaultedOnCreate(Boolean.valueOf(false));
    	emailChanged.setDerived(Boolean.valueOf(false));
    	emailChanged.setDisplayName("emailChanged");
    	emailChanged.setDistinguishingAttribute(Boolean.valueOf(false));
    	emailChanged.setMaxNumberOfValues(Integer.valueOf(1));
    	emailChanged.setMinNumberOfValues(Integer.valueOf(1));
    	emailChanged.setNillable(Boolean.valueOf(true));
    	emailChanged.setOrdered(Boolean.valueOf(false));
		emailChanged.setReferenceAttribute("emailChanged");
		emailChanged.setRequiredOnCreate(Boolean.valueOf(false));
		emailChanged.setRequiredOnUpdate(Boolean.valueOf(false));
		emailChanged.setSensitive(Boolean.valueOf(false));
		emailChanged.setUpdateable(Boolean.valueOf(true));
		emailChanged.setUnique(Boolean.valueOf(false));
		emailChanged.setDefaultValue(Boolean.valueOf(false));
		updateAttributes.add((BaseAttribute) emailChanged);
    	
       return new UpdateUserRequestBuilder(updateAttributes, 
        getActivationAttributeName(), getActiveState());
       
    case DELETE_USERS: 
      return new DeleteUserRequestBuilder();

    case CHECK_CONNECTION: 
     return new CheckConnectionRequestBuilder();

    }
     throw new IllegalArgumentException("Capability Type not supported");
  }
  public String getActivationAttributeName()
  {
    if (activationAttributeName == null) {
       return ReferenceAttribute.ACTIVE.getKey();
    }
     return activationAttributeName;
  }
  public boolean getActiveState()
  {
     if (activeState == null) {
      return true;
    }
    return activeState.booleanValue();
  }
}