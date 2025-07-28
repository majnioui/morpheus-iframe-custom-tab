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

    // Helper method to get vCD session token, returns [token, error]
    private List getVcdSessionToken() {
        try {
            def settings = getPluginSettings()
            String apiBaseUrl = settings.apiBaseUrl
            String vcdUser = settings.vcdUser
            String vcdPassword = settings.vcdPassword
            if (!apiBaseUrl || !vcdUser || !vcdPassword) {
                return [null, "API base URL, vcdUser, or vcdPassword not configured in plugin settings."]
            }
            URL url = new URL("${apiBaseUrl}/api/sessions")
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("POST")
            String userCredentials = "${vcdUser}:${vcdPassword}"
            String basicAuth = "Basic " + Base64.encoder.encodeToString(userCredentials.getBytes("UTF-8"))
            connection.setRequestProperty("Authorization", basicAuth)
            connection.setRequestProperty("Accept", "application/*+xml;version=36.0")
            connection.setDoOutput(false)
            connection.connect()
            String token = connection.getHeaderField("x-vcloud-authorization")
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
            log.error("Failed to get vCD session token: ${ex.message}")
            return [null, "Auth error: ${ex.message}"]
        }
    }

    // Helper method to fetch backups, returns [backups, error]
    private List fetchBackups(String token) {
        try {
            def settings = getPluginSettings()
            String apiBaseUrl = settings.apiBaseUrl
            String urlStr = "${apiBaseUrl}/backups"
            URL url = new URL(urlStr)
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("GET")
            connection.setRequestProperty("X-VCAV-Auth", token)
            connection.setRequestProperty("Accept", "application/json")
            connection.connect()
            String response = connection.inputStream.text
            connection.disconnect()
            def json = new groovy.json.JsonSlurper().parseText(response)
            return [json, null]
        } catch (Exception ex) {
            log.error("Failed to fetch backups: ${ex.message}")
            return [null, "Fetch backups error: ${ex.message}"]
        }
    }

    // Helper to fetch backup repositories for a VDC
    private List fetchBackupRepositories(String token, String vdcId) {
        try {
            def settings = getPluginSettings()
            String apiBaseUrl = settings.apiBaseUrl
            String urlStr = "${apiBaseUrl}/api/admin/extension/vdc/${vdcId}/BackupRepositories"
            URL url = new URL(urlStr)
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("GET")
            connection.setRequestProperty("x-vcloud-authorization", token)
            connection.setRequestProperty("Accept", "application/*+xml;version=36.0")
            connection.connect()
            def xml = new groovy.util.XmlSlurper().parse(connection.inputStream)
            connection.disconnect()
            def repos = []
            xml.BackupRepositoryReference.each { repo ->
                repos << [
                    name: repo.@name.text(),
                    id: repo.@id.text(),
                    href: repo.@href.text()
                ]
            }
            return [repos, null]
        } catch (Exception ex) {
            log.error("Failed to fetch backup repositories: ${ex.message}")
            return [null, "Fetch backup repositories error: ${ex.message}"]
        }
    }

    // Helper to fetch vApp backups for a repository
    private List fetchVappBackups(String token, String repoId) {
        try {
            def settings = getPluginSettings()
            String apiBaseUrl = settings.apiBaseUrl
            String urlStr = "${apiBaseUrl}/api/admin/extension/EmcBackupService/backupRepository/${repoId}/vapps?orderBy=id&reverse=true&filters=&show=backup&page=1&pageSize=100000"
            URL url = new URL(urlStr)
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("GET")
            connection.setRequestProperty("x-vcloud-authorization", token)
            connection.setRequestProperty("Accept", "application/*+xml;version=36.0")
            connection.connect()
            def xml = new groovy.util.XmlSlurper().parse(connection.inputStream)
            connection.disconnect()
            def vapps = []
            xml.VappDetail.each { vapp ->
                vapps << [
                    vAppguid: vapp.@vAppguid.text(),
                    vAppName: vapp.@vAppName.text(),
                    status: vapp.@status.text(),
                    vdcName: vapp.@vdcName.text(),
                    expired: vapp.@expired.text(),
                    eligible: vapp.@eligible.text(),
                    numberOfVMs: vapp.@numberOfVMs.text(),
                    policyName: vapp.policy?.@name?.text(),
                    policyGuid: vapp.policy?.@guid?.text()
                ]
            }
            return [vapps, null]
        } catch (Exception ex) {
            log.error("Failed to fetch vApp backups: ${ex.message}")
            return [null, "Fetch vApp backups error: ${ex.message}"]
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
        // Generate a nonce for CSP
        String nonce = java.util.UUID.randomUUID().toString().replaceAll('-', '')
        viewData['nonce'] = nonce
        // Use the VDC ID from the user's environment
        String vdcId = "c3577fa8-65a5-4a49-8c10-b3ed95f03689"
        def (token, authError) = getVcdSessionToken()
        if (authError) {
            viewData['error'] = authError
            viewData['backups'] = null
        } else {
            def (repos, repoError) = fetchBackupRepositories(token, vdcId)
            if (repoError || !repos || repos.size() == 0) {
                viewData['error'] = repoError ?: 'No backup repositories found.'
                viewData['backups'] = null
            } else {
                // For demo, just use the first repository
                def repoId = repos[0].id
                def (backups, backupsError) = fetchVappBackups(token, repoId)
                if (backupsError) {
                    viewData['error'] = backupsError
                    viewData['backups'] = null
                } else {
                    viewData['backups'] = backups
                    viewData['error'] = null
                }
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
		csp.styleSrc = "'self' 'unsafe-inline'"
		return csp
	}
}