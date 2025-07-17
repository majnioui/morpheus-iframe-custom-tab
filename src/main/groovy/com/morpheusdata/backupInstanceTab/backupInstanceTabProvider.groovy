package com.morpheusdata.backupInstanceTab

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.Instance
import com.morpheusdata.model.TaskConfig
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.User
import com.morpheusdata.model.Permission
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

@Slf4j
class BackupInstanceTabProvider extends AbstractInstanceTabProvider {
	Plugin plugin
	MorpheusContext morpheus
    String customTabUrl

    String code = "backup-instance-tab"
	String name = "PowerM Instance Tab Provider"

	BackupInstanceTabProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheus = context
	}

    // Helper method to get session token, now returns [token, error]
    private List getSessionToken() {
        try {
            URL url = new URL("https://portal.cdc.atlascs.ma/api/sessions")
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("POST")
            String userCredentials = "mcmadmin4@MCM-Org:ACSPower!2025!"
            String basicAuth = "Basic " + Base64.encoder.encodeToString(userCredentials.getBytes("UTF-8"))
            connection.setRequestProperty("Authorization", basicAuth)
            connection.setRequestProperty("Accept", "application/*+xml;version=38.1")
            connection.setDoOutput(true)
            connection.connect()
            String token = connection.getHeaderField("x-vmware-vcloud-access-token")
            if (!token) {
                String errorMsg = null
                try {
                    errorMsg = connection.errorStream ? connection.errorStream.text : connection.inputStream.text
                } catch (Exception ex) {
                    errorMsg = "No session token received and could not read error body."
                }
                connection.disconnect()
                return [null, "No session token received. Endpoint response: ${errorMsg}"]
            }
            connection.disconnect()
            return [token, null]
        } catch (Exception ex) {
            log.error("Failed to get session token: ${ex.message}")
            return [null, "Auth error: ${ex.message}"]
        }
    }

    // Helper method to fetch orgs, now returns [orgs, error]
    private List fetchOrgs(String token) {
        try {
            URL url = new URL("https://portal.cdc.atlascs.ma/cloudapi/1.0.0/orgs")
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("GET")
            connection.setRequestProperty("Authorization", "Bearer ${token}")
            connection.setRequestProperty("Accept", "application/*+xml;version=38.1")
            connection.connect()
            String response = connection.inputStream.text
            connection.disconnect()
            def json = new groovy.json.JsonSlurper().parseText(response)
            return [json, null]
        } catch (Exception ex) {
            log.error("Failed to fetch orgs: ${ex.message}")
            return [null, "Fetch orgs error: ${ex.message}"]
        }
    }

    @Override
    HTMLResponse renderTemplate(Instance instance) {
        def viewData = [:]
        def (token, authError) = getSessionToken()
        if (authError) {
            viewData['error'] = authError
            viewData['orgs'] = null
        } else {
            def (orgs, orgsError) = fetchOrgs(token)
            if (orgsError) {
                viewData['error'] = orgsError
                viewData['orgs'] = null
            } else {
                viewData['orgs'] = orgs
                viewData['error'] = null
            }
        }
        ViewModel<Instance> model = new ViewModel<>()
        model.object = viewData
        getRenderer().renderTemplate("hbs/backupInstanceTab", model)
    }

	@Override
	Boolean show(Instance instance, User user, Account account) {
		def show = true
		plugin.permissions.each { Permission permission ->
		    if(user.permissions[permission.code] != permission.availableAccessTypes.last().toString()){
		 		 show = false
		 	}
		}
		return show
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp.frameSrc = "*"
		return csp
	}
}