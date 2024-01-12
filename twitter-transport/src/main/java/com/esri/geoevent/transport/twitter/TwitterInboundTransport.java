/*
  Copyright 1995-2013 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
 */

package com.esri.geoevent.transport.twitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.InboundTransportBase;
import com.esri.ges.transport.TransportDefinition;
import com.esri.ges.util.Validator;

// import twitter4j.FilterQuery;
// import twitter4j.RawStreamListener;
// import twitter4j.TwitterStream;
// import twitter4j.TwitterStreamFactory;
// import twitter4j.conf.ConfigurationBuilder; 

//API2.0
import com.twitter.clientlib.ApiClient;
import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.Configuration;
import com.twitter.clientlib.JSON;
import com.twitter.clientlib.auth.*;
import com.twitter.clientlib.model.*;
import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.TwitterCredentialsBearer;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.api.TweetsApi;
import java.io.InputStream;
import com.google.common.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.time.OffsetDateTime;

public class TwitterInboundTransport extends InboundTransportBase implements Runnable
{
  public TwitterInboundTransport(TransportDefinition definition) throws ComponentException
  {
    super(definition);
  }

  static final private BundleLogger LOGGER    = BundleLoggerFactory.getLogger(TwitterInboundTransport.class);

  private String                    consumerKey;
  private String                    consumerSecret;
  private String                    accessToken;
  private String                    accessTokenSecret;

  private long[]                    follows   = null;
  private String[]                  tracks    = null;
  private double[][]                locations = null;
  private int                       count     = -1;

  private String                    filterString;
  // private TwitterStream             twitterStream;
  private Thread                    thread    = null;

  // For API2.0
  // private Get2TweetsIdResponse result;

  @Override
  public synchronized void start()
  {
    try
    {
      switch (getRunningState())
      {
        case STARTING:
        case STARTED:
        case STOPPING:
          return;
      }
      setRunningState(RunningState.STARTING);
      thread = new Thread(this);
      thread.start();
    }
    catch (Exception e)
    {
      LOGGER.error("UNEXPECTED_ERROR_STARTING", e);
      stop();
    }
  }

  @Override
  public synchronized void stop()
  {
    try
    {
      // if (this.twitterStream != null)
      // {
      //   twitterStream.cleanUp();
      //   twitterStream.shutdown();
      // }
    }
    catch (Exception ex)
    {
      LOGGER.error("UNABLE_TO_CLOSE", ex);
    }
    setRunningState(RunningState.STOPPED);
    // LOGGER.debug("INBOUND_STOP");
    LOGGER.info("INBOUND_STOP");
  }

  @Override
  public void validate()
  {
    // LOGGER.debug("INBOUND_SKIP_VALIDATION");
    LOGGER.info("INBOUND_SKIP_VALIDATION");
  }

  public void applyProperties() throws Exception
  {
    LOGGER.info(OAuth.CONSUMER_KEY + ":セット");
    if (getProperty(OAuth.CONSUMER_KEY).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_KEY).getValue();
      if (value.length() > 0)
      {
        consumerKey = cryptoService.decrypt(value);
      }
    }
    LOGGER.info(OAuth.CONSUMER_SECRET + ":セット");
    if (getProperty(OAuth.CONSUMER_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_SECRET).getValue();
      if (value.length() > 0)
      {
        consumerSecret = cryptoService.decrypt(value);
      }
    }
    LOGGER.info(OAuth.ACCESS_TOKEN + ":セット");
    if (getProperty(OAuth.ACCESS_TOKEN).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN).getValue();
      if (value.length() > 0)
      {
        accessToken = cryptoService.decrypt(value);
      }
    }
    LOGGER.info(OAuth.ACCESS_TOKEN_SECRET + ":セット");
    if (getProperty(OAuth.ACCESS_TOKEN_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN_SECRET).getValue();
      if (value.length() > 0)
      {
        accessTokenSecret = cryptoService.decrypt(value);
      }
    }

    LOGGER.info("follow:セット");
    StringBuilder paramsStr = new StringBuilder();
    if (getProperty("follow").isValid())
    {
      String value = (String) getProperty("follow").getValue();
      if (StringUtils.isNotEmpty(value))
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
    LOGGER.info("track:セット");
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
    LOGGER.info("locations:セット");
    if (getProperty("locations").isValid())
    {
      String value = (String) getProperty("locations").getValue();
      if (value != null && value.length() > 0)
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
          int dimension = length / 2;
          locations = new double[dimension][2];
          for (int i = 0; i < dimension; i++)
          {
            for (int j = 0; j < 2; j++)
            {
              locations[i][j] = Double.parseDouble(crdStrs[i * 2 + j].trim());
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
        LOGGER.error("INBOUND_COUNT_VALIDATION");
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
      filterString = paramsStr.toString();
    }
  }

  @Override
  public void run()
  {
    receiveData();

  }

  // reference URL is https://dev.to/twitterdev/a-guide-to-working-with-the-twitter-api-v2-in-java-using-twitter-api-java-sdk-c8n.
  // reference URL2   https://github.com/twitterdev/twitter-api-java-sdk/blob/main/docs/TweetsApi.md#samplestream
  // reference URL3   https://zenn.dev/lamrongol/articles/c006704ba525dc
  // reference URL4   https://javadoc.io/doc/com.twitter/twitter-api-java-sdk/2.0.2/allclasses.html
  private void receiveData()
  {
    try
    {
      applyProperties();
      setRunningState(RunningState.STARTED);

      // API 1.1
      // ConfigurationBuilder cb = new ConfigurationBuilder();
      // cb.setDebugEnabled(true);
      // cb.setOAuthConsumerKey(consumerKey);
      // cb.setOAuthConsumerSecret(consumerSecret);
      // cb.setOAuthAccessToken(accessToken);
      // cb.setOAuthAccessTokenSecret(accessTokenSecret);
      // twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

      // For API 2.0
      // https://github.com/twitterdev/twitter-api-java-sdk?tab=readme-ov-file#maven-users
      TwitterApi apiInstance = new TwitterApi(new TwitterCredentialsOAuth2(
      consumerKey,
      consumerSecret,
      accessToken,
      accessTokenSecret)); 

      // String query = "covid -is:retweet";
      // int maxResults = 100;
      // TweetSearchResponse result = apiInstance.tweets()
      // .tweetsRecentSearch(query, null, null, null, null, maxResults,
      //           null, null, null, null, null, null, null, null, null);     

      Set<String> tweetFields = new HashSet<>(Arrays.asList("id", "author_id", "created_at", "lang", "source", "text", "entities", "public_metrics")); // Set<String> | A comma separated list of Tweet fields to display.
      Set<String> expansions = new HashSet<>(Arrays.asList()); // Set<String> | A comma separated list of fields to expand.
      Set<String> mediaFields = new HashSet<>(Arrays.asList()); // Set<String> | A comma separated list of Media fields to display.
      Set<String> pollFields = new HashSet<>(Arrays.asList()); // Set<String> | A comma separated list of Poll fields to display.
      Set<String> userFields = new HashSet<>(Arrays.asList()); // Set<String> | A comma separated list of User fields to display.
      Set<String> placeFields = new HashSet<>(Arrays.asList()); // Set<String> | A comma separated list of Place fields to display.
      try {
        AddOrDeleteRulesRequest addOrDeleteRulesRequest = new AddOrDeleteRulesRequest();
        // Boolean dryRun = true;
        AddOrDeleteRulesResponse rulesResponse = apiInstance.tweets().addOrDeleteRules(addOrDeleteRulesRequest)
            // .dryRun(dryRun)
            .execute();
        LOGGER.info("rulesResponse:"+rulesResponse);

        InputStream result = apiInstance.tweets().sampleStream()
            .tweetFields(tweetFields)
            .expansions(expansions)
            .mediaFields(mediaFields)
            .pollFields(pollFields)
            .userFields(userFields)
            .placeFields(placeFields)
            .execute();

        try {
          // JSON json = new JSON();
          Type localVarReturnType = new TypeToken<StreamingTweetResponse>() {}.getType();
          BufferedReader reader = new BufferedReader(new InputStreamReader(result, "UTF-8"));
          String line = reader.readLine();
          while (line != null) {
            if (line.isEmpty()) {
              System.out.println("==> Empty line");
              line = reader.readLine();
              continue;
            }
            //System.out.println(response != null ? response.toString() : "Null object");
            try {
              StreamingTweetResponse response = JSON.getGson().fromJson(line, localVarReturnType);
              if (response != null) {
                Tweet tweet = response.getData();
                //処理
                LOGGER.info("receice data:"+tweet.getText());
                receive(tweet.getText());
              }
            } catch (IllegalArgumentException e) {//IllegalArgumentExceptionが起きてもプログラムが停止しないように
              System.out.println("IllegalArgumentException: " + line);
              e.printStackTrace();
            }
            line = reader.readLine();
          }
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println(e);
        }
      } catch (ApiException e) {
        LOGGER.error("Exception when calling TweetsApi#sampleStream");
        LOGGER.error("Status code: " + e.getCode());
        LOGGER.error("Reason: " + e.getResponseBody());
        LOGGER.error("Response headers: " + e.getResponseHeaders());
        e.printStackTrace();
      }


      // API 1.0
      // RawStreamListener rl = new RawStreamListener()
      //   {

      //     @Override
      //     public void onException(Exception ex)
      //     {
      //       LOGGER.error("INBOUND_TRANSPORT_RAW_STREAM_LISTERNER_EXCEPTION", ex.getMessage());
      //     }

      //     @Override
      //     public void onMessage(String rawString)
      //     {
      //       receive(rawString);
      //     }
      //   };

      // FilterQuery fq = new FilterQuery();

      // String keywords[] = tracks;

      // if (follows != null && follows.length > 0)
      //   fq.follow(follows);
      // else if (keywords != null && keywords.length > 0)
      //   fq.track(keywords);
      // else if (locations != null)
      //   fq.locations(locations);
      // else
      //   throw new Exception("INBOUND_TRANSPORT_NOFILTER_ERROR");

      // fq.count(count);

      LOGGER.info("INBOUND_TRANSPORT_FILTER", filterString);

      // twitterStream.addListener(rl);
      // twitterStream.filter(fq);

    }
    catch (Throwable ex)
    {
      LOGGER.error("UNEXPECTED_ERROR", ex);
      setRunningState(RunningState.ERROR);
    }
  }

  private void receive(String tweet)
  {
    if (!Validator.isEmpty(tweet))
    {
      byte[] newBytes = tweet.getBytes();

      ByteBuffer bb = ByteBuffer.allocate(newBytes.length);
      try
      {
        bb.put(newBytes);
        bb.flip();
        byteListener.receive(bb, "");
        bb.clear();
      }
      catch (BufferOverflowException boe)
      {
        LOGGER.error("BUFFER_OVERFLOW_ERROR", boe);
        bb.clear();
        setRunningState(RunningState.ERROR);
      }
      catch (Exception e)
      {
        LOGGER.error("UNEXPECTED_ERROR2", e);
        stop();
        setRunningState(RunningState.ERROR);
      }
    }
  }
}
