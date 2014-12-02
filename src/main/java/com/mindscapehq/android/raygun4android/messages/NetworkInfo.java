package main.java.com.mindscapehq.android.raygun4android.messages;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.http.conn.util.InetAddressUtils;

public class NetworkInfo
{
  public List<String> iPAddress = new ArrayList<String>();

  public NetworkInfo()
  {
    readIPAddress();
  }

  private void readIPAddress()
  {
    try {
      List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

      for (NetworkInterface intf : interfaces)
      {
        List<InetAddress> addrs = Collections.list(intf.getInetAddresses());

        for (InetAddress addr : addrs)
        {
          if (!addr.isLoopbackAddress())
          {
            String sAddr = addr.getHostAddress().toUpperCase();
            boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);

            if (isIPv4)
            {
              if (!iPAddress.contains(sAddr))
              {
                iPAddress.add(sAddr);
              }
            }
            else
            {
              if (!isIPv4)
              {
                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                String delimited = delim < 0 ? sAddr : sAddr.substring(0, delim);

                if (!iPAddress.contains(delimited))
                {
                  iPAddress.add(delimited);
                }
              }
            }
          }
        }
      }
    } catch (Exception ex) { }
  }
}
