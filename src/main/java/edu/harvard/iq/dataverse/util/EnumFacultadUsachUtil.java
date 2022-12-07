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


import com.google.gson.Gson;
import edu.harvard.iq.dataverse.authorization.AcademicoUsachResponse;
import edu.harvard.iq.dataverse.authorization.AssigmentRecord;
import edu.harvard.iq.dataverse.authorization.AssigmentsUsachResponse;
import edu.harvard.iq.dataverse.authorization.LdapUsachResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

;

/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation, 
 * but the old code is ported creatively on the method-by-method basis.
 * 
 * @author Leonid Andreev
 */
public enum EnumFacultadUsachUtil implements java.io.Serializable  {

    FACULTAD_ADMS_Y_ECONOMÍA(60,"FAE"),
    FACULTAD_DE_CIENCIA(50, "FCIENCIA"),
	FACULTAD_DE_CIENCIAS_MEDICAS(83, "FCM"),
	FACULTAD_DE_DERECHO(125, "FDERECHO"),
	FACULTAD_DE_HUMANIDADES(55, "FAHU"),
	FACULTAD_DE_INGENIERÍA(40,"FING"),
	FACULTAD_DE_QUÍMICA_Y_BIOLOGÍA(95,"FQYB"),
	FACULTAD_TECNOLÓGICA(65,"FACTEC"),
	ESCUELA_DE_ARQUITECTURA(81,"ARQUITECTURA"),
	PROGRAMA_DE_BACHILLERATO(54,	"BACHI");

    private final Integer codigoFactultad;
    private final String codigoAffiliation;

    EnumFacultadUsachUtil(Integer codigoFactultad, String codigoAffiliation){
        this.codigoAffiliation=codigoAffiliation;
        this.codigoFactultad=codigoFactultad;
    }

    public Integer getCodigoFactultad() {
        return codigoFactultad;
    }

    public String getCodigoAffiliation() {
        return codigoAffiliation;
    }
}
