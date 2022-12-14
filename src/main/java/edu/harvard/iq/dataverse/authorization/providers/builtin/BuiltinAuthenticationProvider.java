package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.*;

import java.util.Arrays;
import java.util.List;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.RestUsachConectorServicesUtil;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * An authentication provider built into the application. Uses JPA and the
 * local database to store the users.
 *
 * @author michael
 */
public class BuiltinAuthenticationProvider implements CredentialsAuthenticationProvider {

    public static final String PROVIDER_ID = "builtin";
    /**
     * TODO: Think more about if it really makes sense to have the key for a
     * credential be a Bundle key. What if we want to reorganize our Bundle
     * files and rename some Bundle keys? Would login be broken until we update
     * the strings below?
     */
    public static final String KEY_USERNAME_OR_EMAIL = "login.builtin.credential.usernameOrEmail";
    public static final String KEY_PASSWORD = "login.builtin.credential.password";
    private static List<Credential> CREDENTIALS_LIST;

    final BuiltinUserServiceBean bean;
    final AuthenticationServiceBean authBean;
    DataverseServiceBean dataverseService;
    private PasswordValidatorServiceBean passwordValidatorService;

    public BuiltinAuthenticationProvider( BuiltinUserServiceBean aBean, PasswordValidatorServiceBean passwordValidatorService, AuthenticationServiceBean auBean  ) {
        this.bean = aBean;
        this.authBean = auBean;
        this.passwordValidatorService = passwordValidatorService;
        CREDENTIALS_LIST = Arrays.asList(new Credential(KEY_USERNAME_OR_EMAIL), new Credential(KEY_PASSWORD, true));
    }

    public BuiltinAuthenticationProvider( BuiltinUserServiceBean aBean, PasswordValidatorServiceBean passwordValidatorService, AuthenticationServiceBean auBean , DataverseServiceBean dataverseService ) {
        this.bean = aBean;
        this.authBean = auBean;
        this.passwordValidatorService = passwordValidatorService;
        this.dataverseService = dataverseService;
        CREDENTIALS_LIST = Arrays.asList(new Credential(KEY_USERNAME_OR_EMAIL), new Credential(KEY_PASSWORD, true));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), BundleUtil.getStringFromBundle("auth.providers.title.builtin"), "Internal user repository");
    }

    @Override
    public boolean isPasswordUpdateAllowed() {
        return true;
    }

    @Override
    public boolean isUserInfoUpdateAllowed() {
        return true;
    }

    @Override
    public boolean isUserDeletionAllowed() {
        return true;
    }

    @Override
    public void deleteUser(String userIdInProvider) {
        bean.removeUser(userIdInProvider);
    }

    @Override
    public void updatePassword(String userIdInProvider, String newPassword) {
        BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
        biUser.updateEncryptedPassword(PasswordEncryption.get().encrypt(newPassword),
                                       PasswordEncryption.getLatestVersionNumber());
        bean.save(biUser);
    }

    /**
     * Validates that the passed password is indeed the password of the user.
     * @param userIdInProvider
     * @param password
     * @return {@code true} if the password matches the user's password; {@code false} otherwise.
     */
    @Override
    public Boolean verifyPassword( String userIdInProvider, String password ) {
        BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
        if ( biUser == null ) return null;
        return PasswordEncryption.getVersion(biUser.getPasswordEncryptionVersion())
                                 .check(password, biUser.getEncryptedPassword());
    }


    @Override
    public AuthenticationResponse authenticate( AuthenticationRequest authReq ) {
        BuiltinUser u = bean.findByUserName(authReq.getCredential(KEY_USERNAME_OR_EMAIL) );
        AuthenticatedUser authUser = null;
        RestUsachConectorServicesUtil utilRestConector = new RestUsachConectorServicesUtil();

        if(u == null) { //If can't find by username in builtin, get the auth user and then the builtin
            authUser = authBean.getAuthenticatedUserByEmail(authReq.getCredential(KEY_USERNAME_OR_EMAIL));
            if (authUser == null) { //if can't find by email return bad username, etc.
                return AuthenticationResponse.makeFail("Bad username, email address, or password");
            }
            u = bean.findByUserName(authUser.getUserIdentifier());
        }

        if ( u == null ) return AuthenticationResponse.makeFail("Bad username, email address, or password");

        //comentado para cambio de validacion de usuario
        //boolean userAuthenticated = PasswordEncryption.getVersion(u.getPasswordEncryptionVersion())
        //                                    .check(authReq.getCredential(KEY_PASSWORD), u.getEncryptedPassword() );

        //comentado por cambio de metodo a utileria de servicios de usach que se conectan por REST
        /*
        boolean userAuthenticated = Boolean.FALSE;
        userAuthenticated = autenticatedLdap(authReq.getCredential(KEY_USERNAME_OR_EMAIL), authReq.getCredential(KEY_PASSWORD));
        if ( ! userAuthenticated ) {
            return AuthenticationResponse.makeFail("Bad username or password");
        }
        */

        authUser = authBean.getAuthenticatedUser(authReq.getCredential(KEY_USERNAME_OR_EMAIL));

        if(authUser!= null && !authUser.isSuperuser()) {

            // codigo generado para autenticar con ldap, tomar la respuesta e ir por los datos del academico
            boolean userAuthenticated = Boolean.FALSE;
            LdapUsachResponse response = utilRestConector.autenticatedLdap(authReq.getCredential(KEY_USERNAME_OR_EMAIL), authReq.getCredential(KEY_PASSWORD));
            userAuthenticated = response.getSuccess();

            if (!userAuthenticated) {
                return AuthenticationResponse.makeFail("Bad username or password");
            }

            //verificar si el rut viene con dv y guion o solo rutdv sin guion para sacar el rut sin dv
            String run = response.getData().getRut();
            AcademicoUsachResponse registroAcademico = utilRestConector.apiAcademico(run);

            if (registroAcademico.getPlanta() == null || (!"ACADEMICOS".equalsIgnoreCase(registroAcademico.getPlanta().toUpperCase()))) {
                return AuthenticationResponse.makeFail("Access not alowed for user");
            }

            //verificando si el usuario tiene o no afiliacion
            if (authUser.getAffiliation() == null || !(registroAcademico.getCodigoUnidadMayorContrato().equalsIgnoreCase(authUser.getAffiliation()))) {
                authBean.updateAffiliationForUserByName(registroAcademico.getCodigoUnidadMayorContrato(), authReq.getCredential(KEY_USERNAME_OR_EMAIL));
            }

            //1.- ir porl el dataverse
            Long dvId = 0L;
            //dvId = findDvIdByAffiliation(String addiliarion);
            List<Dataverse> lista =dataverseService.filterByAliasQuery(registroAcademico.getCodigoUnidadMayorContrato());

            if (lista == null || lista.size()==0) {
                return AuthenticationResponse.makeFail("Access not alowed for user, dataverse not found");
            }
            Dataverse dvRecord = lista.get(0);
            dvId = dvRecord.getId();

            if (dvId == -1L) {
                return AuthenticationResponse.makeFail("Access not alowed for user, dataverse not found");
            }

            Boolean existUser = Boolean.FALSE;
            existUser = utilRestConector.apiExistUsuarioInDataverse(dvId, authReq.getCredential(KEY_USERNAME_OR_EMAIL));
            if (!existUser) {
                return AuthenticationResponse.makeFail("Access not alowed for user, user not asociated in dataverse");
            }
        }

        // sin el usuario no teine configurada su afiliacion se asigna la que serponde el apiacademico,

        /* comentado para cambio de validacion de usuario
        if ( u.getPasswordEncryptionVersion() < PasswordEncryption.getLatestVersionNumber() ) {
            try {
                String passwordResetUrl = bean.requestPasswordUpgradeLink(u);

                return AuthenticationResponse.makeBreakout(u.getUserName(), passwordResetUrl);
            } catch (PasswordResetException ex) {
                return AuthenticationResponse.makeError("Error while attempting to upgrade password", ex);
            }
//        } else {
//            return AuthenticationResponse.makeSuccess(u.getUserName(), u.getDisplayInfo());
        }
        final List<String> errors = passwordValidatorService.validate(authReq.getCredential(KEY_PASSWORD));
        if (!errors.isEmpty()) {
            try {
                String passwordResetUrl = bean.requestPasswordComplianceLink(u);
                return AuthenticationResponse.makeBreakout(u.getUserName(), passwordResetUrl);
            } catch (PasswordResetException ex) {
                return AuthenticationResponse.makeError("Error while attempting to upgrade password", ex);
            }
        }
       */

        if(null == authUser) {
            authUser = authBean.getAuthenticatedUser(u.getUserName());
        }

        return AuthenticationResponse.makeSuccess(u.getUserName(), authUser.getDisplayInfo());
    }

    private Boolean autenticatedLdap(String user, String password){

       // DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpClient httpClient =  HttpClientBuilder.create().build();
        Boolean b = Boolean.FALSE;

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
                b = Boolean.TRUE;
            }

        }catch(Exception e){
            return Boolean.FALSE;
        }
        finally
        {
            //Important: Close the connect
            httpClient.getConnectionManager().shutdown();
        }

        return b;
    }

    @Override
    public List<Credential> getRequiredCredentials() {
        return CREDENTIALS_LIST;
    }

    @Override
    public boolean isOAuthProvider() {
        return false;
    }

    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

}