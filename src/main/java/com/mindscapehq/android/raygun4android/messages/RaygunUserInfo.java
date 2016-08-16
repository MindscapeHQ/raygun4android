package main.java.com.mindscapehq.android.raygun4android.messages;

public class RaygunUserInfo
{
  public Boolean IsAnonymous;

  public String Email;

  public String FullName;

  public String FirstName;

  public String Uuid;

  public String Identifier;

  /**
   * Set the current user's info to be transmitted - any parameters can be null if the data is not available or
   * you do not wish to send it.
   * @param firstName The user's first name
   * @param fullName The user's full name - if setting the first name you should set this too
   * @param emailAddress User's email address
   * @param uuid Device identifier - if this is null we will attempt to generate it automatically (legacy behavior).
   * @param isAnonymous Whether this user data represents an anonymous user
   * @param identifier Unique identifier for this user. Set this to the internal identifier you use to look up users,
   *                   or a correlation ID for anonymous users if you have one. It doesn't have to be unique, but we will treat
   *                   any duplicated values as the same user. If you use their email address here, pass it in as the 'emailAddress' parameter too.
   */
  public RaygunUserInfo(String identifier, String firstName, String fullName, String emailAddress, String uuid, Boolean isAnonymous)
  {
    FirstName = firstName;
    FullName = fullName;
    Email = emailAddress;
    Uuid = uuid;
    IsAnonymous = isAnonymous;
    Identifier = identifier;
  }

  public RaygunUserInfo() { }
}
