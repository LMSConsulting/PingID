import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.unboundid.directory.sdk.common.types.Entry;
import com.unboundid.directory.sdk.common.types.OperationContext;
import com.unboundid.directory.sdk.ds.config.VirtualAttributeProviderConfig;
import com.unboundid.directory.sdk.ds.scripting.
            ScriptedVirtualAttributeProvider;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a scripted virtual attribute provider
 * that will generate a virtual attribute whose value will be the reverse of
 * another attribute in the same entry.  It takes a single configuration
 * argument:
 * <UL>
 *   <LI>source-attribute -- The name of the attribute in the entry whose value
 *       will be reversed to obtain the values for the virtual attribute.</LI>
 * </UL>
 */
public final class accountDisabledProvider
       extends ScriptedVirtualAttributeProvider
{
  /**
   * The name of the argument that will be used for the argument used to specify
   * the attribute whose values should be reversed.
   */
  private static final String ARG_NAME_ATTR = "login-time-attribute";
  private static final String ARG_NUMBER_OF_DAYS_ATTR = "days-before-disabled";

  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;

  // The source attribute from which to obtain the data for the virtual
  // attribute.
  private volatile String loginTimestampAttribute;
  private volatile int daysBeforeInactive;


  /**
   * Creates a new instance of this virtual attribute provider.  All virtual
   * attribute provider implementations must include a default constructor, but
   * any initialization should generally be done in the
   * {@code initializeVirtualAttributeProvider} method.
   */
  public accountDisabledProvider()
  {
    // No implementation required.
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this virtual attribute provider.  The argument parser
   * may also be updated to define relationships between arguments (e.g., to
   * specify required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this virtual attribute
   *                 provider.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the source attribute name.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_ATTR;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{attr}";
    String    description     = "The name of the attribute which holds the login time";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
    
    shortIdentifier = null;
    longIdentifier  = ARG_NUMBER_OF_DAYS_ATTR;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{days}";
    description     = "Number of Days before account should be considered in Active";
    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
            required, maxOccurrences, placeholder, description));

  }



  /**
   * Initializes this virtual attribute provider.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this virtual attribute
   *                        provider.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this virtual attribute
   *                        provider.
   *
   * @throws  LDAPException  If a problem occurs while initializing this virtual
   *                         attribute provider.
   */
  @Override()
  public void initializeVirtualAttributeProvider(
                   final DirectoryServerContext serverContext,
                   final VirtualAttributeProviderConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    this.serverContext = serverContext;

    // Get the source attribute name.
    final StringArgument arg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_ATTR);
    loginTimestampAttribute = arg.getValue();
    
    final StringArgument days =
            (StringArgument) parser.getNamedArgument(ARG_NUMBER_OF_DAYS_ATTR);
       daysBeforeInactive = Integer.parseInt(days.getValue());
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this virtual
   *                              attribute provider.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(
                      final VirtualAttributeProviderConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    // The argument parser will handle all of the necessary validation, so
    // we don't need to do anything here.
    return true;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this virtual
   *                               attribute provider.
   * @param  parser                The argument parser which has been
   *                               initialized with the new configuration.
   * @param  adminActionsRequired  A list that can be updated with information
   *                               about any administrative actions that may be
   *                               required before one or more of the
   *                               configuration changes will be applied.
   * @param  messages              A list that can be updated with information
   *                               about the result of applying the new
   *                               configuration.
   *
   * @return  A result code that provides information about the result of
   *          attempting to apply the configuration change.
   */
  @Override()
  public ResultCode applyConfiguration(
                         final VirtualAttributeProviderConfig config,
                         final ArgumentParser parser,
                         final List<String> adminActionsRequired,
                         final List<String> messages)
  {
    // Get the new source attribute name.
	  final StringArgument arg =
		         (StringArgument) parser.getNamedArgument(ARG_NAME_ATTR);
	  loginTimestampAttribute = arg.getValue();
	  final StringArgument days =
		            (StringArgument) parser.getNamedArgument(ARG_NUMBER_OF_DAYS_ATTR);
	  daysBeforeInactive = Integer.parseInt(days.getValue());
    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this virtual attribute
   * provider is to be taken out of service.
   */
  @Override()
  public void finalizeVirtualAttributeProvider()
  {
    // No implementation required.
  }



  /**
   * Indicates whether the server may cache values generated by this virtual
   * attribute provider for reuse against the same entry in the course of
   * processing the same operation.
   *
   * @return  {@code true} if the server may cache the value generated by this
   *          virtual attribute provider for reuse with the same entry in the
   *          same operation, or {@code false} if not.
   */
  @Override()
  public boolean mayCacheInOperation()
  {
    // The values of this virtual attribute are safe to cache.
    return false;
  }



  /**
   * Indicates whether this virtual attribute provider may generate attributes
   * with multiple values.
   *
   * @return  {@code true} if this virtual attribute provider may generate
   *          attributes with multiple values, or {@code false} if it will only
   *          generate single-valued attributes.
   */
  @Override()
  public boolean isMultiValued()
  {
    // If the source attribute is multi-valued, then the virtual attribute will
    // also be multi-valued.
    return false;
  }



  /**
   * Generates an attribute for inclusion in the provided entry.
   *
   * @param  operationContext  The operation context for the operation in
   *                           progress, if any.  It may be {@code null} if no
   *                           operation is available.
   * @param  entry             The entry for which the attribute is to be
   *                           generated.
   * @param  attributeName     The name of the attribute to be generated.
   *
   * @return  The generated attribute, or {@code null} if no attribute should be
   *          generated.
   */
  @Override()
  public Attribute generateAttribute(final OperationContext operationContext,
                                     final Entry entry,
                                     final String attributeName)
  {
	Attribute va = new Attribute(attributeName, "ACTIVATED");
	final String createTimestamp = "createTimestamp";
	final String disabledFlag = "ds-pwp-account-disabled";
	if(entry.hasAttribute(disabledFlag) && entry.hasAttributeValue(disabledFlag, "true")) {
		va = new Attribute(attributeName,"DEACTIVATED");
		return va;
	} else if (!entry.hasAttribute(loginTimestampAttribute))
    {
      // The source attribute doesn't exist, so we can't generate a virtual
      // attribute.
      if (serverContext.debugEnabled())
      {
        serverContext.debugInfo("Returning null because attribute " +
        		loginTimestampAttribute + " does not exist in entry " + entry.getDN());
      }
      final List<Attribute> createTime = entry.getAttribute(createTimestamp);
      if(isDatePast(createTime,daysBeforeInactive))
    	  va = new Attribute(attributeName,"DEACTIVATED");
    }else {
    	if(isDatePast(entry.getAttribute(loginTimestampAttribute),daysBeforeInactive))
    		va = new Attribute(attributeName,"DEACTIVATED");
    }
    
    return va;
  }
  
  private boolean isDatePast(List<Attribute> date,int days) {
	  String stringDate = date.get(0).getValue();
	  Date localDate = null;
		 if (date != null && !date.isEmpty()) {
			  DateFormat formatter;
			  formatter = new SimpleDateFormat("yyyyMMddHHmmss.S'Z'");
			  try {
				localDate = formatter.parse(stringDate);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				formatter = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'");
				try {
					localDate = formatter.parse(stringDate);
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					formatter = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
					try {
						localDate = formatter.parse(stringDate);
					} catch (ParseException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					
				}
			}
		 }
    if(accountDisabledProvider.add(localDate, days).before(new Date()))
    	return true;
	  return false;
	  
  }
  
  private static final Date add(Date date, int days)
  {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.add(Calendar.DATE, 1*days); //minus number would decrement the days
      return cal.getTime();
  }
}