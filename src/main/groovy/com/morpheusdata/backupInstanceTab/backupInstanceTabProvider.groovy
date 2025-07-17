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

    // Helper method to fetch and parse plugin settings
    private Map getPluginSettings() {
        def settings = [:]
        try {
            def pluginSettings = morpheus.getSettings(plugin)
            def settingsOutput = ""
            pluginSettings.subscribe({ outData -> settingsOutput = outData }, { error -> log.error(error.printStackTrace()) })
            def slurper = new groovy.json.JsonSlurper()
            settings = slurper.parseText(settingsOutput)
        } catch (Exception ex) {
            log.error("Could not parse plugin settings: ", ex)
        }
        return settings
    }

    // Helper method to get session token, now returns [token, error]
    private List getSessionToken() {
        try {
            def settings = getPluginSettings()
            String apiBaseUrl = settings.apiBaseUrl ?: "https://portal.cdc.atlascs.ma"
            String apiUsername = settings.apiUsername ?: "mcmadmin4@MCM-Org"
            String apiPassword = settings.apiPassword ?: "ACSPower!2025!"
            if(!apiBaseUrl || !apiUsername || !apiPassword) {
                return [null, "API base URL, username, or password not configured in plugin settings."]
            }
            URL url = new URL("${apiBaseUrl}/cloudapi/1.0.0/sessions")
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("POST")
            String userCredentials = "${apiUsername}:${apiPassword}"
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
            def settings = getPluginSettings()
            String apiBaseUrl = settings.apiBaseUrl ?: "https://portal.cdc.atlascs.ma"
            URL url = new URL("${apiBaseUrl}/cloudapi/1.0.0/orgs")
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
        def settings = getPluginSettings()
        // Set the tab name dynamically from settings if available
        if (settings.instanceTabName) {
            name = settings.instanceTabName
        }
        // Add tabTitle to viewData for the template
        viewData['tabTitle'] = settings.instanceTabTitle
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