/*
Copyright 2020 Twitter, Inc.
SPDX-License-Identifier: Apache-2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
https://openapi-generator.tech
Do not edit the class manually.
*/


// package com.twitter.clientlib;
package com.esri.geoevent.transport.twitter;


import java.util.HashSet;
import java.util.Set;
import com.twitter.clientlib.ApiClient;
import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.TwitterCredentialsBearer;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.ResourceUnauthorizedProblem;
import com.twitter.clientlib.model.Get2TweetsIdResponse;

/**
 * Initializing the TwitterApi using a non default ApiClient.
 */
public class NonDefaultClient {

  public static void main(String[] args) {
    NonDefaultClient example = new NonDefaultClient();
    // Create an ApiClient and use it instead of the default one in TwitterApi
    ApiClient apiClient = new ApiClient();
    apiClient.setTwitterCredentials(new TwitterCredentialsBearer(System.getenv("TWITTER_BEARER_TOKEN")));
    TwitterApi apiInstance = new TwitterApi(apiClient);
    example.callApi(apiInstance);
  }

  public void callApi(TwitterApi apiInstance) {
    Set<String> tweetFields = new HashSet<>();
    tweetFields.add("author_id");
    tweetFields.add("id");
    tweetFields.add("created_at");

    try {
      // findTweetById
      Get2TweetsIdResponse result = apiInstance.tweets().findTweetById("20")
        .tweetFields(tweetFields)
        .execute();
      if (result.getErrors() != null && result.getErrors().size() > 0) {
        System.out.println("Error:");
        result.getErrors().forEach(e -> {
          System.out.println(e.toString());
          if (e instanceof ResourceUnauthorizedProblem) {
            System.out.println(e.getTitle() + " " + e.getDetail());
          }
        });
      } else {
        System.out.println("findTweetById - Tweet Text: " + result.toString());
      }
    } catch (ApiException e) {
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
