/*
 * **************************************************
 *  Copyright (C) 2019 Ping Identity Corporation
 *  All rights reserved.
 *
 *  The contents of this file are subject to the terms of the
 *  Ping Identity Corporation SDK Developer Guide.
 *
 *  Ping Identity Corporation
 *  1001 17th St Suite 100
 *  Denver, CO 80202
 *  303.468.2900
 *  http://www.pingidentity.com
 * ****************************************************
 */

package com.likeminds.custom.pingfed.adapter.idp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sourceid.saml20.adapter.AuthnAdapterException;
import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.AbstractSelectionFieldDescriptor.OptionValue;
import org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor;
import org.sourceid.saml20.adapter.gui.CheckBoxFieldDescriptor;
import org.sourceid.saml20.adapter.gui.LdapDatastoreFieldDescriptor;
import org.sourceid.saml20.adapter.gui.RadioGroupFieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.saml20.adapter.idp.authn.AuthnPolicy;
import org.sourceid.saml20.adapter.idp.authn.IdpAuthnAdapterDescriptor;
import org.sourceid.saml20.domain.LdapDataSource;
import org.sourceid.saml20.domain.mgmt.MgmtFactory;

import com.likeminds.custom.pingfed.adapter.idp.ldap.DefaultDirContextFactory;
import com.likeminds.custom.pingfed.adapter.idp.ldap.DefaultLdapDAO;
import com.likeminds.custom.pingfed.adapter.idp.ldap.LdapRecord;
import com.pingidentity.sdk.AuthnAdapterResponse;
import com.pingidentity.sdk.AuthnAdapterResponse.AUTHN_STATUS;
import com.pingidentity.sdk.IdpAuthenticationAdapterV2;
import com.pingidentity.sdk.locale.LanguagePackMessages;
import com.pingidentity.sdk.locale.LocaleUtil;
import com.pingidentity.sdk.template.TemplateRendererUtil;
import com.pingidentity.sdk.template.TemplateRendererUtilException;
import com.unboundid.ldap.sdk.Attribute;


public class ExportControlAdapter implements IdpAuthenticationAdapterV2 {

    private static final Logger log = LogManager.getLogger(ExportControlAdapter.class);
    
    private static final String USERNAME = "username";
    private static final String FIELD_FORM_TEMPLATE_NAME = "Export Control Template Name";
    private static final String FIELD_FORM_DECLINE_TEMPLATE_NAME = "Declined Template Name";
    private static final String FIELD_LDAP_DATA_SOURCE = "LDAP Data source";
    private static final String FIELD_LDAP_QUERY_DIRECTORY = "Query Directory";
    private static final String FIELD_LDAP_BASE_DOMAIN = "Base Domain";
    private static final String FIELD_LDAP_SEARCH_FILTER = "Filter";
    private static final String FIELD_LDAP_SEARCH_SCOPE = "LDAP Search Scope";
    private static final String FIELD_LDAP_ATTRIBUTE_NAME = "LDAP Attribute Name";
    private static final String FIELD_LDAP_ATTRIBUTE_VALUE = "Attribute value";
    
    
    private static final String DESC_LDAP_DATA_SOURCE = "The LDAP data source used for looking up the Attribute";
    private static final String DESC_LDAP_QUERY_DIRECTORY = "Query directory for every time during Authentication";
    private static final String DESC_LDAP_BASE_DOMAIN = "The base domain for attribute retrieval.";
    private static final String DESC_LDAP_SEARCH_FILTER = "You may use ${username} as part of the query. Example (for Ping Directory): uid=${username}.)";
    private static final String DESC_LDAP_SEARCH_SCOPE = "OBJECT_SCOPE: limits the search to the base object. ONE_LEVEL_SCOPE: searches the immediate children of a base object, but excludes the base object itself. Default: SUBTREE_SCOPE: searches all child objects as well as the base object";
	private static final String DESC_LDAP_ATTRIBUTE_NAME ="Attribute name that controls rendering of Privacy Policy screen";
	private static final String DESC_LDAP_ATTRIBUTE_VALUE = "Attribute value to be considered as accepted";
	private static final String DESC_DECLINE_TEMPLATE_NAME = "HTML template (in <pf_home>/server/default/conf/template) to render on decline. The default value is attribute.decline.form.template.html.";
    
	
	private static final String DEFAULT_LDAP_SEARCH_SCOPE = "SUBTREE";
	private static final boolean DEFAULT_LDAP_QUERY_DIRECTORY = true;
	private static final String DEFAULT_LDAP_ATTRIBUTE_VALUE = "true";
	private static final String DEFAULT_LDAP_ATTRIBUTE_NAME = "privacypolicyflag";
    
	private LdapDataSource LDAP_DATA_SOURCE;
	private boolean ATTR_LDAP_QUERY_DIRECTORY;
	private String ATTR_LDAP_BASE_DOMAIN;
	private String ATTR_LDAP_SEARCH_FILTER;
	private int ATTR_LDAP_SEARCH_SCOPE;
	private String ATTR_LDAP_ATTRIBUTE_NAME;
	private String ATTR_LDAP_ATTRIBUTE_VALUE;
	
	private static ArrayList<OptionValue> listScopes() {
		ArrayList<OptionValue> searchScopeList = new ArrayList<OptionValue>();
		searchScopeList.add(new OptionValue("OBJECT", "0"));
		searchScopeList.add(new OptionValue("ONE_LEVEL", "1"));
		searchScopeList.add(new OptionValue("SUBTREE", "2"));
		return searchScopeList;
	}
	
    // Fields
    private IdpAuthnAdapterDescriptor descriptor = null;
    private Configuration configuration = null;
    private DefaultLdapDAO ldapDAO = null;

    @Override
    public void configure(Configuration config) 
    {
        this.configuration = config;
        ATTR_LDAP_QUERY_DIRECTORY= configuration.getAdvancedFields().getBooleanFieldValue(FIELD_LDAP_QUERY_DIRECTORY);
		if(ATTR_LDAP_QUERY_DIRECTORY) {
			String ATTR_LDAP_DATA_SOURCE = configuration.getAdvancedFields().getFieldValue(FIELD_LDAP_DATA_SOURCE);
			LDAP_DATA_SOURCE = MgmtFactory.getDataSourceManager().getLdapDataSource(ATTR_LDAP_DATA_SOURCE);
			ATTR_LDAP_QUERY_DIRECTORY = configuration.getAdvancedFields().getBooleanFieldValue(FIELD_LDAP_QUERY_DIRECTORY);
			ATTR_LDAP_BASE_DOMAIN = configuration.getAdvancedFields().getFieldValue(FIELD_LDAP_BASE_DOMAIN);
			ATTR_LDAP_SEARCH_FILTER =configuration.getAdvancedFields().getFieldValue(FIELD_LDAP_SEARCH_FILTER);
			ATTR_LDAP_SEARCH_SCOPE =  Integer.valueOf(configuration.getAdvancedFields().getFieldValue(FIELD_LDAP_SEARCH_SCOPE));
			ATTR_LDAP_ATTRIBUTE_NAME = configuration.getAdvancedFields().getFieldValue(FIELD_LDAP_ATTRIBUTE_NAME);
			ATTR_LDAP_ATTRIBUTE_VALUE = configuration.getAdvancedFields().getFieldValue(FIELD_LDAP_ATTRIBUTE_VALUE);
			ldapDAO = new DefaultLdapDAO(new DefaultDirContextFactory());
		}

    }

    @Override
    public IdpAuthnAdapterDescriptor getAdapterDescriptor() 
    {
    	 // Create input text field to represent name of velocity html template file
        TextFieldDescriptor formTemplateConfig = new TextFieldDescriptor(FIELD_FORM_TEMPLATE_NAME, "HTML template (in <pf_home>/server/default/conf/template) to render for form submission. The default value is attribute.form.template.html.");
        formTemplateConfig.setDefaultValue("attribute.form.template.html");
        formTemplateConfig.addValidator(new RequiredFieldValidator());
        
        TextFieldDescriptor formDeclineTemplateConfig = new TextFieldDescriptor(FIELD_FORM_DECLINE_TEMPLATE_NAME,DESC_DECLINE_TEMPLATE_NAME);
        formDeclineTemplateConfig.setDefaultValue("attribute.decline.form.template.html");
        formDeclineTemplateConfig.addValidator(new RequiredFieldValidator());
        
        // Create an adapter GUI descriptor
        AdapterConfigurationGuiDescriptor configurationGuiDescriptor = new AdapterConfigurationGuiDescriptor("Export Control Adapter");
        configurationGuiDescriptor.addField(formTemplateConfig);
        configurationGuiDescriptor.addField(formDeclineTemplateConfig);
        
        CheckBoxFieldDescriptor queryDirectoryField = new CheckBoxFieldDescriptor(
				FIELD_LDAP_QUERY_DIRECTORY, DESC_LDAP_QUERY_DIRECTORY);
		queryDirectoryField.setDefaultValue(DEFAULT_LDAP_QUERY_DIRECTORY);
		configurationGuiDescriptor.addAdvancedField(queryDirectoryField);
		
		LdapDatastoreFieldDescriptor ldapDatastoreFieldDescriptor = new LdapDatastoreFieldDescriptor(
				FIELD_LDAP_DATA_SOURCE,	DESC_LDAP_DATA_SOURCE);
		configurationGuiDescriptor.addAdvancedField(ldapDatastoreFieldDescriptor);
		
		TextFieldDescriptor baseDomainField = new TextFieldDescriptor(FIELD_LDAP_BASE_DOMAIN,DESC_LDAP_BASE_DOMAIN);
		configurationGuiDescriptor.addAdvancedField(baseDomainField);
		
		TextFieldDescriptor ldapFilerField = new TextFieldDescriptor(FIELD_LDAP_SEARCH_FILTER,DESC_LDAP_SEARCH_FILTER);
		configurationGuiDescriptor.addAdvancedField(ldapFilerField);
		
		RadioGroupFieldDescriptor searchScopeDescriptor = new RadioGroupFieldDescriptor(FIELD_LDAP_SEARCH_SCOPE, 
				DESC_LDAP_SEARCH_SCOPE,listScopes());
		searchScopeDescriptor.setDefaultValue(DEFAULT_LDAP_SEARCH_SCOPE);
		configurationGuiDescriptor.addAdvancedField(searchScopeDescriptor);
		
        TextFieldDescriptor flagAttribute = new TextFieldDescriptor(FIELD_LDAP_ATTRIBUTE_NAME,DESC_LDAP_ATTRIBUTE_NAME);
        flagAttribute.setDefaultValue(DEFAULT_LDAP_ATTRIBUTE_NAME);
        configurationGuiDescriptor.addAdvancedField(flagAttribute);
        
        TextFieldDescriptor flagValue = new TextFieldDescriptor(FIELD_LDAP_ATTRIBUTE_VALUE,DESC_LDAP_ATTRIBUTE_VALUE);
        flagValue.setDefaultValue(DEFAULT_LDAP_ATTRIBUTE_VALUE);
        configurationGuiDescriptor.addAdvancedField(flagValue);
        
        // Create an Idp adapter descriptor 
        Set<String> attributeContract = new HashSet<String>();
        attributeContract.add(USERNAME);
        this.descriptor = new IdpAuthnAdapterDescriptor(this, "Export Control Adapter 1.1", attributeContract, true, configurationGuiDescriptor, false);
        return this.descriptor;
    }

    
    @Override
    public AuthnAdapterResponse lookupAuthN(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> inParameters) throws AuthnAdapterException, IOException 
    {
        AuthnAdapterResponse authnAdapterResponse = new AuthnAdapterResponse();
        String username = getUsernameFromLastAdapter(inParameters);
        if(username == null) {
        	 authnAdapterResponse.setErrorMessage("No Username available to adapter");
             authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
             return authnAdapterResponse;
        }
        
        // Handle Submit if clicked
        if (StringUtils.isNotBlank(req.getParameter("pf.submit")))
        {
        	Map<String, Object> attributeMap = new HashMap<String, Object>();
            attributeMap.put(USERNAME, username);
            
            if(ATTR_LDAP_QUERY_DIRECTORY) {
            	try {
					String userDN = getUserDNfromAdapter(inParameters);
					javax.naming.directory.Attributes privacyAttribute = new javax.naming.directory.BasicAttributes();
					privacyAttribute.put(ATTR_LDAP_ATTRIBUTE_NAME, ATTR_LDAP_ATTRIBUTE_VALUE);
					ldapDAO.update(LDAP_DATA_SOURCE, userDN, privacyAttribute);
				} catch (NamingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.debug("Failed user lookup, unable to determine if user has the attribute");
				}
            }

            authnAdapterResponse.setAttributeMap(attributeMap); 
            authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.SUCCESS);
            return authnAdapterResponse;
        }
        
        // Handle Decline if decline
        if (StringUtils.isNotBlank(req.getParameter("pf.decline")))
        {
        	renderDeclineForm(req, resp, inParameters);
            authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.IN_PROGRESS);
            return authnAdapterResponse;
        }

        
        // Handle Cancel if clicked
        if (StringUtils.isNotBlank(req.getParameter("pf.cancel")))
        {
            authnAdapterResponse.setErrorMessage("User clicked Cancel");
            authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
            return authnAdapterResponse;
        }
        
        // Handle Click return
        if (StringUtils.isNotBlank(req.getParameter("pf.return"))) {
        	renderForm(req, resp, inParameters);
            authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.IN_PROGRESS);
            return authnAdapterResponse;
        }

        
        if(ATTR_LDAP_QUERY_DIRECTORY) {
        	try {
        		ATTR_LDAP_SEARCH_FILTER = "(&("+ATTR_LDAP_ATTRIBUTE_NAME+"="+ATTR_LDAP_ATTRIBUTE_VALUE+")"+"("+ATTR_LDAP_SEARCH_FILTER.replace("${username}", username)+"))";
				List<LdapRecord> results = ldapDAO.search(LDAP_DATA_SOURCE, ATTR_LDAP_SEARCH_FILTER, ATTR_LDAP_BASE_DOMAIN, ATTR_LDAP_SEARCH_SCOPE, 1, ATTR_LDAP_ATTRIBUTE_NAME);
				if(results != null && !results.isEmpty()) {
					Map<String, Object> attributeMap = new HashMap<String, Object>();
		            attributeMap.put(USERNAME, username);
					authnAdapterResponse.setAttributeMap(attributeMap); 
		            authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.SUCCESS);
		            return authnAdapterResponse;
				}
        	} catch (NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.debug("Failed user lookup, Check parameters on Config");
			}
        }
                
       
        // Render form
        renderForm(req, resp, inParameters);
        authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.IN_PROGRESS);
        return authnAdapterResponse;
    }
    
    
    private void renderForm(HttpServletRequest req, HttpServletResponse resp,  Map<String, Object> inParameters) throws AuthnAdapterException
    {
        Map<String, Object> params = new HashMap<String, Object>();        
        params.put("resumePath", inParameters.get(IN_PARAMETER_NAME_RESUME_PATH));
        params.put("submit", "pf.submit");
        params.put("decline", "pf.decline");
        params.put("username", getUsernameFromLastAdapter(inParameters));
        
        // Load attribute-form-template.properties file and store it in the map
        Locale userLocale = LocaleUtil.getUserLocale(req);
        LanguagePackMessages lpm = new LanguagePackMessages("attribute-form-template", userLocale);
        params.put("pluginTemplateMessages", lpm);
        
        try
        {        
            TemplateRendererUtil.render(req, resp, configuration.getFieldValue(FIELD_FORM_TEMPLATE_NAME), params);
        }
        catch (TemplateRendererUtilException e)
        {
            throw new AuthnAdapterException(e);
        }
    }
    
    private void renderDeclineForm(HttpServletRequest req, HttpServletResponse resp,  Map<String, Object> inParameters) throws AuthnAdapterException
    {
        Map<String, Object> params = new HashMap<String, Object>();        
        params.put("resumePath", inParameters.get(IN_PARAMETER_NAME_RESUME_PATH));
        params.put("return", "pf.return");
        params.put("cancel", "pf.cancel");
        params.put("username", getUsernameFromLastAdapter(inParameters));
        
        // Load attribute-form-template.properties file and store it in the map
        Locale userLocale = LocaleUtil.getUserLocale(req);
        LanguagePackMessages lpm = new LanguagePackMessages("attribute-form-template", userLocale);
        params.put("pluginTemplateMessages", lpm);
        
        try
        {        
            TemplateRendererUtil.render(req, resp, configuration.getFieldValue(FIELD_FORM_DECLINE_TEMPLATE_NAME), params);
        }
        catch (TemplateRendererUtilException e)
        {
            throw new AuthnAdapterException(e);
        }
    }
    
	private String getUsernameFromLastAdapter(Map<String, Object> inParameters) {
		
		if(inParameters.containsKey(IN_PARAMETER_NAME_USERID)) {
			return  (String) inParameters.get(IN_PARAMETER_NAME_USERID);
		}
		throw new RuntimeException("No Username available from adapter");
	}
	
	private String getUserDNfromAdapter(Map<String, Object> inParameters) {
		HashMap<String,AttributeValue> UsernameMap = (HashMap) inParameters.get(IN_PARAMETER_NAME_CHAINED_ATTRIBUTES);
		if(UsernameMap.containsKey("DN")) {
			return  UsernameMap.get("DN").getValue();
		}
		throw new RuntimeException("No DN available from adapter");
	}
    
   
    @Override
    public boolean logoutAuthN(Map authnIdentifiers, HttpServletRequest req, HttpServletResponse resp, String resumePath) throws AuthnAdapterException, IOException 
    {
        return true;
    }
    
   
    @Override
    public Map<String, Object> getAdapterInfo() 
    {
        return null;
    }

   
    @Override 
    @Deprecated
    public Map lookupAuthN(HttpServletRequest req, HttpServletResponse resp, String partnerSpEntityId, AuthnPolicy authnPolicy, String resumePath) throws AuthnAdapterException, IOException 
    {
        throw new UnsupportedOperationException();
    }
}
