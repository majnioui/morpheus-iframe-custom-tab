package com.morpheusdata.iframeInstanceTab

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

@Slf4j
class IframeInstanceTabProvider extends AbstractInstanceTabProvider {
	Plugin plugin
	MorpheusContext morpheus
    String iframeUrl

    String code = "iframe-instance-tab"
	String name = "PowerM Instance Tab Provider"

	IframeInstanceTabProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheus = context
	}

	@Override
	HTMLResponse renderTemplate(Instance instance) {
		ViewModel<Instance> model = new ViewModel<>()
		TaskConfig config = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()

        // get the plugin settings
        def Object settings
        try {
             def pluginSettings = morpheus.getSettings(plugin)
             def settingsOutput = ""
             pluginSettings.subscribe({outData -> settingsOutput = outData},{error -> println error.printStackTrace()})
             JsonSlurper slurper = new JsonSlurper()
             settings = slurper.parseText(settingsOutput)
        } catch (Exception ex){
             log.error("could not parse plugin settings")
        }

        iframeUrl = settings.iframeUrl
        name = settings.instanceTabName

        def viewData = [:]
        viewData['tabTitle'] = settings.instanceTabTitle
        viewData['iframeUrl'] = settings.iframeUrl
        viewData['iframeHeight'] = settings.iframeHeight

        model.object = viewData
        getRenderer().renderTemplate("hbs/iframeInstanceTab", model)
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