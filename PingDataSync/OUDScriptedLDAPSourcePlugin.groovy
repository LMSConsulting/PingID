
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.unboundid.directory.sdk.sync.config.LDAPSyncSourcePluginConfig;
import com.unboundid.directory.sdk.sync.scripting.ScriptedLDAPSyncSourcePlugin;
import com.unboundid.directory.sdk.sync.types.PostStepResult;
import com.unboundid.directory.sdk.sync.types.SyncOperation;
import com.unboundid.directory.sdk.sync.types.SyncServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;

/*
 * This is simple plugin to produced two Attributes names pwdChangedTime and 
 * lastsuccessfullogin . Both the values are evaluated based on the criteria
 * for each attribute.
 * pwdChangedTime is calculated based on obpasswordExpiryDate -180 days or 
 * creationTimestamp 
 * 
 * lastSuccessfullogin is calculated based on the larger of oblastsuccessfullogin
 * or  entrustIGLastAuthDate or null (if the user has never loggedin)
 * 
 * 
 */
public class OUDScriptedLDAPSourcePlugin
     extends ScriptedLDAPSyncSourcePlugin
{

  // The server context for the server in which this extension is running.
  private SyncServerContext serverContext;
  private static final int ExpiryDateDelta = 180; // Number of days to convert from expiry date to creation date.

  @Override()
  public void defineConfigArguments(final ArgumentParser parser) throws ArgumentException
  {
   // No Config parameters for this Plugin. Operation will be hard Coded.
  }



  /**
   * Initializes this LDAP sync source plugin.  This method will be called
   * before any other methods in the class.
   *
   * @param  serverContext  A handle to the server context for the
   *                        Data Sync Server in which this extension is
   *                        running.  Extensions should typically store this
   *                        in a class member.
   * @param  config         The general configuration for this proxy
   *                        transformation.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this LDAP sync source
   *                        plugin.
   *
   * @throws  LDAPException  If a problem occurs while initializing this ldap
   *                         sync source plugin.
   */
  @Override
  public void initializeLDAPSyncSourcePlugin(
       final SyncServerContext serverContext,
       final LDAPSyncSourcePluginConfig config,
       final ArgumentParser parser)
       throws LDAPException
  {
	this.serverContext = serverContext;
  }



  /**
    * Indicates whether the configuration contained in the provided argument
    * parser represents a valid configuration for this extension.
    *
    * @param  config               The general configuration for this LDAP sync
    *                              source plugin.
    * @param  parser               The argument parser which has been
    *                              initialized with the proposed configuration.
    * @param  unacceptableReasons  A list that can be updated with reasons that
    *                              the proposed configuration is not acceptable.
    *
    * @return  {@code true} if the proposed configuration is acceptable, or
    *          {@code false} if not.
    */
  @Override
  public boolean isConfigurationAcceptable(
       final LDAPSyncSourcePluginConfig config,
       final ArgumentParser parser,
       final List<String> unacceptableReasons)
  {
    // The built-in ArgumentParser validation does all of the validation that
    // we need.
    return true;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this LDAP sync
   *                               source.
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
       final LDAPSyncSourcePluginConfig config,
       final ArgumentParser parser,
       final List<String> adminActionsRequired,
       final List<String> messages)
  {
    return ResultCode.SUCCESS;
  }



  
 private static final Date parseOracleDate(String date) {
	 Date localDate = null;
	 if (date != null && !date.isEmpty()) {
		  DateFormat formatter;
		  formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		  try {
			localDate = formatter.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			//Doing Nothing Since 11g Date format is above and ignoring old ones
		}
	 }
	 return localDate;
 }
 
 private static final String convertDatetoGeneralized(Date date) {
	 String StringDate = null;
	 DateFormat formatter;
	 formatter = new SimpleDateFormat("yyyyMMddHHmmss.0'Z'");
	 StringDate = formatter.format(date);
	 return StringDate;
 }
 
 /**
  * This method is called after fetching a source entry.  A
  * connection to the source server is provided.  This method is
  * overridden by plugins that need to manipulate the search results that
  * are returned to the Sync Pipe.  This can include filtering out certain
  * entries, remove information from the entries, or adding additional
  * information, possibly by doing a followup LDAP search.
  *
  * @param  sourceConnection       A connection to the source server.
  * @param  fetchedEntryRef        A reference to the entry that was fetched.
  *                                This entry can be edited in place, or the
  *                                reference can be changed to point to a
  *                                different entry that the plugin constructs.
  * @param  operation              The synchronization operation for this
  *                                change.
  *
  * @return  The result of the plugin processing.
  *
  * @throws  LDAPException  In general subclasses should not catch
  *                         LDAPExceptions that are thrown when
  *                         using the LDAPInterface unless there
  *                         are specific exceptions that are
  *                         expected.  The Data Sync Server
  *                         will handle LDAPExceptions in an
  *                         appropriate way based on the specific
  *                         cause of the exception.  For example,
  *                         some errors will result in the
  *                         SyncOperation being retried, and others
  *                         will trigger fail over to a different
  *                         server.  Plugins should only throw
  *                         LDAPException for errors related to
  *                         communication with the LDAP server.
  *                         Use the return code to indicate other
  *                         types of errors, which might require
  *                         retry.
  *                         
  **/
  @Override
  public PostStepResult postFetch(final LDAPInterface sourceConnection,
                                  final AtomicReference<Entry> fetchedEntryRef,
                                  final SyncOperation operation)
       throws LDAPException
  {
	  Entry entry = fetchedEntryRef.get();
	  if(entry == null) {
		  return PostStepResult.CONTINUE;
	  }
	  
	  Date expiryDate = parseOracleDate(entry.getAttributeValue("obpasswordExpiryDate"));
	  Date creationDate = entry.getAttributeValueAsDate("createTimestamp");
	  Date oblastlogin = parseOracleDate(entry.getAttributeValue("oblastsuccessfullogin"));
	  Date entrustlastlogin = entry.getAttributeValueAsDate("entrustIGLastAuthDate");
	  String pwdChangetime = buildpwdChangedTime(expiryDate, creationDate);
	  if(pwdChangetime != null)
	  entry.setAttribute(new Attribute("passwdChangedTime", pwdChangetime));
	  String lastlogin = buildLastLoginTime(entrustlastlogin,oblastlogin);
	  if(lastlogin != null)
	  entry.setAttribute(new Attribute("lastLoginTime",lastlogin));
	  return PostStepResult.CONTINUE;
  }
  
  private static final Date subDays(Date date, int days)
  {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.add(Calendar.DATE, -1*days); //minus number would decrement the days
      return cal.getTime();
  }
  
  private static String buildpwdChangedTime(Date obpasswordExpiryDate, Date createTimestamp) {
	  if(obpasswordExpiryDate == null) { //Password never changed or set
	  	  return convertDatetoGeneralized(createTimestamp);
	  }else {
		  return convertDatetoGeneralized(subDays(obpasswordExpiryDate,ExpiryDateDelta));
	  }
  }
  
  private static String buildLastLoginTime(Date entrustLoginTime, Date oblastlogintime) {
	    if (oblastlogintime ==null && entrustLoginTime == null) {
			  return null;
		  } else if(oblastlogintime==null) { 
			  return convertDatetoGeneralized(entrustLoginTime);
		  } else if(entrustLoginTime == null) {
			  return convertDatetoGeneralized(oblastlogintime);
		  } else if(entrustLoginTime.after(oblastlogintime)){
			  return convertDatetoGeneralized(entrustLoginTime);
		  }else {
			  return convertDatetoGeneralized(oblastlogintime);
		  }
  }
}