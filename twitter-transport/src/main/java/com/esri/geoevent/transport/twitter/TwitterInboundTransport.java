package com.esri.geoevent.transport.twitter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.transport.TransportContext;
import com.esri.ges.transport.TransportDefinition;
import com.esri.ges.transport.http.HttpInboundTransport;
import com.esri.ges.transport.http.HttpTransportContext;

public class TwitterInboundTransport extends HttpInboundTransport
{
  static final private Log logger    = LogFactory.getLog(TwitterInboundTransport.class);

  private String           consumerKey;
  private String           consumerSecret;
  private String           accessToken;
  private String           accessTokenSecret;
  private String           postBodyOrg;

  private long[]           follows   = null;
  private String[]         tracks    = null;
  private double[][]       locations = null;
  private int              count     = -1;

  public TwitterInboundTransport(TransportDefinition definition) throws ComponentException
  {
    super(definition);
  }

  @Override
  public synchronized void start()
  {
    super.start();
    logger.debug("Http-Oauth-Inbound started.");
  }

  @Override
  public synchronized void stop()
  {
    super.stop();
    logger.debug("Http-Oauth-Inbound stopped.");
  }

  @Override
  public synchronized void setup()
  {
    super.setup();
    try
    {
      applyProperties();
      // encode the postBody
      postBodyOrg = postBody;
      postBody = OAuth.encodePostBody(postBodyOrg);
      logger.debug(postBody);
      consoleDebugPrintLn(postBody);
    }
    catch (Exception e)
    {
      logger.error(e.getMessage(), e);
    }
  }

  @Override
  public void beforeConnect(TransportContext context)
  {
    // String url = "https://stream.twitter.com/1/statuses/filter.json";

    HttpRequest request = ((HttpTransportContext) context).getHttpRequest();

    String authorizationHeader = OAuth.createOAuthAuthorizationHeader(clientUrl, httpMethod, postBodyOrg, accessToken, accessTokenSecret, consumerKey, consumerSecret);

    // logger.debug(authorizationHeader);
    request.addHeader(OAuth.AUTHORIZATION, authorizationHeader);
    request.addHeader(OAuth.ACCEPT, OAuth.ACCEPT_VALUES);// "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
    request.setHeader(OAuth.CONTENT_TYPE, this.postBodyType);// "application/x-www-form-urlencoded"
  }

  @Override
  public void validate()
  {

    logger.debug("Inbound Skip validation...");
  }

  @Override
  public void onReceive(TransportContext context)
  {
    super.onReceive(context);
    consoleDebugPrintLn("onReceive");
  }

  public void applyProperties() throws Exception
  {
    if (getProperty(OAuth.CONSUMER_KEY).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_KEY).getValue();
      if (value.length() > 0)
      {
        consumerKey = value;
      }
    }
    if (getProperty(OAuth.CONSUMER_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_SECRET).getValue();
      if (value.length() > 0)
      {
        consumerSecret = value;
      }
    }
    if (getProperty(OAuth.ACCESS_TOKEN).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN).getValue();
      if (value.length() > 0)
      {
        accessToken = value;
      }
    }
    if (getProperty(OAuth.ACCESS_TOKEN_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN_SECRET).getValue();
      if (value.length() > 0)
      {
        accessTokenSecret = value;
      }
    }

    StringBuilder paramsStr = new StringBuilder();
    if (getProperty("follow").isValid())
    {
      String value = (String) getProperty("follow").getValue();
      if (value.length() > 0)
      {
        paramsStr.append("follow=" + value);
        String[] flwStrs = value.split(",");
        if (flwStrs.length > 0)
        {
          follows = new long[flwStrs.length];
          for (int i = 0; i < flwStrs.length; i++)
          {
            follows[i] = Long.parseLong(flwStrs[i].trim());
          }
        }
      }
    }
    if (getProperty("track").isValid())
    {
      String value = (String) getProperty("track").getValue();
      if (value.length() > 0)
      {
        if (paramsStr.length() > 0)
        {
          paramsStr.append("&");
        }
        paramsStr.append("track=" + value);

        tracks = value.split(",");
        for (int i = 0; i < tracks.length; i++)
        {
          tracks[i] = tracks[i].trim();
        }
      }
    }
    if (getProperty("locations").isValid())
    {
      String value = (String) getProperty("locations").getValue();
      if (value.length() > 0)
      {
        if (paramsStr.length() > 0)
        {
          paramsStr.append("&");
        }
        paramsStr.append("locations=" + value);

        String[] crdStrs = value.split(",");
        int length = crdStrs.length;
        // lengh should be multiple of 4
        if (length % 4 == 0)
        {
          int dimension = length / 4;
          locations = new double[dimension][4];
          for (int i = 0; i < dimension; i++)
          {
            for (int j = 0; j < 4; j++)
            {
              locations[i][j] = Double.parseDouble(crdStrs[i + j].trim());
            }
          }
        }
      }
    }
    // required elevated access to use
    if (getProperty("count").isValid())
    {
      Object prop = getProperty("count").getValue();
      count = (Integer) prop;
      if (count < -150000 || count > 150000)
      {
        logger.error("Count value should be within -150000 to 150000");
      }
      else
      {
        if (paramsStr.length() > 0)
        {
          paramsStr.append("&");
        }
        paramsStr.append("count=" + prop.toString());
      }
    }
    if (paramsStr.length() > 0)
    {
      postBody = paramsStr.toString();
    }
  }

  public static void consoleDebugPrintLn(String msg)
  {
    String consoleOut = System.getenv("GEP_CONSOLE_OUTPUT");
    if (consoleOut != null && "1".equals(consoleOut))
    {
      System.out.println(msg);
      logger.debug(msg);
    }
  }

  public static void consoleDebugPrint(String msg)
  {
    String consoleOut = System.getenv("GEP_CONSOLE_OUTPUT");
    if (consoleOut != null && "1".equals(consoleOut))
    {
      System.out.print(msg);
      logger.debug(msg);
    }
  }
}