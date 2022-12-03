/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.util;


import edu.harvard.iq.dataverse.authorization.AcademicoUsachResponse;
import edu.harvard.iq.dataverse.authorization.LdapUsachResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.bytesToHumanReadable;

/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation, 
 * but the old code is ported creatively on the method-by-method basis.
 * 
 * @author Leonid Andreev
 */
public class RestUsachConectorServicesUtil implements java.io.Serializable  {
    private static final Logger logger = Logger.getLogger(RestUsachConectorServicesUtil.class.getCanonicalName());
    public LdapUsachResponse autenticatedLdap(String user, String password){

        LdapUsachResponse responseLdap = new LdapUsachResponse();
        responseLdap.setSuccess(Boolean.FALSE);

        // DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpClient httpClient =  HttpClientBuilder.create().build();

        try{
            //Define a postRequest request
            HttpPost postRequest = new HttpPost("https://run.mocky.io/v3/3be5879f-9f16-4594-9f27-8f85f3ce117c");

            //Set the API media type in http content-type header
            postRequest.addHeader("content-type", "application/json");

            //Set the request post body
            StringEntity userEntity = new StringEntity("{\"user\":\""+ user + "\",\"password\":\" "+ password + "\"}");
            postRequest.setEntity(userEntity);

            //Send the request; It will immediately return the response in HttpResponse object if any
            HttpResponse response = httpClient.execute(postRequest);

            //verify the valid error code first
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                responseLdap.setSuccess(Boolean.TRUE);
            }

        }catch(Exception e){
            return responseLdap;
        } finally {
            //Important: Close the connect
            httpClient.getConnectionManager().shutdown();
        }

        return responseLdap;
    }

    public AcademicoUsachResponse apiAcademico(String run){

        AcademicoUsachResponse responseAcademico = null;

        // DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpClient httpClient =  HttpClientBuilder.create().build();

        /*
        user: docente.usach.cl
pass: 8Q57m9GtznM72NXrZgmP3sbt6xGYWJKcnwPagNwK
         */
        try{
            //Define a postRequest request
            HttpGet getRequest = new HttpGet("https://api.dti.usach.cl/api/docente/"+run);

            //Set the API media type in http content-type header
            getRequest.addHeader("content-type", "application/json");

            //Send the request; It will immediately return the response in HttpResponse object if any
            HttpResponse response = httpClient.execute(getRequest);

            //verify the valid error code first
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                responseAcademico = new AcademicoUsachResponse();;
            }

        }catch(Exception e){
            return responseAcademico;
        } finally {
            //Important: Close the connect
            httpClient.getConnectionManager().shutdown();
        }

        return responseAcademico;
    }
}
